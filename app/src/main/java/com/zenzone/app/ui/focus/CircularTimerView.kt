package com.zenzone.app.ui.focus

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class CircularTimerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F0F4F8") // zen_slate_bg
        style = Paint.Style.STROKE
        strokeWidth = 24f
    }

    private val fgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2A9D8F") // zen_teal_primary
        style = Paint.Style.STROKE
        strokeWidth = 24f
        strokeCap = Paint.Cap.ROUND
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#402A9D8F") // translucent teal
        style = Paint.Style.STROKE
        strokeWidth = 40f
        maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#264653") // zen_slate_dark
        textSize = 100f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    }
    
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6B7280") // zen_gray_text
        textSize = 14f
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.2f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    private var progress = 1f // 0 to 1
    private var timeText = "00:00"
    private val rectF = RectF()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null) // Required for BlurMaskFilter
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val padding = 60f
        rectF.set(padding, padding, w - padding, h - padding)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw subtle background ring
        canvas.drawOval(rectF, bgPaint)
        
        // Draw subtle glow under progress
        canvas.drawArc(rectF, -90f, 360f * progress, false, glowPaint)
        
        // Draw progress arc
        canvas.drawArc(rectF, -90f, 360f * progress, false, fgPaint)

        // Draw time text with a cleaner look
        val textY = rectF.centerY() - (textPaint.descent() + textPaint.ascent()) / 2 - 10f
        canvas.drawText(timeText, rectF.centerX(), textY, textPaint)
        
        // Draw "REMAINING" label below
        val labelY = rectF.centerY() + 50f
        canvas.drawText("FOCUS TIME", rectF.centerX(), labelY, labelPaint)
    }

    fun update(remainingMs: Long, totalMs: Long) {
        progress = if (totalMs == 0L) 0f else remainingMs.toFloat() / totalMs.toFloat()
        val totalSecs = remainingMs / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        timeText = String.format("%02d:%02d", mins, secs)
        invalidate()
    }
}
