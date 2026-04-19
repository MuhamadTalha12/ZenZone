package com.zenzone.app.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.zenzone.app.R
import com.zenzone.app.ui.main.MainActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val ivLogo = findViewById<ImageView>(R.id.iv_logo)
        val tvTitle = findViewById<TextView>(R.id.tv_title)
        val tvSubtitle = findViewById<TextView>(R.id.tv_subtitle)

        val logoFade = AlphaAnimation(0f, 1f).apply { duration = 800 }
        val logoScale = ScaleAnimation(0.5f, 1.0f, 0.5f, 1.0f, 
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f).apply {
            duration = 800
        }
        
        ivLogo.startAnimation(logoFade)
        ivLogo.startAnimation(logoScale)

        val textFade = AlphaAnimation(0f, 1f).apply {
            duration = 400
            startOffset = 400
        }
        tvTitle.startAnimation(textFade)
        tvSubtitle.startAnimation(textFade)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, com.zenzone.app.utils.Constants.SPLASH_DELAY_MS)
    }
}
