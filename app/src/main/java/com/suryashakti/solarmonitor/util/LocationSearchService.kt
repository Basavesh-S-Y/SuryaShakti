package com.suryashakti.solarmonitor.util

import com.suryashakti.solarmonitor.data.LocationSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Searches for locations by name using Nominatim (OpenStreetMap).
 * No API key needed. Returns up to 10 results ranked by relevance.
 *
 * Example query: "Whitefield Bangalore" →
 *   [{ lat: 12.9698, lon: 77.7499, name: "Whitefield, Bengaluru Urban, Karnataka, India" }]
 */
object LocationSearchService {

    private const val BASE = "https://nominatim.openstreetmap.org/search"
    private const val TIMEOUT = 10_000

    suspend fun search(query: String): List<LocationSearchResult> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()
            runCatching {
                val encoded = URLEncoder.encode(query.trim(), "UTF-8")
                val url = "$BASE?q=$encoded&format=json&addressdetails=1&limit=10&countrycodes="

                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod  = "GET"
                    connectTimeout = TIMEOUT
                    readTimeout    = TIMEOUT
                    setRequestProperty("User-Agent",
                        "SuryaShaktiApp/3.0 (solar.monitor.app@gmail.com)")
                    setRequestProperty("Accept-Language", "en")
                }

                if (conn.responseCode != 200) return@runCatching emptyList()
                val json = JSONArray(
                    conn.inputStream.bufferedReader().use { it.readText() }
                )
                conn.disconnect()

                val results = mutableListOf<LocationSearchResult>()
                for (i in 0 until json.length()) {
                    val item = json.getJSONObject(i)
                    val addr = item.optJSONObject("address")

                    val lat = item.getString("lat").toDoubleOrNull() ?: continue
                    val lon = item.getString("lon").toDoubleOrNull() ?: continue

                    // Build short name from address parts
                    val neighbourhood = addr?.optString("neighbourhood")?.takeIf { it.isNotBlank() }
                    val suburb        = addr?.optString("suburb")?.takeIf        { it.isNotBlank() }
                    val village       = addr?.optString("village")?.takeIf       { it.isNotBlank() }
                    val town          = addr?.optString("town")?.takeIf          { it.isNotBlank() }
                    val city          = addr?.optString("city")?.takeIf          { it.isNotBlank() }
                    val state         = addr?.optString("state")?.takeIf         { it.isNotBlank() }
                    val country       = addr?.optString("country")?.takeIf       { it.isNotBlank() } ?: ""

                    val local   = neighbourhood ?: suburb ?: village ?: town ?: city ?: ""
                    val broader = (city ?: state ?: "").takeIf { it != local } ?: ""

                    val shortName = listOf(local, broader)
                        .filter { it.isNotBlank() }.joinToString(", ")
                        .ifBlank { item.optString("display_name", "").split(",").take(2).joinToString(",") }

                    val displayName = item.optString("display_name", shortName)
                        .split(",").take(4).joinToString(", ")

                    results.add(
                        LocationSearchResult(
                            displayName = displayName,
                            shortName   = shortName.ifBlank { displayName.split(",").firstOrNull() ?: displayName },
                            latitude    = lat,
                            longitude   = lon,
                            type        = item.optString("type", ""),
                            country     = country
                        )
                    )
                }
                results
            }.getOrElse { emptyList() }
        }
}
