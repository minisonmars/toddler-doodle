package com.toddlerdoodle

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/**
 * The drawing paper. Every stroke is kept in a list, so the last one can be
 * undone (even a clear can be undone). Several fingers can draw at once.
 */
@SuppressLint("ClickableViewAccessibility", "ViewConstructor")
class DoodleView(context: Context) : View(context) {

    private class Stroke(val colour: Int, val width: Float, val x0: Float, val y0: Float) {
        val path = Path().apply { moveTo(x0, y0) }
        var lastX = x0
        var lastY = y0
        var moved = false
    }

    /** Marks where the paper was cleared. Undo removes it, which brings the drawing back. */
    private object ClearMark

    private val ops = ArrayList<Any>()
    private val active = HashMap<Int, Stroke>() // finger id to the stroke it's drawing
    private var bitmap: Bitmap? = null
    private var paper: Canvas? = null

    var colour = Color.BLACK
    var widthPx = 20f
    var eraser = false

    /** Goes up every time the drawing changes, so we know if it has been saved since. */
    var changes = 0
        private set

    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (w <= 0 || h <= 0) return
        bitmap?.recycle()
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap = b
        paper = Canvas(b)
        redraw()
    }

    private fun lastClear() = ops.indexOfLast { it === ClearMark }

    private fun redraw() {
        val c = paper ?: return
        c.drawColor(Color.WHITE)
        for (i in lastClear() + 1 until ops.size) {
            val op = ops[i]
            if (op is Stroke && op !in active.values) drawStroke(c, op)
        }
        invalidate()
    }

    private fun drawStroke(c: Canvas, s: Stroke) {
        if (!s.moved) {
            dot.color = s.colour
            c.drawCircle(s.x0, s.y0, s.width / 2f, dot)
        } else {
            line.color = s.colour
            line.strokeWidth = s.width
            c.drawPath(s.path, line)
        }
    }

    override fun onDraw(canvas: Canvas) {
        val b = bitmap
        if (b == null) canvas.drawColor(Color.WHITE) else canvas.drawBitmap(b, 0f, 0f, null)
        for (s in active.values) drawStroke(canvas, s)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = e.actionIndex
                val width = if (eraser) widthPx * 1.5f else widthPx
                val s = Stroke(if (eraser) Color.WHITE else colour, width, e.getX(i), e.getY(i))
                active[e.getPointerId(i)] = s
                ops.add(s)
                changes++
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until e.pointerCount) {
                    val s = active[e.getPointerId(i)] ?: continue
                    for (h in 0 until e.historySize) addPoint(s, e.getHistoricalX(i, h), e.getHistoricalY(i, h))
                    addPoint(s, e.getX(i), e.getY(i))
                }
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> finish(e.getPointerId(e.actionIndex))
            MotionEvent.ACTION_CANCEL -> active.keys.toList().forEach { finish(it) }
        }
        return true
    }

    private fun addPoint(s: Stroke, x: Float, y: Float) {
        if (abs(x - s.lastX) < 2f && abs(y - s.lastY) < 2f) return
        // Curves through the midpoints, so lines come out smooth
        s.path.quadTo(s.lastX, s.lastY, (x + s.lastX) / 2f, (y + s.lastY) / 2f)
        s.lastX = x
        s.lastY = y
        s.moved = true
    }

    private fun finish(id: Int) {
        val s = active.remove(id) ?: return
        if (s.moved) s.path.lineTo(s.lastX, s.lastY)
        paper?.let { drawStroke(it, s) }
        invalidate()
    }

    /** True when there's nothing on the paper. */
    fun isBlank(): Boolean = (lastClear() + 1 until ops.size).none { ops[it] is Stroke }

    fun canUndo() = ops.isNotEmpty()

    /** Takes away the last stroke, or brings back a cleared drawing. */
    fun undo() {
        val i = ops.indexOfLast { it !is Stroke || it !in active.values }
        if (i < 0) return
        ops.removeAt(i)
        changes++
        redraw()
    }

    fun clear() {
        if (isBlank()) return
        // Fingers still on the paper stop drawing
        active.clear()
        ops.add(ClearMark)
        changes++
        redraw()
    }

    /** A copy of the drawing as it is now, on white. */
    fun snapshot(): Bitmap? = bitmap?.copy(Bitmap.Config.ARGB_8888, false)
}
