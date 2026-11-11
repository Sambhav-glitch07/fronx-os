package com.fronx.os

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.GradientDrawable
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView

enum class Dest(val label: String, val icon: Icon) {
    HOME("Home", Icon.HOME),
    MEDIA("Media", Icon.MEDIA),
    NAVIGATION("Navigation", Icon.NAV),
    PHONE("Phone", Icon.PHONE),
    VEHICLE("Vehicle", Icon.VEHICLE),
    APPS("Apps", Icon.APPS),
    SETTINGS("Settings", Icon.SETTINGS)
}

interface Screen {
    val view: View
    fun onStart() {}
    fun onStop() {}
    fun onPermissionResult() {}
}

/** Single-activity shell: top bar + left rail + swappable screens. */
class MainActivity : Activity() {
    lateinit var fx: FronxTheme
        private set
    val media by lazy { MediaBridge(this) }
    val location by lazy { LocationBridge(this) }

    private lateinit var content: FrameLayout
    private lateinit var pillBt: TextView
    private lateinit var pillWifi: TextView
    private lateinit var pillGps: TextView
    private val railItems = HashMap<Dest, LinearLayout>()
    private val railIcons = HashMap<Dest, IconView>()
    private val railLabels = HashMap<Dest, TextView>()
    private var screen: Screen? = null
    private var dest = Dest.HOME
    private var started = false
    private var netCallback: ConnectivityManager.NetworkCallback? = null

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) { updateStatus() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val b = Prefs.brightness(this)
        if (b > 0f) setBrightness(b)
        buildShell()
        go(Dest.HOME)
    }

    // ---------------------------------------------------------------- shell

    private fun buildShell() {
        fx = ThemeManager.current(this)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(fx.bg)
        root.addView(buildTopBar(), lp(MATCH, dp(52)))

        val body = LinearLayout(this)
        body.orientation = LinearLayout.HORIZONTAL
        body.setPadding(dp(10), dp(2), dp(10), dp(10))
        body.addView(buildRail(), lp(dp(86), MATCH))
        content = FrameLayout(this)
        content.setPadding(dp(10), 0, 0, 0)
        body.addView(content, lp(0, MATCH, 1f))
        root.addView(body, lp(MATCH, 0, 1f))
        setContentView(root)
    }

    private fun buildTopBar(): View {
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setPadding(dp(22), 0, dp(22), 0)

        val mark = View(this)
        val g = GradientDrawable()
        g.setColor(fx.accent)
        g.cornerRadius = dpf(2f)
        mark.background = g
        bar.addView(mark, lp(dp(4), dp(20)).apply { rightMargin = dp(10) })
        val brand = Ui.text(this, "FRONX OS", 16f, fx.text, true)
        brand.letterSpacing = 0.22f
        bar.addView(brand)
        bar.addView(View(this), lp(0, 1, 1f))

        val date = TextClock(this)
        date.format12Hour = "EEE, d MMM yyyy"
        date.format24Hour = "EEE, d MMM yyyy"
        date.setTextColor(fx.textDim)
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        bar.addView(date, lp(WRAP, WRAP).apply { rightMargin = dp(16) })

        val time = TextClock(this)
        time.format12Hour = "h:mm a"
        time.format24Hour = "HH:mm"
        time.setTextColor(fx.text)
        time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        bar.addView(time, lp(WRAP, WRAP).apply { rightMargin = dp(20) })

        pillBt = Ui.pill(this, fx, "BT")
        pillWifi = Ui.pill(this, fx, "Wi-Fi")
        pillGps = Ui.pill(this, fx, "GPS")
        bar.addView(pillBt, lp(WRAP, WRAP).apply { leftMargin = dp(6) })
        bar.addView(pillWifi, lp(WRAP, WRAP).apply { leftMargin = dp(6) })
        bar.addView(pillGps, lp(WRAP, WRAP).apply { leftMargin = dp(6) })
        // Outside temperature: no real source on a typical head unit, so nothing is shown.
        return bar
    }

    private fun buildRail(): View {
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(6), dp(8), dp(6), dp(8))
        col.background = Ui.panel(this, fx, 26f)
        railItems.clear(); railIcons.clear(); railLabels.clear()
        for (d in Dest.values()) {
            val item = LinearLayout(this)
            item.orientation = LinearLayout.VERTICAL
            item.gravity = Gravity.CENTER
            item.isClickable = true
            item.setOnClickListener { go(d) }
            val icon = IconView(this, d.icon, fx.textDim)
            val label = Ui.text(this, d.label, 10f, fx.textDim)
            label.gravity = Gravity.CENTER
            label.maxLines = 1
            item.addView(icon, lp(dp(26), dp(26)))
            item.addView(label, lp(WRAP, WRAP).apply { topMargin = dp(2) })
            railItems[d] = item; railIcons[d] = icon; railLabels[d] = label
            col.addView(item, lp(MATCH, 0, 1f).apply { topMargin = dp(2); bottomMargin = dp(2) })
        }
        return col
    }

    private fun updateRail() {
        for (d in Dest.values()) {
            val sel = d == dest
            railItems[d]?.background = if (sel) Ui.panel(this, fx, 18f, true) else null
            railIcons[d]?.tint = if (sel) fx.accent else fx.textDim
            railLabels[d]?.setTextColor(if (sel) fx.text else fx.textDim)
        }
    }

    fun go(d: Dest) {
        if (started) screen?.onStop()
        dest = d
        val s: Screen = when (d) {
            Dest.HOME -> HomeScreen(this)
            Dest.MEDIA -> MediaScreen(this)
            Dest.NAVIGATION -> NavigationScreen(this)
            Dest.PHONE -> PhoneScreen(this)
            Dest.VEHICLE -> VehicleScreen(this)
            Dest.APPS -> AppsScreen(this)
            Dest.SETTINGS -> SettingsScreen(this)
        }
        screen = s
        content.removeAllViews()
        content.addView(s.view, FrameLayout.LayoutParams(MATCH, MATCH))
        content.alpha = 0f
        content.animate().alpha(1f).setDuration(160L).start()
        if (started) s.onStart()
        updateRail()
    }

    /** Rebuild everything (theme / custom colour change). */
    fun rebuild() {
        if (started) screen?.onStop()
        screen = null
        buildShell()
        updateStatus()
        go(dest)
    }

    fun setBrightness(v: Float) {
        val a = window.attributes
        a.screenBrightness = v   // -1f = follow system
        window.attributes = a
    }

    fun ask(vararg perms: String) {
        requestPermissions(arrayOf(*perms), 7)
    }

    // ------------------------------------------------------------ lifecycle

    override fun onStart() {
        super.onStart()
        started = true
        registerStatus()
        updateStatus()
        media.start()
        screen?.onStart()
    }

    override fun onStop() {
        screen?.onStop()
        media.stop()
        unregisterStatus()
        started = false
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) immersive()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.hasCategory(Intent.CATEGORY_HOME) == true) go(Dest.HOME)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (dest != Dest.HOME) go(Dest.HOME) else moveTaskToBack(true)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        location.refresh()
        screen?.onPermissionResult()
        updateStatus()
    }

    @Suppress("DEPRECATION")
    private fun immersive() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    // --------------------------------------------------------------- status

    private fun registerStatus() {
        val f = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        f.addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(statusReceiver, f, Context.RECEIVER_NOT_EXPORTED)
        else registerReceiver(statusReceiver, f)
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val cb = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { runOnUiThread { updateStatus() } }
                override fun onLost(network: Network) { runOnUiThread { updateStatus() } }
                override fun onCapabilitiesChanged(network: Network, nc: NetworkCapabilities) { runOnUiThread { updateStatus() } }
            }
            cm.registerDefaultNetworkCallback(cb)
            netCallback = cb
        } catch (e: Exception) { }
    }

    private fun unregisterStatus() {
        try { unregisterReceiver(statusReceiver) } catch (e: Exception) { }
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            netCallback?.let { cm.unregisterNetworkCallback(it) }
        } catch (e: Exception) { }
        netCallback = null
    }

    private fun updateStatus() {
        val bt = try {
            (getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter?.isEnabled == true
        } catch (e: SecurityException) { false }
        var wifi = false
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val n = cm.activeNetwork
            wifi = n != null && cm.getNetworkCapabilities(n)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        } catch (e: Exception) { }
        Ui.setPill(this, fx, pillBt, bt)
        Ui.setPill(this, fx, pillWifi, wifi)
        Ui.setPill(this, fx, pillGps, location.gpsEnabled())
    }
}
