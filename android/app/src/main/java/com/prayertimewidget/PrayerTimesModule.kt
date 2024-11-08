package com.prayertimewidget

import android.content.SharedPreferences
import android.util.Log
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import android.content.Context
import android.content.Intent

class PrayerTimesModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    private val client = OkHttpClient()
    private val reactContext = reactContext  // Store the reference to the react context

    override fun getName(): String {
        return "PrayerTimesModule"
    }

    @ReactMethod
    fun getPrayerTimes(city: String, country: String, promise: Promise) {
        val url = "https://api.aladhan.com/v1/timingsByCity?city=$city&country=$country"

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onResponse(call: okhttp3.Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        promise.reject("ERROR", "Failed to fetch prayer times: ${it.message}")
                    } else {
                        val responseBody = it.body?.string()
                        val jsonResponse = JSONObject(responseBody)
                        val prayerTimes = jsonResponse.getJSONObject("data").getJSONObject("timings")

                        // Create a WritableMap to send back to React Native
                        val writableMap = Arguments.createMap()
                        writableMap.putString("Fajr", prayerTimes.getString("Fajr"))
                        writableMap.putString("Dhuhr", prayerTimes.getString("Dhuhr"))
                        writableMap.putString("Asr", prayerTimes.getString("Asr"))
                        writableMap.putString("Maghrib", prayerTimes.getString("Maghrib"))
                        writableMap.putString("Isha", prayerTimes.getString("Isha"))

                        // Resolve the promise with the writableMap
                        promise.resolve(writableMap)
                    }
                }
            }

            override fun onFailure(call: okhttp3.Call, e: IOException) {
                promise.reject("ERROR", e)
            }
        })
    }

    @ReactMethod
fun sendPrayerTimes(prayerTimes: ReadableMap) {
    val fajr = prayerTimes.getString("Fajr") ?: "Unknown"
    val dhuhr = prayerTimes.getString("Dhuhr") ?: "Unknown"
    val asr = prayerTimes.getString("Asr") ?: "Unknown"
    val maghrib = prayerTimes.getString("Maghrib") ?: "Unknown"
    val isha = prayerTimes.getString("Isha") ?: "Unknown"

    // Add logs to verify that data is received
    Log.d("PrayerTimesDebug", "Received Prayer Times in Native Module")
    Log.d("PrayerTimesDebug", "Fajr: $fajr, Dhuhr: $dhuhr, Asr: $asr, Maghrib: $maghrib, Isha: $isha")

    // Save prayer times to SharedPreferences
    val sharedPreferences: SharedPreferences = reactContext.getSharedPreferences("PrayerTimes", Context.MODE_PRIVATE)
    with(sharedPreferences.edit()) {
        putString("Fajr", fajr)
        putString("Dhuhr", dhuhr)
        putString("Asr", asr)
        putString("Maghrib", maghrib)
        putString("Isha", isha)
        apply()  // Save the changes
    }

    // Send a broadcast to notify the widget of the update
    val intent = Intent("com.prayertimewidget.UPDATE_PRAYER_TIMES")
    intent.putExtra("Fajr", fajr)
    intent.putExtra("Dhuhr", dhuhr)
    intent.putExtra("Asr", asr)
    intent.putExtra("Maghrib", maghrib)
    intent.putExtra("Isha", isha)
    reactContext.sendBroadcast(intent)
}

}
