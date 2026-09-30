package com.emmanuel.calendarrescheduler.data

import android.content.Context
import com.emmanuel.calendarrescheduler.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class Repository(private val context: Context) {

    val api = CalendarApi(context)
    val db = AppDatabase.get(context)

    suspend fun buildWindow(type: String, anchorMs: Long): WeekWindow = withContext(Dispatchers.IO) {
        val a = Dates.startOfDay(anchorMs)
        when (type) {
            "day" -> buildWindowInternal("day", a, 1)
            "agenda" -> buildWindowInternal("agenda", a, 7)
            "week" -> {
                val wd = weekdayNum(a)
                val start = a - (if (wd == 0) 6 else wd - 1) * 86400000L
                buildWindowInternal("week", start, 7)
            }
            else -> {
                val c = Calendar.getInstance().apply {
                    timeInMillis = a
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val first = c.timeInMillis
                val lead = weekdayLead(first)
                val dim = c.getActualMaximum(Calendar.DAY_OF_MONTH)
                val total = lead + dim
                val trail = (7 - (total % 7)) % 7
                buildWindowInternal("month", first - lead * 86400000L, total + trail)
            }
        }
    }

    private fun weekdayNum(ms: Long): Int =
        Calendar.getInstance().apply { timeInMillis = ms }.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun

    private fun weekdayLead(ms: Long): Int {
        val wd = weekdayNum(ms)
        return if (wd == 0) 6 else wd - 1
    }

    private suspend fun buildWindowInternal(type: String, startDay: Long, count: Int): WeekWindow {
        val end = startDay + count * 86400000L
        val today = Dates.startOfDay(System.currentTimeMillis())

        var events: List<CalEvent> = try {
            api.fetchEvents(startDay, end)
        } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
        var offline = events.isEmpty()
        if (offline) {
            events = db.eventDao().getBetween(startDay, end)
                .map { CalEvent(it.id, it.title, it.start, it.end, it.colorId, it.recurring, it.protected_, it.desc, it.durationMin) }
        }

        val busy: List<Pair<Long, Long>> = try {
            api.freebusy(startDay, end)
        } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }

        val monthKey = if (type == "month") {
            Calendar.getInstance().apply { timeInMillis = startDay + 15 * 86400000L }
                .let { c -> "${c.get(Calendar.YEAR)}-${"%02d".format(c.get(Calendar.MONTH) + 1)}" }
        } else null

        val days = (0 until count).map { i ->
            val dayMs = startDay + i * 86400000L
            val c = Calendar.getInstance().apply { timeInMillis = dayMs }
            val wdIdx = c.get(Calendar.DAY_OF_WEEK) - 1
            val inMonth = if (monthKey != null) {
                val ky = "${c.get(Calendar.YEAR)}-${"%02d".format(c.get(Calendar.MONTH) + 1)}"
                ky == monthKey
            } else true
            DayData(
                millis = dayMs,
                weekday = WEEKDAYS[wdIdx],
                weekdayNum = wdIdx,
                monthDay = c.get(Calendar.DAY_OF_MONTH),
                monthShort = MONTHS[c.get(Calendar.MONTH)],
                isToday = dayMs == today,
                inMonth = inMonth,
                events = events.filter { it.start >= dayMs && it.start < dayMs + 86400000L },
                freeSlots = SlotEngine.freeSlotsForDay(dayMs, busy)
            )
        }

        if (!offline) {
            db.eventDao().upsert(
                events.map { CachedEvent(it.id, it.title, it.start, it.end, it.colorId, it.recurring, it.protected, it.desc, it.durationMin, System.currentTimeMillis()) }
            )
            db.eventDao().deleteBefore(System.currentTimeMillis() - 21 * 86400000L)
        }

        return WeekWindow(type, startDay, end - 1, days)
    }

    suspend fun syncAlarms(context: Context): Int {
        return backgroundRefresh(context).second
    }

    suspend fun backgroundRefresh(context: Context): Pair<List<CalEvent>, Int> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val from = Dates.startOfDay(now - 86400000L)
        val until = now + 7 * 86400000L
        val events = try { api.fetchEvents(from, until) } catch (e: Exception) { emptyList() }
        if (events.isNotEmpty()) {
            try {
                context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                    .edit().putLong("last_sync", now).apply()
            } catch (e: Exception) { }
        }
        val future = events.filter { it.start > now && !it.desc.contains("✅ Completed") }.sortedBy { it.start }
        val json = org.json.JSONArray().apply {
            future.forEach { ev ->
                put(org.json.JSONObject(mapOf(
                    "id" to ev.id,
                    "title" to ev.title,
                    "start" to ev.start,
                    "desc" to ev.desc
                )))
            }
        }.toString()
        val oldJson = AlarmScheduler.getCache(context)
        if (!AlarmScheduler.isAlarmsEnabled(context)) {
            AlarmScheduler.saveCache(context, "[]")
            try { AlarmScheduler.cancelStale(context, oldJson, "[]") } catch (e: Exception) { }
            Pair(events, 0)
        } else {
            AlarmScheduler.saveCache(context, json)
            val scheduled = try { AlarmScheduler.scheduleFromJson(context, json) } catch (e: Exception) { 0 }
            try { AlarmScheduler.cancelStale(context, oldJson, json) } catch (e: Exception) { }
            Pair(events, scheduled)
        }
    }

    companion object {
        private val WEEKDAYS = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        private val MONTHS = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }
}