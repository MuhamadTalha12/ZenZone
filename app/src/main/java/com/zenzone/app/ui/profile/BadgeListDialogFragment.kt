package com.zenzone.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import androidx.recyclerview.widget.RecyclerView
import com.zenzone.app.R
import com.zenzone.app.model.ZenBadge

class BadgeListDialogFragment(
    private val badges: List<ZenBadge>
) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_badges_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Style the bottom sheet
        (dialog as? com.google.android.material.bottomsheet.BottomSheetDialog)?.let { bsd ->
            val sheet = bsd.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            sheet?.setBackgroundResource(R.drawable.bg_bottom_sheet)
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_badges_all)
        val btnClose = view.findViewById<MaterialButton>(R.id.btn_close_badges)

        val adapter = BadgeListAdapter(badges)
        recyclerView.adapter = adapter

        btnClose.setOnClickListener {
            dismiss()
        }
    }
}