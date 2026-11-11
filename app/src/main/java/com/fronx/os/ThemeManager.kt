package com.fronx.os

import android.content.Context
import android.graphics.Color

data class FronxTheme(
    val id: String,
    val name: String,
    val bg: Int,
    val panel: Int,
    val panelAlt: Int,
    val border: Int,
    val accent: Int,
    val onAccent: Int,
    val text: Int,
    val textDim: Int
)

object ThemeManager {
    val themes = listOf(
        FronxTheme("graphite", "Graphite & Champagne",
            0xFF0B0B0D.toInt(), 0xFF141416.toInt(), 0xFF1F1E1E.toInt(), 0xFF3C3731.toInt(),
            0xFFE8A468.toInt(), 0xFF1A1208.toInt(), 0xFFF4EEE1.toInt(), 0xFF9C958A.toInt()),
        FronxTheme("steel", "Midnight Steel",
            0xFF080B10.toInt(), 0xFF111720.toInt(), 0xFF1A222D.toInt(), 0xFF2E3A48.toInt(),
            0xFF7FB4E8.toInt(), 0xFF07121D.toInt(), 0xFFEAF0F7.toInt(), 0xFF8794A3.toInt()),
        FronxTheme("olive", "Deep Olive",
            0xFF0B0E0A.toInt(), 0xFF151913.toInt(), 0xFF1F241B.toInt(), 0xFF39402F.toInt(),
            0xFFB8C77A.toInt(), 0xFF141A08.toInt(), 0xFFEEF0E2.toInt(), 0xFF8E9583.toInt())
    )

    /** Base theme + optional user-chosen custom accent colour. */
    fun current(ctx: Context): FronxTheme {
        val base = themes.firstOrNull { it.id == Prefs.theme(ctx) } ?: themes[0]
        val custom = Prefs.customAccent(ctx)
        return if (custom != 0) base.copy(accent = custom, onAccent = contrastOn(custom)) else base
    }

    fun set(ctx: Context, id: String) {
        Prefs.setTheme(ctx, id)
        Prefs.setCustomAccent(ctx, 0) // picking a preset clears the custom colour
    }

    /** Dark text on light accents, light text on dark accents. */
    fun contrastOn(color: Int): Int {
        val lum = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0
        return if (lum > 0.55) 0xFF121212.toInt() else 0xFFFFFFFF.toInt()
    }
}

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("fronx_prefs", Context.MODE_PRIVATE)

    fun theme(c: Context): String = sp(c).getString("theme", "graphite") ?: "graphite"
    fun setTheme(c: Context, id: String) = sp(c).edit().putString("theme", id).apply()

    /** 0 = no custom colour. */
    fun customAccent(c: Context): Int = sp(c).getInt("custom_accent", 0)
    fun setCustomAccent(c: Context, v: Int) = sp(c).edit().putInt("custom_accent", v).apply()

    /** -1 = follow system brightness. */
    fun brightness(c: Context): Float = sp(c).getFloat("brightness", -1f)
    fun setBrightness(c: Context, v: Float) = sp(c).edit().putFloat("brightness", v).apply()
}
