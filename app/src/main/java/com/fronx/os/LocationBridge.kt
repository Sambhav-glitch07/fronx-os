package com.fronx.os

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/** Real GPS data only. Updates run only while at least one screen is listening. */
class LocationBridge(private val ctx: Context) {
    fun interface Listener { fun onLocationUpdate() }

    private val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val listeners = LinkedHashSet<Listener>()
    private val ui = Handler(Looper.getMainLooper())
    private var active = false

    var last: Location? = null
        private set

    private val watch = object : Runnable {
        override fun run() {
            notifyAll2()                 // lets screens notice when a fix has gone stale
            ui.postDelayed(this, 3000L)
        }
    }

    private val ll = object : LocationListener {
        override fun onLocationChanged(location: Location) { last = location; notifyAll2() }
        override fun onProviderEnabled(provider: String) { notifyAll2() }
        override fun onProviderDisabled(provider: String) { last = null; notifyAll2() }
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) { }
    }

    fun hasPermission(): Boolean =
        ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun gpsEnabled(): Boolean = try { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (e: Exception) { false }

    /** A fix younger than 10 s, otherwise null (no stale speed shown). */
    fun fresh(): Location? {
        val l = last ?: return null
        val ageMs = (SystemClock.elapsedRealtimeNanos() - l.elapsedRealtimeNanos) / 1_000_000L
        return if (ageMs <= 10_000L) l else null
    }

    fun add(l: Listener) { listeners.add(l); ensure() }

    fun remove(l: Listener) {
        listeners.remove(l)
        if (listeners.isEmpty()) stopUpdates()
    }

    /** Call after a permission result. */
    fun refresh() {
        stopUpdates()
        if (listeners.isNotEmpty()) ensure()
    }

    private fun notifyAll2() { for (l in ArrayList(listeners)) l.onLocationUpdate() }

    private fun ensure() {
        if (active || !hasPermission()) return
        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, ll)
            active = true
            if (last == null) last = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ui.postDelayed(watch, 3000L)
        } catch (e: SecurityException) {
        } catch (e: IllegalArgumentException) {
        }
    }

    private fun stopUpdates() {
        if (active) {
            try { lm.removeUpdates(ll) } catch (e: Exception) { }
            active = false
        }
        ui.removeCallbacks(watch)
    }
}
