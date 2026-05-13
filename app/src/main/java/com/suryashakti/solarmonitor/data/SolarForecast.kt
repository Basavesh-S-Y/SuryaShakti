package com.suryashakti.solarmonitor.data

import java.util.Calendar

/**
 * Holds hourly direct_radiation values (W/m²) returned by Open-Meteo,
 * plus the resolved [placeName] from reverse geocoding.
 */
data class SolarForecast(
    val hourlyIrradiance: List<Double>,      // 24 values, W/m²
    val hourlyTimes: List<String>,           // ISO strings e.g. "2025-04-30T14:00"
    val latitude: Double,
    val longitude: Double,
    val placeName: String = "",              // ← e.g. "Whitefield, Bengaluru"
    val fetchedAtMillis: Long = System.currentTimeMillis()
) {
    companion object {
        const val PEAK_THRESHOLD_WM2 = 500.0
        const val MAX_DISPLAY_WM2   = 1000.0
    }

    // ── Derived properties ────────────────────────────────────────────────

    val currentIrradiance: Double
        get() {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return if (hour < hourlyIrradiance.size) hourlyIrradiance[hour] else 0.0
        }

    val isPeakSun: Boolean get() = currentIrradiance > PEAK_THRESHOLD_WM2

    val peakIrradiance: Double get() = hourlyIrradiance.maxOrNull() ?: 0.0

    val peakHour: Int get() = hourlyIrradiance.indexOf(peakIrradiance)

    val peakWindowStartHour: Int
        get() = hourlyIrradiance.indexOfFirst { it >= PEAK_THRESHOLD_WM2 }

    val peakWindowEndHour: Int
        get() = hourlyIrradiance.indexOfLast  { it >= PEAK_THRESHOLD_WM2 }

    val peakWindowLabel: String
        get() {
            val s = peakWindowStartHour
            val e = peakWindowEndHour
            return if (s < 0 || e < 0) "No peak today"
            else "%02d:00 – %02d:00".format(s, e + 1)
        }

    val currentIrradiancePercent: Int
        get() = (currentIrradiance / MAX_DISPLAY_WM2 * 100).toInt().coerceIn(0, 100)

    val statusLabel: String
        get() = when {
            currentIrradiance >= 800                 -> "Excellent ☀️"
            currentIrradiance >= 600                 -> "Very Good 🌤️"
            currentIrradiance >= PEAK_THRESHOLD_WM2 -> "Peak — Run appliances! ⚡"
            currentIrradiance >= 300                 -> "Moderate ⛅"
            currentIrradiance >  0                   -> "Low ☁️"
            else                                     -> "No Sun 🌙"
        }

    val statusColor: Int
        get() = when {
            currentIrradiance >= PEAK_THRESHOLD_WM2 -> 0xFF00E676.toInt()
            currentIrradiance >= 300                -> 0xFFFFD700.toInt()
            else                                    -> 0xFFAAAAAA.toInt()
        }

    val ageMinutes: Long
        get() = (System.currentTimeMillis() - fetchedAtMillis) / 60_000

    /**
     * Short display label for the UI location line.
     * Shows place name if resolved, otherwise coordinates.
     */
    val locationDisplayLabel: String
        get() = if (placeName.isNotBlank()) placeName
                else coordinateLabel

    /** Raw "12.97°N, 77.59°E" coordinate string. */
    val coordinateLabel: String
        get() = "%.4f°%s  %.4f°%s".format(
            kotlin.math.abs(latitude),  if (latitude  >= 0) "N" else "S",
            kotlin.math.abs(longitude), if (longitude >= 0) "E" else "W"
        )
}

sealed class ForecastState {
    object Loading : ForecastState()
    data class Success(val forecast: SolarForecast) : ForecastState()
    data class Error(val message: String, val isPermissionError: Boolean = false) : ForecastState()
}
