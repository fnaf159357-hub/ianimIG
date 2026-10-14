package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.MelodyReminderApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d("BootReceiver", "Boot completed received. Rescheduling reminders...")
            val app = context.applicationContext as? MelodyReminderApp ?: return
            val repository = app.repository

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    repository.rescheduleAllEnabledReminders()
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to reschedule reminders on boot: ${e.message}")
                }
            }
        }
    }
}
