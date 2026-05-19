package com.sdm.agendanusantara.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

/**
 * Custom View: bar chart sederhana untuk menampilkan
 * jumlah tugas selesai per hari dalam 7 hari terakhir.
 *
 * Data diisi via [setData] dengan List<Pair<String, Int>>
 * (label, count).
 */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    // ── Data ────────────────────────────────────────────────────────
    private var data: List<Pair<String, Int>> = emptyList()

    // ── Paints ──────────────────────────────────────────────────────
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F59E0B")   // amber-400 sesuai mockup
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9CA3AF")   // gray-400
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#374151")   // gray-700
        textSize = 26f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val baselinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E5E7EB")   // gray-200
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    // ── Dimensions (resolved in onSizeChanged) ───────────────────────
    private val labelHeight = 48f       // height reserved for day labels
    private val valueHeight = 40f       // height reserved for count above bar
    private val barRadius   = 8f
    private val padding     = 24f

    // ── Public API ───────────────────────────────────────────────────
    /** Pass a list of (dayLabel, completedCount) for up to 7 days. */
    fun setData(entries: List<Pair<String, Int>>) {
        data = entries
        invalidate()
    }

    // ── Drawing ──────────────────────────────────────────────────────
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val w       = width.toFloat()
        val h       = height.toFloat()
        val count   = data.size
        val maxVal  = max(1, data.maxOf { it.second })

        val chartTop    = padding + valueHeight
        val chartBottom = h - labelHeight - padding
        val chartHeight = chartBottom - chartTop

        // Baseline
        canvas.drawLine(padding, chartBottom, w - padding, chartBottom, baselinePaint)

        val totalPadding = padding * 2 + (count - 1) * (padding * 0.5f)
        val barWidth     = (w - totalPadding) / count
        val slotWidth    = (w - padding * 2) / count

        data.forEachIndexed { i, (label, value) ->
            val centerX   = padding + slotWidth * i + slotWidth / 2f
            val barFrac   = value.toFloat() / maxVal
            val barHeight = (chartHeight * barFrac).coerceAtLeast(if (value > 0) barRadius * 2 else 0f)
            val barLeft   = centerX - barWidth / 2f
            val barRight  = centerX + barWidth / 2f
            val barTop    = chartBottom - barHeight
            val barBottom = chartBottom

            // Draw bar (only if value > 0)
            if (value > 0) {
                canvas.drawRoundRect(
                    RectF(barLeft, barTop, barRight, barBottom),
                    barRadius, barRadius,
                    barPaint
                )
            } else {
                // Empty bar placeholder (faint)
                barPaint.alpha = 40
                canvas.drawRoundRect(
                    RectF(barLeft, chartBottom - 6f, barRight, chartBottom),
                    barRadius, barRadius,
                    barPaint
                )
                barPaint.alpha = 255
            }

            // Value label above bar
            if (value > 0) {
                canvas.drawText(
                    value.toString(),
                    centerX,
                    barTop - 8f,
                    valuePaint
                )
            }

            // Day label below baseline
            canvas.drawText(
                label,
                centerX,
                chartBottom + labelHeight * 0.7f,
                labelPaint
            )
        }
    }
}
