package com.zenzone.app.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.zenzone.app.R
import com.zenzone.app.viewmodel.HomeViewModel

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var adapter: ZenCardAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rv_zen_cards)
        val fab = view.findViewById<FloatingActionButton>(R.id.fab_add_goal)
        val emptyState = view.findViewById<View>(R.id.empty_state_layout)

        adapter = ZenCardAdapter(emptyList()) {
            // Additional navigation on click can be added here
        }
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        viewModel.goals.observe(viewLifecycleOwner) { goals ->
            adapter.updateData(goals)
            if (goals.isEmpty()) {
                emptyState.visibility = View.VISIBLE
                rv.visibility = View.GONE
            } else {
                emptyState.visibility = View.GONE
                rv.visibility = View.VISIBLE
            }
        }

        fab.setOnClickListener {
            AddFocusDialogFragment { newGoal ->
                viewModel.addGoal(newGoal)
            }.show(childFragmentManager, "AddGoal")
        }

        viewModel.loadGoals()
    }
}
