package com.fronx.os

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView

private class Contact(val name: String, val number: String)

private class ContactAdapter(
    private val app: MainActivity,
    private val t: FronxTheme,
    private val items: List<Contact>
) : BaseAdapter() {
    override fun getCount() = items.size
    override fun getItem(position: Int): Any = items[position]
    override fun getItemId(position: Int) = position.toLong()
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val c = items[position]
        val row = (convertView as? LinearLayout) ?: LinearLayout(app).also {
            it.orientation = LinearLayout.VERTICAL
            it.setPadding(app.dp(8), app.dp(9), app.dp(8), app.dp(9))
            it.addView(Ui.text(app, "", 15f, t.text, true))
            it.addView(Ui.text(app, "", 12f, t.textDim))
        }
        (row.getChildAt(0) as TextView).text = c.name
        (row.getChildAt(1) as TextView).text = c.number
        return row
    }
}

class PhoneScreen(private val app: MainActivity) : Screen {
    private val t = app.fx
    private val number = StringBuilder()
    private val display = Ui.text(app, "", 26f, t.text, true)
    private val list = ListView(app)
    private val contactsMsg = Ui.text(app, "", 13f, t.textDim)
    private lateinit var contactsBtn: TextView
    private val btState = Ui.text(app, "", 15f, t.text, true)
    private val paired = Ui.text(app, "", 13f, t.textDim)
    private lateinit var btPerm: TextView
    private var alive = false

    override val view: View = build()

    private fun build(): View {
        val row = LinearLayout(app)
        row.orientation = LinearLayout.HORIZONTAL

        // ---- dial pad
        val dial = Ui.card(app, t)
        dial.addView(Ui.label(app, t, "DIAL"))
        display.minHeight = app.dp(34)
        dial.addView(display)
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
        for (r in 0 until 4) {
            val kr = LinearLayout(app)
            for (c in 0 until 3) {
                val k = keys[r * 3 + c]
                kr.addView(Ui.button(app, t, k) { number.append(k); display.text = number.toString() },
                    lp(0, WRAP, 1f).apply { setMargins(app.dp(3), app.dp(3), app.dp(3), app.dp(3)) })
            }
            dial.addView(kr, lp(MATCH, 0, 1f))
        }
        val actions = LinearLayout(app)
        actions.addView(Ui.button(app, t, "←") {
            if (number.isNotEmpty()) number.deleteCharAt(number.length - 1)
            display.text = number.toString()
        }, lp(0, WRAP, 1f).apply { rightMargin = app.dp(6) })
        actions.addView(Ui.button(app, t, "Dial", true) { Launch.dial(app, number.toString()) }, lp(0, WRAP, 2f))
        dial.addView(actions, lp(MATCH, WRAP).apply { topMargin = app.dp(6) })
        row.addView(dial, lp(0, MATCH, 1f).apply { rightMargin = app.dp(8) })

        // ---- contacts
        val contacts = Ui.card(app, t)
        contacts.addView(Ui.label(app, t, "CONTACTS"))
        contacts.addView(contactsMsg)
        contactsBtn = Ui.button(app, t, "Allow contacts", true) { app.ask(Manifest.permission.READ_CONTACTS) }
        contacts.addView(contactsBtn, lp(MATCH, WRAP).apply { topMargin = app.dp(10) })
        list.divider = null
        list.isVerticalScrollBarEnabled = false
        list.setOnItemClickListener { _, _, pos, _ ->
            (list.adapter?.getItem(pos) as? Contact)?.let { Launch.dial(app, it.number) }
        }
        contacts.addView(list, lp(MATCH, 0, 1f))
        row.addView(contacts, lp(0, MATCH, 1.3f).apply { rightMargin = app.dp(8) })

        // ---- bluetooth
        val bt = Ui.card(app, t)
        bt.addView(Ui.label(app, t, "BLUETOOTH"))
        bt.addView(btState)
        bt.addView(Ui.text(app, "Paired devices", 11f, t.accent, true), lp(WRAP, WRAP).apply { topMargin = app.dp(14) })
        bt.addView(paired, lp(WRAP, WRAP).apply { topMargin = app.dp(4) })
        btPerm = Ui.button(app, t, "Allow nearby devices", true) {
            app.ask(Manifest.permission.BLUETOOTH_CONNECT)
        }
        bt.addView(btPerm, lp(MATCH, WRAP).apply { topMargin = app.dp(10) })
        bt.addView(View(app), lp(MATCH, 0, 1f))
        bt.addView(Ui.text(app, "Calls are placed with the system dialer. Hands-free audio depends on the head unit's own Bluetooth stack.", 11f, t.textDim))
        bt.addView(Ui.button(app, t, "Bluetooth settings") {
            Launch.settings(app, Settings.ACTION_BLUETOOTH_SETTINGS)
        }, lp(MATCH, WRAP).apply { topMargin = app.dp(10) })
        row.addView(bt, lp(0, MATCH, 1f))
        return row
    }

    override fun onStart() { alive = true; refreshBt(); loadContacts() }
    override fun onStop() { alive = false }
    override fun onPermissionResult() { refreshBt(); loadContacts() }

    private fun refreshBt() {
        val adapter = try { (app.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter } catch (e: Exception) { null }
        val on = try { adapter?.isEnabled == true } catch (e: SecurityException) { false }
        btState.text = when {
            adapter == null -> "Bluetooth not available"
            on -> "Bluetooth: On"
            else -> "Bluetooth: Off"
        }
        val needPerm = Build.VERSION.SDK_INT >= 31 &&
                app.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        if (needPerm) {
            btPerm.visibility = View.VISIBLE
            paired.text = "Permission needed to list paired devices."
        } else {
            btPerm.visibility = View.GONE
            var names = ""
            try {
                names = adapter?.bondedDevices?.joinToString("\n") { it.name ?: it.address } ?: ""
            } catch (e: SecurityException) { }
            paired.text = if (names.isEmpty()) "None found" else names
        }
    }

    private fun loadContacts() {
        if (app.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            contactsBtn.visibility = View.VISIBLE
            contactsMsg.text = "Contact access is off."
            return
        }
        contactsBtn.visibility = View.GONE
        contactsMsg.text = "Loading…"
        Thread {
            val out = ArrayList<Contact>()
            try {
                val col = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                val num = ContactsContract.CommonDataKinds.Phone.NUMBER
                app.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(col, num), null, null, "$col COLLATE NOCASE ASC"
                )?.use { c ->
                    val ni = c.getColumnIndex(col)
                    val pi = c.getColumnIndex(num)
                    while (c.moveToNext() && out.size < 600) {
                        out.add(Contact(c.getString(ni) ?: "", c.getString(pi) ?: ""))
                    }
                }
            } catch (e: Exception) { }
            app.runOnUiThread {
                if (alive) {
                    list.adapter = ContactAdapter(app, t, out)
                    contactsMsg.text = if (out.isEmpty()) "No contacts found." else "Tap a contact to dial."
                }
            }
        }.start()
    }
}
