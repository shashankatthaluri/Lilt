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
                event.source?.let { source ->
                    if (source.isEditable && !event.isPassword) {
                        lastFocusedNode = source
                    }
                }
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

        // Universal keyboard detection across Samsung One UI, Pixel Gboard, SwiftKey, Xiaomi HyperOS, OnePlus/Oppo/Vivo, and standard TYPE_INPUT_METHOD
        try {
            val windowList = windows
            val screenHeight = resources.displayMetrics.heightPixels
            for (w in windowList) {
                val pkgName = w.root?.packageName?.toString()?.lowercase() ?: ""
                val isKnownImePkg = KNOWN_KEYBOARD_PACKAGES.any { pkgName.contains(it) }
                if (w.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD || isKnownImePkg) {
                    val rect = Rect()
                    w.getBoundsInScreen(rect)
                    // Supports standard docked keyboards and floating/split keyboards (>100px height)
                    if (rect.height() > 100 && rect.top < screenHeight && rect.bottom >= (screenHeight * 0.45f).toInt()) {
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
        lastFocusedNode = null
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

        private val KNOWN_KEYBOARD_PACKAGES = listOf(
            "com.google.android.inputmethod",     // Gboard (Pixel, Nothing, Motorola, Xiaomi)
            "com.samsung.android.honeyboard",     // Samsung Keyboard (One UI)
            "com.sec.android.inputmethod",        // Legacy Samsung Keyboard
            "com.touchtype.swiftkey",             // Microsoft SwiftKey (OnePlus, Samsung, Windows Link)
            "com.miui.securityinputmethod",       // Xiaomi HyperOS / MIUI Security Keyboard
            "com.facemoji.lite.xiaomi",           // Xiaomi Mint / Facemoji Keyboard
            "com.baidu.input_mi",                 // Xiaomi IME
            "com.sohu.inputmethod.sogou.xiaomi",  // Xiaomi Sogou IME
            "com.oplus.keyboard",                 // OnePlus / Oppo ColorOS Keyboard
            "com.coloros.oshare",
            "com.vivo.aiime",                     // Vivo Jovi / FuntouchOS IME
            "org.futo.inputmethod",               // FUTO Keyboard
            "dev.patrickgold.florisboard",        // FlorisBoard
            "rkr.simplekeyboard.inputmethod",     // Simple Keyboard
            "com.syntellia.fleksy"                // Fleksy
        )

        @Volatile
        var instance: LiltAccessibilityService? = null
            private set

        @Volatile
        private var lastFocusedNode: AccessibilityNodeInfo? = null

        fun isServiceRunning(): Boolean = instance != null

        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }

        /**
         * Directly pastes or injects text into the active focused input field in any app.
         * Searches across active window, all application windows, and fallback cached nodes.
         */
        fun autoPasteText(text: String): Boolean {
            val service = instance
            if (service == null) {
                DiagnosticLogger.logError(TAG, "autoPasteText: Accessibility service not running")
                return false
            }
            return try {
                // Collect candidate window root nodes (active window + any application windows)
                val candidateRoots = mutableListOf<AccessibilityNodeInfo>()
                try {
                    service.rootInActiveWindow?.let { candidateRoots.add(it) }
                } catch (_: Exception) {}

                try {
                    for (w in service.windows) {
                        if (w.type == AccessibilityWindowInfo.TYPE_APPLICATION || w.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                            w.root?.let { r ->
                                if (!candidateRoots.any { it == r }) {
                                    candidateRoots.add(r)
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}

                var targetNode: AccessibilityNodeInfo? = null

                // 1. Check for standard INPUT focus across candidate roots
                for (root in candidateRoots) {
                    targetNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                    if (targetNode != null && targetNode.isEditable) break
                }

                // 2. Check for focused/selected editable within hierarchy
                if (targetNode == null) {
                    for (root in candidateRoots) {
                        targetNode = findFocusedEditable(root)
                        if (targetNode != null) break
                    }
                }

                // 3. Fallback to cached last focused editable node
                if (targetNode == null) {
                    try {
                        val cached = lastFocusedNode
                        if (cached != null && cached.refresh() && cached.isVisibleToUser && cached.isEditable) {
                            targetNode = cached
                        }
                    } catch (_: Exception) {}
                }

                // 4. Fallback to any visible editable in the application window
                if (targetNode == null) {
                    for (root in candidateRoots) {
                        targetNode = findAnyEditable(root)
                        if (targetNode != null) break
                    }
                }

                if (targetNode == null) {
                    DiagnosticLogger.logError(TAG, "autoPasteText: no focused or visible editable found in windows")
                    return false
                }

                // Method 1: ACTION_PASTE (Respects app's native paste formatting & triggers IME listeners)
                var success = targetNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)

                // Method 2: Fallback to ACTION_SET_TEXT if paste action isn't handled or returned false
                if (!success) {
                    val existing = targetNode.text?.toString() ?: ""
                    val textToInject = if (existing.isNotBlank()) "$existing $text" else text
                    val arguments = Bundle().apply {
                        putCharSequence(
                            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                            textToInject
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

        private fun findAnyEditable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            if (node.isEditable && node.isVisibleToUser) {
                return node
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                val match = findAnyEditable(child)
                if (match != null) return match
            }
            return null
        }
    }
}
