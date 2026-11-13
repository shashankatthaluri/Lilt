package com.lilt.mobile.ui.main

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.provider.ContactsContract
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.NotificationsOff
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import com.lilt.mobile.R
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusManager
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation3.runtime.NavKey
import com.lilt.mobile.accessibility.LiltAccessibilityService
import com.lilt.mobile.core.LiltCore
import com.lilt.mobile.data.DiagnosticLogger
import com.lilt.mobile.data.DictationItem
import com.lilt.mobile.data.ReminderItem
import com.lilt.mobile.reminder.ReminderScheduler
import androidx.compose.material.icons.filled.Schedule
import com.lilt.mobile.data.ReplacementItem
import com.lilt.mobile.data.SettingsManager
import com.lilt.mobile.data.SnippetItem
import com.lilt.mobile.data.VoiceCommandItem
import com.lilt.mobile.engine.CloudCleanupEngine
import com.lilt.mobile.engine.SpeechEngine
import com.lilt.mobile.overlay.LiltOverlayService
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Translate
import kotlinx.coroutines.launch
import com.lilt.mobile.ui.theme.LiltGlassCardBrush
import com.lilt.mobile.ui.theme.LiltGlassSpecularBorderBrush
import com.lilt.mobile.ui.theme.LiltNavBorderBrush
import com.lilt.mobile.ui.theme.LiltNavGlassColor
import com.lilt.mobile.ui.theme.LiltNavSelectedBrush
import com.lilt.mobile.ui.theme.LiltNavSelectedTint
import com.lilt.mobile.ui.theme.LiltNavUnselectedTint
import com.lilt.mobile.ui.theme.LiltThemeState
import com.lilt.mobile.ui.theme.NothingBlack
import com.lilt.mobile.ui.theme.NothingBorder
import com.lilt.mobile.ui.theme.NothingBorderSubtle
import com.lilt.mobile.ui.theme.NothingCard
import com.lilt.mobile.ui.theme.NothingCardElevated
import com.lilt.mobile.ui.theme.NothingGreen
import com.lilt.mobile.ui.theme.NothingOledDark
import com.lilt.mobile.ui.theme.NothingRed
import com.lilt.mobile.ui.theme.NothingTextMuted
import com.lilt.mobile.ui.theme.NothingTextPrimary
import com.lilt.mobile.ui.theme.NothingTextSecondary

/**
 * Automatically dismisses soft keyboard and clears blinking cursor focus when the user touches outside
 * any active text field, interactive card, or blank container.
 */
fun Modifier.clearFocusOnTap(focusManager: FocusManager): Modifier = this.pointerInput(Unit) {
    detectTapGestures(onTap = {
        focusManager.clearFocus()
    })
}

enum class LiveStudioState {
    IDLE,
    RECORDING,
    TRANSCRIBING,
    SUCCESS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val settings = remember { SettingsManager.getInstance(context) }

    var studioState by remember { mutableStateOf(LiveStudioState.IDLE) }
    var studioAudioRms by remember { mutableFloatStateOf(0f) }
    var isRecording by remember { mutableStateOf(false) }
    var testTranscript by remember { mutableStateOf("Tap the microphone to test live voice cleanup.") }
    var testCleaned by remember { mutableStateOf("") }
    var liveStudioText by remember { mutableStateOf("") }
    var isFloatingPillActive by remember { mutableStateOf(settings.floatingPillEnabled) }
    var isAlwaysOnActive by remember { mutableStateOf(!settings.floatingCircleContextualOnly) }

    // Engine & Liquid Glass Theme settings state
    var activeTone by remember { mutableStateOf(settings.activeTone) }
    var activeLanguageMode by remember { mutableStateOf(settings.activeLanguageMode) }
    var enabledLanguageCodes by remember { mutableStateOf(settings.enabledLanguageCodes) }
    var showLanguagePickerDialog by remember { mutableStateOf(false) }
    var spokenNumbers by remember { mutableStateOf(settings.spokenNumbers) }
    var spokenEmoji by remember { mutableStateOf(settings.spokenEmoji) }
    var numberSymbols by remember { mutableStateOf(settings.numberSymbols) }
    var autoCapitalize by remember { mutableStateOf(settings.autoCapitalize) }
    var trailingSpace by remember { mutableStateOf(settings.trailingSpace) }
    var hapticFeedback by remember { mutableStateOf(settings.hapticFeedback) }
    var historyStorageMode by remember { mutableStateOf(settings.historyStorageMode) }
    var noiseReductionEnabled by remember { mutableStateOf(settings.noiseReductionEnabled) }
    var hardwareNoiseSuppression by remember { mutableStateOf(settings.hardwareNoiseSuppression) }

    // Diagnostic & One-Click Support State
    var showErrorDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var errorReportUserNote by remember { mutableStateOf("") }
    var liveAlertMessage by remember { mutableStateOf<String?>(null) }

    // History state
    var historyItems by remember { mutableStateOf(settings.getHistory()) }
    var remindersList by remember { mutableStateOf(settings.getReminders()) }
    var smartRemindersEnabled by remember { mutableStateOf(settings.smartRemindersEnabled) }
    var smartRemindersAskFirst by remember { mutableStateOf(settings.smartRemindersAskFirst) }
    var silentAlarmsEnabled by remember { mutableStateOf(settings.silentAlarmsEnabled) }
    var detectedReminder by remember { mutableStateOf<com.lilt.mobile.engine.ParsedReminder?>(null) }
    var reminderScheduledFeedback by remember { mutableStateOf<String?>(null) }
    var historySearchQuery by remember { mutableStateOf("") }
    var autoCopyClipboard by remember { mutableStateOf(settings.autoCopyToClipboard) }
    var inlineAiCommandsEnabled by remember { mutableStateOf(settings.inlineAiCommandsEnabled) }
    var appContextAwarenessEnabled by remember { mutableStateOf(settings.appContextAwarenessEnabled) }
    var historySubTab by remember { mutableStateOf("recent") } // "recent" or "notes"
    var isMultiSelect by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var dissolvingIds by remember { mutableStateOf(setOf<String>()) }

    // Stats
    var wordsProcessed by remember { mutableStateOf(settings.wordsProcessed) }
    var hesitationsRemoved by remember { mutableStateOf(settings.hesitationsRemoved) }

    val coroutineScope = rememberCoroutineScope()

