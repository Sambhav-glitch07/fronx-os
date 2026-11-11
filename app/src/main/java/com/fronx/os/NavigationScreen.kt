package com.fronx.os

import android.Manifest
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale
import kotlin.math.roundToInt

class NavigationScreen(private val app: MainActivity) : Screen, LocationBridge.Listener {
    private val t = app.fx
    private val locText = Ui.text(app, "", 14f, t.text)
    private lateinit var permBtn: TextView
    private lateinit var dest: EditText

    override val view: View = build()

    private fun build(): View {
        val row = LinearLayout(app)
        row.orientation = LinearLayout.HORIZONTAL

        val left = Ui.card(app, t)
        left.addView(Ui.label(app, t, "NAVIGATION"))
        left.addView(Ui.text(app, "Turn-by-turn navigation is provided by Google Maps.", 13f, t.textDim))
        dest = EditText(app)
        dest.hint = "Where to?"
        dest.setHintTextColor(t.textDim)
        dest.setTextColor(t.text)
        dest.setSingleLine(true)
        dest.imeOptions = EditorInfo.IME_ACTION_GO
        dest.setPadding(app.dp(14), app.dp(12), app.dp(14), app.dp(12))
        dest.background = Ui.panel(app, t, 14f)
        dest.setOnEditorActionListener { _, _, _ -> Launch.navigate(app, dest.text.toString()); true }
        left.addView(dest, lp(MATCH, WRAP).apply { topMargin = app.dp(14) })
        left.addView(Ui.button(app, t, "Start navigation", true) {
            Launch.navigate(app, dest.text.toString())
        }, lp(MATCH, WRAP).apply { topMargin = app.dp(10) })
        left.addView(Ui.button(app, t, "Open Google Maps") { Launch.maps(app) },
            lp(MATCH, WRAP).apply { topMargin = app.dp(8) })
        left.addView(Ui.text(app, "Route preview: not available in FRONX OS. Maps draws the route.", 12f, t.textDim),
            lp(WRAP, WRAP).apply { topMargin = app.dp(14) })
        row.addView(left, lp(0, MATCH, 1.2f).apply { rightMargin = app.dp(8) })

        val right = Ui.card(app, t)
        right.addView(Ui.label(app, t, "LOCATION (GPS)"))
        right.addView(locText)
        permBtn = Ui.button(app, t, "Allow location", true) { app.ask(Manifest.permission.ACCESS_FINE_LOCATION) }
        right.addView(permBtn, lp(MATCH, WRAP).apply { topMargin = app.dp(14) })
        row.addView(right, lp(0, MATCH, 1f))
        return row
    }

    override fun onStart() { app.location.add(this); refresh() }
    override fun onStop() { app.location.remove(this) }
    override fun onPermissionResult() { refresh() }
    override fun onLocationUpdate() { refresh() }

    private fun refresh() {
        val has = app.location.hasPermission()
        permBtn.visibility = if (has) View.GONE else View.VISIBLE
        val l = app.location.fresh()
        locText.text = when {
            !has -> "Location permission not granted."
            !app.location.gpsEnabled() -> "GPS is turned off in system settings."
            l == null -> "Waiting for a GPS fix…"
            else -> {
                val sb = StringBuilder()
                sb.append(String.format(Locale.US, "Latitude   %.5f\nLongitude  %.5f", l.latitude, l.longitude))
                if (l.hasSpeed()) sb.append("\nSpeed      ${(l.speed * 3.6f).roundToInt()} km/h")
                if (l.hasAccuracy()) sb.append("\nAccuracy   ±${l.accuracy.roundToInt()} m")
                if (l.hasAltitude()) sb.append("\nAltitude   ${l.altitude.roundToInt()} m")
                sb.toString()
            }
        }
    }
}
