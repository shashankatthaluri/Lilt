package com.lilt.mobile.reminder

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lilt.mobile.data.SettingsManager
import com.lilt.mobile.ui.theme.LiltTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderAlertActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Turn on screen and show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val settings = SettingsManager.getInstance(this)
        val reminderId = intent.getStringExtra(ReminderReceiver.EXTRA_REMINDER_ID) ?: ""
        val title = intent.getStringExtra(ReminderReceiver.EXTRA_TITLE) ?: "Scheduled Task"
        val eventEpochMs = intent.getLongExtra(ReminderReceiver.EXTRA_EVENT_TIME, 0L)
        val leadMinutes = intent.getIntExtra(ReminderReceiver.EXTRA_LEAD_MINUTES, 30)
        val isSilent = intent.getBooleanExtra(ReminderReceiver.EXTRA_IS_SILENT, false) || settings.silentAlarmsEnabled

        // Start alarm audio/vibration (respecting isSilent)
        ReminderAlarmPlayer.play(this, isSilent = isSilent)

        setContent {
            LiltTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    ReminderAlertScreen(
                        title = title,
                        eventEpochMs = eventEpochMs,
                        leadMinutes = leadMinutes,
                        isSilent = isSilent,
                        onAcknowledge = {
                            ReminderAlarmPlayer.stop()
                            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            nm.cancel(reminderId.hashCode())
                            settings.completeReminder(reminderId)
                            Toast.makeText(this, "Reminder acknowledged ✓", Toast.LENGTH_SHORT).show()
                            finish()
                        },
                        onSnooze = { minutes ->
                            ReminderAlarmPlayer.stop()
                            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            nm.cancel(reminderId.hashCode())
                            settings.snoozeReminder(reminderId, snoozeMinutes = minutes)
                            val snoozed = settings.getReminders().find { it.id == reminderId }
                            if (snoozed != null) {
                                ReminderScheduler.schedule(this, snoozed)
                            }
                            val label = if (minutes >= 60 && minutes % 60 == 0) "${minutes / 60} hour" else "$minutes minutes"
                            Toast.makeText(this, "Snoozed for $label ⏰", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ReminderAlarmPlayer.stop()
    }
}

@Composable
fun ReminderAlertScreen(
    title: String,
    eventEpochMs: Long,
    leadMinutes: Int,
    isSilent: Boolean,
    onAcknowledge: () -> Unit,
    onSnooze: (minutes: Int) -> Unit
) {
    val view = LocalView.current
    var snoozeMinutes by remember { mutableIntStateOf(10) }
    var isAudioSilenced by remember { mutableStateOf(isSilent) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringAlpha"
    )

    val timeFormatted = if (eventEpochMs > 0) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(eventEpochMs))
    } else ""
    val leadStr = if (leadMinutes >= 60) "${leadMinutes / 60}h" else "${leadMinutes}m"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header with Badge & In-Flight Mute Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Mode Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF18181B))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isSilent) Color(0xFFA1A1AA) else Color(0xFFD71921))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSilent) "SILENT ALARM (VISUAL)" else "LILT SMART ALARM",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }

            // Quick Mute button for audible alarms
            if (!isSilent) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isAudioSilenced) Color(0xFF27272A) else Color(0xFF1E1E24))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            ReminderAlarmPlayer.silenceAudio()
                            isAudioSilenced = true
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isAudioSilenced) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = if (isAudioSilenced) Color(0xFFA1A1AA) else Color(0xFFD71921),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isAudioSilenced) "Muted" else "Mute Audio",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Center Content: Pulsing Icon & Event Title
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(130.dp)
            ) {
                val badgeColor = if (isSilent) Color(0xFF52525B) else Color(0xFFD71921)
                // Outer pulsing glow ring
                Box(
                    modifier = Modifier
                        .size(126.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = ringAlpha * 0.35f))
                )
                // Mid glow ring
                Box(
                    modifier = Modifier
                        .size(102.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.25f))
                        .border(1.5.dp, badgeColor.copy(alpha = ringAlpha), CircleShape)
                )
                // Inner solid badge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                ) {
                    Icon(
                        imageVector = if (isSilent) Icons.Default.NotificationsOff else Icons.Default.Alarm,
                        contentDescription = "Alarm Active",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = title,
                color = Color.White,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (timeFormatted.isNotEmpty()) {
                Text(
                    text = "Event at $timeFormatted ($leadStr advance alert)",
                    color = Color(0xFFA1A1AA),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "Scheduled proactive reminder",
                    color = Color(0xFFA1A1AA),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Bottom Action Section: Acknowledge & Interactive [-] [+] Snooze Stepper
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Primary Solid White Button: ACKNOWLEDGE
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(27.dp))
                    .background(Color.White)
                    .clickable { onAcknowledge() }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ACKNOWLEDGE",
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Interactive Snooze Stepper ([-] [ 10 MINUTES ] [+])
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SNOOZE DURATION",
                    color = Color(0xFF71717A),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "-5m / +5m",
                    color = Color(0xFF52525B),
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(Color(0xFF141417))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(25.dp))
                    .padding(horizontal = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Minus Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF222228))
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            if (snoozeMinutes > 5) {
                                snoozeMinutes -= 5
                            }
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease 5 minutes",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Live Minutes Display
                Text(
                    text = if (snoozeMinutes >= 60 && snoozeMinutes % 60 == 0) {
                        val hrs = snoozeMinutes / 60
                        "$hrs ${if (hrs == 1) "HOUR" else "HOURS"}"
                    } else if (snoozeMinutes > 60) {
                        "${snoozeMinutes / 60}H ${snoozeMinutes % 60}M"
                    } else {
                        "$snoozeMinutes MINUTES"
                    },
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                // Plus Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF222228))
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            if (snoozeMinutes < 180) {
                                snoozeMinutes += 5
                            }
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase 5 minutes",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Quick Jump Presets (5m, 10m, 15m, 30m, 1h)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val presets = listOf(5, 10, 15, 30, 60)
                presets.forEach { mins ->
                    val isSelected = snoozeMinutes == mins
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color(0xFF2E2E35) else Color(0xFF101014))
                            .border(
                                width = if (isSelected) 1.2.dp else 1.dp,
                                color = if (isSelected) Color.White else Color(0x22FFFFFF),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                snoozeMinutes = mins
                            }
                    ) {
                        Text(
                            text = if (mins == 60) "1h" else "${mins}m",
                            color = if (isSelected) Color.White else Color(0xFFA1A1AA),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Action Button: SNOOZE FOR X MIN
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(Color(0xFF1A1A20))
                    .border(1.2.dp, Color(0x66FFFFFF), RoundedCornerShape(25.dp))
                    .clickable { onSnooze(snoozeMinutes) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Snooze,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SNOOZE FOR ${if (snoozeMinutes >= 60 && snoozeMinutes % 60 == 0) "${snoozeMinutes / 60} HOUR" else "$snoozeMinutes MIN"}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
