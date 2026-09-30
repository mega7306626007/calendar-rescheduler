package com.emmanuel.calendarrescheduler

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import com.emmanuel.calendarrescheduler.data.AppDatabase
import com.emmanuel.calendarrescheduler.data.Dates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TodayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                doRefresh(context.applicationContext)
            } catch (e: Exception) {
                Log.d("CalResWidget", "refresh failed: ${e.message}")
            } finally {
                try { pending.finish() } catch (_: Exception) { }
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try { doRefresh(context.applicationContext) } catch (_: Exception) { }
                try { pending.finish() } catch (_: Exception) { }
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.emmanuel.calendarrescheduler.WIDGET_REFRESH"

        private val ROW_IDS = intArrayOf(
            R.id.widget_row_0, R.id.widget_row_1, R.id.widget_row_2,
            R.id.widget_row_3, R.id.widget_row_4, R.id.widget_row_5
        )

        fun refresh(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                try { doRefresh(context.applicationContext) } catch (_: Exception) { }
            }
        }

        private suspend fun doRefresh(appCtx: Context) {
            val mgr = AppWidgetManager.getInstance(appCtx)
            val mine = mgr.getAppWidgetIds(ComponentName(appCtx, TodayWidgetProvider::class.java))
            if (mine.isEmpty()) return
            val now = System.currentTimeMillis()
            val dayStart = Dates.startOfDay(now)
            val db = AppDatabase.get(appCtx)
            val cached = try {
                db.eventDao().getBetween(dayStart, dayStart + 86400000L)
            } catch (e: Exception) { emptyList() }
            val today = cached
                .filter { !it.desc.contains("✅ Completed") }
                .sortedBy { it.start }
            val openPi = PendingIntent.getActivity(
                appCtx, 7001,
                Intent(appCtx, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val addPi = PendingIntent.getActivity(
                appCtx, 7002,
                Intent(appCtx, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse("schedule://app/go?to=create")
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val refreshPi = PendingIntent.getBroadcast(
                appCtx, 7003,
                Intent(appCtx, TodayWidgetProvider::class.java).apply { action = ACTION_REFRESH },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
            for (wid in mine) {
                val views = RemoteViews(appCtx.packageName, R.layout.widget_today)
                views.setTextViewText(
                    R.id.widget_title,
                    "📅 " + SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(now))
                )
                val shown = today.filter { !it.protected_ }.take(5)
                for (i in 0 until 5) {
                    val ev = shown.getOrNull(i)
                    views.setTextViewText(
                        ROW_IDS[i],
                        if (ev != null) "${fmt.format(Date(ev.start))}  ${ev.title}"
                        else if (i == 0 && shown.isEmpty()) "Nothing scheduled — enjoy! ✨"
                        else ""
                    )
                }
                views.setTextViewText(
                    ROW_IDS[5],
                    if (today.size > shown.size) "+${today.size - shown.size} more" else ""
                )
                views.setOnClickPendingIntent(R.id.widget_root, openPi)
                views.setOnClickPendingIntent(R.id.widget_btn_add, addPi)
                views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPi)
                try { mgr.updateAppWidget(wid, views) } catch (_: Exception) { }
            }
        }
    }
}
