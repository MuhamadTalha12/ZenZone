package com.zenzone.app.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zenzone.app.R

data class OnboardingSlide(val imageRes: Int, val title: String, val subtitle: String)

class OnboardingAdapter(
    private val slides: List<OnboardingSlide>
) : RecyclerView.Adapter<OnboardingAdapter.SlideViewHolder>() {

    inner class SlideViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.iv_onboarding_img)
        val tvTitle: TextView = view.findViewById(R.id.tv_onboarding_title)
        val tvSubtitle: TextView = view.findViewById(R.id.tv_onboarding_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_onboarding_slide, parent, false)
        return SlideViewHolder(v)
    }

    override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
        val slide = slides[position]
        holder.ivIcon.setImageResource(slide.imageRes)
        holder.tvTitle.text = slide.title
        holder.tvSubtitle.text = slide.subtitle
    }

    override fun getItemCount() = slides.size
}
