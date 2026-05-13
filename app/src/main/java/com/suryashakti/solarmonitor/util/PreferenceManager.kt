package com.suryashakti.solarmonitor.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Lightweight wrapper for SharedPreferences — stores persistent user settings
 * such as panel capacity, unit rate, notifications enabled, etc.
 */
class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Getters ──────────────────────────────────────────────────────────────

    val panelCapacityKw: Double
        get() = prefs.getString(KEY_PANEL_KW, "3.0")?.toDoubleOrNull() ?: 3.0

    val gridRatePerUnit: Double
        get() = prefs.getString(KEY_GRID_RATE, "8.0")?.toDoubleOrNull() ?: 8.0

    val exportRatePerUnit: Double
        get() = prefs.getString(KEY_EXPORT_RATE, "4.0")?.toDoubleOrNull() ?: 4.0

    val notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS, true)

    val isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)

    val userName: String
        get() = prefs.getString(KEY_USER_NAME, "Solar Hero") ?: "Solar Hero"

    // ── Setters ──────────────────────────────────────────────────────────────

    fun setPanelCapacityKw(value: Double) = edit { putString(KEY_PANEL_KW, value.toString()) }
    fun setGridRate(value: Double)        = edit { putString(KEY_GRID_RATE, value.toString()) }
    fun setExportRate(value: Double)      = edit { putString(KEY_EXPORT_RATE, value.toString()) }
    fun setNotifications(enabled: Boolean) = edit { putBoolean(KEY_NOTIFICATIONS, enabled) }
    fun markFirstLaunchDone()             = edit { putBoolean(KEY_FIRST_LAUNCH, false) }
    fun setUserName(name: String)         = edit { putString(KEY_USER_NAME, name) }

    // ── Helper ───────────────────────────────────────────────────────────────

    private inline fun edit(block: SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(block).apply()
    }

    companion object {
        private const val PREFS_NAME      = "surya_shakti_prefs"
        private const val KEY_PANEL_KW    = "panel_capacity_kw"
        private const val KEY_GRID_RATE   = "grid_rate"
        private const val KEY_EXPORT_RATE = "export_rate"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_FIRST_LAUNCH  = "first_launch"
        private const val KEY_USER_NAME     = "user_name"
    }
}
