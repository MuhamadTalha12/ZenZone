package com.zenzone.app.ui.focus

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class CircularTimerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0") // gray background arc
        style = Paint.Style.STROKE
        strokeWidth = 20f
        strokeCap = Paint.Cap.ROUND
    }

    private val fgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2A9D8F") // zen_teal_primary
        style = Paint.Style.STROKE
        strokeWidth = 20f
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#264653") // zen_slate_dark
        textSize = 120f
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private var progress = 1f // 0 to 1
    private var timeText = "00:00"
    private val rectF = RectF()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val padding = 40f
        rectF.set(padding, padding, w - padding, h - padding)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawArc(rectF, -90f, 360f, false, bgPaint)
        canvas.drawArc(rectF, -90f, 360f * progress, false, fgPaint)

        // Draw text
        val textY = rectF.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(timeText, rectF.centerX(), textY, textPaint)
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
