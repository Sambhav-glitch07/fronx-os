package com.fronx.os

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView

class SettingsScreen(private val app: MainActivity) : Screen {
    private val t = app.fx

    override val view: View = build()

    private fun gap(): LinearLayout.LayoutParams = lp(MATCH, WRAP).apply { bottomMargin = app.dp(8) }

    private fun build(): View {
        val scroll = ScrollView(app)
        scroll.isFillViewport = true
        scroll.isVerticalScrollBarEnabled = false
        val cols = LinearLayout(app)
        cols.orientation = LinearLayout.HORIZONTAL
        val left = LinearLayout(app)
        left.orientation = LinearLayout.VERTICAL
        val right = LinearLayout(app)
        right.orientation = LinearLayout.VERTICAL

        left.addView(themeCard(), gap())
        left.addView(brightnessCard(), gap())
        left.addView(volumeCard(), gap())

        right.addView(shortcutsCard(), gap())
        right.addView(infoCard(), gap())

        cols.addView(left, lp(0, WRAP, 1f).apply { rightMargin = app.dp(8) })
        cols.addView(right, lp(0, WRAP, 1f))
        scroll.addView(cols, FrameLayout.LayoutParams(MATCH, WRAP))
        return scroll
    }

    // ---------------------------------------------------------------- theme + custom colour

    private fun chip(name: String, dotColor: Int, selected: Boolean, onClick: () -> Unit): View {
        val chip = LinearLayout(app)
        chip.gravity = Gravity.CENTER_VERTICAL
        chip.setPadding(app.dp(14), app.dp(11), app.dp(14), app.dp(11))
        chip.background = Ui.ripple(app, t, Ui.panel(app, t, 14f, selected), 14f)
        chip.isClickable = true
        chip.setOnClickListener { onClick() }
        val dot = View(app)
        val g = GradientDrawable()
        g.shape = GradientDrawable.OVAL
        g.setColor(dotColor)
        dot.background = g
        chip.addView(dot, lp(app.dp(18), app.dp(18)))
        val tv = Ui.text(app, name, 14f, t.text, selected)
        chip.addView(tv, lp(0, WRAP, 1f).apply { leftMargin = app.dp(12) })
        if (selected) chip.addView(Ui.text(app, "ACTIVE", 10f, t.accent, true))
        return chip
    }

    private fun themeCard(): View {
        val card = Ui.card(app, t)
        card.addView(Ui.label(app, t, "THEME"))
        val hasCustom = Prefs.customAccent(app) != 0
        for (th in ThemeManager.themes) {
            val sel = th.id == Prefs.theme(app) && !hasCustom
            card.addView(chip(th.name, th.accent, sel) {
                ThemeManager.set(app, th.id)
                app.rebuild()
            }, lp(MATCH, WRAP).apply { topMargin = app.dp(6) })
        }
        // Customised colour button
        card.addView(chip("Custom colour…", t.accent, hasCustom) {
            ColorPickerDialog(app, t, t.accent).show()
        }, lp(MATCH, WRAP).apply { topMargin = app.dp(6) })
        return card
    }

    // ---------------------------------------------------------------- sliders

    private fun seek(max: Int, value: Int, onChange: (Int) -> Unit, onDone: (Int) -> Unit): SeekBar {
        val s = SeekBar(app)
        s.max = max
        s.progress = value
        s.progressTintList = ColorStateList.valueOf(t.accent)
        s.thumbTintList = ColorStateList.valueOf(t.accent)
        s.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) { if (fromUser) onChange(p) }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) { onDone(sb?.progress ?: 0) }
        })
        return s
    }

    private fun brightnessCard(): View {
        val card = Ui.card(app, t)
        card.addView(Ui.label(app, t, "BRIGHTNESS"))
        val saved = Prefs.brightness(app)
        val start = if (saved > 0f) (saved * 100f).toInt() - 10 else 70
        card.addView(seek(90, start.coerceIn(0, 90),
            { p -> app.setBrightness((p + 10) / 100f) },
            { p -> Prefs.setBrightness(app, (p + 10) / 100f) }))
        card.addView(Ui.text(app, "Applies to FRONX OS only.", 11f, t.textDim))
        val row = LinearLayout(app)
        row.addView(Ui.button(app, t, "Use system level") {
            Prefs.setBrightness(app, -1f); app.setBrightness(-1f)
        }, lp(0, WRAP, 1f).apply { rightMargin = app.dp(6) })
        row.addView(Ui.button(app, t, "Display settings") {
            Launch.settings(app, Settings.ACTION_DISPLAY_SETTINGS)
        }, lp(0, WRAP, 1f))
        card.addView(row, lp(MATCH, WRAP).apply { topMargin = app.dp(10) })
        return card
    }

    private fun volumeCard(): View {
        val card = Ui.card(app, t)
        card.addView(Ui.label(app, t, "SOUND"))
        val am = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        card.addView(seek(max, am.getStreamVolume(AudioManager.STREAM_MUSIC),
            { p -> am.setStreamVolume(AudioManager.STREAM_MUSIC, p, 0) }, { }))
        card.addView(Ui.text(app, "Media volume", 11f, t.textDim))
        card.addView(Ui.button(app, t, "Sound settings") {
            Launch.settings(app, Settings.ACTION_SOUND_SETTINGS)
        }, lp(MATCH, WRAP).apply { topMargin = app.dp(10) })
        return card
    }

    // ---------------------------------------------------------------- shortcuts + info

    private fun shortcutsCard(): View {
        val card = Ui.card(app, t)
        card.addView(Ui.label(app, t, "CONNECTIVITY"))
        fun pair(a: String, aa: String, b: String, ba: String) {
            val r = LinearLayout(app)
            r.addView(Ui.button(app, t, a) { Launch.settings(app, aa) },
                lp(0, WRAP, 1f).apply { rightMargin = app.dp(6) })
            r.addView(Ui.button(app, t, b) { Launch.settings(app, ba) }, lp(0, WRAP, 1f))
            card.addView(r, lp(MATCH, WRAP).apply { topMargin = app.dp(6) })
        }
        pair("Wi-Fi", Settings.ACTION_WIFI_SETTINGS, "Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS)
        pair("Location", Settings.ACTION_LOCATION_SOURCE_SETTINGS,
            "Media access", Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        return card
    }

    private fun infoCard(): View {
        val card = Ui.card(app, t)
        card.addView(Ui.label(app, t, "SYSTEM INFO"))
        val version = try { app.packageManager.getPackageInfo(app.packageName, 0).versionName } catch (e: Exception) { "?" }
        val dm = app.resources.displayMetrics
        card.addView(Ui.kv(app, t, "App", "FRONX OS $version").first)
        card.addView(Ui.kv(app, t, "Device", "${Build.MANUFACTURER} ${Build.MODEL}").first)
        card.addView(Ui.kv(app, t, "Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})").first)
        card.addView(Ui.kv(app, t, "Display", "${dm.widthPixels} × ${dm.heightPixels} @ ${dm.densityDpi} dpi").first)
        return card
    }
}
