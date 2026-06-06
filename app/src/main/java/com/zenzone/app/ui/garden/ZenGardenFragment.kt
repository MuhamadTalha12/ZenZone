package com.zenzone.app.ui.garden

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zenzone.app.R
import com.zenzone.app.repository.UserRepository
import com.zenzone.app.viewmodel.HomeViewModel
import com.zenzone.app.viewmodel.HomeViewModelFactory
import com.zenzone.app.repository.FocusRepository
import com.zenzone.app.ui.main.MainActivity

class ZenGardenFragment : Fragment(R.layout.fragment_zen_garden) {

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(
            FocusRepository(requireContext()),
            UserRepository(requireContext())
        )
    }

    private lateinit var rvPlants: RecyclerView
    private lateinit var tvGardenTitle: TextView
    private lateinit var tvGardenDescription: TextView
    private lateinit var btnBack: ImageButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvPlants = view.findViewById(R.id.rv_plants)
        tvGardenTitle = view.findViewById(R.id.tv_garden_title)
        tvGardenDescription = view.findViewById(R.id.tv_garden_description)
        btnBack = view.findViewById(R.id.btn_back)

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Common Navbar Views
        val ivCommonInfo = view.findViewById<ImageView>(R.id.iv_common_info)
        val cvCommonProfile = view.findViewById<View>(R.id.cv_common_profile_mini)
        val tvCommonInitial = view.findViewById<TextView>(R.id.tv_common_profile_initial_mini)
        val ivCommonProfileImage = view.findViewById<ImageView>(R.id.iv_common_profile_image_mini)
        view.findViewById<View>(R.id.iv_common_menu)?.setOnClickListener {
            (activity as? MainActivity)?.openDrawer()
        }
        view.findViewById<View>(R.id.iv_common_agent)?.setOnClickListener {
            com.zenzone.app.ui.social.ZenAgentDialog.show(requireContext(), parentFragmentManager, activity as? MainActivity)
        }

        rvPlants.layoutManager = GridLayoutManager(requireContext(), 2)

        val tvPetEmoji = view.findViewById<TextView>(R.id.tv_pet_emoji)
        val tvPetName = view.findViewById<TextView>(R.id.tv_pet_name)
        val tvPetStatus = view.findViewById<TextView>(R.id.tv_pet_status)
        val progressPetXp = view.findViewById<ProgressBar>(R.id.progress_pet_xp)
        val btnChoosePet = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_choose_pet)

        btnChoosePet.setOnClickListener {
            val pets = arrayOf("Cat 🐱", "Fox 🦊", "Turtle 🐢")
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Adopt a Zen Pet")
                .setItems(pets) { _, which ->
                    val chosen = when (which) {
                        0 -> "Cat"
                        1 -> "Fox"
                        2 -> "Turtle"
                        else -> "None"
                    }
                    viewModel.userProfile.value?.let { profile ->
                        val updated = profile.copy(zenPetType = chosen)
                        viewModel.updateUserProfile(updated)
                    }
                }
                .show()
        }

        viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            profile?.let {
                val displayName = it.userName.ifBlank { "ZenZone" }
                view.findViewById<TextView>(R.id.tv_app_logo_name)?.text = "🧘 $displayName"

                // Set mini profile
                val initial = if (it.userName.isNotEmpty()) {
                    it.userName.first().uppercaseChar().toString()
                } else {
                    "Z"
                }
                tvCommonInitial.text = initial
                
                if (!it.profileImageUri.isNullOrBlank()) {
                    if (com.zenzone.app.utils.ImageUtils.isBase64Image(it.profileImageUri)) {
                        val bitmap = com.zenzone.app.utils.ImageUtils.base64ToBitmap(it.profileImageUri)
                        if (bitmap != null) {
                            ivCommonProfileImage.setImageBitmap(bitmap)
                            ivCommonProfileImage.visibility = View.VISIBLE
                            tvCommonInitial.visibility = View.GONE
                        }
                    } else {
                        try {
                            val uri = android.net.Uri.parse(it.profileImageUri)
                            ivCommonProfileImage.setImageURI(uri)
                            ivCommonProfileImage.visibility = View.VISIBLE
                            tvCommonInitial.visibility = View.GONE
                        } catch (e: Exception) {
                            ivCommonProfileImage.visibility = View.GONE
                            tvCommonInitial.visibility = View.VISIBLE
                        }
                    }
                }

                val xp = it.zenXP
                val streak = it.currentChain
                val isWithered = (streak == 0)

                // Zen Pet UI Update
                val petType = it.zenPetType
                if (petType == "None" || petType.isEmpty()) {
                    tvPetEmoji.text = "🐾"
                    tvPetName.text = "No Companion Pet"
                    tvPetStatus.text = "Adopt a companion pet below"
                    progressPetXp.progress = 0
                    btnChoosePet.text = "Adopt Zen Pet"
                } else {
                    val emoji = when (petType) {
                        "Cat" -> "🐱"
                        "Fox" -> "🦊"
                        "Turtle" -> "🐢"
                        else -> "🐾"
                    }
                    tvPetEmoji.text = emoji
                    tvPetName.text = "Zen $petType"
                    
                    val petLevel = it.zenLevel
                    val petStatusText = if (isWithered) "Sad / Curled Up 😢" else "Happy / Focused 🐾"
                    tvPetStatus.text = "Level $petLevel · $petStatusText"
                    
                    val petXp = it.zenXP % 100
                    progressPetXp.progress = petXp
                    btnChoosePet.text = "Change Pet"
                }

                // Render Garden Status
                val plantCount = xp / 100
                val gardenStageName = when {
                    plantCount == 0 -> "Seeds Bed 🕳"
                    plantCount <= 2 -> "Sprout Stage Garden 🌱"
                    plantCount <= 5 -> "Blossoming Garden 🌸"
                    plantCount <= 8 -> "Forest Sanctuary 🌳"
                    else -> "Grand Zen Paradise 🪷"
                }

                tvGardenTitle.text = gardenStageName

                val description = if (isWithered && plantCount > 0) {
                    "Your focus chain broke! 🥀 Your plants look slightly withered and dry. Complete a focus session today to revive them and restore their vibrant green color."
                } else if (plantCount == 0) {
                    "Your garden is currently empty. Complete your focus sessions and earn 100 XP to plant your very first sprout!"
                } else {
                    "Your garden is thriving! You have earned $xp XP and grown $plantCount healthy plants. Keep your focus chain active at $streak to keep them blooming."
                }
                tvGardenDescription.text = description

                // Generate list of plants
                val plants = mutableListOf<PlantItem>()
                val allPossiblePlants = mutableListOf(
                    PlantItem("Sprout", "🌱", "🍂", 100),
                    PlantItem("Baby Shoot", "🌿", "🌾", 200),
                    PlantItem("Wild Flower", "🌸", "🥀", 300),
                    PlantItem("Prickly Pear", "🌵", "🌵", 400),
                    PlantItem("Boston Fern", "🪴", "🍂", 500),
                    PlantItem("Golden Sunflower", "🌻", "🥀", 600),
                    PlantItem("Red Rose", "🌹", "🥀", 700),
                    PlantItem("Oak Sapling", "🌳", "🍂", 800),
                    PlantItem("Majestic Palm", "🌴", "🍂", 900),
                    PlantItem("Zen Lotus", "🪷", "🥀", 1000)
                )

                // Seasonal events check
                val nowCal = java.util.Calendar.getInstance()
                val nowMonth = nowCal.get(java.util.Calendar.MONTH) // 0-indexed
                val nowDay = nowCal.get(java.util.Calendar.DAY_OF_MONTH)
                
                val isHalloween = (nowMonth == 9 && nowDay >= 24) || (nowMonth == 10 && nowDay <= 5)
                val isChristmas = (nowMonth == 11 && nowDay >= 15 && nowDay <= 31)

                if (isHalloween) {
                    allPossiblePlants.add(PlantItem("Halloween Pumpkin", "🎃", "🥀", 150))
                }
                if (isChristmas) {
                    allPossiblePlants.add(PlantItem("Pine Tree", "🎄", "🍂", 250))
                }

                for (p in allPossiblePlants) {
                    if (xp >= p.requiredXp) {
                        plants.add(p.copy(status = if (isWithered) "Withered" else "Healthy"))
                    } else {
                        // Locked plant placeholder
                        plants.add(p.copy(name = "Locked", emoji = "🔒", witheredEmoji = "🔒", status = "Requires ${p.requiredXp} XP"))
                    }
                }

                rvPlants.adapter = PlantAdapter(plants, isWithered)
            }
        }

        cvCommonProfile.setOnClickListener {
            (activity as? MainActivity)?.navigateToMenuItem(R.id.nav_profile)
        }

        ivCommonInfo.setOnClickListener {
            showGardenInfoDialog()
        }

        viewModel.loadUserProfile()
    }

    private fun showGardenInfoDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_garden_instructions, null)
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)

        dialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundColor(
            androidx.core.content.ContextCompat.getColor(requireContext(), R.color.zen_slate_dark)
        )

        dialogView.findViewById<View>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    data class PlantItem(
        val name: String,
        val emoji: String,
        val witheredEmoji: String,
        val requiredXp: Int,
        val status: String = ""
    )

    class PlantAdapter(private val items: List<PlantItem>, private val isWithered: Boolean) :
        RecyclerView.Adapter<PlantAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val emoji: TextView = view.findViewById(R.id.tv_plant_emoji)
            val name: TextView = view.findViewById(R.id.tv_plant_name)
            val status: TextView = view.findViewById(R.id.tv_plant_status)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_plant, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val isLocked = item.name == "Locked"
            
            holder.emoji.text = if (isWithered && !isLocked) item.witheredEmoji else item.emoji
            holder.name.text = item.name
            holder.status.text = item.status

            if (isLocked) {
                holder.status.setTextColor(Color.parseColor("#9CA3AF"))
                holder.emoji.alpha = 0.4f
                holder.name.alpha = 0.5f
            } else if (isWithered) {
                holder.status.setTextColor(Color.parseColor("#B08968")) // Withered brown/gray status
                holder.status.text = "Withered 🥀"
                holder.itemView.alpha = 0.8f
            } else {
                holder.status.setTextColor(Color.parseColor("#2A9D8F")) // Zen teal healthy color
                holder.itemView.alpha = 1.0f
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
