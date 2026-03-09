package com.attentiondiscapture.ui

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.attentiondiscapture.R
import com.attentiondiscapture.databinding.ActivityMainBinding
import com.attentiondiscapture.service.ScreenshotService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val vm: MainViewModel by viewModels()
    private lateinit var adapter: AppListAdapter
    private val handler = Handler(Looper.getMainLooper())

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            startForegroundService(Intent(this, ScreenshotService::class.java).apply {
                action = ScreenshotService.ACTION_START
                putExtra(ScreenshotService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenshotService.EXTRA_RESULT_DATA, result.data)
            })
            // Service needs a moment to start before we can check isRunning
            handler.postDelayed({ updatePermissionBanners() }, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        adapter = AppListAdapter(
            onToggle = vm::toggleEnabled,
            onDelayChange = vm::setDelay,
            getIcon = vm::getAppIcon
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // Filter chip group — singleSelection enforced by ChipGroup
        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            vm.setFilter(when (checkedIds.firstOrNull()) {
                R.id.chipMonitored   -> MainViewModel.Filter.MONITORED
                R.id.chipUnmonitored -> MainViewModel.Filter.UNMONITORED
                else                 -> MainViewModel.Filter.ALL
            })
        }

        // Search
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { vm.setSearch(s?.toString() ?: "") }
        })

        binding.btnGrantAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        binding.btnGrantProjection.setOnClickListener { requestMediaProjection() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.apps.collect { adapter.submitList(it) } }
                launch {
                    vm.enabledCount.collect { count ->
                        val plural = if (count != 1) "s" else ""
                        supportActionBar?.subtitle =
                            if (count > 0) "Monitoring $count app$plural" else "No apps selected"
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionBanners()
    }

    private fun updatePermissionBanners() {
        val accessibilityOk = isAccessibilityEnabled()
        // Check both the static field AND whether the service is actually running
        val projectionOk = ScreenshotService.isRunning

        binding.permissionBanner.isVisible      = !accessibilityOk || !projectionOk
        binding.btnGrantAccessibility.isVisible = !accessibilityOk
        binding.btnGrantProjection.isVisible    = accessibilityOk && !projectionOk
        binding.tvPermissionStatus.text = when {
            !accessibilityOk -> "⚠ Accessibility permission needed"
            !projectionOk    -> "⚠ Screen capture permission needed"
            else             -> "✓ All permissions granted"
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val name = "$packageName/${com.attentiondiscapture.service.AppMonitorService::class.java.canonicalName}"
        return try {
            Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                ?.contains(name) == true
        } catch (e: Exception) { false }
    }

    private fun requestMediaProjection() {
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(mpm.createScreenCaptureIntent())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_gallery  -> { startActivity(Intent(this, GalleryActivity::class.java)); true }
        R.id.action_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
        else -> super.onOptionsItemSelected(item)
    }
}
