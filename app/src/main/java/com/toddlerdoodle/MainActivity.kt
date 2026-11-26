package com.toddlerdoodle

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import kotlin.math.min

/** The drawing screen: paper, 10 colours, eraser, home, undo, clear, save and gallery. */
class MainActivity : Activity() {

    private lateinit var settings: Settings
    private lateinit var doodle: DoodleView

    private val colourButtons = mutableListOf<RoundIconButton>()
    private var eraserButton: RoundIconButton? = null
    private var frame: FrameLayout? = null

    private var eraserOn = false
    private var savedAt = -1 // the drawing's change count when it was last saved

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = Settings(this)
        doodle = DoodleView(this)
    }

    override fun onResume() {
        super.onResume()
        if (!settings.showEraser) eraserOn = false
        build() // the parent area may have changed which buttons show
        hideBars()
        Updater.checkInBackground(this) // quietly, at most every 6 hours; shown only in the parent area
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        build()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideBars()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Ignored, so a stray tap on Back doesn't lose the drawing. Home still works.
    }

    @Suppress("DEPRECATION")
    private fun hideBars() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    private fun dp(v: Int) = Ui.dp(this, v)

    // ---------- Layout ----------

    /**
     * Portrait: paper on top, buttons underneath. Landscape: paper on the left,
     * buttons down the right. The buttons are in 2 lines: the eraser and the
     * colours (which scroll), then home, undo, clear, save and gallery.
     */
    private fun build() {
        (doodle.parent as? ViewGroup)?.removeView(doodle)
        colourButtons.clear()
        eraserButton = null

        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val metrics = resources.displayMetrics
        val lineLength = if (landscape) metrics.heightPixels else metrics.widthPixels
        val margin = dp(4)
        val cell = min(dp(76), (lineLength - dp(12)) / 5 - margin * 2)

        val f = FrameLayout(this)
        frame = f
        f.addView(doodle, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val gear = HoldButton(this, Icon.GEAR, Color.argb(70, 0, 0, 0), 2000L) { openParent() }
        f.addView(gear, FrameLayout.LayoutParams(dp(52), dp(52), Gravity.TOP or Gravity.END).apply {
            setMargins(dp(8), dp(8), dp(8), dp(8))
        })

        val panel = LinearLayout(this).apply {
            orientation = if (landscape) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Ui.PANEL)
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }

        fun line(views: List<View>) {
            val l = LinearLayout(this).apply {
                orientation = if (landscape) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            for (v in views) {
                l.addView(v, LinearLayout.LayoutParams(cell, cell).apply { setMargins(margin, margin, margin, margin) })
            }
            panel.addView(l)
        }

        // Line 1: the eraser, then the colours, which scroll across if they don't all fit
        val colourLine = LinearLayout(this).apply {
            orientation = if (landscape) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        if (settings.showEraser) {
            eraserButton = RoundIconButton(this, Icon.ERASER, Ui.PURPLE).apply {
                setOnClickListener {
                    eraserOn = !eraserOn
                    refresh()
                }
            }
            colourLine.addView(eraserButton, LinearLayout.LayoutParams(cell, cell).apply { setMargins(margin, margin, margin, margin) })
        }
        val slot = cell + margin * 2
        val scrollRoom = lineLength - dp(12) - (if (settings.showEraser) slot else 0)
        // About four and a half colours show at a time, so the cut-off one hints that there are more
        val colourSlot = min(slot, (scrollRoom / 4.5f).toInt())
        val colourCell = colourSlot - margin * 2
        val colours = LinearLayout(this).apply {
            orientation = if (landscape) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        Ui.CRAYONS.forEachIndexed { i, c ->
            val b = RoundIconButton(this, Icon.NONE, c).apply {
                setOnClickListener {
                    settings.colour = i
                    eraserOn = false
                    refresh()
                }
            }
            colourButtons.add(b)
            colours.addView(b, LinearLayout.LayoutParams(colourCell, colourCell).apply { setMargins(margin, margin, margin, margin) })
        }
        val scroller: ViewGroup = if (landscape) ScrollView(this) else HorizontalScrollView(this)
        scroller.isVerticalScrollBarEnabled = false
        scroller.isHorizontalScrollBarEnabled = false
        scroller.addView(colours)
        colourLine.addView(
            scroller,
            if (landscape) LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 0, 1f)
            else LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        panel.addView(
            colourLine,
            if (landscape) LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
            else LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )

        // Line 2: home, undo, clear, save and gallery
        val actions = mutableListOf<View>()
        actions.add(RoundIconButton(this, Icon.HOME, Ui.GREY).apply { setOnClickListener { goHome() } })
        actions.add(RoundIconButton(this, Icon.UNDO, Ui.ORANGE).apply { setOnClickListener { doodle.undo() } })
        val clear = HoldButton(this, Icon.BIN, Ui.RED, 1000L) { doodle.clear() }
        clear.onTooShort = { wiggle(clear) }
        actions.add(clear)
        if (settings.showSave) {
            actions.add(RoundIconButton(this, Icon.SAVE, Ui.GREEN).apply { setOnClickListener { save() } })
        }
        if (settings.showGallery) {
            actions.add(RoundIconButton(this, Icon.GALLERY, Ui.BLUE).apply {
                setOnClickListener { startActivity(Intent(this@MainActivity, GalleryActivity::class.java)) }
            })
        }
        line(actions)

        val root = LinearLayout(this).apply {
            orientation = if (landscape) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            setBackgroundColor(Ui.PANEL)
        }
        if (landscape) {
            root.addView(f, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
            root.addView(panel, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
        } else {
            root.addView(f, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            root.addView(panel, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        setContentView(root.fitSystemBars())
        refresh()
    }

    /** Shows which colour or the eraser is chosen, and passes them to the paper. */
    private fun refresh() {
        val colour = Ui.CRAYONS[settings.colour]
        colourButtons.forEachIndexed { i, b -> b.chosen = !eraserOn && i == settings.colour }
        eraserButton?.chosen = eraserOn
        doodle.colour = colour
        doodle.eraser = eraserOn
        doodle.widthPx = Ui.BRUSH_DP * resources.displayMetrics.density
    }

    // ---------- Actions ----------

    private fun save() {
        if (doodle.isBlank()) {
            return
        }
        if (savedAt == doodle.changes) {
            showSaved() // already saved, just show the tick again
            return
        }
        val picture = doodle.snapshot() ?: return
        savedAt = doodle.changes
        showSaved()
        Thread {
            try {
                Doodles.save(this, picture)
            } catch (e: Exception) {
                runOnUiThread { Ui.toast(this, "Couldn't save the doodle: ${e.message}") }
            } finally {
                picture.recycle()
            }
        }.start()
    }

    /** A big green tick that pops up and fades away. */
    private fun showSaved() {
        val f = frame ?: return
        val size = dp(140)
        val tick = RoundIconButton(this, Icon.CHECK, Ui.GREEN).apply {
            isClickable = false
            scaleX = 0.4f
            scaleY = 0.4f
        }
        f.addView(tick, FrameLayout.LayoutParams(size, size, Gravity.CENTER))
        tick.animate().scaleX(1f).scaleY(1f).setDuration(250).withEndAction {
            tick.animate().alpha(0f).setStartDelay(500).setDuration(400).withEndAction { f.removeView(tick) }.start()
        }.start()
    }

    /** A little shake, so a quick tap on Clear shows it needs holding. */
    private fun wiggle(v: View) {
        v.animate().rotation(12f).setDuration(70).withEndAction {
            v.animate().rotation(-12f).setDuration(110).withEndAction {
                v.animate().rotation(0f).setDuration(70).start()
            }.start()
        }.start()
    }

    /** Goes to the phone's home screen (Toddler Launcher, if that's the home app). The drawing is kept. */
    private fun goHome() {
        try {
            startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            moveTaskToBack(true)
        }
    }

    private fun openParent() {
        Gate.ask(this, settings) { startActivity(Intent(this, ParentActivity::class.java)) }
    }
}
