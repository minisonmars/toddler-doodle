package com.toddlerdoodle

import android.app.Activity
import android.app.AlertDialog
import android.os.SystemClock
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout

/** Asks for the parent PIN, and lets parents set a new one. */
object Gate {

    private var wrongCount = 0
    private var lockedUntil = 0L

    fun ask(activity: Activity, settings: Settings, onOk: () -> Unit) {
        val wait = (lockedUntil - SystemClock.elapsedRealtime()) / 1000
        if (wait > 0) {
            Ui.toast(activity, "Too many wrong tries. Wait $wait seconds.")
            return
        }
        askPin(activity, settings, onOk)
    }

    private fun wrong(activity: Activity) {
        wrongCount++
        if (wrongCount >= 5) {
            wrongCount = 0
            lockedUntil = SystemClock.elapsedRealtime() + 30_000
            Ui.toast(activity, "Too many wrong tries. Wait 30 seconds.")
        } else {
            Ui.toast(activity, "That's not right")
        }
    }

    private fun right(onOk: () -> Unit) {
        wrongCount = 0
        onOk()
    }

    private fun askPin(activity: Activity, settings: Settings, onOk: () -> Unit) {
        val pin = pinField(activity, "PIN")
        val dialog = AlertDialog.Builder(activity)
            .setTitle("Parent PIN")
            .setView(boxed(activity, pin))
            .setPositiveButton("OK") { _, _ ->
                if (settings.checkPin(pin.text.toString())) right(onOk) else wrong(activity)
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Forgot?") { _, _ -> forgot(activity, settings, onOk) }
            .create()
        dialog.setOnShowListener { pin.requestFocus() }
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.show()
    }

    private fun forgot(activity: Activity, settings: Settings, onOk: () -> Unit) {
        if (!settings.hasRecoveryWord()) {
            AlertDialog.Builder(activity)
                .setTitle("No recovery word set")
                .setMessage(
                    "Without a recovery word the only way back in is to clear this app's storage: " +
                        "Android Settings > Apps > Toddler Doodle > Storage > Clear data. " +
                        "This also deletes the saved doodles."
                )
                .setPositiveButton("OK", null)
                .show()
            return
        }
        val word = EditText(activity).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "Recovery word"
        }
        AlertDialog.Builder(activity)
            .setTitle("Reset PIN")
            .setMessage("Enter your recovery word. The PIN goes back to ${Settings.DEFAULT_PIN}.")
            .setView(boxed(activity, word))
            .setPositiveButton("Reset") { _, _ ->
                if (settings.checkRecoveryWord(word.text.toString())) {
                    settings.resetPin()
                    Ui.toast(activity, "PIN reset to ${Settings.DEFAULT_PIN}. Please set a new one.")
                    right(onOk)
                } else {
                    wrong(activity)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ---------- Setting a new PIN or recovery word ----------

    fun changePin(activity: Activity, settings: Settings, onDone: () -> Unit) {
        val newPin = pinField(activity, "New PIN (4 to 8 digits)")
        val again = pinField(activity, "New PIN again")
        val box = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(newPin)
            addView(again)
        }
        AlertDialog.Builder(activity)
            .setTitle("Change PIN")
            .setView(boxed(activity, box))
            .setPositiveButton("Save") { _, _ ->
                val pin = newPin.text.toString()
                when {
                    pin.length !in 4..8 -> Ui.toast(activity, "The PIN needs 4 to 8 digits")
                    pin != again.text.toString() -> Ui.toast(activity, "The two PINs don't match")
                    else -> {
                        settings.setPin(pin)
                        Ui.toast(activity, "PIN changed")
                        onDone()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun changeRecoveryWord(activity: Activity, settings: Settings, onDone: () -> Unit) {
        val word = EditText(activity).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "Recovery word"
        }
        AlertDialog.Builder(activity)
            .setTitle("Recovery word")
            .setMessage("If you forget the PIN, this word resets it. Capitals don't matter.")
            .setView(boxed(activity, word))
            .setPositiveButton("Save") { _, _ ->
                if (word.text.toString().isBlank()) {
                    Ui.toast(activity, "Please enter a word")
                } else {
                    settings.setRecoveryWord(word.text.toString())
                    Ui.toast(activity, "Recovery word saved")
                    onDone()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun pinField(activity: Activity, hintText: String) = EditText(activity).apply {
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        hint = hintText
        textSize = 22f
    }

    private fun boxed(activity: Activity, view: View): FrameLayout =
        FrameLayout(activity).apply {
            val pad = Ui.dp(activity, 24)
            setPadding(pad, Ui.dp(activity, 8), pad, 0)
            addView(view)
        }
}
