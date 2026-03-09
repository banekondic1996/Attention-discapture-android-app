package com.attentiondiscapture.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ─── Entity ───────────────────────────────────────────────────────────────────

@Entity(tableName = "monitored_apps")
data class MonitoredApp(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isEnabled: Boolean = false,
    val delaySeconds: Float = 0.2f   // default 1 second
)

// ─── DAO ──────────────────────────────────────────────────────────────────────

@Dao
interface MonitoredAppDao {

    @Query("SELECT * FROM monitored_apps ORDER BY appName ASC")
    fun getAllApps(): Flow<List<MonitoredApp>>

    @Query("SELECT * FROM monitored_apps WHERE isEnabled = 1")
    suspend fun getEnabledApps(): List<MonitoredApp>

    @Query("SELECT * FROM monitored_apps WHERE isEnabled = 1")
    fun getEnabledAppsFlow(): Flow<List<MonitoredApp>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(app: MonitoredApp)

    @Upsert
    suspend fun upsert(app: MonitoredApp)

    @Query("UPDATE monitored_apps SET isEnabled = :enabled WHERE packageName = :packageName")
    suspend fun setEnabled(packageName: String, enabled: Boolean)

    @Query("UPDATE monitored_apps SET delaySeconds = :delay WHERE packageName = :packageName")
    suspend fun setDelay(packageName: String, delay: Float)

    @Query("SELECT COUNT(*) FROM monitored_apps")
    suspend fun count(): Int
}

// ─── Database ─────────────────────────────────────────────────────────────────

@Database(entities = [MonitoredApp::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun monitoredAppDao(): MonitoredAppDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: android.content.Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "attention_discapture.db"
                ).build().also { INSTANCE = it }
            }
    }
}
