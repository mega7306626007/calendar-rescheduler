package com.emmanuel.calendarrescheduler

import android.Manifest
import android.accounts.Account
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.pdf.PdfDocument
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.work.Configuration as WorkConfiguration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.emmanuel.calendarrescheduler.data.AppDatabase
import com.emmanuel.calendarrescheduler.data.AuthManager
import com.emmanuel.calendarrescheduler.data.Backup
import com.emmanuel.calendarrescheduler.data.CalColors
import com.emmanuel.calendarrescheduler.data.CalEvent
import com.emmanuel.calendarrescheduler.data.Dates
import com.emmanuel.calendarrescheduler.data.DayData
import com.emmanuel.calendarrescheduler.data.MIN_SLOT_MIN
import com.emmanuel.calendarrescheduler.data.MoveResult
import com.emmanuel.calendarrescheduler.data.PushOutcome
import com.emmanuel.calendarrescheduler.data.Repository
import com.emmanuel.calendarrescheduler.data.RepeatRule
import com.emmanuel.calendarrescheduler.data.Rescheduler
import com.emmanuel.calendarrescheduler.data.SeriesEntity
import com.emmanuel.calendarrescheduler.data.SeriesMember
import com.emmanuel.calendarrescheduler.data.Slot
import com.emmanuel.calendarrescheduler.data.SlotEngine
import com.emmanuel.calendarrescheduler.data.UndoAction
import com.emmanuel.calendarrescheduler.data.WeekWindow
import com.emmanuel.calendarrescheduler.data.newSeriesId
import com.emmanuel.calendarrescheduler.data.repeatLabel
import com.emmanuel.calendarrescheduler.data.repeatStarts
import com.emmanuel.calendarrescheduler.ui.CalScheme
import com.emmanuel.calendarrescheduler.ui.CalTheme
import com.emmanuel.calendarrescheduler.ui.DarkCal
import com.emmanuel.calendarrescheduler.ui.DeepPurple
import com.emmanuel.calendarrescheduler.ui.Gold
import com.emmanuel.calendarrescheduler.ui.GoldDim
import com.emmanuel.calendarrescheduler.ui.HeaderA
import com.emmanuel.calendarrescheduler.ui.HeaderB
import com.emmanuel.calendarrescheduler.ui.InkOnGold
import com.emmanuel.calendarrescheduler.ui.Inter
import com.emmanuel.calendarrescheduler.ui.LightCal
import com.emmanuel.calendarrescheduler.ui.Playfair
import com.emmanuel.calendarrescheduler.ui.Violet
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.UserRecoverableAuthException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

sealed interface DialogKind {
    data object Create : DialogKind
    data class Event(val ev: CalEvent) : DialogKind
    data class Reason(val ev: CalEvent, val slotMs: Long?, val autoPush: Boolean) : DialogKind
    data class Strategy(val ev: CalEvent, val reason: String, val outcome: PushOutcome) : DialogKind
    data class Moved(val res: MoveResult) : DialogKind
    data object Search : DialogKind
    data object Onboard : DialogKind
    data class ConfirmDelete(val ev: CalEvent) : DialogKind
}

class MainActivity : ComponentActivity() {

    private lateinit var repo: Repository

    // ---------- state ----------
    private var mode by mutableStateOf("week")
    private var anchor by mutableLongStateOf(System.currentTimeMillis())
    private var win by mutableStateOf<WeekWindow?>(null)
    private var mainTab by mutableStateOf("cal")
    private var signedIn by mutableStateOf(true)
    private var status by mutableStateOf("")
    private var themeMode by mutableStateOf("dark")
    private var dialog by mutableStateOf<DialogKind?>(null)
    private var settingsTick by mutableIntStateOf(0)
    private var refreshing by mutableStateOf(false)

    // focus
    private var focusTotalMs by mutableLongStateOf(25 * 60_000L)
    private var focusLeftMs by mutableLongStateOf(25 * 60_000L)
    private var focusRunning by mutableStateOf(false)
    private var focusAmbient by mutableStateOf("rain")
    private var focusJob: Job? = null
    private var focusPlayer: MediaPlayer? = null

    // history tab data
    private var undoRows by mutableStateOf<List<UndoAction>>(emptyList())
    private var movesPerDay by mutableStateOf(IntArray(7))
    private var movesSpan by mutableIntStateOf(0)
    private var historyLine by mutableStateOf("")
    private var doneTodayN by mutableIntStateOf(0)
    private var doneWeekN by mutableIntStateOf(0)
    private var spanDays by mutableIntStateOf(7)

    // create-dialog draft (activity-level so voice input can fill it)
    private var createTitle by mutableStateOf("")

