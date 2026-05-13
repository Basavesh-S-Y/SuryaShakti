package com.suryashakti.solarmonitor.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class LatLon(val lat: Double, val lon: Double) {
    override fun toString() = "%.4f, %.4f".format(lat, lon)
}

object LocationHelper {

    // Fallback: Bengaluru (used if GPS unavailable / permission denied)
    private val FALLBACK = LatLon(12.9716, 77.5946)

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Returns the best available location:
     *  1. Last-known GPS fix (fastest, no wait)
     *  2. Last-known network fix
     *  3. Bengaluru fallback (if permissions denied or no cached location)
     *
     * This is a suspend fun — safe to call from any coroutine.
     * Resolves in <50 ms since we only read the cached last-known location.
     */
    suspend fun getLocation(context: Context): LatLon {
        if (!hasPermission(context)) return FALLBACK

        return withTimeoutOrNull(3_000) {
            suspendCancellableCoroutine { cont ->
                val manager = context.getSystemService(Context.LOCATION_SERVICE)
                        as LocationManager

                val best: Location? = listOf(
                    LocationManager.GPS_PROVIDER,
                    LocationManager.NETWORK_PROVIDER,
                    LocationManager.PASSIVE_PROVIDER
                ).mapNotNull { provider ->
                    runCatching {
                        if (manager.isProviderEnabled(provider))
                            @Suppress("MissingPermission")
                            manager.getLastKnownLocation(provider)
                        else null
                    }.getOrNull()
                }.maxByOrNull { it.accuracy }   // prefer most accurate fix

                cont.resume(
                    if (best != null) LatLon(best.latitude, best.longitude)
                    else FALLBACK
                )
            }
        } ?: FALLBACK
    }

    /**
     * Returns a saved LatLon from SharedPreferences (written after a
     * successful GPS fix) — used by the background WorkManager worker
     * which cannot request a fresh location.
     */
    fun getSavedLocation(context: Context): LatLon {
        val prefs = context.getSharedPreferences("surya_location", Context.MODE_PRIVATE)
        val lat = prefs.getFloat("lat", FALLBACK.lat.toFloat()).toDouble()
        val lon = prefs.getFloat("lon", FALLBACK.lon.toFloat()).toDouble()
        return LatLon(lat, lon)
    }

    fun saveLocation(context: Context, latLon: LatLon) {
        context.getSharedPreferences("surya_location", Context.MODE_PRIVATE)
            .edit()
            .putFloat("lat", latLon.lat.toFloat())
            .putFloat("lon", latLon.lon.toFloat())
            .apply()
    }
}
