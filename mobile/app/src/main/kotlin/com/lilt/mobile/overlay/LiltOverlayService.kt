package com.lilt.mobile.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.lilt.mobile.MainActivity
import com.lilt.mobile.R
import com.lilt.mobile.accessibility.LiltAccessibilityService
import com.lilt.mobile.data.DiagnosticLogger
import com.lilt.mobile.data.SettingsManager
import com.lilt.mobile.engine.SpeechEngine

enum class CircleState {
    IDLE,         // White dot
    LISTENING,    // Ruby Red dot (pulsing with mic audio)
    TRANSCRIBING, // Amber / Yellow dot (orbiting sweep)
    SUCCESS,      // Emerald Green dot (auto-pasted / copied)
    NO_SPEECH     // Muted Grey dot (short recording / no speech detected)
}

class LiltOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private lateinit var speechEngine: SpeechEngine
    private lateinit var settings: SettingsManager
    private var vibrator: Vibrator? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // UI States
    private val circleState = mutableStateOf(CircleState.IDLE)
    private val isVisibleState = mutableStateOf(false)
    private val audioRmsState = mutableFloatStateOf(0f)
    @Volatile private var isBankingSuppressed = false

    fun setBankingSuppressed(suppressed: Boolean) {
        if (isBankingSuppressed == suppressed) return
        isBankingSuppressed = suppressed
        mainHandler.post {
            if (suppressed) {
                hideCircleGracefully()
            } else if (!settings.floatingCircleContextualOnly && settings.floatingPillEnabled) {
                showCircleGracefully()
            }
        }
    }

    private var hideRunnable: Runnable? = null
    private var isUserDragging = false
    private var slideAnimator: ValueAnimator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        settings = SettingsManager.getInstance(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager

        speechEngine = SpeechEngine(this).apply {
            listener = object : SpeechEngine.Listener {
                override fun onReady() {
                    circleState.value = CircleState.LISTENING
                    triggerHaptic(40)
                }

                override fun onBeginningOfSpeech() {
                    circleState.value = CircleState.LISTENING
                }

                override fun onRmsChanged(rmsdB: Float) {
                    audioRmsState.floatValue = rmsdB
                }

                override fun onPartialResult(partialText: String) {
                    if (circleState.value != CircleState.LISTENING) {
                        circleState.value = CircleState.LISTENING
                    }
                }

                override fun onEndOfSpeech() {
                    circleState.value = CircleState.TRANSCRIBING
                }

                override fun onFinalResult(cleanedText: String, rawText: String) {
                    circleState.value = CircleState.SUCCESS
                    triggerHaptic(60)

                    val textToDeliver = cleanedText.ifBlank { rawText }
                    if (textToDeliver.isBlank()) {
                        DiagnosticLogger.log("LiltOverlayService", "onFinalResult received blank text")
                        mainHandler.postDelayed({
                            circleState.value = CircleState.IDLE
                        }, 500)
                        return
                    }

                    // 1. Copy to system clipboard immediately
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = ClipData.newPlainText("Lilt Voice Dictation", textToDeliver)
                    clipboard?.setPrimaryClip(clip)

                    // 2. Direct Auto-Paste via Accessibility Service with slight dispatch delay for clipboard sync
                    mainHandler.postDelayed({
                        val didAutoPaste = LiltAccessibilityService.autoPasteText(textToDeliver)
                        DiagnosticLogger.log("LiltOverlayService", "Auto-paste result: $didAutoPaste")
                        if (!didAutoPaste) {
                            Toast.makeText(applicationContext, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    }, 50)

                    // 3. Keep Green success dot for 1.2s, then smoothly fade out
                    mainHandler.postDelayed({
                        if (settings.floatingCircleContextualOnly) {
                            hideCircleGracefully()
                        }
                        circleState.value = CircleState.IDLE
                    }, 1200)
                }

                override fun onNoSpeechDetected() {
                    DiagnosticLogger.log("LiltOverlayService", "No speech detected - transitioning to subtle grey dot")
                    circleState.value = CircleState.NO_SPEECH
                    triggerHaptic(20) // Subtle tick

                    mainHandler.postDelayed({
                        circleState.value = CircleState.IDLE
                        if (settings.floatingCircleContextualOnly) {
                            hideCircleGracefully()
                        }
                    }, 500)
                }

                override fun onError(error: String) {
                    DiagnosticLogger.logError("LiltOverlayService", "Dictation error: $error")
                    circleState.value = CircleState.IDLE
                    triggerHaptic(80)
                    Toast.makeText(applicationContext, "Lilt: $error", Toast.LENGTH_SHORT).show()

                    if (settings.floatingCircleContextualOnly) {
                        mainHandler.postDelayed({
                            hideCircleGracefully()
                        }, 1200)
                    }
                }

                override fun onStopped() {
                    if (circleState.value == CircleState.LISTENING) {
                        circleState.value = CircleState.TRANSCRIBING
                    }
                }
            }
        }

        startForegroundServiceWithNotification()
        createFloatingCircleView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val isContextual = intent?.getBooleanExtra(EXTRA_CONTEXTUAL_SHOW, false) ?: false
        if (isContextual) {
            handleKeyboardVisibility(true, -1)
        }
        return START_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "lilt_floating_pill_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.overlay_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Lilt Floating Liquid Glass Circle"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Lilt Active")
            .setContentText("Voice dictation active when typing")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createFloatingCircleView() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val displayMetrics = resources.displayMetrics
        val density = displayMetrics.density
        val circleDiameterPx = (56 * density).toInt()

        val savedY = settings.floatingCircleY
        val defaultY = if (savedY != -1 && savedY <= (displayMetrics.heightPixels * 0.65f)) savedY else (displayMetrics.heightPixels * 0.52f).toInt()
        val isDockedLeft = settings.floatingCircleDockLeft
        val restingX = if (isDockedLeft) (16 * density).toInt() else displayMetrics.widthPixels - circleDiameterPx - (16 * density).toInt()

        // When contextual-only, initially park off-screen to the docked edge
        val initialX = if (settings.floatingCircleContextualOnly) {
            if (isDockedLeft) -circleDiameterPx else displayMetrics.widthPixels
        } else {
            restingX
        }

        val params = WindowManager.LayoutParams(
            circleDiameterPx,
            circleDiameterPx,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = defaultY
        }
        layoutParams = params

        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@LiltOverlayService)
            setViewTreeSavedStateRegistryOwner(this@LiltOverlayService)

            // Start invisible until keyboard appears (unless in Always-On mode)
            if (settings.floatingCircleContextualOnly) {
                visibility = View.GONE
                isVisibleState.value = false
            } else {
                visibility = View.VISIBLE
                isVisibleState.value = true
            }

            var initialTouchX = 0f
            var initialTouchY = 0f
            var dragStartX = 0
            var dragStartY = 0
            var isPushToTalk = false
            var touchDownTime = 0L
            var lastTapTime = 0L
            val longPressTimeout = 450L

            val holdToTalkRunnable = Runnable {
                if (!isUserDragging) {
                    isPushToTalk = true
                    triggerHaptic(50)
                    if (!speechEngine.isListening) {
                        speechEngine.startListening()
                    }
                }
            }

            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        slideAnimator?.cancel()
                        slideAnimator = null
                        dragStartX = params.x
                        dragStartY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isUserDragging = false
                        isPushToTalk = false
                        touchDownTime = System.currentTimeMillis()

                        // Start hold timer for push-to-talk (450ms)
                        mainHandler.postDelayed(holdToTalkRunnable, longPressTimeout)
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (dx * dx + dy * dy > touchSlop * touchSlop) {
                            mainHandler.removeCallbacks(holdToTalkRunnable)
                            if (!isPushToTalk) {
                                isUserDragging = true
                                params.x = dragStartX + dx
                                params.y = dragStartY + dy
                                try {
                                    windowManager?.updateViewLayout(this, params)
                                } catch (_: Exception) {}
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        mainHandler.removeCallbacks(holdToTalkRunnable)
                        val now = System.currentTimeMillis()
                        val pressDuration = now - touchDownTime

                        if (isPushToTalk) {
                            isPushToTalk = false
                            if (pressDuration >= longPressTimeout) {
                                // Sustained hold release: finish dictation and process
                                triggerHaptic(30)
                                if (speechEngine.isListening) {
                                    speechEngine.stopListening()
                                }
                            } else {
                                // Touch transitioned near threshold: keep recording in toggle mode
                                triggerHaptic(40)
                            }
                        } else if (!isUserDragging) {
                            // Normal deliberate tap: toggle recording with debounce protection
                            if (now - lastTapTime > 180) {
                                lastTapTime = now
                                toggleRecord()
                            }
                        } else {
                            // Drag released: gentle magnetic snap to nearest edge
                            val screenWidth = resources.displayMetrics.widthPixels
                            val isLeft = (params.x + circleDiameterPx / 2) < (screenWidth / 2)
                            val targetSnapX = if (isLeft) {
                                (16 * density).toInt()
                            } else {
                                screenWidth - circleDiameterPx - (16 * density).toInt()
                            }
                            settings.floatingCircleDockLeft = isLeft
                            settings.floatingCircleY = params.y

                            animateSnapX(params.x, targetSnapX)
                        }
                        isUserDragging = false
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        mainHandler.removeCallbacks(holdToTalkRunnable)
                        if (isPushToTalk) {
                            isPushToTalk = false
                            if (speechEngine.isListening) {
                                speechEngine.stopListening()
                            }
                        }
                        isUserDragging = false
                        true
                    }
                    else -> false
                }
            }

            setContent {
                MaterialTheme {
                    LiquidGlassCircle(
                        state = circleState.value,
                        audioRms = audioRmsState.floatValue,
                        isVisible = isVisibleState.value
                    )
                }
            }
        }

        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            DiagnosticLogger.logError("LiltOverlayService", "addView error: ${e.message}", e)
        }
    }

    private fun animateSnapX(fromX: Int, toX: Int) {
        slideAnimator?.cancel()
        slideAnimator = ValueAnimator.ofInt(fromX, toX).apply {
            duration = 200
            interpolator = DecelerateInterpolator(1.6f)
            addUpdateListener { anim ->
                val curX = anim.animatedValue as Int
                layoutParams?.x = curX
                try {
                    overlayView?.let { windowManager?.updateViewLayout(it, layoutParams) }
                } catch (_: Exception) {}
            }
            start()
        }
    }

    private fun showCircleGracefully() {
        val density = resources.displayMetrics.density
        val screenWidth = resources.displayMetrics.widthPixels
        val circlePx = (56 * density).toInt()

        val isDockedLeft = settings.floatingCircleDockLeft
        val targetX = if (isDockedLeft) {
            (16 * density).toInt()
        } else {
            screenWidth - circlePx - (16 * density).toInt()
        }

        if (isBankingSuppressed) return
        if (isVisibleState.value && overlayView?.visibility == View.VISIBLE && layoutParams?.x == targetX && slideAnimator?.isRunning != true) {
            return
        }

        var targetY = settings.floatingCircleY
        if (targetY == -1 || targetY > (resources.displayMetrics.heightPixels * 0.65f)) {
            targetY = (resources.displayMetrics.heightPixels * 0.52f).toInt()
        }

        val startX = layoutParams?.x?.takeIf { it != targetX } ?: (if (isDockedLeft) -circlePx else screenWidth)

        slideAnimator?.cancel()

        layoutParams?.x = startX
        layoutParams?.y = targetY

        overlayView?.visibility = View.VISIBLE
        isVisibleState.value = true

        try {
            overlayView?.let { windowManager?.updateViewLayout(it, layoutParams) }
        } catch (_: Exception) {}

        slideAnimator = ValueAnimator.ofInt(startX, targetX).apply {
            duration = 260
            interpolator = DecelerateInterpolator(1.8f)
            addUpdateListener { anim ->
                val curX = anim.animatedValue as Int
                layoutParams?.x = curX
                try {
                    overlayView?.let { windowManager?.updateViewLayout(it, layoutParams) }
                } catch (_: Exception) {}
            }
            start()
        }
    }

    private fun hideCircleGracefully() {
        if (!isVisibleState.value && overlayView?.visibility == View.GONE) {
            return
        }

        val density = resources.displayMetrics.density
        val screenWidth = resources.displayMetrics.widthPixels
        val circlePx = (56 * density).toInt()

        val isDockedLeft = settings.floatingCircleDockLeft
        val endX = if (isDockedLeft) -circlePx else screenWidth
        val currentX = layoutParams?.x ?: (if (isDockedLeft) (16 * density).toInt() else screenWidth - circlePx - (16 * density).toInt())

        slideAnimator?.cancel()
        isVisibleState.value = false // Triggers Compose fadeOut + scaleOut

        slideAnimator = ValueAnimator.ofInt(currentX, endX).apply {
            duration = 240
            interpolator = AccelerateInterpolator(1.6f)
            addUpdateListener { anim ->
                val curX = anim.animatedValue as Int
                layoutParams?.x = curX
                try {
                    overlayView?.let { windowManager?.updateViewLayout(it, layoutParams) }
                } catch (_: Exception) {}
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!isVisibleState.value) {
                        overlayView?.visibility = View.GONE
                    }
                }
            })
            start()
        }
    }

    private fun toggleRecord() {
        triggerHaptic(40)
        when (circleState.value) {
            CircleState.IDLE, CircleState.SUCCESS, CircleState.NO_SPEECH -> {
                speechEngine.startListening()
            }
            CircleState.LISTENING -> {
                speechEngine.stopListening()
            }
            CircleState.TRANSCRIBING -> {
                // Currently processing with Groq Whisper/Qwen
            }
        }
    }

    fun handleKeyboardVisibility(isShown: Boolean, keyboardTopPx: Int) {
        if (!settings.floatingPillEnabled) return

        // In Always-On mode, keep the circle persistently visible
        if (!settings.floatingCircleContextualOnly) {
            if (!isVisibleState.value) {
                mainHandler.post {
                    showCircleGracefully()
                }
            }
            return
        }

        mainHandler.post {
            if (isShown) {
                // Cancel pending auto-hide
                hideRunnable?.let { mainHandler.removeCallbacks(it) }
                hideRunnable = null

                if (!isVisibleState.value || overlayView?.visibility != View.VISIBLE) {
                    showCircleGracefully()
                }
            } else {
                // If currently listening or transcribing, keep visible until finished!
                if (circleState.value == CircleState.LISTENING || circleState.value == CircleState.TRANSCRIBING) {
                    return@post
                }

                // Smoothly dismiss toward edge when keyboard disappears
                hideRunnable?.let { mainHandler.removeCallbacks(it) }
                hideRunnable = Runnable {
                    if (circleState.value != CircleState.LISTENING && circleState.value != CircleState.TRANSCRIBING) {
                        hideCircleGracefully()
                    }
                }
                mainHandler.postDelayed(hideRunnable!!, 150)
            }
        }
    }

    fun updateAlwaysOnMode(isAlwaysOn: Boolean) {
        settings.floatingCircleContextualOnly = !isAlwaysOn
        mainHandler.post {
            if (isAlwaysOn) {
                hideRunnable?.let { mainHandler.removeCallbacks(it) }
                hideRunnable = null
                showCircleGracefully()
            } else {
                if (circleState.value != CircleState.LISTENING && circleState.value != CircleState.TRANSCRIBING) {
                    hideCircleGracefully()
                }
            }
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
        slideAnimator?.cancel()
        slideAnimator = null
        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
        }
        if (instance == this) {
            instance = null
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 2001
        private const val EXTRA_CONTEXTUAL_SHOW = "extra_contextual_show"

        @Volatile
        var instance: LiltOverlayService? = null
            private set

        fun start(context: Context, isContextualShow: Boolean = false) {
            val intent = Intent(context, LiltOverlayService::class.java).apply {
                putExtra(EXTRA_CONTEXTUAL_SHOW, isContextualShow)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LiltOverlayService::class.java)
            context.stopService(intent)
        }

        fun onKeyboardVisibilityChanged(isShown: Boolean, keyboardTopPx: Int) {
            instance?.handleKeyboardVisibility(isShown, keyboardTopPx)
        }

        fun setAlwaysOn(isAlwaysOn: Boolean) {
            instance?.updateAlwaysOnMode(isAlwaysOn)
        }
    }
}

