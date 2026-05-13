package com.suryashakti.solarmonitor.repository

import android.content.Context
import com.suryashakti.solarmonitor.data.PanelLocationManager
import com.suryashakti.solarmonitor.data.SolarForecast
import com.suryashakti.solarmonitor.util.OpenMeteoService

/**
 * Fetches irradiance for the SOLAR PANEL's location (not the user's GPS).
 * Panel location is set once by the user and stored in SharedPreferences.
 */
class ForecastRepository(private val context: Context) {

    private var cache: SolarForecast? = null

    companion object {
        private const val CACHE_TTL_MINUTES = 30L
    }

    suspend fun getForecast(force: Boolean = false): Result<SolarForecast> {
        val cached = cache
        if (!force && cached != null && cached.ageMinutes < CACHE_TTL_MINUTES) {
            return Result.success(cached)
        }

        // ── Use the saved PANEL location, not GPS ─────────────────────────
        val panelLoc = PanelLocationManager.getLocation(context)
            ?: return Result.failure(Exception(
                "Solar panel location not set. Please set it in the app."
            ))

        val result = OpenMeteoService.fetchForecast(panelLoc.latitude, panelLoc.longitude)

        return result.map { forecast ->
            // Attach the panel's place name directly — no extra geocoding call needed
            forecast.copy(placeName = panelLoc.displayName).also { cache = it }
        }.recoverCatching { error ->
            cached?.copy(placeName = panelLoc.displayName) ?: throw error
        }
    }

    fun clearCache() { cache = null }

    suspend fun isPeakSunRightNow(): Boolean {
        val panelLoc = PanelLocationManager.getLocation(context) ?: return false
        return OpenMeteoService.fetchForecast(panelLoc.latitude, panelLoc.longitude)
            .getOrNull()?.isPeakSun ?: false
    }
}
