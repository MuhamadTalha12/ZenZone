package com.zenzone.app.ui.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.zenzone.app.R
import com.zenzone.app.model.BlockedAppEntity
import com.zenzone.app.repository.AppDatabase
import com.zenzone.app.utils.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BlockedAppsDialogFragment : DialogFragment() {

    private lateinit var rvApps: RecyclerView
    private lateinit var etSearch: com.google.android.material.textfield.TextInputEditText
    private lateinit var adapter: AppInfoAdapter

    private var allAppsList = mutableListOf<AppDisplayInfo>()
    private var filteredList = mutableListOf<AppDisplayInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.ZenDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_blocked_apps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        rvApps = view.findViewById(R.id.rv_apps_list)
        etSearch = view.findViewById(R.id.et_search_apps)
        
        rvApps.layoutManager = LinearLayoutManager(requireContext())

        view.findViewById<View>(R.id.btn_cancel)?.setOnClickListener {
            dismiss()
        }

        view.findViewById<View>(R.id.btn_save)?.setOnClickListener {
            saveBlocklist()
        }

        loadApps()

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadApps() {
        lifecycleScope.launch(Dispatchers.IO) {
            val pm = requireContext().packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            
            // Get all launcher applications
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            
            // Get custom blocked settings from DB
            val db = AppDatabase.getDatabase(requireContext())
            val customBlockedList = db.blockedAppDao().getAllBlockedApps()

            val tempMap = customBlockedList.associateBy { it.packageName }
            
            val apps = resolveInfos.mapNotNull { info ->
                val packageName = info.activityInfo.packageName
                if (packageName == requireContext().packageName) return@mapNotNull null // Skip our own app
                
                val label = info.loadLabel(pm).toString()
                val icon = info.loadIcon(pm)
                
                val isBlocked = if (tempMap.containsKey(packageName)) {
                    tempMap[packageName]?.isBlocked ?: false
                } else {
                    // Fallback to defaults
                    UsageStatsHelper.isDistractingApp(packageName)
                }

                AppDisplayInfo(packageName, label, icon, isBlocked)
            }.distinctBy { it.packageName }.sortedBy { it.appName }

            withContext(Dispatchers.Main) {
                allAppsList.clear()
                allAppsList.addAll(apps)
                filteredList.clear()
                filteredList.addAll(apps)
                
                adapter = AppInfoAdapter(filteredList)
                rvApps.adapter = adapter
            }
        }
    }

    private fun filterApps(query: String) {
        filteredList.clear()
        if (query.isBlank()) {
            filteredList.addAll(allAppsList)
        } else {
            filteredList.addAll(allAppsList.filter {
                it.appName.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            })
        }
        adapter.notifyDataSetChanged()
    }

    private fun saveBlocklist() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val entities = allAppsList.map { info ->
                    BlockedAppEntity(info.packageName, info.appName, info.isBlocked)
                }
                db.blockedAppDao().insertBlockedApps(entities)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Blocklist updated successfully!", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    data class AppDisplayInfo(
        val packageName: String,
        val appName: String,
        val icon: Drawable,
        var isBlocked: Boolean
    )

    class AppInfoAdapter(private val items: List<AppDisplayInfo>) :
        RecyclerView.Adapter<AppInfoAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.iv_app_icon)
            val name: TextView = view.findViewById(R.id.tv_app_name)
            val toggle: SwitchMaterial = view.findViewById(R.id.switch_blocked)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_blocked_app, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.icon.setImageDrawable(item.icon)
            holder.name.text = item.appName
            
            // Remove listener before setting state to avoid recursion trigger
            holder.toggle.setOnCheckedChangeListener(null)
            holder.toggle.isChecked = item.isBlocked
            
            holder.toggle.setOnCheckedChangeListener { _, isChecked ->
                item.isBlocked = isChecked
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