/**
 * Liquid Glass Floating Circle with Ultra-Smooth Spring Physics Animation
 * - Appearance: Springs gently upward with expanding scale & opacity
 * - Disappearance: Slides downward and smoothly collapses
 * - 4 Iconic Colored Dots:
 *   * White: Idle / Ready
 *   * Ruby Red: Listening (pulses with live speech dB)
 *   * Amber / Yellow: Transcribing (orbiting halo)
 *   * Emerald Green: Success (auto-pasted)
 */
@Composable
fun LiquidGlassCircle(
    state: CircleState,
    audioRms: Float,
    isVisible: Boolean
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) +
                scaleIn(
                    animationSpec = spring(
                        dampingRatio = 0.72f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    initialScale = 0.45f
                ),
        exit = fadeOut(animationSpec = tween(220, easing = FastOutLinearInEasing)) +
                scaleOut(
                    animationSpec = tween(220, easing = FastOutLinearInEasing),
                    targetScale = 0.45f
                )
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "liquid_glass_fx")

        // Subtle idle breath for White dot
        val idleBreath by infiniteTransition.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "idleBreath"
        )

        // Listening pulse ripple for Red dot
        val rippleScale by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.6f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearOutSlowInEasing), RepeatMode.Restart),
            label = "rippleScale"
        )
        val rippleAlpha by infiniteTransition.animateFloat(
            initialValue = 0.7f,
            targetValue = 0.0f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearOutSlowInEasing), RepeatMode.Restart),
            label = "rippleAlpha"
        )

        // Transcribing rotating sweep for Amber / Yellow dot
        val rotationAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
            label = "rotationAngle"
        )

        // Success gentle ping for Green dot
        val successPing by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "successPing"
        )

        // 1. The Liquid Glass Outer Circular Container
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x66242630), // Translucent liquid glass core
                            Color(0x880C0D12)  // Frosted dark glass perimeter
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0x40FFFFFF),
                            Color(0x10FFFFFF),
                            Color(0x28FFFFFF)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                CircleState.IDLE -> {
                    // Iconic White Dot with calm luxury breath
                    Box(
                        modifier = Modifier
                            .size((12 * idleBreath).dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }

                CircleState.LISTENING -> {
                    // Pulsing Outer Ripple
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer {
                                scaleX = rippleScale
                                scaleY = rippleScale
                                alpha = rippleAlpha
                            }
                            .border(1.5.dp, Color(0xFFFF2A2A), CircleShape)
                    )

                    // Ruby Red Dot reacting dynamically to audio dB
                    val audioBoost = (audioRms.coerceIn(0f, 30f) / 6f).dp
                    Box(
                        modifier = Modifier
                            .size(13.dp + audioBoost)
                            .clip(CircleShape)
                            .background(Color(0xFFFF2A2A))
                    )
                }

                CircleState.TRANSCRIBING -> {
                    // Rotating Amber / Yellow Halo
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer { rotationZ = rotationAngle }
                            .border(
                                width = 1.5.dp,
                                brush = Brush.sweepGradient(
                                    listOf(
                                        Color(0x00FFB800),
                                        Color(0x44FFB800),
                                        Color(0xFFFFB800)
                                    )
                                ),
                                shape = CircleShape
                            )
                    )

                    // Amber / Yellow Center Dot
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFB800))
                    )
                }

                CircleState.SUCCESS -> {
                    // Emerald Green Dot with success pulse
                    Box(
                        modifier = Modifier
                            .size((13 * successPing).dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676))
                    )
                }

                CircleState.NO_SPEECH -> {
                    // Subtle Muted Grey Dot for zero-speech / short tap
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF71717A))
                    )
                }
            }
        }
    }
}
