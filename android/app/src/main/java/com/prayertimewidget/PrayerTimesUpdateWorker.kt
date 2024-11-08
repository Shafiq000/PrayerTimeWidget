package com.prayertimewidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class PrayerTimesUpdateWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        // Perform your widget update logic here
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(applicationContext, PrayerTimesWidgetProvider::class.java))

        if (appWidgetIds.isNotEmpty()) {
            // Trigger your widget update
            PrayerTimesWidgetProvider().onUpdate(applicationContext, appWidgetManager, appWidgetIds)
        }

        return Result.success()
    }
}
