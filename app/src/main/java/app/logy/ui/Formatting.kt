package app.logy.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

fun formatDateTime(time: Long): String =
    SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(time))

fun formatElapsed(time: Long?, now: Long = System.currentTimeMillis()): String {
    if (time == null) return "No entries yet"
    val seconds = abs(now - time) / 1000
    return when {
        seconds < 60 -> "just now"
        seconds < 3_600 -> "${seconds / 60}m ago"
        seconds < 86_400 -> "${seconds / 3_600}h ago"
        seconds < 2_592_000 -> "${seconds / 86_400}d ago"
        seconds < 31_536_000 -> "${seconds / 2_592_000}mo ago"
        else -> "${seconds / 31_536_000}y ago"
    }
}

fun formatDuration(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}
