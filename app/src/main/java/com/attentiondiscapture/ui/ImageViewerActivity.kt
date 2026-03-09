package com.attentiondiscapture.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import androidx.recyclerview.widget.RecyclerView
import com.attentiondiscapture.databinding.ActivityImageViewerBinding
import com.attentiondiscapture.util.ScreenshotManager
import com.bumptech.glide.Glide
import java.io.File

class ImageViewerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_INDEX    = "extra_index"
    }

    private lateinit var binding: ActivityImageViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val appName  = intent.getStringExtra(EXTRA_APP_NAME) ?: run { finish(); return }
        val startIdx = intent.getIntExtra(EXTRA_INDEX, 0)

        val files = ScreenshotManager.getScreenshotsFor(this, appName)
        if (files.isEmpty()) { finish(); return }

        supportActionBar?.hide()

        // Make status/nav bar transparent
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        binding.tvAppName.text = appName
        binding.btnClose.setOnClickListener { finish() }

        val adapter = PagerAdapter(files)
        binding.viewPager.adapter = adapter
        binding.viewPager.setCurrentItem(startIdx, false)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.tvCounter.text = "${position + 1} / ${files.size}"
            }
        })
        binding.tvCounter.text = "${startIdx + 1} / ${files.size}"
    }

    inner class PagerAdapter(private val files: List<File>) :
        RecyclerView.Adapter<PagerAdapter.VH>() {

        inner class VH(val image: ImageView) : RecyclerView.ViewHolder(image)

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val iv = ImageView(this@ImageViewerActivity).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.FIT_CENTER
                setBackgroundColor(android.graphics.Color.BLACK)
            }
            return VH(iv)
        }

        override fun getItemCount() = files.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            Glide.with(this@ImageViewerActivity)
                .load(files[position])
                .into(holder.image)
        }
    }
}
