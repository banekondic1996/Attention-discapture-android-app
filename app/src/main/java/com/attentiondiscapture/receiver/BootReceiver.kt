package com.attentiondiscapture.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * On boot: The accessibility service restarts automatically by Android.
 * We just log here; the service re-subscribes to the DB on connect.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed — accessibility service will auto-restart if enabled")
        }
    }
}
