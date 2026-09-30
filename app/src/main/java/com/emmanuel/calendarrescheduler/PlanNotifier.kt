package com.emmanuel.calendarrescheduler

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.emmanuel.calendarrescheduler.data.CalEvent
import com.emmanuel.calendarrescheduler.data.Dates
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object PlanNotifier {
    const val PLAN_CHANNEL_ID = "plan_updates"
    private const val PREFS = "ntf_state"
    private const val PLAN_NOTIF_ID = 9001
    private const val MISSED_NOTIF_BASE = 9100

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val alarmUri = try { AlarmScheduler.soundUri(context) } catch (e: Exception) {
            android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
        }
        val alarmCh = android.app.NotificationChannel(
            MainActivity.CHANNEL_ID, "Task reminders", NotificationManager.IMPORTANCE_MAX
        ).apply {
            description = "Reminders shortly before each task"
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            try {
                setSound(alarmUri, AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
            } catch (_: Exception) {
                enableVibration(true)
            }
            enableVibration(AlarmScheduler.isVibrateEnabled(context))
        }
        val planCh = android.app.NotificationChannel(
            PLAN_CHANNEL_ID, "Daily plan", NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Today's plan and missed-task nudges"
        }
        nm.createNotificationChannel(alarmCh)
        nm.createNotificationChannel(planCh)
    }

    fun recreateAlarmChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.deleteNotificationChannel(MainActivity.CHANNEL_ID)
        } catch (_: Exception) { }
        createChannels(context)
    }

    fun sendDailyPlan(context: Context, events: List<CalEvent>) {
        if (!canNotify(context)) return
        if (!AlarmScheduler.isPlanEnabled(context)) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val todayKey = dayKey(now)
        if (prefs.getString("daily_plan", "") == todayKey) return
        if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 6) return

        val dayStart = Dates.startOfDay(now)
        val soonDay = dayStart + 86400000L
        val today = events
            .filter { it.start >= dayStart && it.start < soonDay }
            .sortedBy { it.start }
        if (today.isEmpty()) return
        if (today.none { !it.protected }) return

        val visible = today.filter { !it.protected }.take(6)
        val text = buildString {
            visible.forEach { append(fmtTime(it.start)).append("   ").append(it.title).append("\n") }
            if (today.size > visible.size) append("and ${today.size - visible.size} more…")
        }.trimEnd()

        notify(context, PLAN_NOTIF_ID, PLAN_CHANNEL_ID, "Today's plan", text, eventId = null)
        prefs.edit().putString("daily_plan", todayKey).apply()
    }

    fun sendStreakNudge(context: Context) {
        if (!canNotify(context)) return
        if (!AlarmScheduler.isNudgesEnabled(context)) return
        try {
            val p = context.getSharedPreferences("focus", Context.MODE_PRIVATE)
            val streak = p.getInt("streak", 0)
            if (streak <= 0) return
            val today = dayKey(System.currentTimeMillis())
            if (p.getString("last_day", "") == today) return
            if (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) < 18) return
            if (p.getString("streak_nudge", "") == today) return
            notify(context, 9301, PLAN_CHANNEL_ID, "🔥 Streak at risk",
                "$streak-day focus streak ends tonight — squeeze in one session!", null)
            p.edit().putString("streak_nudge", today).apply()
        } catch (e: Exception) { }
    }

    fun sendMissedNudges(context: Context, events: List<CalEvent>) {
        if (!canNotify(context)) return
        if (!AlarmScheduler.isNudgesEnabled(context)) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val todayKey = dayKey(now)
        val missed = events.filter { ev ->
            ev.start + 5 * 60000L < now && ev.start > now - 3 * 3600000L &&
                !ev.protected &&
                !ev.desc.contains("✅ Completed") &&
                prefs.getString("missed|${ev.id}", "") != todayKey
        }.sortedBy { it.start }
        var sent = 0
        for (ev in missed) {
            if (sent >= 3) break
            val text = "'${ev.title}' started at ${fmtTime(ev.start)} and wasn't marked done."
            notify(context, MISSED_NOTIF_BASE + sent, PLAN_CHANNEL_ID, "Missed task 😬", text, ev.id)
            prefs.edit().putString("missed|${ev.id}", todayKey).apply()
            sent++
        }
    }

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun notify(context: Context, id: Int, channel: String, title: String, text: String, eventId: String?) {
        val openPi = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (eventId != null) putExtra(MainActivity.EXTRA_EVENT_ID, eventId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val reschedPi = if (eventId != null) {
            PendingIntent.getActivity(
                context, id + 1,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(MainActivity.EXTRA_EVENT_ID, eventId)
                    putExtra(MainActivity.EXTRA_ACTION, MainActivity.ACTION_RESCHEDULE)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else null
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openPi)
        if (reschedPi != null) builder.addAction(android.R.drawable.ic_menu_rotate, "Reschedule", reschedPi)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(id, builder.build())
    }

    private fun dayKey(ms: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(ms))

    private fun fmtTime(ms: Long): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ms))
}