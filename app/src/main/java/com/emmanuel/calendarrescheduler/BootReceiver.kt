package com.emmanuel.calendarrescheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("CalResBoot", "Boot completed — re-scheduling alarms from cache")
            AlarmScheduler.scheduleFromCache(context)
            AlarmScheduler.scheduleDailyPlan(context)
        }
    }
}