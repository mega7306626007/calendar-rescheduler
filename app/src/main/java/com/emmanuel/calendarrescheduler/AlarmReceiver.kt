package com.emmanuel.calendarrescheduler

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (intent.action == "DISMISS") {
            val nid = intent.getIntExtra(MainActivity.NOTIF_ID, 0)
            try { nm.cancel(nid) } catch (_: Exception) { }
            return
        }
        if (intent.action == "SNOOZE") {
            val nid = intent.getIntExtra(MainActivity.NOTIF_ID, 0)
            val sid = intent.getStringExtra(MainActivity.EXTRA_EVENT_ID) ?: return
            val stitle = intent.getStringExtra(MainActivity.EXTRA_TITLE) ?: "Upcoming task"
            val sstart = intent.getLongExtra(MainActivity.EXTRA_START, 0L)
            val sdesc = intent.getStringExtra(MainActivity.EXTRA_DESC) ?: ""
            AlarmScheduler.snoozeOneShot(context, sid, stitle, sstart, sdesc, nid, AlarmScheduler.getSnoozeMinutes(context))
            try { nm.cancel(nid) } catch (_: Exception) { }
            return
        }
        val eventId = intent.getStringExtra(MainActivity.EXTRA_EVENT_ID) ?: return
        val title = intent.getStringExtra(MainActivity.EXTRA_TITLE) ?: "Upcoming task"
        val start = intent.getLongExtra(MainActivity.EXTRA_START, 0L)
        val desc = intent.getStringExtra(MainActivity.EXTRA_DESC) ?: ""
        val note = intent.getStringExtra(MainActivity.EXTRA_NOTE) ?: ""
        val notifId = intent.getIntExtra(MainActivity.NOTIF_ID, 0)

        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        try {
            PlanNotifier.createChannels(context)
        } catch (_: Exception) { }

        val openPi = pendingActivity(context, eventId, title, start, notifId, MainActivity.ACTION_OPEN)
        val reschedPi = pendingActivity(context, eventId, title, start, notifId + 1, MainActivity.ACTION_RESCHEDULE)
        val fullScreenPi = PendingIntent.getActivity(
            context, notifId + 2,
            Intent(context, AlarmFullScreenActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(MainActivity.EXTRA_EVENT_ID, eventId)
                putExtra(MainActivity.EXTRA_TITLE, title)
                putExtra(MainActivity.EXTRA_START, start)
                putExtra(MainActivity.EXTRA_DESC, desc)
                putExtra(MainActivity.NOTIF_ID, notifId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val whenText = if (start > 0) "starts " + SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault()).format(Date(start)) else ""
        val movedLine = when (true) {
            desc.contains("Reason:", ignoreCase = true) -> "You moved this task (reason below) — try to do it this time! 😢"
            desc.contains("✅ Completed") -> "Done — well done! 🎉"
            else -> "It's time"
        }
        val text = buildString {
            if (note.isNotEmpty()) append(note).append("\n")
            append(whenText)
            if (desc.isNotEmpty()) append("\n").append(desc)
            append("\n").append(movedLine)
        }

        val alarmUri = try { AlarmScheduler.soundUri(context) } catch (e: Exception) {
            android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
        }
        val quiet = AlarmScheduler.inQuietHours(context)
        val snoozeMin = AlarmScheduler.getSnoozeMinutes(context)
        val builder = NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(if (quiet) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(!quiet)
            .setContentIntent(openPi)
            .addAction(android.R.drawable.ic_menu_rotate, "Reschedule", reschedPi)
            .addAction(android.R.drawable.ic_menu_recent_history, "Snooze ${snoozeMin}m", snoozePi(context, eventId, title, start, desc, notifId))
            .addAction(android.R.drawable.ic_menu_zoom, "Open", openPi)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPi(context, notifId))
        if (!quiet) {
            builder.setFullScreenIntent(fullScreenPi, true)
        }

        if (quiet) {
            try {
                builder.setSound(
                    android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
                if (AlarmScheduler.isVibrateEnabled(context)) {
                    builder.setVibrate(longArrayOf(300, 300, 300))
                }
            } catch (_: Exception) {
                builder.setDefaults(Notification.DEFAULT_ALL)
            }
        } else if (alarmUri != null) {
            try {
                builder.setSound(alarmUri, AudioManager.STREAM_ALARM)
                if (AlarmScheduler.isVibrateEnabled(context)) {
                    builder.setVibrate(longArrayOf(800, 400, 800, 400, 1500))
                }
            } catch (_: Exception) {
                builder.setDefaults(Notification.DEFAULT_ALL)
            }
        } else {
            builder.setDefaults(Notification.DEFAULT_ALL)
        }

        try {
            nm.notify(notifId, builder.build())
            AlarmScheduler.logFired(context, title)
        } catch (_: Exception) { }
    }

    private fun dismissPi(context: Context, notifId: Int): PendingIntent {
        val i = Intent(context, AlarmReceiver::class.java).apply {
            action = "DISMISS"
            putExtra(MainActivity.NOTIF_ID, notifId)
        }
        return PendingIntent.getBroadcast(
            context, notifId + 3, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun snoozePi(context: Context, eventId: String, title: String, start: Long, desc: String, notifId: Int): PendingIntent {
        val i = Intent(context, AlarmReceiver::class.java).apply {
            action = "SNOOZE"
            putExtra(MainActivity.NOTIF_ID, notifId)
            putExtra(MainActivity.EXTRA_EVENT_ID, eventId)
            putExtra(MainActivity.EXTRA_TITLE, title)
            putExtra(MainActivity.EXTRA_START, start)
            putExtra(MainActivity.EXTRA_DESC, desc)
        }
        return PendingIntent.getBroadcast(
            context, notifId + 4, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun pendingActivity(
        context: Context,
        eventId: String,
        title: String,
        start: Long,
        notifId: Int,
        action: String
    ): PendingIntent {
        val i = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_EVENT_ID, eventId)
            putExtra(MainActivity.EXTRA_TITLE, title)
            putExtra(MainActivity.EXTRA_START, start)
            putExtra(MainActivity.EXTRA_ACTION, action)
        }
        return PendingIntent.getActivity(
            context, notifId, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}