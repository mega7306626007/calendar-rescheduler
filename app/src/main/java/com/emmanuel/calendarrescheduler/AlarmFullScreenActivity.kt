package com.emmanuel.calendarrescheduler

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmFullScreenActivity : AppCompatActivity() {

    private var ringtone: Ringtone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_fullscreen)
        supportActionBar?.hide()

        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val eventId = intent.getStringExtra(MainActivity.EXTRA_EVENT_ID) ?: run { finish(); return }
        val title = intent.getStringExtra(MainActivity.EXTRA_TITLE) ?: "Upcoming task"
        val start = intent.getLongExtra(MainActivity.EXTRA_START, 0L)
        val desc = intent.getStringExtra(MainActivity.EXTRA_DESC) ?: ""
        val notifId = intent.getIntExtra(MainActivity.NOTIF_ID, 0)

        findViewById<TextView>(R.id.alarm_title).text = title
        val sub = findViewById<TextView>(R.id.alarm_sub)
        val text = buildString {
            if (start > 0) append(SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault()).format(Date(start)))
            if (desc.isNotEmpty()) append("\n").append(desc)
        }
        sub.text = text

        findViewById<Button>(R.id.btn_alarm_open).setOnClickListener {
            stopAlarm()
            launchMain(eventId, start, MainActivity.ACTION_OPEN)
        }
        findViewById<Button>(R.id.btn_alarm_reschedule).setOnClickListener {
            stopAlarm()
            launchMain(eventId, start, MainActivity.ACTION_RESCHEDULE)
        }
        val snoozeMin = AlarmScheduler.getSnoozeMinutes(this)
        findViewById<Button>(R.id.btn_alarm_snooze).apply {
            setText("Snooze ${snoozeMin} min")
            setOnClickListener {
                AlarmScheduler.snoozeOneShot(this@AlarmFullScreenActivity, eventId, title, start, desc, notifId, snoozeMin)
                stopAlarm()
                try {
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.cancel(notifId)
                } catch (_: Exception) { }
                finish()
            }
        }
        findViewById<Button>(R.id.btn_alarm_dismiss).setOnClickListener {
            stopAlarm()
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(notifId)
            finish()
        }

        // Dismiss on swiping up from lock screen (already handled above on API 27+)

        val alarmUri = try { AlarmScheduler.soundUri(this) } catch (e: Exception) {
            android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
        }
        val rt = RingtoneManager.getRingtone(this, alarmUri)
        if (rt != null) {
            if (Build.VERSION.SDK_INT >= 28) {
                rt.audioAttributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            rt.play()
            ringtone = rt
        }
    }

    private fun launchMain(eventId: String, start: Long, action: String) {
        val i = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_EVENT_ID, eventId)
            putExtra(MainActivity.EXTRA_START, start)
            putExtra(MainActivity.EXTRA_ACTION, action)
        }
        startActivity(i)
        finish()
    }

    private fun stopAlarm() {
        try { ringtone?.stop() } catch (e: Exception) {}
        finish()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            try { ringtone?.stop() } catch (e: Exception) {}
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}