package com.prayertimewidget

import android.content.Context
import android.content.Intent
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.uimanager.ViewManager

class BroadcastSenderModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String {
        return "BroadcastSenderModule"
    }

    @ReactMethod
    fun sendUpdatePrayerTimes(fajr: String, dhuhr: String, asr: String, maghrib: String, isha: String) {
        val intent = Intent("com.prayertimewidget.UPDATE_PRAYER_TIMES").apply {
            putExtra("fajr", fajr)
            putExtra("dhuhr", dhuhr)
            putExtra("asr", asr)
            putExtra("maghrib", maghrib)
            putExtra("isha", isha)
        }
        reactContext.sendBroadcast(intent)
    }

    @ReactMethod
    fun sendBroadcast() {
        val intent = Intent("com.prayertimewidget.ACTION_REFRESH_DATA")
        reactContext.sendBroadcast(intent)
    }
}

// Implement ReactPackage interface
class BroadcastSenderPackage : ReactPackage {
    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
        return listOf(BroadcastSenderModule(reactContext))
    }

    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        return emptyList()
    }
}
