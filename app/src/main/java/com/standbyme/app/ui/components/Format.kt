package com.standbyme.app.ui.components

import java.util.Locale

/** 125_000 ms -> "2:05". */
fun formatDuration(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
    return String.format(Locale.ROOT, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}

/** 7.2 h -> "7:12". */
fun formatHours(hours: Double): String {
    val h = hours.toInt()
    val m = ((hours - h) * 60).toInt()
    return String.format(Locale.ROOT, "%d:%02d", h, m)
}
