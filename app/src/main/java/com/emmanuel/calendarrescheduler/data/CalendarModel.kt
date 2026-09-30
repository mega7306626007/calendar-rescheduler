package com.emmanuel.calendarrescheduler.data

import java.util.Calendar

data class CalEvent(
    val id: String,
    val title: String,
    val start: Long,
    val end: Long,
    val colorId: String,
    val recurring: Boolean,
    val protected: Boolean,
    val desc: String,
    val durationMin: Int
) {
    val durationMs: Long get() = end - start
}

data class DayData(
    val millis: Long,
    val weekday: String,
    val weekdayNum: Int,
    val monthDay: Int,
    val monthShort: String,
    val isToday: Boolean,
    val inMonth: Boolean,
    val events: List<CalEvent>,
    val freeSlots: List<Slot>
) {
    val isWeekend: Boolean get() = weekdayNum == 6 || weekdayNum == 0
}

data class Slot(val ms: Long, val end: Long)

data class WeekWindow(val type: String, val startMs: Long, val endMs: Long, val days: List<DayData>)

object CalColors {
    // Google Calendar colorId -> (background, foreground)
    val MAP = mapOf(
        "1" to Pair("#cdd8fd", "#394c8c"),
        "2" to Pair("#b2f5d5", "#137a52"),
        "3" to Pair("#e7d5ff", "#6b2d8b"),
        "4" to Pair("#ffccc9", "#a43024"),
        "5" to Pair("#fff3b8", "#8f6b00"),
        "6" to Pair("#ffe5cc", "#9c4a00"),
        "7" to Pair("#c9f1f2", "#00777d"),
        "8" to Pair("#f0f0f0", "#5b5b5b"),
        "9" to Pair("#c3d8f7", "#1e3f8f"),
        "10" to Pair("#c4ecc2", "#1f5f1f"),
        "11" to Pair("#fdb2b4", "#8c1216")
    )
    fun of(c: String): Pair<String, String> = MAP[c] ?: Pair("#e8f0ff", "#2563eb")
}

object Dates {
    fun startOfDay(ms: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = ms
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }
    fun addDays(ms: Long, days: Int): Long = ms + days * 86400000L
    fun daysBetween(a: Long, b: Long): Int = Math.round((startOfDay(a) - startOfDay(b)) / 86400000.0).toInt()
    fun hourOf(ms: Long): Int = Calendar.getInstance().apply { timeInMillis = ms }.get(Calendar.HOUR_OF_DAY)
    fun minuteOf(ms: Long): Int = Calendar.getInstance().apply { timeInMillis = ms }.get(Calendar.MINUTE)
    fun timeOn(ms: Long, hour: Int, minute: Int): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = ms
        c.set(Calendar.HOUR_OF_DAY, hour); c.set(Calendar.MINUTE, minute); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }
}

val PROTECTED_TITLES = listOf("rest & social time", "bible study", "prayer", "church")
val PROTECTED_COLOR_IDS = listOf("10", "1")

fun isProtected(title: String, desc: String, colorId: String): Boolean {
    val text = (title + " " + desc).lowercase()
    for (t in PROTECTED_TITLES) if (text.contains(t)) return true
    return colorId in PROTECTED_COLOR_IDS
}