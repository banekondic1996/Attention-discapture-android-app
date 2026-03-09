package com.attentiondiscapture.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.attentiondiscapture.databinding.ActivitySettingsBinding
import com.attentiondiscapture.util.Prefs
import com.attentiondiscapture.util.ScreenshotManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        val current = Prefs.getMaxScreenshots(this)
        binding.etMaxScreenshots.setText(current.toString())

        binding.etMaxScreenshots.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val v = s?.toString()?.toIntOrNull() ?: return
                if (v in 1..500) {
                    Prefs.setMaxScreenshots(applicationContext, v)
                }
            }
        })

        updateStorageInfo()
    }

    private fun updateStorageInfo() {
        val count = ScreenshotManager.getTotalCount(this)
        binding.tvStorageInfo.text = "$count screenshot${if (count != 1) "s" else ""} stored across all apps"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
