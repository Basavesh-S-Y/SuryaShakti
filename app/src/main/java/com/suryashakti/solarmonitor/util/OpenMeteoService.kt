package com.suryashakti.solarmonitor.util

import com.suryashakti.solarmonitor.data.SolarForecast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches hourly direct_radiation (W/m²) from the Open-Meteo API.
 *
 * ✅ Completely free — no API key, no account needed.
 * ✅ No third-party HTTP library — uses java.net.HttpURLConnection.
 * ✅ JSON parsing with org.json (built into the Android SDK).
 *
 * API docs: https://open-meteo.com/en/docs
 */
object OpenMeteoService {

    private const val BASE_URL = "https://api.open-meteo.com/v1/forecast"
    private const val CONNECT_TIMEOUT_MS = 12_000
    private const val READ_TIMEOUT_MS    = 12_000

    /**
     * Fetches today's hourly solar irradiance for [lat]/[lon].
     *
     * Returns [Result.success] with a [SolarForecast] on success,
     * or [Result.failure] with a descriptive exception on any error.
     *
     * Must be called from a coroutine — runs entirely on [Dispatchers.IO].
     */
    suspend fun fetchForecast(lat: Double, lon: Double): Result<SolarForecast> =
        withContext(Dispatchers.IO) {
            runCatching {
                // ── Build URL ────────────────────────────────────────────
                val urlString = buildString {
                    append(BASE_URL)
                    append("?latitude=%.4f".format(lat))
                    append("&longitude=%.4f".format(lon))
                    append("&hourly=direct_radiation")
                    append("&forecast_days=1")
                    append("&timezone=auto")        // respects device timezone
                }

                // ── HTTP GET ─────────────────────────────────────────────
                val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                    requestMethod    = "GET"
                    connectTimeout   = CONNECT_TIMEOUT_MS
                    readTimeout      = READ_TIMEOUT_MS
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "SuryaShaktiApp/1.0")
                }

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("HTTP $responseCode from Open-Meteo")
                }

                val rawJson = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                // ── Parse JSON ───────────────────────────────────────────
                // Expected shape:
                // {
                //   "latitude": 12.9716,
                //   "longitude": 77.5946,
                //   "hourly": {
                //     "time":             ["2025-04-30T00:00", ..., "2025-04-30T23:00"],
                //     "direct_radiation": [0.0, 0.0, 0.0, ..., 820.5, 710.0, ...]
                //   }
                // }
                val root    = JSONObject(rawJson)
                val hourly  = root.getJSONObject("hourly")
                val times   = hourly.getJSONArray("time")
                val radiation = hourly.getJSONArray("direct_radiation")

                val irradianceList = ArrayList<Double>(24)
                val timeList       = ArrayList<String>(24)

                for (i in 0 until radiation.length()) {
                    // API occasionally returns null for missing values — treat as 0
                    irradianceList.add(
                        if (radiation.isNull(i)) 0.0 else radiation.getDouble(i)
                    )
                    timeList.add(times.getString(i))
                }

                SolarForecast(
                    hourlyIrradiance = irradianceList,
                    hourlyTimes      = timeList,
                    latitude         = root.getDouble("latitude"),
                    longitude        = root.getDouble("longitude")
                )
            }
        }
}
