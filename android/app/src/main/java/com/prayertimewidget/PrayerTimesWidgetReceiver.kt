package com.prayertimewidget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews

class PrayerTimesWidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null && intent.action == "com.prayertimewidget.UPDATE_PRAYER_TIMES") {
            val fajr = intent.getStringExtra("Fajr")
            val dhuhr = intent.getStringExtra("Dhuhr")
            val asr = intent.getStringExtra("Asr")
            val maghrib = intent.getStringExtra("Maghrib")
            val isha = intent.getStringExtra("Isha")

            Log.d("PrayerTimesWidgetReceiver", "Received updated prayer times: Fajr=$fajr, Dhuhr=$dhuhr, Asr=$asr, Maghrib=$maghrib, Isha=$isha")

            // Update your widget here with the new prayer times
            updateWidget(context, fajr, dhuhr, asr, maghrib, isha)
        }
    }

    private fun updateWidget(context: Context?, fajr: String?, dhuhr: String?, asr: String?, maghrib: String?, isha: String?) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        
        // Get the ComponentName for the widget
        val componentName = ComponentName(context!!, PrayerTimesWidgetProvider::class.java)
        
        // Get the IDs of all instances of the widget
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        // Update each widget instance
        for (appWidgetId in appWidgetIds) {
            updateWidgetViews(context, appWidgetManager, appWidgetId, fajr, dhuhr, asr, maghrib, isha)
        }
    }

    private fun updateWidgetViews(context: Context?, appWidgetManager: AppWidgetManager, appWidgetId: Int, fajr: String?, dhuhr: String?, asr: String?, maghrib: String?, isha: String?) {
        val views = RemoteViews(context?.packageName, R.layout.widget_prayer_times).apply {
            setTextViewText(R.id.tvFajr, "Fajr: $fajr")
            setTextViewText(R.id.tvDhuhr, "Dhuhr: $dhuhr")
            setTextViewText(R.id.tvAsr, "Asr: $asr")
            setTextViewText(R.id.tvMaghrib, "Maghrib: $maghrib")
            setTextViewText(R.id.tvIsha, "Isha: $isha")
        }
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
