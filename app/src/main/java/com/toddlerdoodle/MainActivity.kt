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
import android.widget.LinearLayout
import kotlin.math.min

/** The drawing screen: paper, 10 colours, 3 brush sizes, eraser, undo, clear, save and gallery. */
class MainActivity : Activity() {

    private lateinit var settings: Settings
    private lateinit var doodle: DoodleView

    private val colourButtons = mutableListOf<RoundIconButton>()
    private val sizeButtons = mutableListOf<RoundIconButton>()
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
     * buttons down the right. The buttons are in 4 lines: 5 colours, 5 colours,
     * brush sizes and eraser, then undo, clear, save and gallery.
     */
    private fun build() {
        (doodle.parent as? ViewGroup)?.removeView(doodle)
        colourButtons.clear()
        sizeButtons.clear()
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

        Ui.CRAYONS.forEachIndexed { i, c ->
            colourButtons.add(RoundIconButton(this, Icon.NONE, c).apply {
                setOnClickListener {
                    settings.colour = i
                    eraserOn = false
                    refresh()
                }
            })
        }
        line(colourButtons.subList(0, 5))
        line(colourButtons.subList(5, 10))

        val tools = mutableListOf<View>()
        val dotSizes = floatArrayOf(0.09f, 0.17f, 0.27f)
        for (i in 0..2) {
            sizeButtons.add(RoundIconButton(this, Icon.DOT, Color.WHITE).apply {
                dotSize = dotSizes[i]
                setOnClickListener {
                    settings.size = i
                    refresh()
                }
            })
        }
        tools.addAll(sizeButtons)
        if (settings.showEraser) {
            eraserButton = RoundIconButton(this, Icon.ERASER, Ui.PURPLE).apply {
                setOnClickListener {
                    eraserOn = !eraserOn
                    refresh()
                }
            }
            tools.add(eraserButton!!)
        }
        line(tools)

        val actions = mutableListOf<View>()
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
        setContentView(root)
        refresh()
    }

    /** Shows which colour, size and tool are chosen, and passes them to the paper. */
    private fun refresh() {
        val colour = Ui.CRAYONS[settings.colour]
        colourButtons.forEachIndexed { i, b -> b.chosen = !eraserOn && i == settings.colour }
        sizeButtons.forEachIndexed { i, b ->
            b.chosen = i == settings.size
            b.dotColour = if (eraserOn) Color.WHITE else colour
        }
        eraserButton?.chosen = eraserOn
        doodle.colour = colour
        doodle.eraser = eraserOn
        doodle.widthPx = Ui.SIZES_DP[settings.size] * resources.displayMetrics.density
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

    private fun openParent() {
        Gate.ask(this, settings) { startActivity(Intent(this, ParentActivity::class.java)) }
    }
}
