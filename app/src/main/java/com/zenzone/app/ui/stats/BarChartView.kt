package com.zenzone.app.ui.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class BarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val data = mutableMapOf<String, Int>()
    private val labels = mutableListOf<String>()
    
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2A9D8F") // zen_teal_primary
    }
    
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B6E65") // zen_teal_dark
    }
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6B7280") // zen_gray_text
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#264653") // zen_slate_dark
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }
    
    fun setData(newData: Map<String, Int>, newLabels: List<String>) {
        data.clear()
        data.putAll(newData)
        labels.clear()
        labels.addAll(newLabels)
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (labels.isEmpty()) return
        
        val maxVal = data.values.maxOrNull()?.coerceAtLeast(10) ?: 10
        val widthPerBar = width / labels.size.toFloat()
        
        val paddingBottom = 60f
        val paddingTop = 40f
        val maxBarHeight = height - paddingBottom - paddingTop
        
        labels.forEachIndexed { index, label ->
            val value = data[label] ?: 0
            val barHeight = (value.toFloat() / maxVal.toFloat()) * maxBarHeight
            
            val left = index * widthPerBar + widthPerBar * 0.2f
            val right = left + widthPerBar * 0.6f
            val bottom = height - paddingBottom
            val top = bottom - barHeight
            
            val rect = RectF(left, top, right, bottom)
            
            val isToday = index == labels.size - 1
            val paint = if (isToday) highlightPaint else barPaint
            
            canvas.drawRoundRect(rect, 8f, 8f, paint)
            
            canvas.drawText(label, left + (right - left) / 2, height - 10f, textPaint)
            
            if (value > 0) {
                canvas.drawText(value.toString(), left + (right - left) / 2, top - 10f, valuePaint)
            }
        }
    }
}
