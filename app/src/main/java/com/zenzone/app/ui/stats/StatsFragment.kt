package com.zenzone.app.ui.stats

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zenzone.app.R
import com.zenzone.app.viewmodel.StatsViewModel
import com.zenzone.app.ui.main.MainActivity
import java.text.SimpleDateFormat
import java.util.*

class StatsFragment : Fragment(R.layout.fragment_stats) {

    private val viewModel: StatsViewModel by viewModels()
    private lateinit var rvHistory: RecyclerView
    private lateinit var adapter: SessionHistoryAdapter
    private lateinit var emptyState: View
    private lateinit var progressBar: ProgressBar
    private lateinit var barChart: BarChart

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            val tvWeeklyHours: TextView = view.findViewById(R.id.tv_weekly_hours)
            val tvWeeklySummary: TextView = view.findViewById(R.id.tv_weekly_summary)
            barChart = view.findViewById(R.id.bar_chart_view)
            val tvMilestoneText: TextView = view.findViewById(R.id.tv_milestone_text)
            val progressMilestone: ProgressBar = view.findViewById(R.id.progress_milestone)
            val tvCurrentStreak: TextView = view.findViewById(R.id.tv_current_streak)
            val btnViewAll: TextView = view.findViewById(R.id.btn_view_all)
            val ivCommonInfo: View = view.findViewById(R.id.iv_common_info)
            val cvCommonProfile: View = view.findViewById(R.id.cv_common_profile_mini)
            val tvCommonInitial = view.findViewById<TextView>(R.id.tv_common_profile_initial_mini)
            val ivCommonProfileImage = view.findViewById<ImageView>(R.id.iv_common_profile_image_mini)
            view.findViewById<View>(R.id.iv_common_menu)?.setOnClickListener {
                (activity as? MainActivity)?.openDrawer()
            }
            view.findViewById<View>(R.id.iv_common_agent)?.setOnClickListener {
                com.zenzone.app.ui.social.ZenAgentDialog.show(requireContext(), parentFragmentManager, activity as? MainActivity)
            }
            val btnMilestoneInfo: View = view.findViewById(R.id.btn_milestone_info)
            
            rvHistory = view.findViewById(R.id.rv_session_history)
            emptyState = view.findViewById(R.id.empty_state_history)
            progressBar = view.findViewById(R.id.progress_bar)

            // Initialize chart
            setupBarChart()

            adapter = SessionHistoryAdapter(emptyList())
            rvHistory.layoutManager = LinearLayoutManager(requireContext())
            rvHistory.adapter = adapter

            viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }

            viewModel.profile.observe(viewLifecycleOwner) { profile ->
                profile?.let {
                    val displayName = it.userName.ifBlank { "ZenZone" }
                    view.findViewById<TextView>(R.id.tv_app_logo_name)?.text = "🧘 $displayName"

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
                            } else {
                                ivCommonProfileImage.visibility = View.GONE
                                tvCommonInitial.visibility = View.VISIBLE
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
                    } else {
                        ivCommonProfileImage.visibility = View.GONE
                        tvCommonInitial.visibility = View.VISIBLE
                    }

                    tvCurrentStreak.text = it.currentChain.toString()
                    
                    val totalHours = it.totalFocusedMinutes / 60.0
                    
                    // Milestone thresholds in hours
                    val milestones = listOf(0, 5, 15, 40, 100, 250, 500)
                    val milestoneNames = listOf(
                        "Novice Monk",
                        "Calm Keeper", 
                        "Focused Warrior",
                        "Seasoned Practitioner",
                        "Deep Diver",
                        "Zen Master",
                        "Enlightened One"
                    )
                    
                    // Find current level
                    var level = 1
                    for (i in 1 until milestones.size) {
                        if (totalHours >= milestones[i]) {
                            level = i + 1
                        } else {
                            break
                        }
                    }
                    
                    if (level < milestones.size) {
                        val currentLevelMin = milestones[level - 1].toDouble()
                        val nextLevelMin = milestones[level].toDouble()
                        
                        val hoursToNext = nextLevelMin - totalHours
                        val progressInRange = totalHours - currentLevelMin
                        val rangeSize = nextLevelMin - currentLevelMin
                        val progressPercent = ((progressInRange / rangeSize) * 100).coerceIn(0.0, 100.0).toInt()
                        
                        progressMilestone.progress = progressPercent
                        tvMilestoneText.text = String.format(
                            "You're just %.1f hours away from the '%s' badge.",
                            hoursToNext,
                            milestoneNames[level]
                        )
                    } else {
                        progressMilestone.progress = 100
                        tvMilestoneText.text = "Congratulations! You've reached the highest level: ${milestoneNames.last()}!"
                    }
                }
            }

            viewModel.sessions.observe(viewLifecycleOwner) { sessions ->
                sessions?.let {
                    val recentSessions = it.take(3)
                    adapter.updateData(recentSessions)
                    
                    if (it.isEmpty()) {
                        emptyState.visibility = View.VISIBLE
                        rvHistory.visibility = View.GONE
                        btnViewAll.visibility = View.GONE
                    } else {
                        emptyState.visibility = View.GONE
                        rvHistory.visibility = View.VISIBLE
                        btnViewAll.visibility = if (it.size > 3) View.VISIBLE else View.GONE
                    }

                    // Update Peak Hours and Forecasting
                    updatePeakHoursChart(it)
                    updateForecasting(it)
                }
            }

            viewModel.weeklyMinutesMap.observe(viewLifecycleOwner) { map ->
                map?.let {
                    // Update heatmap view
                    view.findViewById<HeatmapView>(R.id.heatmap_view)?.setData(it)

                    val calendar = Calendar.getInstance()
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    var weeklyTotal = 0
                    var peakDay = ""
                    var peakMinutes = 0
                    
                    val last7Days = mutableListOf<String>()
                    for (i in 6 downTo 0) {
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.DAY_OF_YEAR, -i)
                        last7Days.add(dateFormat.format(cal.time))
                    }
                    
                    for (dateStr in last7Days) {
                        val minutes = it[dateStr] ?: 0
                        weeklyTotal += minutes
                        
                        if (minutes > peakMinutes) {
                            peakMinutes = minutes
                            val cal = Calendar.getInstance()
                            cal.time = dateFormat.parse(dateStr) ?: Date()
                            peakDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(cal.time)
                        }
                    }
                    
                    val weeklyHours = weeklyTotal / 60.0
                    tvWeeklyHours.text = String.format("%.1f", weeklyHours)
                    
                    val targetHours = 15.0
                    val percentage = ((weeklyHours / targetHours) * 100).coerceAtMost(100.0).toInt()
                    
                    if (peakDay.isNotEmpty() && weeklyTotal > 0) {
                        tvWeeklySummary.text = String.format(
                            "You've reached %d%% of your weekly focus target. Your peak focus was on %s.",
                            percentage,
                            peakDay
                        )
                    } else {
                        tvWeeklySummary.text = "Start your focus journey this week!"
                    }
                    
                    updateBarChart(it, last7Days)
                }
            }

            btnViewAll.setOnClickListener {
                android.widget.Toast.makeText(requireContext(), "Full history coming soon!", android.widget.Toast.LENGTH_SHORT).show()
            }

            ivCommonInfo.setOnClickListener {
                showStatsInstructions()
            }

            btnMilestoneInfo.setOnClickListener {
                showMilestoneInstructions()
            }

            cvCommonProfile.setOnClickListener {
                (activity as? MainActivity)?.navigateToMenuItem(R.id.nav_profile)
            }

            viewModel.loadStats()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadStats()
    }

    private fun showMilestoneInstructions() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_milestone_instructions, null)
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

    private fun showStatsInstructions() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_stats_instructions, null)
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

    private fun setupBarChart() {
        val prefs = requireContext().getSharedPreferences(com.zenzone.app.utils.Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val isAmoled = prefs.getBoolean("pref_amoled_theme", false)
        val labelColor = if (isAmoled) Color.parseColor("#A0A0A0") else Color.parseColor("#6B7280")
        val gridColor = if (isAmoled) Color.parseColor("#222222") else Color.parseColor("#E0E0E0")

        barChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            setMaxVisibleValueCount(7)
            setPinchZoom(false)
            setScaleEnabled(false)
            setDrawBorders(false)
            legend.isEnabled = false
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = labelColor
                textSize = 10f
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                this.gridColor = gridColor
                textColor = labelColor
                textSize = 10f
                axisMinimum = 0f
                setDrawAxisLine(false)
            }
            
            axisRight.isEnabled = false
            animateY(800)
        }
    }

    private fun updateBarChart(dataMap: Map<String, Int>, labels: List<String>) {
        val entries = mutableListOf<BarEntry>()
        val dayLabels = mutableListOf<String>()
        
        labels.forEachIndexed { index, dateStr ->
            val minutes = dataMap[dateStr] ?: 0
            entries.add(BarEntry(index.toFloat(), minutes.toFloat()))
            
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = dateFormat.parse(dateStr)
                val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
                dayLabels.add(dayFormat.format(date ?: Date()))
            } catch (e: Exception) {
                dayLabels.add("")
            }
        }
        
        val prefs = requireContext().getSharedPreferences(com.zenzone.app.utils.Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val isAmoled = prefs.getBoolean("pref_amoled_theme", false)
        val barColor = if (isAmoled) Color.parseColor("#4DB6AC") else Color.parseColor("#2A9D8F")
        val valueColor = if (isAmoled) Color.WHITE else Color.parseColor("#264653")

        val dataSet = BarDataSet(entries, "Minutes").apply {
            color = barColor
            valueTextColor = valueColor
            valueTextSize = 10f
            setDrawValues(true)
        }
        
        val barData = BarData(dataSet).apply {
            barWidth = 0.7f
        }
        
        barChart.apply {
            data = barData
            xAxis.valueFormatter = IndexAxisValueFormatter(dayLabels)
            xAxis.labelCount = dayLabels.size
            invalidate()
        }
    }

    private fun updatePeakHoursChart(sessions: List<com.zenzone.app.model.FocusSession>) {
        val peakHoursChart = view?.findViewById<BarChart>(R.id.bar_chart_peak_hours) ?: return
        
        // Group sessions by hour of day
        val hourCounts = IntArray(24) { 0 }
        for (session in sessions) {
            val hour = com.zenzone.app.utils.DateUtils.getHourFromTimestamp(session.completedAt)
            if (hour in 0..23) {
                hourCounts[hour]++
            }
        }
        
        val entries = mutableListOf<BarEntry>()
        for (hour in 0..23) {
            entries.add(BarEntry(hour.toFloat(), hourCounts[hour].toFloat()))
        }
        
        // Apply theme color
        val prefs = requireContext().getSharedPreferences(com.zenzone.app.utils.Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val isAmoled = prefs.getBoolean("pref_amoled_theme", false)
        val barColor = if (isAmoled) Color.parseColor("#4DB6AC") else Color.parseColor("#2A9D8F")
        val textColor = if (isAmoled) Color.WHITE else Color.parseColor("#264653")
        val labelColor = if (isAmoled) Color.parseColor("#A0A0A0") else Color.parseColor("#6B7280")
        
        val dataSet = BarDataSet(entries, "Sessions").apply {
            color = barColor
            valueTextColor = textColor
            valueTextSize = 8f
            setDrawValues(false)
        }
        
        val barData = BarData(dataSet).apply {
            barWidth = 0.6f
        }
        
        peakHoursChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            setPinchZoom(false)
            setScaleEnabled(false)
            setDrawBorders(false)
            legend.isEnabled = false
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                this.textColor = labelColor
                textSize = 9f
                valueFormatter = IndexAxisValueFormatter(List(24) { "${it}h" })
                labelCount = 12
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = if (isAmoled) Color.parseColor("#222222") else Color.parseColor("#E0E0E0")
                this.textColor = labelColor
                textSize = 9f
                axisMinimum = 0f
                setDrawAxisLine(false)
            }
            
            axisRight.isEnabled = false
            data = barData
            invalidate()
        }
    }

    private fun updateForecasting(sessions: List<com.zenzone.app.model.FocusSession>) {
        val tvForecast = view?.findViewById<TextView>(R.id.tv_goals_forecast) ?: return
        
        if (sessions.isEmpty()) {
            tvForecast.text = "Start focusing to see your goals projection. Complete sessions to build your daily XP pace!"
            return
        }

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Start of this week (Monday)
        val calMonday = Calendar.getInstance()
        val dayOfWeek = calMonday.get(Calendar.DAY_OF_WEEK)
        val daysToSubtract = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        calMonday.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
        calMonday.set(Calendar.HOUR_OF_DAY, 0)
        calMonday.set(Calendar.MINUTE, 0)
        calMonday.set(Calendar.SECOND, 0)
        calMonday.set(Calendar.MILLISECOND, 0)
        
        val mondayDate = calMonday.time

        // 14 days ago for historical pace
        val cal14DaysAgo = Calendar.getInstance()
        cal14DaysAgo.add(Calendar.DAY_OF_YEAR, -14)
        val date14DaysAgo = cal14DaysAgo.time

        var currentWeekXP = 0
        var historicalXP = 0
        val sessionDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

        for (session in sessions) {
            val sessionDate = try {
                sessionDateFormat.parse(session.completedAt) ?: Date()
            } catch (e: Exception) {
                Date()
            }

            val xp = session.durationMinutes * 2 + (if (session.wasChainSaved) 10 else 0)

            if (!sessionDate.before(mondayDate)) {
                currentWeekXP += xp
            }
            if (!sessionDate.before(date14DaysAgo)) {
                historicalXP += xp
            }
        }

        val dailyPace = historicalXP / 14.0
        val todayNormalDayOfWeek = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1
        val daysRemaining = 7 - todayNormalDayOfWeek
        
        val projectedXP = currentWeekXP + (dailyPace * daysRemaining)
        val targetXP = 300 // A reasonable weekly XP target for users

        if (dailyPace == 0.0) {
            tvForecast.text = "Start focusing to see your goals projection. Complete sessions to build your daily XP pace!"
        } else {
            val paceStr = String.format("%.1f", dailyPace)
            val projectedStr = projectedXP.toInt().toString()
            if (projectedXP >= targetXP) {
                tvForecast.text = "Based on your pace of $paceStr XP/day, you're projected to hit $projectedStr XP this week (Target: $targetXP XP). You are on track! 🎉"
            } else {
                tvForecast.text = "Based on your pace of $paceStr XP/day, you're projected to hit $projectedStr XP this week (Target: $targetXP XP). Try adding a short focus session today to stay on track! 🚀"
            }
        }
    }
}
