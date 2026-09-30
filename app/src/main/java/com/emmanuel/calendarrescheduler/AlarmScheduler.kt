package com.emmanuel.calendarrescheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object AlarmScheduler {
    private const val PREFS = "task_cache"
    private const val KEY = "upcoming"
    const val DEFAULT_REMINDER_MINUTES = 10L

    fun getReminderMinutes(context: Context): Long {
        return try {
            context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .getLong("reminder_min", DEFAULT_REMINDER_MINUTES)
                .coerceIn(1L, 120L)
        } catch (e: Exception) { DEFAULT_REMINDER_MINUTES }
    }

    private val LEAD_RE = Regex("⏰(\\d{1,3})")

    fun leadForDesc(desc: String, defaultMin: Long): Long {
        return try {
            LEAD_RE.find(desc)?.groupValues?.getOrNull(1)?.toLongOrNull()?.coerceIn(0L, 180L)
                ?: defaultMin
        } catch (e: Exception) { defaultMin }
    }

    fun withLeadToken(desc: String, minutes: Long?): String {
        val clean = try { LEAD_RE.replace(desc, "").trim() } catch (e: Exception) { desc }
        return if (minutes == null) clean else (if (clean.isNotEmpty()) "$clean\n" else "") + "⏰$minutes"
    }

    fun describeLead(desc: String, defaultMin: Long): String {
        val m = leadForDesc(desc, -1L)
        return if (m < 0) "Default (${defaultMin}m)" else "$m min"
    }

    fun setReminderMinutes(context: Context, minutes: Long) {
        try {
            context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .edit().putLong("reminder_min", minutes.coerceIn(1L, 120L)).apply()
        } catch (e: Exception) { }
    }

    val SOUND_OPTIONS = arrayOf("device_alarm", "device_notification", "device_ringtone", "device_pick")
    val SOUND_LABELS = arrayOf("Device alarm sound", "Device notification", "Device ringtone", "Choose from device…")

    fun getSoundName(context: Context): String {
        return try {
            val s = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .getString("alarm_sound", "device_alarm") ?: "device_alarm"
            if (s.startsWith("device_") || s.startsWith("custom:")) s else "device_alarm"
        } catch (e: Exception) { "device_alarm" }
    }

    fun setSoundName(context: Context, name: String) {
        try {
            context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .edit().putString("alarm_sound", name).apply()
        } catch (e: Exception) { }
    }

    fun soundLabel(context: Context): String {
        val s = getSoundName(context)
        val i = SOUND_OPTIONS.indexOf(s)
        if (i >= 0 && s != "device_pick") return SOUND_LABELS[i]
        if (s.startsWith("custom:")) {
            return try {
                val title = android.media.RingtoneManager.getRingtone(
                    context, android.net.Uri.parse(s.removePrefix("custom:")))?.getTitle(context)
                title?.takeIf { it.isNotBlank() }?.let { "Chosen: $it" } ?: "Chosen sound"
            } catch (e: Exception) { "Chosen sound" }
        }
        return SOUND_LABELS[0]
    }

    fun soundUri(context: Context): android.net.Uri {
        return try {
            when (val s = getSoundName(context)) {
                "device_notification" -> android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                "device_ringtone" -> android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
                "device_pick" -> android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                else -> if (s.startsWith("custom:")) android.net.Uri.parse(s.removePrefix("custom:"))
                        else android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
            }
        } catch (e: Exception) {
            android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun isAlarmsEnabled(context: Context): Boolean {
        return try { prefs(context).getBoolean("alarms_on", true) } catch (e: Exception) { true }
    }

    fun setAlarmsEnabled(context: Context, on: Boolean) {
        try { prefs(context).edit().putBoolean("alarms_on", on).apply() } catch (e: Exception) { }
    }

    fun isPlanEnabled(context: Context): Boolean {
        return try { prefs(context).getBoolean("plan_on", true) } catch (e: Exception) { true }
    }

    fun setPlanEnabled(context: Context, on: Boolean) {
        try { prefs(context).edit().putBoolean("plan_on", on).apply() } catch (e: Exception) { }
    }

    fun isNudgesEnabled(context: Context): Boolean {
        return try { prefs(context).getBoolean("nudges_on", true) } catch (e: Exception) { true }
    }

    fun setNudgesEnabled(context: Context, on: Boolean) {
        try { prefs(context).edit().putBoolean("nudges_on", on).apply() } catch (e: Exception) { }
    }

    fun isVibrateEnabled(context: Context): Boolean {
        return try { prefs(context).getBoolean("vibrate_on", true) } catch (e: Exception) { true }
    }

    fun setVibrateEnabled(context: Context, on: Boolean) {
        try { prefs(context).edit().putBoolean("vibrate_on", on).apply() } catch (e: Exception) { }
    }

    fun getSnoozeMinutes(context: Context): Long {
        return try {
            prefs(context).getLong("snooze_min", 10L).coerceIn(1L, 120L)
        } catch (e: Exception) { 10L }
    }

    fun setSnoozeMinutes(context: Context, minutes: Long) {
        try { prefs(context).edit().putLong("snooze_min", minutes.coerceIn(1L, 120L)).apply() } catch (e: Exception) { }
    }

    fun isQuietEnabled(context: Context): Boolean {
        return try { prefs(context).getBoolean("quiet_on", false) } catch (e: Exception) { false }
    }

    fun setQuietEnabled(context: Context, on: Boolean) {
        try { prefs(context).edit().putBoolean("quiet_on", on).apply() } catch (e: Exception) { }
    }

    fun getQuietFrom(context: Context): Int {
        return try { prefs(context).getInt("quiet_from", 22).coerceIn(0, 23) } catch (e: Exception) { 22 }
    }

    fun setQuietFrom(context: Context, h: Int) {
        try { prefs(context).edit().putInt("quiet_from", h.coerceIn(0, 23)).apply() } catch (e: Exception) { }
    }

    fun getQuietTo(context: Context): Int {
        return try { prefs(context).getInt("quiet_to", 6).coerceIn(0, 23) } catch (e: Exception) { 6 }
    }

    fun setQuietTo(context: Context, h: Int) {
        try { prefs(context).edit().putInt("quiet_to", h.coerceIn(0, 23)).apply() } catch (e: Exception) { }
    }

    fun isSecondEnabled(context: Context): Boolean {
        return try { prefs(context).getBoolean("second_on", false) } catch (e: Exception) { false }
    }

    fun setSecondEnabled(context: Context, on: Boolean) {
        try { prefs(context).edit().putBoolean("second_on", on).apply() } catch (e: Exception) { }
    }

    fun getSecondMinutes(context: Context): Long {
        return try { prefs(context).getLong("second_min", 60L).coerceIn(5L, 180L) } catch (e: Exception) { 60L }
    }

    fun setSecondMinutes(context: Context, minutes: Long) {
        try { prefs(context).edit().putLong("second_min", minutes.coerceIn(5L, 180L)).apply() } catch (e: Exception) { }
    }

    fun inQuietHours(context: Context, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (!isQuietEnabled(context)) return false
        return try {
            val h = java.util.Calendar.getInstance().apply { timeInMillis = nowMs }.get(java.util.Calendar.HOUR_OF_DAY)
            val from = getQuietFrom(context)
            val to = getQuietTo(context)
            if (from == to) false else if (from < to) h in from until to else h >= from || h < to
        } catch (e: Exception) { false }
    }

    fun logFired(context: Context, title: String) {
        try {
            val p = prefs(context)
            val arr = try { JSONArray(p.getString("fired_log", "[]") ?: "[]") } catch (e: Exception) { JSONArray() }
            arr.put(JSONObject(mapOf("t" to title.take(60), "at" to System.currentTimeMillis())))
            while (arr.length() > 10) arr.remove(0)
            p.edit().putString("fired_log", arr.toString()).apply()
        } catch (e: Exception) { }
    }

    fun readFired(context: Context): List<Pair<String, Long>> {
        return try {
            val arr = JSONArray(prefs(context).getString("fired_log", "[]") ?: "[]")
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                o.optString("t", "").takeIf { it.isNotEmpty() }?.let { it to o.optLong("at", 0L) }
            }.sortedByDescending { it.second }
        } catch (e: Exception) { emptyList() }
    }

    fun saveCache(context: Context, json: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, json).apply()
    }

    fun scheduleFromCache(context: Context) {
        val json = getCache(context) ?: return
        scheduleFromJson(context, json)
    }

    fun scheduleFromJson(context: Context, json: String): Int {
        val arr = try { JSONArray(json) } catch (e: Exception) { return 0 }
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()
        val defaultLead = getReminderMinutes(context)
        val secondOn = isSecondEnabled(context)
        val secondLead = getSecondMinutes(context)
        var scheduled = 0
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.optString("id", "")
            val title = o.optString("title", "Upcoming task")
            val start = o.optLong("start", 0L)
            val desc = o.optString("desc", "")
            if (id.isEmpty() || start <= 0) continue
            val leadMin = leadForDesc(desc, defaultLead)
            val fire = start - leadMin * 60_000L
            if (fire > now + 60_000L) {
                if (scheduleOne(alarm, context, id, title, start, desc, stableId(id), fire, "")) scheduled++
            }
            if (secondOn) {
                val fire2 = start - secondLead * 60_000L
                if (fire2 > now + 60_000L && fire2 != fire) {
                    val note = "⏳ about ${fmtLead(secondLead)} left"
                    if (scheduleOne(alarm, context, id, title, start, desc, stableId(id) + SECOND_OFFSET, fire2, note)) scheduled++
                }
            }
        }
        return scheduled
    }

    private fun fmtLead(m: Long): String = when {
        m >= 60 && m % 60 == 0L -> "${m / 60}h"
        m >= 60 -> "${m / 60}h ${m % 60}m"
        else -> "${m}m"
    }

    private fun scheduleOne(alarm: AlarmManager, context: Context, id: String, title: String, start: Long, desc: String, notifId: Int, fire: Long, note: String): Boolean {
        return try {
            val pi = PendingIntent.getBroadcast(
                context, notifId,
                alarmIntent(context, id, title, start, desc, notifId, note),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val showPi = PendingIntent.getActivity(
                context, notifId + 2,
                fullScreenIntent(context, id, title, start, desc, notifId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try {
                alarm.setAlarmClock(AlarmManager.AlarmClockInfo(fire, showPi), pi)
            } catch (e: SecurityException) {
                if (android.os.Build.VERSION.SDK_INT >= 23) {
                    alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fire, pi)
                } else {
                    alarm.set(AlarmManager.RTC_WAKEUP, fire, pi)
                }
            }
            true
        } catch (_: Exception) { false }
    }

    fun scheduleDailyPlan(context: Context) {
        try {
            val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val c = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 6)
                set(java.util.Calendar.MINUTE, 5)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis() + 60_000L) {
                    add(java.util.Calendar.DAY_OF_YEAR, 1)
                }
            }
            val pi = PendingIntent.getBroadcast(
                context, 8001,
                Intent(context, DailyPlanReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try {
                if (android.os.Build.VERSION.SDK_INT >= 31) {
                    if (alarm.canScheduleExactAlarms()) {
                        alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.timeInMillis, pi)
                    } else {
                        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.timeInMillis, pi)
                    }
                } else if (android.os.Build.VERSION.SDK_INT >= 23) {
                    alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.timeInMillis, pi)
                } else {
                    alarm.setExact(AlarmManager.RTC_WAKEUP, c.timeInMillis, pi)
                }
            } catch (e: SecurityException) {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.timeInMillis, pi)
            }
        } catch (_: Exception) { }
    }

    fun snoozeOneShot(context: Context, id: String, title: String, start: Long, desc: String, notifId: Int, delayMin: Long = 10L) {
        try {
            val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val fire = System.currentTimeMillis() + delayMin * 60_000L
            val pi = PendingIntent.getBroadcast(
                context, notifId,
                alarmIntent(context, id, title, start, desc, notifId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val showPi = PendingIntent.getActivity(
                context, notifId + 2,
                fullScreenIntent(context, id, title, start, desc, notifId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try {
                alarm.setAlarmClock(AlarmManager.AlarmClockInfo(fire, showPi), pi)
            } catch (e: SecurityException) {
                if (android.os.Build.VERSION.SDK_INT >= 23) {
                    alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fire, pi)
                } else {
                    alarm.set(AlarmManager.RTC_WAKEUP, fire, pi)
                }
            }
        } catch (_: Exception) { }
    }

    fun cancelStale(context: Context, oldJson: String?, newJson: String) {
        if (oldJson.isNullOrBlank()) return
        val oldArr = try { JSONArray(oldJson) } catch (e: Exception) { return }
        val newArr = try { JSONArray(newJson) } catch (e: Exception) { return }
        val newIds = HashSet<String>()
        for (i in 0 until newArr.length()) {
            newArr.optJSONObject(i)?.optString("id", "")?.takeIf { it.isNotEmpty() }?.let { newIds.add(it) }
        }
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (i in 0 until oldArr.length()) {
            val id = oldArr.optJSONObject(i)?.optString("id", "") ?: continue
            if (id.isEmpty() || newIds.contains(id)) continue
            cancelOne(context, alarm, id)
        }
    }

    private fun cancelOne(context: Context, alarm: AlarmManager, id: String) {
        for (nid in arrayOf(stableId(id), stableId(id) + SECOND_OFFSET)) {
            try {
                val pi = PendingIntent.getBroadcast(
                    context, nid,
                    alarmIntent(context, id, "", 0L, "", nid),
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pi != null) alarm.cancel(pi)
            } catch (_: Exception) { }
        }
    }

    fun cancelAll(context: Context) {
        val json = getCache(context) ?: return
        val arr = try { JSONArray(json) } catch (e: Exception) { return }
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.optString("id", "")
            if (id.isEmpty()) continue
            cancelOne(context, alarm, id)
        }
    }

    fun getCache(context: Context): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
    }

    const val SECOND_OFFSET = 1_000_003

    private fun alarmIntent(context: Context, id: String, title: String, start: Long, desc: String, notifId: Int, note: String = ""): Intent {
        return Intent(context, AlarmReceiver::class.java).apply {
            putExtra(MainActivity.EXTRA_EVENT_ID, id)
            putExtra(MainActivity.EXTRA_TITLE, title)
            putExtra(MainActivity.EXTRA_START, start)
            putExtra(MainActivity.EXTRA_DESC, desc)
            putExtra(MainActivity.NOTIF_ID, notifId)
            putExtra(MainActivity.EXTRA_NOTE, note)
        }
    }

    private fun fullScreenIntent(context: Context, id: String, title: String, start: Long, desc: String, notifId: Int): Intent {
        return Intent(context, AlarmFullScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(MainActivity.EXTRA_EVENT_ID, id)
            putExtra(MainActivity.EXTRA_TITLE, title)
            putExtra(MainActivity.EXTRA_START, start)
            putExtra(MainActivity.EXTRA_DESC, desc)
            putExtra(MainActivity.NOTIF_ID, notifId)
        }
    }

    fun stableId(id: String): Int {
        var h = 0
        for (c in id) h = h * 31 + c.code
        if (h == Int.MIN_VALUE) h = Int.MAX_VALUE
        return Math.abs(h)
    }
}