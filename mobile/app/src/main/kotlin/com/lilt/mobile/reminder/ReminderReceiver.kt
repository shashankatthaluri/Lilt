package com.lilt.mobile.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.lilt.mobile.MainActivity
import com.lilt.mobile.R
import com.lilt.mobile.data.DiagnosticLogger
import com.lilt.mobile.data.SettingsManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_FIRE_REMINDER = "com.lilt.mobile.ACTION_FIRE_REMINDER"
        const val ACTION_MARK_DONE = "com.lilt.mobile.ACTION_MARK_DONE"
        const val ACTION_SNOOZE = "com.lilt.mobile.ACTION_SNOOZE"

        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_EVENT_TIME = "extra_event_time"
        const val EXTRA_LEAD_MINUTES = "extra_lead_minutes"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        const val EXTRA_IS_SILENT = "extra_is_silent"
        const val NOTIFICATION_CHANNEL_ID = "lilt_reminders_alarm_channel"
        const val NOTIFICATION_SILENT_CHANNEL_ID = "lilt_reminders_silent_alarm_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val settings = SettingsManager.getInstance(context)

        when (action) {
            ACTION_FIRE_REMINDER -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Scheduled Task"
                val eventTime = intent.getLongExtra(EXTRA_EVENT_TIME, 0L)
                val leadMinutes = intent.getIntExtra(EXTRA_LEAD_MINUTES, 30)
                val isSilent = settings.silentAlarmsEnabled

                // 1. Play Alarm Ringtone & Vibration (or vibration-only if silent)
                ReminderAlarmPlayer.play(context, isSilent = isSilent)

                // 2. Show Full-Screen Alarm Alert & Notification
                showNotification(context, reminderId, title, eventTime, leadMinutes, isSilent)
            }
            ACTION_MARK_DONE -> {
                ReminderAlarmPlayer.stop()
                settings.completeReminder(reminderId)
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(reminderId.hashCode())
                Toast.makeText(context, "Reminder acknowledged ✓", Toast.LENGTH_SHORT).show()
                DiagnosticLogger.log("ReminderReceiver", "Acknowledged reminder $reminderId")
            }
            ACTION_SNOOZE -> {
                val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10)
                ReminderAlarmPlayer.stop()
                settings.snoozeReminder(reminderId, snoozeMinutes = snoozeMinutes)
                val reminder = settings.getReminders().find { it.id == reminderId }
                if (reminder != null) {
                    ReminderScheduler.schedule(context, reminder)
                }
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(reminderId.hashCode())
                val label = if (snoozeMinutes >= 60 && snoozeMinutes % 60 == 0) "${snoozeMinutes / 60} hour" else "$snoozeMinutes minutes"
                Toast.makeText(context, "Snoozed for $label ⏰", Toast.LENGTH_SHORT).show()
                DiagnosticLogger.log("ReminderReceiver", "Snoozed reminder $reminderId for ${snoozeMinutes}m")
            }
        }
    }

    private fun showNotification(
        context: Context,
        reminderId: String,
        title: String,
        eventEpochMs: Long,
        leadMinutes: Int,
        isSilent: Boolean
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = if (isSilent) NOTIFICATION_SILENT_CHANNEL_ID else NOTIFICATION_CHANNEL_ID

        // Create Channel with Alarm Sound & High Importance
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isSilent) {
                val silentChannel = NotificationChannel(
                    NOTIFICATION_SILENT_CHANNEL_ID,
                    "Lilt Silent Alarm Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Silent alarm-grade reminders that wake screen with zero audio"
                    enableLights(true)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 600)
                    setSound(null, null)
                    setShowBadge(true)
                }
                nm.createNotificationChannel(silentChannel)
            } else {
                val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Lilt Smart Alarm Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alarm-grade proactive reminders for meetings, calls, and appointments"
                    enableLights(true)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 600)
                    setSound(alarmSound, audioAttributes)
                    setBypassDnd(true)
                    setShowBadge(true)
                }
                nm.createNotificationChannel(channel)
            }
        }

        // Full-Screen Intent to launch ReminderAlertActivity immediately
        val fullScreenIntent = Intent(context, ReminderAlertActivity::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_EVENT_TIME, eventEpochMs)
            putExtra(EXTRA_LEAD_MINUTES, leadMinutes)
            putExtra(EXTRA_IS_SILENT, isSilent)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode() + 10,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Content Intent
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Acknowledge
        val doneIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_MARK_DONE
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode() + 1,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Snooze 10 Minutes
        val snooze10Intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_SNOOZE_MINUTES, 10)
        }
        val snooze10PendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode() + 2,
            snooze10Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeStr = if (eventEpochMs > 0) {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(eventEpochMs))
        } else ""

        val leadStr = if (leadMinutes >= 60) "${leadMinutes / 60}h" else "${leadMinutes}m"
        val bodyText = if (timeStr.isNotEmpty()) "Event is at $timeStr ($leadStr advance alert)" else "Scheduled reminder alert"
        val silentNotice = if (isSilent) " [🔕 Silent]" else ""

        val defaultSound = if (isSilent) null else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⏰ $title$silentNotice")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$bodyText\nTap to acknowledge or choose custom snooze."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .apply {
                if (defaultSound != null) {
                    setSound(defaultSound)
                }
            }
            .setVibrate(longArrayOf(0, 600, 300, 600, 300, 600))
            .addAction(0, "✓ Acknowledge", donePendingIntent)
            .addAction(0, "⏰ Snooze 10m", snooze10PendingIntent)
            .build()

        nm.notify(reminderId.hashCode(), notification)
    }
}
