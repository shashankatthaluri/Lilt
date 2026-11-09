package com.lilt.mobile.ime

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.lilt.mobile.data.DiagnosticLogger
import com.lilt.mobile.data.SettingsManager
import com.lilt.mobile.engine.SpeechEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LiltInputMethodService : InputMethodService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var speechEngine: SpeechEngine
    private lateinit var settings: SettingsManager
    private var vibrator: Vibrator? = null

    // UI state
    private val isRecordingState = mutableStateOf(false)
    private val statusTextState = mutableStateOf("Tap mic to speak")
    private val partialTextState = mutableStateOf("")
    private val audioRmsState = mutableFloatStateOf(0f)

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        settings = SettingsManager.getInstance(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        speechEngine = SpeechEngine(this).apply {
            listener = object : SpeechEngine.Listener {
                override fun onReady() {
                    isRecordingState.value = true
                    statusTextState.value = "Listening…"
                    partialTextState.value = ""
                }

                override fun onBeginningOfSpeech() {
                    statusTextState.value = "Listening…"
                }

                override fun onRmsChanged(rmsdB: Float) {
                    audioRmsState.floatValue = rmsdB
                }

                override fun onPartialResult(partialText: String) {
                    partialTextState.value = partialText
                }

                override fun onEndOfSpeech() {
                    statusTextState.value = "Transcribing…"
                }

                override fun onFinalResult(cleanedText: String, rawText: String) {
                    isRecordingState.value = false
                    statusTextState.value = "Tap mic to speak"
                    partialTextState.value = ""
                    triggerHaptic(50)

                    // Commit cleaned text directly to the focused input field
                    currentInputConnection?.commitText(cleanedText, 1)
                }

                override fun onNoSpeechDetected() {
                    isRecordingState.value = false
                    statusTextState.value = "Tap mic to speak"
                    partialTextState.value = ""
                    triggerHaptic(20)
                }

                override fun onError(error: String) {
                    isRecordingState.value = false
                    statusTextState.value = error
                    DiagnosticLogger.logError("LiltIME", error)
                    triggerHaptic(80)
                }

                override fun onStopped() {
                    isRecordingState.value = false
                }
            }
        }
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@LiltInputMethodService)
            setViewTreeSavedStateRegistryOwner(this@LiltInputMethodService)

            setContent {
                MaterialTheme {
                    VoiceKeyboardContent(
                        isRecording = isRecordingState.value,
                        statusText = statusTextState.value,
                        partialText = partialTextState.value,
                        audioRms = audioRmsState.floatValue,
                        onToggleRecord = { toggleRecord() },
                        onStartHoldRecord = { startHoldRecord() },
                        onStopHoldRecord = { stopHoldRecord() },
                        onInsertSpace = {
                            triggerHaptic(20)
                            currentInputConnection?.commitText(" ", 1)
                        },
                        onBackspace = {
                            triggerHaptic(20)
                            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
                        },
                        onEnter = {
                            triggerHaptic(30)
                            val editorInfo = currentInputEditorInfo
                            if (editorInfo != null && (editorInfo.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0) {
                                val action = editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION
                                if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                                    currentInputConnection?.performEditorAction(action)
                                    return@VoiceKeyboardContent
                                }
                            }
                            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                        },
                        onSwitchKeyboard = {
                            triggerHaptic(20)
                            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                            imm?.showInputMethodPicker()
                        }
                    )
                }
            }
        }
        return composeView
    }

    private fun toggleRecord() {
        triggerHaptic(40)
        if (speechEngine.isListening) {
            speechEngine.stopListening()
        } else {
            speechEngine.startListening()
        }
    }

    private fun startHoldRecord() {
        triggerHaptic(45)
        if (!speechEngine.isListening) {
            speechEngine.startListening()
        }
    }

    private fun stopHoldRecord() {
        triggerHaptic(30)
        if (speechEngine.isListening) {
            speechEngine.stopListening()
        }
    }

    private fun triggerHaptic(durationMs: Long) {
        if (!settings.hapticFeedback) return
        try {
            vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        speechEngine.stopListening()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }
}

