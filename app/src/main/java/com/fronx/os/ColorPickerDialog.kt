package com.fronx.os

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.Window
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

/**
 * "Customised colour" picker: 10 quick swatches + Hue / Saturation / Brightness sliders
 * with live preview. Apply stores the colour and rebuilds the UI with it as the accent.
 */
class ColorPickerDialog(private val app: MainActivity, private val t: FronxTheme, initial: Int) {
    private val hsv = FloatArray(3)
    private lateinit var preview: View
    private lateinit var hex: TextView
    private lateinit var hue: SeekBar
    private lateinit var sat: SeekBar
    private lateinit var bri: SeekBar

    private val presets = intArrayOf(
        0xFFE8A468.toInt(), 0xFFFF7A3D.toInt(), 0xFFD64545.toInt(), 0xFFE58AA5.toInt(), 0xFF9B7BE0.toInt(),
        0xFF5BA3F5.toInt(), 0xFF3CC4B3.toInt(), 0xFF7BC96F.toInt(), 0xFFF0C550.toInt(), 0xFFEDEDED.toInt()
    )

    init { Color.colorToHSV(initial, hsv) }

    private fun current(): Int = Color.HSVToColor(hsv)

    fun show() {
        val d = Dialog(app)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        d.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val root = LinearLayout(app)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(app.dp(22), app.dp(18), app.dp(22), app.dp(18))
        root.background = Ui.panel(app, t, 24f, true).also { it.setColor(t.panel) }

        root.addView(Ui.label(app, t, "CUSTOM COLOUR"))

        val top = LinearLayout(app)
        top.gravity = Gravity.CENTER_VERTICAL
        preview = View(app)
        top.addView(preview, lp(app.dp(64), app.dp(44)))
        hex = Ui.text(app, "", 18f, t.text, true)
        top.addView(hex, lp(WRAP, WRAP).apply { leftMargin = app.dp(14) })
        root.addView(top, lp(MATCH, WRAP))

        // quick swatches
        val sw = LinearLayout(app)
        for (c in presets) {
            val dot = View(app)
            val g = GradientDrawable()
            g.shape = GradientDrawable.OVAL
            g.setColor(c)
            g.setStroke(app.dp(1), t.border)
            dot.background = g
            dot.setOnClickListener {
                Color.colorToHSV(c, hsv)
                syncSliders()
                updatePreview()
            }
            sw.addView(dot, lp(0, app.dp(30), 1f).apply { setMargins(app.dp(3), 0, app.dp(3), 0) })
        }
        root.addView(sw, lp(MATCH, WRAP).apply { topMargin = app.dp(14) })

        hue = slider("Hue", 360, root) { hsv[0] = it.toFloat() }
        sat = slider("Saturation", 100, root) { hsv[1] = it / 100f }
        bri = slider("Brightness", 75, root) { hsv[2] = 0.25f + it / 100f }
        syncSliders()
        updatePreview()

        val btns = LinearLayout(app)
        btns.addView(Ui.button(app, t, "Reset") {
            Prefs.setCustomAccent(app, 0); d.dismiss(); app.rebuild()
        }, lp(0, WRAP, 1f).apply { rightMargin = app.dp(6) })
        btns.addView(Ui.button(app, t, "Cancel") { d.dismiss() }, lp(0, WRAP, 1f).apply { rightMargin = app.dp(6) })
        btns.addView(Ui.button(app, t, "Apply", true) {
            Prefs.setCustomAccent(app, current() or 0xFF000000.toInt()); d.dismiss(); app.rebuild()
        }, lp(0, WRAP, 1.3f))
        root.addView(btns, lp(MATCH, WRAP).apply { topMargin = app.dp(18) })

        d.setContentView(root)
        d.show()
        d.window?.setLayout(app.dp(470), WRAP)
    }

    private fun slider(name: String, max: Int, parent: LinearLayout, onChange: (Int) -> Unit): SeekBar {
        parent.addView(Ui.text(app, name, 12f, t.textDim), lp(WRAP, WRAP).apply { topMargin = app.dp(12) })
        val s = SeekBar(app)
        s.max = max
        s.progressTintList = ColorStateList.valueOf(t.accent)
        s.thumbTintList = ColorStateList.valueOf(t.accent)
        s.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) { onChange(p); updatePreview() }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        parent.addView(s, lp(MATCH, WRAP))
        return s
    }

    private fun syncSliders() {
        hue.progress = hsv[0].toInt().coerceIn(0, 360)
        sat.progress = (hsv[1] * 100f).toInt().coerceIn(0, 100)
        bri.progress = ((hsv[2] - 0.25f) * 100f).toInt().coerceIn(0, 75)
        // make sure stored value respects the brightness floor
        if (hsv[2] < 0.25f) hsv[2] = 0.25f
    }

    private fun updatePreview() {
        val c = current()
        val g = GradientDrawable()
        g.cornerRadius = app.dpf(12f)
        g.setColor(c)
        preview.background = g
        hex.text = String.format("#%06X", c and 0xFFFFFF)
    }
}
