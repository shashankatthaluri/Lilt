package com.lilt.mobile.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lilt.mobile.data.DiagnosticLogger

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            DiagnosticLogger.log("BootReceiver", "Device rebooted / app replaced. Rescheduling alarms...")
            ReminderScheduler.rescheduleAll(context)
        }
    }
}
