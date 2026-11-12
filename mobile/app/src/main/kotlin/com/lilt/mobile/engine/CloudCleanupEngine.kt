package com.lilt.mobile.engine

import com.lilt.mobile.data.DiagnosticLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder

object CloudCleanupEngine {

    fun encodePcmToWav(
        pcmData: ByteArray,
        sampleRate: Int = 16000,
        channels: Int = 1,
        bitsPerSample: Int = 16
    ): ByteArray {
        val totalAudioLen = pcmData.size
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()

        val header = ByteArray(44)
        val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        bb.put("RIFF".toByteArray(Charsets.US_ASCII))
        bb.putInt(totalDataLen)
        bb.put("WAVE".toByteArray(Charsets.US_ASCII))
        bb.put("fmt ".toByteArray(Charsets.US_ASCII))
        bb.putInt(16) // Subchunk1Size for PCM
        bb.putShort(1.toShort()) // AudioFormat 1 = PCM
        bb.putShort(channels.toShort())
        bb.putInt(sampleRate)
        bb.putInt(byteRate)
        bb.putShort(blockAlign)
        bb.putShort(bitsPerSample.toShort())
        bb.put("data".toByteArray(Charsets.US_ASCII))
        bb.putInt(totalAudioLen)

        val wav = ByteArray(44 + totalAudioLen)
        System.arraycopy(header, 0, wav, 0, 44)
        System.arraycopy(pcmData, 0, wav, 44, totalAudioLen)
        return wav
    }

    suspend fun transcribeAudio(
        wavBytes: ByteArray,
        provider: String = "groq",
        apiKey: String,
        language: String? = null,
        prompt: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Cloud API Key is missing"))
        }

        val endpoint = when (provider.lowercase()) {
            "openai" -> "https://api.openai.com/v1/audio/transcriptions"
            else -> "https://api.groq.com/openai/v1/audio/transcriptions"
        }
        val model = when (provider.lowercase()) {
            "openai" -> "whisper-1"
            else -> "whisper-large-v3-turbo"
        }

        try {
            val boundary = "----LiltBoundary" + System.currentTimeMillis().toString(16)
            val lineEnd = "\r\n"
            val twoHyphens = "--"

            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 12000
                doOutput = true
                doInput = true
                useCaches = false
                setRequestProperty("Connection", "Keep-Alive")
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                setRequestProperty("Authorization", "Bearer $apiKey")
            }

            DataOutputStream(conn.outputStream).use { dos ->
                // 1. model
                dos.writeBytes("$twoHyphens$boundary$lineEnd")
                dos.writeBytes("Content-Disposition: form-data; name=\"model\"$lineEnd$lineEnd")
                dos.writeBytes("$model$lineEnd")

                // 2. response_format
                dos.writeBytes("$twoHyphens$boundary$lineEnd")
                dos.writeBytes("Content-Disposition: form-data; name=\"response_format\"$lineEnd$lineEnd")
                dos.writeBytes("json$lineEnd")

                // 3. language (if specified and not auto)
                val langTag = language?.trim()?.lowercase()
                if (!langTag.isNullOrBlank() && langTag != "auto") {
                    dos.writeBytes("$twoHyphens$boundary$lineEnd")
                    dos.writeBytes("Content-Disposition: form-data; name=\"language\"$lineEnd$lineEnd")
                    dos.writeBytes("$langTag$lineEnd")
                }

                // 4. prompt (vocabulary hints)
                if (!prompt.isNullOrBlank()) {
                    dos.writeBytes("$twoHyphens$boundary$lineEnd")
                    dos.writeBytes("Content-Disposition: form-data; name=\"prompt\"$lineEnd$lineEnd")
                    dos.write(prompt.toByteArray(Charsets.UTF_8))
                    dos.writeBytes(lineEnd)
                }

                // 4b. temperature = 0.0 (strictly deterministic, prevents greedy hallucination loops on noisy audio)
                dos.writeBytes("$twoHyphens$boundary$lineEnd")
                dos.writeBytes("Content-Disposition: form-data; name=\"temperature\"$lineEnd$lineEnd")
                dos.writeBytes("0.0$lineEnd")

                // 5. file
                dos.writeBytes("$twoHyphens$boundary$lineEnd")
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"$lineEnd")
                dos.writeBytes("Content-Type: audio/wav$lineEnd$lineEnd")
                dos.write(wavBytes)
                dos.writeBytes(lineEnd)

