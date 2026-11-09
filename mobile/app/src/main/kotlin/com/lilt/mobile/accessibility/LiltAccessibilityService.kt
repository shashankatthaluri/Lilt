package com.lilt.mobile.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.lilt.mobile.data.DiagnosticLogger
import com.lilt.mobile.data.SettingsManager
import com.lilt.mobile.overlay.LiltOverlayService

class LiltAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val recheckRunnable = Runnable { checkKeyboardVisibility() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Lilt Accessibility Service Connected")
        DiagnosticLogger.log(TAG, "Accessibility Service Connected and Running")
        checkKeyboardVisibility()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val settings = SettingsManager.getInstance(applicationContext)
        if (!settings.floatingPillEnabled) return

        val pkg = event.packageName?.toString()?.lowercase() ?: ""
        if (pkg.isNotEmpty() && !pkg.contains("com.lilt")) {
            val isBanking = isBankingPackage(pkg)
            LiltOverlayService.instance?.setBankingSuppressed(isBanking)
            if (isBanking) return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                checkKeyboardVisibility()
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                checkKeyboardVisibility()
                handler.removeCallbacks(recheckRunnable)
                handler.postDelayed(recheckRunnable, 180)
                handler.postDelayed(recheckRunnable, 380)
            }
        }
    }

    private fun checkKeyboardVisibility() {
        var isKeyboardShown = false
        var keyboardTop = -1

        // Inspect active system windows strictly for the soft keyboard IME window
        try {
            val windowList = windows
            val screenHeight = resources.displayMetrics.heightPixels
            for (w in windowList) {
                if (w.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                    val rect = Rect()
                    w.getBoundsInScreen(rect)
                    // The soft keyboard must be on-screen with valid height (>120px)
                    if (rect.height() > 120 && rect.top < screenHeight && rect.bottom >= (screenHeight * 0.6f).toInt()) {
                        isKeyboardShown = true
                        keyboardTop = rect.top
                        break
                    }
                }
            }
        } catch (e: Exception) {
            DiagnosticLogger.logError(TAG, "Error checking windows: ${e.message}")
        }

        DiagnosticLogger.log(TAG, "checkKeyboardVisibility: isKeyboardShown=$isKeyboardShown, keyboardTop=$keyboardTop")

        if (isKeyboardShown) {
            if (LiltOverlayService.instance != null) {
                LiltOverlayService.onKeyboardVisibilityChanged(true, keyboardTop)
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
                LiltOverlayService.start(applicationContext, isContextualShow = true)
            }
        } else {
            LiltOverlayService.onKeyboardVisibilityChanged(false, -1)
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Lilt Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        if (instance == this) {
            instance = null
        }
        super.onDestroy()
    }

    private fun isBankingPackage(pkg: String): Boolean {
        val lower = pkg.lowercase()
        return lower.contains("supermoney") || lower.contains("npci") || lower.contains("bhim") ||
               lower.contains("phonepe") || lower.contains("paytm") || lower.contains("paisa") ||
               lower.contains("cred") || lower.contains("mobikwik") || lower.contains("freecharge") ||
               lower.contains("jupiter") || lower.contains("slice") || lower.contains("fi.money") ||
               (lower.contains("bank") && !lower.contains("powerbank"))
    }

    companion object {
        private const val TAG = "LiltAccessibility"

        @Volatile
        var instance: LiltAccessibilityService? = null
            private set

        fun isServiceRunning(): Boolean = instance != null

        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }

        /**
         * Directly pastes or injects text into the active focused input field in any app.
         */
        fun autoPasteText(text: String): Boolean {
            val service = instance
            if (service == null) {
                DiagnosticLogger.logError(TAG, "autoPasteText: Accessibility service not running")
                return false
            }
            return try {
                val root = service.rootInActiveWindow ?: run {
                    DiagnosticLogger.logError(TAG, "autoPasteText: rootInActiveWindow is null")
                    return false
                }
                val targetNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                    ?: findFocusedEditable(root)
                    ?: run {
                        DiagnosticLogger.logError(TAG, "autoPasteText: no focused editable found in active window")
                        return false
                    }

                // Method 1: ACTION_PASTE (Respects app's native paste formatting)
                var success = targetNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)

                // Method 2: Fallback to ACTION_SET_TEXT if paste action isn't handled
                if (!success) {
                    val arguments = Bundle().apply {
                        putCharSequence(
                            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                            text
                        )
                    }
                    success = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                }

                DiagnosticLogger.log(TAG, "autoPasteText injected ${text.length} chars. Result: $success")
                success
            } catch (e: Exception) {
                DiagnosticLogger.logError(TAG, "Error auto-pasting text: ${e.message}", e)
                false
            }
        }

        private fun findFocusedEditable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            if (node.isEditable && (node.isFocused || node.isSelected)) {
                return node
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                val match = findFocusedEditable(child)
                if (match != null) return match
            }
            return null
        }
    }
}
