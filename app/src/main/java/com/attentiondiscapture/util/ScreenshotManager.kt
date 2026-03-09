package com.attentiondiscapture.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ScreenshotManager {

    private const val TAG = "ScreenshotManager"

    fun capture(
        context: Context,
        mediaProjection: MediaProjection,
        appName: String,
        packageName: String
    ) {
        val metrics = context.resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        var virtualDisplay: VirtualDisplay? = null

        try {
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "AttentionDiscaptureDisplay",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface, null, null
            )
            Thread.sleep(200)

            val image: Image? = imageReader.acquireLatestImage()
            if (image != null) {
                val bitmap = imageToBitmap(image)
                image.close()
                if (bitmap != null) {
                    saveScreenshot(context, bitmap, appName)
                    bitmap.recycle()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Capture failed for $appName", e)
        } finally {
            virtualDisplay?.release()
            imageReader.close()
        }
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        return try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width
            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
        } catch (e: Exception) {
            Log.e(TAG, "imageToBitmap failed", e)
            null
        }
    }

    private fun saveScreenshot(context: Context, bitmap: Bitmap, appName: String) {
        val baseDir = getFolderForApp(context, appName)
        if (!baseDir.exists()) baseDir.mkdirs()
        enforceLimit(context, baseDir)

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val file = File(baseDir, "screenshot_$timestamp.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        Log.d(TAG, "Saved: ${file.absolutePath}")
    }

    private fun enforceLimit(context: Context, dir: File) {
        val max = Prefs.getMaxScreenshots(context)
        val files = dir.listFiles { f -> f.extension == "jpg" }
            ?.sortedBy { it.lastModified() }
            ?.toMutableList() ?: return
        while (files.size >= max) {
            files.removeAt(0).delete()
        }
    }

    fun getFolderForApp(context: Context, appName: String): File =
        File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), sanitizeName(appName))

    fun sanitizeName(name: String): String =
        name.replace(Regex("[^a-zA-Z0-9._\\- ]"), "_").trim()

    fun getScreenshotsFor(context: Context, appName: String): List<File> {
        val dir = getFolderForApp(context, appName)
        return dir.listFiles { f -> f.extension == "jpg" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun getAllAppFolders(context: Context): List<File> {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return emptyList()
        return base.listFiles { f -> f.isDirectory }
            ?.filter { it.listFiles { f -> f.extension == "jpg" }?.isNotEmpty() == true }
            ?.toList() ?: emptyList()
    }

    fun clearAll(context: Context) {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return
        base.listFiles()?.forEach { it.deleteRecursively() }
    }

    fun getTotalCount(context: Context): Int {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return 0
        return base.walkBottomUp().count { it.extension == "jpg" }
    }
}