                // Final boundary
                dos.writeBytes("$twoHyphens$boundary$twoHyphens$lineEnd")
                dos.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val respJson = JSONObject(response)
                val transcript = respJson.optString("text", "").trim()
                DiagnosticLogger.log("CloudCleanupEngine", "Whisper STT Success ($model): '$transcript'")
                Result.success(transcript)
            } else {
                val errorStream = conn.errorStream?.let { BufferedReader(InputStreamReader(it)).use { r -> r.readText() } } ?: ""
                val err = "Whisper HTTP $responseCode from $provider: $errorStream"
                DiagnosticLogger.logError("CloudCleanupEngine", err)
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            DiagnosticLogger.logError("CloudCleanupEngine", "Whisper STT Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun fixUnnaturalTitleCase(text: String): String {
        val lines = text.split("\n")
        return lines.joinToString("\n") { line ->
            val sentences = line.split(Regex("(?<=[.!?])\\s+"))
            sentences.joinToString(" ") { sentence ->
                val words = sentence.split(" ")
                if (words.size <= 2) return@joinToString sentence

                val alphaWords = words.filter { it.firstOrNull()?.isLetter() == true }
                if (alphaWords.isEmpty()) return@joinToString sentence

                val capitalizedCount = alphaWords.count { word ->
                    word.first().isUpperCase() && !word.all { it.isUpperCase() }
                }

                if (capitalizedCount.toDouble() / alphaWords.size > 0.55) {
                    val normalizedWords = words.mapIndexed { index, word ->
                        if (index == 0) {
                            word
                        } else if (word.all { it.isUpperCase() && it.isLetter() }) {
                            word
                        } else {
                            word.replaceFirstChar { it.lowercaseChar() }
                        }
                    }
                    normalizedWords.joinToString(" ")
                } else {
                    sentence
                }
            }
        }
    }

    private fun fixErroneousQuestionMarks(text: String): String {
        val lines = text.split("\n")
        return lines.joinToString("\n") { line ->
            val trimmed = line.trimEnd()
            if (!trimmed.endsWith("?")) return@joinToString line

            val lower = trimmed.lowercase()
            val teluguQuestionWords = setOf(
                "ela", "enti", "emiti", "ekkada", "ekkadiki", "eppudu", "enduku", 
                "evadu", "evaru", "edhi", "edi", "evariki", "enno", "enni"
            )
            val teluguQuestionSuffixes = listOf(
                "aa", "raa", "ra", "dha", "dhaa", "undha", "ledha", "chesara", 
                "unnara", "ayindha", "vachara", "kadha", "kada", "leka"
            )
            val englishQuestionStarters = listOf(
                "who", "what", "where", "when", "why", "how", "is", "are", 
                "am", "do", "does", "did", "can", "could", "will", "would", 
                "should", "may", "might"
            )

            val words = lower.split(Regex("[\\s,;]+")).filter { it.isNotBlank() }
            val firstWord = words.firstOrNull()?.replace(Regex("[^a-z]"), "") ?: ""
            val lastWord = words.lastOrNull()?.replace(Regex("[^a-z]"), "") ?: ""

            val hasEnglishQuestion = englishQuestionStarters.contains(firstWord)
            val hasTeluguQuestionWord = words.any { teluguQuestionWords.contains(it.replace(Regex("[^a-z]"), "")) }
            val hasTeluguSuffix = teluguQuestionSuffixes.any { lastWord.endsWith(it) }

            if (!hasEnglishQuestion && !hasTeluguQuestionWord && !hasTeluguSuffix) {
                trimmed.dropLast(1).trimEnd() + "."
            } else {
                line
            }
        }
    }


    fun transliterateTeluguToRoman(text: String): String {
        return TenglishNormalizationEngine.normalize(text)
    }

    fun formatEmailSpacing(text: String): String {
        val trimmed = text.trim()
        if (!trimmed.startsWith("Subject:", ignoreCase = true) &&
            !trimmed.contains("\nSubject:", ignoreCase = true)) {
            return text
        }

        var t = trimmed.replace("\r\n", "\n")
        // Collapse 3 or more consecutive newlines down to 2
        t = t.replace(Regex("\n{3,}"), "\n\n")

        // Ensure exactly one blank line after "Subject: ..."
        t = t.replace(Regex("(?i)^(Subject:[^\n]*)\n(?!\n)"), "$1\n\n")

        // Ensure exactly one blank line after Salutation (Hi ..., Dear ..., Hello ..., etc.)
        t = t.replace(Regex("(?im)^((?:Hi|Dear|Hello|Hey)\\b[^\n]*,)\n(?!\n)"), "$1\n\n")

        // Ensure exactly one blank line before Sign-off (Best regards, Sincerely, Thanks, etc.)
        t = t.replace(Regex("(?im)(?<!\n\n)\n((?:Best regards|Warm regards|Kind regards|Regards|Sincerely|Thanks|Best|Cheers),)"), "\n\n$1")

        // Ensure exactly ONE newline between Sign-off and Name (no blank line in signature block)
        t = t.replace(Regex("(?im)^((?:Best regards|Warm regards|Kind regards|Regards|Sincerely|Thanks|Best|Cheers),)\n\n+([^\n]+)"), "$1\n$2")

        return t.trim()
    }

    data class LanguageDirectiveResult(
        val strippedText: String,
        val effectiveLanguageMode: String,
        val wasExplicitDirective: Boolean
    )

    fun parseLanguageDirective(
        text: String,
        currentMode: String,
        enabledLanguages: List<String> = listOf("en", "te_roman", "hi_roman")
    ): LanguageDirectiveResult {
        val trimmed = text.trim()

        // 1. Explicit "Romanized <Language>" spoken prefix (e.g., "Romanized Telugu ...", "Romanized Hindi ...", "Romanized Tamil ...", "Romanized Arabic ...")
        val romanizedGeneric = Regex(
            "(?i)^\\s*(?:in\\s+)?(?:romanized|romanised|romanize|romanise)\\s+([a-zA-Z]+)(?:\\s*[:,\\-—]\\s*|\\s+)",
            RegexOption.IGNORE_CASE
        ).find(trimmed)
        if (romanizedGeneric != null) {
            val langWord = romanizedGeneric.groupValues[1].lowercase()
            val remainder = trimmed.substring(romanizedGeneric.range.last + 1).trim()
            val modeCode = when (langWord) {
                "telugu", "tenglish" -> "te_roman"
                "hindi", "hinglish" -> "hi_roman"
                "tamil", "tanglish" -> "ta_roman"
                "kannada", "kanglish" -> "kn_roman"
                "malayalam", "manglish" -> "ml_roman"
                "marathi", "minglish" -> "mr_roman"
                "bengali", "bangla", "banglish" -> "bn_roman"
                "gujarati", "gujlish" -> "gu_roman"
                "punjabi" -> "pa_roman"
                "urdu" -> "ur_roman"
                else -> "romanized_custom:$langWord"
            }
            val processed = if (modeCode == "te_roman") transliterateTeluguToRoman(remainder) else remainder
            return LanguageDirectiveResult(processed, modeCode, true)
        }

        // 2. Colloquial Romanized / Native shortcuts ("Tenglish ...", "Hinglish ...", "Tanglish ...", "Kanglish ...", "Manglish ...")
        val colloquialMap = listOf(
            Regex("(?i)^\\s*(?:tenglish|telugu\\s+lo|telugulo)(?:\\s*[:,\\-—]\\s*|\\s+)") to "te_roman",
            Regex("(?i)^\\s*(?:hinglish|hindi\\s+mein|hindimein)(?:\\s*[:,\\-—]\\s*|\\s+)") to "hi_roman",
            Regex("(?i)^\\s*(?:tanglish|tamil\\s+la)(?:\\s*[:,\\-—]\\s*|\\s+)") to "ta_roman",
            Regex("(?i)^\\s*(?:kanglish)(?:\\s*[:,\\-—]\\s*|\\s+)") to "kn_roman",
            Regex("(?i)^\\s*(?:manglish)(?:\\s*[:,\\-—]\\s*|\\s+)") to "ml_roman",
            Regex("(?i)^\\s*(?:minglish)(?:\\s*[:,\\-—]\\s*|\\s+)") to "mr_roman",
            Regex("(?i)^\\s*(?:banglish)(?:\\s*[:,\\-—]\\s*|\\s+)") to "bn_roman"
        )
        for ((pattern, modeCode) in colloquialMap) {
            val match = pattern.find(trimmed)
            if (match != null) {
                val remainder = trimmed.substring(match.range.last + 1).trim()
                val processed = if (modeCode == "te_roman") transliterateTeluguToRoman(remainder) else remainder
                return LanguageDirectiveResult(processed, modeCode, true)
            }
        }

        // 3. Explicit "Write in <Language>" / "In <Language>" spoken prefix
        val inLangMatch = Regex(
            "(?i)^\\s*(?:write\\s+in|speak\\s+in|type\\s+in|in)\\s+(english|telugu|hindi|tamil|kannada|malayalam|marathi|bengali|gujarati|punjabi|urdu|spanish|french|german|portuguese|italian|japanese|korean|chinese|mandarin|arabic|russian|turkish|indonesian|vietnamese)(?:\\s*[:,\\-—]\\s*|\\s+)",
            RegexOption.IGNORE_CASE
        ).find(trimmed)
        if (inLangMatch != null) {
            val langWord = inLangMatch.groupValues[1].lowercase()
            val remainder = trimmed.substring(inLangMatch.range.last + 1).trim()
            val resolvedCode = when (langWord) {
                "english" -> "en"
                "telugu" -> if (enabledLanguages.contains("te") && !enabledLanguages.contains("te_roman")) "te" else "te_roman"
                "hindi" -> if (enabledLanguages.contains("hi") && !enabledLanguages.contains("hi_roman")) "hi" else "hi_roman"
                "tamil" -> if (enabledLanguages.contains("ta") && !enabledLanguages.contains("ta_roman")) "ta" else "ta_roman"
                "kannada" -> if (enabledLanguages.contains("kn") && !enabledLanguages.contains("kn_roman")) "kn" else "kn_roman"
                "malayalam" -> if (enabledLanguages.contains("ml") && !enabledLanguages.contains("ml_roman")) "ml" else "ml_roman"
                "marathi" -> "mr_roman"
                "bengali" -> "bn_roman"
                "gujarati" -> "gu_roman"
                "punjabi" -> "pa_roman"
                "urdu" -> "ur_roman"
                "spanish" -> "es"
                "french" -> "fr"
                "german" -> "de"
                "portuguese" -> "pt"
                "italian" -> "it"
                "japanese" -> "ja"
                "korean" -> "ko"
                "chinese", "mandarin" -> "zh"
                "arabic" -> "ar"
                "russian" -> "ru"
                "turkish" -> "tr"
                "indonesian" -> "id"
                "vietnamese" -> "vi"
                else -> "en"
            }
            val processed = if (resolvedCode == "te_roman") transliterateTeluguToRoman(remainder) else remainder
            return LanguageDirectiveResult(processed, resolvedCode, true)
        }

        // 4. Default when no voice prefix was spoken:
        // Stay in currentMode ("auto" uses the user's bounded enabledLanguages pool, or specific selected tab)
        val shouldTransliterateTelugu = currentMode == "te_roman" ||
            (currentMode == "auto" && enabledLanguages.contains("te_roman") && !enabledLanguages.contains("te"))
        val processed = if (shouldTransliterateTelugu && trimmed.any { it in '\u0C00'..'\u0C7F' }) {
            transliterateTeluguToRoman(trimmed)
        } else {
            trimmed
        }
        return LanguageDirectiveResult(processed, currentMode, false)
    }

    private fun formatEmailsAndCurrencies(text: String): String {
        var t = text
        // Email formats: "name dot surname at gmail dot com" -> "name.surname@gmail.com"
        t = t.replace(Regex("(?i)(\\b[a-zA-Z0-9_\\-\\+]+)\\s+dot\\s+([a-zA-Z0-9_\\-\\+]+)\\s+at\\s+([a-zA-Z0-9_\\-\\+]+)\\s+dot\\s+(com|org|net|in|io|co|ai|edu|gov)\\b")) { m ->
            "${m.groupValues[1]}.${m.groupValues[2]}@${m.groupValues[3]}.${m.groupValues[4]}"
        }
        t = t.replace(Regex("(?i)(\\b[a-zA-Z0-9_\\-\\+\\.]+)\\s+at\\s+([a-zA-Z0-9_\\-\\+]+)\\s+dot\\s+(com|org|net|in|io|co|ai|edu|gov)\\b")) { m ->
            "${m.groupValues[1]}@${m.groupValues[2]}.${m.groupValues[3]}"
        }
        t = t.replace(Regex("(?i)(\\b[a-zA-Z0-9_\\-\\+\\.]+)\\s*@\\s*([a-zA-Z0-9_\\-\\+]+)\\s+dot\\s+(com|org|net|in|io|co|ai|edu|gov)\\b")) { m ->
            "${m.groupValues[1]}@${m.groupValues[2]}.${m.groupValues[3]}"
        }

        // Indian Currency format: "5000 rupees" / "5000 rs" / "rs 5000" -> ₹5,000
        val inrPattern = Regex("(?i)\\b(?:rs\\.?|rupees?|inr)\\s*(\\d[\\d,]*)\\b")
        t = t.replace(inrPattern) { m ->
            val num = m.groupValues[1].replace(",", "").toLongOrNull()
            if (num != null) "₹%,d".format(java.util.Locale.US, num) else "₹${m.groupValues[1]}"
        }
        val inrSuffixPattern = Regex("(?i)\\b(\\d[\\d,]*)\\s*(?:rs\\.?|rupees?|inr)\\b")
        t = t.replace(inrSuffixPattern) { m ->
            val num = m.groupValues[1].replace(",", "").toLongOrNull()
            if (num != null) "₹%,d".format(java.util.Locale.US, num) else "₹${m.groupValues[1]}"
        }

        // Dollar formats: "250 dollars" / "$ 250" -> $250
        t = t.replace(Regex("(?i)\\b(\\d[\\d,]*)\\s*dollars?\\b")) { m ->
            "$${m.groupValues[1]}"
        }

        return t
    }

    private fun ensureQuestionMarks(text: String): String {
        val questionStarters = setOf(
            "who", "what", "where", "when", "why", "how", "can", "could", "would", "is", "are", 
            "do", "does", "did", "should", "will", "may", "which",
            "enti", "emiti", "eppudu", "ekkada", "enduku", "ela", "evaru", "edi",
            "kya", "kab", "kahan", "kaise", "kyun", "kaun"
        )
        val questionEndings = listOf("chesara", "unnara", "ayindha", "undha", "ledha", "kadha", "kada", "leka")

        val lines = text.split("\n")
        return lines.joinToString("\n") { line ->
            val trimmed = line.trimEnd()
            if (trimmed.endsWith(".")) {
                val words = trimmed.lowercase().split(Regex("[\\s,;]+")).filter { it.isNotBlank() }
                val firstWord = words.firstOrNull()?.replace(Regex("[^a-z]"), "") ?: ""
                val lastWord = words.lastOrNull()?.replace(Regex("[^a-z]"), "") ?: ""

                if (questionStarters.contains(firstWord) || questionEndings.any { lastWord.endsWith(it) }) {
                    trimmed.dropLast(1).trimEnd() + "?"
                } else {
                    line
                }
            } else {
                line
            }
        }
    }

    private fun sanitizePunctuationAndCasing(
        text: String,
        languageMode: String = "en",
        enabledLanguages: List<String> = listOf("en", "te_roman", "hi_roman")
    ): String {
        val shouldTransliterateTelugu = languageMode == "te_roman" ||
            (languageMode == "auto" && enabledLanguages.contains("te_roman") && !enabledLanguages.contains("te"))
        var result = if (shouldTransliterateTelugu) {
            transliterateTeluguToRoman(text.trim())
        } else {
            text.trim()
        }
        result = result
            .replace(Regex("\\s+,\\s*"), ", ")
            .replace(Regex(",{2,}"), ",")
            .replace(Regex(",\\s*\\."), ".")
            .replace(Regex(",\\s*\\?"), "?")
            .replace(Regex("\\s+\\."), ".")
            .replace(Regex("\\s+\\?"), "?")

        result = formatEmailsAndCurrencies(result)
        result = fixUnnaturalTitleCase(result)
        result = fixErroneousQuestionMarks(result)
        result = ensureQuestionMarks(result)
        result = formatEmailSpacing(result)
        return result
    }

    suspend fun polish(
        rawText: String,
        provider: String = "groq",
        apiKey: String,
        model: String = "qwen/qwen3.8-27b",
        tone: String = "natural",
        customVocabulary: List<String> = emptyList(),
        replacements: Map<String, String> = emptyMap(),
        voiceCommands: List<com.lilt.mobile.data.VoiceCommandItem> = emptyList(),
        languageMode: String = "en",
        enabledLanguages: List<String> = listOf("en", "te_roman", "hi_roman")
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Cloud API Key is missing"))
        }

        val endpoint = when (provider.lowercase()) {
            "openai" -> "https://api.openai.com/v1/chat/completions"
            "gemini" -> "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"
            else -> "https://api.groq.com/openai/v1/chat/completions"
        }

        try {
            val systemPrompt = buildSystemPrompt(
                tone = tone,
                customVocabulary = customVocabulary,
                replacements = replacements,
                voiceCommands = voiceCommands,
                languageMode = languageMode,
                enabledLanguages = enabledLanguages
            )
            val jsonPayload = JSONObject().apply {
                put("model", model)
                put("temperature", 0.0)
                put("max_tokens", 1024)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "<transcript>\n$rawText\n</transcript>")
                    })
                })
            }

            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 6000
                readTimeout = 8000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(jsonPayload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val respJson = JSONObject(response)
                val choices = respJson.optJSONArray("choices")
                val text = choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content")?.trim() ?: rawText
                val sanitized = sanitizePunctuationAndCasing(text, languageMode, enabledLanguages)
                DiagnosticLogger.log("CloudCleanupEngine", "Success from $provider ($model): sanitized output: '$sanitized'")
                Result.success(sanitized)
            } else {
                val errorStream = conn.errorStream?.let { BufferedReader(InputStreamReader(it)).use { r -> r.readText() } } ?: ""
                val err = "HTTP $responseCode from $provider: $errorStream"
                DiagnosticLogger.logError("CloudCleanupEngine", err)
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            DiagnosticLogger.logError("CloudCleanupEngine", "Network or parsing exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun testConnection(
        provider: String = "groq",
        apiKey: String,
        model: String = "qwen/qwen3.8-27b"
    ): Result<Pair<Long, String>> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val res = polish(
            rawText = "um testing one two three like you know everything is working",
            provider = provider,
            apiKey = apiKey,
            model = model,
            tone = "natural"
        )
        val latency = System.currentTimeMillis() - start
        if (res.isSuccess) {
            Result.success(Pair(latency, res.getOrThrow()))
        } else {
            Result.failure(res.exceptionOrNull() ?: Exception("Unknown error"))
        }
    }

    private fun buildSystemPrompt(
        tone: String,
        customVocabulary: List<String>,
        replacements: Map<String, String>,
        voiceCommands: List<com.lilt.mobile.data.VoiceCommandItem> = emptyList(),
        languageMode: String = "en",
        enabledLanguages: List<String> = listOf("en", "te_roman", "hi_roman")
    ): String {
        val toneInstruction = when (tone.lowercase()) {
            "pro" -> "Format professionally with refined executive vocabulary, precise grammar, and formal sentence structure."
            "chat", "casual" -> "Format conversationally as a clean text message with natural colloquial phrasing."
            "bullets" -> "Format the key points as clean, concise bullet points (using '• ') with brief header context."
            else -> "Format naturally: preserve the speaker's original voice, flow, and intent while ensuring clean punctuation and capitalization."
        }

        val languageInstruction = when {
            languageMode == "en" -> """

=== STRICT LANGUAGE LOCK: 100% PURE ENGLISH ONLY ===
- ACTIVE SELECTION: The user has explicitly selected ENGLISH MODE.
- ABSOLUTE REQUIREMENT: The final output MUST be 100% in the English language.
- Under NO circumstance should any Telugu, Hindi, Spanish, Welsh, or foreign words appear in the output.
- If the raw speech contains any foreign acoustic misrecognitions, translate or adapt them into natural, standard English.
- Use natural, polished English grammar, spelling, and sentence-level capitalization.
"""
            languageMode == "te_roman" -> """

=== STRICT LANGUAGE LOCK: ROMANIZED TELUGU (TENGLISH) ONLY ===
- ACTIVE SELECTION: The user has explicitly selected TENGLISH / ROMANIZED TELUGU MODE.
- ABSOLUTE REQUIREMENT: The output MUST consist EXCLUSIVELY of English and Romanized Telugu (Tenglish).
- ZERO FOREIGN LANGUAGES: Under NO circumstance should Hindi words (e.g. 'haan', 'kya', 'hai', 'kaise', 'accha'), Welsh words, Arabic words, or any other language appear. The text MUST strictly be Romanized Telugu + English.
- ABSOLUTE SCRIPT RULE: Output EXCLUSIVELY in the Roman alphabet (English/Latin letters A-Z, a-z).
  * NEVER output native Telugu Unicode script (తెలుగు లిపి).
  * If the input transcript contains Telugu script (e.g. 'నేను ఆఫీస్ కి వెళ్తున్నాను' or 'బాగున్నారా'), TRANSLITERATE it into modern Romanized Telugu immediately (e.g. 'Nenu office ki velthunnanu', 'Bagunnara?').
- NATURAL SENTENCE CASING (STRICT NO TITLE-CASING RULE):
  * DO NOT capitalize every word. NEVER use Title Case like "Nenu Repu Morning Office Ki Vasthanu".
  * Use standard natural sentence casing: ONLY capitalize the first letter of the sentence, and proper nouns / acronyms (e.g. 'Hyderabad', 'Google', 'WhatsApp', 'API', 'UI', 'AM', 'PM').
  * All other words MUST be in natural lowercase.
- NATURAL, MINIMALIST PUNCTUATION:
  * DO NOT insert unnecessary commas between words or short phrases.
  * ONLY add a question mark ('?') if the speaker is genuinely asking a clear question (e.g. "Ela unnaru?", "Eppudu vasthunnav?", "Bhojanam chesara?"). If the speaker is making a statement or giving an update, end with a simple period ('.'), NEVER a question mark.
- MODERN CONVERSATIONAL SPELLING (WHATSAPP / DAILY TEXTING STYLE):
  * ZERO MECHANICAL TRANSLITERATIONS OF ENGLISH WORDS:
    - ALWAYS preserve standard English spelling: 'office', 'cinema', 'meeting', 'lunch', 'car', 'developer', 'project', 'client', 'call', 'update', 'status', 'laptop', 'bug', 'deploy'.
  * TELUGU POSTPOSITION DETACHMENT:
    - Keep Telugu postposition suffixes separated from English words by a space: 'office ki' (not 'officeki'), 'meeting lo' (not 'meetinglo'), 'client tho' (not 'clienttho').
  * Remove Telugu verbal hesitation fillers ('ante', 'adi', 'mari', 'chudu', 'chudandi', 'vachesi', 'um', 'uh').
"""
            languageMode == "hi_roman" -> """

=== STRICT LANGUAGE LOCK: ROMANIZED HINDI (HINGLISH) ONLY ===
- ACTIVE SELECTION: The user has explicitly selected HINGLISH / ROMANIZED HINDI MODE.
- ABSOLUTE REQUIREMENT: The output MUST consist EXCLUSIVELY of English and Romanized Hindi (Hinglish).
- ZERO FOREIGN LANGUAGES: Under NO circumstance should Telugu words (e.g. 'enti', 'undi', 'cheppandi', 'nenu'), Welsh words, or any other foreign language appear. The text MUST strictly be Romanized Hindi + English.
- ABSOLUTE SCRIPT RULE: Output EXCLUSIVELY in the Roman alphabet (English/Latin letters A-Z, a-z).
  * NEVER output Devanagari script (देवनागरी लिपि).
  * If the input transcript contains Devanagari script (e.g. 'आप कैसे हो' or 'कल सुबह मीटिंग करते हैं'), you MUST TRANSLITERATE it into modern Romanized conversational Hinglish immediately (e.g. 'Aap kaise ho', 'Kal subah meeting karte hain').
- NATURAL SENTENCE CASING (STRICT NO TITLE-CASING RULE):
  * DO NOT capitalize every word. Never use Title Case like "Aap Kaise Ho Kya Chal Raha Hai".
  * Use standard natural sentence casing: ONLY capitalize the first letter of the sentence, and proper nouns / acronyms.
- NATURAL, MINIMALIST PUNCTUATION:
  * ONLY add a question mark ('?') if the speaker is asking an actual question (e.g. "Aap kaise ho?", "Kya chal raha hai?"). For statements, always end with a simple period ('.').
  * Keep all English words in standard English spelling ('meeting', 'project', 'client', 'call', 'office', 'update', 'status').
  * Remove Hindi verbal hesitation fillers ('matlab', 'woh', 'haan', 'arre', 'um', 'uh').
"""
            languageMode.startsWith("romanized_custom:") || languageMode.endsWith("_roman") -> {
                val langObj = com.lilt.mobile.data.SettingsManager.findLanguageByCode(languageMode)
                val langName = langObj?.baseLanguageName ?: languageMode.removePrefix("romanized_custom:").replaceFirstChar { it.uppercaseChar() }
                """

=== STRICT LANGUAGE LOCK: ROMANIZED ${langName.uppercase()} (LATIN SCRIPT) ONLY ===
- ACTIVE SELECTION: The user has explicitly requested ROMANIZED $langName (Latin alphabet transliteration + English).
- ABSOLUTE SCRIPT RULE: Output EXCLUSIVELY in the Roman alphabet (English/Latin letters A-Z, a-z).
  * NEVER output native non-Latin script for $langName.
  * If the raw transcript contains native $langName script, you MUST TRANSLITERATE it into modern, natural conversational Romanized $langName immediately.
  * Preserve all English loanwords in standard English spelling (e.g. 'office', 'meeting', 'project', 'call', 'update', 'laptop').
- NATURAL SENTENCE CASING: Capitalize ONLY the first letter of the sentence and proper nouns. NEVER capitalize every word.
- ZERO UNRELATED FOREIGN LANGUAGES: Output strictly in Romanized $langName and English.
"""
            }
            languageMode != "auto" -> {
                val langObj = com.lilt.mobile.data.SettingsManager.findLanguageByCode(languageMode)
                val langName = langObj?.fullName ?: languageMode
                """

=== STRICT LANGUAGE LOCK: ${langName.uppercase()} ONLY ===
- ACTIVE SELECTION: The user has explicitly selected $langName.
- ABSOLUTE REQUIREMENT: The final output MUST be strictly in $langName (preserving standard English technical loanwords if naturally spoken).
- Under NO circumstance should unrelated foreign languages appear in the output.
- Use natural, polished grammar, native orthography, and clean sentence-level punctuation.
"""
            }
            else -> {
                val enabledObjs = enabledLanguages.mapNotNull { com.lilt.mobile.data.SettingsManager.findLanguageByCode(it) }
                    .ifEmpty { listOfNotNull(com.lilt.mobile.data.SettingsManager.findLanguageByCode("en")) }
                val bulletList = enabledObjs.mapIndexed { idx, lang ->
                    val scriptNote = if (lang.isRomanized) " [MUST output in Roman/Latin alphabet A-Z ONLY — NEVER native script]" else ""
                    "  ${idx + 1}. ${lang.fullName}$scriptNote"
                }.joinToString("\n")
                val namesSummary = enabledObjs.joinToString(", ") { it.fullName }
                val allRomanOnly = enabledObjs.all { it.code == "en" || it.isRomanized || it.whisperIsoCode in listOf("es", "fr", "de", "pt", "it", "nl", "tr", "id", "vi", "pl") }
                val scriptRule = if (allRomanOnly) {
                    "- ABSOLUTE SCRIPT RULE: Every language in the user's selected pool uses the Roman/Latin alphabet. Output EXCLUSIVELY in Latin letters — if the raw transcript contains any native Indian or non-Latin script (such as తెలుగు or देवनागरी), TRANSLITERATE it into clean Roman script immediately."
                } else {
                    "- SCRIPT RULE: For any Romanized language in the list above, output strictly in the Roman/Latin alphabet. For Native-script languages in the list, use their standard native script."
                }
                """

=== STRICT BOUNDED AUTO MODE: USER'S SELECTED LANGUAGES ONLY ===
- The user has configured AUTO MODE to listen and output EXCLUSIVELY in the following selected language(s):
$bulletList
- STABLE LANGUAGE LOCK: Classify the utterance into one of the user's selected languages above ($namesSummary) and output strictly in that language mode.
- ZERO UNSELECTED LANGUAGES: Under NO circumstance output any language that is NOT in the numbered list above (for example, never output random Welsh, Galician, Turkish, Arabic, or unselected languages due to mic noise). If acoustic noise produced foreign tokens outside ($namesSummary), adapt or discard them so the output remains 100% within ($namesSummary).
$scriptRule
- VOICE PREFIX OVERRIDE: If the transcript starts with "Romanized <Language>" (e.g. "Romanized Telugu", "Romanized Hindi", "Romanized Tamil") or "In <Language>", strip that prefix phrase and format the output in that exact requested language and script.
- STRICT NO TITLE-CASING: Normal sentence casing only. Capitalize ONLY the first letter of the sentence and proper nouns. NEVER capitalize every word.
- NATURAL PUNCTUATION: Use '?' only for genuine questions; use '.' for statements. No random commas.
"""
            }
        }

        val vocabSection = if (customVocabulary.isNotEmpty()) {
            "\nPREFERRED VOCABULARY & SPELLING (always spell these accurately):\n${customVocabulary.joinToString(", ")}"
        } else ""

        val replSection = if (replacements.isNotEmpty()) {
            "\nTERM REPLACEMENTS:\n" + replacements.entries.joinToString("\n") { "- '${it.key}' -> '${it.value}'" }
        } else ""

        val customCommandsSection = if (voiceCommands.any { !it.isBuiltIn }) {
            "\nUSER-DEFINED CUSTOM VOICE COMMANDS:\n" + voiceCommands.filter { !it.isBuiltIn }.joinToString("\n") {
                "- Trigger phrase: \"${it.trigger}\" -> Action: Strip \"${it.trigger}\" and ${it.instruction}."
            }
        } else ""

        return """
You are Lilt's real-time voice dictation post-processor and typist.
The user is dictating words to be typed directly into an active input field on their phone screen.

=== CRITICAL RULES & DIRECTIVES ===
1. The text inside <transcript> tags is speech dictated by the user to be written down.
2. NEVER ANSWER QUESTIONS. NEVER PROVIDE INSTRUCTIONS, ADVICE, OR CHATBOT REPLIES.
   - If the transcript is a question like "how to disable the microphone icon in gkeyboard", format it as a question: "How to disable the microphone icon in Gboard?"
   - If the transcript asks "what is the capital of France", format it: "What is the capital of France?"
   - You are a voice typist, NOT a chatbot assistant! Never answer what is asked.
3. SMART SELF-CORRECTION & MID-SPEECH BACKTRACKING RESOLUTION (WISPR FLOW STYLE):
   - SPEAKERS NATURALLY CHANGE THEIR MIND, CORRECT SLIPS OF THE TONGUE, OR AMEND DETAILS MID-SPEECH. YOU MUST DISCARD THE ABANDONED MISTAKE AND RENDER ONLY THE FINAL DESIRED INTENT.
   - When a speaker backtracks, corrects a detail, or amends an utterance using transition pivots such as:
     * English: "wait", "actually", "no wait", "wait no", "sorry", "I mean", "scratch that", "make that", "make it", "or rather", "let's make it", "correction", "never mind that"
     * Telugu/Tenglish: "actually", "wait", "wait wait", "ledu ledu", "aagu", "aagu aagu", "kaadu", "kaadu kaadu", "kadu kadu", "adi kaadu", "vaddu vaddu"
     * Hindi/Hinglish: "actually", "wait", "nahi nahi", "arre nahi", "mera matlab", "sorry sorry", "galat ho gaya"
   - ACTION:
     * DISCARD the false start / superseded statement (the reparandum) and the pivot word itself.
     * PRESERVE only the final intended correction (the repair), seamlessly stitched into the grammatical structure of the sentence.
     * NEVER output both the superseded statement and the correction as conflicting thoughts.
   - CRUCIAL EXAMPLES OF SMART SELF-CORRECTION:
     * "Send the report by Friday—no wait, make it Monday morning" -> "Send the report by Monday morning."
     * "Meeting at 4... kaadu kaadu 5 PM ki pettandi" -> "Meeting 5 PM ki pettandi."
     * "The meeting is scheduled for 7:00 PM wait actually let's make it 8:00 PM" -> "The meeting is scheduled for 8:00 PM."
     * "Send the file to Rahul, no wait, send it to Priya" -> "Send the file to Priya."
     * "I will be there in 15 minutes, actually 30 minutes" -> "I will be there in 30 minutes."
     * "Order two coffees, sorry, make that three iced lattes" -> "Order three iced lattes."
     * "Call me at my office number... wait no, call my mobile" -> "Call my mobile."
     * "Let's plan for Friday... actually Saturday morning is much better" -> "Let's plan for Saturday morning."
     * "Nenu repu vasthanu... ledu ledu ellundi vasthanu" -> "Nenu ellundi vasthanu."
     * "Rahul ki call chey, aagu aagu, Suresh ki ping chey" -> "Suresh ki ping chey."
     * "Main kal subah aunga, nahi nahi parso milte hain" -> "Parso milte hain."
     * "Meeting 4 baje hai, arre nahi 5 baje rakho" -> "Meeting 5 baje rakho."
4. PRESERVE THE SPEAKER'S INTENT, TONE, AND VOCABULARY:
   - For parts of speech where the speaker did NOT backtrack or correct themselves, do NOT rewrite, summarize, or invent new thoughts.
   - Retain the speaker's natural tone, style, and vocabulary (e.g. casual texting vs professional discussion).
   - Strip verbal hesitation filler sounds ('um', 'uh', 'like', 'you know', 'er', 'ah', repeated stutters).
   - The ONLY exception for structural transformation is when the user explicitly begins with voice commands like "write the email" or "make a list".
5. Output ONLY the final transcribed text. Never add quotes, markdown fences, or conversational filler like "Here is your text:".

=== INTENT & VOICE COMMAND DETECTION ===
Carefully inspect the opening utterance of the user's transcript for voice formatting directives:

1. EMAIL MODE:
   - Triggers: Starts with or contains phrases like "write the email", "write an email", "draft an email", "email to", "send an email to", "compose an email".
   - Actions:
     * STRIP the command phrase, recipient intro ("to [Name] saying/that"), and conversational filler.
     * Extract the intended recipient name if spoken (e.g. "Write the email to Alex saying..." -> Recipient is Alex -> "Hi Alex,").
     * Structure the message in clean, professional email format with EXACTLY one blank line between sections:

Subject: Concise, specific subject line based on content (DO NOT wrap subject in brackets)

Hi [Recipient Name or 'there' if unstated],

[Clear, professional email body in 1-2 well-written paragraphs. State the purpose clearly and courteously.]

Best regards,
[Your Name]

     * STRICT SPACING RULES:
       - There MUST be exactly ONE blank line between "Subject: ..." and "Hi ...,".
       - There MUST be exactly ONE blank line between "Hi ...," and the body paragraph.
       - There MUST be exactly ONE blank line between the body paragraph and "Best regards,".
       - Use a SINGLE newline between "Best regards," and "[Your Name]" (no blank line in the signature block).
       - Never output multiple consecutive blank lines.

2. RAW / VERBATIM MODE:
   - Triggers: Starts with "write it raw", "write raw", "raw text", "verbatim", "exact words", "don't change anything".
   - Actions:
     * STRIP the trigger phrase.
     * Keep the speaker's EXACT words, phrasing, colloquialisms, slang, and dialect.
     * Do NOT rephrase, do NOT alter grammar, and do NOT force standard syntax.
     * ONLY insert commas, periods, question marks, and sentence-start capitalization for clean legibility.

3. CASUAL / HUMOR / JOKE MODE:
   - Triggers: Starts with "write it casual", "make it casual", "joke kind", "say something funny", "make it a joke", "casual joke", "witty".
   - Actions:
     * STRIP the trigger phrase.
     * Deliver the message with a relaxed, playful, witty, or humorous tone while preserving the core message.

4. BULLET POINTS / LIST MODE:
   - Triggers: Starts with "make a list", "bullet points", "bullet this", "write as bullet points", "list of".
   - Actions:
     * STRIP the trigger phrase.
     * Format the output as clean bullet points starting with '• '.

5. NOTE / SNIPPET MODE:
   - Triggers: Starts with "take a note", "make a note", "note down", "note to self", "quick note", "save a note", "note that", "create a note".
   - Actions:
     * STRIP the trigger phrase completely (never repeat "take a note" or "note to self" in the output).
     * Structure the content as clean, concise bullet points starting with '• ' for each idea, key takeaway, or task.
     * Do NOT add conversational intro like "Here is your note:".

6. NUMERICAL, CURRENCY, EMAIL & SYMBOL DIRECTIVES:
   - SPOKEN EMAIL ADDRESSES & HANDLES:
     * "suresh dot kumar at gmail dot com" -> "suresh.kumar@gmail.com"
     * "rajesh at outlook dot com" -> "rajesh@outlook.com"
     * "at the rate" -> "@", "dot com" -> ".com", "dot in" -> ".in", "dot org" -> ".org"
     * "hashtag" -> "#", "slash" -> "/"
   - CURRENCY & AMOUNTS:
     * Convert spoken currency to symbols:
       - "five thousand rupees" / "5000 rupees" / "5000 rs" -> "₹5,000"
       - "five hundred rupees" / "500 rupees" -> "₹500"
       - "10 lakhs" / "10 lakh rupees" -> "₹10,00,000"
       - "25 lakh rupees" -> "₹25,00,000"
       - "5 crore rupees" -> "₹5,00,00,000"
       - "two hundred dollars" / "200 dollars" -> "$200"
       - "50 euros" -> "€50"
   - SPOKEN PHONE NUMBERS & OTPs:
     * Multipliers: "double 9" -> "99", "triple 5" -> "555", "double 0" -> "00".
     * 10-digit phone numbers: When spoken in chunks (e.g. "double 9 double 8 63 triple 5 8" or "9 8 4 9 0 1 2 3 4 5"), ALWAYS fuse into a single unbroken 10-digit phone number: "9988635558".
     * OTPs / Codes: Fuse without spaces ("4499222").

7. SPOKEN LANGUAGE DIRECTIVES (VOICE-TRIGGERED LANGUAGE & SCRIPT SWITCHING):
   - ROMANIZED <LANGUAGE> (e.g., "Romanized Telugu", "Romanized Hindi", "Romanized Tamil", "Romanized Kannada", "Romanized Malayalam", "Romanized Marathi", "Romanized Bengali", "Romanized Gujarati", "Romanized Punjabi", "Romanized Urdu"):
     * Triggers: Starts with or contains phrases like "romanized telugu", "romanized hindi", "romanized tamil", "romanized <language>", "tenglish", "hinglish", "tanglish", "kanglish", "manglish".
     * Actions:
       - STRIP the language command phrase completely (never repeat "romanized telugu" or "romanized hindi" in the output).
       - Transcribe & format the speech strictly in Modern Conversational Romanized <Language> + English (WhatsApp texting style) using EXCLUSIVELY the Latin alphabet (A-Z, a-z).
       - Never output native non-Latin script when "Romanized <Language>" is requested.
       - Natural sentence casing only. Never Title Case every word.
   - IN <LANGUAGE> DIRECTIVE:
     * Triggers: Starts with "in english", "write in english", "in spanish", "in french", "in german", "in hindi", "in telugu", "in tamil", etc.
     * Actions: STRIP the trigger phrase and format strictly in the requested language.

8. DEFAULT MODE (No language directive detected):
   - If no language directive is spoken, strictly follow the ACTIVE LANGUAGE LOCK / BOUNDED AUTO MODE rules below.
   - If no formatting directive is present, format normally:
     * Remove all verbal fillers and hesitations ('um', 'uh', 'like', 'you know', repeated stutter words).
     * Add precise punctuation and capitalization.
     * Apply the default tone: $toneInstruction$languageInstruction$vocabSection$replSection$customCommandsSection
""".trimIndent()
    }
}
