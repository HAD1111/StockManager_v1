package com.example.stockmaneger.ui.dashboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.stockmaneger.R
import java.util.Locale
import kotlin.math.ceil

/**
 * Clean, lightweight, professional sales performance bar chart.
 * Renders recognized revenue over time adhering strictly to the Stage 1 design system.
 */
class SalesPerformanceChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class BarEntry(
        val label: String,
        val value: Double,
        val isCurrent: Boolean = false
    )

    private var entries: List<BarEntry> = emptyList()

    private val density = context.resources.displayMetrics.density

    // Design System Colors
    private val colorPrimary = ContextCompat.getColor(context, R.color.primary)
    private val colorAccent = ContextCompat.getColor(context, R.color.accent)
    private val colorBorder = ContextCompat.getColor(context, R.color.border)
    private val colorTextSecondary = ContextCompat.getColor(context, R.color.text_secondary)
    private val colorTextPrimary = ContextCompat.getColor(context, R.color.text_primary)
    private val colorSecondarySurface = ContextCompat.getColor(context, R.color.surface_secondary)

    // Paints
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = colorBorder
        strokeWidth = 1f * density
    }

    private val dmSansTypeface = try {
        ResourcesCompat.getFont(context, R.font.dm_sans)
    } catch (_: Exception) {
        null
    }

    private val axisTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorTextSecondary
        textSize = 10f * density
        textAlign = Paint.Align.CENTER
        typeface = dmSansTypeface
    }

    private val yAxisTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorTextSecondary
        textSize = 9.5f * density
        textAlign = Paint.Align.RIGHT
        typeface = dmSansTypeface
    }

    private val emptyStatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorTextSecondary
        textSize = 12f * density
        textAlign = Paint.Align.CENTER
        typeface = dmSansTypeface
    }

    private val barRect = RectF()

    fun setChartData(items: List<BarEntry>) {
        this.entries = items
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (175 * density).toInt()
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val padLeft = paddingLeft + (38f * density)
        val padRight = width - paddingRight - (12f * density)
        val padTop = paddingTop + (16f * density)
        val padBottom = height - paddingBottom - (26f * density)

        val chartWidth = padRight - padLeft
        val chartHeight = padBottom - padTop

        if (chartWidth <= 0 || chartHeight <= 0) return

        val hasData = entries.isNotEmpty() && entries.any { it.value > 0.005 }

        if (!hasData) {
            // Draw clean baseline
            canvas.drawLine(padLeft, padBottom, padRight, padBottom, gridPaint)

            // Draw centered empty state message
            val centerX = padLeft + (chartWidth / 2f)
            val centerY = padTop + (chartHeight / 2f)
            canvas.drawText("No recognized revenue in this period", centerX, centerY, emptyStatePaint)
            return
        }

        // Calculate a clean ceiling for Y-axis
        val rawMax = entries.maxOfOrNull { it.value } ?: 0.0
        val cleanMax = calculateCeiling(rawMax)

        // Draw 3 horizontal gridlines (0%, 50%, 100%)
        val steps = listOf(0.0, 0.5, 1.0)
        for (step in steps) {
            val y = padBottom - (chartHeight * step).toFloat()
            canvas.drawLine(padLeft, y, padRight, y, gridPaint)

            val labelValue = (cleanMax * step).toInt()
            val yText = "$$labelValue"
            canvas.drawText(yText, padLeft - (6f * density), y + (3.5f * density), yAxisTextPaint)
        }

        // Draw Bars
        val slotWidth = chartWidth / entries.size
        val maxBarWidth = 26f * density
        val minBarWidth = 7f * density
        val barWidth = (slotWidth * 0.44f).coerceIn(minBarWidth, maxBarWidth)
        val cornerRadius = 4f * density

        for (i in entries.indices) {
            val entry = entries[i]
            val slotCenterX = padLeft + (i + 0.5f) * slotWidth

            val barHeight = if (cleanMax > 0.0) {
                ((entry.value / cleanMax) * chartHeight).toFloat()
            } else 0f

            val effectiveHeight = if (entry.value > 0.005) {
                barHeight.coerceAtLeast(3.5f * density)
            } else 0f

            val left = slotCenterX - (barWidth / 2f)
            val right = slotCenterX + (barWidth / 2f)
            val top = padBottom - effectiveHeight
            val bottom = padBottom

            if (effectiveHeight > 0f) {
                barPaint.color = if (entry.isCurrent) colorAccent else colorPrimary
                barRect.set(left, top, right, bottom)
                canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, barPaint)

                // Fill square bottom to make only top corners rounded
                if (effectiveHeight > cornerRadius) {
                    canvas.drawRect(left, bottom - cornerRadius, right, bottom, barPaint)
                }
            } else {
                // Subtle indicator on baseline for 0 value
                barPaint.color = colorSecondarySurface
                canvas.drawCircle(slotCenterX, padBottom - (1.5f * density), 1.5f * density, barPaint)
            }

            // Draw X-axis label
            axisTextPaint.color = if (entry.isCurrent) colorPrimary else colorTextSecondary
            axisTextPaint.isFakeBoldText = entry.isCurrent
            canvas.drawText(entry.label, slotCenterX, padBottom + (16f * density), axisTextPaint)
        }
    }

    private fun calculateCeiling(max: Double): Double {
        if (max <= 10.0) return 10.0
        if (max <= 50.0) return ceil(max / 10.0) * 10.0
        if (max <= 200.0) return ceil(max / 25.0) * 25.0
        if (max <= 1000.0) return ceil(max / 100.0) * 100.0
        if (max <= 5000.0) return ceil(max / 500.0) * 500.0
        return ceil(max / 1000.0) * 1000.0
    }
}
