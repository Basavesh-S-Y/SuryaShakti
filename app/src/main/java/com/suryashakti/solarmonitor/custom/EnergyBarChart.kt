package com.suryashakti.solarmonitor.custom

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.suryashakti.solarmonitor.data.EnergyLog
import com.suryashakti.solarmonitor.util.ThemeManager
import java.text.SimpleDateFormat
import java.util.*

class EnergyBarChart @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var logs: List<EnergyLog> = emptyList()
    private var animFraction = 0f
    private val dateFormat = SimpleDateFormat("d", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())

    private val barWidth = 18f
    private val barSpacing = 10f
    private val paddingH = 60f
    private val paddingBottom = 60f
    private val paddingTop = 40f

    private val generationPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD700")
        style = Paint.Style.FILL
    }

    private val consumptionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF5252")
        style = Paint.Style.FILL
    }

    private val gridLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333")
        style = Paint.Style.STROKE
        strokeWidth = 1f
        pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)
    }

    private val axisLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#888888")
        textSize = 26f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
    }

    private val yLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#888888")
        textSize = 24f
        textAlign = Paint.Align.RIGHT
    }

    private val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
    }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#444444")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    fun setLogs(logs: List<EnergyLog>) {
        this.logs = logs.sortedBy { it.dateMillis }
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                animFraction = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        applyThemeColors()
        if (logs.isEmpty()) {
            drawEmpty(canvas)
            return
        }
        val w = width.toFloat()
        val h = height.toFloat()
        val chartH = h - paddingBottom - paddingTop
        val chartW = w - paddingH * 2

        val maxVal = logs.maxOf { maxOf(it.generatedKwh, it.consumedKwh) } * 1.2
        if (maxVal <= 0) { drawEmpty(canvas); return }

        // Grid lines (horizontal)
        val gridCount = 4
        for (i in 0..gridCount) {
            val y = paddingTop + chartH - (chartH * i / gridCount)
            canvas.drawLine(paddingH, y, w - paddingH, y, gridLinePaint)
            val label = "%.0f".format(maxVal * i / gridCount)
            canvas.drawText(label, paddingH - 8f, y + 8f, yLabelPaint)
        }

        // X axis
        canvas.drawLine(paddingH, paddingTop + chartH, w - paddingH, paddingTop + chartH, axisPaint)

        // Draw bars - show last N bars that fit
        val pairWidth = barWidth * 2 + barSpacing + 4f
        val maxBars = ((chartW) / pairWidth).toInt().coerceAtMost(logs.size)
        val visibleLogs = logs.takeLast(maxBars)
        val totalWidth = maxBars * pairWidth
        val startX = paddingH + (chartW - totalWidth) / 2f + pairWidth / 2

        visibleLogs.forEachIndexed { index, log ->
            val x = startX + index * pairWidth
            val genH = (log.generatedKwh / maxVal * chartH * animFraction).toFloat()
            val conH = (log.consumedKwh / maxVal * chartH * animFraction).toFloat()
            val baseY = paddingTop + chartH

            // Generation bar (yellow)
            val genRect = RectF(x - barWidth - 2, baseY - genH, x - 2, baseY)
            generationPaint.shader = LinearGradient(
                genRect.left, genRect.top, genRect.left, genRect.bottom,
                Color.parseColor("#FFE57F"), Color.parseColor("#FF8F00"),
                Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(genRect, 4f, 4f, generationPaint)

            // Consumption bar (red)
            val conRect = RectF(x + 2, baseY - conH, x + barWidth + 2, baseY)
            consumptionPaint.shader = LinearGradient(
                conRect.left, conRect.top, conRect.left, conRect.bottom,
                Color.parseColor("#FF5252"), Color.parseColor("#B71C1C"),
                Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(conRect, 4f, 4f, consumptionPaint)

            // Date label (every 3rd bar)
            if (index % 3 == 0 || maxBars <= 10) {
                val dateLabel = dateFormat.format(Date(log.dateMillis))
                canvas.drawText(dateLabel, x, baseY + 30f, axisLabelPaint)
            }

            // Export marker (small dot above yellow bar if over-generation)
            if (log.exportedKwh > 0 && animFraction > 0.8f) {
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                dotPaint.color = Color.parseColor("#00E676")
                canvas.drawCircle(x - barWidth / 2 - 2, baseY - genH - 10f, 5f, dotPaint)
            }
        }

        // Legend
        drawLegend(canvas, w, h)
    }

    private fun applyThemeColors() {
        val colors = ThemeManager.palette(context)
        gridLinePaint.color = colors.outline
        axisLabelPaint.color = colors.secondaryText
        yLabelPaint.color = colors.secondaryText
        legendPaint.color = colors.primaryText
        axisPaint.color = colors.outline
    }

    private fun drawLegend(canvas: Canvas, w: Float, h: Float) {
        val y = paddingTop - 10f
        val dotR = 10f
        legendPaint.textSize = 26f

        // Generation legend
        val gDotX = paddingH + 20f
        val gPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD700") }
        canvas.drawCircle(gDotX, y, dotR, gPaint)
        legendPaint.color = Color.parseColor("#FFD700")
        canvas.drawText("Generated", gDotX + 16f, y + 9f, legendPaint)

        // Consumption legend
        val cDotX = gDotX + 160f
        val cPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF5252") }
        canvas.drawCircle(cDotX, y, dotR, cPaint)
        legendPaint.color = Color.parseColor("#FF5252")
        canvas.drawText("Consumed", cDotX + 16f, y + 9f, legendPaint)

        // Export legend
        val eDotX = cDotX + 155f
        val ePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#00E676") }
        canvas.drawCircle(eDotX, y, dotR, ePaint)
        legendPaint.color = Color.parseColor("#00E676")
        canvas.drawText("Exported", eDotX + 16f, y + 9f, legendPaint)

        legendPaint.color = Color.WHITE
    }

    private fun drawEmpty(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ThemeManager.palette(context).secondaryText
            textSize = 40f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("No data yet. Start logging!", width / 2f, height / 2f, paint)
    }
}
