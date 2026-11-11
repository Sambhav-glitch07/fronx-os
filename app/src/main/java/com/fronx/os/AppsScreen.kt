package com.fronx.os

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class AppsScreen(private val app: MainActivity) : Screen {
    private val t = app.fx
    private val grid = GridView(app)
    private val msg: TextView = Ui.text(app, "Loading apps…", 13f, t.textDim)
    private var alive = false

    override val view: View = build()

    private fun build(): View {
        val card = Ui.card(app, t)
        card.addView(Ui.label(app, t, "APPS"))
        card.addView(msg)
        grid.numColumns = GridView.AUTO_FIT
        grid.columnWidth = app.dp(96)
        grid.stretchMode = GridView.STRETCH_COLUMN_WIDTH
        grid.verticalSpacing = app.dp(8)
        grid.isVerticalScrollBarEnabled = false
        grid.selector = ColorDrawable(alpha(t.accent, 40))
        grid.setOnItemClickListener { _, _, pos, _ ->
            val e = (grid.adapter as? AppAdapter)?.items?.getOrNull(pos) ?: return@setOnItemClickListener
            try { app.startActivity(e.intent) } catch (ex: Exception) {
                Toast.makeText(app, "Couldn't open ${e.label}", Toast.LENGTH_SHORT).show()
            }
        }
        card.addView(grid, lp(MATCH, 0, 1f))
        return card
    }

    override fun onStart() {
        alive = true
        Thread {
            val pm = app.packageManager
            val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val list = try {
                pm.queryIntentActivities(q, 0)
                    .filter { it.activityInfo.packageName != app.packageName }
                    .map { ri ->
                        val pkg = ri.activityInfo.packageName
                        AppEntry(
                            ri.loadLabel(pm).toString(), pkg, ri.loadIcon(pm),
                            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                                .setClassName(pkg, ri.activityInfo.name)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    .sortedBy { it.label.lowercase() }
            } catch (e: Exception) { emptyList() }
            app.runOnUiThread {
                if (alive) {
                    grid.adapter = AppAdapter(app, t, list)
                    msg.text = if (list.isEmpty()) "No launchable apps found." else "${list.size} apps"
                }
            }
        }.start()
    }

    override fun onStop() { alive = false }
}
