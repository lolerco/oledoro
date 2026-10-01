package com.lolerco.oledoro.domain

import java.util.Locale
import kotlin.math.abs

object TimeFormatter {
    fun format(remainingMs: Long): String {
        return if (remainingMs >= 0) {
            val totalSeconds = remainingMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        } else {
            val absMs = abs(remainingMs)
            val absSeconds = absMs / 1000
            val minutes = absSeconds / 60
            val seconds = absSeconds % 60
            String.format(Locale.US, "-%02d:%02d", minutes, seconds)
        }
    }
}
