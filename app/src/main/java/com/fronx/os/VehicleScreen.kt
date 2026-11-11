package com.fronx.os

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale
import kotlin.math.roundToInt

class VehicleScreen(private val app: MainActivity) : Screen, LocationBridge.Listener {
    private val t = app.fx
    private val speed = Ui.text(app, "—", 54f, t.text, true)
    private lateinit var gpsVal: TextView
    private lateinit var altVal: TextView
    private lateinit var accVal: TextView

    override val view: View = build()

    private fun build(): View {
        val row = LinearLayout(app)
        row.orientation = LinearLayout.HORIZONTAL

        val info = Ui.card(app, t)
        info.addView(Ui.label(app, t, "FRONX SIGMA"))
        info.addView(Ui.kv(app, t, "Engine", "1.2 L K-Series petrol").first)
        info.addView(Ui.kv(app, t, "Transmission", "5-speed manual").first)
        info.addView(Ui.kv(app, t, "Power / torque", "≈ 89 hp / 113 Nm").first)
        info.addView(Ui.kv(app, t, "Fuel tank", "≈ 37 L").first)
        info.addView(Ui.text(app,
            "Reference figures, not live data. Check your owner's manual for exact values.", 11f, t.textDim),
            lp(WRAP, WRAP).apply { topMargin = app.dp(10) })
        row.addView(info, lp(0, MATCH, 1f).apply { rightMargin = app.dp(8) })

        val live = Ui.card(app, t)
        live.addView(Ui.label(app, t, "DRIVING (GPS)"))
        val sRow = LinearLayout(app)
        sRow.gravity = android.view.Gravity.BOTTOM
        sRow.addView(speed)
        sRow.addView(Ui.text(app, " km/h", 16f, t.textDim), lp(WRAP, WRAP).apply { bottomMargin = app.dp(8) })
        live.addView(sRow)
        val g = Ui.kv(app, t, "GPS", "—"); gpsVal = g.second; live.addView(g.first)
        val a = Ui.kv(app, t, "Altitude", "—"); altVal = a.second; live.addView(a.first)
        val c = Ui.kv(app, t, "Accuracy", "—"); accVal = c.second; live.addView(c.first)
        live.addView(Ui.kv(app, t, "Fuel level", "Not available").first)
        live.addView(Ui.kv(app, t, "Range", "Not available").first)
        live.addView(Ui.text(app,
            "Fuel and range need a vehicle data connection (CAN / OBD), which this app does not have. Nothing here is simulated.",
            11f, t.textDim), lp(WRAP, WRAP).apply { topMargin = app.dp(8) })
        row.addView(live, lp(0, MATCH, 1.2f))
        return row
    }

    override fun onStart() { app.location.add(this); refresh() }
    override fun onStop() { app.location.remove(this) }
    override fun onLocationUpdate() { refresh() }
    override fun onPermissionResult() { refresh() }

    private fun refresh() {
        val l = app.location.fresh()
        speed.text = if (l != null && l.hasSpeed()) (l.speed * 3.6f).roundToInt().toString() else "—"
        gpsVal.text = when {
            !app.location.hasPermission() -> "Permission off (see Navigation)"
            !app.location.gpsEnabled() -> "GPS off"
            l == null -> "Searching…"
            else -> "Fix OK"
        }
        altVal.text = if (l != null && l.hasAltitude()) "${l.altitude.roundToInt()} m" else "—"
        accVal.text = if (l != null && l.hasAccuracy()) String.format(Locale.US, "±%d m", l.accuracy.roundToInt()) else "—"
    }
}
