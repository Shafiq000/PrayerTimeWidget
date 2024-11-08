package com.prayertimewidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.appwidget.AppWidgetManager
import android.content.ComponentName

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Get an instance of AppWidgetManager
            val appWidgetManager = AppWidgetManager.getInstance(context)
            
            // Get the IDs of all widgets for the PrayerTimesWidgetProvider
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, PrayerTimesWidgetProvider::class.java)
            )
            
            // Update all the widgets
            for (appWidgetId in appWidgetIds) {
                PrayerTimesWidgetProvider().onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
            }
        }
    }
}
