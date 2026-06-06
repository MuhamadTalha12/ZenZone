package com.zenzone.app.ui.social

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.zenzone.app.R
import com.zenzone.app.model.UserProfile
import com.zenzone.app.repository.UserRepository
import com.zenzone.app.ui.main.MainActivity
import com.zenzone.app.utils.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@Suppress("UNCHECKED_CAST")
class FocusRoomsFragment : Fragment(R.layout.fragment_focus_rooms) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var userRepo: UserRepository

    private lateinit var cvRoomSetup: View
    private lateinit var cvActiveRoom: View
    private lateinit var etRoomCode: com.google.android.material.textfield.TextInputEditText
    private lateinit var tvRoomId: TextView
    private lateinit var tvRoomStatus: TextView
    private lateinit var tvRoomTimer: TextView
    private lateinit var btnHostAction: com.google.android.material.button.MaterialButton
    private lateinit var btnJoinRoom: com.google.android.material.button.MaterialButton
    private lateinit var btnCreateRoom: com.google.android.material.button.MaterialButton
    private lateinit var btnLeaveRoom: View
    private lateinit var rvParticipants: RecyclerView

    private var activeRoomId: String? = null
    private var isHost = false
    private var roomListener: ListenerRegistration? = null
    private var localTimer: CountDownTimer? = null
    private var appMonitorJob: Job? = null
    private var myName = "Zen Mind"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userRepo = UserRepository(requireContext())

        cvRoomSetup = view.findViewById(R.id.cv_room_setup)
        cvActiveRoom = view.findViewById(R.id.cv_active_room)
        etRoomCode = view.findViewById(R.id.et_room_code)
        tvRoomId = view.findViewById(R.id.tv_room_id)
        tvRoomStatus = view.findViewById(R.id.tv_room_status)
        tvRoomTimer = view.findViewById(R.id.tv_room_timer)
        btnHostAction = view.findViewById(R.id.btn_host_action)
        btnJoinRoom = view.findViewById(R.id.btn_join_room)
        btnCreateRoom = view.findViewById(R.id.btn_create_room)
        btnLeaveRoom = view.findViewById(R.id.btn_leave_room)
        rvParticipants = view.findViewById(R.id.rv_participants)

        view.findViewById<View>(R.id.btn_back)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        view.findViewById<View>(R.id.iv_common_menu)?.setOnClickListener {
            (activity as? MainActivity)?.openDrawer()
        }

        rvParticipants.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch {
            try {
                val profile = userRepo.loadProfile()
                myName = profile.userName.ifBlank { "Zen Mind" }
                val displayName = profile.userName.ifBlank { "ZenZone" }
                view.findViewById<TextView>(R.id.tv_app_logo_name)?.text = "🧘 $displayName"
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        btnCreateRoom.setOnClickListener { createNewRoom() }
        btnJoinRoom.setOnClickListener {
            val code = etRoomCode.text?.toString()?.trim()?.uppercase()
            if (code.isNullOrBlank() || code.length != 6) {
                Toast.makeText(requireContext(), "Please enter a valid 6-character room code", Toast.LENGTH_SHORT).show()
            } else {
                joinExistingRoom(code)
            }
        }
        btnLeaveRoom.setOnClickListener { leaveRoom() }
        btnHostAction.setOnClickListener { startRoomTimer() }
    }

    private fun createNewRoom() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "You must be signed in to create a room", Toast.LENGTH_SHORT).show()
            return
        }
        val code = UUID.randomUUID().toString().substring(0, 6).uppercase()
        val roomData = hashMapOf(
            "roomId" to code,
            "hostId" to uid,
            "status" to "WAITING",
            "durationMinutes" to 25,
            "startTime" to null,
            "participants" to hashMapOf(
                uid to hashMapOf(
                    "name" to myName,
                    "status" to "READY"
                )
            )
        )

        firestore.collection("focus_rooms").document(code)
            .set(roomData)
            .addOnSuccessListener {
                isHost = true
                listenToRoom(code)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to create room: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun joinExistingRoom(code: String) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "You must be signed in to join a room", Toast.LENGTH_SHORT).show()
            return
        }
        firestore.collection("focus_rooms").document(code).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    Toast.makeText(requireContext(), "Room not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                
                val status = snapshot.getString("status")
                if (status == "CLOSED") {
                    Toast.makeText(requireContext(), "Room has been closed", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val rawParticipants = snapshot.get("participants") as? Map<String, Any>
                val participants = if (rawParticipants != null) HashMap(rawParticipants) else hashMapOf<String, Any>()
                participants[uid] = hashMapOf(
                    "name" to myName,
                    "status" to "READY"
                )

                firestore.collection("focus_rooms").document(code)
                    .update("participants", participants)
                    .addOnSuccessListener {
                        isHost = false
                        listenToRoom(code)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Failed to join: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error finding room: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun listenToRoom(code: String) {
        activeRoomId = code
        cvRoomSetup.visibility = View.GONE
        cvActiveRoom.visibility = View.VISIBLE
        tvRoomId.text = "Room: $code"

        roomListener = firestore.collection("focus_rooms").document(code)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(requireContext(), "Sync error: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    updateRoomUi(snapshot)
                } else {
                    Toast.makeText(requireContext(), "Room was closed by host", Toast.LENGTH_SHORT).show()
                    resetSetupUi()
                }
            }
    }

    private fun updateRoomUi(snapshot: DocumentSnapshot) {
        val status = snapshot.getString("status") ?: "WAITING"
        val rawParticipants = snapshot.get("participants") as? Map<*, *> ?: emptyMap<Any, Any>()
        
        tvRoomStatus.text = when (status) {
            "WAITING" -> "Waiting for host to start..."
            "RUNNING" -> "Deep Focus Session Active!"
            "COMPLETED" -> "Session Completed! 🎉"
            else -> "Status: $status"
        }

        btnHostAction.isVisible = isHost && status == "WAITING"

        // Setup participants recycler
        val participantsList = rawParticipants.entries.mapNotNull { entry ->
            val pId = entry.key as? String ?: return@mapNotNull null
            val valueMap = entry.value as? Map<*, *> ?: return@mapNotNull null
            val name = valueMap["name"] as? String ?: "Zen Mind"
            val pStatus = valueMap["status"] as? String ?: "READY"
            Participant(pId, name, pStatus)
        }
        rvParticipants.adapter = ParticipantAdapter(participantsList)

        if (status == "RUNNING" && localTimer == null) {
            val startTime = snapshot.getLong("startTime") ?: System.currentTimeMillis()
            val duration = snapshot.getLong("durationMinutes") ?: 25L
            val elapsedMs = System.currentTimeMillis() - startTime
            val totalMs = duration * 60 * 1000L
            val remainMs = totalMs - elapsedMs

            if (remainMs > 0) {
                startLocalTimer(remainMs)
                startAppMonitoring()
            } else {
                tvRoomTimer.text = "00:00"
                tvRoomStatus.text = "Completed!"
            }
        }
    }

    private fun startRoomTimer() {
        val code = activeRoomId ?: return
        firestore.collection("focus_rooms").document(code)
            .update(
                "status", "RUNNING",
                "startTime", System.currentTimeMillis()
            )
    }

    private fun startLocalTimer(durationMs: Long) {
        localTimer?.cancel()
        localTimer = object : CountDownTimer(durationMs, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val totalSecs = millisUntilFinished / 1000
                val mins = totalSecs / 60
                val secs = totalSecs % 60
                tvRoomTimer.text = String.format("%02d:%02d", mins, secs)
            }

            override fun onFinish() {
                tvRoomTimer.text = "00:00"
                tvRoomStatus.text = "Session Completed! 🎉"
                appMonitorJob?.cancel()
                updateMyStatus("COMPLETED")
                Toast.makeText(requireContext(), "Co-Focus Session Complete! Great job!", Toast.LENGTH_LONG).show()
            }
        }.start()

        updateMyStatus("FOCUSING")
    }

    private fun startAppMonitoring() {
        appMonitorJob?.cancel()
        appMonitorJob = lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(3000L)
                if (UsageStatsHelper.hasUsageStatsPermission(requireContext())) {
                    val fgApp = UsageStatsHelper.getForegroundApp(requireContext())
                    if (fgApp != null && fgApp != requireContext().packageName) {
                        val isDistracting = UsageStatsHelper.isDistractingApp(fgApp)
                        if (isDistracting) {
                            withContext(Dispatchers.Main) {
                                updateMyStatus("DISTRACTED ⚠️")
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                updateMyStatus("FOCUSING 🧘‍♂️")
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            updateMyStatus("FOCUSING 🧘‍♂️")
                        }
                    }
                }
            }
        }
    }

    private fun updateMyStatus(newStatus: String) {
        val code = activeRoomId ?: return
        val uid = auth.currentUser?.uid ?: return
        
        firestore.collection("focus_rooms").document(code).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) return@addOnSuccessListener
                val rawParticipants = snapshot.get("participants") as? Map<String, Any>
                val participants = if (rawParticipants != null) HashMap(rawParticipants) else hashMapOf<String, Any>()
                
                val rawMe = participants[uid] as? Map<String, Any>
                val me = if (rawMe != null) HashMap(rawMe) else hashMapOf<String, Any>()
                me["status"] = newStatus
                participants[uid] = me
                
                firestore.collection("focus_rooms").document(code).update("participants", participants)
            }
    }

    private fun leaveRoom() {
        val code = activeRoomId ?: return
        val uid = auth.currentUser?.uid ?: return

        roomListener?.remove()
        localTimer?.cancel()
        appMonitorJob?.cancel()

        if (isHost) {
            // Close the room entirely
            firestore.collection("focus_rooms").document(code)
                .delete()
                .addOnCompleteListener {
                    resetSetupUi()
                }
        } else {
            // Remove user from participants list
            firestore.collection("focus_rooms").document(code).get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.exists()) {
                        resetSetupUi()
                        return@addOnSuccessListener
                    }
                    val rawParticipants = snapshot.get("participants") as? Map<String, Any>
                    val participants = if (rawParticipants != null) HashMap(rawParticipants) else hashMapOf<String, Any>()
                    participants.remove(uid)
                    firestore.collection("focus_rooms").document(code)
                        .update("participants", participants)
                        .addOnCompleteListener {
                            resetSetupUi()
                        }
                }
                .addOnFailureListener {
                    resetSetupUi()
                }
        }
    }

    private fun resetSetupUi() {
        activeRoomId = null
        isHost = false
        roomListener = null
        localTimer = null
        appMonitorJob = null
        
        cvActiveRoom.visibility = View.GONE
        cvRoomSetup.visibility = View.VISIBLE
        etRoomCode.setText("")
        tvRoomTimer.text = "25:00"
    }

    override fun onDestroyView() {
        roomListener?.remove()
        localTimer?.cancel()
        appMonitorJob?.cancel()
        super.onDestroyView()
    }

    data class Participant(
        val id: String,
        val name: String,
        val status: String
    )

    class ParticipantAdapter(private val items: List<Participant>) :
        RecyclerView.Adapter<ParticipantAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val avatar: TextView = view.findViewById(R.id.tv_participant_avatar)
            val name: TextView = view.findViewById(R.id.tv_participant_name)
            val desc: TextView = view.findViewById(R.id.tv_participant_status_desc)
            val badge: TextView = view.findViewById(R.id.tv_participant_badge)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_participant, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val initial = if (item.name.isNotEmpty()) item.name.first().uppercase() else "Z"
            holder.avatar.text = initial
            holder.name.text = item.name
            holder.desc.text = "Status: ${item.status}"

            holder.badge.text = item.status
            when {
                item.status.contains("DISTRACTED") -> {
                    holder.badge.setBackgroundColor(Color.parseColor("#FEE2E2"))
                    holder.badge.setTextColor(Color.parseColor("#EF4444"))
                }
                item.status.contains("FOCUSING") -> {
                    holder.badge.setBackgroundColor(Color.parseColor("#CCFBF1"))
                    holder.badge.setTextColor(Color.parseColor("#0D9488"))
                }
                item.status == "COMPLETED" -> {
                    holder.badge.setBackgroundColor(Color.parseColor("#DCFCE7"))
                    holder.badge.setTextColor(Color.parseColor("#16A34A"))
                }
                else -> {
                    holder.badge.setBackgroundColor(Color.parseColor("#F3F4F6"))
                    holder.badge.setTextColor(Color.parseColor("#4B5563"))
                }
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
