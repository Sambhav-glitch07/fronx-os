package com.fronx.os

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/** App / settings launching with honest "not available" messages. */
object Launch {
    const val MAPS = "com.google.android.apps.maps"

    private fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

    fun pkg(ctx: Context, pkg: String, label: String) {
        val i = ctx.packageManager.getLaunchIntentForPackage(pkg)
        if (i == null) {
            toast(ctx, "$label isn't installed on this device")
            return
        }
        try {
            ctx.startActivity(i)
        } catch (e: Exception) {
            toast(ctx, "Couldn't open $label")
        }
    }

    fun maps(ctx: Context) = pkg(ctx, MAPS, "Google Maps")

    fun navigate(ctx: Context, query: String) {
        if (query.isBlank()) { maps(ctx); return }
        val i = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + Uri.encode(query)))
        i.setPackage(MAPS)
        try {
            ctx.startActivity(i)
        } catch (e: ActivityNotFoundException) {
            toast(ctx, "Google Maps isn't installed on this device")
        }
    }

    fun dial(ctx: Context, number: String?) {
        val uri = if (number.isNullOrBlank()) Uri.parse("tel:") else Uri.fromParts("tel", number, null)
        try {
            ctx.startActivity(Intent(Intent.ACTION_DIAL, uri))
        } catch (e: ActivityNotFoundException) {
            toast(ctx, "No dialer app available on this device")
        }
    }

    fun settings(ctx: Context, action: String) {
        try {
            ctx.startActivity(Intent(action))
        } catch (e: Exception) {
            toast(ctx, "This settings page isn't available on this device")
        }
    }
}
