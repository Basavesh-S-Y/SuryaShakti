package com.suryashakti.solarmonitor

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.suryashakti.solarmonitor.util.ThemeManager
import com.suryashakti.solarmonitor.worker.PeakSunWorker

class SuryaShaktiApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ThemeManager.apply(this)          // apply saved theme on every launch
        createNotificationChannel()
        PeakSunWorker.schedule(this)      // background irradiance checks
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Peak Sun Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts when solar panel irradiance exceeds 500 W/m²"
            enableVibration(true)
        }
        (getSystemService(NotificationManager::class.java))
            .createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "peak_sun_channel"
    }
}
