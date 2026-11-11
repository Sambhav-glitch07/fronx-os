package com.fronx.os

import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale
import kotlin.math.roundToInt

class HomeScreen(private val app: MainActivity) : Screen, LocationBridge.Listener {
    private val t = app.fx
    private val nowPlaying = NowPlayingView(app, t, false)
    private val navSub = Ui.text(app, "", 12f, t.textDim)
    private lateinit var speedVal: TextView
    private val hero = ImageView(app)
    private var animated = false

    override val view: View = build()

    private fun build(): View {
        val row = LinearLayout(app)
        row.orientation = LinearLayout.HORIZONTAL

        // ---- centre: hero stage + dock
        val center = LinearLayout(app)
        center.orientation = LinearLayout.VERTICAL
        val stage = FrameLayout(app)
        stage.addView(GlowView(app, t.accent), FrameLayout.LayoutParams(MATCH, MATCH))

        val id = app.resources.getIdentifier("fronx_hero", "drawable", app.packageName)
        hero.setImageResource(if (id != 0) id else R.drawable.fronx_placeholder)
        hero.scaleType = ImageView.ScaleType.FIT_CENTER
        hero.alpha = 0f
        val hp = FrameLayout.LayoutParams(MATCH, MATCH)
        hp.gravity = Gravity.CENTER
        hp.setMargins(app.dp(24), app.dp(44), app.dp(24), app.dp(28))
        stage.addView(hero, hp)

        val titleBox = LinearLayout(app)
        titleBox.orientation = LinearLayout.VERTICAL
        val name = Ui.text(app, "FRONX", 34f, t.text, true)
        name.typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC)
        name.letterSpacing = 0.08f
        val sub = Ui.text(app, "SIGMA", 12f, t.accent, true)
        sub.letterSpacing = 0.4f
        titleBox.addView(name)
        titleBox.addView(sub, lp(WRAP, WRAP).apply { topMargin = app.dp(4) })
        val tp = FrameLayout.LayoutParams(WRAP, WRAP)
        tp.gravity = Gravity.TOP or Gravity.START
        tp.setMargins(app.dp(24), app.dp(14), 0, 0)
        stage.addView(titleBox, tp)

        center.addView(stage, lp(MATCH, 0, 1f))
        center.addView(buildDock(), lp(MATCH, app.dp(88)).apply { topMargin = app.dp(8) })
        row.addView(center, lp(0, MATCH, 2f))

        // ---- right: info panels
        val scroll = ScrollView(app)
        scroll.isFillViewport = true
        scroll.isVerticalScrollBarEnabled = false
        val col = LinearLayout(app)
        col.orientation = LinearLayout.VERTICAL

        val music = Ui.card(app, t) { app.go(Dest.MEDIA) }
        music.gravity = Gravity.CENTER_VERTICAL
        music.minimumHeight = app.dp(112)
        music.addView(Ui.label(app, t, "NOW PLAYING"))
        music.addView(nowPlaying, lp(MATCH, WRAP))
        col.addView(music, lp(MATCH, 0, 1.5f).apply { bottomMargin = app.dp(8) })

        val nav = Ui.card(app, t) { Launch.maps(app) }
        nav.minimumHeight = app.dp(84)
        nav.addView(Ui.label(app, t, "NAVIGATION"))
        val navRow = LinearLayout(app)
        navRow.gravity = Gravity.CENTER_VERTICAL
        navRow.addView(IconView(app, Icon.NAV, t.accent), lp(app.dp(30), app.dp(30)))
        val navCol = LinearLayout(app)
        navCol.orientation = LinearLayout.VERTICAL
        navCol.addView(Ui.text(app, "Open Google Maps", 15f, t.text, true))
        navCol.addView(navSub, lp(WRAP, WRAP).apply { topMargin = app.dp(3) })
        navRow.addView(navCol, lp(0, WRAP, 1f).apply { leftMargin = app.dp(10) })
        nav.addView(navRow)
        col.addView(nav, lp(MATCH, 0, 1f).apply { bottomMargin = app.dp(8) })

        val veh = Ui.card(app, t) { app.go(Dest.VEHICLE) }
        veh.minimumHeight = app.dp(84)
        veh.addView(Ui.label(app, t, "VEHICLE"))
        veh.addView(Ui.kv(app, t, "Fuel / range", "Not available").first)
        val sp = Ui.kv(app, t, "Speed (GPS)", "—")
        speedVal = sp.second
        veh.addView(sp.first)
        col.addView(veh, lp(MATCH, 0, 1f).apply { bottomMargin = app.dp(8) })

