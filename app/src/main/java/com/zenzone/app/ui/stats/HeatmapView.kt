package com.zenzone.app.ui.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.zenzone.app.utils.Constants
import java.text.SimpleDateFormat
import java.util.*

class HeatmapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var dataMap: Map<String, Int> = emptyMap()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // 53 columns represents 1 year of weeks
    private val numColumns = 53
    private val numRows = 7
    private var cellSize = 0f
    private val cellSpacing = 6f // Spacing in pixels
    private val cornerRadius = 4f // Rounded corner radius for cells

    // Colors for Light Theme
    private val lightColors = listOf(
        Color.parseColor("#E5E7EB"), // 0 min (light gray)
        Color.parseColor("#B2DFDB"), // 1-15 min (lightest teal)
        Color.parseColor("#80CBC4"), // 16-30 min (light teal)
        Color.parseColor("#26A69A"), // 31-60 min (medium teal)
        Color.parseColor("#00796B")  // >60 min (dark teal)
    )

    // Colors for AMOLED Theme
    private val amoledColors = listOf(
        Color.parseColor("#262626"), // 0 min (very dark gray)
        Color.parseColor("#004D40"), // 1-15 min (very dark teal)
        Color.parseColor("#00796B"), // 16-30 min (dark teal)
        Color.parseColor("#009688"), // 31-60 min (teal)
        Color.parseColor("#4DB6AC")  // >60 min (light teal)
    )

    init {
        minimumHeight = 120 // Set a default minimum height
    }

    fun setData(data: Map<String, Int>) {
        this.dataMap = data
        invalidate() // Redraw view with new data
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        
        // Calculate cell size based on available width
        val totalSpacingWidth = cellSpacing * (numColumns - 1)
        cellSize = (width - paddingLeft - paddingRight - totalSpacingWidth) / numColumns
        
        // Height is determined by the cell size for 7 rows
        val totalHeight = (cellSize * numRows) + (cellSpacing * (numRows - 1)) + paddingTop + paddingBottom
        
        setMeasuredDimension(width, totalHeight.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (cellSize <= 0) return

        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val isAmoled = prefs.getBoolean("pref_amoled_theme", false)
        val colors = if (isAmoled) amoledColors else lightColors

        val calendar = Calendar.getInstance()
        val todayDayOfWeekNormal = (calendar.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY + 7) % 7 // Sunday = 0

        val rect = RectF()

        for (col in 0 until numColumns) {
            for (row in 0 until numRows) {
                // Compute how many days before today this cell represents
                val daysBefore = (numColumns - 1 - col) * 7 + (todayDayOfWeekNormal - row)

                if (daysBefore < 0) {
                    // Future day in the last week column, skip drawing
                    continue
                }
                if (daysBefore >= 365) {
                    // Older than 1 year, skip drawing
                    continue
                }

                // Calculate date string for this cell
                val cellCal = Calendar.getInstance()
                cellCal.add(Calendar.DAY_OF_YEAR, -daysBefore)
                val dateStr = dateFormat.format(cellCal.time)

                val minutes = dataMap[dateStr] ?: 0
                val colorIndex = when {
                    minutes <= 0 -> 0
                    minutes <= 15 -> 1
                    minutes <= 30 -> 2
                    minutes <= 60 -> 3
                    else -> 4
                }

                cellPaint.color = colors[colorIndex]

                // Calculate drawing bounds for this cell
                val left = paddingLeft + col * (cellSize + cellSpacing)
                val top = paddingTop + row * (cellSize + cellSpacing)
                rect.set(left, top, left + cellSize, top + cellSize)

                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, cellPaint)
            }
        }
    }
}
