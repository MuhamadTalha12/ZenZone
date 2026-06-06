package com.zenzone.app.utils

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.widget.Toast
import androidx.core.content.FileProvider
import com.zenzone.app.model.UserProfile
import java.io.File
import java.io.FileOutputStream

object ShareCardHelper {

    fun shareStreakCard(context: Context, profile: UserProfile) {
        val width = 1000
        val height = 1000
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw Background Gradient (Slate dark palette)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            Color.parseColor("#111827"), Color.parseColor("#1F2937"),
            Shader.TileMode.CLAMP
        )
        paint.shader = shader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        // 2. Draw Teal accents / borders
        val tealPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2A9D8F")
            style = Paint.Style.STROKE
            strokeWidth = 16f
        }
        canvas.drawRect(40f, 40f, (width - 40).toFloat(), (height - 40).toFloat(), tealPaint)

        // Accent line under header
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2A9D8F")
            strokeWidth = 4f
        }
        canvas.drawLine(150f, 220f, (width - 150).toFloat(), 220f, linePaint)

        // 3. Draw App Title
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0F2F1")
            textSize = 54f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ZenZone 🧘\u200D♂️", (width / 2).toFloat(), 150f, textPaint)

        // 4. Draw User Name
        textPaint.apply {
            color = Color.WHITE
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val name = profile.userName.ifBlank { "Zen Practitioner" }
        canvas.drawText(name, (width / 2).toFloat(), 300f, textPaint)

        // 5. Draw Streak Count
        textPaint.apply {
            color = Color.parseColor("#F59E0B") // orange/gold for fire streak
            textSize = 180f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("${profile.currentChain} 🔥", (width / 2).toFloat(), 500f, textPaint)

        // Label under streak
        textPaint.apply {
            color = Color.parseColor("#9CA3AF") // light gray
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("CURRENT DAILY CHAIN STREAK", (width / 2).toFloat(), 560f, textPaint)

        // 6. Draw Garden Info
        textPaint.apply {
            color = Color.WHITE
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        
        val plantCount = profile.zenXP / 100
        val stageName = when {
            plantCount == 0 -> "Seeds Bed 🕳"
            plantCount <= 2 -> "Sprout Stage Garden 🌱"
            plantCount <= 5 -> "Blossoming Garden 🌸"
            plantCount <= 8 -> "Forest Sanctuary 🌳"
            else -> "Grand Zen Paradise 🪷"
        }
        canvas.drawText("Zen Level: ${profile.zenLevel} · Garden: $stageName", (width / 2).toFloat(), 680f, textPaint)

        // 7. Draw Total focused minutes
        val hours = profile.totalFocusedMinutes / 60.0
        val timeStr = if (hours >= 1) String.format("%.1f hours", hours) else "${profile.totalFocusedMinutes} minutes"
        canvas.drawText("Total Focused Time: $timeStr", (width / 2).toFloat(), 750f, textPaint)

        // 8. Draw Footer
        textPaint.apply {
            color = Color.parseColor("#6B7280")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        canvas.drawText("Find your flow. Nurture your digital garden.", (width / 2).toFloat(), 870f, textPaint)

        // Save bitmap to file in cache
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "zenzone_streak_share.png")
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }

            // Share Intent
            val contentUri = FileProvider.getUriForFile(context, "com.zenzone.app.fileprovider", file)
            if (contentUri != null) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Your Zen Streak"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to share streak card: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