    // Cloud AI State (Groq / OpenAI)
    var cloudAiEnabled by remember { mutableStateOf(settings.cloudAiEnabled) }
    var cloudProvider by remember { mutableStateOf(settings.cloudProvider) }
    var cloudApiKey by remember { mutableStateOf(settings.cloudApiKey) }
    var cloudModel by remember { mutableStateOf(settings.cloudModel) }
    var cloudTesting by remember { mutableStateOf(false) }
    var cloudTestResult by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }

    // Personal Dictionary & Replacements State
    var customVocabulary by remember { mutableStateOf(settings.customVocabulary) }
    var replacementsList by remember { mutableStateOf(settings.getReplacements()) }
    var newHeard by remember { mutableStateOf("") }
    var newReplacement by remember { mutableStateOf("") }

    // Text Snippets State
    var snippetsList by remember { mutableStateOf(settings.getSnippets()) }
    var newTrigger by remember { mutableStateOf("") }
    var newExpansion by remember { mutableStateOf("") }

    // Writing Rules Hub State (Voice Commands, Dictionary, Snippets)
    var activeWritingTab by remember { mutableStateOf("commands") } // "commands", "dictionary", "snippets"
    var voiceCommandsList by remember { mutableStateOf(settings.getVoiceCommands()) }
    var newCommandTrigger by remember { mutableStateOf("") }
    var newCommandInstruction by remember { mutableStateOf("") }

    // Interactive Test Sandbox State
    var sandboxSelectedPreset by remember { mutableStateOf<String?>("Email") }
    var sandboxInput by remember { mutableStateOf("Write the email to Alex saying the demo is postponed to Thursday at 2 PM") }
    var sandboxOutput by remember { mutableStateOf("") }
    var sandboxLoading by remember { mutableStateOf(false) }
    var sandboxLangOverride by remember { mutableStateOf<String?>(null) } // null = use global

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Microphone permission is required for voice dictation", Toast.LENGTH_SHORT).show()
        }
    }

    val speechEngine = remember {
        SpeechEngine(context).apply {
            listener = object : SpeechEngine.Listener {
                override fun onReady() {
                    isRecording = true
                    studioState = LiveStudioState.RECORDING
                    liveStudioText = ""
                    testTranscript = ""
                    testCleaned = ""
                    detectedReminder = null
                    reminderScheduledFeedback = null
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    studioAudioRms = rmsdB
                }

                override fun onPartialResult(partialText: String) {
                    testTranscript = partialText
                    liveStudioText = partialText
                }

                override fun onEndOfSpeech() {
                    studioState = LiveStudioState.TRANSCRIBING
                    isRecording = false
                    studioAudioRms = 0f
                }

                override fun onFinalResult(cleanedText: String, rawText: String) {
                    isRecording = false
                    studioState = LiveStudioState.SUCCESS
                    studioAudioRms = 0f
                    val textToDeliver = cleanedText.ifBlank { rawText }
                    testTranscript = rawText
                    testCleaned = textToDeliver
                    liveStudioText = textToDeliver

                    // 1. Copy to clipboard
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText("Lilt Output", textToDeliver))
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()

                    historyItems = settings.getHistory()
                    wordsProcessed = settings.wordsProcessed
                    hesitationsRemoved = settings.hesitationsRemoved

                    // Return to IDLE after 1.6 seconds
                    coroutineScope.launch {
                        delay(1600)
                        if (studioState == LiveStudioState.SUCCESS) {
                            studioState = LiveStudioState.IDLE
                        }
                    }
                }

                override fun onNoSpeechDetected() {
                    isRecording = false
                    studioState = LiveStudioState.IDLE
                    studioAudioRms = 0f
                }

                override fun onError(error: String) {
                    isRecording = false
                    studioState = LiveStudioState.IDLE
                    studioAudioRms = 0f
                    Toast.makeText(context, "Lilt: $error", Toast.LENGTH_SHORT).show()
                    DiagnosticLogger.logError("SpeechEngine", error)
                }

                override fun onStopped() {
                    isRecording = false
                    studioAudioRms = 0f
                    if (studioState == LiveStudioState.RECORDING) {
                        studioState = LiveStudioState.TRANSCRIBING
                    }
                }

                override fun onReminderDetected(reminder: com.lilt.mobile.engine.ParsedReminder, originalText: String) {
                    if (settings.smartRemindersEnabled) {
                        detectedReminder = reminder
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechEngine.stopListening()
        }
    }

    // Bottom Navigation State & Scroll Management
    var activeTab by remember { mutableStateOf("dictate") } // "dictate", "history", "rules", "settings"
    val mainScrollState = rememberScrollState()

    // Automatically clear cursor focus whenever the user scrolls
    LaunchedEffect(mainScrollState.isScrollInProgress) {
        if (mainScrollState.isScrollInProgress) {
            focusManager.clearFocus()
        }
    }

    // Apple HIG: Switching tabs or sub-tabs automatically resets view to top, clears focus and refreshes history
    LaunchedEffect(activeTab) {
        focusManager.clearFocus()
        if (mainScrollState.value != 0) {
            mainScrollState.scrollTo(0)
        }
        if (activeTab == "history") {
            historyItems = settings.getHistory()
            remindersList = settings.getReminders()
            wordsProcessed = settings.wordsProcessed
            hesitationsRemoved = settings.hesitationsRemoved
        }
    }

    // Real-time zero-effort live sync: Whenever history changes or app resumes, automatically update state
    LaunchedEffect(Unit) {
        SettingsManager.historyUpdatedFlow.collect {
            historyItems = settings.getHistory()
            remindersList = settings.getReminders()
            wordsProcessed = settings.wordsProcessed
            hesitationsRemoved = settings.hesitationsRemoved
        }
    }
    LaunchedEffect(historySubTab) {
        focusManager.clearFocus()
        mainScrollState.scrollTo(0)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NothingBlack,
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                color = if (LiltThemeState.isDark) Color(0xF20B0C10) else Color(0xF2FFFFFF),
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 24.dp,
                border = BorderStroke(
                    width = 1.2.dp,
                    brush = LiltGlassSpecularBorderBrush
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(66.dp)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf(
                        Triple("dictate", Icons.Default.Mic, "Dictate"),
                        Triple("history", Icons.Default.History, "History"),
                        Triple("rules", Icons.Default.Tune, "Rules"),
                        Triple("settings", Icons.Default.Settings, "Settings")
                    ).forEach { (tabId, icon, label) ->
                        val isSelected = activeTab == tabId
                        val animScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.05f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = 0.65f,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "nav_tab_scale"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) {
                                        LiltNavSelectedBrush
                                    } else {
                                        SolidColor(Color.Transparent)
                                    }
                                )
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    brush = if (isSelected) {
                                        Brush.verticalGradient(
                                            listOf(Color(0x66FFFFFF), Color(0x22FFFFFF))
                                        )
                                    } else {
                                        Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                                    },
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (activeTab == tabId) {
                                        if (tabId == "history") {
                                            historyItems = settings.getHistory()
                                            remindersList = settings.getReminders()
                                        }
                                        coroutineScope.launch {
                                            mainScrollState.animateScrollTo(0)
                                        }
                                    } else {
                                        activeTab = tabId
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = animScale
                                    scaleY = animScale
                                }
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) LiltNavSelectedTint else LiltNavUnselectedTint,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = label.uppercase(),
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) LiltNavSelectedTint else LiltNavUnselectedTint,
                                    letterSpacing = 0.6.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clearFocusOnTap(focusManager)
        ) {
            // Layer 1: Adaptive Liquid Glass Ambient Mesh Backdrop (Dark Aurora / Light Quartz)
            LiquidGlassAmbientBackdrop(isDark = LiltThemeState.isDark)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(mainScrollState)
                    .clearFocusOnTap(focusManager)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Spacer(modifier = Modifier.height(6.dp))

            // 1. Header: Universal Liquid Glass Lockup (Persistent on all tabs)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.lilt_brand_dots),
                        contentDescription = "Lilt Logo",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "L I L T",
                            color = NothingTextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp
                        )
                        Text(
                            text = "Speak at the speed of thought",
                            color = NothingTextSecondary,
                            fontSize = 11.sp,
                            letterSpacing = 0.4.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // One-Click Report Button (Top Bar)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(NothingCard)
                            .border(1.dp, LiltGlassSpecularBorderBrush, RoundedCornerShape(16.dp))
                            .clickable { showErrorDialog = true }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = "Report Issue",
                                tint = NothingTextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "REPORT",
                                color = NothingTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            }

            // Live Alert / Error Notification Banner (with 1-Click Dev Email Action)
            AnimatedVisibility(visible = liveAlertMessage != null) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = NothingCard)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF2A2A34), RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Alert",
                                    tint = Color(0xFFA1A1AA),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ENGINE NOTICE",
                                    color = Color(0xFFA1A1AA),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = NothingTextMuted,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { liveAlertMessage = null }
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = liveAlertMessage ?: "",
                                color = NothingTextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Brush.horizontalGradient(listOf(Color(0xFFD71921), Color(0xFFB51219))))
                                        .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(8.dp))
                                        .clickable {
                                            DiagnosticLogger.sendErrorReport(
                                                context,
                                                "Reported from Live Alert Banner",
                                                liveAlertMessage
                                            )
                                            liveAlertMessage = null
                                        }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = null,
                                            tint = NothingTextPrimary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "1-CLICK REPORT TO DEV",
                                            color = NothingTextPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NothingCardElevated)
                                        .clickable { liveAlertMessage = null }
                                ) {
                                    Text(
                                        text = "DISMISS",
                                        color = NothingTextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(140, easing = LinearOutSlowInEasing))
                        .togetherWith(fadeOut(animationSpec = tween(90, easing = FastOutLinearInEasing)))
                },
                label = "tab_liquid_transition"
            ) { currentTab ->
                Column(modifier = Modifier.fillMaxWidth()) {
            // ── DICTATE TAB ─────────────────────────────────
            if (currentTab == "dictate") {

            // 2. Speed & Productivity Stats Card (Nothing OS Widget / Wispr Flow Parity)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NothingCard),
                border = BorderStroke(1.dp, LiltGlassSpecularBorderBrush)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LiltGlassCardBrush)
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = "Speed",
                                tint = NothingTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PRODUCTIVITY SUMMARY",
                                color = NothingTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(NothingCard)
                                .border(1.dp, Color(0xFF282830), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "GROQ CLOUD",                                color = Color(0xFFA1A1AA),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Stat 1: Words Processed
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "$wordsProcessed",
                                color = NothingTextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Words Spoken",
                                color = NothingTextMuted,
                                fontSize = 11.sp
                            )
                        }

                        // Stat 2: Time Saved
                        val timeSavedMin = (wordsProcessed * 0.02).toInt().coerceAtLeast(3)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${timeSavedMin}m",
                                color = NothingTextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Time Saved",
                                color = NothingTextMuted,
                                fontSize = 11.sp
                            )
                        }

                        // Stat 3: Hesitations Filtered
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$hesitationsRemoved",
                                color = NothingGreen,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Fillers Cleaned",
                                color = NothingTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2b. Language & Script Selector Strip
            Text(
                text = "LANGUAGE & SCRIPT MODE",
                color = NothingTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TonePill(
                    label = "Auto (${enabledLanguageCodes.size})",
                    isActive = activeLanguageMode == "auto",
                    modifier = Modifier.width(96.dp),
                    onClick = {
                        activeLanguageMode = "auto"
                        settings.activeLanguageMode = "auto"
                    }
                )
                val enabledLangObjs = enabledLanguageCodes.mapNotNull { SettingsManager.findLanguageByCode(it) }
                enabledLangObjs.forEach { lang ->
                    TonePill(
                        label = lang.shortLabel,
                        isActive = activeLanguageMode == lang.code,
                        modifier = Modifier.width(96.dp),
                        onClick = {
                            activeLanguageMode = lang.code
                            settings.activeLanguageMode = lang.code
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Quick Tone & Style Selector Strip
            Text(
                text = "DICTATION TONE PRESET",
                color = NothingTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TonePill(
                    label = "Natural",
                    isActive = activeTone == "natural",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        activeTone = "natural"
                        settings.activeTone = "natural"
                    }
                )
                TonePill(
                    label = "Executive",
                    isActive = activeTone == "pro",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        activeTone = "pro"
                        settings.activeTone = "pro"
                    }
                )
                TonePill(
                    label = "Casual",
                    isActive = activeTone == "casual",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        activeTone = "casual"
                        settings.activeTone = "casual"
                    }
                )
                TonePill(
                    label = "Bullets",
                    isActive = activeTone == "bullets",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        activeTone = "bullets"
                        settings.activeTone = "bullets"
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

                        // 4. Interactive Live Dictation Studio Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clearFocusOnTap(focusManager),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NothingCard),
                border = BorderStroke(1.dp, LiltGlassSpecularBorderBrush)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LiltGlassCardBrush)
                        .clearFocusOnTap(focusManager)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "LIVE SPEECH STUDIO",
                        color = NothingTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Pulse & Ripple transitions
                    val infiniteTransition = rememberInfiniteTransition(label = "studio_fx")

                    // Voice-reactive ripple based strictly on audio decibels heard (NOT a continuous loop)
                    val voiceRippleScale by animateFloatAsState(
                        targetValue = if (studioState == LiveStudioState.RECORDING && studioAudioRms > 2f) {
                            1f + (studioAudioRms.coerceIn(0f, 30f) / 30f)
                        } else 1f,
                        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
                        label = "voiceRippleScale"
                    )
                    val voiceRippleAlpha by animateFloatAsState(
                        targetValue = if (studioState == LiveStudioState.RECORDING && studioAudioRms > 2f) {
                            (studioAudioRms.coerceIn(0f, 30f) / 30f).coerceIn(0.2f, 0.75f)
                        } else 0f,
                        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
                        label = "voiceRippleAlpha"
                    )

                    // Continuous rippling effect while transcribing (yellow dot state)
                    val transcribingRippleScale by infiniteTransition.animateFloat(
                        initialValue = 0.95f,
                        targetValue = if (studioState == LiveStudioState.TRANSCRIBING) 1.55f else 1.0f,
                        animationSpec = infiniteRepeatable(tween(900, easing = LinearOutSlowInEasing), RepeatMode.Restart),
                        label = "transcribingRippleScale"
                    )
                    val transcribingRippleAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.7f,
                        targetValue = if (studioState == LiveStudioState.TRANSCRIBING) 0.0f else 0.0f,
                        animationSpec = infiniteRepeatable(tween(900, easing = LinearOutSlowInEasing), RepeatMode.Restart),
                        label = "transcribingRippleAlpha"
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(108.dp)
                    ) {
                        // Voice-Reactive Red Ripple (only replicates when voice is heard)
                        if (studioState == LiveStudioState.RECORDING) {
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .graphicsLayer {
                                        scaleX = voiceRippleScale
                                        scaleY = voiceRippleScale
                                        alpha = voiceRippleAlpha
                                    }
                                    .border(2.dp, Brush.radialGradient(listOf(Color(0xFFFF3B30), Color(0x33D71921))), CircleShape)
                            )
                        }

                        // Continuous Yellow Ripple (Active during transcribing)
                        if (studioState == LiveStudioState.TRANSCRIBING) {
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .graphicsLayer {
                                        scaleX = transcribingRippleScale
                                        scaleY = transcribingRippleScale
                                        alpha = transcribingRippleAlpha
                                    }
                                    .border(2.dp, Brush.radialGradient(listOf(Color(0xFFFFB800), Color(0x33FFB800))), CircleShape)
                            )
                        }

                        // Outer Translucent Liquid Glass Ring
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = when (studioState) {
                                            LiveStudioState.RECORDING -> listOf(Color(0x88D71921), Color(0x442B0D11), Color(0x6614141A))
                                            LiveStudioState.TRANSCRIBING -> listOf(Color(0x66FFB800), Color(0x333A2A0C), Color(0x6614141A))
                                            LiveStudioState.SUCCESS -> listOf(Color(0x6600E676), Color(0x330B3019), Color(0x6614141A))
                                            LiveStudioState.IDLE -> listOf(Color(0x55282B38), Color(0x8812141C))
                                        }
                                    )
                                )
                                .border(
                                    width = 1.5.dp,
                                    brush = when (studioState) {
                                        LiveStudioState.RECORDING -> Brush.sweepGradient(listOf(Color(0xFFFF5252), Color(0xFFD71921), Color(0x88FF5252), Color(0xFFFF5252)))
                                        LiveStudioState.TRANSCRIBING -> Brush.sweepGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFB800), Color(0x66FFD54F), Color(0xFFFFD54F)))
                                        LiveStudioState.SUCCESS -> Brush.sweepGradient(listOf(Color(0xFF69F0AE), Color(0xFF00E676), Color(0x6669F0AE), Color(0xFF69F0AE)))
                                        LiveStudioState.IDLE -> LiltGlassSpecularBorderBrush
                                    },
                                    shape = CircleShape
                                )
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    if (ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        if (studioState == LiveStudioState.RECORDING) {
                                            studioState = LiveStudioState.TRANSCRIBING
                                            speechEngine.stopListening()
                                        } else if (studioState == LiveStudioState.IDLE || studioState == LiveStudioState.SUCCESS) {
                                            speechEngine.startListening()
                                        }
                                    }
                                }
                        ) {
                            // Inside the Circle: Only Red Dot when recording, Yellow Dot when transcribing, Green Dot on success, Mic when idle
                            when (studioState) {
                                LiveStudioState.RECORDING -> {
                                    // ONLY a dynamic Red Dot inside the circle
                                    val dynamicRedDotSize = (15 + (studioAudioRms.coerceIn(0f, 30f) / 6f)).dp
                                    Box(
                                        modifier = Modifier
                                            .size(dynamicRedDotSize)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF2A2A))
                                    )
                                }

                                LiveStudioState.TRANSCRIBING -> {
                                    // Yellow / Amber Dot inside the circle
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFFB800))
                                    )
                                }

                                LiveStudioState.SUCCESS -> {
                                    // Green Dot inside the circle
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00E676))
                                    )
                                }

                                LiveStudioState.IDLE -> {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Dictate",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // No wave form under the circle
                    if (studioState == LiveStudioState.IDLE) {
                        Text(
                            text = "Tap microphone to test live speech cleanup",
                            color = NothingTextMuted,
                            fontSize = 12.sp
                        )
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Interactive Live Speech Sandbox Text Box
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF71717A))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                // NO change of text color above the text box - constant grey as normal
                                Text(
                                    text = when (studioState) {
                                        LiveStudioState.RECORDING -> "Recording, please speak"
                                        LiveStudioState.TRANSCRIBING -> "Transcribing…"
                                        LiveStudioState.SUCCESS -> "Transcribed & copied to clipboard"
                                        LiveStudioState.IDLE -> if (liveStudioText.isNotBlank()) "Polished speech result" else "Live speech output box"
                                    },
                                    color = Color(0xFF71717A),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                            }

                            if (liveStudioText.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Clear",
                                        color = Color(0xFFA1A1AA),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable {
                                            liveStudioText = ""
                                            testCleaned = ""
                                            testTranscript = ""
                                        }
                                    )
                                    Text(text = "•", color = Color(0xFF3F3F46), fontSize = 11.sp)
                                    Text(
                                        text = "Copy",
                                        color = NothingTextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            clipboard?.setPrimaryClip(ClipData.newPlainText("Lilt Output", liveStudioText))
                                            Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = liveStudioText,
                            onValueChange = { liveStudioText = it },
                            readOnly = true,
                            placeholder = {
                                Text(
                                    text = "Tap the microphone above and speak. Your live transcript and polished output will stream right into this showcase…",
                                    color = Color(0xFF52525B),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 220.dp),
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = NothingCardElevated,
                                unfocusedContainerColor = NothingCardElevated,
                                focusedBorderColor = Color(0x60FFFFFF),
                                unfocusedBorderColor = NothingBorder,
                                focusedTextColor = NothingTextPrimary,
                                unfocusedTextColor = Color(0xFFE4E4E7),
                                cursorColor = Color.Transparent
                            ),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        )

                        if (testTranscript.isNotBlank() && testTranscript != liveStudioText && testCleaned.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Raw spoken: $testTranscript",
                                color = Color(0xFF71717A),
                                fontSize = 11.sp,
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        // Interactive Smart Reminder Confirmation Card
                        AnimatedVisibility(
                            visible = detectedReminder != null || reminderScheduledFeedback != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF141416))
                                        .border(
                                            1.dp,
                                            if (reminderScheduledFeedback != null) Color(0x6600E676) else Color(0x40FFB800),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .padding(14.dp)
                                ) {
                                    if (reminderScheduledFeedback != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0x2600E676)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color(0xFF00E676),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = "Reminder Scheduled",
                                                    color = Color(0xFF00E676),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = reminderScheduledFeedback ?: "",
                                                    color = Color(0xFFA1A1AA),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    } else {
                                        detectedReminder?.let { parsed ->
                                            val timeFormat = java.text.SimpleDateFormat("EEE, MMM d • h:mm a", java.util.Locale.getDefault())
                                            val timeStr = timeFormat.format(java.util.Date(parsed.eventEpochMs))
                                            val leadStr = if (parsed.leadMinutes >= 60) "${parsed.leadMinutes / 60}h" else "${parsed.leadMinutes}m"

                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0x26FFB800)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Alarm,
                                                            contentDescription = null,
                                                            tint = Color(0xFFFFB800),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "Set reminder for \"${parsed.title}\"?",
                                                            color = Color.White,
                                                            fontSize = 12.5.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "$timeStr ($leadStr advance alert)",
                                                            color = Color(0xFFA1A1AA),
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Dismiss Button
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(34.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(Color(0x1AFFFFFF))
                                                            .clickable {
                                                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                                detectedReminder = null
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "Dismiss",
                                                            color = Color(0xFFA1A1AA),
                                                            fontSize = 11.5.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }

                                                    // Confirm Button
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(34.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(Color(0xFFD71921))
                                                            .clickable {
                                                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                                                val reminder = com.lilt.mobile.data.ReminderItem(
                                                                    title = parsed.title,
                                                                    originalText = liveStudioText.ifBlank { parsed.title },
                                                                    eventEpochMs = parsed.eventEpochMs,
                                                                    triggerEpochMs = parsed.triggerEpochMs,
                                                                    leadMinutes = parsed.leadMinutes
                                                                )
                                                                settings.addReminder(reminder)
                                                                com.lilt.mobile.reminder.ReminderScheduler.schedule(context, reminder)
                                                                remindersList = settings.getReminders()

                                                                reminderScheduledFeedback = "$timeStr ($leadStr alert)"
                                                                detectedReminder = null

                                                                coroutineScope.launch {
                                                                    delay(3000)
                                                                    reminderScheduledFeedback = null
                                                                }
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = Color.White,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Text(
                                                                text = "Set Reminder",
                                                                color = Color.White,
                                                                fontSize = 11.5.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Smart Voice Commands Trigger Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clearFocusOnTap(focusManager),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = NothingCard),
                border = BorderStroke(1.dp, LiltGlassSpecularBorderBrush)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LiltGlassCardBrush)
                        .clearFocusOnTap(focusManager)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(NothingRed)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SMART VOICE COMMANDS",
                                color = NothingTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x1AD71921))
                                .border(1.dp, Color(0x33D71921), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "AUTO-DETECT",
                                color = NothingRed,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Speak these opening prefixes to format your speech on the fly:",
                        color = NothingTextMuted,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    listOf(
                        "\"Write the email...\"" to "Email format with Subject & [Placeholders]",
                        "\"Write it raw...\"" to "Exact words verbatim + punctuation (no rephrasing)",
                        "\"Write it casual...\"" to "Warm, relaxed, or playful conversational tone",
                        "\"Make a list...\"" to "Clean markdown bullet points with '• '"
                    ).forEach { (trigger, desc) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF13151D))
                                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Fixed-width trigger pill badge: perfectly aligned across all rows
                            Box(
                                modifier = Modifier
                                    .width(126.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1D202C))
                                    .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 7.dp, vertical = 4.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = trigger,
                                    color = Color.White,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "→",
                                color = NothingRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = desc,
                                color = NothingTextSecondary,
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            } // end dictate tab

            // ── HISTORY TAB ─────────────────────────────────
            if (currentTab == "history") {

            Spacer(modifier = Modifier.height(8.dp))

            // History Header (with Apple HIG Multi-Select controls)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isMultiSelect) "${selectedIds.size} SELECTED" else "H I S T O R Y",
                        color = if (isMultiSelect) Color(0xFFFFB800) else NothingTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = if (isMultiSelect) 1.5.sp else 3.sp
                    )
                    Text(
                        text = if (isMultiSelect) "Select cards to move to notes or delete" else "Your voice dictation & note snippets",
                        color = NothingTextMuted,
                        fontSize = 12.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (historyItems.isNotEmpty()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0x14FFFFFF))
                                .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(18.dp))
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    showClearHistoryDialog = true
                                }
                                .padding(horizontal = 14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Clear History",
                                    tint = Color(0xFFA1A1AA),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Clear All",
                                    color = Color(0xFFA1A1AA),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stats summary at top of History
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NothingCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LiltGlassCardBrush).border(1.dp, LiltGlassSpecularBorderBrush, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$wordsProcessed", color = NothingTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Words", color = NothingTextMuted, fontSize = 11.sp)
                    }
                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(NothingBorder))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "${historyItems.size}", color = NothingTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Sessions", color = NothingTextMuted, fontSize = 11.sp)
                    }
                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(NothingBorder))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val notesCount = historyItems.count { it.isNote }
                        Text(text = "$notesCount", color = Color(0xFFFFB800), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Notes", color = NothingTextMuted, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sort pinned items to the very top
            val recentItems = historyItems.sortedWith(
                compareByDescending<DictationItem> { it.isPinned }
                    .thenByDescending { it.timestamp }
            )
            val noteItems = historyItems.filter { it.isNote }.sortedWith(
                compareByDescending<DictationItem> { it.isPinned }
                    .thenByDescending { it.timestamp }
            )

            val queryClean = historySearchQuery.trim()
            val isSearching = queryClean.isNotEmpty()

            val displayRecent = if (isSearching) {
                recentItems.filter {
                    it.cleanedText.contains(queryClean, ignoreCase = true) ||
                    it.rawText.contains(queryClean, ignoreCase = true) ||
                    (it.contactName?.contains(queryClean, ignoreCase = true) == true)
                }
            } else recentItems

            val displayNotes = if (isSearching) {
                noteItems.filter {
                    it.cleanedText.contains(queryClean, ignoreCase = true) ||
                    it.rawText.contains(queryClean, ignoreCase = true) ||
                    (it.noteTitle?.contains(queryClean, ignoreCase = true) == true)
                }
            } else noteItems

            val displayReminders = if (isSearching) {
                remindersList.filter {
                    it.title.contains(queryClean, ignoreCase = true) ||
                    it.originalText.contains(queryClean, ignoreCase = true)
                }
            } else remindersList

            // Nothing OS History Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(NothingCard)
                    .border(
                        1.dp,
                        if (isSearching) Color(0x88FFB800) else Color(0x28FFFFFF),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isSearching) Color(0xFFFFB800) else Color(0xFF71717A),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = historySearchQuery,
                        onValueChange = { historySearchQuery = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        textStyle = TextStyle(
                            color = NothingTextPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(Color(0xFFFFB800)),
                        decorationBox = { innerTextField ->
                            if (historySearchQuery.isEmpty()) {
                                Text(
                                    text = "Search dictations, notes, alerts...",
                                    color = Color(0xFF71717A),
                                    fontSize = 13.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (historySearchQuery.isNotEmpty()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(NothingCardElevated)
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    historySearchQuery = ""
                                    focusManager.clearFocus()
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = Color(0xFFA1A1AA),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            if (isSearching) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val totalMatches = displayRecent.size + displayReminders.size
                    Text(
                        text = "FOUND $totalMatches MATCH${if (totalMatches == 1) "" else "ES"} FOR \"${queryClean.uppercase()}\"",
                        color = Color(0xFFFFB800),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Clear",
                        color = Color(0xFFA1A1AA),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable {
                            historySearchQuery = ""
                            focusManager.clearFocus()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Liquid Glass Segmented Switcher [ Recent | Alerts | Notes ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NothingCard)
                    .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(12.dp))
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Recent Segment
                val isRecentSelected = historySubTab == "recent"
                val recentBgAnim by animateColorAsState(
                    targetValue = if (isRecentSelected) Color(0xFF26262B) else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "recentBg"
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(9.dp))
                        .background(recentBgAnim)
                        .clickable { historySubTab = "recent" }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = if (isRecentSelected) Color.White else Color(0xFF71717A),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Recent (${displayRecent.size})",
                            color = if (isRecentSelected) Color.White else Color(0xFF71717A),
                            fontSize = 12.sp,
                            fontWeight = if (isRecentSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                // Reminders Segment
                val isRemindersSelected = historySubTab == "reminders"
                val remindersBgAnim by animateColorAsState(
                    targetValue = if (isRemindersSelected) Color(0xFF26262B) else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "remindersBg"
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(9.dp))
                        .background(remindersBgAnim)
                        .clickable {
                            historySubTab = "reminders"
                            remindersList = settings.getReminders()
                        }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (isRemindersSelected) NothingRed else Color(0xFF71717A),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Alerts (${displayReminders.size})",
                            color = if (isRemindersSelected) Color.White else Color(0xFF71717A),
                            fontSize = 12.sp,
                            fontWeight = if (isRemindersSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                // Notes Segment
                val isNotesSelected = historySubTab == "notes"
                val notesBgAnim by animateColorAsState(
                    targetValue = if (isNotesSelected) Color(0xFF26262B) else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "notesBg"
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(9.dp))
                        .background(notesBgAnim)
                        .clickable { historySubTab = "notes" }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = if (isNotesSelected) Color(0xFFFFB800) else Color(0xFF71717A),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Notes (${displayNotes.size})",
                            color = if (isNotesSelected) Color.White else Color(0xFF71717A),
                            fontSize = 12.sp,
                            fontWeight = if (isNotesSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))



            // Instant 120 FPS hardware-accelerated crossfade between Recent and Notes
            AnimatedContent(
                targetState = historySubTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(120, easing = LinearOutSlowInEasing))
                        .togetherWith(fadeOut(animationSpec = tween(80, easing = FastOutLinearInEasing)))
                },
                label = "HistorySubTabAnimation"
            ) { subTab ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (subTab == "reminders") {
                        // Silent Alarm Mode Toggle Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = NothingCard)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, if (silentAlarmsEnabled) Color(0x44FFFFFF) else Color(0x22FFFFFF), RoundedCornerShape(18.dp))
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (silentAlarmsEnabled) Color(0xFF222228) else Color(0x26D71921))
                                            .border(1.dp, if (silentAlarmsEnabled) Color(0x44FFFFFF) else Color(0x4DD71921), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = if (silentAlarmsEnabled) Icons.Default.NotificationsOff else Icons.Default.Alarm,
                                            contentDescription = null,
                                            tint = if (silentAlarmsEnabled) Color(0xFFA1A1AA) else Color(0xFFD71921),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (silentAlarmsEnabled) "Silent Alarm Mode" else "Audible Alarm Mode",
                                            color = NothingTextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (silentAlarmsEnabled) "Wakes screen & demands action with zero audio" else "Rings loud alarm stream until actioned",
                                            color = Color(0xFF71717A),
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                                Switch(
                                    checked = silentAlarmsEnabled,
                                    onCheckedChange = {
                                        silentAlarmsEnabled = it
                                        settings.silentAlarmsEnabled = it
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF3F3F46),
                                        uncheckedThumbColor = Color(0xFF71717A),
                                        uncheckedTrackColor = NothingCardElevated
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        if (displayReminders.isEmpty()) {
                            if (isSearching) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(22.dp),
                                    colors = CardDefaults.cardColors(containerColor = NothingCard)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(22.dp))
                                            .padding(vertical = 40.dp, horizontal = 24.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(CircleShape)
                                                    .background(NothingCardElevated)
                                                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.SearchOff,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFFB800),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Text(
                                                text = "No alerts match \"${queryClean}\"",
                                                color = NothingTextPrimary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Try different keywords or tap clear to view all alerts.",
                                                color = Color(0xFF71717A),
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .height(34.dp)
                                                    .clip(RoundedCornerShape(17.dp))
                                                    .background(Color.White)
                                                    .clickable { historySearchQuery = "" }
                                                    .padding(horizontal = 16.dp)
                                            ) {
                                                Text(
                                                    text = "Clear Search",
                                                    color = Color.Black,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = NothingCard)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(22.dp))
                                        .padding(vertical = 44.dp, horizontal = 24.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .background(NothingCardElevated)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = Color(0xFFA1A1AA),
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text(
                                            text = "No scheduled reminders yet",
                                            color = NothingTextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Dictate anything with a time: 'Meeting tomorrow at 3 PM', 'Dentist appointment at 11 AM', or 'Repu 10 AM ki doctor deggariki vellali'. Lilt automatically sets reminder alerts.",
                                            color = Color(0xFF71717A),
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 17.sp
                                        )
                                    }
                                }
                            }
                            }
                        } else {
                            displayReminders.forEach { rem ->
                                ReminderCard(
                                    reminder = rem,
                                    searchQuery = queryClean,
                                    isDissolving = rem.id in dissolvingIds,
                                    onComplete = {
                                        settings.completeReminder(rem.id)
                                        remindersList = settings.getReminders()
                                    },
                                    onDelete = {
                                        coroutineScope.launch {
                                            dissolvingIds = setOf(rem.id)
                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                            delay(600)
                                            ReminderScheduler.cancel(context, rem.id)
                                            settings.deleteReminder(rem.id)
                                            remindersList = settings.getReminders()
                                            dissolvingIds = emptySet()
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    } else if (subTab == "recent") {
                        if (displayRecent.isEmpty()) {
                            if (isSearching) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(22.dp),
                                    colors = CardDefaults.cardColors(containerColor = NothingCard)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(22.dp))
                                            .padding(vertical = 40.dp, horizontal = 24.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(CircleShape)
                                                    .background(NothingCardElevated)
                                                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.SearchOff,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFFB800),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Text(
                                                text = "No dictations match \"${queryClean}\"",
                                                color = NothingTextPrimary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Try different keywords or tap clear to view all recordings.",
                                                color = Color(0xFF71717A),
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .height(34.dp)
                                                    .clip(RoundedCornerShape(17.dp))
                                                    .background(Color.White)
                                                    .clickable { historySearchQuery = "" }
                                                    .padding(horizontal = 16.dp)
                                            ) {
                                                Text(
                                                    text = "Clear Search",
                                                    color = Color.Black,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = NothingCard)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(22.dp))
                                        .padding(vertical = 44.dp, horizontal = 24.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(NothingCardElevated)
                                                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = null,
                                                tint = Color(0xFFA1A1AA),
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text(
                                            text = "No voice dictations yet",
                                            color = NothingTextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Record speech from the Dictate studio or tap the floating edge pill across any app.",
                                            color = Color(0xFF71717A),
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 17.sp
                                        )
                                    }
                                }
                            }
                            }
                        } else {
                            displayRecent.forEach { item ->
                                VoiceNoteCard(
                                    item = item,
                                    searchQuery = queryClean,
                                    isMultiSelect = isMultiSelect,
                                    isSelected = item.id in selectedIds,
                                    isDissolving = item.id in dissolvingIds,
                                    onCopy = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        clipboard?.setPrimaryClip(ClipData.newPlainText("Lilt Dictation", item.cleanedText))
                                        Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    onDelete = {
                                        coroutineScope.launch {
                                            dissolvingIds = setOf(item.id)
                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                            delay(600)
                                            settings.deleteHistory(item.id)
                                            historyItems = settings.getHistory()
                                            dissolvingIds = emptySet()
                                        }
                                    },
                                    onTogglePin = {
                                        settings.toggleNote(item.id)
                                        historyItems = settings.getHistory()
                                    },
                                    onTogglePinTop = {
                                        settings.togglePin(item.id)
                                        historyItems = settings.getHistory()
                                    },
                                    onSelectToggle = {
                                        selectedIds = if (item.id in selectedIds) selectedIds - item.id else selectedIds + item.id
                                    },
                                    onLongClick = {
                                        isMultiSelect = true
                                        selectedIds = selectedIds + item.id
                                    },
                                    onSaveContact = {
                                        settings.markContactSaved(item.id)
                                        historyItems = settings.getHistory()
                                    }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    } else {
                        // NOTES SUB-TAB
                        if (displayNotes.isEmpty()) {
                            if (isSearching) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(22.dp),
                                    colors = CardDefaults.cardColors(containerColor = NothingCard)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(22.dp))
                                            .padding(vertical = 40.dp, horizontal = 24.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(CircleShape)
                                                    .background(NothingCardElevated)
                                                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.SearchOff,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFFB800),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Text(
                                                text = "No notes match \"${queryClean}\"",
                                                color = NothingTextPrimary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Try different keywords or tap clear to view all notes.",
                                                color = Color(0xFF71717A),
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .height(34.dp)
                                                    .clip(RoundedCornerShape(17.dp))
                                                    .background(Color.White)
                                                    .clickable { historySearchQuery = "" }
                                                    .padding(horizontal = 16.dp)
                                            ) {
                                                Text(
                                                    text = "Clear Search",
                                                    color = Color.Black,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = NothingCard)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(22.dp))
                                        .padding(vertical = 44.dp, horizontal = 24.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF1E1A14))
                                                .border(1.dp, Color(0x4DFFB800), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.BookmarkBorder,
                                                contentDescription = null,
                                                tint = Color(0xFFFFB800),
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text(
                                            text = "No structured notes yet",
                                            color = NothingTextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Say \"Take a note…\" during dictation to auto-structure notes, or tap the bookmark icon on any recent voice card.",
                                            color = Color(0xFF71717A),
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 17.sp
                                        )
                                    }
                                }
                            }
                            }
                        } else {
                            displayNotes.forEach { item ->
                                NoteCard(
                                    item = item,
                                    searchQuery = queryClean,
                                    isMultiSelect = isMultiSelect,
                                    isSelected = item.id in selectedIds,
                                    isDissolving = item.id in dissolvingIds,
                                    onCopy = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        clipboard?.setPrimaryClip(ClipData.newPlainText("Lilt Note", item.cleanedText.replace("~", "")))
                                        Toast.makeText(context, "Copied note to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    onDelete = {
                                        coroutineScope.launch {
                                            dissolvingIds = setOf(item.id)
                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                            delay(600)
                                            settings.deleteHistory(item.id)
                                            historyItems = settings.getHistory()
                                            dissolvingIds = emptySet()
                                        }
                                    },
                                    onTogglePin = {
                                        settings.toggleNote(item.id)
                                        historyItems = settings.getHistory()
                                    },
                                    onTogglePinTop = {
                                        settings.togglePin(item.id)
                                        historyItems = settings.getHistory()
                                    },
                                    onUpdateText = { newText ->
                                        settings.updateHistoryItem(item.id, newText)
                                        historyItems = settings.getHistory()
                                    },
                                    onSelectToggle = {
                                        selectedIds = if (item.id in selectedIds) selectedIds - item.id else selectedIds + item.id
                                    },
                                    onLongClick = {
                                        isMultiSelect = true
                                        selectedIds = selectedIds + item.id
                                    },
                                    onSaveContact = {
                                        settings.markContactSaved(item.id)
                                        historyItems = settings.getHistory()
                                    }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }

            } // end history tab
            
            // ── RULES TAB ─────────────────────────────────
            if (currentTab == "rules") {

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "R U L E S",
                color = NothingTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Voice commands, dictionary & snippets",
                color = NothingTextMuted,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 8. Unified Writing Rules & Smart Commands Hub
            Text(
                text = "WRITING RULES & SMART COMMANDS",
                color = NothingTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NothingCard),
                border = BorderStroke(1.dp, LiltGlassSpecularBorderBrush)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LiltGlassCardBrush)
                        .padding(16.dp)
                ) {
                    // Sub-Tab Navigation Bar: Voice Commands, Dictionary, Snippets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple("commands", Icons.Default.Mic, "Commands (${voiceCommandsList.size})"),
                            Triple("dictionary", Icons.Default.Book, "Dictionary (${replacementsList.size})"),
                            Triple("snippets", Icons.Default.Edit, "Snippets (${snippetsList.size})")
                        ).forEach { (tabId, tabIcon, tabLabel) ->
                            val isSelected = activeWritingTab == tabId
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) {
                                            Brush.verticalGradient(
                                                if (LiltThemeState.isDark) listOf(Color(0xFF2C2F3C), Color(0xFF1B1D25)) else listOf(Color(0xFFFFFFFF), Color(0xFFEBEAE4))
                                            )
                                        } else {
                                            Brush.verticalGradient(
                                                if (LiltThemeState.isDark) listOf(Color(0xFF121319), Color(0xFF0C0D12)) else listOf(Color(0xFFF7F6F2), Color(0xFFEFECE6))
                                            )
                                        }
                                    )
                                    .border(
                                        width = 1.2.dp,
                                        color = if (isSelected) Color(0x99FFFFFF) else NothingBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { activeWritingTab = tabId }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = tabIcon,
                                        contentDescription = tabLabel,
                                        tint = if (isSelected) Color.White else Color(0xFFA1A1AA),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = tabLabel,
                                        color = if (isSelected) Color.White else Color(0xFFA1A1AA),
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // TAB 1: SMART VOICE COMMANDS & TEST SANDBOX
                    if (activeWritingTab == "commands") {
                        Text(
                            text = "Active Voice Commands",
                            color = NothingTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Begin speaking with these triggers to format on the fly:",
                            color = NothingTextMuted,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        voiceCommandsList.forEach { cmd ->
                            val isCmdDissolving = cmd.id in dissolvingIds
                            val cmdDustAnim by animateFloatAsState(
                                targetValue = if (isCmdDissolving) 1f else 0f,
                                animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
                                label = "dustDisintegrationCmd_${cmd.id}"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        if (isCmdDissolving) {
                                            alpha = ((1f - cmdDustAnim) * (1f - cmdDustAnim)).coerceIn(0f, 1f)
                                            scaleX = 1f + cmdDustAnim * 0.16f
                                            scaleY = (1f - cmdDustAnim * 0.85f).coerceAtLeast(0.05f)
                                            translationX = cmdDustAnim * 90f
                                            translationY = -cmdDustAnim * 30f
                                            rotationZ = cmdDustAnim * 6f
                                        }
                                    }
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NothingCard)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "\"${cmd.trigger}\"",
                                            color = Color(0xFFA1A1AA),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (cmd.isBuiltIn) Color(0xFF1F1F1F) else Color(0xFF2B1214))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = if (cmd.isBuiltIn) "SYSTEM" else "CUSTOM",
                                                color = if (cmd.isBuiltIn) NothingTextMuted else Color.White,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = cmd.instruction,
                                        color = NothingTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                if (!cmd.isBuiltIn) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = Color(0xFF777777),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                coroutineScope.launch {
                                                    dissolvingIds = setOf(cmd.id)
                                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                    delay(600)
                                                    settings.deleteVoiceCommand(cmd.id)
                                                    voiceCommandsList = settings.getVoiceCommands()
                                                    dissolvingIds = emptySet()
                                                }
                                            }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Add Custom Voice Command Form
                        Text(
                            text = "+ Add Custom Voice Command",
                            color = NothingTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newCommandTrigger,
                                onValueChange = { newCommandTrigger = it },
                                placeholder = { Text("Trigger (e.g. Bug report)", color = NothingTextMuted, fontSize = 11.sp) },
                                singleLine = true,
                                textStyle = TextStyle(color = NothingTextPrimary, fontSize = 11.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = NothingBorder,
                                    focusedContainerColor = NothingCardElevated,
                                    unfocusedContainerColor = NothingCardElevated
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newCommandInstruction,
                                onValueChange = { newCommandInstruction = it },
                                placeholder = { Text("Instruction (e.g. Format as: **Steps**, **Actual**)", color = NothingTextMuted, fontSize = 11.sp) },
                                singleLine = true,
                                textStyle = TextStyle(color = NothingTextPrimary, fontSize = 11.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = NothingBorder,
                                    focusedContainerColor = NothingCardElevated,
                                    unfocusedContainerColor = NothingCardElevated
                                ),
                                modifier = Modifier.weight(1.5f)
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.horizontalGradient(listOf(Color(0xFF2A2D3A), Color(0xFF1E202A)))
                                    )
                                    .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(10.dp))
                                    .clickable(enabled = newCommandTrigger.isNotBlank() && newCommandInstruction.isNotBlank()) {
                                        settings.addVoiceCommand(newCommandTrigger.trim(), newCommandInstruction.trim())
                                        voiceCommandsList = settings.getVoiceCommands()
                                        newCommandTrigger = ""
                                        newCommandInstruction = ""
                                    }
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // ⚡ INTERACTIVE TEST SANDBOX (Glass Master Card with Specular Outline)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clearFocusOnTap(focusManager),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = NothingCard),
                            border = BorderStroke(1.2.dp, LiltGlassSpecularBorderBrush)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(LiltGlassCardBrush)
                                    .clearFocusOnTap(focusManager)
                                    .padding(18.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(if (LiltThemeState.isDark) Color(0xFF1E202A) else Color(0xFFECECEC))
                                                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FlashOn,
                                                contentDescription = "Sandbox",
                                                tint = Color(0xFFD71921),
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "INTERACTIVE TEST SANDBOX",
                                                color = NothingTextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            )
                                            Text(
                                                text = "Real-time cleanup & voice formatting preview",
                                                color = NothingTextMuted,
                                                fontSize = 10.5.sp
                                            )
                                        }
                                    }


                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Unified Sample Formatting Presets
                                Text(
                                    text = "SAMPLE PROMPTS",
                                    color = NothingTextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        Triple(Icons.AutoMirrored.Filled.Send, "Email", "Write the email to Alex saying the demo is postponed to Thursday at 2 PM"),
                                        Triple(Icons.Default.Edit, "@fix", "he dont know nothing about the new schedule @fix"),
                                        Triple(Icons.Default.FlashOn, "@email", "need to reschedule our sprint demo to friday 3pm @email"),
                                        Triple(Icons.Default.Translate, "Tenglish", "Write the email to Rahul repu morning 10 AM ki quick sprint catchup undi please be on time"),
                                        Triple(Icons.Default.AutoAwesome, "Notes", "Meeting notes team aligned on Q4 goals Shiva is leading Android launch by Friday"),
                                        Triple(Icons.Default.CheckCircle, "List", "Make a list buy organic milk finish mobile build deploy to prod")
                                    ).forEach { (icon, label, phrase) ->
                                        val isSelected = sandboxSelectedPreset == label
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .height(38.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isSelected) {
                                                        Brush.verticalGradient(
                                                            if (LiltThemeState.isDark) listOf(Color(0xFF282C3A), Color(0xFF141620)) else listOf(Color(0xFFFFFFFF), Color(0xFFF2F0EA))
                                                        )
                                                    } else {
                                                        SolidColor(NothingCardElevated)
                                                    }
                                                )
                                                .border(
                                                    width = if (isSelected) 1.2.dp else 1.dp,
                                                    brush = if (isSelected) {
                                                        Brush.verticalGradient(listOf(Color(0x99FFFFFF), Color(0x66FFFFFF)))
                                                    } else {
                                                        SolidColor(Color(0x22FFFFFF))
                                                    },
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    sandboxSelectedPreset = label
                                                    sandboxInput = phrase
                                                    sandboxOutput = ""
                                                    sandboxLangOverride = null
                                                }
                                                .padding(horizontal = 14.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                if (isSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFFD71921))
                                                    )
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                }
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = if (isSelected) Color.White else NothingTextSecondary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = label,
                                                    color = if (isSelected) Color.White else NothingTextSecondary,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = sandboxInput,
                                    onValueChange = {
                                        sandboxInput = it
                                        sandboxSelectedPreset = null
                                    },
                                    label = { Text("Input prompt to test", color = NothingTextMuted, fontSize = 11.sp) },
                                    placeholder = { Text("Type any spoken phrase or tap a sample prompt above…", color = Color(0xFF52525B), fontSize = 12.sp) },
                                    textStyle = TextStyle(color = NothingTextPrimary, fontSize = 12.5.sp),
                                    shape = RoundedCornerShape(14.dp),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.White,
                                        unfocusedBorderColor = Color(0x33FFFFFF),
                                        focusedContainerColor = NothingCardElevated,
                                        unfocusedContainerColor = NothingCardElevated
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (sandboxLoading) SolidColor(Color(0xFF222222)) else Brush.horizontalGradient(listOf(Color(0xFFD71921), Color(0xFFB51219))))
                                            .border(1.dp, if (sandboxLoading) Color(0xFF333333) else Color(0x66FFFFFF), RoundedCornerShape(12.dp))
                                            .clickable(enabled = !sandboxLoading && sandboxInput.isNotBlank()) {
                                                sandboxLoading = true
                                                sandboxOutput = "Processing with AI…"
                                                coroutineScope.launch {
                                                    val customVocab = settings.customVocabulary
                                                        .split(",")
                                                        .map { it.trim() }
                                                        .filter { it.isNotEmpty() }
                                                    val reps = settings.getReplacements().associate { it.heard to it.replacement }

                                                    val directive = CloudCleanupEngine.parseLanguageDirective(
                                                        sandboxInput,
                                                        sandboxLangOverride ?: activeLanguageMode,
                                                        enabledLanguageCodes
                                                    )
                                                    var result = ""
                                                    val inlineMatch = Regex("(?s)^(.*?)(?:\\s+|\\n)@(fix|email|tenglish|short|bullets|lilt|ai)\\s*$").find(sandboxInput)
                                                    if (inlineMatch != null && settings.cloudApiKey.isNotBlank()) {
                                                        val prompt = inlineMatch.groupValues[1].trim()
                                                        val cmd = inlineMatch.groupValues[2].lowercase()
                                                        val inlineRes = CloudCleanupEngine.processInlineCommand(
                                                            promptText = prompt,
                                                            command = cmd,
                                                            provider = settings.cloudProvider,
                                                            apiKey = settings.cloudApiKey,
                                                            model = settings.cloudModel,
                                                            languageMode = directive.effectiveLanguageMode,
                                                            customVocabulary = customVocab
                                                        )
                                                        if (inlineRes.isSuccess) {
                                                            result = inlineRes.getOrThrow()
                                                        }
                                                    } else if (settings.cloudAiEnabled && settings.cloudApiKey.isNotBlank()) {
                                                        val cloudRes = CloudCleanupEngine.polish(
                                                            rawText = directive.strippedText,
                                                            provider = settings.cloudProvider,
                                                            apiKey = settings.cloudApiKey,
                                                            model = settings.cloudModel,
                                                            tone = settings.activeTone,
                                                            customVocabulary = customVocab,
                                                            replacements = reps,
                                                            voiceCommands = settings.getVoiceCommands(),
                                                            languageMode = directive.effectiveLanguageMode,
                                                            enabledLanguages = enabledLanguageCodes
                                                        )
                                                        if (cloudRes.isSuccess) {
                                                            result = cloudRes.getOrThrow()
                                                        }
                                                    }

                                                    if (result.isBlank()) {
                                                        result = LiltCore.processText(
                                                            raw = sandboxInput,
                                                            cleanupLevel = settings.cleanupLevel,
                                                            numbersAsDigits = settings.spokenNumbers,
                                                            numberSymbols = settings.numberSymbols,
                                                            spokenEmoji = settings.spokenEmoji,
                                                            autoCapitalize = settings.autoCapitalize,
                                                            trailingSpace = settings.trailingSpace,
                                                            tone = settings.activeTone
                                                        )
                                                    }

                                                    result = settings.applyReplacementsAndSnippets(result)
                                                    result = CloudCleanupEngine.formatEmailSpacing(result)
                                                    sandboxOutput = result
                                                    sandboxLoading = false
                                                    sandboxLangOverride = null
                                                }
                                            }
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = if (sandboxLoading) "PROCESSING…" else "TEST FORMATTING",
                                            color = Color.White,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp
                                        )
                                    }

                                    if (sandboxOutput.isNotBlank() && !sandboxLoading) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(NothingCardElevated)
                                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                                                .clickable {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                    clipboard?.setPrimaryClip(ClipData.newPlainText("Lilt Sandbox", sandboxOutput))
                                                    Toast.makeText(context, "Copied sandbox result!", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = NothingTextPrimary, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text("Copy Result", color = NothingTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }

                                if (sandboxOutput.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(NothingCardElevated)
                                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                                            .padding(14.dp)
                                    ) {
                                        Text(
                                            text = sandboxOutput,
                                            color = NothingTextPrimary,
                                            fontSize = 13.sp,
                                            lineHeight = 21.sp
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(NothingCardElevated)
                                            .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(14.dp))
                                            .padding(vertical = 14.dp, horizontal = 14.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(if (LiltThemeState.isDark) Color(0xFF1E2028) else Color(0xFFE2E2E2))
                                                    .border(1.dp, Color(0x28FFFFFF), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = Color(0xFFA1A1AA),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "Sandbox Output Ready",
                                                    color = Color(0xFFA1A1AA),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "Tap a preset chip above and press TEST FORMATTING to preview cleanup",
                                                    color = Color(0xFF71717A),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TAB 2: PERSONAL DICTIONARY & REPLACEMENTS
                    if (activeWritingTab == "dictionary") {
                        Text(
                            text = "Vocabulary Context Hints",
                            color = NothingTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Comma-separated terms fed into the recognition engine and LLM.",
                            color = NothingTextMuted,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customVocabulary,
                            onValueChange = {
                                customVocabulary = it
                                settings.customVocabulary = it
                            },
                            textStyle = TextStyle(color = NothingTextPrimary, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = NothingBorder,
                                focusedContainerColor = NothingCardElevated,
                                unfocusedContainerColor = NothingCardElevated
                            ),
                            placeholder = { Text("e.g. Lilt, Groq, Nothing OS, Rust ARM64", color = NothingTextMuted, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Word Replacements (${replacementsList.size})",
                            color = NothingTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Replaces misrecognized words automatically.",
                            color = NothingTextMuted,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Existing replacements list
                        replacementsList.forEach { rep ->
                            val isRepDissolving = rep.id in dissolvingIds
                            val repDustAnim by animateFloatAsState(
                                targetValue = if (isRepDissolving) 1f else 0f,
                                animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
                                label = "dustDisintegrationRep_${rep.id}"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        if (isRepDissolving) {
                                            alpha = ((1f - repDustAnim) * (1f - repDustAnim)).coerceIn(0f, 1f)
                                            scaleX = 1f + repDustAnim * 0.16f
                                            scaleY = (1f - repDustAnim * 0.85f).coerceAtLeast(0.05f)
                                            translationX = repDustAnim * 90f
                                            translationY = -repDustAnim * 30f
                                            rotationZ = repDustAnim * 6f
                                        }
                                    }
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NothingCard)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = rep.heard,
                                        color = NothingTextMuted,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = " → ",
                                        color = NothingTextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = rep.replacement,
                                        color = NothingTextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = Color(0xFF777777),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            coroutineScope.launch {
                                                dissolvingIds = setOf(rep.id)
                                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                delay(600)
                                                settings.deleteReplacement(rep.id)
                                                replacementsList = settings.getReplacements()
                                                dissolvingIds = emptySet()
                                            }
                                        }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Add new replacement row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newHeard,
                                onValueChange = { newHeard = it },
                                placeholder = { Text("Heard", color = NothingTextMuted, fontSize = 11.sp) },
                                singleLine = true,
                                textStyle = TextStyle(color = NothingTextPrimary, fontSize = 11.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = NothingBorder,
                                    focusedContainerColor = NothingCardElevated,
                                    unfocusedContainerColor = NothingCardElevated
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newReplacement,
                                onValueChange = { newReplacement = it },
                                placeholder = { Text("Replace with", color = NothingTextMuted, fontSize = 11.sp) },
                                singleLine = true,
                                textStyle = TextStyle(color = NothingTextPrimary, fontSize = 11.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = NothingBorder,
                                    focusedContainerColor = NothingCardElevated,
                                    unfocusedContainerColor = NothingCardElevated
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.horizontalGradient(listOf(Color(0xFF2A2D3A), Color(0xFF1E202A)))
                                    )
                                    .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(10.dp))
                                    .clickable(enabled = newHeard.isNotBlank() && newReplacement.isNotBlank()) {
                                        settings.addReplacement(newHeard.trim(), newReplacement.trim())
                                        replacementsList = settings.getReplacements()
                                        newHeard = ""
                                        newReplacement = ""
                                    }
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // TAB 3: TEXT SNIPPETS & MACROS
                    if (activeWritingTab == "snippets") {
                        Text(
                            text = "Trigger Snippets (${snippetsList.size})",
                            color = NothingTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Saying or typing triggers auto-expands into full templates.",
                            color = NothingTextMuted,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        snippetsList.forEach { snippet ->
                            val isSnpDissolving = snippet.id in dissolvingIds
                            val snpDustAnim by animateFloatAsState(
                                targetValue = if (isSnpDissolving) 1f else 0f,
                                animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
                                label = "dustDisintegrationSnp_${snippet.id}"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        if (isSnpDissolving) {
                                            alpha = ((1f - snpDustAnim) * (1f - snpDustAnim)).coerceIn(0f, 1f)
                                            scaleX = 1f + snpDustAnim * 0.16f
                                            scaleY = (1f - snpDustAnim * 0.85f).coerceAtLeast(0.05f)
                                            translationX = snpDustAnim * 90f
                                            translationY = -snpDustAnim * 30f
                                            rotationZ = snpDustAnim * 6f
                                        }
                                    }
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NothingCard)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = snippet.trigger,
                                        color = Color(0xFFA1A1AA),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = snippet.expansion,
                                        color = NothingTextPrimary,
                                        fontSize = 12.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = Color(0xFF777777),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            coroutineScope.launch {
                                                dissolvingIds = setOf(snippet.id)
                                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                delay(600)
                                                settings.deleteSnippet(snippet.id)
                                                snippetsList = settings.getSnippets()
                                                dissolvingIds = emptySet()
                                            }
                                        }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Add new snippet row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newTrigger,
                                onValueChange = { newTrigger = it },
                                placeholder = { Text("Trigger (;email)", color = NothingTextMuted, fontSize = 11.sp) },
                                singleLine = true,
                                textStyle = TextStyle(color = NothingTextPrimary, fontSize = 11.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = NothingBorder,
                                    focusedContainerColor = NothingCardElevated,
                                    unfocusedContainerColor = NothingCardElevated
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newExpansion,
                                onValueChange = { newExpansion = it },
                                placeholder = { Text("Expansion", color = NothingTextMuted, fontSize = 11.sp) },
                                singleLine = true,
                                textStyle = TextStyle(color = NothingTextPrimary, fontSize = 11.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = NothingBorder,
                                    focusedContainerColor = NothingCardElevated,
                                    unfocusedContainerColor = NothingCardElevated
                                ),
                                modifier = Modifier.weight(1.5f)
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.horizontalGradient(listOf(Color(0xFF2A2D3A), Color(0xFF1E202A)))
                                    )
                                    .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(10.dp))
                                    .clickable(enabled = newTrigger.isNotBlank() && newExpansion.isNotBlank()) {
                                        settings.addSnippet(newTrigger.trim(), newExpansion.trim())
                                        snippetsList = settings.getSnippets()
                                        newTrigger = ""
                                        newExpansion = ""
                                    }
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            } // end rules tab

            // ── SETTINGS TAB (Consolidated Liquid Glass Architecture) ──────────
            if (currentTab == "settings") {

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "S E T T I N G S",
                color = NothingTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Universal Liquid Glass theme, engine, integrations & privacy",
                color = NothingTextMuted,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── MASTER CARD 1: SURFACES & FLOATING PILL ────────────────
            Text(
                text = "SURFACES & FLOATING PILL",
                color = NothingTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NothingCard),
                border = BorderStroke(1.dp, LiltGlassSpecularBorderBrush)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LiltGlassCardBrush)
                        .padding(16.dp)
                ) {
                    // Item 1: Floating Glass Circle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NothingCardElevated)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureInPicture,
                                contentDescription = "Floating Pill",
                                tint = NothingTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Floating Glass Circle",
                                color = NothingTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Overlay bubble over any app. Tap to dictate & auto-paste.",
                                color = NothingTextMuted,
                                fontSize = 11.5.sp
                            )
                        }

                        Switch(
                            checked = isFloatingPillActive,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    } else {
                                        settings.floatingPillEnabled = true
                                        isFloatingPillActive = true
                                        LiltOverlayService.start(context)
                                    }
                                } else {
                                    settings.floatingPillEnabled = false
                                    isFloatingPillActive = false
                                    LiltOverlayService.stop(context)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFD71921),
                                checkedBorderColor = Color(0xFFE50914),
                                uncheckedThumbColor = Color(0xFFA1A1AA),
                                uncheckedTrackColor = NothingCardElevated,
                                uncheckedBorderColor = Color(0xFF282933)
                            )
                        )
                    }

                    AnimatedVisibility(visible = isFloatingPillActive) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NothingCardElevated)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Always-On Mode",
                                        color = NothingTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (isAlwaysOnActive) "Pill stays visible everywhere" else "Pill appears only when keyboard is open",
                                        color = NothingTextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = isAlwaysOnActive,
                                    onCheckedChange = { checked ->
                                        isAlwaysOnActive = checked
                                        settings.floatingCircleContextualOnly = !checked
                                        LiltOverlayService.setAlwaysOn(checked)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFFD71921),
                                        checkedBorderColor = Color(0xFFE50914),
                                        uncheckedThumbColor = Color(0xFFA1A1AA),
                                        uncheckedTrackColor = NothingCard,
                                        uncheckedBorderColor = Color(0xFF282933)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x1AFFFFFF)))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Item 2: Accessibility Service
                    val isAccessibilityActive = LiltAccessibilityService.isServiceRunning()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isAccessibilityActive) Color(0x2234C759) else Color(0x22FFA000))
                            ) {
                                Icon(
                                    imageVector = if (isAccessibilityActive) Icons.Default.Check else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isAccessibilityActive) Color(0xFF34C759) else Color(0xFFFFA000),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Accessibility Service",
                                        color = NothingTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isAccessibilityActive) "ACTIVE" else "TAP TO ENABLE",
                                        color = if (isAccessibilityActive) Color(0xFF34C759) else Color(0xFFFFA000),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Required for contextual floating pill and direct cursor paste.",
                                    color = NothingTextMuted,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                        Switch(
                            checked = isAccessibilityActive,
                            onCheckedChange = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF34C759),
                                checkedBorderColor = Color(0xFF34C759),
                                uncheckedThumbColor = Color(0xFFA1A1AA),
                                uncheckedTrackColor = NothingCard,
                                uncheckedBorderColor = Color(0xFF282933)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x1AFFFFFF)))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Item 3: Voice Keyboard IME
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NothingCardElevated)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Keyboard,
                                    contentDescription = "Keyboard",
                                    tint = NothingTextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Lilt Voice Keyboard (IME)",
                                    color = NothingTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Hold spacebar to dictate inside any OEM keyboard.",
                                    color = NothingTextMuted,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingCardElevated)
                                .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                                    context.startActivity(intent)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = "SETUP", color = NothingTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── MASTER CARD 2: AUDIO DSP & LANGUAGES ──────────────────
            Text(
                text = "AUDIO DSP & LANGUAGES",
                color = NothingTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NothingCard),
                border = BorderStroke(1.dp, LiltGlassSpecularBorderBrush)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LiltGlassCardBrush)
                        .padding(16.dp)
                ) {
                    // Row 1: Languages in Auto Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Languages in Auto Mode",
                                color = NothingTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            val summary = enabledLanguageCodes.mapNotNull { SettingsManager.findLanguageByCode(it)?.shortLabel }.joinToString(", ")
                            Text(
                                text = summary,
                                color = NothingTextMuted,
                                fontSize = 11.5.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingCardElevated)
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                                .clickable { showLanguagePickerDialog = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "MANAGE (${enabledLanguageCodes.size})",
                                color = NothingTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x1AFFFFFF)))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Row 2: Audio DSP Noise Filtering
                    NothingSettingToggle(
                        icon = Icons.Default.Tune,
                        title = "Audio DSP Noise Filtering",
                        subtitle = "85Hz Butterworth filter + adaptive noise gate silences AC hum & fan rumble.",
                        checked = noiseReductionEnabled,
                        onCheckedChange = { checked ->
                            noiseReductionEnabled = checked
                            settings.noiseReductionEnabled = checked
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x1AFFFFFF)))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Row 3: Hardware Multi-Mic Beamforming
                    NothingSettingToggle(
                        icon = Icons.Default.Mic,
                        title = "Hardware Beamforming",
                        subtitle = "Directional multi-mic array and acoustic echo cancellation.",
                        checked = hardwareNoiseSuppression,
                        onCheckedChange = { checked ->
                            hardwareNoiseSuppression = checked
                            settings.hardwareNoiseSuppression = checked
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x1AFFFFFF)))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Row 4: Audio Privacy Policy
                    Column {
                        Text(
                            text = "Audio Privacy Policy",
                            color = NothingTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Choose whether raw audio is retained locally for replay or discarded.",
                            color = NothingTextMuted,
                            fontSize = 11.5.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val isAudioMode = historyStorageMode == "audio_and_text"
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isAudioMode) Color(0xFF221111) else Color(0xFF141414))
                                    .border(1.dp, if (isAudioMode) Color.White else NothingBorder, RoundedCornerShape(10.dp))
                                    .clickable {
                                        historyStorageMode = "audio_and_text"
                                        settings.historyStorageMode = "audio_and_text"
                                    }
                                    .padding(vertical = 10.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Audio & Text", color = if (isAudioMode) Color.White else NothingTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(text = "Save for replay", color = NothingTextMuted, fontSize = 10.sp)
                                }
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (!isAudioMode) Color(0xFF14241B) else Color(0xFF141414))
                                    .border(1.dp, if (!isAudioMode) NothingGreen else NothingBorder, RoundedCornerShape(10.dp))
                                    .clickable {
                                        historyStorageMode = "text_only"
                                        settings.historyStorageMode = "text_only"
                                    }
                                    .padding(vertical = 10.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Text Only", color = if (!isAudioMode) NothingGreen else NothingTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(text = "0 MB Audio / Private", color = NothingTextMuted, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── MASTER CARD 3: CLOUD AI ENGINE & CLEANUP ──────────────
            Text(
                text = "CLOUD AI & CLEANUP PREFERENCES",
                color = NothingTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NothingCard),
                border = BorderStroke(1.dp, LiltGlassSpecularBorderBrush)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LiltGlassCardBrush)
                        .padding(16.dp)
                ) {
                    // Ultra-Fast Cloud Polish Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF181818))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = "Cloud AI",
                                tint = if (cloudAiEnabled) Color.White else NothingTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ultra-Fast Cloud Polish",
                                color = NothingTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (cloudAiEnabled) "Active • ~150ms latency via Groq Qwen 3.8" else "Disabled • Using on-device engine",
                                color = if (cloudAiEnabled) NothingGreen else NothingTextMuted,
                                fontSize = 11.sp
                            )
                        }

                        Switch(
                            checked = cloudAiEnabled,
                            onCheckedChange = {
                                cloudAiEnabled = it
                                settings.cloudAiEnabled = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFD71921),
                                checkedBorderColor = Color(0xFFE50914),
                                uncheckedThumbColor = Color(0xFFA1A1AA),
                                uncheckedTrackColor = NothingCardElevated,
                                uncheckedBorderColor = Color(0xFF282933)
                            )
                        )
                    }

                    if (cloudAiEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // Provider Selector
                        Text(
                            text = "PROVIDER",
                            color = NothingTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("groq" to "GROQ (FAST)", "openai" to "OPENAI", "gemini" to "GEMINI").forEach { (id, label) ->
                                val isSelected = cloudProvider.lowercase() == id
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0xFF2A2D3A) else Color(0xFF141416))
                                        .border(1.dp, if (isSelected) Color(0x99FFFFFF) else NothingBorder, RoundedCornerShape(10.dp))
                                        .clickable {
                                            cloudProvider = id
                                            settings.cloudProvider = id
                                            cloudApiKey = settings.getApiKeyForProvider(id)
                                            cloudModel = when (id) {
                                                "openai" -> "gpt-4o-mini"
                                                "gemini" -> "gemini-2.0-flash"
                                                else -> "qwen/qwen3.8-27b"
                                            }
                                            settings.cloudModel = cloudModel
                                            cloudTestResult = ""
                                        }
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else Color(0xFFA1A1AA),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Model Presets
                        Text(
                            text = "MODEL PRESET",
                            color = NothingTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val currentPresets: List<Pair<String, String>> = when (cloudProvider.lowercase()) {
                            "openai" -> listOf(
                                "gpt-4o-mini" to "GPT-4o Mini (Fast)",
                                "gpt-4o" to "GPT-4o (Deep)"
                            )
                            "gemini" -> listOf(
                                "gemini-2.0-flash" to "Gemini 2.0 Flash (Fast)",
                                "gemini-1.5-flash" to "Gemini 1.5 Flash"
                            )
                            else -> listOf(
                                "qwen/qwen3.8-27b" to "Qwen 3.8 27B",
                                "llama-3.3-70b-versatile" to "Llama 3.3 70B",
                                "llama-3.1-8b-instant" to "Llama 3.1 8B"
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            currentPresets.forEach { (modelId, modelLabel) ->
                                val isModelSelected = cloudModel == modelId
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isModelSelected) Color(0xFF232530) else Color(0xFF101216))
                                        .border(1.dp, if (isModelSelected) Color(0x99FFFFFF) else NothingBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            cloudModel = modelId
                                            settings.cloudModel = modelId
                                            cloudTestResult = ""
                                        }
                                ) {
                                    Text(
                                        text = modelLabel,
                                        color = if (isModelSelected) Color.White else Color(0xFFA1A1AA),
                                        fontSize = 9.5.sp,
                                        fontWeight = if (isModelSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Active Model ID input (editable for custom models)
                        Text(
                            text = "ACTIVE MODEL ID",
                            color = NothingTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = cloudModel,
                            onValueChange = {
                                cloudModel = it
                                settings.cloudModel = it
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            textStyle = TextStyle(color = NothingTextPrimary, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = NothingBorder,
                                focusedContainerColor = NothingCardElevated,
                                unfocusedContainerColor = NothingCardElevated
                            ),
                            placeholder = { Text("Model ID (e.g. qwen/qwen3.8-27b)", color = NothingTextMuted, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // API Key Input
                        Text(
                            text = "API KEY (${cloudProvider.uppercase()})",
                            color = NothingTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = cloudApiKey,
                            onValueChange = {
                                cloudApiKey = it
                                settings.setApiKeyForProvider(cloudProvider, it)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation('●'),
                            textStyle = TextStyle(color = NothingTextPrimary, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = NothingBorder,
                                focusedContainerColor = NothingCardElevated,
                                unfocusedContainerColor = NothingCardElevated
                            ),
                            placeholder = {
                                val ph = when (cloudProvider.lowercase()) {
                                    "openai" -> "Enter OpenAI API key (sk-...)"
                                    "gemini" -> "Enter Gemini API key (AIza...)"
                                    else -> "Enter Groq API key (gsk_...)"
                                }
                                Text(ph, color = NothingTextMuted, fontSize = 12.sp)
                            },
                            trailingIcon = {
                                IconButton(onClick = { showApiKey = !showApiKey }) {
                                    Icon(
                                        imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showApiKey) "Hide key" else "Show key",
                                        tint = NothingTextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Test Connection Button & Result Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NothingCardElevated)
                                    .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                                    .clickable(enabled = !cloudTesting && cloudApiKey.isNotBlank()) {
                                        cloudTesting = true
                                        cloudTestResult = "Testing connection…"
                                        coroutineScope.launch {
                                            val res = CloudCleanupEngine.testConnection(
                                                provider = cloudProvider,
                                                apiKey = cloudApiKey,
                                                model = cloudModel
                                            )
                                            cloudTesting = false
                                            cloudTestResult = if (res.isSuccess) {
                                                val (latency, _) = res.getOrThrow()
                                                "✓ Connected (${latency}ms)"
                                            } else {
                                                "✕ ${res.exceptionOrNull()?.message ?: "Failed"}"
                                            }
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (cloudTesting) "TESTING…" else "TEST CONNECTION",
                                    color = NothingTextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                            }

                            if (cloudTestResult.isNotBlank()) {
                                Text(
                                    text = cloudTestResult,
                                    color = if (cloudTestResult.startsWith("✓")) NothingGreen else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x1AFFFFFF)))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Smart Reminders
                    NothingSettingToggle(
                        icon = Icons.Default.Schedule,
                        title = "Smart Reminders & Scheduling",
                        subtitle = "Detects meetings, calls & deadlines in speech to schedule alerts.",
                        checked = smartRemindersEnabled,
                        onCheckedChange = { checked ->
                            smartRemindersEnabled = checked
                            settings.smartRemindersEnabled = checked
                        }
                    )

                    if (smartRemindersEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))
                        NothingSettingToggle(
                            icon = Icons.Default.Tune,
                            title = "Ask Before Scheduling",
                            subtitle = "Prompts you to confirm detected reminders instead of auto-scheduling alerts.",
                            checked = smartRemindersAskFirst,
                            onCheckedChange = { checked ->
                                smartRemindersAskFirst = checked
                                settings.smartRemindersAskFirst = checked
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        NothingSettingToggle(
                            icon = if (silentAlarmsEnabled) Icons.Default.NotificationsOff else Icons.Default.Alarm,
                            title = "Silent Alarm Mode",
                            subtitle = "Wakes screen and presents alarm overlay persistently with zero sound.",
                            checked = silentAlarmsEnabled,
                            onCheckedChange = { checked ->
                                silentAlarmsEnabled = checked
                                settings.silentAlarmsEnabled = checked
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x1AFFFFFF)))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Formatting Preferences List
                    NothingSettingToggle(
                        icon = Icons.Default.ContentCopy,
                        title = "Auto-Copy to Clipboard",
                        subtitle = "Automatically copies dictation results to clipboard when you stop speaking.",
                        checked = autoCopyClipboard,
                        onCheckedChange = { checked ->
                            autoCopyClipboard = checked
                            settings.autoCopyToClipboard = checked
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    NothingSettingToggle(
                        icon = Icons.Default.Edit,
                        title = "Inline AI Commands (@fix, @email)",
                        subtitle = "Type @fix, @email, @tenglish, @short, or @lilt in any app to transform text in place without switching keyboards.",
                        checked = inlineAiCommandsEnabled,
                        onCheckedChange = { checked ->
                            inlineAiCommandsEnabled = checked
                            settings.inlineAiCommandsEnabled = checked
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    NothingSettingToggle(
                        icon = Icons.Default.FlashOn,
                        title = "Smart App Context Awareness",
                        subtitle = "Adapts tone and structure to active app (Email for Gmail/Outlook, concise chat for WhatsApp/Slack, notes for Keep).",
                        checked = appContextAwarenessEnabled,
                        onCheckedChange = { checked ->
                            appContextAwarenessEnabled = checked
                            settings.appContextAwarenessEnabled = checked
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    NothingSettingToggle(
                        icon = Icons.Default.Numbers,
                        title = "Spoken Numbers to Digits",
                        subtitle = "Converts 'three hundred' to '300'.",
                        checked = spokenNumbers,
                        onCheckedChange = {
                            spokenNumbers = it
                            settings.spokenNumbers = it
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    NothingSettingToggle(
                        icon = Icons.Default.EmojiEmotions,
                        title = "Spoken Emoji",
                        subtitle = "Replaces 'laughing emoji' with '😂'.",
                        checked = spokenEmoji,
                        onCheckedChange = {
                            spokenEmoji = it
                            settings.spokenEmoji = it
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    NothingSettingToggle(
                        icon = Icons.Default.CheckCircle,
                        title = "Auto Capitalize Sentences",
                        subtitle = "Capitalizes sentence starts automatically.",
                        checked = autoCapitalize,
                        onCheckedChange = {
                            autoCapitalize = it
                            settings.autoCapitalize = it
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    NothingSettingToggle(
                        icon = Icons.Default.AttachMoney,
                        title = "Currency & Percent Symbols ($ / %)",
                        subtitle = "Converts spoken 'fifty dollars' to '$50' and 'percent' to '%'.",
                        checked = numberSymbols,
                        onCheckedChange = {
                            numberSymbols = it
                            settings.numberSymbols = it
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    NothingSettingToggle(
                        icon = Icons.Default.Vibration,
                        title = "Tactile Haptic Feedback",
                        subtitle = "Subtle vibration pulse on mic start, stop, and text paste.",
                        checked = hapticFeedback,
                        onCheckedChange = {
                            hapticFeedback = it
                            settings.hapticFeedback = it
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── MASTER CARD 4: SUPPORT & DIAGNOSTICS ──────────────────
            Text(
                text = "SUPPORT & DIAGNOSTICS",
                color = NothingTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NothingCard),
                border = BorderStroke(1.dp, LiltGlassSpecularBorderBrush)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LiltGlassCardBrush)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF261012))
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                tint = Color(0xFFA1A1AA),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "ONE-CLICK ISSUE REPORTING",
                                color = NothingTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Direct dispatch to shashankatthaluri@gmail.com",
                                color = NothingTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Encountering any speech recognition hiccup, formatting bug, or keyboard issue? Tap below to generate and send a complete diagnostic trace with full logs.",
                        color = NothingTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFFD71921), Color(0xFFB51219))))
                                .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(10.dp))
                                .clickable { showErrorDialog = true }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    tint = NothingTextPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "REPORT ISSUE TO DEV",
                                    color = NothingTextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NothingCardElevated)
                                .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    val report = DiagnosticLogger.buildDiagnosticReport(context, "Direct diagnostic copy")
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("Lilt Diagnostics", report))
                                    Toast.makeText(context, "Copied diagnostic logs to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = NothingTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "COPY LOGS",
                                    color = NothingTextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            } // end settings tab
                }
            }

            // Clean Brand Footer
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "L I L T   •   V O I C E ,   R E F I N E D",
                    color = NothingTextMuted,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Raw thought, polished instantly",
                    color = Color(0xFF666666),
                    fontSize = 10.5.sp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }

        // Popping Floating Multi-Select Capsule (Docked bottom-right above navigation pill)
        AnimatedVisibility(
            visible = activeTab == "history" && isMultiSelect && selectedIds.isNotEmpty(),
            enter = scaleIn(spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = scaleOut(spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium)) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 86.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = if (LiltThemeState.isDark) Color(0xF514151C) else Color(0xF8FBFBFD),
                shadowElevation = 24.dp,
                border = BorderStroke(1.2.dp, LiltGlassSpecularBorderBrush)
            ) {
                val pinInteractionSource = remember { MutableInteractionSource() }
                val isPinPressed by pinInteractionSource.collectIsPressedAsState()

                val noteInteractionSource = remember { MutableInteractionSource() }
                val isNotePressed by noteInteractionSource.collectIsPressedAsState()

                val deleteInteractionSource = remember { MutableInteractionSource() }
                val isDeletePressed by deleteInteractionSource.collectIsPressedAsState()

                val cancelInteractionSource = remember { MutableInteractionSource() }
                val isCancelPressed by cancelInteractionSource.collectIsPressedAsState()

                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Minimalist Frosted Badge Counter
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0x18FFFFFF))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp))
                            .padding(horizontal = 11.dp)
                    ) {
                        Text(
                            text = "${selectedIds.size}",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 1. PIN Action (Resting monochrome, ignites Nothing Crimson on click/press)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isPinPressed) Color(0x33D71921) else Color(0x12FFFFFF))
                            .border(1.dp, if (isPinPressed) Color(0xFFD71921) else Color(0x22FFFFFF), CircleShape)
                            .clickable(
                                interactionSource = pinInteractionSource,
                                indication = null
                            ) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                val anyUnpinned = historyItems.any { it.id in selectedIds && !it.isPinned }
                                settings.setPinnedStateMultiple(selectedIds, anyUnpinned)
                                historyItems = settings.getHistory()
                                Toast.makeText(
                                    context,
                                    if (anyUnpinned) "Pinned ${selectedIds.size} items" else "Unpinned ${selectedIds.size} items",
                                    Toast.LENGTH_SHORT
                                ).show()
                                isMultiSelect = false
                                selectedIds = emptySet()
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pin",
                            tint = if (isPinPressed) Color(0xFFD71921) else Color(0xFFA1A1AA),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // 2. NOTE Action (Resting monochrome, ignites Warm Amber on click/press)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isNotePressed) Color(0x33FFB800) else Color(0x12FFFFFF))
                            .border(1.dp, if (isNotePressed) Color(0xFFFFB800) else Color(0x22FFFFFF), CircleShape)
                            .clickable(
                                interactionSource = noteInteractionSource,
                                indication = null
                            ) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                val anyNotNote = historyItems.any { it.id in selectedIds && !it.isNote }
                                settings.setNoteStateMultiple(selectedIds, anyNotNote)
                                historyItems = settings.getHistory()
                                Toast.makeText(
                                    context,
                                    if (anyNotNote) "Moved ${selectedIds.size} to Notes" else "Removed ${selectedIds.size} from Notes",
                                    Toast.LENGTH_SHORT
                                ).show()
                                isMultiSelect = false
                                selectedIds = emptySet()
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Notes",
                            tint = if (isNotePressed) Color(0xFFFFB800) else Color(0xFFA1A1AA),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // 3. DELETE Action (Resting monochrome, ignites Intense Red on click/press)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDeletePressed) Brush.horizontalGradient(listOf(Color(0xFFD71921), Color(0xFFB51219)))
                                else Brush.linearGradient(listOf(Color(0x14FFFFFF), Color(0x12FFFFFF)))
                            )
                            .border(1.dp, if (isDeletePressed) Color(0xFFFFFFFF) else Color(0x22FFFFFF), CircleShape)
                            .clickable(
                                interactionSource = deleteInteractionSource,
                                indication = null
                            ) {
                                coroutineScope.launch {
                                    dissolvingIds = selectedIds
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    delay(600)
                                    settings.deleteMultipleHistory(selectedIds)
                                    historyItems = settings.getHistory()
                                    dissolvingIds = emptySet()
                                    selectedIds = emptySet()
                                    isMultiSelect = false
                                    Toast.makeText(context, "Disintegrated selected items into dust", Toast.LENGTH_SHORT).show()
                                }
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = if (isDeletePressed) Color.White else Color(0xFFA1A1AA),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 4. CANCEL / CLOSE Action (Resting subtle slate, highlights on click/press)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isCancelPressed) Color(0x2EFFFFFF) else Color(0x0EFFFFFF))
                            .border(1.dp, if (isCancelPressed) Color(0x40FFFFFF) else Color(0x16FFFFFF), CircleShape)
                            .clickable(
                                interactionSource = cancelInteractionSource,
                                indication = null
                            ) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                isMultiSelect = false
                                selectedIds = emptySet()
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = if (isCancelPressed) Color.White else Color(0xFF71717A),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Diagnostic & Error Report Dialog
                if (showLanguagePickerDialog) {
                AlertDialog(
                    onDismissRequest = { showLanguagePickerDialog = false },
                    containerColor = if (LiltThemeState.isDark) Color(0xFF141418) else Color(0xFFFBFBFD),
                    titleContentColor = NothingTextPrimary,
                    textContentColor = NothingTextSecondary,
                    title = {
                        Column {
                            Text(
                                text = "Select Your Languages",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = NothingTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Auto Mode restricts recognition & text output strictly to the languages you check below. Say \"Romanized Telugu\" or \"Romanized Hindi\" before speaking to automatically output in Latin script.",
                                fontSize = 12.sp,
                                color = NothingTextMuted
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SettingsManager.ALL_SUPPORTED_LANGUAGES.forEach { lang ->
                                val isChecked = enabledLanguageCodes.contains(lang.code)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isChecked) Color(0x26FF3B1F) else NothingCardElevated)
                                        .border(
                                            1.dp,
                                            if (isChecked) Color(0x88FF3B1F) else NothingBorder,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            val updated = if (isChecked) {
                                                if (enabledLanguageCodes.size > 1) enabledLanguageCodes - lang.code else enabledLanguageCodes
                                            } else {
                                                enabledLanguageCodes + lang.code
                                            }
                                            enabledLanguageCodes = updated
                                            settings.enabledLanguageCodes = updated
                                            if (activeLanguageMode != "auto" && !updated.contains(activeLanguageMode)) {
                                                activeLanguageMode = "auto"
                                                settings.activeLanguageMode = "auto"
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = lang.fullName,
                                            color = NothingTextPrimary,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (lang.isRomanized) "Voice trigger: \"Romanized ${lang.baseLanguageName}\"" else "Whisper ISO: ${lang.whisperIsoCode.uppercase()}",
                                            color = NothingTextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(if (isChecked) Color(0xFFFF3B1F) else Color(0x22FFFFFF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isChecked) {
                                            Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White)
                                .clickable { showLanguagePickerDialog = false }
                                .padding(horizontal = 18.dp, vertical = 8.dp)
                        ) {
                            Text("Done", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                )
            }



    if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                containerColor = NothingCard,
                titleContentColor = Color.White,
                textContentColor = NothingTextSecondary,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = Color(0xFFA1A1AA),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SYSTEM REPORT & SUPPORT",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "Diagnostic reports are directly addressed to:",
                            fontSize = 11.sp,
                            color = NothingTextMuted
                        )
                        Text(
                            text = DiagnosticLogger.DEV_EMAIL,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingTextPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Diagnostic Quick Status Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingCard)
                                .border(1.dp, Color(0xFF222222), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Speech Recognizer", fontSize = 10.sp, color = NothingTextMuted)
                                    Text("Ready (${settings.getRecognitionLanguageTag()})", fontSize = 10.sp, color = NothingGreen, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Direct Auto-Paste", fontSize = 10.sp, color = NothingTextMuted)
                                    Text(
                                        if (LiltAccessibilityService.isServiceRunning()) "Running" else "Not Enabled",
                                        fontSize = 10.sp,
                                        color = if (LiltAccessibilityService.isServiceRunning()) NothingGreen else Color(0xFFFFA000),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Cloud Polish AI", fontSize = 10.sp, color = NothingTextMuted)
                                    Text(
                                        if (settings.cloudAiEnabled && settings.cloudApiKey.isNotBlank()) "${cloudProvider.uppercase()} Ready" else "Off",
                                        fontSize = 10.sp,
                                        color = if (settings.cloudAiEnabled) NothingGreen else NothingTextMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (DiagnosticLogger.lastErrorMessage != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Last Error: ${DiagnosticLogger.lastErrorMessage}",
                                        fontSize = 10.sp,
                                        color = NothingTextPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = errorReportUserNote,
                            onValueChange = { errorReportUserNote = it },
                            label = { Text("What happened? (optional description)", color = NothingTextMuted, fontSize = 11.sp) },
                            textStyle = TextStyle(color = NothingTextPrimary, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color(0xFF333333),
                                focusedContainerColor = Color(0xFF080808),
                                unfocusedContainerColor = Color(0xFF080808)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                        )
                    }
                },
                confirmButton = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFFD71921), Color(0xFFB51219))))
                            .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(8.dp))
                            .clickable {
                                DiagnosticLogger.sendErrorReport(context, errorReportUserNote)
                                showErrorDialog = false
                                errorReportUserNote = ""
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "SEND EMAIL TO DEV",
                            color = NothingTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingCardElevated)
                                .clickable {
                                    val report = DiagnosticLogger.buildDiagnosticReport(context, errorReportUserNote)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("Lilt Diagnostics", report))
                                    Toast.makeText(context, "Full diagnostic log copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text("COPY LOGS", color = NothingTextPrimary, fontSize = 11.sp)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingCard)
                                .clickable { showErrorDialog = false }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text("CANCEL", color = NothingTextMuted, fontSize = 11.sp)
                        }
                    }
                }
            )
        }
        // Clear History Confirmation & Scope Dialog
        if (showClearHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showClearHistoryDialog = false },
                containerColor = if (LiltThemeState.isDark) Color(0xFF141418) else Color(0xFFFBFBFD),
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x26D71921))
                                .border(1.dp, Color(0x66D71921), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = Color(0xFFD71921),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Clear History",
                                color = NothingTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Select how much history to wipe out",
                                color = NothingTextMuted,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("Last 30 Minutes", "Wipes dictations recorded in the past 30 min", 30L * 60 * 1000),
                            Triple("Last 1 Hour", "Wipes dictations recorded in the past hour", 60L * 60 * 1000),
                            Triple("Last 24 Hours", "Wipes all dictations from today", 24L * 60 * 60 * 1000),
                            Triple("All Time (Complete Wipe)", "Permanently deletes all voice history & notes", -1L)
                        ).forEach { (label, desc, durationMs) ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (durationMs == -1L) Color(0xFF221316) else NothingCardElevated)
                                    .border(
                                        1.dp,
                                        if (durationMs == -1L) Color(0x66D71921) else Color(0x22FFFFFF),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                        if (durationMs == -1L) {
                                            settings.clearHistory()
                                            historyItems = emptyList()
                                            Toast.makeText(context, "All history wiped out", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val cutoff = System.currentTimeMillis() - durationMs
                                            settings.clearHistorySince(cutoff)
                                            historyItems = settings.getHistory()
                                            Toast.makeText(context, "Cleared $label", Toast.LENGTH_SHORT).show()
                                        }
                                        showClearHistoryDialog = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = label,
                                        color = if (durationMs == -1L) Color(0xFFD71921) else NothingTextPrimary,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = desc,
                                        color = NothingTextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(NothingCardElevated)
                            .clickable { showClearHistoryDialog = false }
                            .padding(horizontal = 16.dp)
                    ) {
                        Text("CANCEL", color = NothingTextMuted, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }

        } // end LiquidGlassAmbientBackdrop Box
    }
}

/**
 * 4-Layer Liquid Glass Ambient Mesh Backdrop:
 * Renders slow-breathing refractive light orbs behind all translucent cards
 * ("Obsidian Aurora" in Dark Mode, "Alabaster Pearl Quartz" in Light Mode).
 */
@Composable
fun LiquidGlassAmbientBackdrop(isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_mesh_drift")
    val driftX by infiniteTransition.animateFloat(
        initialValue = -0.06f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(tween(9000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "meshDriftX"
    )
    val driftY by infiniteTransition.animateFloat(
        initialValue = -0.05f,
        targetValue = 0.07f,
        animationSpec = infiniteRepeatable(tween(11000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "meshDriftY"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NothingBlack)
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val orb1Color = if (isDark) Color(0x22FF3B1F) else Color(0x24FF6B4A)
            val orb2Color = if (isDark) Color(0x1E6366F1) else Color(0x2438BDF8)
            val orb3Color = if (isDark) Color(0x18FFB547) else Color(0x1CA855F7)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orb1Color, Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(w * (0.82f + driftX), h * (0.14f + driftY)),
                    radius = w * 0.72f
                ),
                radius = w * 0.72f,
                center = androidx.compose.ui.geometry.Offset(w * (0.82f + driftX), h * (0.14f + driftY))
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orb2Color, Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(w * (0.18f - driftX), h * (0.48f - driftY)),
                    radius = w * 0.78f
                ),
                radius = w * 0.78f,
                center = androidx.compose.ui.geometry.Offset(w * (0.18f - driftX), h * (0.48f - driftY))
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orb3Color, Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(w * (0.75f + driftY), h * (0.84f - driftX)),
                    radius = w * 0.68f
                ),
                radius = w * 0.68f,
                center = androidx.compose.ui.geometry.Offset(w * (0.75f + driftY), h * (0.84f - driftX))
            )
        }
    }
}

@Composable
fun NothingSettingToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NothingCardElevated)
                    .border(1.dp, NothingBorderSubtle, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFFD71921),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    color = NothingTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    color = NothingTextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFD71921),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = if (LiltThemeState.isDark) Color(0xFF2C2C2E) else Color(0xFFD4D4D8)
            )
        )
    }
}

