package com.attentiondiscapture.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).monitoredAppDao()

    val allApps: Flow<List<MonitoredApp>> = dao.getAllApps()
    val enabledApps: Flow<List<MonitoredApp>> = dao.getEnabledAppsFlow()

    suspend fun syncInstalledApps() = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val installed = pm
            .queryIntentActivities(launcherIntent, PackageManager.GET_META_DATA)
            .map { it.activityInfo.packageName }
            .toSet()
            .mapNotNull { pkg ->
                try {
                    val info = pm.getApplicationInfo(pkg, 0)
                    MonitoredApp(
                        packageName = pkg,
                        appName = pm.getApplicationLabel(info).toString(),
                        isEnabled = false,
                        delaySeconds = 0.2f
                    )
                } catch (e: Exception) { null }
            }
            .sortedBy { it.appName }

        // INSERT OR IGNORE — preserves existing enabled/delay settings
        installed.forEach { dao.insertIfNotExists(it) }
    }

    suspend fun setEnabled(packageName: String, enabled: Boolean) =
        dao.setEnabled(packageName, enabled)

    suspend fun setDelay(packageName: String, delay: Float) =
        dao.setDelay(packageName, delay)

    suspend fun getEnabledApps(): List<MonitoredApp> = dao.getEnabledApps()
}
