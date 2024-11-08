package com.prayertimewidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.appwidget.AppWidgetManager

class PrayerTimesUpdateScheduler {

   fun setAlarm(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, PrayerTimesWidgetProvider::class.java).apply {
        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
    }
    
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE // Add FLAG_IMMUTABLE for newer Android versions
    )

    // Set the alarm to fire every fifteen minutes
    alarmManager.setInexactRepeating(
        AlarmManager.ELAPSED_REALTIME_WAKEUP,
        System.currentTimeMillis(),
        AlarmManager.INTERVAL_FIFTEEN_MINUTES,
        pendingIntent
    )
}

}
