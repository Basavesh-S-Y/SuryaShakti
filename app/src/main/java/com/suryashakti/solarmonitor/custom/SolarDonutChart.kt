package com.suryashakti.solarmonitor.custom

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.suryashakti.solarmonitor.util.ThemeManager
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Solar donut chart with:
 * - Outer ring: Solar generation (yellow gradient)
 * - Middle ring: Consumed (red/orange) — NEW
 * - Inner arc:  Grid import (red) or export (green)
 * - Clean 3-column legend below the arc
 */
class SolarDonutChart @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Sweep angles (0–300°)
    private var solarSweep    = 0f
    private var consumedSweep = 0f
    private var exportSweep   = 0f
    private var gridSweep     = 0f

    private var centerScore   = 0
    private var subLabel      = ""
    private var glowPulse     = 0f
    private var glowAnimator: ValueAnimator? = null

    // ── Paints ────────────────────────────────────────────────────────────

    private val trackPaintOuter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 26f
        color = Color.parseColor("#222222"); strokeCap = Paint.Cap.ROUND
    }
    private val trackPaintInner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 18f
        color = Color.parseColor("#1A1A1A"); strokeCap = Paint.Cap.ROUND
    }

    private val solarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 26f; strokeCap = Paint.Cap.ROUND
    }
    private val consumedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 18f; strokeCap = Paint.Cap.ROUND
    }
    private val exportPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 10f; strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#00E676")
    }
    private val gridImportPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 10f; strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#FF5252")
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 36f
    }

    private val centerTextBig = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val centerTextSmall = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AAAAAA"); textAlign = Paint.Align.CENTER
    }
    private val subLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD700"); textAlign = Paint.Align.CENTER
    }

    private val dotPaint  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val legPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
        typeface  = Typeface.DEFAULT
    }

    private val outerOval = RectF()
    private val oval      = RectF()
    private val innerOval = RectF()
    private val tinyOval  = RectF()

    init {
        glowAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2200; repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = DecelerateInterpolator()
            addUpdateListener { glowPulse = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    fun setValues(
        solarPercent: Float,    // 0–1: solar vs consumption
        exportPercent: Float,   // 0–1: how much of generation is exported
        gridPercent: Float,     // 0–1: how much is imported from grid
        score: Int,
        sublabel: String = ""
    ) {
        val total = 300f
        val tSolar    = solarPercent.coerceIn(0f, 1f) * total
        val tConsumed = (solarPercent + gridPercent).coerceIn(0f, 1f) * total
        val tExport   = exportPercent.coerceIn(0f, 0.6f) * total
        val tGrid     = gridPercent.coerceIn(0f, 1f) * total
        centerScore = score.coerceIn(0, 100)
        subLabel = sublabel

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1300; interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                val f = anim.animatedValue as Float
                solarSweep    = tSolar * f
                consumedSweep = tConsumed * f
                exportSweep   = tExport * f
                gridSweep     = tGrid * f
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        applyThemeColors()
        val w = width.toFloat(); val h = height.toFloat()
        val size = min(w, h)

        // Reserve bottom strip for legend
        val legendH = size * 0.17f
        val arcH    = h - legendH
        val cx = w / 2f
        val cy = arcH / 2f

        val outerR = (min(w, arcH) / 2f) - 38f
        val midR   = outerR - 36f
        val innerR = outerR - 62f

        val start = 150f   // 150° → gap at bottom

        outerOval.set(cx - outerR, cy - outerR, cx + outerR, cy + outerR)
        oval.set(cx - midR, cy - midR, cx + midR, cy + midR)
        innerOval.set(cx - innerR, cy - innerR, cx + innerR, cy + innerR)

        // ── Track rings ───────────────────────────────────────────────────
        canvas.drawArc(outerOval, start, 300f, false, trackPaintOuter)
        canvas.drawArc(oval,      start, 300f, false, trackPaintInner)

        // ── Glow on solar arc ─────────────────────────────────────────────
        if (solarSweep > 0) {
            val a = (70 + 55 * glowPulse).toInt()
            glowPaint.shader = SweepGradient(cx, cy,
                intArrayOf(Color.argb(0,255,215,0), Color.argb(a,255,215,0), Color.argb(0,255,215,0)),
                floatArrayOf(0f, 0.5f, 1f))
            glowPaint.alpha = a
            outerOval.inset(-6f, -6f)
            canvas.drawArc(outerOval, start, solarSweep, false, glowPaint)
            outerOval.inset(6f, 6f)
        }

        // ── Outer ring: Solar generation (yellow gradient) ─────────────────
        if (solarSweep > 0) {
            solarPaint.shader = SweepGradient(cx, cy,
                intArrayOf(
                    Color.parseColor("#FFC107"),
                    Color.parseColor("#FFD700"),
                    Color.parseColor("#FF8F00")
                ),
                floatArrayOf(0f, 0.5f, 1f))
            canvas.drawArc(outerOval, start, solarSweep, false, solarPaint)
        }

        // ── Middle ring: Consumed (red/orange gradient) ────────────────────
        if (consumedSweep > 0) {
            consumedPaint.shader = SweepGradient(cx, cy,
                intArrayOf(
                    Color.parseColor("#FF6B35"),
                    Color.parseColor("#FF5252"),
                    Color.parseColor("#C62828")
                ),
                floatArrayOf(0f, 0.5f, 1f))
            canvas.drawArc(oval, start, consumedSweep, false, consumedPaint)
        }

        // ── Inner arc: Export (green) or Grid import (red) ────────────────
        if (exportSweep > 0) {
            canvas.drawArc(innerOval, start, exportSweep, false, exportPaint)
        }
        if (gridSweep > 0) {
            canvas.drawArc(innerOval, start + solarSweep, gridSweep, false, gridImportPaint)
        }

        // ── End dot on solar arc ──────────────────────────────────────────
        if (solarSweep > 2f) {
            val rad = Math.toRadians((start + solarSweep).toDouble())
            val dx = (cx + outerR * cos(rad)).toFloat()
            val dy = (cy + outerR * sin(rad)).toFloat()
            dotPaint.color = Color.WHITE
            canvas.drawCircle(dx, dy, 10f, dotPaint)
            dotPaint.color = Color.parseColor("#FFD700")
            canvas.drawCircle(dx, dy, 6f, dotPaint)
        }

        // ── Centre text ────────────────────────────────────────────────────
        val numSz = size * 0.20f
        centerTextBig.textSize = numSz
        canvas.drawText("$centerScore", cx, cy - numSz * 0.06f, centerTextBig)

        centerTextBig.textSize = numSz * 0.36f
        canvas.drawText("%", cx + numSz * 0.58f, cy - numSz * 0.50f, centerTextBig)

        centerTextSmall.textSize = size * 0.050f
        canvas.drawText("Independence", cx, cy + size * 0.080f, centerTextSmall)
        canvas.drawText("Score",        cx, cy + size * 0.130f, centerTextSmall)

        if (subLabel.isNotBlank()) {
            subLabelPaint.textSize = size * 0.040f
            canvas.drawText(subLabel, cx, cy + size * 0.185f, subLabelPaint)
        }

        // ── Legend ─────────────────────────────────────────────────────────
        drawLegend(canvas, cx, arcH, w, legendH, size)
    }

    private fun applyThemeColors() {
        val colors = ThemeManager.palette(context)
        trackPaintOuter.color = colors.elevatedSurface
        trackPaintInner.color = colors.outline
        centerTextBig.color = colors.primaryText
        centerTextSmall.color = colors.secondaryText
        subLabelPaint.color = colors.accent
        legPaint.color = colors.secondaryText
    }

    private fun drawLegend(
        canvas: Canvas, cx: Float, arcBottom: Float,
        viewW: Float, legH: Float, size: Float
    ) {
        val legY  = arcBottom + legH / 2f
        val dotR  = 8f
        val textSz = legH * 0.30f
        val colW  = viewW / 4f   // 4 equal columns

        legPaint.textSize = textSz
        legPaint.color    = Color.parseColor("#CCCCCC")

        data class Item(val color: Int, val label: String)
        val items = listOf(
            Item(Color.parseColor("#FFD700"), "Generated"),
            Item(Color.parseColor("#FF5252"), "Consumed"),
            Item(Color.parseColor("#00E676"), "Exported"),
            Item(Color.parseColor("#FF5252"), "Grid")
        )

        items.forEachIndexed { i, item ->
            val colCx = colW * i + colW / 2f
            val textW = legPaint.measureText(item.label)
            val totalW = dotR * 2 + 5f + textW
            val dotX = colCx - totalW / 2f + dotR

            dotPaint.color = item.color
            canvas.drawCircle(dotX, legY, dotR, dotPaint)
            canvas.drawText(item.label, dotX + dotR + 5f, legY + dotR * 0.4f, legPaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        glowAnimator?.cancel()
    }
}
