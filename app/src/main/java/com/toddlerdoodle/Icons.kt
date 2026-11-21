package com.toddlerdoodle

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

enum class Icon { NONE, GEAR, ARROW_LEFT, ARROW_RIGHT, CLOSE, CHECK, UNDO, BIN, SAVE, GALLERY, ERASER, DOT, HOME }

/**
 * A round button with an icon drawn in code, so every icon has the same size,
 * weight and alignment on every phone (emoji and symbol characters don't).
 * When [chosen] is set it gets a dark ring, to show the colour or tool in use.
 */
open class RoundIconButton(context: Context, icon: Icon, fill: Int) : View(context) {

    var icon: Icon = icon
        set(value) {
            field = value
            invalidate()
        }
    var fill: Int = fill
        set(value) {
            field = value
            invalidate()
        }
    var chosen = false
        set(value) {
            field = value
            invalidate()
        }

    /** For [Icon.DOT]: the dot's colour and its size (0 to 0.5 of the button). */
    var dotColour = Color.BLACK
        set(value) {
            field = value
            invalidate()
        }
    var dotSize = 0.15f

    protected val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()
    private val rect = RectF()

    init {
        isFocusable = true
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val s = min(width, height).toFloat()
        val c = s / 2f
        canvas.save()
        canvas.translate((width - s) / 2f, (height - s) / 2f)
        if (isPressed) canvas.scale(0.92f, 0.92f, c, c)

        paint.style = Paint.Style.FILL
        if (chosen) {
            paint.color = Ui.DARK
            canvas.drawCircle(c, c, c, paint)
            paint.color = Color.WHITE
            canvas.drawCircle(c, c, c * 0.86f, paint)
            paint.color = fill
            canvas.drawCircle(c, c, c * 0.78f, paint)
        } else {
            paint.color = fill
            canvas.drawCircle(c, c, c * 0.9f, paint)
        }

        canvas.save()
        canvas.scale(s, s)
        drawIcon(canvas)
        canvas.restore()
        drawExtra(canvas, s)
        canvas.restore()
    }

    /** For subclasses, drawn on top in button pixels (0 to [s]). */
    protected open fun drawExtra(canvas: Canvas, s: Float) {}

    private fun fillStyle() {
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
    }

    private fun strokeStyle(width: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = width
        paint.color = Color.WHITE
    }

