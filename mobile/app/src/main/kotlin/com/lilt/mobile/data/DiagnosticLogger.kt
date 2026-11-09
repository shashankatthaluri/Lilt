package com.lilt.mobile.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.lilt.mobile.accessibility.LiltAccessibilityService
import com.lilt.mobile.core.LiltCore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque

data class DiagnosticLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val level: String,
    val message: String
) {
    fun format(): String {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        return "[${sdf.format(Date(timestamp))}] [$level] [$tag] $message"
    }
}

object DiagnosticLogger {
    private const val TAG = "DiagnosticLogger"
    private const val MAX_LOGS = 100
    const val DEV_EMAIL = "shashankatthaluri@gmail.com"

    private val logs = ConcurrentLinkedDeque<DiagnosticLogEntry>()
    var lastErrorMessage: String? = null
        private set
    var lastErrorTime: Long = 0L
        private set

    fun log(tag: String, message: String, level: String = "INFO") {
        Log.println(
            when (level) {
                "ERROR" -> Log.ERROR
                "WARN" -> Log.WARN
                "DEBUG" -> Log.DEBUG
                else -> Log.INFO
            },
            tag,
            message
        )
        logs.add(DiagnosticLogEntry(tag = tag, level = level, message = message))
        while (logs.size > MAX_LOGS) {
            logs.pollFirst()
        }
    }

    fun logError(tag: String, error: String, throwable: Throwable? = null) {
        lastErrorMessage = error
        lastErrorTime = System.currentTimeMillis()
        val fullMsg = if (throwable != null) {
            "$error\nStackTrace: ${Log.getStackTraceString(throwable)}"
        } else {
            error
        }
        log(tag, fullMsg, "ERROR")
    }

    fun getRecentLogs(count: Int = 30): List<DiagnosticLogEntry> {
        return logs.toList().takeLast(count)
    }

    fun buildDiagnosticReport(context: Context, userNote: String = "", specificError: String? = null): String {
        val settings = SettingsManager.getInstance(context)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US)
        val now = sdf.format(Date())
        val errToReport = specificError ?: lastErrorMessage ?: "No uncaught errors recorded (Diagnostics Check)"

        val sb = StringBuilder()
        sb.appendLine("==================================================")
        sb.appendLine("         LILT MOBILE DIAGNOSTIC & ERROR REPORT    ")
        sb.appendLine("==================================================")
        sb.appendLine("Report Generated : $now")
        sb.appendLine("Target Developer : $DEV_EMAIL")
        sb.appendLine()

        if (userNote.isNotBlank()) {
            sb.appendLine("--- USER NOTES / INCIDENT CONTEXT ---")
            sb.appendLine(userNote.trim())
            sb.appendLine()
        }

        sb.appendLine("--- ERROR SUMMARY ---")
        sb.appendLine("Summary          : $errToReport")
        if (lastErrorTime > 0) {
            sb.appendLine("Error Timestamp  : ${sdf.format(Date(lastErrorTime))}")
        }
        sb.appendLine()

        sb.appendLine("--- DEVICE & OS ENVIRONMENT ---")
        sb.appendLine("Manufacturer     : ${Build.MANUFACTURER}")
        sb.appendLine("Model            : ${Build.MODEL} (${Build.DEVICE})")
        sb.appendLine("Android Version  : ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        sb.appendLine("Supported ABIs   : ${Build.SUPPORTED_ABIS.joinToString(", ")}")
        sb.appendLine("App Version      : Lilt v1.0.0 (Release-Engine)")
        sb.appendLine("Rust Native Core : ${if (LiltCore.isNativeLoaded()) "ARM64 Loaded (OK)" else "Fallback Mode"}")
        sb.appendLine()

        sb.appendLine("--- CURRENT ENGINE SETTINGS ---")
        sb.appendLine("Language Mode    : ${settings.activeLanguageMode}")
        sb.appendLine("Recognition Tag  : ${settings.getRecognitionLanguageTag()}")
        sb.appendLine("Tone Preset      : ${settings.activeTone}")
        sb.appendLine("Cleanup Level    : ${settings.cleanupLevel}")
        sb.appendLine("Cloud AI Active  : ${settings.cloudAiEnabled}")
        sb.appendLine("Cloud Provider   : ${settings.cloudProvider}")
        sb.appendLine("Cloud Model      : ${settings.cloudModel}")
        sb.appendLine("API Key Status   : ${if (settings.cloudApiKey.isNotBlank()) "Configured (${settings.cloudApiKey.take(7)}...)" else "EMPTY"}")
        sb.appendLine("Accessibility Svc: ${if (LiltAccessibilityService.isServiceRunning()) "CONNECTED & RUNNING" else "DISCONNECTED"}")
        sb.appendLine("Floating Pill    : ${if (settings.floatingPillEnabled) "ENABLED" else "DISABLED"}")
        sb.appendLine("Spoken Numbers   : ${settings.spokenNumbers}")
        sb.appendLine("Spoken Emoji     : ${settings.spokenEmoji}")
        sb.appendLine("Auto-Capitalize  : ${settings.autoCapitalize}")
        sb.appendLine("Words Processed  : ${settings.wordsProcessed}")
        sb.appendLine("Fillers Cleaned  : ${settings.hesitationsRemoved}")
        sb.appendLine()

        sb.appendLine("--- RECENT EVENT LOGS (Last ${logs.size.coerceAtMost(30)}) ---")
        val recent = getRecentLogs(30)
        if (recent.isEmpty()) {
            sb.appendLine("No logs recorded yet.")
        } else {
            recent.forEach { entry ->
                sb.appendLine(entry.format())
            }
        }
        sb.appendLine("==================================================")
        return sb.toString()
    }

    /**
     * One-Click Error Report Dispatcher:
     * 1. Copies full structured diagnostics to clipboard.
     * 2. Launches email intent targeting shashankatthaluri@gmail.com with subject and detailed body.
     */
    fun sendErrorReport(context: Context, userNote: String = "", specificError: String? = null) {
        val report = buildDiagnosticReport(context, userNote, specificError)
        val errSummary = specificError ?: lastErrorMessage ?: "App Diagnostics"
        val subject = "[Lilt Mobile Error Report] $errSummary - ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"

        // Copy to clipboard first so user has immediate access
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("Lilt Diagnostic Report", report))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy diagnostic report to clipboard: ${e.message}")
        }

        // Prepare email intent
        val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$DEV_EMAIL")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(DEV_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, report)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(Intent.createChooser(mailIntent, "Send Error Report to Dev").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            Toast.makeText(context, "Opening email to $DEV_EMAIL (Report copied to clipboard)", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            // Fallback to generic ACTION_SEND if ACTION_SENDTO cannot resolve
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(DEV_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, report)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(Intent.createChooser(fallbackIntent, "Send Error Report").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (e2: Exception) {
                Toast.makeText(context, "Diagnostics copied to clipboard. Please email to $DEV_EMAIL", Toast.LENGTH_LONG).show()
            }
        }
    }
}
