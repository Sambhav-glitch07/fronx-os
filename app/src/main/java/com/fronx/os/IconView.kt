package com.fronx.os

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class Icon { HOME, MEDIA, NAV, PHONE, VEHICLE, APPS, SETTINGS, PLAY, PAUSE, NEXT, PREV }

/** Lightweight vector icons drawn with Canvas paths (24x24 grid). No image assets needed. */
class IconView(ctx: Context, icon: Icon, tint: Int) : View(ctx) {
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.7f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private var path = Path()
    private var filled = false

    var icon: Icon = icon
        set(v) { field = v; rebuild(); invalidate() }

    var tint: Int = tint
        set(v) { field = v; stroke.color = v; fill.color = v; invalidate() }

    init {
        stroke.color = tint
        fill.color = tint
        rebuild()
    }

    private fun rebuild() {
        val p = Path()
        filled = false
        when (icon) {
            Icon.HOME -> {
                p.moveTo(3f, 11f); p.lineTo(12f, 3f); p.lineTo(21f, 11f)
                p.moveTo(5f, 10f); p.lineTo(5f, 21f); p.lineTo(10f, 21f); p.lineTo(10f, 14f)
                p.lineTo(14f, 14f); p.lineTo(14f, 21f); p.lineTo(19f, 21f); p.lineTo(19f, 10f)
            }
            Icon.MEDIA -> {
                p.moveTo(11f, 18f); p.lineTo(11f, 4f); p.lineTo(19f, 6f); p.lineTo(19f, 16f)
                p.addCircle(8f, 18f, 3f, Path.Direction.CW)
                p.addCircle(16f, 16f, 3f, Path.Direction.CW)
            }
            Icon.NAV -> {
                p.moveTo(3f, 11f); p.lineTo(21f, 3f); p.lineTo(13f, 21f); p.lineTo(11f, 13f); p.close()
            }
            Icon.PHONE -> {
                p.moveTo(6f, 3.5f); p.lineTo(9.5f, 3.5f); p.lineTo(11f, 8f); p.lineTo(8.8f, 9.6f)
                p.cubicTo(9.8f, 12f, 12f, 14.2f, 14.4f, 15.2f); p.lineTo(16f, 13f)
                p.lineTo(20.5f, 14.5f); p.lineTo(20.5f, 18f)
                p.cubicTo(20.5f, 19.4f, 19.4f, 20.5f, 18f, 20.5f)
                p.cubicTo(10.5f, 20.5f, 3.5f, 13.5f, 3.5f, 6f)
                p.cubicTo(3.5f, 4.6f, 4.6f, 3.5f, 6f, 3.5f); p.close()
            }
            Icon.VEHICLE -> {
                p.moveTo(2f, 16f); p.lineTo(2f, 12f); p.lineTo(5f, 11f); p.lineTo(8f, 6f)
                p.lineTo(16f, 6f); p.lineTo(19f, 11f); p.lineTo(22f, 12f); p.lineTo(22f, 16f); p.close()
                p.moveTo(5f, 11f); p.lineTo(19f, 11f)
                p.addCircle(7f, 17f, 2.2f, Path.Direction.CW)
                p.addCircle(17f, 17f, 2.2f, Path.Direction.CW)
            }
            Icon.APPS -> {
                p.addRoundRect(RectF(4f, 4f, 10f, 10f), 1.5f, 1.5f, Path.Direction.CW)
                p.addRoundRect(RectF(14f, 4f, 20f, 10f), 1.5f, 1.5f, Path.Direction.CW)
                p.addRoundRect(RectF(4f, 14f, 10f, 20f), 1.5f, 1.5f, Path.Direction.CW)
                p.addRoundRect(RectF(14f, 14f, 20f, 20f), 1.5f, 1.5f, Path.Direction.CW)
            }
            Icon.SETTINGS -> {
                p.addCircle(12f, 12f, 3f, Path.Direction.CW)
                p.addCircle(12f, 12f, 6.5f, Path.Direction.CW)
                for (i in 0 until 8) {
                    val a = Math.PI / 4.0 * i
                    val c = cos(a).toFloat()
                    val s = sin(a).toFloat()
                    p.moveTo(12f + c * 6.5f, 12f + s * 6.5f)
                    p.lineTo(12f + c * 9.3f, 12f + s * 9.3f)
                }
            }
            Icon.PLAY -> {
                filled = true
                p.moveTo(7f, 4f); p.lineTo(20f, 12f); p.lineTo(7f, 20f); p.close()
            }
            Icon.PAUSE -> {
                filled = true
                p.addRect(6f, 5f, 10f, 19f, Path.Direction.CW)
                p.addRect(14f, 5f, 18f, 19f, Path.Direction.CW)
            }
            Icon.NEXT -> {
                filled = true
                p.moveTo(5f, 5f); p.lineTo(16f, 12f); p.lineTo(5f, 19f); p.close()
                p.addRect(17f, 5f, 20f, 19f, Path.Direction.CW)
            }
            Icon.PREV -> {
                filled = true
                p.moveTo(19f, 5f); p.lineTo(8f, 12f); p.lineTo(19f, 19f); p.close()
                p.addRect(4f, 5f, 7f, 19f, Path.Direction.CW)
            }
        }
        path = p
    }

    override fun onDraw(canvas: Canvas) {
        val s = min(width, height) / 24f * 0.85f
        canvas.save()
        canvas.translate((width - 24f * s) / 2f, (height - 24f * s) / 2f)
        canvas.scale(s, s)
        canvas.drawPath(path, if (filled) fill else stroke)
        canvas.restore()
    }
}
