package com.emmanuel.calendarrescheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.emmanuel.calendarrescheduler.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DailyPlanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("CalResPlan", "6 AM plan alarm fired")
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                PlanNotifier.createChannels(context)
                val repo = Repository(context)
                val (events, _) = repo.backgroundRefresh(context)
                PlanNotifier.sendDailyPlan(context, events)
            } catch (e: Exception) {
                Log.d("CalResPlan", "Plan failed: ${e.message}")
            } finally {
                try { AlarmScheduler.scheduleDailyPlan(context) } catch (_: Exception) { }
                try { pending.finish() } catch (_: Exception) { }
            }
        }
    }
}
