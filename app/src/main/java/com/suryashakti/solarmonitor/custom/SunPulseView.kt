package com.suryashakti.solarmonitor.custom

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * Renders a multi-ring radial pulse animation — purely canvas-drawn,
 * used as the animated background on the splash screen.
 */
class SunPulseView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val rings = 5
    private var pulsePhase = 0f          // 0f → 1f cycling

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 3000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            pulsePhase = it.animatedValue as Float
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val maxR = minOf(cx, cy) * 1.1f

        for (i in 0 until rings) {
            // Each ring has its own phase offset so they stagger outward
            val ringPhase = (pulsePhase + i.toFloat() / rings) % 1f
            val radius = ringPhase * maxR
            val alpha = ((1f - ringPhase) * 160).toInt().coerceIn(0, 255)

            ringPaint.color = Color.argb(alpha, 0xFF, 0xD7, 0x00)
            ringPaint.strokeWidth = (3f - ringPhase * 2f).coerceAtLeast(0.5f)
            canvas.drawCircle(cx, cy, radius, ringPaint)
        }
    }
}
