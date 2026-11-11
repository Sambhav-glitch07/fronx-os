package com.fronx.os

import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale
import kotlin.math.max

class ThinProgress(ctx: Context, private val track: Int, private val fill: Int) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    var fraction = 0f
        set(v) {
            val n = v.coerceIn(0f, 1f)
            if (n != field) { field = n; invalidate() }
        }

    override fun onDraw(c: Canvas) {
        val h = height.toFloat()
        val r = h / 2f
        p.color = track
        c.drawRoundRect(0f, 0f, width.toFloat(), h, r, r, p)
        p.color = fill
        c.drawRoundRect(0f, 0f, width * fraction, h, r, r, p)
    }
}

/** Static glow + thin ground ring behind the hero car. Drawn once per size change. */
class GlowView(ctx: Context, private val accent: Int) : View(ctx) {
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f }
    private val ring2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f }
    private val r1 = RectF()
    private val r2 = RectF()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        glow.shader = RadialGradient(w * 0.5f, h * 0.62f, max(w, h) * 0.55f,
            alpha(accent, 70), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        ring.color = alpha(accent, 150)
        ring2.color = alpha(accent, 70)
        r1.set(w * 0.16f, h * 0.70f, w * 0.84f, h * 0.92f)
        r2.set(w * 0.10f, h * 0.66f, w * 0.90f, h * 0.97f)
    }

    override fun onDraw(c: Canvas) {
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glow)
        c.drawOval(r1, ring)
        c.drawOval(r2, ring2)
    }
}

/** Now-playing widget used compactly on Home and large on the Media screen. */
class NowPlayingView(
    private val app: MainActivity,
    private val t: FronxTheme,
    private val large: Boolean
) : LinearLayout(app), MediaBridge.Listener {

    private val art = ImageView(app)
    private val title = Ui.text(app, "", if (large) 26f else 15f, t.text, true)
    private val artist = Ui.text(app, "", if (large) 16f else 12f, t.textDim)
    private val bar = ThinProgress(app, alpha(t.text, 40), t.accent)
    private val posTxt = Ui.text(app, "0:00", 12f, t.textDim)
    private val durTxt = Ui.text(app, "--:--", 12f, t.textDim)
    private val prev = IconView(app, Icon.PREV, t.text)
    private val play = IconView(app, Icon.PLAY, t.accent)
    private val next = IconView(app, Icon.NEXT, t.text)
    private val controls = LinearLayout(app)
    private val grant: TextView = Ui.button(app, t, "Allow media access") {
        Launch.settings(app, Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    }
    private val ui = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            if (app.media.isPlaying()) updateProgress()
            ui.postDelayed(this, 1000L)
        }
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val artSize = app.dp(if (large) 220 else 56)
        art.scaleType = ImageView.ScaleType.CENTER_CROP
        art.background = Ui.panel(app, t, 12f)
        art.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, app.dpf(12f))
            }
        }
        art.clipToOutline = true
        addView(art, lp(artSize, artSize))

        val col = LinearLayout(app)
        col.orientation = VERTICAL
        col.gravity = Gravity.CENTER_VERTICAL
        title.maxLines = 1
        title.ellipsize = android.text.TextUtils.TruncateAt.END
        artist.maxLines = 1
        artist.ellipsize = android.text.TextUtils.TruncateAt.END
        col.addView(title)
        col.addView(artist, lp(MATCH, WRAP).apply { topMargin = app.dp(3) })
        col.addView(bar, lp(MATCH, app.dp(4)).apply { topMargin = app.dp(if (large) 18 else 8) })
        if (large) {
            val times = LinearLayout(app)
            times.addView(posTxt, lp(0, WRAP, 1f))
            durTxt.gravity = Gravity.END
            times.addView(durTxt, lp(0, WRAP, 1f))
            col.addView(times, lp(MATCH, WRAP).apply { topMargin = app.dp(6) })
        }
        controls.orientation = HORIZONTAL
        controls.gravity = Gravity.CENTER_VERTICAL
        addCtl(prev, if (large) 52 else 34) { app.media.previous() }
        addCtl(play, if (large) 72 else 40) { app.media.toggle() }
        addCtl(next, if (large) 52 else 34) { app.media.next() }
        col.addView(controls, lp(WRAP, WRAP).apply { topMargin = app.dp(if (large) 14 else 4) })
        col.addView(grant, lp(WRAP, WRAP).apply { topMargin = app.dp(10) })
        addView(col, lp(0, WRAP, 1f).apply { leftMargin = app.dp(if (large) 28 else 12) })
    }

    private fun addCtl(v: IconView, sizeDp: Int, onClick: () -> Unit) {
        v.isClickable = true
        v.background = Ui.circleRipple(t)
        v.setOnClickListener { onClick() }
        controls.addView(v, lp(app.dp(sizeDp), app.dp(sizeDp)).apply {
            leftMargin = app.dp(4); rightMargin = app.dp(4)
        })
    }

    override fun onMediaChanged() { refresh() }

    fun onStart() {
        app.media.add(this)
        refresh()
        ui.removeCallbacks(tick)
        ui.postDelayed(tick, 1000L)
    }

    fun onStop() {
        ui.removeCallbacks(tick)
        app.media.remove(this)
    }

    private fun refresh() {
        val b = app.media
        val access = b.hasAccess()
        val c = b.controller
        grant.visibility = if (access) GONE else VISIBLE
        controls.visibility = if (c != null) VISIBLE else GONE
        when {
            !access -> { title.text = "Media access needed"; artist.text = "Lets FRONX OS show what is playing" }
            c == null -> { title.text = "Nothing playing"; artist.text = "Start playback in any media app" }
            else -> { title.text = b.title() ?: "Unknown title"; artist.text = b.artist() ?: "" }
        }
        art.setImageBitmap(b.art())
        play.icon = if (b.isPlaying()) Icon.PAUSE else Icon.PLAY
        updateProgress()
    }

    private fun fmt(ms: Long): String {
        val s = (ms / 1000L).coerceAtLeast(0L)
        return String.format(Locale.US, "%d:%02d", s / 60L, s % 60L)
    }

    private fun updateProgress() {
        val dur = app.media.durationMs()
        val pos = app.media.positionMs()
        bar.fraction = if (dur > 0L) pos.toFloat() / dur.toFloat() else 0f
        posTxt.text = fmt(pos)
        durTxt.text = if (dur > 0L) fmt(dur) else "--:--"
    }
}