        val wx = Ui.card(app, t)
        wx.minimumHeight = app.dp(70)
        wx.addView(Ui.label(app, t, "WEATHER"))
        wx.addView(Ui.text(app, "Not available", 15f, t.text, true))
        wx.addView(Ui.text(app, "No weather source is configured", 12f, t.textDim),
            lp(WRAP, WRAP).apply { topMargin = app.dp(3) })
        col.addView(wx, lp(MATCH, 0, 0.8f))

        scroll.addView(col, FrameLayout.LayoutParams(MATCH, MATCH))
        row.addView(scroll, lp(0, MATCH, 1f).apply { leftMargin = app.dp(10) })
        return row
    }

    private fun buildDock(): View {
        val dock = LinearLayout(app)
        dock.orientation = LinearLayout.HORIZONTAL
        dock.gravity = Gravity.CENTER_VERTICAL
        dock.background = Ui.panel(app, t, 26f)
        dock.setPadding(app.dp(8), app.dp(6), app.dp(8), app.dp(6))
        fun add(label: String, icon: View, onClick: () -> Unit) {
            dock.addView(tile(label, icon, onClick), lp(0, MATCH, 1f))
        }
        add("Maps", pkgIcon(Launch.MAPS, "Maps")) { Launch.maps(app) }
        add("Spotify", pkgIcon("com.spotify.music", "Spotify")) { Launch.pkg(app, "com.spotify.music", "Spotify") }
        add("YouTube", pkgIcon("com.google.android.youtube", "YouTube")) { Launch.pkg(app, "com.google.android.youtube", "YouTube") }
        add("Phone", IconView(app, Icon.PHONE, t.accent)) { Launch.dial(app, null) }
        add("Android Auto", pkgIcon("com.google.android.projection.gearhead", "Android Auto")) {
            Launch.pkg(app, "com.google.android.projection.gearhead", "Android Auto")
        }
        add("Settings", IconView(app, Icon.SETTINGS, t.accent)) { app.go(Dest.SETTINGS) }
        return dock
    }

    private fun tile(label: String, icon: View, onClick: () -> Unit): View {
        val c = LinearLayout(app)
        c.orientation = LinearLayout.VERTICAL
        c.gravity = Gravity.CENTER
        c.isClickable = true
        c.background = Ui.ripple(app, t, ColorDrawable(Color.TRANSPARENT), 18f)
        c.setOnClickListener { onClick() }
        c.addView(icon, lp(app.dp(40), app.dp(40)))
        val tv = Ui.text(app, label, 11f, t.text)
        tv.gravity = Gravity.CENTER
        tv.maxLines = 1
        c.addView(tv, lp(WRAP, WRAP).apply { topMargin = app.dp(4) })
        return c
    }

    /** Real installed-app icon if present, else a monogram. No third-party logo assets are bundled. */
    private fun pkgIcon(pkg: String, label: String): View {
        return try {
            val iv = ImageView(app)
            iv.setImageDrawable(app.packageManager.getApplicationIcon(pkg))
            iv
        } catch (e: PackageManager.NameNotFoundException) {
            val tv = Ui.text(app, label.take(1), 18f, t.accent, true)
            tv.gravity = Gravity.CENTER
            tv.background = Ui.panel(app, t, 12f)
            tv
        }
    }

    override fun onStart() {
        app.location.add(this)
        nowPlaying.onStart()
        refresh()
        if (!animated) {
            animated = true
            hero.translationY = app.dpf(14f)
            hero.animate().alpha(1f).translationY(0f).setDuration(600L)
                .setInterpolator(DecelerateInterpolator()).start()
        } else hero.alpha = 1f
    }

    override fun onStop() {
        nowPlaying.onStop()
        app.location.remove(this)
    }

    override fun onLocationUpdate() { refresh() }

    private fun refresh() {
        val l = app.location.fresh()
        navSub.text = when {
            !app.location.hasPermission() -> "Allow location in Navigation"
            l == null -> "Waiting for GPS…"
            else -> String.format(Locale.US, "%.4f, %.4f", l.latitude, l.longitude)
        }
        speedVal.text = if (l != null && l.hasSpeed()) "${(l.speed * 3.6f).roundToInt()} km/h" else "—"
    }
}
