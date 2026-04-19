package com.zenzone.app.ui.stats

import android.graphics.Color
import android.os.Bundle
import android.view.View
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
                }
            }

            viewModel.weeklyMinutesMap.observe(viewLifecycleOwner) { map ->
                map?.let {
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
                val bottomNav = activity?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_nav)
                bottomNav?.selectedItemId = R.id.nav_profile
            }

            viewModel.loadStats()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showMilestoneInstructions() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Zen Milestones")
            .setMessage("Level up your Zen status by accumulating focus hours:\n\n" +
                        "• LVL 1: Novice Monk (0h+)\n" +
                        "• LVL 2: Calm Keeper (5h+)\n" +
                        "• LVL 3: Focused Warrior (15h+)\n" +
                        "• LVL 4: Seasoned Practitioner (40h+)\n" +
                        "• LVL 5: Deep Diver (100h+)\n" +
                        "• LVL 6: Zen Master (250h+)\n" +
                        "• LVL 7: Enlightened One (500h+)\n\n" +
                        "Keep focusing to reach the next level!")
            .setPositiveButton("Got it", null)
            .show()
    }

    private fun showStatsInstructions() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("📊 Stats & Progress")
            .setMessage(
                "Track your focus journey:\n\n" +
                "📈 Weekly Insights\n" +
                "See your total focus hours for the past 7 days and identify your peak focus days.\n\n" +
                "🎯 Milestone Progress\n" +
                "Track your progress toward the next Zen level. Each level requires more focused hours.\n\n" +
                "🔥 Day Streak\n" +
                "Your current consecutive days of maintaining focus. Keep the streak alive!\n\n" +
                "📅 Bar Chart\n" +
                "Visual representation of your daily focus minutes over the past week.\n\n" +
                "📝 Session History\n" +
                "Recent focus sessions with duration and efficiency ratings."
            )
            .setPositiveButton("Got it", null)
            .show()
    }

    private fun setupBarChart() {
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
                textColor = Color.parseColor("#6B7280")
                textSize = 10f
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E0E0E0")
                textColor = Color.parseColor("#6B7280")
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
        
        val dataSet = BarDataSet(entries, "Minutes").apply {
            color = Color.parseColor("#2A9D8F")
            valueTextColor = Color.parseColor("#264653")
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
}
