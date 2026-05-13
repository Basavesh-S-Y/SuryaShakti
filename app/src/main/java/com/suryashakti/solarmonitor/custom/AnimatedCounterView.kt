package com.suryashakti.solarmonitor.custom

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * A lightweight custom View that animates a numeric value from 0 → target
 * with optional prefix (₹) and suffix (kWh / kg / %).
 */
class AnimatedCounterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var prefix: String = ""
    var suffix: String = ""
    var textColor: Int = Color.WHITE
    var textSizeSp: Float = 24f
    var bold: Boolean = true

    private var currentValue: Float = 0f
    private var targetValue: Float = 0f
    private var decimals: Int = 2

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        setWillNotDraw(false)
    }

    fun animateTo(target: Float, decimals: Int = 2, durationMs: Long = 1000) {
        this.decimals = decimals
        this.targetValue = target
        ValueAnimator.ofFloat(currentValue, target).apply {
            duration = durationMs
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                currentValue = anim.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun setInstant(value: Float, decimals: Int = 2) {
        this.decimals = decimals
        this.currentValue = value
        this.targetValue = value
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.color = textColor
        paint.textSize = textSizeSp * resources.displayMetrics.scaledDensity
        paint.typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        else Typeface.DEFAULT
        paint.textAlign = Paint.Align.CENTER

        val format = "%.${decimals}f"
        val text = "$prefix${format.format(currentValue)}$suffix"
        val x = width / 2f
        val y = height / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(text, x, y, paint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredW = 200
        val desiredH = (textSizeSp * resources.displayMetrics.scaledDensity * 1.5f).toInt()
        val w = resolveSize(desiredW, widthMeasureSpec)
        val h = resolveSize(desiredH, heightMeasureSpec)
        setMeasuredDimension(w, h)
    }
}
