package com.toddlerdoodle

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The saved doodles. They are kept inside the app's own storage, so the
 * gallery only ever shows doodles (never the phone's photos), and other apps
 * can't see them unless a parent copies them to Photos.
 */
object Doodles {

    private fun dir(ctx: Context): File = File(ctx.filesDir, "doodles").apply { mkdirs() }

    /** Newest first. */
    fun list(ctx: Context): List<File> =
        (dir(ctx).listFiles { f -> f.name.endsWith(".png") } ?: emptyArray()).sortedByDescending { it.name }

    /** Blocking, so call it off the main thread. */
    fun save(ctx: Context, bitmap: Bitmap): File {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.UK).format(Date())
        val file = File(dir(ctx), "doodle_$stamp.png")
        val temp = File(dir(ctx), "saving.tmp")
        temp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        temp.renameTo(file)
        return file
    }

    /** "21/11/2026 18:52" from the file name. */
    fun dateOf(file: File): String =
        try {
            val d = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.UK).parse(file.name.removePrefix("doodle_").take(15))
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.UK).format(d!!)
        } catch (e: Exception) {
            ""
        }

    /** A smaller copy, roughly [maxPx] on its longest side. Blocking. */
    fun load(file: File, maxPx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) sample *= 2
        return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    /** True if Android needs the storage permission first (Android 9 and older). */
    fun copyNeedsPermission() = Build.VERSION.SDK_INT < 29

    /** Copies a doodle into the phone's Pictures/Toddler Doodle folder, so it shows in Photos. Blocking. */
    fun copyToPhotos(ctx: Context, file: File): Boolean =
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                val cr = ctx.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Toddler Doodle")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri == null) false
                else {
                    cr.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    cr.update(uri, values, null, null)
                    true
                }
            } else {
                @Suppress("DEPRECATION")
                val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Toddler Doodle")
                folder.mkdirs()
                val dest = File(folder, file.name)
                file.copyTo(dest, overwrite = true)
                MediaScannerConnection.scanFile(ctx, arrayOf(dest.path), arrayOf("image/png"), null)
                true
            }
        } catch (e: Exception) {
            false
        }
}
