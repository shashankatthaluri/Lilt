package com.lilt.mobile.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.lilt.mobile.data.DiagnosticLogger
import com.lilt.mobile.data.ReminderItem
import com.lilt.mobile.data.SettingsManager

object ReminderScheduler {
    private const val TAG = "ReminderScheduler"

    fun schedule(context: Context, reminder: ReminderItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE_REMINDER
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderReceiver.EXTRA_TITLE, reminder.title)
            putExtra(ReminderReceiver.EXTRA_EVENT_TIME, reminder.eventEpochMs)
            putExtra(ReminderReceiver.EXTRA_LEAD_MINUTES, reminder.leadMinutes)
        }

        val requestCode = reminder.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.triggerEpochMs,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reminder.triggerEpochMs,
                    pendingIntent
                )
            }
            DiagnosticLogger.log(TAG, "Scheduled reminder '${reminder.title}' for epoch ${reminder.triggerEpochMs}")
        } catch (e: SecurityException) {
            // In Android 12+ (API 31+), if exact alarm permission is missing, fall back to normal setAndAllowWhileIdle
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminder.triggerEpochMs,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        reminder.triggerEpochMs,
                        pendingIntent
                    )
                }
                DiagnosticLogger.log(TAG, "Fallback scheduled reminder '${reminder.title}'")
            } catch (e2: Exception) {
                DiagnosticLogger.logError(TAG, "Failed to schedule alarm: ${e2.message}", e2)
            }
        } catch (e: Exception) {
            DiagnosticLogger.logError(TAG, "Error scheduling alarm: ${e.message}", e)
        }
    }

    fun cancel(context: Context, reminderId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun rescheduleAll(context: Context) {
        val settings = SettingsManager.getInstance(context)
        if (!settings.smartRemindersEnabled) return
        val now = System.currentTimeMillis()
        val pending = settings.getReminders().filter { !it.isCompleted && !it.isDismissed && it.eventEpochMs > now }
        for (reminder in pending) {
            schedule(context, reminder)
        }
    }
}
