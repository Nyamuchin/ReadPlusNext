package com.readplusnext.ui.util

import com.readplusnext.domain.model.Comic
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", Locale.getDefault())

fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "--:--"
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "--"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.getDefault(), "%.1f GB", gb)
        mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
        else -> String.format(Locale.getDefault(), "%.0f KB", kb)
    }
}

fun formatDate(millis: Long): String =
    if (millis <= 0L) "--" else dateFormatter.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

fun comicProgressLabel(comic: Comic): String {
    val total = comic.pageCount.coerceAtLeast(0)
    if (total == 0) return "0 页"
    if (comic.lastReadPage <= 0) return "$total 页"
    val read = (comic.lastReadPage + 1).coerceAtMost(total)
    return "已读 $read/$total 页"
}

fun readingProgressFraction(comic: Comic): Float {
    if (comic.pageCount <= 0) return 0f
    val read = (comic.lastReadPage + 1).coerceIn(0, comic.pageCount)
    return (read.toFloat() / comic.pageCount).coerceIn(0f, 1f)
}

fun percentage(fraction: Float): Int = (fraction.coerceIn(0f, 1f) * 100).roundToInt()
