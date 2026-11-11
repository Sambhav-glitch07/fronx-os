package com.fronx.os

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout

class MediaScreen(private val app: MainActivity) : Screen {
    private val t = app.fx
    private val np = NowPlayingView(app, t, true)

    override val view: View = build()

    private fun build(): View {
        val root = FrameLayout(app)
        val card = Ui.card(app, t)
        card.setPadding(app.dp(28), app.dp(24), app.dp(28), app.dp(24))
        card.addView(Ui.label(app, t, "MEDIA"))
        card.addView(np, lp(MATCH, WRAP))
        val shortcuts = LinearLayout(app)
        shortcuts.orientation = LinearLayout.HORIZONTAL
        shortcuts.addView(Ui.button(app, t, "Open Spotify") {
            Launch.pkg(app, "com.spotify.music", "Spotify")
        }, lp(0, WRAP, 1f).apply { rightMargin = app.dp(8) })
        shortcuts.addView(Ui.button(app, t, "Open YouTube") {
            Launch.pkg(app, "com.google.android.youtube", "YouTube")
        }, lp(0, WRAP, 1f))
        card.addView(shortcuts, lp(MATCH, WRAP).apply { topMargin = app.dp(22) })
        val p = FrameLayout.LayoutParams(MATCH, WRAP)
        p.gravity = Gravity.CENTER
        root.addView(card, p)
        return root
    }

    override fun onStart() { np.onStart() }
    override fun onStop() { np.onStop() }
}
