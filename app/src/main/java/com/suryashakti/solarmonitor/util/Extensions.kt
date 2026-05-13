package com.suryashakti.solarmonitor.util

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar

// ── View helpers ─────────────────────────────────────────────────────────────

fun View.show() { visibility = View.VISIBLE }
fun View.hide() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }

fun View.fadeIn(duration: Long = 300, delay: Long = 0) {
    alpha = 0f
    visibility = View.VISIBLE
    animate()
        .alpha(1f)
        .setDuration(duration)
        .setStartDelay(delay)
        .setInterpolator(DecelerateInterpolator())
        .start()
}

fun View.slideInFromBottom(translationPx: Float = 80f, duration: Long = 400, delay: Long = 0) {
    alpha = 0f
    translationY = translationPx
    animate()
        .alpha(1f)
        .translationY(0f)
        .setDuration(duration)
        .setStartDelay(delay)
        .setInterpolator(DecelerateInterpolator())
        .start()
}

fun View.pulse(scale: Float = 0.92f) {
    animate().scaleX(scale).scaleY(scale).setDuration(80)
        .withEndAction {
            animate().scaleX(1f).scaleY(1f).setDuration(120).start()
        }.start()
}

fun View.snack(
    message: String,
    duration: Int = Snackbar.LENGTH_SHORT,
    actionLabel: String? = null,
    action: (() -> Unit)? = null
) {
    val snack = Snackbar.make(this, message, duration)
    if (actionLabel != null && action != null) {
        snack.setAction(actionLabel) { action() }
    }
    snack.show()
}

// ── Number formatting ─────────────────────────────────────────────────────────

fun Double.toRupeeString(): String  = "₹%.2f".format(this)
fun Double.toKwhString(): String    = "%.2f kWh".format(this)
fun Float.toPercentString(): String = "%.1f%%".format(this)

// ── dp / sp conversions ───────────────────────────────────────────────────────

fun Context.dpToPx(dp: Float): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

fun Context.spToPx(sp: Float): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)

// ── LiveData one-shot observer ────────────────────────────────────────────────

fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
    observe(lifecycleOwner, object : Observer<T> {
        override fun onChanged(value: T) {
            observer.onChanged(value)
            removeObserver(this)
        }
    })
}
