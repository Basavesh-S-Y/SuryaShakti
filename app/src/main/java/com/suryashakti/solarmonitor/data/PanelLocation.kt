package com.suryashakti.solarmonitor.data

import android.content.Context

data class PanelLocation(
    val latitude: Double,
    val longitude: Double,
    val displayName: String,      // "Whitefield, Bengaluru"
    val shortName: String,        // "Whitefield"
    val country: String = "India",
    val savedAtMillis: Long = System.currentTimeMillis()
) {
    val coordinateLabel: String
        get() = "%.4f°%s  %.4f°%s".format(
            kotlin.math.abs(latitude),  if (latitude  >= 0) "N" else "S",
            kotlin.math.abs(longitude), if (longitude >= 0) "E" else "W"
        )

    val isValid: Boolean get() = latitude != 0.0 || longitude != 0.0
}

/** Nominatim search result item shown in the picker list */
data class LocationSearchResult(
    val displayName: String,
    val shortName: String,
    val latitude: Double,
    val longitude: Double,
    val type: String = "",
    val country: String = ""
)

object PanelLocationManager {

    private const val PREFS      = "surya_panel_location"
    private const val KEY_LAT    = "panel_lat"
    private const val KEY_LON    = "panel_lon"
    private const val KEY_NAME   = "panel_name"
    private const val KEY_SHORT  = "panel_short"
    private const val KEY_COUNTRY= "panel_country"
    private const val KEY_SET    = "panel_location_set"

    fun isLocationSet(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SET, false)

    fun saveLocation(context: Context, loc: PanelLocation) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putFloat(KEY_LAT,   loc.latitude.toFloat())
            .putFloat(KEY_LON,   loc.longitude.toFloat())
            .putString(KEY_NAME,  loc.displayName)
            .putString(KEY_SHORT, loc.shortName)
            .putString(KEY_COUNTRY, loc.country)
            .putBoolean(KEY_SET, true)
            .apply()
    }

    fun getLocation(context: Context): PanelLocation? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_SET, false)) return null
        return PanelLocation(
            latitude    = prefs.getFloat(KEY_LAT, 0f).toDouble(),
            longitude   = prefs.getFloat(KEY_LON, 0f).toDouble(),
            displayName = prefs.getString(KEY_NAME,  "Unknown") ?: "Unknown",
            shortName   = prefs.getString(KEY_SHORT, "Unknown") ?: "Unknown",
            country     = prefs.getString(KEY_COUNTRY, "India") ?: "India"
        )
    }

    fun clearLocation(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .clear().apply()
    }
}