@Composable
fun VoiceKeyboardContent(
    isRecording: Boolean,
    statusText: String,
    partialText: String,
    audioRms: Float,
    onToggleRecord: () -> Unit,
    onStartHoldRecord: () -> Unit,
    onStopHoldRecord: () -> Unit,
    onInsertSpace: () -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSwitchKeyboard: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Animated waveform bars for voice input
    val wBar1 by infiniteTransition.animateFloat(
        initialValue = 8f, targetValue = if (isRecording) 24f else 8f,
        animationSpec = infiniteRepeatable(tween(240, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "w1"
    )
    val wBar2 by infiniteTransition.animateFloat(
        initialValue = 12f, targetValue = if (isRecording) 32f else 12f,
        animationSpec = infiniteRepeatable(tween(320, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "w2"
    )
    val wBar3 by infiniteTransition.animateFloat(
        initialValue = 6f, targetValue = if (isRecording) 20f else 6f,
        animationSpec = infiniteRepeatable(tween(280, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "w3"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(270.dp),
        color = Color(0xFF000000)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Status Bar & Partial Transcript Preview
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) Color.White else Color(0xFF555555))
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = statusText.uppercase(),
                        color = if (isRecording) Color.White else Color(0xFF888888),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )

                    if (isRecording) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Box(modifier = Modifier.width(2.dp).height(wBar1.dp).clip(RoundedCornerShape(1.dp)).background(Color.White))
                            Box(modifier = Modifier.width(2.dp).height(wBar2.dp).clip(RoundedCornerShape(1.dp)).background(Color.White))
                            Box(modifier = Modifier.width(2.dp).height(wBar3.dp).clip(RoundedCornerShape(1.dp)).background(Color.White))
                        }
                    }
                }

                if (partialText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = partialText,
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Center: The Hero Voice Dictation Capsule Button (Nothing Red Glow)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(90.dp)
            ) {
                // Outer glow ripple when recording
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .size((82 * pulseScale).dp)
                            .clip(CircleShape)
                            .background(Color(0x33D71921))
                    )
                }

                // Main capsule button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRecording) {
                                Brush.linearGradient(
                                    listOf(Color.White, Color(0xFFE4E4E7))
                                )
                            } else {
                                Brush.linearGradient(
                                    listOf(Color(0xFF1E1E1E), Color(0xFF141414))
                                )
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (isRecording) Color(0xFFFF4D4D) else Color(0xFF2C2C2C),
                            shape = CircleShape
                        )
                        .clickable { onToggleRecord() }
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Voice Dictation",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            // Bottom Action Bar: Switch Keyboard, Space, Backspace, Enter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Switch Keyboard Button
                IconButton(
                    onClick = onSwitchKeyboard,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF141414))
                        .border(1.dp, Color(0xFF242424), RoundedCornerShape(14.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Switch Keyboard",
                        tint = Color(0xFFAAAAAA)
                    )
                }

                // Space Key: Hold to Speak (Push-to-Talk) + Tap for Space
                val coroutineScope = rememberCoroutineScope()
                var isSpacePressed by remember { mutableStateOf(false) }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .padding(horizontal = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isRecording || isSpacePressed) {
                                Brush.horizontalGradient(listOf(Color(0xFF2A1012), Color(0xFF1C0D0E)))
                            } else {
                                Brush.horizontalGradient(listOf(Color(0xFF161616), Color(0xFF131313)))
                            }
                        )
                        .border(
                            1.dp,
                            if (isRecording || isSpacePressed) Color.White else Color(0xFF262626),
                            RoundedCornerShape(14.dp)
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isSpacePressed = true
                                    val holdJob = coroutineScope.launch {
                                        delay(220)
                                        // Hold detected: start listening
                                        onStartHoldRecord()
                                    }
                                    tryAwaitRelease()
                                    isSpacePressed = false
                                    if (holdJob.isActive) {
                                        // Released before 220ms -> standard Space tap
                                        holdJob.cancel()
                                        onInsertSpace()
                                    } else {
                                        // Released after holding -> stop listening and commit text
                                        onStopHoldRecord()
                                    }
                                }
                            )
                        }
                ) {
                    if (isRecording) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "RELEASE TO INSERT",
                                color = Color(0xFFFF4D4D),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SpaceBar,
                                contentDescription = "Space",
                                tint = Color(0xFFCCCCCC),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Hold to Speak • Space",
                                color = Color(0xFFCCCCCC),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Backspace Key
                IconButton(
                    onClick = onBackspace,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF141414))
                        .border(1.dp, Color(0xFF242424), RoundedCornerShape(14.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = Color(0xFFAAAAAA)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Enter Key (Nothing Red Accent)
                IconButton(
                    onClick = onEnter,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                        contentDescription = "Enter",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
