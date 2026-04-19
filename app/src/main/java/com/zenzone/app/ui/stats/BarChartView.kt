package com.zenzone.app.ui.stats

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class BarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val data = mutableMapOf<String, Int>()
    private val labels = mutableListOf<String>()
    
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#A8DADB") // zen_teal_light
        style = Paint.Style.FILL
    }
    
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2A9D8F") // zen_teal_primary
        style = Paint.Style.FILL
    }
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6B7280") // zen_gray_text
        textSize = 28f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#264653") // zen_slate_dark
        textSize = 22f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0")
        strokeWidth = 1f
        style = Paint.Style.STROKE
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

        val maxVal = data.values.maxOrNull()?.coerceAtLeast(1) ?: 10
        val chartPaddingLeft = 20f
        val chartPaddingRight = 20f
        val paddingBottom = 60f
        val paddingTop = 40f

        val availableWidth = width - chartPaddingLeft - chartPaddingRight
        val widthPerBar = availableWidth / labels.size.toFloat()
        val maxBarHeight = height - paddingBottom - paddingTop

        // Draw baseline
        canvas.drawLine(chartPaddingLeft, height - paddingBottom, width - chartPaddingRight, height - paddingBottom, gridPaint)

        labels.forEachIndexed { index, label ->
            val value = data[label] ?: 0
            val barHeight = (value.toFloat() / maxVal.toFloat()) * maxBarHeight
            
            val barWidthRatio = 0.75f // Make bars wider for a histogram look
            val left = chartPaddingLeft + index * widthPerBar + widthPerBar * (1 - barWidthRatio) / 2
            val right = left + widthPerBar * barWidthRatio
            val bottom = height - paddingBottom
            val top = bottom - barHeight.coerceAtLeast(4f) // Minimum height for visibility
            
            val rect = RectF(left, top, right, bottom)
            
            // Highlight the last bar (usually today)
            val isToday = index == labels.size - 1
            val paint = if (isToday) highlightPaint else barPaint
            
            // Draw bar with rounded top corners only
            val cornerRadius = 12f
            val path = Path()
            path.addRoundRect(rect, floatArrayOf(cornerRadius, cornerRadius, cornerRadius, cornerRadius, 0f, 0f, 0f, 0f), Path.Direction.CW)
            canvas.drawPath(path, paint)
            
            // Draw label
            canvas.drawText(label, left + (right - left) / 2, height - 15f, textPaint)
            
            // Draw value above bar if it's significant or highlighted
            if (value > 0) {
                canvas.drawText(value.toString(), left + (right - left) / 2, top - 12f, valuePaint)
            }
        }
    }
}
