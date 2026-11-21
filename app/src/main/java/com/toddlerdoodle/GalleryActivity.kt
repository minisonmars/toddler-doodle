package com.toddlerdoodle

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File

/**
 * The saved doodles. The toddler can look through them. With [MANAGE] set
 * (from the parent area), tapping one offers to copy it to Photos or delete it.
 */
class GalleryActivity : Activity() {

    private var manage = false
    private var files: List<File> = emptyList()
    private var viewer: View? = null
    private var pendingCopy: List<File> = emptyList() // waiting for the storage permission
    private var loader: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        manage = intent.getBooleanExtra(MANAGE, false)
        build()
    }

    override fun onResume() {
        super.onResume()
        if (!manage) hideBars()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        build()
    }

    override fun onDestroy() {
        loader?.interrupt()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val v = viewer
        if (v != null) {
            (v.parent as? ViewGroup)?.removeView(v)
            viewer = null
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    @Suppress("DEPRECATION")
    private fun hideBars() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    private fun dp(v: Int) = Ui.dp(this, v)

    private fun build() {
        loader?.interrupt()
        viewer = null
        files = Doodles.list(this)
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val cols = if (landscape) 5 else 3
        val gap = dp(8)
        val cell = (resources.displayMetrics.widthPixels - gap * (cols + 1)) / cols

        val root = FrameLayout(this).apply { setBackgroundColor(Ui.PANEL) }
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(page)

        // Top bar
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(gap, gap, gap, gap)
        }
        bar.addView(RoundIconButton(this, Icon.ARROW_LEFT, Ui.BLUE).apply { setOnClickListener { finish() } },
            LinearLayout.LayoutParams(dp(64), dp(64)))
        bar.addView(TextView(this).apply {
            text = if (manage) "Saved doodles (${files.size})" else ""
            textSize = 20f
            setTextColor(Ui.DARK)
            setPadding(dp(12), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        if (manage && files.isNotEmpty()) {
            bar.addView(textButton("Copy all to Photos") { copy(files) })
            bar.addView(textButton("Delete all") { deleteAll() })
        }
        page.addView(bar)

        if (files.isEmpty()) {
            val empty = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }
            empty.addView(RoundIconButton(this, Icon.GALLERY, Ui.GREY).apply { isClickable = false },
                LinearLayout.LayoutParams(dp(120), dp(120)))
            empty.addView(TextView(this).apply {
                text = "No doodles yet"
                textSize = 20f
                setTextColor(Ui.GREY)
                gravity = Gravity.CENTER
                setPadding(0, dp(12), 0, 0)
            })
            page.addView(empty, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            setContentView(root)
            return
        }

        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(gap, 0, 0, gap)
        }
        val images = mutableListOf<ImageView>()
        var row: LinearLayout? = null
        files.forEachIndexed { i, file ->
            if (i % cols == 0) {
                row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                grid.addView(row)
            }
            val img = ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(Color.WHITE)
                elevation = dp(2).toFloat()
                setOnClickListener { if (manage) manageOne(file) else showFull(i) }
            }
            images.add(img)
            row!!.addView(img, LinearLayout.LayoutParams(cell, cell).apply { setMargins(0, gap, gap, 0) })
        }
        page.addView(ScrollView(this).apply { addView(grid) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)

        // Thumbnails load one by one in the background
        val list = files
        loader = Thread {
            for (i in list.indices) {
                if (Thread.currentThread().isInterrupted) return@Thread
                val b = Doodles.load(list[i], cell) ?: continue
                runOnUiThread { images.getOrNull(i)?.setImageBitmap(b) }
            }
        }.apply { start() }
    }

    private fun textButton(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 15f
        setOnClickListener { onClick() }
    }

    /** One doodle filling the screen, with arrows to the next and previous ones. */
    private fun showFull(start: Int) {
        val root = window.decorView.findViewById<ViewGroup>(android.R.id.content).getChildAt(0) as FrameLayout
        viewer?.let { root.removeView(it) }
        var index = start
        val box = FrameLayout(this).apply {
            setBackgroundColor(Ui.PANEL)
            isClickable = true // stops taps reaching the grid underneath
        }
        val img = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        box.addView(img, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val maxPx = maxOf(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
        fun show() {
            val file = files[index]
            Thread {
                val b = Doodles.load(file, maxPx)
                runOnUiThread { img.setImageBitmap(b) }
            }.start()
        }
        val big = dp(72)
        val m = dp(12)
        box.addView(RoundIconButton(this, Icon.CLOSE, Ui.RED).apply {
            setOnClickListener { root.removeView(box); viewer = null }
        }, FrameLayout.LayoutParams(big, big, Gravity.TOP or Gravity.START).apply { setMargins(m, m, m, m) })
        if (files.size > 1) {
            box.addView(RoundIconButton(this, Icon.ARROW_LEFT, Ui.BLUE).apply {
                setOnClickListener { index = (index - 1 + files.size) % files.size; show() }
            }, FrameLayout.LayoutParams(big, big, Gravity.BOTTOM or Gravity.START).apply { setMargins(m, m, m, m) })
            box.addView(RoundIconButton(this, Icon.ARROW_RIGHT, Ui.BLUE).apply {
                setOnClickListener { index = (index + 1) % files.size; show() }
            }, FrameLayout.LayoutParams(big, big, Gravity.BOTTOM or Gravity.END).apply { setMargins(m, m, m, m) })
        }
        root.addView(box, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        viewer = box
        show()
    }

    // ---------- Parent options ----------

    private fun manageOne(file: File) {
        val index = files.indexOf(file)
        AlertDialog.Builder(this)
            .setTitle("Doodle from ${Doodles.dateOf(file)}")
            .setItems(arrayOf("Look at it", "Copy to Photos", "Delete")) { _, which ->
                when (which) {
                    0 -> showFull(index)
                    1 -> copy(listOf(file))
                    2 -> AlertDialog.Builder(this)
                        .setTitle("Delete this doodle?")
                        .setMessage("It can't be brought back.")
                        .setPositiveButton("Delete") { _, _ ->
                            file.delete()
                            build()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAll() {
        AlertDialog.Builder(this)
            .setTitle("Delete all ${files.size} doodles?")
            .setMessage("They can't be brought back. Copies already in Photos are kept.")
            .setPositiveButton("Delete all") { _, _ ->
                files.forEach { it.delete() }
                build()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun copy(list: List<File>) {
        if (Doodles.copyNeedsPermission() &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingCopy = list
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            return
        }
        Thread {
            val ok = list.count { Doodles.copyToPhotos(this, it) }
            runOnUiThread {
                Ui.toast(
                    this,
                    if (ok == list.size) "Copied $ok to Photos (Pictures > Toddler Doodle)"
                    else "Copied $ok of ${list.size}. Some couldn't be copied."
                )
            }
        }.start()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        val list = pendingCopy
        pendingCopy = emptyList()
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) copy(list)
        else Ui.toast(this, "Copying needs the storage permission")
    }

    companion object {
        const val MANAGE = "manage"
    }
}
