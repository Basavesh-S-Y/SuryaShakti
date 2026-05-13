package com.suryashakti.solarmonitor.custom

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.suryashakti.solarmonitor.R

/**
 * Full-screen animated overlay shown during:
 *   • "Logging in…"    — solar-yellow pulse + spinner
 *   • "Creating account…"
 *   • "Syncing…"       — rotating arc + count
 *   • "Reset sent ✓"   — success green flash
 *   • Error            — red shake + message
 *
 * Usage: call show()/hide() from Fragment; configure via StatusConfig.
 */
class StatusOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class Type { LOADING, SUCCESS, ERROR }

    data class StatusConfig(
        val type: Type,
        val emoji: String,
        val title: String,
        val subtitle: String = "",
        val tintColor: Int = 0xFFFFD700.toInt()
    )

    private val tvEmoji   by lazy { findViewById<TextView>(R.id.tvStatusEmoji) }
    private val tvTitle   by lazy { findViewById<TextView>(R.id.tvStatusTitle) }
    private val tvSub     by lazy { findViewById<TextView>(R.id.tvStatusSub) }
    private val spinner   by lazy { findViewById<CircularProgressIndicator>(R.id.statusSpinner) }
    private val bgPulse   by lazy { findViewById<View>(R.id.viewStatusPulse) }

    private var pulseAnimator: ValueAnimator? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_status_overlay, this, true)
        isVisible = false
        elevation = 999f
        setBackgroundColor(Color.parseColor("#E6000000"))
    }

    fun show(config: StatusConfig) {
        tvEmoji.text = config.emoji
        tvTitle.text = config.title
        tvSub.text   = config.subtitle
        tvSub.isVisible = config.subtitle.isNotBlank()

        spinner.isVisible    = config.type == Type.LOADING
        spinner.setIndicatorColor(config.tintColor)

        when (config.type) {
            Type.LOADING -> startPulse(config.tintColor)
            Type.SUCCESS -> showSuccess(config.tintColor)
            Type.ERROR   -> showError()
        }

        isVisible = true
        alpha     = 0f
        animate().alpha(1f).setDuration(250).start()
    }

    fun hide(delay: Long = 0) {
        postDelayed({
            animate().alpha(0f).setDuration(300).withEndAction {
                isVisible = false
                pulseAnimator?.cancel()
            }.start()
        }, delay)
    }

    private fun startPulse(color: Int) {
        pulseAnimator?.cancel()
        bgPulse.isVisible = true
        bgPulse.setBackgroundColor(color and 0x00FFFFFF or 0x18000000)
        pulseAnimator = ValueAnimator.ofFloat(0.85f, 1.0f).apply {
            duration       = 900
            repeatCount    = ValueAnimator.INFINITE
            repeatMode     = ValueAnimator.REVERSE
            interpolator   = DecelerateInterpolator()
            addUpdateListener {
                val s = it.animatedValue as Float
                tvEmoji.scaleX = s; tvEmoji.scaleY = s
            }
            start()
        }
    }

    private fun showSuccess(color: Int) {
        pulseAnimator?.cancel()
        bgPulse.isVisible = false
        // Quick scale bounce
        tvEmoji.scaleX = 0.5f; tvEmoji.scaleY = 0.5f
        tvEmoji.animate().scaleX(1f).scaleY(1f)
            .setDuration(500)
            .setInterpolator(OvershootInterpolator(2f))
            .start()
    }

    private fun showError() {
        pulseAnimator?.cancel()
        bgPulse.isVisible = false
        // Horizontal shake
        val shake = TranslateAnimation(-20f, 20f, 0f, 0f).apply {
            duration      = 60
            repeatCount   = 5
            repeatMode    = Animation.REVERSE
        }
        tvEmoji.startAnimation(shake)
    }
}
