package com.attentiondiscapture.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.attentiondiscapture.data.AppRepository
import com.attentiondiscapture.data.MonitoredApp
import kotlinx.coroutines.*

/**
 * Accessibility service that watches for app-switch events.
 * When a monitored app comes to the foreground it schedules a screenshot
 * via [ScreenshotService].
 */
class AppMonitorService : AccessibilityService() {

    companion object {
        const val TAG = "AppMonitorService"
        var instance: AppMonitorService? = null
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var enabledApps: Map<String, MonitoredApp> = emptyMap()
    private var lastPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Service connected")
        observeEnabledApps()
    }

    private fun observeEnabledApps() {
        scope.launch {
            val repo = AppRepository(applicationContext)
            repo.enabledApps.collect { list ->
                enabledApps = list.associateBy { it.packageName }
                Log.d(TAG, "Watching ${enabledApps.size} apps")
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == lastPackage) return  // same app, ignore
        lastPackage = pkg

        val monitored = enabledApps[pkg] ?: return
        Log.d(TAG, "Detected launch of ${monitored.appName}, delay=${monitored.delaySeconds}s")

        val delayMs = (monitored.delaySeconds * 1000).toLong()
        scope.launch {
            delay(delayMs)
            triggerScreenshot(monitored)
        }
    }

    private fun triggerScreenshot(app: MonitoredApp) {
        val intent = Intent(applicationContext, ScreenshotService::class.java).apply {
            action = ScreenshotService.ACTION_CAPTURE
            putExtra(ScreenshotService.EXTRA_APP_NAME, app.appName)
            putExtra(ScreenshotService.EXTRA_PACKAGE_NAME, app.packageName)
        }
        applicationContext.startService(intent)
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        scope.cancel()
    }
}
