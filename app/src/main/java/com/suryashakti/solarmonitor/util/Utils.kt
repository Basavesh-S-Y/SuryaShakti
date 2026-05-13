package com.suryashakti.solarmonitor.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.suryashakti.solarmonitor.R
import com.suryashakti.solarmonitor.SuryaShaktiApp
import com.suryashakti.solarmonitor.ui.main.MainActivity
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun toDisplayString(millis: Long): String = displayFormat.format(Date(millis))

    fun toMidnightMillis(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0);      cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun todayMidnightMillis(): Long = toMidnightMillis(System.currentTimeMillis())

    fun daysAgo(days: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        return toMidnightMillis(cal.timeInMillis)
    }
}

object EnergyUtils {
    fun formatKwh(kwh: Double)       = "%.2f kWh".format(kwh)
    fun formatRupees(amount: Double) = "₹%.2f".format(amount)
    fun formatPercent(p: Float)      = "%.1f%%".format(p)

    fun getScoreGrade(score: Int): Pair<String, String> = when {
        score >= 90 -> Pair("S", "Solar Champion! 🌟")
        score >= 75 -> Pair("A", "Excellent! 🌞")
        score >= 60 -> Pair("B", "Great Progress! ⚡")
        score >= 40 -> Pair("C", "Good Start! 🌱")
        score >= 20 -> Pair("D", "Keep Going! 💪")
        else        -> Pair("F", "Need More Sun! ☁️")
    }

    fun getCO2Label(kg: Double) = if (kg >= 1000) "%.1f tonne".format(kg / 1000)
                                  else             "%.1f kg".format(kg)

    /** Legacy clock check — used only for instant UI hints. */
    fun isPeakSunHour(): Boolean =
        Calendar.getInstance().get(Calendar.HOUR_OF_DAY) in 10..15
}

/**
 * Shows peak-sun notification.
 * [irradianceWm2] = actual W/m² from Open-Meteo (-1 = unknown).
 */
fun Context.showPeakSunNotification(irradianceWm2: Double = -1.0) {
    val pi = PendingIntent.getActivity(
        this, 0, Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val body = if (irradianceWm2 >= 0)
        "☀️ Live irradiance: %.0f W/m² — above 500 threshold.\nRun your pump, washing machine or EV charger for free solar energy!".format(irradianceWm2)
    else
        "🌞 Peak solar intensity detected. Ideal time for heavy appliances!"

    val notif = NotificationCompat.Builder(this, SuryaShaktiApp.CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification_sun)
        .setContentTitle("⚡ Peak Solar Irradiance!")
        .setContentText("Live irradiance > 500 W/m² — Run appliances now!")
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pi)
        .setAutoCancel(true)
        .setColor(0xFFFFD700.toInt())
        .build()

    (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        .notify(1001, notif)
}

class PeakSunReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED)
            com.suryashakti.solarmonitor.worker.PeakSunWorker.schedule(context)
    }
}
