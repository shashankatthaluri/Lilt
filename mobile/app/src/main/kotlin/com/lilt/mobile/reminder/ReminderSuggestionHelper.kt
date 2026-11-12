package com.lilt.mobile.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.lilt.mobile.R
import com.lilt.mobile.engine.ParsedReminder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReminderSuggestionHelper {

    const val CHANNEL_ID = "lilt_reminder_suggestions_channel"

    fun showSuggestionNotification(
        context: Context,
        parsed: ParsedReminder,
        originalText: String
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val notifId = (parsed.title + parsed.eventEpochMs).hashCode()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lilt Reminder Suggestions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Interactive suggestions to schedule reminders detected from dictation"
                enableLights(true)
                setShowBadge(true)
            }
            nm.createNotificationChannel(channel)
        }

        val timeStr = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()).format(Date(parsed.eventEpochMs))
        val leadStr = if (parsed.leadMinutes >= 60) "${parsed.leadMinutes / 60}h" else "${parsed.leadMinutes}m"

        // Confirm Action Intent
        val confirmIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_CONFIRM_REMINDER
            putExtra(ReminderReceiver.EXTRA_TITLE, parsed.title)
            putExtra(ReminderReceiver.EXTRA_ORIGINAL_TEXT, originalText)
            putExtra(ReminderReceiver.EXTRA_EVENT_TIME, parsed.eventEpochMs)
            putExtra(ReminderReceiver.EXTRA_TRIGGER_TIME, parsed.triggerEpochMs)
            putExtra(ReminderReceiver.EXTRA_LEAD_MINUTES, parsed.leadMinutes)
            putExtra(ReminderReceiver.EXTRA_NOTIF_ID, notifId)
        }
        val confirmPendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 1,
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss Action Intent
        val dismissIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_DISMISS_SUGGESTION
            putExtra(ReminderReceiver.EXTRA_NOTIF_ID, notifId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 2,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bodyText = "$timeStr ($leadStr advance alert)\nTap 'Set Reminder' to schedule alarm, or 'Dismiss' to ignore."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⏰ Set reminder for \"${parsed.title}\"?")
            .setContentText("$timeStr ($leadStr advance alert)")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDeleteIntent(dismissPendingIntent)
            .addAction(0, "✓ Set Reminder", confirmPendingIntent)
            .addAction(0, "✕ Dismiss", dismissPendingIntent)
            .build()

        nm.notify(notifId, notification)
    }
}
