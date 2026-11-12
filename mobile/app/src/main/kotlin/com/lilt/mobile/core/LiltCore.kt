package com.lilt.mobile.core

import android.util.Log

object LiltCore {
    private const val TAG = "LiltCore"
    private var isLoaded = false

    init {
        try {
            System.loadLibrary("lilt_core")
            isLoaded = true
            Log.i(TAG, "LiltCore native library loaded successfully.")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to load lilt_core native library: ${t.message}")
            isLoaded = false
        }
    }

    fun isNativeLoaded(): Boolean = isLoaded

    @JvmStatic
    external fun cleanText(
        rawText: String,
        cleanupLevel: String,
        numbersAsDigits: Boolean,
        numberSymbols: Boolean,
        spokenEmoji: Boolean,
        autoCapitalize: Boolean,
        trailingSpace: Boolean
    ): String

    @JvmStatic
    external fun applyReplacements(
        text: String,
        replacementsJson: String
    ): String

    @JvmStatic
    external fun expandSnippets(
        text: String,
        snippetsJson: String
    ): String

    /**
     * Clean and format text with tone formatting, smart voice commands, and graceful fallback.
     */
    fun processText(
        raw: String,
        cleanupLevel: String = "medium",
        numbersAsDigits: Boolean = true,
        numberSymbols: Boolean = true,
        spokenEmoji: Boolean = true,
        autoCapitalize: Boolean = true,
        trailingSpace: Boolean = true,
        tone: String = "natural"
    ): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""

        val lower = trimmed.lowercase()

        // 1. Raw / Verbatim Mode Command: "write it raw", "write raw", "raw text", "verbatim"
        val rawPrefix = listOf("write it raw", "write raw", "raw text", "verbatim")
            .firstOrNull { lower.startsWith(it) }
        if (rawPrefix != null) {
            val content = trimmed.substring(rawPrefix.length).trimStart(' ', ',', ':', '-')
            return formatRawPunctuation(content, autoCapitalize, trailingSpace)
        }

        // 2. Email Mode Command: "write the email", "write an email", "draft an email", "compose an email", "email to"
        val emailPrefix = listOf("write the email", "write an email", "draft an email", "compose an email", "email to")
            .firstOrNull { lower.startsWith(it) }
        var isEmailMode = emailPrefix != null
        var emailRecipient: String? = null
        var inputToProcess = trimmed

        if (emailPrefix != null) {
            var stripped = trimmed.substring(emailPrefix.length).trimStart(' ', ',', ':', '-')
            val toMatch = Regex("(?i)^to\\s+([a-zA-Z]+)(?:\\s+(?:saying|that|telling|about|regarding)?\\s+|\\s*,\\s*)").find(stripped)
            if (toMatch != null) {
                emailRecipient = toMatch.groupValues[1].replaceFirstChar { it.uppercaseChar() }
                stripped = stripped.substring(toMatch.range.last + 1).trim()
            } else if (emailPrefix == "email to") {
                val parts = stripped.split(" ", limit = 2)
                emailRecipient = parts.getOrNull(0)?.replaceFirstChar { it.uppercaseChar() }
                stripped = parts.getOrNull(1) ?: ""
            }
            inputToProcess = stripped
        }

        // 3. Bullet Points Command: "make a list", "bullet points", "bullet this"
        val bulletPrefix = listOf("make a list", "bullet points", "bullet this", "write as bullet points")
            .firstOrNull { lower.startsWith(it) }
        val effectiveTone = if (bulletPrefix != null) {
            inputToProcess = trimmed.substring(bulletPrefix.length).trimStart(' ', ',', ':', '-')
            "bullets"
        } else tone

        var cleaned = if (isLoaded) {
            try {
                cleanText(
                    rawText = inputToProcess,
                    cleanupLevel = cleanupLevel,
                    numbersAsDigits = numbersAsDigits,
                    numberSymbols = numberSymbols,
                    spokenEmoji = spokenEmoji,
                    autoCapitalize = autoCapitalize,
                    trailingSpace = trailingSpace
                )
            } catch (t: Throwable) {
                Log.e(TAG, "Error running native cleanText: ${t.message}", t)
                fallbackClean(inputToProcess, autoCapitalize, trailingSpace)
            }
        } else {
            fallbackClean(inputToProcess, autoCapitalize, trailingSpace)
        }

        // Apply Tone Presets
        cleaned = when (effectiveTone) {
            "bullets" -> {
                val lines = cleaned.split(Regex("[.;\n]|(?<=\\w)\\s+and\\s+also\\s+|(?<=\\w)\\s+next\\s+"))
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                if (lines.size > 1) {
                    lines.joinToString("\n") { "• " + it.replaceFirstChar { c -> c.uppercaseChar() } }
                } else {
                    "• " + cleaned.trim().replaceFirstChar { c -> c.uppercaseChar() }
                }
            }
            "pro" -> {
                var proText = cleaned.trim()
                if (autoCapitalize && proText.isNotEmpty()) {
                    proText = proText.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
                if (!proText.endsWith(".") && !proText.endsWith("!") && !proText.endsWith("?")) {
                    proText = "$proText."
                }
                if (trailingSpace) "$proText " else proText
            }
            "casual" -> {
                cleaned.trim()
            }
            else -> cleaned
        }

        // If Email Mode was triggered, structure into email format with clean professional spacing
        if (isEmailMode) {
            val recipientName = emailRecipient ?: "[Recipient Name]"
            val subjectTitle = when {
                cleaned.contains("demo", ignoreCase = true) && cleaned.contains("postponed", ignoreCase = true) -> "Demo Postponed Update"
                cleaned.contains("meeting", ignoreCase = true) -> "Meeting Update"
                else -> "[Subject Line]"
            }
            val subjectLine = "Subject: $subjectTitle"
            cleaned = "$subjectLine\n\nHi $recipientName,\n\n${cleaned.trim()}\n\nBest regards,\n[Your Name]"
        }

        return cleaned
    }

    private fun formatRawPunctuation(
        raw: String,
        autoCapitalize: Boolean,
        trailingSpace: Boolean
    ): String {
        var text = raw.trim()
        if (autoCapitalize && text.isNotEmpty()) {
            text = text.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        if (!text.endsWith(".") && !text.endsWith("!") && !text.endsWith("?")) {
            text = "$text."
        }
        if (trailingSpace && !text.endsWith(" ")) {
            text = "$text "
        }
        return text
    }

    private fun fallbackClean(
        raw: String,
        autoCapitalize: Boolean,
        trailingSpace: Boolean
    ): String {
        var text = raw.trim()
        // Common filler cleanup in fallback
        text = text.replace(Regex("(?i)\\b(um|uh|erm|ah)\\b[,\\s]*"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (autoCapitalize && text.isNotEmpty()) {
            text = text.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        if (trailingSpace && !text.endsWith(" ")) {
            text = "$text "
        }
        return text
    }
}
