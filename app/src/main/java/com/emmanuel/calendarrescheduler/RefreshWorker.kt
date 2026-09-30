package com.emmanuel.calendarrescheduler

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.emmanuel.calendarrescheduler.data.Repository

class RefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.d("CalResWorker", "Background refresh (network sync + plan notifications)")
        val ctx = applicationContext
        PlanNotifier.createChannels(ctx)
        return try {
            val repo = Repository(ctx)
            val (events, scheduled) = repo.backgroundRefresh(ctx)
            Log.d("CalResWorker", "Scheduled $scheduled alarms")
            PlanNotifier.sendDailyPlan(ctx, events)
            PlanNotifier.sendMissedNudges(ctx, events)
            PlanNotifier.sendStreakNudge(ctx)
            try { TodayWidgetProvider.refresh(ctx) } catch (_: Exception) { }
            Result.success()
        } catch (e: Exception) {
            Log.d("CalResWorker", "Network sync failed, re-alarm from cache: ${e.message}")
            AlarmScheduler.scheduleFromCache(ctx)
            Result.success()
        }
    }
}