    // ---------- launchers ----------
    private val notifPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val pickLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME)?.let { name ->
                    AuthManager.saveAccount(this, Account(name, "com.google"))
                    signedIn = true
                    toast("Signed in as $name")
                    load()
                }
            }
        }

    private val authLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            toast("Authenticating…")
            load()
        }

    private val voiceLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val heard = result.data?.getStringArrayListExtra(
                    android.speech.RecognizerIntent.EXTRA_RESULTS
                )?.firstOrNull()
                if (!heard.isNullOrBlank()) createTitle = heard
            }
        }

    private val ringtonePickerLauncher =        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri: android.net.Uri? = if (Build.VERSION.SDK_INT >= 33) {
                    result.data?.getParcelableExtra(
                        android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                        android.net.Uri::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    result.data?.getParcelableExtra(
                        android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI
                    )
                }
                if (uri != null) {
                    AlarmScheduler.setSoundName(this, "custom:$uri")
                    PlanNotifier.recreateAlarmChannel(this)
                    settingsTick++
                    toast("Sound updated")
                }
            }
        }

    private val backupRestoreLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@registerForActivityResult
            lifecycleScope.launch {
                toast("Restoring…")
                try {
                    val json = withContext(Dispatchers.IO) {
                        contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                    }
                    if (json.isBlank()) {
                        toast("Empty file")
                        return@launch
                    }
                    val msg = withContext(Dispatchers.IO) {
                        Backup.importFromJson(this@MainActivity, repo.db, json)
                    }
                    themeMode = loadThemeMode()
                    spanDays = loadSpanDays()
                    settingsTick++
                    toast(msg)
                    load()
                    loadHistory()
                } catch (e: Exception) {
                    toast("Restore failed: ${e.message}")
                }
            }
        }

    // ---------- lifecycle ----------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            repo = Repository(this)
            PlanNotifier.createChannels(this)
            if (savedInstanceState != null) {
                mode = savedInstanceState.getString("mode") ?: "week"
                anchor = savedInstanceState.getLong("anchor", System.currentTimeMillis())
            }
            themeMode = loadThemeMode()
            spanDays = loadSpanDays()
            enableEdgeToEdge()
            setContent { CalTheme(dark = isDark()) { App() } }
            requestNotifyPermissionIfNeeded()
            setupBackgroundRefresh()
            handleIntent(intent)
            load()
            maybeOnboard()
        } catch (e: Exception) {
            Log.e("CalRes", "onCreate failed: ${e.message}", e)
            Toast.makeText(this, "App failed to start — ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("mode", mode)
        outState.putLong("anchor", anchor)
    }

    override fun onResume() {
        super.onResume()
        if (win != null) load()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onDestroy() {
        focusJob?.cancel()
        stopAmbient()
        super.onDestroy()
    }

    // ---------- prefs ----------
    private fun settings() = getSharedPreferences("settings", Context.MODE_PRIVATE)
    private fun loadThemeMode(): String = try {
        val p = settings()
        p.getString("theme_mode", null)
            ?: if (p.contains("dark_mode") && !p.getBoolean("dark_mode", true)) "light" else "dark"
    } catch (e: Exception) { "dark" }

    private fun applyThemeMode(m: String) {
        try { settings().edit().putString("theme_mode", m).apply() } catch (e: Exception) { }
        themeMode = m
    }

    private fun isDark(): Boolean = when (themeMode) {
        "light" -> false
        "auto" -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) !=
            Configuration.UI_MODE_NIGHT_NO
        else -> true
    }

    private fun scheme(): CalScheme = if (isDark()) DarkCal else LightCal
    private fun loadSpanDays(): Int = try {
        settings().getInt("stats_range", 7)
    } catch (e: Exception) { 7 }

    private fun focusPrefs() = getSharedPreferences("focus", Context.MODE_PRIVATE)

    // ---------- small helpers ----------
    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    private fun time12(ms: Long): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ms))

    private fun dayFmt(ms: Long): String = when (Dates.daysBetween(ms, System.currentTimeMillis())) {
        0 -> "Today"
        1 -> "Tomorrow"
        -1 -> "Yesterday"
        else -> SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(ms))
    }

    private fun durFmt(min: Int): String {
        val h = min / 60; val m = min % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    private fun fmtClock(ms: Long): String {
        val s = (ms / 1000).toInt()
        return String.format(Locale.getDefault(), "%02d:%02d", s / 60, s % 60)
    }

    private fun hex(s: String, fallback: Color = Color.Gray): Color = try {
        Color(android.graphics.Color.parseColor(s))
    } catch (e: Exception) { fallback }

    private fun blend(a: Color, b: Color, t: Float): Color {
        val k = t.coerceIn(0f, 1f)
        return Color(
            red = a.red + (b.red - a.red) * k,
            green = a.green + (b.green - a.green) * k,
            blue = a.blue + (b.blue - a.blue) * k,
            alpha = 1f
        )
    }

    private fun syncAge(): String {
        return try {
            val last = settings().getLong("last_sync", 0L)
            if (last <= 0L) return ""
            val mins = ((System.currentTimeMillis() - last) / 60000L).toInt()
            when {
                mins < 1 -> " · just synced"
                mins < 60 -> " · ${mins}m ago"
                else -> " · ${mins / 60}h ago"
            }
        } catch (e: Exception) { "" }
    }

    private fun notifEnabled(): Boolean {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        return if (Build.VERSION.SDK_INT >= 33) {
            nm.areNotificationsEnabled() &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else nm.areNotificationsEnabled()
    }

    private fun canExactAlarm(): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        return try {
            (getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager)
                .canScheduleExactAlarms()
        } catch (e: Exception) { true }
    }

    private fun promptExactAlarm() {
        if (Build.VERSION.SDK_INT >= 31 && !canExactAlarm()) {
            try {
                startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = android.net.Uri.parse("package:$packageName")
                })
            } catch (e: Exception) {
                toast("Enable exact alarms in Settings → Apps → Alarms & reminders")
            }
        }
    }

    private fun requestNotifyPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // ---------- data load ----------
    private fun load() {
        lifecycleScope.launch {
            try {
                win = withContext(Dispatchers.IO) { repo.buildWindow(mode, anchor) }
            } catch (e: UserRecoverableAuthException) {
                e.intent?.let { authLauncher.launch(it) }
            } catch (e: GoogleAuthException) {
                signedIn = false
                toast("Sign-in needed: ${e.message ?: "re-approve calendar access"}")
            } catch (e: Exception) {
                toast(e.message ?: "Error loading")
            }
        }
        if (signedIn) {
            lifecycleScope.launch {
                val n = withContext(Dispatchers.IO) {
                    try {
                        AlarmScheduler.scheduleDailyPlan(this@MainActivity)
                        repo.syncAlarms(this@MainActivity)
                    } catch (e: Exception) {
                        Log.d("CalRes", "syncAlarms failed: ${e.message}")
                        -1
                    }
                }
                if (n >= 0) {
                    val w = win
                    if (w != null) {
                        val evCount = w.days.sumOf { it.events.size }
                        status = "🔄 Synced · $evCount events · $n alarms set${syncAge()}" +
                            (if (!notifEnabled()) " · 🔕 Notifications off" else "") +
                            (if (!canExactAlarm()) " · ⚠️ Exact alarm off" else "")
                    }
                    try { TodayWidgetProvider.refresh(this@MainActivity) } catch (_: Exception) { }
                }
            }
        }
        if (!notifEnabled()) toast("🔕 Notifications off — enable in Settings for task reminders")
        if (!canExactAlarm()) toast("⚠️ Exact alarms off — enable in Settings → Alarms for on-time reminders")
    }

    private fun refreshStatusFromWindow() {
        val w = win ?: return
        val evCount = w.days.sumOf { it.events.size }
        status = "🔄 $evCount events · alarms active${syncAge()}" +
            (if (!notifEnabled()) " · 🔕 off" else "") +
            (if (!canExactAlarm()) " · ⚠️ exact off" else "")
    }

    private fun changeMode(m: String) {
        mode = m
        load()
    }

    private fun nav(dir: Int) {
        anchor = when (mode) {
            "day" -> anchor + dir * 86400000L
            "week", "agenda" -> anchor + dir * 7 * 86400000L
            else -> Calendar.getInstance().apply {
                timeInMillis = anchor
                set(Calendar.DAY_OF_MONTH, 15)
                add(Calendar.MONTH, dir)
            }.timeInMillis
        }
        load()
    }

    private fun setupBackgroundRefresh() {
        try {
            if (!WorkManager.isInitialized()) {
                WorkManager.initialize(this, WorkConfiguration.Builder().build())
            }
            val work = PeriodicWorkRequestBuilder<RefreshWorker>(12, TimeUnit.HOURS).build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "refresh_alarms", ExistingPeriodicWorkPolicy.KEEP, work
            )
        } catch (e: Exception) {
            Log.d("CalRes", "WorkManager setup failed: ${e.message}")
        }
    }

    private fun maybeOnboard() {
        val done = try {
            settings().getBoolean("onboarded", false)
        } catch (e: Exception) { true }
        if (!done) dialog = DialogKind.Onboard
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        val go = try { intent.data?.getQueryParameter("to") } catch (e: Exception) { null }
        if (go == "create") {
            lifecycleScope.launch {
                delay(600)
                dialog = DialogKind.Create
            }
            return
        }
        if (go == "today") {
            anchor = System.currentTimeMillis()
            mainTab = "cal"
            changeMode("day")
            return
        }
        if (go == "focus") {
            mainTab = "focus"
            return
        }
        val eventId = intent.getStringExtra(EXTRA_EVENT_ID)
        if (eventId != null) {
            lifecycleScope.launch {
                try {
                    val ev = withContext(Dispatchers.IO) { repo.api.getEvent(eventId) }
                    if (ev != null) dialog = DialogKind.Event(ev)
                    else toast("Event not found")
                } catch (e: Exception) { }
            }
        }
    }

    // ---------- focus engine ----------
    private fun focusStreakText(): String = try {
        val p = focusPrefs()
        "🔥 ${p.getInt("streak", 0)}-day streak · ${p.getInt("sessions", 0)} sessions"
    } catch (e: Exception) { "🔥 Start your first session" }

    private fun focusSessionDone() {
        try {
            val p = focusPrefs()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val last = p.getString("last_day", "")
            var streak = p.getInt("streak", 0)
            if (last != today) {
                val y = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Date(System.currentTimeMillis() - 86400000L))
                streak = if (last == y) streak + 1 else 1
            }
            p.edit().putInt("sessions", p.getInt("sessions", 0) + 1)
                .putInt("streak", streak).putString("last_day", today).apply()
        } catch (e: Exception) { }
    }

    private fun focusAmbientRes(): Int? = when (focusAmbient) {
        "rain" -> R.raw.focus_rain
        "ocean" -> R.raw.focus_ocean
        "piano_moonlight" -> R.raw.piano_moonlight
        "piano_furelise" -> R.raw.piano_furelise
        "piano_pathetique" -> R.raw.piano_pathetique
        "piano_turkish" -> R.raw.piano_turkish
        else -> null
    }

    private fun startAmbient() {
        stopAmbient()
        val res = focusAmbientRes() ?: return
        try {
            focusPlayer = MediaPlayer.create(this, res)?.apply {
                isLooping = true
                setVolume(0.6f, 0.6f)
                start()
            }
        } catch (e: Exception) { focusPlayer = null }
    }

    private fun stopAmbient() {
        try { focusPlayer?.stop() } catch (e: Exception) { }
        try { focusPlayer?.release() } catch (e: Exception) { }
        focusPlayer = null
    }

    private fun focusStart() {
        focusJob?.cancel()
        startAmbient()
        focusRunning = true
        focusJob = lifecycleScope.launch {
            while (focusLeftMs > 0) {
                delay(1000)
                focusLeftMs = (focusLeftMs - 1000).coerceAtLeast(0)
            }
            focusRunning = false
            stopAmbient()
            focusSessionDone()
            focusLeftMs = focusTotalMs
            toast("🎉 Focus complete! Streak updated.")
            try { sendTestNotification(silent = true) } catch (e: Exception) { }
        }
    }

    private fun focusPause() {
        focusJob?.cancel()
        focusJob = null
        focusRunning = false
        stopAmbient()
    }

    private fun focusReset() {
        focusJob?.cancel()
        focusJob = null
        focusRunning = false
        stopAmbient()
        focusLeftMs = focusTotalMs
    }

    // ---------- event actions ----------
    private fun handleMoveResult(res: MoveResult) {
        if (res.ok) {
            toast("Moved ✅")
            load()
            dialog = DialogKind.Moved(res)
        } else {
            toast(res.reason ?: "Move failed")
            load()
        }
    }

    private fun doMove(ev: CalEvent, slotMs: Long, reason: String) {
        lifecycleScope.launch {
            toast("Working…")
            val res = withContext(Dispatchers.IO) {
                Rescheduler.moveTo(repo.api, repo.db, ev, slotMs, reason)
            }
            handleMoveResult(res)
        }
    }

    private fun doAutoPush(ev: CalEvent, reason: String) {
        lifecycleScope.launch {
            toast("Working…")
            val outcome = withContext(Dispatchers.IO) { Rescheduler.planPush(repo.api, ev) }
            val full = outcome.full
            when {
                full != null -> {
                    val res = withContext(Dispatchers.IO) {
                        Rescheduler.moveTo(repo.api, repo.db, ev, full.start, reason)
                    }
                    handleMoveResult(res)
                }
                outcome.alternatives.isEmpty() -> {
                    toast("No free slot found in the next 14 days.")
                    load()
                }
                else -> dialog = DialogKind.Strategy(ev, reason, outcome)
            }
        }
    }

    private fun doStrategy(ev: CalEvent, reason: String, kind: String, target: Long, segments: List<com.emmanuel.calendarrescheduler.data.Segment>) {
        lifecycleScope.launch {
            toast("Working…")
            val res = withContext(Dispatchers.IO) {
                if (kind == "split") {
                    Rescheduler.applySplit(repo.api, repo.db, ev, segments, reason)
                } else {
                    val durMs = maxOf(ev.durationMin, MIN_SLOT_MIN) * 60000L
                    val rollover = target >= Dates.startOfDay(System.currentTimeMillis()) + 86400000L
                    Rescheduler.placeAt(repo.api, repo.db, ev, target, target + durMs, reason, rollover)
                }
            }
            handleMoveResult(res)
        }
    }

    private fun markDone(ev: CalEvent) {
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) { Rescheduler.markDone(repo.api, ev.id) }
            toast(if (ok) "Done! Great job 🎉" else "Could not mark done")
            load()
        }
    }

    private fun unmark(ev: CalEvent) {
        lifecycleScope.launch {
            val clean = ev.desc.lines().filterNot { it.contains("✅ Completed") }.joinToString("\n").trim()
            val ok = withContext(Dispatchers.IO) {
                try { repo.api.patchDescription(ev.id, clean); true } catch (e: Exception) { false }
            }
            toast(if (ok) "Unmarked — back on the list" else "Could not unmark")
            load()
        }
    }

    private fun duplicate(ev: CalEvent) {
        lifecycleScope.launch {
            toast("Duplicating…")
            val ok = withContext(Dispatchers.IO) {
                try {
                    repo.api.insertWithReminders(
                        ev.title + " (copy)", ev.start, ev.end,
                        ev.colorId.takeIf { it.isNotEmpty() }, ev.desc.takeIf { it.isNotEmpty() }
                    )
                    true
                } catch (e: Exception) { false }
            }
            toast(if (ok) "Duplicated ✅" else "Could not duplicate")
            load()
        }
    }

    private fun deleteEvent(ev: CalEvent) {
        lifecycleScope.launch {
            toast("Deleting…")
            withContext(Dispatchers.IO) { repo.api.deleteEventSync(ev.id) }
            toast("Deleted")
            load()
        }
    }

    private fun setLead(ev: CalEvent, mins: Long?) {
        lifecycleScope.launch {
            toast("Saving…")
            val ok = withContext(Dispatchers.IO) {
                try {
                    repo.api.patchDescription(ev.id, AlarmScheduler.withLeadToken(ev.desc, mins))
                    true
                } catch (e: Exception) { false }
            }
            if (ok) {
                toast("Reminder: ${if (mins == null) "default" else "$mins min"}")
                dialog = null
                load()
            } else toast("Could not save")
        }
    }

    private fun doCreate(
        title: String, dayMs: Long, hour: Int, minute: Int,
        durMin: Int, rule: RepeatRule, count: Int, colorId: String
    ) {
        lifecycleScope.launch {
            toast("Creating…")
            val made: Int = withContext(Dispatchers.IO) {
                try {
                    val starts = repeatStarts(rule, dayMs, hour, minute, count)
                    val durMs = durMin * 60000L
                    val created = mutableListOf<Pair<String, Long>>()
                    for (st in starts) {
                        val c = repo.api.insertWithReminders(
                            title, st, st + durMs,
                            colorId.takeIf { it.isNotEmpty() }, null
                        )
                        created.add(c.id to st)
                    }
                    if (created.size > 1) {
                        val sid = newSeriesId()
                        repo.db.seriesDao().insertSeries(
                            SeriesEntity(sid, title, System.currentTimeMillis())
                        )
                        repo.db.seriesDao().insertMembers(
                            created.map { (id, st) -> SeriesMember(sid, id, st) }
                        )
                        repo.db.undoDao().insert(
                            com.emmanuel.calendarrescheduler.data.UndoAction(
                                created = System.currentTimeMillis(),
                                originalId = null,
                                originalIsRecurring = false,
                                createdId = created.first().first,
                                createdIds = created.joinToString(",") { it.first },
                                title = "$title ×${created.size}"
                            )
                        )
                    }
                    created.size
                } catch (e: Exception) {
                    Log.d("CalRes", "create failed: ${e.message}")
                    0
                }
            }
            toast(if (made > 0) "Created ${if (made > 1) "$made events ✅" else "✅"}" else "Could not create")
            dialog = null
            createTitle = ""
            load()
        }
    }

    private fun deleteSeriesFollowing(ev: CalEvent) {
        lifecycleScope.launch {
            toast("Deleting series…")
            val n = withContext(Dispatchers.IO) {
                val m = repo.db.seriesDao().memberOf(ev.id) ?: return@withContext 0
                val following = repo.db.seriesDao().following(m.seriesId, ev.start)
                following.forEach { repo.api.deleteEventSync(it.eventId) }
                repo.db.seriesDao().deleteFollowing(m.seriesId, ev.start)
                if (repo.db.seriesDao().count(m.seriesId) == 0) {
                    repo.db.seriesDao().deleteSeries(m.seriesId)
                }
                following.size
            }
            toast(if (n > 0) "Deleted $n in series 🗑" else "Not in a series")
            dialog = null
            load()
        }
    }

    private fun exportBackupNow() {
        lifecycleScope.launch {
            toast("Building backup…")
            try {
                val file = withContext(Dispatchers.IO) {
                    Backup.exportToFile(this@MainActivity, repo.db)
                }
                val uri = FileProvider.getUriForFile(this@MainActivity, "$packageName.provider", file)
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "Share backup"))
            } catch (e: Exception) {
                toast("Backup failed: ${e.message}")
            }
        }
    }

    private fun loadTemplates(): List<Pair<String, Int>> = try {
        val raw = settings().getString("custom_templates", "[]") ?: "[]"
        val arr = org.json.JSONArray(raw)
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val t = o.optString("t", "")
            val m = o.optInt("m", 60)
            if (t.isNotBlank()) t to m.coerceIn(5, 480) else null
        }.take(8)
    } catch (e: Exception) { emptyList() }

    private fun saveTemplate(title: String, mins: Int) {
        try {
            val cur = loadTemplates().toMutableList()
            cur.removeAll { it.first.equals(title, ignoreCase = true) }
            cur.add(0, title to mins.coerceIn(5, 480))
            val arr = org.json.JSONArray()
            cur.take(8).forEach { (t, m) -> arr.put(org.json.JSONObject().put("t", t).put("m", m)) }
            settings().edit().putString("custom_templates", arr.toString()).apply()
        } catch (e: Exception) { }
    }

    private fun deleteTemplate(title: String) {
        try {
            val cur = loadTemplates().filterNot { it.first.equals(title, ignoreCase = true) }
            val arr = org.json.JSONArray()
            cur.forEach { (t, m) -> arr.put(org.json.JSONObject().put("t", t).put("m", m)) }
            settings().edit().putString("custom_templates", arr.toString()).apply()
        } catch (e: Exception) { }
    }

    // ---------- history helpers ----------
    private fun loadHistory() {
        lifecycleScope.launch {
            val rows = withContext(Dispatchers.IO) { repo.db.undoDao().recent() }
            undoRows = rows
            val span = spanDays
            val spanStart = Dates.startOfDay(System.currentTimeMillis() - (span - 1) * 86400000L)
            val perDay = IntArray(span)
            var spanMoves = 0
            rows.forEach { a ->
                val d = ((Dates.startOfDay(a.created) - spanStart) / 86400000L).toInt()
                if (d in 0 until span) { perDay[d]++; spanMoves++ }
            }
            movesPerDay = perDay
            movesSpan = spanMoves
            val doneW = withContext(Dispatchers.IO) {
                try {
                    repo.db.eventDao().getBetween(spanStart, spanStart + span * 86400000L)
                        .count { it.desc.contains("✅ Completed") }
                } catch (e: Exception) { 0 }
            }
            val doneT = withContext(Dispatchers.IO) {
                try {
                    val ts = Dates.startOfDay(System.currentTimeMillis())
                    repo.db.eventDao().getBetween(ts, ts + 86400000L)
                        .count { it.desc.contains("✅ Completed") }
                } catch (e: Exception) { 0 }
            }
            doneTodayN = doneT
            doneWeekN = doneW
            historyLine = "${focusStreakText()} · $spanMoves moves · $doneW done last $span days"
        }
    }

    private fun carryOver() {
        lifecycleScope.launch {
            toast("Carrying overdue tasks…")
            val result = withContext(Dispatchers.IO) {
                var moved = 0; var failed = 0
                try {
                    val now = System.currentTimeMillis()
                    val past = try {
                        repo.api.fetchEvents(now - 7 * 86400000L, now)
                    } catch (e: Exception) { emptyList() }
                    val todo = past.filter { it.end < now && !it.desc.contains("✅ Completed") && !it.protected }
                        .sortedBy { it.start }.take(20)
                    for (ev in todo) {
                        val src = Calendar.getInstance().apply { timeInMillis = ev.start }
                        var slot = Calendar.getInstance().apply {
                            timeInMillis = Dates.startOfDay(now)
                            set(Calendar.HOUR_OF_DAY, src.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, src.get(Calendar.MINUTE))
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        if (slot <= now + 120000) slot += 86400000L
                        val res = Rescheduler.moveTo(repo.api, repo.db, ev, slot, "Carry-over: wasn't finished in time")
                        if (res.ok) moved++ else failed++
                    }
                } catch (e: Exception) { }
                Pair(moved, failed)
            }
            toast("Moved ${result.first} · ${result.second} conflicted")
            load()
            loadHistory()
        }
    }

    private fun exportHistoryCsv() {
        lifecycleScope.launch {
            toast("Building CSV…")
            try {
                val file = withContext(Dispatchers.IO) {
                    val sb = StringBuilder("type,title,when,detail\n")
                    fun esc(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
                    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    for (a in repo.db.undoDao().recent()) {
                        sb.append("move,").append(esc(a.title))
                            .append(",").append(esc(fmt.format(Date(a.created))))
                            .append(",").append(esc(if (!a.createdIds.isNullOrBlank()) "${a.createdIds.split(",").size} parts" else "moved"))
                            .append("\n")
                    }
                    val now = System.currentTimeMillis()
                    val done = repo.db.eventDao().getBetween(now - 30 * 86400000L, now + 86400000L)
                        .filter { it.desc.contains("✅ Completed") }
                        .sortedBy { it.start }
                    for (c in done) {
                        sb.append("done,").append(esc(c.title))
                            .append(",").append(esc(fmt.format(Date(c.start))))
                            .append(",").append(esc("${c.durationMin} min"))
                            .append("\n")
                    }
                    File(cacheDir, "schedule-history.csv").apply { writeText(sb.toString()) }
                }
                val uri = FileProvider.getUriForFile(this@MainActivity, "$packageName.provider", file)
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "Share history CSV"))
            } catch (e: Exception) {
                toast("CSV failed: ${e.message}")
            }
        }
    }

    private fun exportWeekPdf() {
        lifecycleScope.launch {
            toast("Building PDF…")
            try {
                val w = withContext(Dispatchers.IO) { repo.buildWindow("week", System.currentTimeMillis()) }
                val file = withContext(Dispatchers.IO) {
                    val doc = PdfDocument()
                    val pw = 595
                    var pageNum = 1
                    var page = doc.startPage(PdfDocument.PageInfo.Builder(pw, 842, pageNum).create())
                    var y = 56f
                    val titleP = android.graphics.Paint().apply {
                        textSize = 22f; typeface = android.graphics.Typeface.DEFAULT_BOLD
                        color = android.graphics.Color.rgb(62, 24, 112)
                    }
                    val dayP = android.graphics.Paint().apply {
                        textSize = 15f; typeface = android.graphics.Typeface.DEFAULT_BOLD
                        color = android.graphics.Color.rgb(30, 20, 50)
                    }
                    val evP = android.graphics.Paint().apply {
                        textSize = 12f; color = android.graphics.Color.rgb(40, 40, 40)
                    }
                    val fmtD = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
                    page.canvas.drawText("My Week — Schedule", 40f, y, titleP)
                    y += 30f
                    for (d in w.days) {
                        if (y > 770f) {
                            doc.finishPage(page); pageNum++
                            page = doc.startPage(PdfDocument.PageInfo.Builder(pw, 842, pageNum).create())
                            y = 56f
                        }
                        page.canvas.drawText(fmtD.format(Date(d.millis)), 40f, y, dayP)
                        y += 20f
                        if (d.events.isEmpty()) {
                            page.canvas.drawText("— free —", 56f, y, evP); y += 17f
                        } else {
                            for (ev in d.events.sortedBy { it.start }) {
                                if (y > 790f) {
                                    doc.finishPage(page); pageNum++
                                    page = doc.startPage(PdfDocument.PageInfo.Builder(pw, 842, pageNum).create())
                                    y = 56f
                                }
                                page.canvas.drawText("${time12(ev.start)}  ${ev.title.take(60)}", 56f, y, evP)
                                y += 17f
                            }
                        }
                        y += 8f
                    }
                    doc.finishPage(page)
                    val out = File(cacheDir, "my-week.pdf")
                    out.outputStream().use { doc.writeTo(it) }
                    doc.close()
                    out
                }
                val uri = FileProvider.getUriForFile(this@MainActivity, "$packageName.provider", file)
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "Share your week"))
            } catch (e: Exception) {
                toast("PDF failed: ${e.message}")
            }
        }
    }

    // ---------- alert helpers ----------
    private fun previewSound() {
        try {
            val uri = AlarmScheduler.soundUri(this)
            val rt = android.media.RingtoneManager.getRingtone(this, uri)
            rt?.play()
            lifecycleScope.launch {
                delay(4000)
                try { rt?.stop() } catch (e: Exception) { }
            }
        } catch (e: Exception) { toast("Preview failed") }
    }

    private fun sendTestNotification(silent: Boolean = false) {
        PlanNotifier.createChannels(this)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            if (!silent) toast("Allow notifications first")
            return
        }
        try {
            val pi = android.app.PendingIntent.getActivity(
                this, 9999, Intent(this, MainActivity::class.java),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val b = androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_today)
                .setContentTitle(if (silent) "Focus complete 🎉" else "Test alert 🔔")
                .setContentText("If you see this, reminders work.")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pi)
            nm.notify(9999, b.build())
            if (!silent) toast("Test sent — check your shade")
        } catch (e: Exception) { toast("Test failed: ${e.message}") }
    }

    private fun testFullAlarm() {
        lifecycleScope.launch {
            val upcoming = withContext(Dispatchers.IO) {
                try {
                    val now = System.currentTimeMillis()
                    repo.api.fetchEvents(now, now + 7 * 86400000L)
                        .filter { it.start > now && !it.desc.contains("✅ Completed") }
                        .sortedBy { it.start }.firstOrNull()
                } catch (e: Exception) { null }
            }
            val fireAt = System.currentTimeMillis() + 8000
            val id = upcoming?.id ?: "test"
            val title = upcoming?.title ?: "Test alarm"
            val start = upcoming?.start ?: (System.currentTimeMillis() + 600000)
            val desc = upcoming?.desc ?: "Full-screen + sound check. No upcoming tasks found, so this is a demo."
            val nid = AlarmScheduler.stableId(id + "test")
            try {
                val am = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                val pi = android.app.PendingIntent.getBroadcast(
                    this@MainActivity, nid,
                    Intent(this@MainActivity, AlarmReceiver::class.java).apply {
                        putExtra(EXTRA_EVENT_ID, id)
                        putExtra(EXTRA_TITLE, title)
                        putExtra(EXTRA_START, start)
                        putExtra(EXTRA_DESC, desc)
                        putExtra(NOTIF_ID, nid)
                    },
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, fireAt, pi)
                toast("Demo alarm in ~8s — keep the app open")
            } catch (e: Exception) { toast("Test failed: ${e.message}") }
        }
    }

    private fun launchRingtonePicker() {
        try {
            val cur = try {
                android.net.Uri.parse(AlarmScheduler.getSoundName(this).removePrefix("custom:"))
            } catch (e: Exception) { null }
            ringtonePickerLauncher.launch(Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_ALARM)
                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TITLE, "Choose alarm sound")
                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                if (cur != null) putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, cur)
            })
        } catch (e: Exception) {
            toast("Device sound picker not available")
        }
    }

    private fun channelNeedsReset(): Boolean {
        if (Build.VERSION.SDK_INT < 26) return false
        return try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val ch = nm.getNotificationChannel(CHANNEL_ID) ?: return true
            ch.importance < android.app.NotificationManager.IMPORTANCE_HIGH
        } catch (e: Exception) { false }
    }

    private fun channelState(): String {
        if (Build.VERSION.SDK_INT < 26) return "legacy"
        return try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            when (nm.getNotificationChannel(CHANNEL_ID)?.importance ?: -1) {
                android.app.NotificationManager.IMPORTANCE_NONE -> "Off"
                android.app.NotificationManager.IMPORTANCE_MIN -> "Min"
                android.app.NotificationManager.IMPORTANCE_LOW -> "Quiet"
                android.app.NotificationManager.IMPORTANCE_DEFAULT -> "Default"
                android.app.NotificationManager.IMPORTANCE_HIGH -> "High"
                android.app.NotificationManager.IMPORTANCE_MAX -> "Urgent ✅"
                else -> "missing"
            }
        } catch (e: Exception) { "?" }
    }

    private fun openChannelSettings() {
        try {
            startActivity(Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
                putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, CHANNEL_ID)
            })
        } catch (e: Exception) { toast("Open Settings → Apps → Notifications manually") }
    }

    private fun fullScreenBlocked(): Boolean {
        if (Build.VERSION.SDK_INT < 34) return false
        return try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.canUseFullScreenIntent().not()
        } catch (e: Exception) { false }
    }

    private fun fullScreenState(): String = if (fullScreenBlocked()) "blocked" else "allowed ✅"

    private fun openFullScreenSettings() {
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startActivity(Intent(android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = android.net.Uri.parse("package:$packageName")
                })
            } else toast("Full-screen alarms available on this version")
        } catch (e: Exception) { toast("Open Settings → Apps → Special access manually") }
    }

    private fun dndOn(): Boolean = try {
        (getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager)
            .currentInterruptionFilter != android.app.NotificationManager.INTERRUPTION_FILTER_ALL
    } catch (e: Exception) { false }

    // ================= UI =================

    @Composable
    private fun App() {
        val s = scheme()
        settingsTick // recompose trigger for alert toggles
        Scaffold(
            topBar = { Header(s) },
            bottomBar = { TabBar(s) },
            containerColor = s.bg
        ) { pad ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(pad)
            ) {
                if (!signedIn) AuthBanner(s)
                if (mainTab == "cal") CalBar(s)
                AnimatedContent(
                    targetState = mainTab,
                    modifier = Modifier.weight(1f),
                    label = "tabs"
                ) { tab ->
                    when (tab) {
                        "focus" -> FocusTab(s)
                        "history" -> HistoryTab(s)
                        "alerts" -> AlertsTab(s)
                        else -> CalTab(s)
                    }
                }
                Text(
                    status, color = s.muted, fontSize = 11.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                )
            }
        }
        val d = dialog
        if (d != null) SheetHost(d, s)
    }

    @Composable
    private fun Header(s: CalScheme) {
        val today = remember(mainTab) {
            SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(HeaderA, HeaderB)))
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("📅 Schedule", fontSize = 20.sp, color = Gold, fontFamily = Playfair)
                Text(
                    today, fontSize = 11.5.sp, color = Color(0xFFBEAFD2),
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            IconButton(onClick = {
                if (!canExactAlarm()) promptExactAlarm()
                load()
            }) {
                Icon(Icons.Filled.Refresh, "Refresh", tint = Gold)
            }
            GoldButton("＋", { dialog = DialogKind.Create }, small = true)
        }
    }

    @Composable
    private fun AuthBanner(s: CalScheme) {
        Card(
            Modifier
                .fillMaxWidth()
                .padding(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF28223A))
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("👋 Sign in with your Google account to sync your calendar", color = s.soft, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                GoldButton("Sign in with Google", {
                    try { pickLauncher.launch(AuthManager.pickAccountIntent()) }
                    catch (e: Exception) { toast("Sign-in unavailable: ${e.message}") }
                })
            }
        }
    }

    @Composable
    private fun CalBar(s: CalScheme) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(s.calBar)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(s.row)
                    .padding(2.dp)
            ) {
                listOf("day" to "Day", "week" to "Week", "month" to "Month", "agenda" to "List").forEach { (k, label) ->
                    val active = mode == k
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (active) Gold else Color.Transparent)
                            .clickable { changeMode(k) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label, fontSize = 13.5.sp, fontWeight = FontWeight.Bold,
                            color = if (active) InkOnGold else Color(0xFF828CA0)
                        )
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavArrow("‹", s) { nav(-1) }
                Text(
                    titleFor(win), fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = s.text, textAlign = TextAlign.Center, maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                )
                NavArrow("›", s) { nav(1) }
                IconButton(onClick = { dialog = DialogKind.Search }) {
                    Icon(Icons.Filled.Search, "Search", tint = Gold)
                }
                GoldButton("Today", { anchor = System.currentTimeMillis(); load() }, small = true)
            }
        }
    }

    @Composable
    private fun NavArrow(label: String, s: CalScheme, onClick: () -> Unit) {
        Box(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(DeepPurple)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) { Text(label, fontSize = 20.sp, color = Gold) }
    }

    private fun titleFor(w: WeekWindow?): String {
        if (w == null) return ""
        return when (mode) {
            "day" -> {
                val d = w.days[0]
                SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(d.millis)) +
                    if (d.isToday) " · Today" else ""
            }
            "week", "agenda" -> {
                val a = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(w.days[0].millis))
                val b = Calendar.getInstance().apply { timeInMillis = w.days[6].millis }
                "$a – ${SimpleDateFormat("MMM d", Locale.getDefault()).format(b.time)}, ${b.get(Calendar.YEAR)}"
            }
            else -> {
                val d = w.days.first { it.inMonth }
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(d.millis))
            }
        }
    }

    @Composable
    private fun TabBar(s: CalScheme) {
        NavigationBar(containerColor = s.tab) {
            val items = listOf(
                Triple("cal", "Plan", Icons.Filled.CalendarToday),
                Triple("focus", "Focus", Icons.Filled.Timer),
                Triple("history", "Past", Icons.Filled.History),
                Triple("alerts", "Alerts", Icons.Filled.Notifications),
            )
            items.forEach { (k, label, icon) ->
                NavigationBarItem(
                    selected = mainTab == k,
                    onClick = {
                        mainTab = k
                        if (k == "cal") load()
                        if (k == "history") loadHistory()
                    },
                    icon = { Icon(icon, label) },
                    label = { Text(label, fontSize = 11.5.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = InkOnGold,
                        selectedTextColor = if (isDark()) Gold else Violet,
                        unselectedIconColor = s.muted,
                        unselectedTextColor = s.muted,
                        indicatorColor = Gold
                    )
                )
            }
        }
    }

    // ---------- calendar tab ----------
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun CalTab(s: CalScheme) {
        val w = win
        LaunchedEffect(w) {
            if (mainTab == "cal") refreshStatusFromWindow()
            if (refreshing) refreshing = false
        }
        val pullState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                refreshing = true
                load()
            },
            state = pullState,
            modifier = Modifier.fillMaxSize()
        ) {
            if (w == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading…", color = s.muted)
                }
                return@PullToRefreshBox
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                AnimatedContent(targetState = mode, label = "mode") { m ->
                    when (m) {
                        "day" -> DayView(w.days[0], s)
                        "week" -> WeekView(w, s)
                        "agenda" -> AgendaView(w, s)
                        else -> MonthView(w, s)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    @Composable
    private fun UpNextHero(ev: CalEvent, s: CalScheme) {
        val c = CalColors.of(ev.colorId)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF3E1870), Color(0xFF281450))))
                .clickable(enabled = !ev.protected) { dialog = DialogKind.Event(ev) }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(5.dp, 48.dp).background(hex(c.first)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("⚡ Up next", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Gold)
                Text(ev.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    dayFmt(ev.start) + " · " + time12(ev.start) + "–" + time12(ev.end),
                    fontSize = 12.5.sp, color = s.soft
                )
            }
        }
    }

    @Composable
    private fun DayView(d: DayData, s: CalScheme) {
        val now = System.currentTimeMillis()
        val upcoming = d.events.filter { it.end > now && !it.desc.contains("✅ Completed") }.sortedBy { it.start }
        if (upcoming.isNotEmpty()) UpNextHero(upcoming.first(), s)
        else Text(
            if (d.isToday) "✨ Today is clear — add something great with ＋ above" else "🌙 Nothing scheduled this day",
            fontSize = 14.sp, color = s.muted, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(28.dp)
        )
        val totalH = 1520.dp
        val pxPerMin = 1520f / 1440f
        val density = LocalDensity.current
        val pxPerMinPx = with(density) { pxPerMin.dp.toPx() }
        Text(
            "Long-press + drag an event to move it ↕", fontSize = 11.5.sp, color = s.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
        )
        Box(Modifier.fillMaxWidth().height(totalH)) {
            for (h in 5..23) {
                val top = (h * 60 * pxPerMin).dp
                Box(Modifier.offset(y = top).fillMaxWidth().height(1.dp).background(s.line))
                Text(
                    timeH(h), fontSize = 10.sp, color = s.muted,
                    modifier = Modifier.offset(y = top).padding(start = 6.dp)
                )
            }
            for (ev in d.events) {
                val top = ((ev.start - d.millis) / 60000f * pxPerMin).dp
                val hgt = (((ev.end - ev.start) / 60000f * pxPerMin).dp).coerceAtLeast(46.dp)
                DraggableEventBlock(ev, s, d.millis, pxPerMinPx, Modifier.offset(y = top).height(hgt))
            }
            if (d.isToday) {
                val c = Calendar.getInstance()
                val nowDp = ((c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)) * pxPerMin).dp
                Box(Modifier.offset(y = nowDp).fillMaxWidth().height(2.dp).background(Gold))
            }
        }
    }

    private fun timeH(h: Int): String = when (h) {
        0 -> "12a"; 12 -> "12p"; in 13..23 -> "${h - 12}p"; else -> "${h}a"
    }

    @Composable
    private fun EventBlock(ev: CalEvent, s: CalScheme, mod: Modifier = Modifier) {
        val c = CalColors.of(ev.colorId)
        Column(
            mod
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(blend(s.card, hex(c.first), 0.32f))
                .clickable(enabled = !ev.protected) { dialog = DialogKind.Event(ev) }
        ) {
            Box(Modifier.fillMaxWidth().height(5.dp).background(hex(c.first)))
            Text(
                time12(ev.start) + "–" + time12(ev.end), fontSize = 12.sp,
                fontWeight = FontWeight.Bold, color = hex(c.first), maxLines = 1,
                modifier = Modifier.padding(start = 8.dp, top = 6.dp, end = 6.dp)
            )
            Text(
                ev.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = s.text,
                maxLines = 3, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp, end = 6.dp, bottom = 6.dp)
            )
        }
    }

    @Composable
    private fun DraggableEventBlock(
        ev: CalEvent, s: CalScheme, dayMs: Long, pxPerMin: Float, mod: Modifier = Modifier
    ) {
        var dy by remember { mutableFloatStateOf(0f) }
        var dragging by remember { mutableStateOf(false) }
        val haptic = LocalHapticFeedback.current
        EventBlock(
            ev, s,
            mod
                .graphicsLayer {
                    translationY = dy
                    shadowElevation = if (dragging) 14f else 0f
                }
                .pointerInput(ev.id, ev.start) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            dragging = true
                            dy = 0f
                        },
                        onDragCancel = {
                            dragging = false
                            dy = 0f
                        },
                        onDragEnd = {
                            dragging = false
                            val mins = (dy / pxPerMin / 5).roundToInt() * 5
                            val durMs = (ev.end - ev.start).coerceAtLeast(15 * 60000L)
                            val minStart = dayMs + 5 * 3600000L
                            val maxStart = (SlotEngine.dayEndLimit(dayMs) - durMs)
                                .coerceAtLeast(minStart)
                            var ns = (ev.start + mins * 60000L).coerceIn(minStart, maxStart)
                            ns = dayMs + ((ns - dayMs) / 300000L) * 300000L
                            dy = 0f
                            if (abs(ns - ev.start) >= 60000L) {
                                toast("Dropped at ${time12(ns)} — pick a reason")
                                dialog = DialogKind.Reason(ev, ns, false)
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dy += dragAmount.y
                        }
                    )
                }
        )
    }

    @Composable
    private fun WeekView(w: WeekWindow, s: CalScheme) {
        Row(Modifier.fillMaxWidth()) {
            for (d in w.days) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(d.weekday, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = s.muted)
                    Box(
                        Modifier
                            .padding(top = 2.dp)
                            .size(26.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (d.isToday) Gold else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            d.monthDay.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            color = if (d.isToday) InkOnGold else s.soft
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        val bodyH = 760.dp
        val pxMin = 760f / (16f * 60f)
        Row(
            Modifier
                .fillMaxWidth()
                .height(bodyH)
                .clip(RoundedCornerShape(8.dp))
                .background(s.calBar)
        ) {
            for (d in w.days) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(bodyH)
                        .background(if (d.isToday) s.row else s.card)
                ) {
                    for (ev in d.events) {
                        val top = ((ev.start - d.millis) / 60000f * pxMin).dp
                            .coerceIn(0.dp, bodyH)
                        val hgt = (((ev.end - ev.start) / 60000f * pxMin).dp)
                            .coerceIn(30.dp, bodyH - top)
                        EventBlock(ev, s, Modifier.offset(y = top).height(hgt))
                    }
                }
            }
        }
    }

    @Composable
    private fun AgendaView(w: WeekWindow, s: CalScheme) {
        val now = System.currentTimeMillis()
        var shown = 0
        for (d in w.days) {
            val evs = d.events.sortedBy { it.start }
            if (evs.isEmpty()) continue
            Text(
                (if (d.isToday) "● " else "") +
                    SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(d.millis)),
                fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = if (d.isToday) s.goldTxt else s.soft,
                modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp)
            )
            for (ev in evs) {
                val c = CalColors.of(ev.colorId)
                val done = ev.desc.contains("✅ Completed")
                val past = ev.end <= now
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(blend(s.card, hex(c.first), 0.28f))
                        .clickable(enabled = !ev.protected) { dialog = DialogKind.Event(ev) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(5.dp, 34.dp).background(hex(c.first)))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f).let { if (done || past) it else it }) {
                        Text(
                            (if (done) "✅ " else "") + ev.title, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, color = s.text, maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            time12(ev.start) + "–" + time12(ev.end) + " · " + durFmt(ev.durationMin),
                            fontSize = 12.sp, color = hex(c.first), maxLines = 1
                        )
                    }
                }
                shown++
            }
        }
        if (shown == 0) Text(
            "✨ Nothing coming up — enjoy the calm or add something with ＋",
            fontSize = 14.sp, color = s.muted, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(32.dp)
        )
    }

    @Composable
    private fun MonthView(w: WeekWindow, s: CalScheme) {
        Row(Modifier.fillMaxWidth()) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { h ->
                Text(
                    h, fontSize = 10.5.sp, color = s.muted, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
                )
            }
        }
        var i = 0
        while (i < w.days.size) {
            Row(Modifier.fillMaxWidth()) {
                for (j in 0 until 7) {
                    val idx = i + j
                    if (idx >= w.days.size) break
                    MonthCell(w.days[idx], s, Modifier.weight(1f))
                }
            }
            i += 7
        }
    }

    @Composable
    private fun MonthCell(d: DayData, s: CalScheme, mod: Modifier = Modifier) {
        val total = d.events.size
        val done = d.events.count { it.desc.contains("✅ Completed") }
        val ratio = if (total == 0) 0f else done.toFloat() / total
        val cellBg = blend(if (d.isToday) s.row else s.card, s.heat, ratio * 0.55f)
        Column(
            mod
                .padding(1.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(cellBg)
                .clickable { if (d.inMonth) { anchor = d.millis; changeMode("day") } }
                .padding(3.dp)
                .height(72.dp)
        ) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (d.isToday) Gold else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (d.monthDay != 0) d.monthDay.toString() else "", fontSize = 12.sp,
                    fontWeight = if (d.isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (d.isToday) InkOnGold else s.muted
                )
            }
            d.events.take(3).forEach { ev ->
                val c = CalColors.of(ev.colorId)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 3.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0xFF2C2742))
                        .clickable(enabled = !ev.protected) { dialog = DialogKind.Event(ev) }
                        .padding(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(6.dp, 22.dp).background(hex(c.first)))
                    Spacer(Modifier.width(5.dp))
                    Column(Modifier.weight(1f)) {
                        Text(time12(ev.start), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = hex(c.first), maxLines = 1)
                        Text(ev.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = s.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (d.events.size > 3) Text(
                "+${d.events.size - 3} more", fontSize = 10.sp, color = s.goldTxt,
                fontWeight = FontWeight.Bold
            )
        }
    }

    // ---------- focus tab ----------
    @Composable
    private fun FocusTab(s: CalScheme) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(10.dp)
        ) {
            Text("⏱ Focus", fontSize = 18.sp, fontFamily = Playfair, color = s.goldTxt,
                modifier = Modifier.padding(8.dp))
            Text(
                focusStreakText(), fontSize = 13.5.sp, fontWeight = FontWeight.Bold,
                color = s.soft, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF3E1870), Color(0xFF1E123C))))
                    .padding(vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(fmtClock(focusLeftMs), fontSize = 52.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    if (focusRunning) "Stay with it 🌊" else "Pick a length and begin ✨",
                    fontSize = 13.sp, color = s.soft, modifier = Modifier.padding(top = 6.dp)
                )
            }
            SectionLabel("Session length", s)
            Row(Modifier.fillMaxWidth()) {
                listOf(15L, 25L, 50L).forEach { m ->
                    val active = focusTotalMs == m * 60_000L
                    ChipButton(
                        "${m}m", active, s, Modifier.weight(1f),
                        enabled = !focusRunning
                    ) {
                        focusTotalMs = m * 60_000L
                        focusLeftMs = focusTotalMs
                    }
                }
            }
            SectionLabel("Background sound", s)
            Row(Modifier.fillMaxWidth()) {
                listOf("off" to "Silent", "rain" to "🌧 Rain", "ocean" to "🐚 Ocean").forEach { (key, label) ->
                    ChipButton(label, focusAmbient == key, s, Modifier.weight(1f)) {
                        focusAmbient = key
                        if (focusRunning) startAmbient()
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                listOf(
                    "piano_moonlight" to "🌙 Moonlight", "piano_furelise" to "✨ Für Elise",
                    "piano_pathetique" to "🔥 Pathétique", "piano_turkish" to "🎠 Turkish"
                ).forEach { (key, label) ->
                    ChipButton(label, focusAmbient == key, s, Modifier.weight(1f), fontSize = 10.sp) {
                        focusAmbient = key
                        if (focusRunning) startAmbient()
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                if (focusRunning) {
                    GhostButton("⏸ Pause", { focusPause() }, s, Modifier.weight(1f))
                } else {
                    GoldButton(
                        if (focusLeftMs < focusTotalMs) "▶ Resume" else "▶ Start",
                        { focusStart() }, modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.width(8.dp))
                GhostButton("↺ Reset", { focusReset() }, s, Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
        }
        LaunchedEffect(mainTab) { status = "⏱ " + focusStreakText() }
    }

    // ---------- history tab ----------
    @Composable
    private fun HistoryTab(s: CalScheme) {
        LaunchedEffect(mainTab) { loadHistory() }
        val goal = try { settings().getInt("daily_goal", 5) } catch (e: Exception) { 5 }
        val wgoal = try { settings().getInt("weekly_goal", 20) } catch (e: Exception) { 20 }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(10.dp)
        ) {
            Text("📊 Your progress", fontSize = 18.sp, fontFamily = Playfair, color = s.goldTxt,
                modifier = Modifier.padding(8.dp))
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = s.row)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Moves chart:", fontSize = 12.5.sp, color = s.muted, modifier = Modifier.weight(1f))
                        listOf(7 to "7d", 30 to "30d").forEach { (days, label) ->
                            ChipButton(label, spanDays == days, s, Modifier.padding(start = 4.dp)) {
                                try { settings().edit().putInt("stats_range", days).apply() } catch (e: Exception) { }
                                spanDays = days
                                loadHistory()
                            }
                        }
                    }
                    MovesChart(s)
                    Text(historyLine.ifEmpty { focusStreakText() }, fontSize = 13.sp, color = Color(0xFFC8B9E1),
                        modifier = Modifier.padding(top = 8.dp))
                    GoalRow("🎯 Today: $doneTodayN / $goal done", doneTodayN, goal, s, onMinus = {
                        settings().edit().putInt("daily_goal", maxOf(1, goal - 1)).apply()
                        settingsTick++
                    }, onPlus = {
                        settings().edit().putInt("daily_goal", minOf(30, goal + 1)).apply()
                        settingsTick++
                    })
                    GoalRow("📅 Week: $doneWeekN / $wgoal done", doneWeekN, wgoal, s, onMinus = {
                        settings().edit().putInt("weekly_goal", maxOf(1, wgoal - 1)).apply()
                        settingsTick++
                    }, onPlus = {
                        settings().edit().putInt("weekly_goal", minOf(100, wgoal + 1)).apply()
                        settingsTick++
                    })
                    Spacer(Modifier.height(8.dp))
                    GhostButton("→ Carry overdue undone to today", { carryOver() }, s)
                }
            }
            Text("↩ Recent changes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = s.text,
                modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 4.dp))
            if (undoRows.isEmpty()) {
                Text("✨ Nothing to undo yet.\nMoved tasks will appear here.", color = s.muted,
                    fontSize = 14.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(32.dp))
            } else {
                undoRows.forEach { a ->
                    val whenText = SimpleDateFormat("MMM d · h:mm a", Locale.getDefault()).format(Date(a.created))
                    val parts = if (!a.createdIds.isNullOrBlank()) "  ·  ${a.createdIds.split(",").size} parts" else ""
                    SlotButton("${a.title}\n$whenText$parts", s) {
                        lifecycleScope.launch {
                            toast("Restoring…")
                            val ok = withContext(Dispatchers.IO) { Rescheduler.undo(repo.api, repo.db, a) }
                            toast(if (ok) "Restored ✅" else "Could not undo (event already gone?)")
                            loadHistory()
                            load()
                        }
                    }
                }
            }
            GhostButton("📄 Export this week as PDF", { exportWeekPdf() }, s)
            GhostButton("📊 Export history as CSV", { exportHistoryCsv() }, s)
            Text("💾 Backup", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = s.text,
                modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 4.dp))
            GhostButton("💾 Backup now (share JSON)", { exportBackupNow() }, s)
            GhostButton("📥 Restore from file", { backupRestoreLauncher.launch("application/json") }, s)
            ToggleRow(
                "Auto-backup weekly", "Keeps the last 4 backups on this device",
                Backup.isAutoEnabled(this@MainActivity), s
            ) {
                Backup.setAutoEnabled(this@MainActivity, it)
                settingsTick++
                toast(if (it) "Auto-backup on" else "Auto-backup off")
            }
            Spacer(Modifier.height(16.dp))
        }
        LaunchedEffect(mainTab) { status = "↩ Tap a change to revert it" }
    }

    @Composable
    private fun MovesChart(s: CalScheme) {
        val data = movesPerDay
        val peak = maxOf(1, data.maxOrNull() ?: 1)
        val dayNames = arrayOf("M", "T", "W", "T", "F", "S", "S")
        val spanStart = Dates.startOfDay(System.currentTimeMillis() - (data.size - 1) * 86400000L)
        val firstDow = Calendar.getInstance().apply { timeInMillis = spanStart }.get(Calendar.DAY_OF_WEEK)
        val labelEvery = if (data.size > 10) 5 else 1
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(top = 8.dp)
        ) { /* bars drawn below via layout for text labels */ }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            for (i in data.indices) {
                val h = maxOf(6, (data[i] * 84 / peak)).dp
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(if (data[i] > 0) data[i].toString() else "", fontSize = 10.sp, color = s.goldTxt)
                    Box(
                        Modifier
                            .width(if (data.size > 10) 8.dp else 16.dp)
                            .height(h)
                            .background(if (data[i] > 0) Gold else Color(0xFF3C3458))
                    )
                    val labelIdx = ((firstDow - 2 + i) % 7 + 7) % 7
                    Text(if (i % labelEvery == 0) dayNames[labelIdx] else "", fontSize = 10.sp, color = s.muted)
                }
            }
        }
    }

    @Composable
    private fun GoalRow(
        label: String, done: Int, goal: Int, s: CalScheme,
        onMinus: () -> Unit, onPlus: () -> Unit
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = s.text, modifier = Modifier.weight(1f))
            StepBtn("−", s, onMinus)
            Spacer(Modifier.width(6.dp))
            StepBtn("＋", s, onPlus)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(s.btn)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(maxOf(0.02f, minOf(1f, done.toFloat() / maxOf(1, goal))))
                    .height(10.dp)
                    .background(Gold)
            )
        }
    }

    @Composable
    private fun StepBtn(label: String, s: CalScheme, onClick: () -> Unit) {
        Box(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(s.btn)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) { Text(label, fontSize = 16.sp, color = s.goldTxt) }
    }

    // ---------- alerts tab ----------
    @Composable
    private fun AlertsTab(s: CalScheme) {
        settingsTick
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(10.dp)
        ) {
            Text("🔔 Alerts", fontSize = 18.sp, fontFamily = Playfair, color = s.goldTxt,
                modifier = Modifier.padding(8.dp))
            Text("🎨 Theme", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = s.text,
                modifier = Modifier.padding(start = 8.dp, top = 10.dp, bottom = 2.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf("dark" to "🌙 Dark", "light" to "☀ Light", "auto" to "🌓 Auto").forEach { (key, label) ->
                    ChipButton(label, themeMode == key, s, Modifier.weight(1f).padding(horizontal = 4.dp)) {
                        applyThemeMode(key)
                        toast("$label theme")
                    }
                }
            }
            val leadMin = AlarmScheduler.getReminderMinutes(this@MainActivity)
            ToggleRow("Task reminders", "Alarm before each task", AlarmScheduler.isAlarmsEnabled(this@MainActivity), s) {
                AlarmScheduler.setAlarmsEnabled(this@MainActivity, it); settingsTick++; load()
            }
            ToggleRow("Daily plan (6:05 AM)", "Today's agenda each morning", AlarmScheduler.isPlanEnabled(this@MainActivity), s) {
                AlarmScheduler.setPlanEnabled(this@MainActivity, it); settingsTick++
            }
            ToggleRow("Missed-task nudges", "Gentle poke for undone tasks", AlarmScheduler.isNudgesEnabled(this@MainActivity), s) {
                AlarmScheduler.setNudgesEnabled(this@MainActivity, it); settingsTick++
            }
            ToggleRow("Vibration", "Buzz with the alarm", AlarmScheduler.isVibrateEnabled(this@MainActivity), s) {
                AlarmScheduler.setVibrateEnabled(this@MainActivity, it)
                PlanNotifier.recreateAlarmChannel(this@MainActivity); settingsTick++
            }
            Text("Remind me before each task", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = s.text,
                modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 2.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf(5L, 10L, 15L, 30L).forEach { m ->
                    ChipButton("${m}m", m == leadMin, s, Modifier.weight(1f).padding(horizontal = 4.dp)) {
                        AlarmScheduler.setReminderMinutes(this@MainActivity, m)
                        toast("Reminders $m min before each task"); settingsTick++; load()
                    }
                }
            }
            Text("Snooze length", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = s.text,
                modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 2.dp))
            val snoozeMin = AlarmScheduler.getSnoozeMinutes(this@MainActivity)
            Row(Modifier.fillMaxWidth()) {
                listOf(5L, 10L, 15L, 30L).forEach { m ->
                    ChipButton("${m}m", m == snoozeMin, s, Modifier.weight(1f).padding(horizontal = 4.dp)) {
                        AlarmScheduler.setSnoozeMinutes(this@MainActivity, m); settingsTick++
                    }
                }
            }
            ToggleRow("🌙 Quiet hours", "Gentle notification, no full-screen siren", AlarmScheduler.isQuietEnabled(this@MainActivity), s) {
                AlarmScheduler.setQuietEnabled(this@MainActivity, it); settingsTick++
            }
            if (AlarmScheduler.isQuietEnabled(this@MainActivity)) {
                HourStepper(
                    "From ${AlarmScheduler.getQuietFrom(this@MainActivity)}:00 → ${AlarmScheduler.getQuietTo(this@MainActivity)}:00",
                    s,
                    onFromMinus = {
                        AlarmScheduler.setQuietFrom(this@MainActivity, (AlarmScheduler.getQuietFrom(this@MainActivity) + 23) % 24)
                        settingsTick++
                    },
                    onFromPlus = {
                        AlarmScheduler.setQuietFrom(this@MainActivity, (AlarmScheduler.getQuietFrom(this@MainActivity) + 1) % 24)
                        settingsTick++
                    },
                    onToMinus = {
                        AlarmScheduler.setQuietTo(this@MainActivity, (AlarmScheduler.getQuietTo(this@MainActivity) + 23) % 24)
                        settingsTick++
                    },
                    onToPlus = {
                        AlarmScheduler.setQuietTo(this@MainActivity, (AlarmScheduler.getQuietTo(this@MainActivity) + 1) % 24)
                        settingsTick++
                    }
                )
            }
            ToggleRow("⏰ Second reminder", "Extra early heads-up per task", AlarmScheduler.isSecondEnabled(this@MainActivity), s) {
                AlarmScheduler.setSecondEnabled(this@MainActivity, it); settingsTick++; load()
            }
            if (AlarmScheduler.isSecondEnabled(this@MainActivity)) {
                val secMin = AlarmScheduler.getSecondMinutes(this@MainActivity)
                Row(Modifier.fillMaxWidth()) {
                    listOf(15L, 30L, 60L, 120L).forEach { m ->
                        ChipButton(if (m >= 60) "${m / 60}h" else "${m}m", m == secMin, s, Modifier.weight(1f).padding(horizontal = 4.dp)) {
                            AlarmScheduler.setSecondMinutes(this@MainActivity, m); settingsTick++; load()
                        }
                    }
                }
            }
            Text("Alarm sound", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = s.text,
                modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 2.dp))
            Text("Current: " + AlarmScheduler.soundLabel(this@MainActivity), fontSize = 12.5.sp, color = s.muted,
                modifier = Modifier.padding(start = 8.dp, bottom = 6.dp))
            val curSound = AlarmScheduler.getSoundName(this@MainActivity)
            AlarmScheduler.SOUND_OPTIONS.forEachIndexed { i, name ->
                val active = name == curSound || (name == "device_pick" && curSound.startsWith("custom:"))
                SlotButton((if (active) "▶ " else "") + AlarmScheduler.SOUND_LABELS[i], s, gold = active) {
                    if (name == "device_pick") launchRingtonePicker()
                    else {
                        AlarmScheduler.setSoundName(this@MainActivity, name)
                        PlanNotifier.recreateAlarmChannel(this@MainActivity)
                        toast("Sound: ${AlarmScheduler.SOUND_LABELS[i]}"); settingsTick++
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                GhostButton("▶ Preview sound", { previewSound() }, s, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                GoldButton("🔔 Test alert", { sendTestNotification() }, modifier = Modifier.weight(1f))
            }
            GoldButton("🚨 Test full-screen alarm", { testFullAlarm() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            Text("🚨 Alarm health", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = s.text,
                modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 2.dp))
            HealthRow("Full-screen alarm: ${fullScreenState()}", "Android 14+ can block takeover alerts", s) { openFullScreenSettings() }
            HealthRow("Task channel: ${channelState()}", "Must be Urgent for full-screen alarms", s) { openChannelSettings() }
            if (channelNeedsReset()) {
                GoldButton("🔧 Reset alarm channel to Urgent", {
                    PlanNotifier.recreateAlarmChannel(this@MainActivity); settingsTick++
                }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
            HealthRow(
                "Do Not Disturb: ${if (dndOn()) "ON — alarms may hush" else "Off"}",
                "Alarms bypass DND only if allowed", s
            ) {
                try { startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }
                catch (e: Exception) { toast("Open Settings → Sound → Do Not Disturb") }
            }
            Text("Recently fired", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = s.text,
                modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 2.dp))
            val fired = remember(settingsTick) { AlarmScheduler.readFired(this@MainActivity) }
            if (fired.isEmpty()) Text("No alarms fired yet.", color = s.muted, fontSize = 13.sp,
                modifier = Modifier.padding(start = 8.dp))
            fired.forEach { (t, at) ->
                Text("• $t — ${SimpleDateFormat("MMM d · h:mm a", Locale.getDefault()).format(Date(at))}",
                    color = s.soft, fontSize = 12.5.sp, modifier = Modifier.padding(start = 12.dp, bottom = 2.dp))
            }
            Spacer(Modifier.height(16.dp))
        }
        LaunchedEffect(mainTab) { status = "🔔 Tune every notification" }
    }

    @Composable
    private fun ToggleRow(title: String, sub: String, checked: Boolean, s: CalScheme, onToggle: (Boolean) -> Unit) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(s.row)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = s.text)
                Text(sub, fontSize = 12.sp, color = s.muted)
            }
            Switch(checked = checked, onCheckedChange = onToggle)
        }
    }

    @Composable
    private fun HealthRow(title: String, sub: String, s: CalScheme, onClick: () -> Unit) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(s.row)
                .clickable(onClick = onClick)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = s.text)
                Text(sub, fontSize = 12.sp, color = s.muted)
            }
        }
    }

    @Composable
    private fun HourStepper(
        label: String, s: CalScheme,
        onFromMinus: () -> Unit, onFromPlus: () -> Unit,
        onToMinus: () -> Unit, onToPlus: () -> Unit
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
            Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = s.text,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            StepBtn("−", s, onFromMinus)
            Spacer(Modifier.width(6.dp))
            StepBtn("＋", s, onFromPlus)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
            Text("Until hour (tap −/＋)", fontSize = 12.sp, color = s.muted, modifier = Modifier.weight(1f))
            StepBtn("−", s, onToMinus)
            Spacer(Modifier.width(6.dp))
            StepBtn("＋", s, onToPlus)
        }
    }

    // ---------- shared widgets ----------
    @Composable
    private fun SectionLabel(text: String, s: CalScheme) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = s.text,
            modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 2.dp))
    }

    @Composable
    private fun GoldButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, small: Boolean = false) {
        Button(
            onClick = onClick, modifier = modifier,
            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = InkOnGold),
            shape = RoundedCornerShape(10.dp),
            contentPadding = if (small) androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                else ButtonDefaults.ContentPadding
        ) { Text(label, fontWeight = FontWeight.Bold, fontSize = if (small) 12.sp else 15.sp) }
    }

    @Composable
    private fun GhostButton(label: String, onClick: () -> Unit, s: CalScheme, modifier: Modifier = Modifier) {
        Button(
            onClick = onClick, modifier = modifier,
            colors = ButtonDefaults.buttonColors(containerColor = s.btn, contentColor = s.goldTxt),
            shape = RoundedCornerShape(12.dp)
        ) { Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
    }

    @Composable
    private fun SlotButton(label: String, s: CalScheme, gold: Boolean = false, onClick: () -> Unit) {
        Button(
            onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (gold) Gold else s.btn,
                contentColor = if (gold) InkOnGold else s.text
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(label, fontSize = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
        }
    }

    @Composable
    private fun ChipButton(
        label: String, active: Boolean, s: CalScheme,
        modifier: Modifier = Modifier, fontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
        enabled: Boolean = true, onClick: () -> Unit
    ) {
        Button(
            onClick = onClick, enabled = enabled,
            modifier = modifier.padding(horizontal = 2.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (active) Gold else s.btn,
                contentColor = if (active) InkOnGold else s.goldTxt,
                disabledContainerColor = s.btn.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp)
        ) { Text(label, fontSize = fontSize, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }

    @Composable
    private fun TabIconBtn(icon: ImageVector, desc: String, onClick: () -> Unit) {
        IconButton(onClick = onClick) { Icon(icon, desc, tint = Gold) }
    }

    // ---------- dialogs ----------
    @Composable
    private fun SheetHost(kind: DialogKind, s: CalScheme) {
        Dialog(
            onDismissRequest = { dialog = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState()),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = s.sheet)
            ) {
                Column(Modifier.padding(20.dp)) {
                    when (kind) {
                        is DialogKind.Create -> CreateSheet(s)
                        is DialogKind.Event -> EventSheet(kind.ev, s)
                        is DialogKind.Reason -> ReasonSheet(kind, s)
                        is DialogKind.Strategy -> StrategySheet(kind, s)
                        is DialogKind.Moved -> MovedSheet(kind.res, s)
                        is DialogKind.Search -> SearchSheet(s)
                        is DialogKind.Onboard -> OnboardSheet(s)
                        is DialogKind.ConfirmDelete -> ConfirmDeleteSheet(kind.ev, s)
                    }
                }
            }
        }
    }

    @Composable
    private fun SheetTitle(text: String, s: CalScheme, size: Int = 18) {
        Text(text, fontSize = size.sp, fontWeight = FontWeight.Bold, color = s.goldTxt)
    }

    @Composable
    private fun CreateSheet(s: CalScheme) {
        var dayMs by remember { mutableStateOf(Dates.startOfDay(anchor)) }
        var hour by remember { mutableIntStateOf(9) }
        var minute by remember { mutableIntStateOf(0) }
        var durMin by remember { mutableIntStateOf(60) }
        var rule by remember { mutableStateOf(RepeatRule.ONCE) }
        var count by remember { mutableIntStateOf(4) }
        var colorId by remember { mutableStateOf("") }
        val templates = remember { loadTemplates() }

        SheetTitle("＋ New task", s, 17)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = createTitle,
                onValueChange = { createTitle = it },
                label = { Text("Task title") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = {
                try {
                    voiceLauncher.launch(Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Say the task…")
                    })
                } catch (e: Exception) { toast("Voice input not available on this device") }
            }) { Icon(Icons.Filled.Mic, "Voice", tint = Gold) }
        }
        Text("Quick templates", fontSize = 12.sp, color = s.muted, modifier = Modifier.padding(top = 8.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp)) {
            val all = listOf("📚 Study" to 60, "🏋 Gym" to 60, "📖 Read" to 30, "🙏 Pray" to 20) +
                templates.map { "★ ${it.first}" to it.second }
            all.take(8).forEach { (label, mins) ->
                val isCustom = label.startsWith("★ ")
                ChipButton(label.take(13), false, s, Modifier.padding(end = 4.dp)) {
                    createTitle = if (isCustom) label.removePrefix("★ ").trim() else label.substring(2).trim()
                    durMin = mins
                }
            }
        }
        GhostButton("＋ Save current as template", {
            val t = createTitle.trim()
            if (t.isEmpty()) toast("Type a title first.")
            else { saveTemplate(t, durMin); toast("Template saved") }
        }, s, Modifier.fillMaxWidth().padding(top = 6.dp))
        // date stepper
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
            StepBtn("‹", s) { dayMs -= 86400000L }
            Text(
                "📅 " + SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(dayMs)),
                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = s.text,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
            )
            StepBtn("›", s) { dayMs += 86400000L }
        }
        // time
        Text(
            "⏰ " + String.format(Locale.getDefault(), "%02d:%02d", hour, minute),
            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = s.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                .clickable {
                    TimePickerDialog(this@MainActivity, { _, h, m -> hour = h; minute = m }, hour, minute, false).show()
                }
        )
        Text("Tap the time to change it", fontSize = 11.sp, color = s.muted,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        // duration
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Spacer(Modifier.weight(1f))
            StepBtn("−", s) { durMin = maxOf(15, durMin - 15) }
            Text("⏳ $durMin min", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = s.text,
                modifier = Modifier.padding(horizontal = 14.dp))
            StepBtn("＋", s) { durMin = minOf(480, durMin + 15) }
            Spacer(Modifier.weight(1f))
        }
        // repeat rule
        Text("Repeat", fontSize = 12.sp, color = s.muted, modifier = Modifier.padding(top = 8.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 2.dp)) {
            RepeatRule.entries.forEach { r ->
                ChipButton(repeatLabel(r), rule == r, s, Modifier.padding(end = 4.dp), fontSize = 11.sp) {
                    rule = r
                }
            }
        }
        if (rule != RepeatRule.ONCE) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Spacer(Modifier.weight(1f))
                StepBtn("−", s) { count = maxOf(2, count - 1) }
                Text(
                    "×$count ${repeatLabel(rule).lowercase()}", fontSize = 13.sp, color = s.text,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
                StepBtn("＋", s) { count = minOf(12, count + 1) }
                Spacer(Modifier.weight(1f))
            }
        }
        Text("Category & color", fontSize = 12.sp, color = s.muted, modifier = Modifier.padding(top = 8.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            listOf(
                "📚 Study" to "9", "🏋 Fitness" to "10", "🙏 Faith" to "3",
                "🎨 Skills" to "6", "💰 Finance" to "5", "🎉 Social" to "4"
            ).forEach { (label, cid) ->
                ChipButton(label, colorId == cid, s, Modifier.padding(end = 4.dp), fontSize = 11.sp) {
                    colorId = if (colorId == cid) "" else cid
                }
            }
        }
        Text("Color", fontSize = 12.sp, color = s.muted, modifier = Modifier.padding(top = 8.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp)) {
            listOf("1", "2", "3", "4", "5", "6", "7", "9", "10", "11").forEach { cid ->
                val on = colorId == cid
                Box(
                    Modifier
                        .padding(end = 8.dp)
                        .size(26.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(hex(CalColors.of(cid).first))
                        .clickable { colorId = if (on) "" else cid }
                )
            }
        }
        GoldButton("Create task", {
            val t = createTitle.trim()
            if (t.isEmpty()) { toast("Type a title first."); return@GoldButton }
            doCreate(t, dayMs, hour, minute, durMin, rule, count, colorId)
        }, Modifier.fillMaxWidth().padding(top = 12.dp))
        GhostButton("Cancel", { dialog = null }, s, Modifier.fillMaxWidth().padding(top = 6.dp))
    }

    @Composable
    private fun EventSheet(ev: CalEvent, s: CalScheme) {
        var slots by remember { mutableStateOf<List<Slot>?>(null) }
        LaunchedEffect(ev.id, ev.start) {
            slots = withContext(Dispatchers.IO) {
                try {
                    repo.buildWindow("week", System.currentTimeMillis()).days
                        .flatMap { it.freeSlots }
                        .filter { sl -> sl.end - sl.ms >= ev.durationMin * 60000L && sl.ms > System.currentTimeMillis() }
                } catch (e: Exception) { emptyList() }
            }
        }
        SheetTitle("📅 " + ev.title, s, 20)
        Text(
            dayFmt(ev.start) + " · " + time12(ev.start) + "–" + time12(ev.end) + " · " + durFmt(ev.durationMin),
            fontSize = 13.5.sp, color = s.soft, modifier = Modifier.padding(top = 4.dp)
        )
        val cleanDesc = try { Regex("⏰\\d{1,3}").replace(ev.desc, "").trim() } catch (e: Exception) { ev.desc }
        if (cleanDesc.isNotEmpty()) Text(cleanDesc.take(300), fontSize = 13.sp, color = s.soft, modifier = Modifier.padding(top = 8.dp))
        // reminder lead
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
            Text("🔔 Remind:", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = s.muted)
            Spacer(Modifier.width(6.dp))
            Text(
                AlarmScheduler.describeLead(ev.desc, AlarmScheduler.getReminderMinutes(this@MainActivity)),
                fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = s.goldTxt
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            listOf("Default" to null, "5m" to 5L, "15m" to 15L, "30m" to 30L, "1h" to 60L).forEach { (label, mins) ->
                val isCur = if (mins == null) !Regex("⏰\\d{1,3}").containsMatchIn(ev.desc)
                else AlarmScheduler.leadForDesc(ev.desc, -1L) == mins
                ChipButton(label, isCur, s, Modifier.weight(1f), fontSize = 11.5.sp) { setLead(ev, mins) }
            }
        }
        Text("Move to another free slot:", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = s.muted,
            modifier = Modifier.padding(top = 14.dp))
        val sl = slots
        if (sl == null) {
            Text("Looking for free slots…", color = s.muted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 6.dp))
        } else if (sl.isEmpty()) {
            Text("No full-length free slot next 7 days.", color = GoldDim, fontSize = 13.sp, modifier = Modifier.padding(vertical = 6.dp))
            GhostButton("Try splitting / extending / waking early", {
                dialog = DialogKind.Reason(ev, null, true)
            }, s, Modifier.fillMaxWidth())
        } else {
            sl.take(7).forEach { slot ->
                val label = if (Dates.daysBetween(slot.ms, System.currentTimeMillis()) == 0) "Today"
                else SimpleDateFormat("EEE MMM d", Locale.getDefault()).format(Date(slot.ms))
                SlotButton("$label   ${time12(slot.ms)}", s) {
                    dialog = DialogKind.Reason(ev, slot.ms, false)
                }
            }
        }
        GoldButton("⚡ Push to next free time (auto)", {
            dialog = DialogKind.Reason(ev, null, true)
        }, Modifier.fillMaxWidth().padding(top = 8.dp))
        if (ev.desc.contains("✅ Completed")) {
            GhostButton("↩ Unmark (not done yet)", { dialog = null; unmark(ev) }, s, Modifier.fillMaxWidth().padding(top = 6.dp))
        } else {
            GhostButton("🎉 Mark as done", { dialog = null; markDone(ev) }, s, Modifier.fillMaxWidth().padding(top = 6.dp))
        }
        var seriesN by remember { mutableIntStateOf(-1) }
        LaunchedEffect(ev.id) {
            seriesN = withContext(Dispatchers.IO) {
                val m = repo.db.seriesDao().memberOf(ev.id) ?: return@withContext -1
                repo.db.seriesDao().following(m.seriesId, ev.start).size
            }
        }
        if (seriesN > 1) {
            GhostButton("🗑 Delete this + $seriesN in series", { deleteSeriesFollowing(ev) }, s, Modifier.fillMaxWidth().padding(top = 6.dp))
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            GhostButton("⧉ Duplicate", { dialog = null; duplicate(ev) }, s, Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            GhostButton("↗ Share", {
                try {
                    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "📌 ${ev.title}\n${dayFmt(ev.start)} · ${time12(ev.start)}–${time12(ev.end)}")
                    }, "Share task"))
                } catch (e: Exception) { toast("Share failed") }
            }, s, Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            GhostButton("🗑 Delete", { dialog = DialogKind.ConfirmDelete(ev) }, s, Modifier.weight(1f))
        }
        GhostButton("Close", { dialog = null }, s, Modifier.fillMaxWidth().padding(top = 6.dp))
    }

    @Composable
    private fun ConfirmDeleteSheet(ev: CalEvent, s: CalScheme) {
        SheetTitle("Delete this task?", s, 17)
        Text("\"${ev.title}\" will be removed. This can't be undone.", fontSize = 13.5.sp, color = s.soft,
            modifier = Modifier.padding(top = 6.dp))
        GoldButton("Delete", { dialog = null; deleteEvent(ev) }, Modifier.fillMaxWidth().padding(top = 10.dp))
        GhostButton("Keep", { dialog = DialogKind.Event(ev) }, s, Modifier.fillMaxWidth().padding(top = 6.dp))
    }

    @Composable
    private fun ReasonSheet(kind: DialogKind.Reason, s: CalScheme) {
        var selected by remember { mutableStateOf("") }
        var custom by remember { mutableStateOf("") }
        SheetTitle("Why move it?", s, 17)
        Text("“${kind.ev.title}” — pick a reason so future-you learns.", fontSize = 13.5.sp, color = s.soft,
            modifier = Modifier.padding(top = 4.dp))
        listOf("Fell behind", "Forgot", "Took longer than expected", "Emergency", "Needed rest").forEach { r ->
            SlotButton((if (selected == r) "● " else "○ ") + r, s, gold = selected == r) {
                selected = if (selected == r) "" else r
            }
        }
        OutlinedTextField(
            value = custom, onValueChange = { custom = it },
            label = { Text("…or type your own reason") }, singleLine = false,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
        GoldButton("Confirm move", {
            var reason = selected
            if (custom.trim().isNotEmpty()) reason = if (reason.isNotEmpty()) "$reason — ${custom.trim()}" else custom.trim()
            if (reason.isEmpty()) { toast("Pick or type a reason first."); return@GoldButton }
            if (kind.autoPush) { dialog = null; doAutoPush(kind.ev, reason) }
            else { dialog = null; doMove(kind.ev, kind.slotMs!!, reason) }
        }, Modifier.fillMaxWidth().padding(top = 10.dp))
        GhostButton("Cancel", { dialog = null }, s, Modifier.fillMaxWidth().padding(top = 6.dp))
    }

    @Composable
    private fun StrategySheet(kind: DialogKind.Strategy, s: CalScheme) {
        SheetTitle("No full gap is free 😬", s, 17)
        Text(
            "“${kind.ev.title}” needs ${durFmt(kind.ev.durationMin)}, but no single free slot fits it. How should we arrange it?",
            fontSize = 13.5.sp, color = s.soft, modifier = Modifier.padding(top = 4.dp)
        )
        kind.outcome.alternatives.forEach { alt ->
            SlotButton(alt.label, s) {
                dialog = null
                doStrategy(kind.ev, kind.reason, alt.kind, alt.target, alt.segments)
            }
            if (alt.detail.isNotEmpty()) Text(alt.detail, fontSize = 12.sp, color = Color(0xFFAFA5C3),
                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp))
        }
        GhostButton("Cancel", { dialog = null }, s, Modifier.fillMaxWidth().padding(top = 6.dp))
    }

    @Composable
    private fun MovedSheet(res: MoveResult, s: CalScheme) {
        SheetTitle("✅ Task moved", s, 17)
        Text(
            buildString {
                append(res.title ?: "")
                append("\n")
                append(dayFmt(res.newStart))
                append(" · ")
                append(time12(res.newStart) + "–" + time12(res.newEnd))
                if (res.rollover) append("\nOn a later day")
                if (res.trimmed) append(" · shortened to fit")
            },
            fontSize = 14.sp, color = s.soft, modifier = Modifier.padding(top = 4.dp)
        )
        Text("😔 Remember — moving a task is a delay. Do it right when the time comes!",
            fontSize = 13.sp, color = Color(0xFFB45309), modifier = Modifier.padding(top = 8.dp))
        GoldButton("View the new day", {
            dialog = null
            anchor = res.newStart
            changeMode("day")
        }, Modifier.fillMaxWidth().padding(top = 10.dp))
        GhostButton("↩ Undo this move", {
            dialog = null
            lifecycleScope.launch {
                toast("Restoring…")
                val ok = withContext(Dispatchers.IO) { Rescheduler.undoLast(repo.api, repo.db) }
                toast(if (ok) "Restored ✅" else "Could not undo")
                load()
            }
        }, s, Modifier.fillMaxWidth().padding(top = 6.dp))
        GhostButton("Done", { dialog = null }, s, Modifier.fillMaxWidth().padding(top = 6.dp))
    }

    @Composable
    private fun SearchSheet(s: CalScheme) {
        var q by remember { mutableStateOf("") }
        var hits by remember { mutableStateOf<List<com.emmanuel.calendarrescheduler.data.CachedEvent>>(emptyList()) }
        var searched by remember { mutableStateOf(false) }
        SheetTitle("⌕ Search tasks", s, 17)
        OutlinedTextField(
            value = q, onValueChange = { q = it },
            label = { Text("Type to search…") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        GoldButton("Search", {
            val query = q.trim().lowercase()
            if (query.length < 2) return@GoldButton
            lifecycleScope.launch {
                hits = withContext(Dispatchers.IO) {
                    try {
                        val now = System.currentTimeMillis()
                        repo.db.eventDao().getBetween(now - 30 * 86400000L, now + 90 * 86400000L)
                            .filter { (it.title + " " + it.desc).lowercase().contains(query) }
                            .sortedBy { it.start }
                            .take(12)
                    } catch (e: Exception) { emptyList() }
                }
                searched = true
            }
        }, Modifier.fillMaxWidth().padding(top = 8.dp))
        if (searched && hits.isEmpty()) {
            Text("No matches in recent cache — sync first.", color = s.muted, fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp))
        }
        hits.forEach { c ->
            SlotButton("${time12(c.start)}  ${c.title}", s) {
                dialog = null
                anchor = c.start
                mainTab = "cal"
                changeMode("day")
            }
        }
        GhostButton("Close", { dialog = null }, s, Modifier.fillMaxWidth().padding(top = 6.dp))
    }

    @Composable
    private fun OnboardSheet(s: CalScheme) {
        SheetTitle("👋 Welcome to Schedule", s, 20)
        Text("Your poet-planner: move tasks, get reminded, stay focused.\n\nTwo quick permissions so reminders actually fire:",
            fontSize = 14.sp, color = s.soft, modifier = Modifier.padding(top = 8.dp))
        GoldButton(if (notifEnabled()) "✅ Notifications on" else "1 · Allow notifications", {
            if (!notifEnabled()) requestNotifyPermissionIfNeeded() else toast("Already on ✅")
        }, Modifier.fillMaxWidth().padding(top = 10.dp))
        GoldButton(if (canExactAlarm()) "✅ Exact alarms on" else "2 · Allow exact alarms", {
            if (!canExactAlarm()) promptExactAlarm() else toast("Already on ✅")
        }, Modifier.fillMaxWidth().padding(top = 6.dp))
        GhostButton("Start planning ✨", {
            try { settings().edit().putBoolean("onboarded", true).apply() } catch (e: Exception) { }
            dialog = null
            load()
        }, s, Modifier.fillMaxWidth().padding(top = 6.dp))
    }

    companion object {
        const val CHANNEL_ID = "upcoming_tasks"
        const val NOTIF_ID = "notifId"
        const val EXTRA_EVENT_ID = "eventId"
        const val EXTRA_TITLE = "title"
        const val EXTRA_START = "start"
        const val EXTRA_DESC = "desc"
        const val EXTRA_NOTE = "note"
        const val EXTRA_ACTION = "action"
        const val ACTION_OPEN = "action.open"
        const val ACTION_RESCHEDULE = "action.reschedule"
        const val NOTIF_ACTION_DISMISS = "DISMISS"
        const val NOTIF_ACTION_SNOOZE = "SNOOZE"
    }
}

// ---------- ambient pickers shared labels ----------
private val AmbientLabels = mapOf(
    "off" to "Silent",
    "rain" to "🌧 Rain",
    "ocean" to "🐚 Ocean",
    "piano_moonlight" to "🌙 Moonlight",
    "piano_furelise" to "✨ Für Elise",
    "piano_pathetique" to "🔥 Pathétique",
    "piano_turkish" to "🎠 Turkish",
)

private fun Dp.coerceIn(min: Dp, max: Dp): Dp = when {
    this < min -> min
    this > max -> max
    else -> this
}
