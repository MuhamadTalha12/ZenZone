package com.zenzone.app.ui.stats

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zenzone.app.R
import com.zenzone.app.viewmodel.StatsViewModel

class StatsFragment : Fragment(R.layout.fragment_stats) {

    private val viewModel: StatsViewModel by viewModels()
    private lateinit var rvHistory: RecyclerView
    private lateinit var adapter: SessionHistoryAdapter
    private lateinit var emptyState: View
    private lateinit var tvHistoryCount: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTotalSessions: TextView = view.findViewById(R.id.tv_total_sessions)
        val tvTotalHours: TextView = view.findViewById(R.id.tv_total_hours)
        val tvBestChain: TextView = view.findViewById(R.id.tv_best_chain)
        val barChartView: BarChartView = view.findViewById(R.id.bar_chart_view)
        rvHistory = view.findViewById(R.id.rv_session_history)
        emptyState = view.findViewById(R.id.empty_state_history)
        tvHistoryCount = view.findViewById(R.id.tv_history_count)

        adapter = SessionHistoryAdapter(emptyList())
        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        rvHistory.adapter = adapter

        viewModel.profile.observe(viewLifecycleOwner) { profile ->
            tvTotalSessions.text = profile.totalSessions.toString()
            val totalHoursFormated = String.format("%.1f", profile.totalFocusedMinutes / 60.0)
            tvTotalHours.text = totalHoursFormated
            tvBestChain.text = profile.longestEverChain.toString()
        }

        viewModel.sessions.observe(viewLifecycleOwner) { sessions ->
            adapter.updateData(sessions)
            tvHistoryCount.text = sessions.size.toString()
            if (sessions.isEmpty()) {
                emptyState.visibility = View.VISIBLE
                rvHistory.visibility = View.GONE
            } else {
                emptyState.visibility = View.GONE
                rvHistory.visibility = View.VISIBLE
            }
        }

        viewModel.weeklyMinutesMap.observe(viewLifecycleOwner) { map ->
            val labels = map.keys.sorted().takeLast(7)
            val dataMap = map.filterKeys { labels.contains(it) }
            barChartView.setData(dataMap, labels)
        }

        viewModel.loadStats()
    }
}
