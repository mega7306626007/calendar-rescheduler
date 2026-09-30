package com.emmanuel.calendarrescheduler.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object Backup {
    const val VERSION = 1

    private val PREF_KEYS_STRING = listOf("theme_mode", "alarm_sound", "custom_templates")
    private val PREF_KEYS_LONG = listOf("reminder_min", "snooze_min", "second_min", "last_sync")
    private val PREF_KEYS_INT = listOf(
        "daily_goal", "weekly_goal", "stats_range",
        "quiet_from", "quiet_to"
    )
    private val PREF_KEYS_BOOL = listOf(
        "onboarded", "alarms_on", "plan_on", "nudges_on", "vibrate_on",
        "quiet_on", "second_on"
    )

    suspend fun exportToFile(context: Context, db: AppDatabase): File {
        val p = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val f = context.getSharedPreferences("focus", Context.MODE_PRIVATE)
        val root = JSONObject()
            .put("v", VERSION)
            .put("at", System.currentTimeMillis())
        val prefs = JSONObject()
        PREF_KEYS_STRING.forEach { k ->
            p.getString(k, null)?.let { prefs.put("s|$k", it) }
        }
        PREF_KEYS_LONG.forEach { k ->
            if (p.contains(k)) prefs.put("l|$k", p.getLong(k, 0L))
        }
        PREF_KEYS_INT.forEach { k ->
            if (p.contains(k)) prefs.put("i|$k", p.getInt(k, 0))
        }
        PREF_KEYS_BOOL.forEach { k ->
            if (p.contains(k)) prefs.put("b|$k", p.getBoolean(k, false))
        }
        root.put("prefs", prefs)
        val focus = JSONObject()
            .put("sessions", f.getInt("sessions", 0))
            .put("streak", f.getInt("streak", 0))
        f.getString("last_day", null)?.let { focus.put("last_day", it) }
        root.put("focus", focus)
        val undo = JSONArray()
        db.undoDao().recent().forEach { a ->
            undo.put(
                JSONObject()
                    .put("created", a.created)
                    .put("originalId", a.originalId)
                    .put("originalIsRecurring", a.originalIsRecurring)
                    .put("createdId", a.createdId)
                    .put("title", a.title)
                    .put("createdIds", a.createdIds)
            )
        }
        root.put("undo", undo)
        val series = JSONArray()
        db.seriesDao().allSeries().forEach { s ->
            val members = JSONArray()
            db.seriesDao().membersOf(s.id).forEach { m ->
                members.put(JSONObject().put("eventId", m.eventId).put("start", m.start))
            }
            series.put(
                JSONObject()
                    .put("id", s.id).put("title", s.title).put("created", s.created)
                    .put("members", members)
            )
        }
        root.put("series", series)
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())
        val out = File(context.cacheDir, "schedule-backup-$stamp.json")
        out.writeText(root.toString())
        return out
    }

    /** Returns a short summary of what was restored. */
    suspend fun importFromJson(context: Context, db: AppDatabase, json: String): String {
        val root = JSONObject(json)
        var applied = 0
        val prefs = root.optJSONObject("prefs")
        if (prefs != null) {
            val e = context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
            val keys = prefs.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val type = k.substringBefore("|")
                val name = k.substringAfter("|")
                when (type) {
                    "s" -> e.putString(name, prefs.optString(k))
                    "l" -> e.putLong(name, prefs.optLong(k))
                    "i" -> e.putInt(name, prefs.optInt(k))
                    "b" -> e.putBoolean(name, prefs.optBoolean(k))
                }
                applied++
            }
            e.apply()
        }
        val focus = root.optJSONObject("focus")
        if (focus != null) {
            context.getSharedPreferences("focus", Context.MODE_PRIVATE).edit()
                .putInt("sessions", focus.optInt("sessions", 0))
                .putInt("streak", focus.optInt("streak", 0))
                .putString("last_day", focus.optString("last_day", ""))
                .apply()
            applied++
        }
        val undo = root.optJSONArray("undo")
        if (undo != null) {
            for (i in 0 until undo.length()) {
                val o = undo.optJSONObject(i) ?: continue
                db.undoDao().insert(
                    UndoAction(
                        created = o.optLong("created", System.currentTimeMillis()),
                        originalId = o.optString("originalId").takeIf { it.isNotEmpty() },
                        originalIsRecurring = o.optBoolean("originalIsRecurring", false),
                        createdId = o.optString("createdId").takeIf { it.isNotEmpty() },
                        title = o.optString("title", ""),
                        createdIds = o.optString("createdIds").takeIf { it.isNotEmpty() }
                    )
                )
                applied++
            }
        }
        val series = root.optJSONArray("series")
        if (series != null) {
            for (i in 0 until series.length()) {
                val o = series.optJSONObject(i) ?: continue
                val id = o.optString("id")
                if (id.isEmpty()) continue
                db.seriesDao().insertSeries(
                    SeriesEntity(id, o.optString("title", ""), o.optLong("created", 0L))
                )
                val members = o.optJSONArray("members") ?: JSONArray()
                val rows = (0 until members.length()).mapNotNull { j ->
                    val m = members.optJSONObject(j) ?: return@mapNotNull null
                    val eid = m.optString("eventId")
                    if (eid.isEmpty()) null else SeriesMember(id, eid, m.optLong("start", 0L))
                }
                if (rows.isNotEmpty()) db.seriesDao().insertMembers(rows)
                applied++
            }
        }
        return "Restored $applied items ✅"
    }

    fun isAutoEnabled(context: Context): Boolean = try {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getBoolean("backup_auto", false)
    } catch (e: Exception) { false }

    fun setAutoEnabled(context: Context, on: Boolean) {
        try {
            context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .edit().putBoolean("backup_auto", on).apply()
        } catch (e: Exception) { }
        try {
            val wm = WorkManager.getInstance(context)
            if (on) {
                val work = PeriodicWorkRequestBuilder<BackupWorker>(7, TimeUnit.DAYS).build()
                wm.enqueueUniquePeriodicWork("auto_backup", ExistingPeriodicWorkPolicy.KEEP, work)
            } else {
                wm.cancelUniqueWork("auto_backup")
            }
        } catch (e: Exception) { }
    }
}

class BackupWorker(
    appContext: Context,
    params: androidx.work.WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val ctx = applicationContext
            val db = AppDatabase.get(ctx)
            val src = Backup.exportToFile(ctx, db)
            val dir = File(ctx.filesDir, "backups").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            src.copyTo(File(dir, "auto-backup-$stamp.json"), overwrite = true)
            dir.listFiles()
                ?.sortedByDescending { it.lastModified() }
                ?.drop(4)
                ?.forEach { try { it.delete() } catch (_: Exception) { } }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
