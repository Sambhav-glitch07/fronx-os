package com.fronx.os

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class AppEntry(val label: String, val pkg: String, val icon: Drawable, val intent: Intent)

class AppAdapter(
    private val ctx: Context,
    private val t: FronxTheme,
    val items: List<AppEntry>
) : BaseAdapter() {
    override fun getCount() = items.size
    override fun getItem(position: Int): Any = items[position]
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val e = items[position]
        val v = (convertView as? LinearLayout) ?: build()
        (v.getChildAt(0) as ImageView).setImageDrawable(e.icon)
        (v.getChildAt(1) as TextView).text = e.label
        return v
    }

    private fun build(): LinearLayout {
        val v = LinearLayout(ctx)
        v.orientation = LinearLayout.VERTICAL
        v.gravity = Gravity.CENTER_HORIZONTAL
        v.setPadding(ctx.dp(4), ctx.dp(8), ctx.dp(4), ctx.dp(4))
        val iv = ImageView(ctx)
        iv.setPadding(ctx.dp(9), ctx.dp(9), ctx.dp(9), ctx.dp(9))
        iv.background = Ui.panel(ctx, t, 18f)
        v.addView(iv, lp(ctx.dp(62), ctx.dp(62)))
        val tv = Ui.text(ctx, "", 11f, t.text)
        tv.gravity = Gravity.CENTER
        tv.maxLines = 1
        tv.ellipsize = android.text.TextUtils.TruncateAt.END
        v.addView(tv, lp(MATCH, WRAP).apply { topMargin = ctx.dp(6) })
        return v
    }
}
