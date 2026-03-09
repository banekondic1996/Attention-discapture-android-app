package com.attentiondiscapture.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.attentiondiscapture.R
import com.attentiondiscapture.ui.MainActivity
import com.attentiondiscapture.util.ScreenshotManager
import kotlinx.coroutines.*

/**
 * Foreground service that holds the MediaProjection token.
 *
 * To minimise the status-bar casting indicator we:
 *  1. Acquire the projection only while a capture is in flight.
 *  2. Stop (release) it immediately after saving the file.
 *  3. Re-create it from the stored resultCode/resultData on the next capture.
 *
 * Android OS still shows the indicator for the brief moment the projection
 * is active (~300 ms per capture). There is no API to suppress it entirely
 * on Android 10–14; this is the least-intrusive approach possible without
 * root or a system app signature.
 */
class ScreenshotService : Service() {

    companion object {
        const val TAG = "ScreenshotService"
        const val ACTION_START   = "ACTION_START"
        const val ACTION_STOP    = "ACTION_STOP"
        const val ACTION_CAPTURE = "ACTION_CAPTURE"
        const val EXTRA_RESULT_CODE  = "EXTRA_RESULT_CODE"
        const val EXTRA_RESULT_DATA  = "EXTRA_RESULT_DATA"
        const val EXTRA_APP_NAME     = "EXTRA_APP_NAME"
        const val EXTRA_PACKAGE_NAME = "EXTRA_PACKAGE_NAME"
        const val CHANNEL_ID = "screenshot_service_channel"
        const val NOTIF_ID   = 1001

        // Visible to AppMonitorService
        var isRunning = false

        // Kept so we can recreate the projection on each capture
        private var savedResultCode: Int = -1
        private var savedResultData: Intent? = null

        // Held only during capture, null otherwise → minimises indicator time
        var mediaProjection: MediaProjection? = null
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {

            ACTION_START -> {
                // Must startForeground BEFORE getMediaProjection
                startForeground(NOTIF_ID, buildNotification())
                isRunning = true

                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
                @Suppress("DEPRECATION")
                val resultData: Intent? = intent.getParcelableExtra(EXTRA_RESULT_DATA)
                if (resultData != null) {
                    savedResultCode = resultCode
                    savedResultData = resultData
                    Log.d(TAG, "Projection credentials stored")
                }
            }

            ACTION_STOP -> {
                releaseProjection()
                savedResultData = null
                isRunning = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }

            ACTION_CAPTURE -> {
                val appName     = intent.getStringExtra(EXTRA_APP_NAME) ?: return START_STICKY
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return START_STICKY
                val resultData  = savedResultData ?: run {
                    Log.w(TAG, "No stored projection data, skipping $appName")
                    return START_STICKY
                }
                scope.launch {
                    try {
                        // Acquire projection just for this capture
                        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        val proj = mpm.getMediaProjection(savedResultCode, resultData)
                        mediaProjection = proj
                        ScreenshotManager.capture(applicationContext, proj, appName, packageName)
                    } catch (e: Exception) {
                        Log.e(TAG, "Capture error for $appName", e)
                    } finally {
                        // Release immediately — hides casting icon as soon as possible
                        releaseProjection()
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun releaseProjection() {
        try { mediaProjection?.stop() } catch (_: Exception) {}
        mediaProjection = null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.screenshot_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.screenshot_channel_desc)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, ScreenshotService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_camera)
            .setContentIntent(openIntent)
            .addAction(R.drawable.ic_back, "Stop", stopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        isRunning = false
        releaseProjection()
        scope.cancel()
        super.onDestroy()
    }
}
