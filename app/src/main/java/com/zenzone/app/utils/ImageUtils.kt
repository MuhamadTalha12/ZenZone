package com.zenzone.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageUtils {

    private const val MAX_DIMENSION = 150
    private const val COMPRESSION_QUALITY = 75
    private const val BASE64_PREFIX = "data:image/jpeg;base64,"

    fun uriToBase64(context: Context, uri: Uri): String? {
        var inputStream: InputStream? = null
        return try {
            inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            
            // Resize bitmap maintaining aspect ratio
            val width = originalBitmap.width
            val height = originalBitmap.height
            val (newWidth, newHeight) = if (width > height) {
                val ratio = width.toFloat() / height.toFloat()
                Pair(MAX_DIMENSION, (MAX_DIMENSION / ratio).toInt())
            } else {
                val ratio = height.toFloat() / width.toFloat()
                Pair((MAX_DIMENSION / ratio).toInt(), MAX_DIMENSION)
            }
            
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            
            // Compress to JPEG
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outputStream)
            val imageBytes = outputStream.toByteArray()
            
            // Convert to Base64
            val base64String = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            "$BASE64_PREFIX$base64String"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try {
                inputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val pureBase64 = if (base64Str.startsWith(BASE64_PREFIX)) {
                base64Str.substring(BASE64_PREFIX.length)
            } else {
                base64Str
            }
            val decodedBytes = Base64.decode(pureBase64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun isBase64Image(str: String?): Boolean {
        return str != null && (str.startsWith(BASE64_PREFIX) || str.length > 500)
    }
}
