package com.toddlerdoodle

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom

/** The parent options and the last colour and brush used, saved on the device and applied straight away. */
class Settings(ctx: Context) {

    private val p = ctx.applicationContext.getSharedPreferences("doodle", Context.MODE_PRIVATE)

    private fun getB(k: String, d: Boolean) = p.getBoolean(k, d)
    private fun putB(k: String, v: Boolean) = p.edit().putBoolean(k, v).apply()
    private fun getI(k: String, d: Int) = p.getInt(k, d)
    private fun putI(k: String, v: Int) = p.edit().putInt(k, v).apply()

    // ---------- Which buttons the toddler sees ----------
    var showSave: Boolean
        get() = getB("showSave", true)
        set(v) = putB("showSave", v)
    var showEraser: Boolean
        get() = getB("showEraser", true)
        set(v) = putB("showEraser", v)
    var showGallery: Boolean
        get() = getB("showGallery", true)
        set(v) = putB("showGallery", v)

    // ---------- Remembered between uses ----------
    var colour: Int // index into Ui.CRAYONS
        get() = getI("colour", 0).coerceIn(0, Ui.CRAYONS.size - 1)
        set(v) = putI("colour", v)

    // ---------- Parent PIN (stored as a salted hash, never the PIN itself) ----------
    fun checkPin(pin: String): Boolean {
        val salt = p.getString("pinSalt", null)
        val hash = p.getString("pinHash", null)
        if (salt == null || hash == null) return pin == DEFAULT_PIN
        return sha(salt + pin) == hash
    }

    fun isDefaultPin() = p.getString("pinHash", null) == null

    fun setPin(pin: String) {
        val salt = randomSalt()
        p.edit().putString("pinSalt", salt).putString("pinHash", sha(salt + pin)).apply()
    }

    fun hasRecoveryWord(): Boolean = p.getString("recHash", null) != null

    fun setRecoveryWord(word: String) {
        val salt = randomSalt()
        p.edit().putString("recSalt", salt).putString("recHash", sha(salt + normalise(word))).apply()
    }

    fun checkRecoveryWord(word: String): Boolean {
        val salt = p.getString("recSalt", null) ?: return false
        val hash = p.getString("recHash", null) ?: return false
        return sha(salt + normalise(word)) == hash
    }

    /** Back to PIN 0000 (the recovery word is kept). */
    fun resetPin() {
        p.edit().remove("pinSalt").remove("pinHash").apply()
    }

    private fun normalise(s: String) = s.trim().lowercase()

    private fun randomSalt(): String {
        val b = ByteArray(8)
        SecureRandom().nextBytes(b)
        return b.joinToString("") { "%02x".format(it) }
    }

    private fun sha(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    companion object {
        const val DEFAULT_PIN = "0000"
    }
}
