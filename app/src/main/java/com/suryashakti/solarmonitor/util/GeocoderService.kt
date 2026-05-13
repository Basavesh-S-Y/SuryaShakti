package com.suryashakti.solarmonitor.util

import android.content.Context
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Converts GPS coordinates → human-readable place name.
 *
 * Strategy (in order):
 *  1. Android built-in [Geocoder] — instant, works offline on devices
 *     that have Google Play Services or a system geocoder.
 *  2. Nominatim (OpenStreetMap) reverse geocoding — completely free,
 *     no API key, works everywhere as long as there is internet.
 *  3. Fallback: formatted "12.97°N, 77.59°E" coordinate string.
 *
 * Nominatim ToS: one request per second, must send User-Agent.
 * We only call it once per ~30-minute cache window so this is fine.
 */
object GeocoderService {

    private const val NOMINATIM_URL =
        "https://nominatim.openstreetmap.org/reverse"
    private const val CONNECT_TIMEOUT = 8_000
    private const val READ_TIMEOUT    = 8_000

    /**
     * Returns a short, friendly place name for [lat]/[lon].
     *
     * Examples:
     *  • "Whitefield, Bengaluru"
     *  • "Andheri West, Mumbai"
     *  • "Connaught Place, New Delhi"
     *  • "12.97°N, 77.59°E"  ← coordinate fallback
     */
    suspend fun getPlaceName(context: Context, lat: Double, lon: Double): String {
        // ── Try Android Geocoder ───────────────────────────────────────
        val geocoderResult = tryAndroidGeocoder(context, lat, lon)
        if (!geocoderResult.isNullOrBlank()) return geocoderResult

        // ── Try Nominatim (OpenStreetMap) ──────────────────────────────
        val nominatimResult = tryNominatim(lat, lon)
        if (!nominatimResult.isNullOrBlank()) return nominatimResult

        // ── Coordinate fallback ────────────────────────────────────────
        return coordinateFallback(lat, lon)
    }

    // ── 1. Android Geocoder ───────────────────────────────────────────────

    private suspend fun tryAndroidGeocoder(
        context: Context,
        lat: Double,
        lon: Double
    ): String? = withTimeoutOrNull(4_000) {
        runCatching {
            if (!Geocoder.isPresent()) return@runCatching null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // API 33+: async listener
                suspendCancellableCoroutine { cont ->
                    Geocoder(context, Locale.getDefault())
                        .getFromLocation(lat, lon, 1) { addresses ->
                            cont.resume(formatAndroidAddress(addresses.firstOrNull()))
                        }
                }
            } else {
                // API 26–32: synchronous (run on IO)
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    val addresses = Geocoder(context, Locale.getDefault())
                        .getFromLocation(lat, lon, 1)
                    formatAndroidAddress(addresses?.firstOrNull())
                }
            }
        }.getOrNull()
    }

    private fun formatAndroidAddress(address: android.location.Address?): String? {
        if (address == null) return null

        // Build: "Locality/SubLocality, City/AdminArea"
        // e.g.  "Whitefield, Bengaluru" or "Bengaluru, Karnataka"
        val parts = listOfNotNull(
            address.subLocality?.takeIf { it.isNotBlank() }
                ?: address.locality?.takeIf { it.isNotBlank() },
            address.locality?.takeIf { it.isNotBlank() }
                ?: address.subAdminArea?.takeIf { it.isNotBlank() }
                ?: address.adminArea?.takeIf { it.isNotBlank() }
        ).distinct().take(2)

        return if (parts.isNotEmpty()) parts.joinToString(", ") else null
    }

    // ── 2. Nominatim (OpenStreetMap) ──────────────────────────────────────

    private suspend fun tryNominatim(lat: Double, lon: Double): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = "$NOMINATIM_URL" +
                        "?lat=%.6f&lon=%.6f".format(lat, lon) +
                        "&format=json&zoom=14&addressdetails=1"

                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT
                    readTimeout    = READ_TIMEOUT
                    // Nominatim requires a descriptive User-Agent
                    setRequestProperty(
                        "User-Agent",
                        "SuryaShaktiApp/2.0 (solar.monitor.app@gmail.com)"
                    )
                    setRequestProperty("Accept-Language", "en")
                }

                if (conn.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                conn.disconnect()

                formatNominatimResponse(json)
            }.getOrNull()
        }

    /**
     * Extracts a short readable name from Nominatim's JSON.
     *
     * Nominatim address hierarchy (most to least specific):
     * neighbourhood → suburb → village → town → city → county → state
     *
     * We pick the two most specific non-blank parts.
     */
    private fun formatNominatimResponse(json: JSONObject): String? {
        val addr = json.optJSONObject("address") ?: return null

        // Priority list — most specific first
        val neighbourhood = addr.optString("neighbourhood").takeIf { it.isNotBlank() }
        val suburb        = addr.optString("suburb").takeIf        { it.isNotBlank() }
        val village       = addr.optString("village").takeIf       { it.isNotBlank() }
        val town          = addr.optString("town").takeIf          { it.isNotBlank() }
        val city          = addr.optString("city").takeIf          { it.isNotBlank() }
        val county        = addr.optString("county").takeIf        { it.isNotBlank() }
        val state         = addr.optString("state").takeIf         { it.isNotBlank() }

        // Level 1: most specific local area
        val local = neighbourhood ?: suburb ?: village ?: town ?: city ?: county
        // Level 2: city or state (avoid repeating level 1)
        val broader = (city ?: town ?: state)?.takeIf { it != local }

        val parts = listOfNotNull(local, broader)
        return if (parts.isNotEmpty()) parts.joinToString(", ") else null
    }

    // ── 3. Coordinate fallback ────────────────────────────────────────────

    private fun coordinateFallback(lat: Double, lon: Double): String {
        val latDir = if (lat >= 0) "N" else "S"
        val lonDir = if (lon >= 0) "E" else "W"
        return "%.2f°%s, %.2f°%s".format(
            kotlin.math.abs(lat), latDir,
            kotlin.math.abs(lon), lonDir
        )
    }
}
