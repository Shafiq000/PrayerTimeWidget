package com.prayertimewidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import java.util.*
import java.util.concurrent.TimeUnit
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import android.app.PendingIntent

import com.prayertimewidget.PrayerTimesUpdateWorker

class PrayerTimesWidgetProvider : AppWidgetProvider() {

    private val handler = Handler(Looper.getMainLooper())

   override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
    for (appWidgetId in appWidgetIds) {
        val prayerTimesMap = fetchPrayerTimesFromModule(context)

        val appWidgetOptions = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidth = appWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val minHeight = appWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

        // Create an Intent to launch your app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // Create a PendingIntent
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val views: RemoteViews = when {
            // Large widget layout (no countdown here)
            minWidth >= 200 && minHeight >= 250 -> {
                RemoteViews(context.packageName, R.layout.widget_large).apply {
                    updatePrayerTimesForLargeWidget(context, appWidgetManager, appWidgetId, prayerTimesMap)

                    // Set the PendingIntent for the large widget layout
                    setOnClickPendingIntent(R.id.widget_large_layout, pendingIntent)  // Replace with your actual layout ID
                }
            }
            // Medium widget layout (with countdown)
            minWidth >= 150 && minHeight >= 150 -> {
                RemoteViews(context.packageName, R.layout.widget_medium).apply {
                    val (nextPrayerName, nextPrayerTime) = getNextPrayer(prayerTimesMap)
                    setTextViewText(R.id.next_prayer_name, "Next: $nextPrayerName")
                    setTextViewText(R.id.next_prayer_time, convert24To12HourFormat(nextPrayerTime))

                    // Start countdown for medium widget only
                    startPrayerCountdown(context, appWidgetManager, appWidgetId)

                    // Set the PendingIntent for the medium widget layout
                    setOnClickPendingIntent(R.id.widget_medium_layout, pendingIntent)  // Replace with your actual layout ID
                }
            }
            // Small widget layout (with countdown)
            else -> {
                RemoteViews(context.packageName, R.layout.widget_small).apply {
                    val (nextPrayerName, nextPrayerTime) = getNextPrayer(prayerTimesMap)
                    setTextViewText(R.id.next_prayer_name, "Next: $nextPrayerName")
                    setTextViewText(R.id.next_prayer_time, convert24To12HourFormat(nextPrayerTime))

                    // Start countdown for small widget only
                    startPrayerCountdown(context, appWidgetManager, appWidgetId)

                    // Set the PendingIntent for the small widget layout
                    setOnClickPendingIntent(R.id.widget_small_layout, pendingIntent)  // Replace with your actual layout ID
                }
            }
        }

        // Update the widget with the corresponding view
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    // Schedule the worker to update prayer times
    val workRequest: WorkRequest = OneTimeWorkRequestBuilder<PrayerTimesUpdateWorker>().build()
    WorkManager.getInstance(context).enqueue(workRequest)
   }


    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Code to execute when the first widget is created
        Log.d("PrayerTimesWidget", "First widget instance created.")
        // Any additional setup can go here, like initializing shared preferences or settings
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Code to execute when the last widget is disabled
        Log.d("PrayerTimesWidget", "Last widget instance removed.")
        // Any cleanup code can go here, such as canceling any ongoing background work
    }

    private fun updatePrayerTimesForLargeWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        prayerTimesMap: WritableMap
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_large).apply {
            // Get the next prayer and its time
            val (nextPrayerName, nextPrayerTime) = getNextPrayer(prayerTimesMap)

            // Display next prayer name and time at the top
            setTextViewText(R.id.next_prayer_name, "Next: $nextPrayerName")
            setTextViewText(R.id.next_prayer_time, convert24To12HourFormat(nextPrayerTime))

            // Display individual prayer times
            setTextViewText(R.id.fajrTime, prayerTimesMap.getString("Fajr") ?: "Unknown")
            setTextViewText(R.id.tvFajrAMPM, getAMPM(prayerTimesMap.getString("Fajr") ?: "Unknown"))
            setTextViewText(R.id.dhuhrTime, prayerTimesMap.getString("Dhuhr") ?: "Unknown")
            setTextViewText(R.id.tvDhuhrAMPM, getAMPM(prayerTimesMap.getString("Dhuhr") ?: "Unknown"))
            setTextViewText(R.id.asrTime, prayerTimesMap.getString("Asr") ?: "Unknown")
            setTextViewText(R.id.tvAsrAMPM, getAMPM(prayerTimesMap.getString("Asr") ?: "Unknown"))
            setTextViewText(R.id.maghribTime, prayerTimesMap.getString("Maghrib") ?: "Unknown")
            setTextViewText(R.id.tvMaghribAMPM, getAMPM(prayerTimesMap.getString("Maghrib") ?: "Unknown"))
            setTextViewText(R.id.ishaTime, prayerTimesMap.getString("Isha") ?: "Unknown")
            setTextViewText(R.id.tvIshaAMPM, getAMPM(prayerTimesMap.getString("Isha") ?: "Unknown"))
        }

        // Update the widget with the new view for large size
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == Intent.ACTION_TIME_TICK ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_SCREEN_ON ||
            intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {

            // Schedule the WorkManager job
            val workRequest = OneTimeWorkRequestBuilder<PrayerTimesUpdateWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

    private fun startPrayerCountdown(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        handler.post(object : Runnable {
            override fun run() {
                val prayerTimes = fetchPrayerTimesFromModule(context)
                val (nextPrayerName, nextPrayerTime) = getNextPrayer(prayerTimes)

                // Get the remaining time until the next prayer
                val remainingTimeMillis = getRemainingTimeInMillis(nextPrayerTime)

                // Get widget options to determine size
                val appWidgetOptions = appWidgetManager.getAppWidgetOptions(appWidgetId)
                val minWidth = appWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                val minHeight = appWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

                // Handle widget update for large size
                if (minWidth >= 200 && minHeight >= 250) {
                    // Update large widget with prayer times
                    updatePrayerTimesForLargeWidget(context, appWidgetManager, appWidgetId, prayerTimes)
                } else {
                    // Update medium and small widgets with countdown
                    val views: RemoteViews? = when {
                        // Medium widget
                        minWidth >= 150 && minHeight >= 150 -> {
                            RemoteViews(context.packageName, R.layout.widget_medium).apply {
                                setTextViewText(R.id.next_prayer_name, "Next: $nextPrayerName")
                                setTextViewText(R.id.next_prayer_time, convert24To12HourFormat(nextPrayerTime))
                            }
                        }
                        // Small widget
                        else -> {
                            RemoteViews(context.packageName, R.layout.widget_small).apply {
                                setTextViewText(R.id.next_prayer_name, "Next: $nextPrayerName")
                                setTextViewText(R.id.next_prayer_time, convert24To12HourFormat(nextPrayerTime))
                            }
                        }
                    }

                    // Only update the views if they're not null (i.e., not the large widget)
                    views?.let {
                        // Update this part in startPrayerCountdown method
                        if (remainingTimeMillis > 0) {
                            val remainingTimeFormatted = formatMillisToHHMM(remainingTimeMillis) // Changed to show only hours and minutes
                            it.setTextViewText(R.id.remaining_time, remainingTimeFormatted)
                        } else {
                            if (nextPrayerName == "Isha") {
                                it.setTextViewText(R.id.remaining_time, "00:00") // Changed to hours and minutes
                            } else {
                                val nextFajrTime = prayerTimes.getString("Fajr") ?: "00:00"
                                val remainingTimeUntilFajr = getRemainingTimeInMillis(nextFajrTime)
                                val remainingTimeFormatted = formatMillisToHHMM(remainingTimeUntilFajr) // Changed to show only hours and minutes
                                it.setTextViewText(R.id.remaining_time, remainingTimeFormatted)
                            }
                        }

                        // Update the widget with the new view
                        appWidgetManager.updateAppWidget(appWidgetId, it)

                        // Schedule the next update in 1 second for small and medium widgets
                        handler.postDelayed(this, 1000)
                    }
                }
            }
        })
    }

    private fun fetchPrayerTimesFromModule(context: Context): WritableMap {
        val prayerTimes = Arguments.createMap()
        val sharedPreferences = context.getSharedPreferences("PrayerTimes", Context.MODE_PRIVATE)

        val fajrTime = sharedPreferences.getString("Fajr", "Unknown")
        val dhuhrTime = sharedPreferences.getString("Dhuhr", "Unknown")
        val asrTime = sharedPreferences.getString("Asr", "Unknown")
        val maghribTime = sharedPreferences.getString("Maghrib", "Unknown")
        val ishaTime = sharedPreferences.getString("Isha", "Unknown")

        Log.d("PrayerTimesWidget", "Fetched prayer times: Fajr=$fajrTime, Dhuhr=$dhuhrTime, Asr=$asrTime, Maghrib=$maghribTime, Isha=$ishaTime")

        // Store the fetched times in the WritableMap
        prayerTimes.putString("Fajr", fajrTime)
        prayerTimes.putString("Dhuhr", dhuhrTime)
        prayerTimes.putString("Asr", asrTime)
        prayerTimes.putString("Maghrib", maghribTime)
        prayerTimes.putString("Isha", ishaTime)

        return prayerTimes
    }

    private fun getNextPrayer(prayerTimes: WritableMap): Pair<String, String> {
        val currentTime = Calendar.getInstance()
        val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentTime.get(Calendar.MINUTE)

        val prayerNames = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
        val nextPrayer = prayerNames.firstOrNull { prayerName ->
            val prayerTime = prayerTimes.getString(prayerName)?.split(":")
            prayerTime?.let {
                val hour = it[0].toInt()
                val minute = it[1].toInt()
                hour > currentHour || (hour == currentHour && minute > currentMinute)
            } ?: false
        } ?: "Fajr"

        val nextPrayerTime = prayerTimes.getString(nextPrayer) ?: "00:00"
        return Pair(nextPrayer, nextPrayerTime)
    }

    private fun getRemainingTimeInMillis(nextPrayerTime: String): Long {
        val timeParts = nextPrayerTime.split(":")
        val nextPrayerHour = timeParts[0].toInt()
        val nextPrayerMinute = timeParts[1].toInt()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, nextPrayerHour)
            set(Calendar.MINUTE, nextPrayerMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return calendar.timeInMillis - System.currentTimeMillis()
    }

     private fun formatMillisToHHMM(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60

    return String.format("%02d:%02d", hours, minutes)
    }


    private fun convert24To12HourFormat(time: String): String {
        val parts = time.split(":")
        if (parts.size != 2) return time // Return original if format is incorrect

        val hour = parts[0].toInt()
        val minute = parts[1]
        val ampm = if (hour < 12) "AM" else "PM"
        val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour

        return String.format("%02d:%s %s", hour12, minute, ampm)
    }

    private fun getAMPM(time: String): String {
        val hour = time.split(":")[0].toInt()
        return if (hour < 12) "AM" else "PM"
    }
}
