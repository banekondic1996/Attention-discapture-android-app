package com.attentiondiscapture.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.attentiondiscapture.data.MonitoredApp
import com.attentiondiscapture.databinding.ItemAppBinding

class AppListAdapter(
    private val onToggle: (String, Boolean) -> Unit,
    private val onDelayChange: (String, Float) -> Unit,
    private val getIcon: (String) -> android.graphics.drawable.Drawable?
) : ListAdapter<MonitoredApp, AppListAdapter.ViewHolder>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<MonitoredApp>() {
            override fun areItemsTheSame(a: MonitoredApp, b: MonitoredApp) = a.packageName == b.packageName
            // Only trigger rebind for enabled/name changes — NOT delaySeconds while editing
            override fun areContentsTheSame(a: MonitoredApp, b: MonitoredApp) =
                a.isEnabled == b.isEnabled && a.appName == b.appName && a.packageName == b.packageName
        }
    }

    inner class ViewHolder(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root) {

        private var currentApp: MonitoredApp? = null

        init {
            // Save delay when user leaves the field OR presses Done on keyboard
            binding.delayInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) commitDelay()
            }
            binding.delayInput.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    commitDelay()
                    binding.delayInput.clearFocus()
                }
                false
            }
        }

        private fun commitDelay() {
            val app = currentApp ?: return
            val text = binding.delayInput.text?.toString() ?: return
            val delay = text.toFloatOrNull()
            if (delay != null && delay > 0f) {
                onDelayChange(app.packageName, delay)
            } else {
                // Reset to current valid value if input is empty/invalid
                binding.delayInput.setText(app.delaySeconds.toDisplayString())
            }
        }

        fun bind(app: MonitoredApp) {
            currentApp = app
            binding.appName.text = app.appName
            binding.appIcon.setImageDrawable(getIcon(app.packageName))

            binding.enabledCheckbox.setOnCheckedChangeListener(null)
            binding.enabledCheckbox.isChecked = app.isEnabled
            binding.enabledCheckbox.setOnCheckedChangeListener { _, checked ->
                onToggle(app.packageName, checked)
            }

            // Only set delay text if field doesn't have focus (user might be editing)
            if (!binding.delayInput.hasFocus()) {
                binding.delayInput.setText(app.delaySeconds.toDisplayString())
            }

            binding.root.setBackgroundColor(
                if (app.isEnabled)
                    itemView.context.getColor(com.attentiondiscapture.R.color.checked_bg)
                else android.graphics.Color.TRANSPARENT
            )
        }

        private fun Float.toDisplayString(): String =
            if (this == toLong().toFloat()) toLong().toString() else toString()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))
}
