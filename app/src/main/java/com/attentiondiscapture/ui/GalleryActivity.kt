package com.attentiondiscapture.ui

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.attentiondiscapture.R
import com.attentiondiscapture.databinding.ActivityGalleryBinding
import com.attentiondiscapture.util.ScreenshotManager
import com.bumptech.glide.Glide
import java.io.File

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.gallery_title)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }

    // Refresh every time activity becomes visible — covers: back-nav, recent-apps resume
    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        val folders = ScreenshotManager.getAllAppFolders(this)
        val total   = ScreenshotManager.getTotalCount(this)

        binding.tvTotal.text = if (total > 0) "$total screenshot${if (total != 1) "s" else ""}" else ""
        binding.btnClearAll.isEnabled = total > 0

        if (folders.isEmpty()) {
            binding.emptyText.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.emptyText.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            binding.recyclerView.adapter = FolderAdapter(folders.toMutableList())
        }

        binding.btnClearAll.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear all screenshots?")
                .setMessage("This will permanently delete all $total saved screenshots.")
                .setPositiveButton("Delete all") { _, _ ->
                    ScreenshotManager.clearAll(this)
                    loadData()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onSupportNavigateUp(): Boolean { onBackPressedDispatcher.onBackPressed(); return true }

    inner class FolderAdapter(private val folders: MutableList<File>) :
        RecyclerView.Adapter<FolderAdapter.FolderVH>() {

        inner class FolderVH(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView    = view.findViewById(R.id.folderTitle)
            val count: TextView    = view.findViewById(R.id.folderCount)
            val grid: RecyclerView = view.findViewById(R.id.thumbnailGrid)
            val btnClear: android.widget.ImageButton = view.findViewById(R.id.btnClearFolder)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            FolderVH(LayoutInflater.from(parent.context).inflate(R.layout.item_folder, parent, false))

        override fun getItemCount() = folders.size

        override fun onBindViewHolder(holder: FolderVH, position: Int) {
            val folder = folders[position]
            val files  = folder.listFiles { f -> f.extension == "jpg" }
                ?.sortedByDescending { it.lastModified() } ?: emptyList()
            val appName = folder.name
            val plural = if (files.size != 1) "s" else ""

            holder.title.text = appName
            holder.count.text = "${files.size} screenshot$plural"
            holder.grid.layoutManager = GridLayoutManager(this@GalleryActivity, 3)
            holder.grid.adapter = ThumbnailAdapter(files, appName)

            holder.btnClear.setOnClickListener {
                AlertDialog.Builder(this@GalleryActivity)
                    .setTitle("Clear $appName?")
                    .setMessage("Delete all ${files.size} screenshots for this app?")
                    .setPositiveButton("Delete") { _, _ ->
                        folder.listFiles()?.forEach { it.delete() }
                        val idx = folders.indexOf(folder)
                        if (idx >= 0) { folders.removeAt(idx); notifyItemRemoved(idx) }
                        loadData()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    inner class ThumbnailAdapter(private val files: List<File>, private val appName: String) :
        RecyclerView.Adapter<ThumbnailAdapter.ThumbVH>() {

        inner class ThumbVH(view: View) : RecyclerView.ViewHolder(view) {
            val image: ImageView = view.findViewById(R.id.thumbnail)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ThumbVH(LayoutInflater.from(parent.context).inflate(R.layout.item_thumbnail, parent, false))

        override fun getItemCount() = files.size

        override fun onBindViewHolder(holder: ThumbVH, position: Int) {
            Glide.with(this@GalleryActivity).load(files[position]).centerCrop()
                .placeholder(R.drawable.ic_camera).into(holder.image)

            holder.image.setOnClickListener {
                startActivity(Intent(this@GalleryActivity, ImageViewerActivity::class.java).apply {
                    putExtra(ImageViewerActivity.EXTRA_APP_NAME, appName)
                    putExtra(ImageViewerActivity.EXTRA_INDEX, position)
                })
            }
        }
    }
}
