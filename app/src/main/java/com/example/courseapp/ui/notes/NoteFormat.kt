package com.example.courseapp.ui.notes

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val hourMinute = DateTimeFormatter.ofPattern("HH:mm")
private val monthDay = DateTimeFormatter.ofPattern("M月d日")
private val fullDateTime = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm")

/** 文件夹卡片与照片卡片的“最近更新”文案。 */
fun formatNoteTime(
    timestamp: Long,
    now: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault()
): String {
    if (timestamp <= 0L) return "暂无内容"
    val moment = Instant.ofEpochMilli(timestamp).atZone(zone)
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val date = moment.toLocalDate()
    return when {
        date == today -> "今天 ${moment.format(hourMinute)}"
        date == today.minusDays(1) -> "昨天 ${moment.format(hourMinute)}"
        date.year == today.year -> moment.format(monthDay)
        else -> "${date.year}年${moment.format(monthDay)}"
    }
}

fun formatNoteDateTime(
    timestamp: Long,
    zone: ZoneId = ZoneId.systemDefault()
): String = Instant.ofEpochMilli(timestamp).atZone(zone).format(fullDateTime)
