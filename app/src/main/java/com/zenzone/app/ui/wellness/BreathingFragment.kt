package com.zenzone.app.ui.wellness

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieAnimationView
import com.zenzone.app.R

class BreathingFragment : Fragment(R.layout.fragment_breathing) {

    private lateinit var lottieBreathing: LottieAnimationView
    private lateinit var tvInstruction: TextView
    private lateinit var tvBreathingTimer: TextView
    
    private val handler = Handler(Looper.getMainLooper())
    private var cycleSeconds = 0
    private var totalSeconds = 0
    private val maxDurationSeconds = 120 // 2 minutes

    private val breathingRunnable = object : Runnable {
        override fun run() {
            if (totalSeconds >= maxDurationSeconds) {
                if (isAdded) {
                    parentFragmentManager.popBackStack()
                }
                return
            }

            val phase = (cycleSeconds % 16)
            val count = 4 - (phase % 4)

            when (phase) {
                in 0..3 -> {
                    tvInstruction.text = "Breathe In... 🌬️"
                    tvBreathingTimer.text = count.toString()
                    lottieBreathing.speed = 1.0f
                }
                in 4..7 -> {
                    tvInstruction.text = "Hold... 🧘"
                    tvBreathingTimer.text = count.toString()
                    lottieBreathing.speed = 0f
                }
                in 8..11 -> {
                    tvInstruction.text = "Breathe Out... 💨"
                    tvBreathingTimer.text = count.toString()
                    lottieBreathing.speed = -1.0f
                }
                in 12..15 -> {
                    tvInstruction.text = "Hold... 🧘"
                    tvBreathingTimer.text = count.toString()
                    lottieBreathing.speed = 0f
                }
            }

            cycleSeconds++
            totalSeconds++
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lottieBreathing = view.findViewById(R.id.lottie_breathing)
        tvInstruction = view.findViewById(R.id.tv_instruction)
        tvBreathingTimer = view.findViewById(R.id.tv_breathing_timer)

        view.findViewById<View>(R.id.btn_back)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Set Lottie animation URL
        lottieBreathing.setAnimationFromUrl("https://assets10.lottiefiles.com/packages/lf20_tuxbgzo5.json")
        lottieBreathing.playAnimation()

        // Start breathing timer loop
        handler.post(breathingRunnable)
    }

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroyView()
    }
}