@Composable
fun TonePill(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = LiltThemeState.isDark

    val textColorAnim by animateColorAsState(
        targetValue = if (isActive) Color.White else NothingTextSecondary,
        animationSpec = tween(220),
        label = "tone_pill_text_color"
    )

    val dotScale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.68f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tone_pill_dot"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(
                if (isActive) {
                    Brush.verticalGradient(
                        if (LiltThemeState.isDark) listOf(Color(0xFF282C3A), Color(0xFF141620)) else listOf(Color(0xFFFFFFFF), Color(0xFFF2F0EA))
                    )
                } else if (isDark) {
                    Brush.verticalGradient(
                        listOf(Color(0xCC161822), Color(0xB30E1017))
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color(0xF0FFFFFF), Color(0xD9F4F3EF))
                    )
                }
            )
            .border(
                width = 1.2.dp,
                brush = if (isActive) {
                    Brush.verticalGradient(listOf(Color(0x99FFFFFF), Color(0x66FFFFFF)))
                } else {
                    Brush.verticalGradient(listOf(Color(0x40FFFFFF), Color(0x25FFFFFF)))
                },
                shape = RoundedCornerShape(19.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (dotScale > 0.05f) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = dotScale
                            scaleY = dotScale
                            alpha = dotScale
                        }
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD71921))
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                color = textColorAnim,
                fontSize = 11.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 0.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun ReminderCard(
    reminder: com.lilt.mobile.data.ReminderItem,
    searchQuery: String = "",
    isDissolving: Boolean = false,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val isPast = reminder.eventEpochMs <= System.currentTimeMillis()
    val isCompleted = reminder.isCompleted
    val timeFormat = java.text.SimpleDateFormat("EEE, d MMM • h:mm a", java.util.Locale.getDefault())
    val formattedTime = timeFormat.format(java.util.Date(reminder.eventEpochMs))
    val leadStr = if (reminder.leadMinutes >= 60) "${reminder.leadMinutes / 60}h" else "${reminder.leadMinutes}m"

    val dustAnim by animateFloatAsState(
        targetValue = if (isDissolving) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "dustDisintegrationReminder"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                if (isDissolving) {
                    alpha = ((1f - dustAnim) * (1f - dustAnim)).coerceIn(0f, 1f)
                    scaleX = 1f + dustAnim * 0.16f
                    scaleY = (1f - dustAnim * 0.85f).coerceAtLeast(0.05f)
                    translationX = dustAnim * 90f
                    translationY = -dustAnim * 30f
                    rotationZ = dustAnim * 6f
                }
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) NothingCard else NothingCardElevated
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    LiltGlassSpecularBorderBrush,
                    RoundedCornerShape(18.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCompleted) Color(0x2234C759)
                                else if (isPast) Color(0x22FF9500)
                                else Color(0x22D71921)
                            )
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Default.Check else Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (isCompleted) Color(0xFF34C759)
                            else if (isPast) Color(0xFFFF9500)
                            else Color(0xFFD71921),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        val highlightedTitle = rememberHighlightedText(reminder.title, searchQuery)
                        Text(
                            text = highlightedTitle,
                            color = if (isCompleted) Color(0xFF8E8E93) else Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formattedTime,
                            color = if (isCompleted) Color(0xFF636366) else Color(0xFFA1A1AA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isCompleted) Color(0x2234C759)
                    else if (isPast) Color(0x22FF9500)
                    else Color(0x22D71921)
                ) {
                    Text(
                        text = if (isCompleted) "DONE" else if (isPast) "PAST" else "${leadStr} ALERT",
                        color = if (isCompleted) Color(0xFF34C759)
                        else if (isPast) Color(0xFFFF9500)
                        else Color(0xFFE50914),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isCompleted) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NothingCardElevated)
                            .clickable { onComplete() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Complete",
                                tint = Color(0xFF34C759),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Mark Done",
                                color = NothingTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(NothingCardElevated)
                        .clickable { onDelete() }
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ContactActionChip(
    name: String,
    phone: String,
    isSaved: Boolean,
    onSaveClick: () -> Unit
) {
    val view = LocalView.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NothingCard)
            .border(1.dp, if (isSaved) Color(0x3310B981) else Color(0x2EFFFFFF), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isSaved) Color(0x2610B981) else NothingCardElevated)
                        .border(1.dp, if (isSaved) Color(0x4D10B981) else Color(0x33FFFFFF), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Check else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isSaved) Color(0xFF10B981) else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "CONTACT",
                            color = if (isSaved) Color(0xFF10B981) else Color(0xFF71717A),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "•",
                            color = Color(0xFF52525B),
                            fontSize = 9.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = name,
                            color = NothingTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = phone,
                        color = Color(0xFFA1A1AA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (!isSaved) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onSaveClick()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Save",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0x1A10B981))
                        .border(1.dp, Color(0x4D10B981), RoundedCornerShape(18.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Saved",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceNoteCard(
    item: DictationItem,
    searchQuery: String = "",
    isMultiSelect: Boolean = false,
    isSelected: Boolean = false,
    isDissolving: Boolean = false,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    onTogglePinTop: () -> Unit = {},
    onSelectToggle: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onSaveContact: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val view = LocalView.current

    val dustAnim by animateFloatAsState(
        targetValue = if (isDissolving) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "dustDisintegrationVN"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                if (isDissolving) {
                    alpha = ((1f - dustAnim) * (1f - dustAnim)).coerceIn(0f, 1f)
                    scaleX = 1f + dustAnim * 0.16f // bursting outwards into dust particles
                    scaleY = (1f - dustAnim * 0.85f).coerceAtLeast(0.05f) // collapsing atom structure
                    translationX = dustAnim * 90f // wind blowing particles away
                    translationY = -dustAnim * 30f // ash lifting into air
                    rotationZ = dustAnim * 6f // flutter
                }
            }
            .pointerInput(isMultiSelect) {
                detectTapGestures(
                    onLongPress = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onLongClick()
                    },
                    onTap = {
                        if (isMultiSelect) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onSelectToggle()
                        }
                    }
                )
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) NothingCardElevated else NothingCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (isSelected) Color(0x99FFB800) else if (item.isPinned) Color(0x44D71921) else if (item.isNote) Color(0x33FFB800) else NothingBorder, RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Apple-style selection check circle
                    if (isMultiSelect) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFFFFB800) else Color.Transparent)
                                .border(1.5.dp, if (isSelected) Color(0xFFFFB800) else Color(0xFF52525B), CircleShape)
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    onSelectToggle()
                                }
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.Black,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (item.isPinned) Color(0xFFD71921) else if (item.isNote) Color(0xFFFFB800) else Color.White)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.tone.uppercase(),
                        color = if (item.isNote) Color(0xFFFFB800) else NothingTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val wordCount = item.cleanedText.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                    Text(
                        text = "• $wordCount words",
                        color = NothingTextMuted,
                        fontSize = 10.sp
                    )
                }

                if (!isMultiSelect) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Pin to top button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(NothingCardElevated)
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    onTogglePinTop()
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = if (item.isPinned) "Unpin from top" else "Pin to top",
                                tint = if (item.isPinned) Color(0xFFD71921) else Color(0xFFA1A1AA),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        // Bookmark / Pin icon
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(NothingCardElevated)
                                .clickable { onTogglePin() }
                        ) {
                            Icon(
                                imageVector = if (item.isNote) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (item.isNote) "Unpin note" else "Pin as note",
                                tint = if (item.isNote) Color(0xFFFFB800) else Color(0xFFA1A1AA),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        // Share icon
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(NothingCardElevated)
                                .clickable {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, item.cleanedText)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Voice Note").apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    })
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = NothingTextPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        // Copy icon
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(NothingCardElevated)
                                .clickable { onCopy() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = NothingTextPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        // Delete icon
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(NothingCardElevated)
                                .clickable { onDelete() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = Color(0xFF888888),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val highlightedVNText = rememberHighlightedText(item.cleanedText, searchQuery)
            Text(
                text = highlightedVNText,
                color = NothingTextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal
            )

            if (!item.contactPhone.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                ContactActionChip(
                    name = item.contactName ?: "Contact",
                    phone = item.contactPhone,
                    isSaved = item.isContactSaved,
                    onSaveClick = {
                        val intent = Intent(Intent.ACTION_INSERT).apply {
                            type = ContactsContract.RawContacts.CONTENT_TYPE
                            putExtra(ContactsContract.Intents.Insert.NAME, item.contactName ?: "")
                            putExtra(ContactsContract.Intents.Insert.PHONE, item.contactPhone)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            context.startActivity(intent)
                            onSaveContact?.invoke()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Unable to open contacts app", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun rememberHighlightedText(text: String, query: String): androidx.compose.ui.text.AnnotatedString {
    val q = query.trim()
    return remember(text, q) {
        if (q.isEmpty() || !text.contains(q, ignoreCase = true)) {
            androidx.compose.ui.text.AnnotatedString(text)
        } else {
            buildAnnotatedString {
                var start = 0
                while (start < text.length) {
                    val idx = text.indexOf(q, start, ignoreCase = true)
                    if (idx < 0) {
                        append(text.substring(start))
                        break
                    }
                    if (idx > start) {
                        append(text.substring(start, idx))
                    }
                    val end = idx + q.length
                    withStyle(
                        SpanStyle(
                            background = Color(0x77FFB800),
                            color = Color(0xFFFFE082),
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(text.substring(idx, end))
                    }
                    start = end
                }
            }
        }
    }
}

fun isBulletLine(line: String): Boolean {
    val trimmed = line.trim()
    return trimmed.startsWith("• ") || trimmed.startsWith("- ") || trimmed.startsWith("* ") ||
           trimmed.startsWith("• ~") || trimmed.startsWith("- ~") || trimmed.startsWith("* ~")
}

@Composable
fun NoteCard(
    item: DictationItem,
    searchQuery: String = "",
    isMultiSelect: Boolean = false,
    isSelected: Boolean = false,
    isDissolving: Boolean = false,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    onTogglePinTop: () -> Unit = {},
    onUpdateText: (String) -> Unit,
    onSelectToggle: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onSaveContact: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    var isEditing by remember { mutableStateOf(false) }
    var editedText by remember(item.cleanedText) { mutableStateOf(item.cleanedText) }

    val dustAnim by animateFloatAsState(
        targetValue = if (isDissolving) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "dustDisintegrationNote"
    )

    val timeFormatted = remember(item.timestamp) {
        val now = System.currentTimeMillis()
        val diff = now - item.timestamp
        if (diff < 60_000) "Just now"
        else if (diff < 3600_000) "${diff / 60_000}m ago"
        else if (diff < 86400_000) "Today, " + java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date(item.timestamp))
        else java.text.SimpleDateFormat("d MMM, h:mm a", java.util.Locale.getDefault()).format(java.util.Date(item.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                if (isDissolving) {
                    alpha = ((1f - dustAnim) * (1f - dustAnim)).coerceIn(0f, 1f)
                    scaleX = 1f + dustAnim * 0.16f // bursting outwards into dust particles
                    scaleY = (1f - dustAnim * 0.85f).coerceAtLeast(0.05f) // collapsing atom structure
                    translationX = dustAnim * 90f // wind blowing particles away
                    translationY = -dustAnim * 30f // ash lifting into air
                    rotationZ = dustAnim * 6f // flutter
                }
            }
            .pointerInput(isMultiSelect) {
                detectTapGestures(
                    onLongPress = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onLongClick()
                    },
                    onTap = {
                        if (isMultiSelect) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onSelectToggle()
                        }
                    }
                )
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) NothingCardElevated else NothingCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (isSelected) Color(0x99FFB800) else Color(0x33FFB800), RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isMultiSelect) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFFFFB800) else Color.Transparent)
                                .border(1.5.dp, if (isSelected) Color(0xFFFFB800) else Color(0xFF52525B), CircleShape)
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    onSelectToggle()
                                }
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.Black,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (item.isPinned) Color(0xFFD71921) else Color(0xFFFFB800))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.tone.uppercase(),
                        color = Color(0xFFFFB800),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• $timeFormatted",
                        color = Color(0xFFA1A1AA),
                        fontSize = 11.sp
                    )
                }

                if (!isMultiSelect) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Pin to top button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(NothingCardElevated)
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    onTogglePinTop()
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = if (item.isPinned) "Unpin from top" else "Pin to top",
                                tint = if (item.isPinned) Color(0xFFD71921) else Color(0xFFA1A1AA),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        // Edit / Done toggle icon
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isEditing) Color(0x33FFB800) else NothingCardElevated)
                                .border(1.dp, if (isEditing) Color(0xFFFFB800) else Color.Transparent, CircleShape)
                            .clickable {
                                if (isEditing) {
                                    onUpdateText(editedText)
                                    isEditing = false
                                } else {
                                    editedText = item.cleanedText
                                    isEditing = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = if (isEditing) "Save note" else "Edit note",
                                tint = if (isEditing) Color(0xFFFFB800) else Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        if (isEditing) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(NothingCardElevated)
                                    .clickable {
                                        editedText = item.cleanedText
                                        isEditing = false
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel edit",
                                    tint = Color(0xFFA1A1AA),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        // Unpin / Bookmark icon
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E1A14))
                                .border(1.dp, Color(0x4DFFB800), CircleShape)
                                .clickable { onTogglePin() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "Unpin note",
                                tint = Color(0xFFFFB800),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        // Share icon
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(NothingCardElevated)
                                .clickable {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, item.cleanedText.replace("~", ""))
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Note").apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    })
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = NothingTextPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        // Copy icon
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(NothingCardElevated)
                                .clickable { onCopy() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = NothingTextPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        // Delete icon
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(NothingCardElevated)
                                .clickable { onDelete() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = Color(0xFF888888),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isEditing) {
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    textStyle = TextStyle(color = NothingTextPrimary, fontSize = 14.sp, lineHeight = 21.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFFB800),
                        unfocusedBorderColor = Color(0x66FFB800),
                        focusedContainerColor = NothingCardElevated,
                        unfocusedContainerColor = NothingCardElevated
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFB800))
                            .clickable {
                                onUpdateText(editedText)
                                isEditing = false
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "SAVE",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                val lines = item.cleanedText.split("\\n")
                val hasBullets = lines.any { isBulletLine(it) }

                if (hasBullets) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        lines.forEachIndexed { lineIndex, line ->
                            if (isBulletLine(line)) {
                                val isStruck = line.contains("~")
                                val rawContent = line.replace("~", "").trim()
                                val bulletSymbol = if (rawContent.startsWith("• ")) "• "
                                    else if (rawContent.startsWith("- ")) "- "
                                    else if (rawContent.startsWith("* ")) "* "
                                    else ""
                                val itemText = if (bulletSymbol.isNotEmpty()) rawContent.removePrefix(bulletSymbol).trim() else rawContent

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                            val newLines = lines.toMutableList()
                                            if (isStruck) {
                                                newLines[lineIndex] = line.replace("~", "")
                                            } else {
                                                val prefix = if (line.startsWith("• ")) "• " else if (line.startsWith("- ")) "- " else if (line.startsWith("* ")) "* " else ""
                                                val contentOnly = line.removePrefix(prefix).trim()
                                                newLines[lineIndex] = "${prefix}~${contentOnly}~"
                                            }
                                            onUpdateText(newLines.joinToString("\\n"))
                                        }
                                        .padding(vertical = 3.dp, horizontal = 2.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(19.dp)
                                            .clip(CircleShape)
                                            .background(if (isStruck) Color(0xFFFFB800) else NothingCardElevated)
                                            .border(1.5.dp, if (isStruck) Color(0xFFFFB800) else Color(0xFF52525B), CircleShape)
                                    ) {
                                        if (isStruck) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Completed",
                                                tint = Color.Black,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    val highlightedItemText = rememberHighlightedText(itemText, searchQuery)
                                    Text(
                                        text = highlightedItemText,
                                        color = if (isStruck) NothingTextMuted else NothingTextPrimary,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        textDecoration = if (isStruck) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                }
                            } else if (line.isNotBlank()) {
                                Text(
                                    text = line,
                                    color = NothingTextPrimary,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                } else {
                    val highlightedNoteText = rememberHighlightedText(item.cleanedText, searchQuery)
                    Text(
                        text = highlightedNoteText,
                        color = NothingTextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            if (!item.contactPhone.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                ContactActionChip(
                    name = item.contactName ?: "Contact",
                    phone = item.contactPhone,
                    isSaved = item.isContactSaved,
                    onSaveClick = {
                        val intent = Intent(Intent.ACTION_INSERT).apply {
                            type = ContactsContract.RawContacts.CONTENT_TYPE
                            putExtra(ContactsContract.Intents.Insert.NAME, item.contactName ?: "")
                            putExtra(ContactsContract.Intents.Insert.PHONE, item.contactPhone)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            context.startActivity(intent)
                            onSaveContact?.invoke()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Unable to open contacts app", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}
