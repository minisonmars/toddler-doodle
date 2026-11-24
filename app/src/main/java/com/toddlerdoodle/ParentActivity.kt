package com.toddlerdoodle

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView

/** The parent area. Every change is saved and applied straight away. */
class ParentActivity : Activity() {

    private lateinit var settings: Settings
    private lateinit var scroll: ScrollView
    private lateinit var col: LinearLayout

    private fun dp(v: Int) = Ui.dp(this, v)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = Settings(this)
        col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(24), dp(16), dp(32))
        }
        scroll = ScrollView(this).apply { addView(col) }
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        build()
        // Check GitHub for a new version now, and show it as soon as the answer comes back
        Updater.checkInBackground(this, force = true) { if (!isFinishing) build() }
    }

    private fun build() {
        val y = scroll.scrollY
        col.removeAllViews()
        val rows = Rows(this, col)
        rows.title("Parent area")
        rows.button("Back to drawing") { finish() }

        rows.header("Buttons the child sees")
        rows.switchRow("Save button", settings.showSave) { settings.showSave = it; it }
        rows.switchRow("Eraser", settings.showEraser) { settings.showEraser = it; it }
        rows.switchRow("Gallery button", settings.showGallery) { settings.showGallery = it; it }
        rows.note("Colours, home, undo and clear are always shown. Clear has to be held for 1 second.")

        rows.header("Doodles")
        val count = Doodles.list(this).size
        rows.menuItem(
            "Saved doodles",
            if (count == 1) "1 doodle. Look at, copy to Photos or delete." else "$count doodles. Look at, copy to Photos or delete."
        ) {
            startActivity(Intent(this, GalleryActivity::class.java).putExtra(GalleryActivity.MANAGE, true))
        }

        rows.header("Parent PIN")
        rows.menuItem(
            "Change PIN",
            if (settings.isDefaultPin()) "Still the starting PIN ${Settings.DEFAULT_PIN}. Please change it." else "PIN is set",
            warn = settings.isDefaultPin()
        ) { Gate.changePin(this, settings) { build() } }
        rows.menuItem(
            "Recovery word",
            if (settings.hasRecoveryWord()) "Set. Resets the PIN if you forget it." else "Not set. Without it, a forgotten PIN means losing the doodles.",
            warn = !settings.hasRecoveryWord()
        ) { Gate.changeRecoveryWord(this, settings) { build() } }
        rows.note("To open this area, hold the grey gear on the drawing screen for 2 seconds.")

        rows.header("Updates")
        val newVersion = Updater.available(this)
        val have = Updater.installedVersion(this)
        rows.menuItem(
            "App updates",
            if (newVersion != null) "Version $newVersion is ready to install (you have $have)"
            else "You have version $have. See what changed, or pick another version.",
            warn = newVersion != null
        ) { Updater.showDialog(this) }

        scroll.post { scroll.scrollTo(0, y) }
    }
}
