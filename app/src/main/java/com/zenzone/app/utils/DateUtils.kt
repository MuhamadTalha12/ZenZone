package com.zenzone.app.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    fun getTodayString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getIsoTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    fun daysBetween(date1Str: String, date2Str: String): Int {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return try {
            val d1 = sdf.parse(date1Str)
            val d2 = sdf.parse(date2Str)
            if (d1 != null && d2 != null) {
                val diffCount = d2.time - d1.time
                (diffCount / (1000 * 60 * 60 * 24)).toInt()
            } else {
                -1
            }
        } catch (e: Exception) {
            -1
        }
    }
}
