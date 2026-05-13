package com.suryashakti.solarmonitor.util

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.suryashakti.solarmonitor.R
import kotlin.math.abs

enum class AppTheme(val label: String, val marker: String, val prefsKey: String) {
    DARK("Dark", "D", "dark"),
    LIGHT("Light", "L", "light")
}

object ThemeManager {

    private const val PREFS = "surya_theme"
    private const val KEY = "selected_theme"

    data class Palette(
        val isLight: Boolean,
        val background: Int,
        val surface: Int,
        val elevatedSurface: Int,
        val primaryText: Int,
        val secondaryText: Int,
        val mutedText: Int,
        val accent: Int,
        val onAccent: Int,
        val success: Int,
        val danger: Int,
        val outline: Int
    )

    fun getSavedTheme(context: Context): AppTheme {
        val key = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, AppTheme.DARK.prefsKey)
        return AppTheme.values().firstOrNull { it.prefsKey == key } ?: AppTheme.DARK
    }

    fun saveTheme(context: Context, theme: AppTheme) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, theme.prefsKey).apply()
    }

    fun apply(context: Context) {
        val mode = when (getSavedTheme(context)) {
            AppTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            AppTheme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun palette(context: Context): Palette = when (getSavedTheme(context)) {
        AppTheme.DARK -> Palette(
            isLight = false,
            background = Color.rgb(8, 12, 16),
            surface = Color.rgb(20, 25, 31),
            elevatedSurface = Color.rgb(28, 35, 43),
            primaryText = Color.rgb(247, 250, 252),
            secondaryText = Color.rgb(184, 194, 204),
            mutedText = Color.rgb(116, 128, 138),
            accent = Color.rgb(255, 212, 77),
            onAccent = Color.rgb(25, 18, 0),
            success = Color.rgb(46, 230, 166),
            danger = Color.rgb(255, 92, 108),
            outline = Color.rgb(78, 91, 104)
        )
        AppTheme.LIGHT -> Palette(
            isLight = true,
            background = Color.rgb(246, 248, 251),
            surface = Color.WHITE,
            elevatedSurface = Color.rgb(237, 243, 247),
            primaryText = Color.rgb(20, 33, 43),
            secondaryText = Color.rgb(78, 91, 102),
            mutedText = Color.rgb(107, 119, 133),
            accent = Color.rgb(183, 110, 0),
            onAccent = Color.WHITE,
            success = Color.rgb(8, 125, 96),
            danger = Color.rgb(180, 35, 58),
            outline = Color.rgb(200, 210, 219)
        )
    }

    fun applyWindow(window: Window, context: Context) {
        val colors = palette(context)
        window.statusBarColor = colors.background
        window.navigationBarColor = colors.background
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = colors.isLight
            isAppearanceLightNavigationBars = colors.isLight
        }
    }

    fun applyToViewTree(root: View) {
        applyToView(root, palette(root.context), depth = 0)
    }

    fun applyAndRestart(activity: Activity, theme: AppTheme) {
        saveTheme(activity, theme)
        apply(activity)
        applyWindow(activity.window, activity)
        activity.recreate()
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun applyToView(view: View, colors: Palette, depth: Int) {
        when (view) {
            is BottomNavigationView -> {
                view.setBackgroundColor(colors.surface)
                view.itemIconTintList = navColors(colors)
                view.itemTextColor = navColors(colors)
                view.itemRippleColor = ColorStateList.valueOf(withAlpha(colors.accent, 40))
                view.setItemActiveIndicatorColor(ColorStateList.valueOf(withAlpha(colors.accent, 36)))
            }
            is MaterialButton -> styleButton(view, colors)
            is SwitchMaterial -> styleSwitch(view, colors)
            is TextInputLayout -> {
                view.setBoxStrokeColor(colors.accent)
                view.setHintTextColor(ColorStateList.valueOf(colors.secondaryText))
                view.setDefaultHintTextColor(ColorStateList.valueOf(colors.secondaryText))
            }
            is TextInputEditText -> styleEditText(view, colors)
            is EditText -> styleEditText(view, colors)
            is Chip -> {
                view.setTextColor(colors.primaryText)
                view.chipStrokeColor = ColorStateList.valueOf(colors.accent)
                view.chipBackgroundColor = ColorStateList.valueOf(colors.elevatedSurface)
            }
            is MaterialCardView -> {
                view.setCardBackgroundColor(cardColor(view, colors))
                view.setStrokeColor(colors.outline)
            }
            is CardView -> view.setCardBackgroundColor(cardColor(view, colors))
            is LinearProgressIndicator -> view.trackColor = colors.elevatedSurface
            is TextView -> styleText(view, colors)
            is RecyclerView -> view.setBackgroundColor(colors.background)
            is ViewGroup -> if (depth == 0 || view.id == R.id.nav_host_fragment) {
                view.setBackgroundColor(colors.background)
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) applyToView(view.getChildAt(i), colors, depth + 1)
        }
    }

    private fun cardColor(view: View, colors: Palette): Int = when (view.id) {
        R.id.cardProfile2,
        R.id.cardAccountInfo -> colors.elevatedSurface
        R.id.cardPanelLocation -> if (colors.isLight) Color.rgb(241, 249, 236) else Color.rgb(15, 28, 18)
        else -> colors.surface
    }

    private fun styleText(view: TextView, colors: Palette) {
        val current = view.currentTextColor
        val text = view.text?.toString().orEmpty()
        val target = when {
            isClose(current, Color.rgb(255, 215, 0)) -> colors.accent
            isClose(current, Color.rgb(0, 230, 118)) -> colors.success
            isClose(current, Color.rgb(255, 82, 82)) -> colors.danger
            isClose(current, Color.rgb(26, 26, 26)) -> colors.onAccent
            text.contains("saved", ignoreCase = true) -> colors.success
            text.contains("error", ignoreCase = true) -> colors.danger
            view.textSize <= 12f * view.resources.displayMetrics.scaledDensity -> colors.mutedText
            isClose(current, Color.rgb(136, 136, 136)) ||
                isClose(current, Color.rgb(102, 102, 102)) ||
                isClose(current, Color.rgb(170, 170, 170)) -> colors.secondaryText
            else -> colors.primaryText
        }
        view.setTextColor(target)
    }

    private fun styleEditText(view: EditText, colors: Palette) {
        view.setTextColor(colors.primaryText)
        view.setHintTextColor(colors.secondaryText)
        view.backgroundTintList = ColorStateList.valueOf(colors.accent)
    }

    private fun styleButton(view: MaterialButton, colors: Palette) {
        val text = view.text?.toString().orEmpty()
        when {
            text.contains("sign out", ignoreCase = true) -> {
                view.backgroundTintList = ColorStateList.valueOf(withAlpha(colors.danger, if (colors.isLight) 24 else 32))
                view.setStrokeColor(ColorStateList.valueOf(colors.danger))
                view.setTextColor(colors.danger)
            }
            text.contains("change", ignoreCase = true) -> {
                view.backgroundTintList = ColorStateList.valueOf(colors.elevatedSurface)
                view.setStrokeColor(ColorStateList.valueOf(colors.accent))
                view.setTextColor(colors.accent)
            }
            else -> {
                view.backgroundTintList = ColorStateList.valueOf(colors.accent)
                view.setStrokeColor(ColorStateList.valueOf(colors.accent))
                view.setTextColor(colors.onAccent)
            }
        }
    }

    private fun styleSwitch(view: SwitchMaterial, colors: Palette) {
        view.thumbTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(colors.accent, colors.secondaryText)
        )
        view.trackTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(withAlpha(colors.accent, 80), colors.elevatedSurface)
        )
    }

    private fun navColors(colors: Palette) = ColorStateList(
        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
        intArrayOf(colors.accent, colors.mutedText)
    )

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    private fun isClose(color: Int, target: Int): Boolean {
        val dr = abs(Color.red(color) - Color.red(target))
        val dg = abs(Color.green(color) - Color.green(target))
        val db = abs(Color.blue(color) - Color.blue(target))
        return dr + dg + db < 45
    }
}