    private fun drawIcon(canvas: Canvas) {
        when (icon) {
            Icon.NONE -> {}

            Icon.GEAR -> {
                fillStyle()
                for (i in 0 until 8) {
                    canvas.save()
                    canvas.rotate(i * 45f, 0.5f, 0.5f)
                    rect.set(0.44f, 0.16f, 0.56f, 0.31f)
                    canvas.drawRoundRect(rect, 0.02f, 0.02f, paint)
                    canvas.restore()
                }
                strokeStyle(0.09f)
                canvas.drawCircle(0.5f, 0.5f, 0.19f, paint)
            }

            Icon.ARROW_LEFT, Icon.ARROW_RIGHT -> {
                if (icon == Icon.ARROW_RIGHT) canvas.scale(-1f, 1f, 0.5f, 0.5f)
                strokeStyle(0.10f)
                path.reset()
                path.moveTo(0.76f, 0.50f)
                path.lineTo(0.26f, 0.50f)
                path.moveTo(0.48f, 0.28f)
                path.lineTo(0.26f, 0.50f)
                path.lineTo(0.48f, 0.72f)
                canvas.drawPath(path, paint)
            }

            Icon.CLOSE -> {
                strokeStyle(0.10f)
                canvas.drawLine(0.32f, 0.32f, 0.68f, 0.68f, paint)
                canvas.drawLine(0.68f, 0.32f, 0.32f, 0.68f, paint)
            }

            Icon.CHECK -> {
                strokeStyle(0.10f)
                path.reset()
                path.moveTo(0.27f, 0.52f)
                path.lineTo(0.43f, 0.68f)
                path.lineTo(0.74f, 0.35f)
                canvas.drawPath(path, paint)
            }

            Icon.UNDO -> {
                // An arrow curling back to the left
                strokeStyle(0.09f)
                rect.set(0.30f, 0.30f, 0.72f, 0.72f)
                path.reset()
                path.addArc(rect, 180f, 230f)
                canvas.drawPath(path, paint)
                fillStyle()
                path.reset()
                path.moveTo(0.30f, 0.66f)
                path.lineTo(0.17f, 0.46f)
                path.lineTo(0.43f, 0.46f)
                path.close()
                canvas.drawPath(path, paint)
            }

            Icon.BIN -> {
                strokeStyle(0.07f)
                canvas.drawLine(0.27f, 0.31f, 0.73f, 0.31f, paint)
                path.reset()
                path.moveTo(0.43f, 0.31f)
                path.lineTo(0.43f, 0.24f)
                path.lineTo(0.57f, 0.24f)
                path.lineTo(0.57f, 0.31f)
                canvas.drawPath(path, paint)
                path.reset()
                path.moveTo(0.33f, 0.38f)
                path.lineTo(0.37f, 0.77f)
                path.lineTo(0.63f, 0.77f)
                path.lineTo(0.67f, 0.38f)
                canvas.drawPath(path, paint)
                paint.strokeWidth = 0.05f
                canvas.drawLine(0.45f, 0.46f, 0.46f, 0.68f, paint)
                canvas.drawLine(0.55f, 0.46f, 0.54f, 0.68f, paint)
            }

            Icon.SAVE -> {
                // An arrow going down into a tray
                strokeStyle(0.09f)
                path.reset()
                path.moveTo(0.5f, 0.22f)
                path.lineTo(0.5f, 0.58f)
                path.moveTo(0.34f, 0.44f)
                path.lineTo(0.5f, 0.60f)
                path.lineTo(0.66f, 0.44f)
                canvas.drawPath(path, paint)
                path.reset()
                path.moveTo(0.27f, 0.62f)
                path.lineTo(0.27f, 0.75f)
                path.lineTo(0.73f, 0.75f)
                path.lineTo(0.73f, 0.62f)
                canvas.drawPath(path, paint)
            }

            Icon.GALLERY -> {
                // A picture with a hill and a sun
                strokeStyle(0.06f)
                rect.set(0.24f, 0.28f, 0.76f, 0.72f)
                canvas.drawRoundRect(rect, 0.05f, 0.05f, paint)
                fillStyle()
                path.reset()
                path.moveTo(0.27f, 0.70f)
                path.lineTo(0.43f, 0.48f)
                path.lineTo(0.54f, 0.61f)
                path.lineTo(0.62f, 0.52f)
                path.lineTo(0.73f, 0.70f)
                path.close()
                canvas.drawPath(path, paint)
                canvas.drawCircle(0.62f, 0.40f, 0.055f, paint)
            }

            Icon.ERASER -> {
                canvas.save()
                canvas.rotate(-40f, 0.5f, 0.5f)
                strokeStyle(0.06f)
                rect.set(0.24f, 0.39f, 0.76f, 0.61f)
                canvas.drawRoundRect(rect, 0.05f, 0.05f, paint)
                fillStyle()
                rect.set(0.24f, 0.39f, 0.43f, 0.61f)
                canvas.drawRoundRect(rect, 0.05f, 0.05f, paint)
                canvas.restore()
            }

            Icon.HOME -> {
                // A house: roof and walls, with a door
                strokeStyle(0.07f)
                path.reset()
                path.moveTo(0.22f, 0.50f)
                path.lineTo(0.50f, 0.25f)
                path.lineTo(0.78f, 0.50f)
                canvas.drawPath(path, paint)
                path.reset()
                path.moveTo(0.31f, 0.44f)
                path.lineTo(0.31f, 0.75f)
                path.lineTo(0.69f, 0.75f)
                path.lineTo(0.69f, 0.44f)
                canvas.drawPath(path, paint)
                fillStyle()
                rect.set(0.44f, 0.56f, 0.56f, 0.75f)
                canvas.drawRect(rect, paint)
            }

            Icon.DOT -> {
                paint.style = Paint.Style.FILL
                paint.color = dotColour
                canvas.drawCircle(0.5f, 0.5f, dotSize, paint)
                // A thin outline so light colours still show on the white button
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 0.02f
                paint.color = Color.argb(90, 0, 0, 0)
                canvas.drawCircle(0.5f, 0.5f, dotSize, paint)
            }
        }
    }
}

/**
 * A round button that only works when held down for [holdMs]. A ring fills up
 * while it's held, so a quick tap from a toddler does nothing.
 */
@SuppressLint("ClickableViewAccessibility")
class HoldButton(context: Context, icon: Icon, fill: Int, private val holdMs: Long, private val onHold: () -> Unit) :
    RoundIconButton(context, icon, fill) {

    /** Called when the button is let go too early. */
    var onTooShort: (() -> Unit)? = null

    private var downAt = 0L
    private var holding = false
    private val arc = RectF()

    private val tick = object : Runnable {
        override fun run() {
            if (!holding) return
            if (SystemClock.uptimeMillis() - downAt >= holdMs) {
                holding = false
                isPressed = false
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                invalidate()
                onHold()
            } else {
                invalidate()
                postOnAnimation(this)
            }
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downAt = SystemClock.uptimeMillis()
                holding = true
                isPressed = true
                postOnAnimation(tick)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val early = holding
                holding = false
                isPressed = false
                invalidate()
                if (early && e.actionMasked == MotionEvent.ACTION_UP) onTooShort?.invoke()
            }
        }
        return true
    }

    override fun drawExtra(canvas: Canvas, s: Float) {
        if (!holding) return
        val f = ((SystemClock.uptimeMillis() - downAt).toFloat() / holdMs).coerceIn(0f, 1f)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * 0.09f
        paint.color = Ui.DARK
        val r = s * 0.455f
        arc.set(s / 2 - r, s / 2 - r, s / 2 + r, s / 2 + r)
        canvas.drawArc(arc, -90f, 360f * f, false, paint)
    }
}
