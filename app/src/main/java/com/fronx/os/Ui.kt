package com.fronx.os

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()
fun Context.dpf(v: Float): Float = v * resources.displayMetrics.density
fun alpha(color: Int, a: Int): Int = Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
fun lp(w: Int, h: Int, weight: Float = 0f) = LinearLayout.LayoutParams(w, h, weight)

/** Small helpers so every screen shares one visual language. */
object Ui {
    fun panel(ctx: Context, t: FronxTheme, radiusDp: Float = 22f, highlight: Boolean = false): GradientDrawable {
        val g = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(t.panelAlt, t.panel))
        g.cornerRadius = ctx.dpf(radiusDp)
        g.setStroke(ctx.dp(1), if (highlight) t.accent else t.border)
        return g
    }

    fun ripple(ctx: Context, t: FronxTheme, content: Drawable?, radiusDp: Float): RippleDrawable {
        val mask = GradientDrawable()
        mask.setColor(Color.WHITE)
        mask.cornerRadius = ctx.dpf(radiusDp)
        return RippleDrawable(ColorStateList.valueOf(alpha(t.accent, 70)), content, mask)
    }

    fun circleRipple(t: FronxTheme): RippleDrawable {
        val mask = GradientDrawable()
        mask.shape = GradientDrawable.OVAL
        mask.setColor(Color.WHITE)
        return RippleDrawable(ColorStateList.valueOf(alpha(t.accent, 70)), null, mask)
    }

    fun text(ctx: Context, s: String, sp: Float, color: Int, bold: Boolean = false): TextView {
        val tv = TextView(ctx)
        tv.text = s
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        tv.setTextColor(color)
        if (bold) tv.typeface = Typeface.DEFAULT_BOLD
        tv.includeFontPadding = false
        return tv
    }

    fun label(ctx: Context, t: FronxTheme, s: String): TextView {
        val tv = text(ctx, s, 10f, t.accent, true)
        tv.letterSpacing = 0.18f
        tv.setPadding(0, 0, 0, ctx.dp(6))
        return tv
    }

    fun card(ctx: Context, t: FronxTheme, onClick: (() -> Unit)? = null): LinearLayout {
        val c = LinearLayout(ctx)
        c.orientation = LinearLayout.VERTICAL
        c.setPadding(ctx.dp(16), ctx.dp(12), ctx.dp(16), ctx.dp(12))
        val bg = panel(ctx, t)
        if (onClick != null) {
            c.background = ripple(ctx, t, bg, 22f)
            c.isClickable = true
            c.setOnClickListener { onClick() }
        } else {
            c.background = bg
        }
        return c
    }

    fun button(ctx: Context, t: FronxTheme, label: String, primary: Boolean = false, onClick: () -> Unit): TextView {
        val tv = text(ctx, label, 14f, if (primary) t.onAccent else t.text, true)
        tv.gravity = Gravity.CENTER
        tv.setPadding(ctx.dp(16), ctx.dp(12), ctx.dp(16), ctx.dp(12))
        val bg: Drawable = if (primary) {
            val g = GradientDrawable()
            g.setColor(t.accent)
            g.cornerRadius = ctx.dpf(14f)
            g
        } else panel(ctx, t, 14f)
        tv.background = ripple(ctx, t, bg, 14f)
        tv.isClickable = true
        tv.setOnClickListener { onClick() }
        return tv
    }

    fun pill(ctx: Context, t: FronxTheme, label: String): TextView {
        val tv = text(ctx, label, 11f, t.textDim, true)
        tv.letterSpacing = 0.08f
        tv.setPadding(ctx.dp(10), ctx.dp(5), ctx.dp(10), ctx.dp(5))
        setPill(ctx, t, tv, false)
        return tv
    }

    fun setPill(ctx: Context, t: FronxTheme, tv: TextView, on: Boolean) {
        tv.setTextColor(if (on) t.accent else t.textDim)
        val g = GradientDrawable()
        g.cornerRadius = ctx.dpf(12f)
        g.setStroke(ctx.dp(1), if (on) t.accent else t.border)
        tv.background = g
    }

    /** Key / value row. Returns the row and its value TextView for later updates. */
    fun kv(ctx: Context, t: FronxTheme, key: String, value: String): Pair<LinearLayout, TextView> {
        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(0, ctx.dp(7), 0, ctx.dp(7))
        row.addView(text(ctx, key, 13f, t.textDim), lp(0, WRAP, 1f))
        val v = text(ctx, value, 14f, t.text)
        v.gravity = Gravity.END
        row.addView(v, lp(0, WRAP, 1.4f))
        return Pair(row, v)
    }
}
