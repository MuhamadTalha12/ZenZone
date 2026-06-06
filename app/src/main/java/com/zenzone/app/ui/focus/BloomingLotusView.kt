package com.zenzone.app.ui.focus

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

class BloomingLotusView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val petalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E9C46A") // Warm yellow center
        style = Paint.Style.FILL
    }

    private var bloomProgress = 0f
    private val petalPath = Path()

    init {
        startAnimation()
    }

    fun startAnimation() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2200L
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                bloomProgress = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = Math.min(cx, cy) * 0.75f

        if (maxRadius <= 0f) return

        // 1. Draw outer background shadow / glow petals
        val outerPetalCount = 12
        for (i in 0 until outerPetalCount) {
            val angle = i * (360f / outerPetalCount)
            canvas.save()
            canvas.rotate(angle, cx, cy)

            val scaleX = bloomProgress * 0.6f
            val scaleY = bloomProgress * 0.9f

            petalPath.reset()
            petalPath.moveTo(cx, cy)
            petalPath.quadTo(cx - 40f * scaleX, cy - maxRadius * 0.6f * scaleY, cx, cy - maxRadius * scaleY)
            petalPath.quadTo(cx + 40f * scaleX, cy - maxRadius * 0.6f * scaleY, cx, cy)

            val gradient = RadialGradient(
                cx, cy - maxRadius * 0.5f * scaleY,
                maxRadius * scaleY,
                Color.parseColor("#4D2A9D8F"), // Soft teal glow
                Color.parseColor("#002A9D8F"),
                Shader.TileMode.CLAMP
            )
            petalPaint.shader = gradient
            petalPaint.alpha = (bloomProgress * 200).toInt()

            canvas.drawPath(petalPath, petalPaint)
            canvas.restore()
        }

        // 2. Draw inner vibrant blooming petals
        val innerPetalCount = 8
        for (i in 0 until innerPetalCount) {
            val angle = i * (360f / innerPetalCount) + 22.5f // Offset angle
            canvas.save()
            canvas.rotate(angle, cx, cy)

            val scaleX = bloomProgress * 0.7f
            val scaleY = bloomProgress * 0.8f

            petalPath.reset()
            petalPath.moveTo(cx, cy)
            petalPath.quadTo(cx - 30f * scaleX, cy - maxRadius * 0.5f * scaleY, cx, cy - maxRadius * 0.8f * scaleY)
            petalPath.quadTo(cx + 30f * scaleX, cy - maxRadius * 0.5f * scaleY, cx, cy)

            val gradient = RadialGradient(
                cx, cy - maxRadius * 0.4f * scaleY,
                maxRadius * 0.8f * scaleY,
                Color.parseColor("#E76F51"), // Rose coral
                Color.parseColor("#F4A261"), // Peach
                Shader.TileMode.CLAMP
            )
            petalPaint.shader = gradient
            petalPaint.alpha = (bloomProgress * 255).toInt()

            canvas.drawPath(petalPath, petalPaint)
            canvas.restore()
        }

        // 3. Draw flower center pistil
        val centerRadius = 24f * bloomProgress
        centerPaint.alpha = (bloomProgress * 255).toInt()
        canvas.drawCircle(cx, cy, centerRadius, centerPaint)
    }
}
