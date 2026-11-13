package com.lilt.mobile.engine

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import com.lilt.mobile.core.LiltCore
import com.lilt.mobile.data.DiagnosticLogger
import com.lilt.mobile.data.DictationItem
import com.lilt.mobile.data.SettingsManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class SpeechEngine(private val context: Context) {
    private val tag = "LiltSpeechEngine"
    private val settings = SettingsManager.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    var listener: Listener? = null
    var isListening: Boolean = false
        private set

    // Cloud Whisper AudioRecord pipeline
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    @Volatile private var isRecordingAudio = false
    private val pcmBuffer = ByteArrayOutputStream()
    private var processingJob: Job? = null

    // Hardware Audio Effects (Noise Suppression, Echo Cancellation)
    private var noiseSuppressor: android.media.audiofx.NoiseSuppressor? = null
    private var echoCanceler: android.media.audiofx.AcousticEchoCanceler? = null

    // System SpeechRecognizer fallback pipeline
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerSafetyTimeoutRunnable: Runnable? = null

    interface Listener {
        fun onReady()
        fun onBeginningOfSpeech()
        fun onRmsChanged(rmsdB: Float)
        fun onPartialResult(partialText: String)
        fun onFinalResult(cleanedText: String, rawText: String)
        fun onError(error: String)
        fun onStopped()
        fun onEndOfSpeech() {}
        fun onNoSpeechDetected() {}
        fun onReminderDetected(reminder: ParsedReminder, originalText: String) {}
    }

    fun startListening(language: String = settings.getRecognitionLanguageTag()) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            val err = "Microphone permission required"
            DiagnosticLogger.logError(tag, err)
            listener?.onError(err)
            return
        }

        stopListening()

        val useCloudWhisper = settings.cloudAiEnabled && settings.cloudApiKey.isNotBlank()
        DiagnosticLogger.log(tag, "startListening (useCloudWhisper: $useCloudWhisper, lang: $language, mode: ${settings.activeLanguageMode})")

        if (useCloudWhisper) {
            startCloudWhisperRecording()
        } else {
            startSystemSpeechRecognizer(language)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startCloudWhisperRecording() {
        try {
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = (minBufSize * 2).coerceAtLeast(4096)

            val primarySource = if (settings.hardwareNoiseSuppression) {
                MediaRecorder.AudioSource.VOICE_RECOGNITION
            } else {
                MediaRecorder.AudioSource.MIC
            }

            var record = AudioRecord(
                primarySource,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (record.state != AudioRecord.STATE_INITIALIZED && primarySource != MediaRecorder.AudioSource.MIC) {
                DiagnosticLogger.log(tag, "VOICE_RECOGNITION initialization failed, fallback to standard MIC")
                record = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
            }
            audioRecord = record

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                DiagnosticLogger.logError(tag, "AudioRecord failed to initialize")
                // Fallback to system recognizer if AudioRecord fails initialization
                startSystemSpeechRecognizer(settings.getRecognitionLanguageTag())
                return
            }

            // Attach hardware acoustic effects if supported by device DSP
            try {
                val sessionId = audioRecord?.audioSessionId ?: 0
                if (sessionId != 0 && settings.hardwareNoiseSuppression) {
                    if (android.media.audiofx.NoiseSuppressor.isAvailable()) {
                        noiseSuppressor = android.media.audiofx.NoiseSuppressor.create(sessionId)?.apply {
                            enabled = true
                        }
                        DiagnosticLogger.log(tag, "Hardware NoiseSuppressor active on session $sessionId")
                    }
                    if (android.media.audiofx.AcousticEchoCanceler.isAvailable()) {
                        echoCanceler = android.media.audiofx.AcousticEchoCanceler.create(sessionId)?.apply {
                            enabled = true
                        }
                    }
                }
            } catch (e: Exception) {
                DiagnosticLogger.log(tag, "Hardware audio effects note: ${e.message}")
            }

            pcmBuffer.reset()
            isRecordingAudio = true
            isListening = true

            audioRecord?.startRecording()
            listener?.onReady()
            listener?.onBeginningOfSpeech()

            recordingThread = Thread({
                val buffer = ByteArray(bufferSize)
                while (isRecordingAudio) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (read > 0) {
                        synchronized(pcmBuffer) {
                            pcmBuffer.write(buffer, 0, read)
                        }

                        // Calculate RMS dB for live waveform animation
                        var sum = 0.0
                        var count = 0
                        var i = 0
                        while (i < read - 1) {
                            val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)).toShort()
                            sum += sample * sample
                            count++
                            i += 2
                        }
                        if (count > 0) {
                            val rms = Math.sqrt(sum / count)
                            val rmsDb = if (rms > 1.0) {
                                (20 * Math.log10(rms / 32767.0) + 90).toFloat().coerceIn(0f, 100f)
                            } else {
                                0f
                            }
                            mainHandler.post {
                                if (isRecordingAudio) {
                                    listener?.onRmsChanged(rmsDb)
                                }
                            }
                        }
                    }
                }
            }, "LiltAudioRecordThread").apply { start() }

        } catch (e: Exception) {
            DiagnosticLogger.logError(tag, "AudioRecord exception: ${e.message}", e)
            isRecordingAudio = false
            isListening = false
            listener?.onError("Failed to access microphone: ${e.message}")
            listener?.onStopped()
        }
    }

    private fun isWhisperHallucination(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length < 2) return true
        val clean = trimmed.lowercase().replace(Regex("[^a-z0-9 ]"), "").trim()
        if (clean.isEmpty()) return true

        // Only filter true Whisper video/subtitle boilerplate hallucinations generated on near-silence
        val knownHallucinations = setOf(
            "thank you for watching", "thanks for watching",
            "subtitles by", "subtitles", "amara org",
            "amaraorg", "please subscribe", "subscribe to my channel",
            "transcribed by", "translated by",
            "english telugu", "tenglish in roman",
            "spoken strictly in english", "spoken strictly in english telugu or hindi only"
        )
        if (clean in knownHallucinations) return true
        if (clean.startsWith("thanks for watching") || clean.startsWith("thank you for watching") || clean.startsWith("subtitles by")) return true
        return false
    }

    private fun stopCloudWhisperRecording() {
        if (!isRecordingAudio && !isListening) return

        isRecordingAudio = false
        isListening = false
        listener?.onEndOfSpeech()

        try {
            noiseSuppressor?.release()
            noiseSuppressor = null
            echoCanceler?.release()
            echoCanceler = null
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.e(tag, "Error stopping audioRecord: ${e.message}")
        }

        try {
            recordingThread?.join(500)
            recordingThread = null
        } catch (_: Exception) {}

        val rawPcm = synchronized(pcmBuffer) { pcmBuffer.toByteArray() }
        val pcmData = if (settings.noiseReductionEnabled) {
            DiagnosticLogger.log(tag, "Applying LiltAudioDSP (High-Pass + Soft-Knee Noise Gate + VAD)")
            LiltAudioDSP.processPcm(rawPcm, sampleRate = 16000)
        } else {
            rawPcm
        }
        // 16000 samples/sec * 2 bytes = 32000 bytes/sec
        // Minimum ~250ms of audio (8000 bytes) required for quick human speech
        if (pcmData.size < 8000) {
            DiagnosticLogger.log(tag, "Recording too short: ${pcmData.size} bytes (${pcmData.size / 32}ms). No speech detected.")
            listener?.onNoSpeechDetected()
            listener?.onStopped()
            return
        }

        // Voice Activity Detection (VAD) / Energy Gate
        val numSamples = pcmData.size / 2
        var sumSquares = 0.0
        var maxAmplitude = 0
        var speechFramesCount = 0
        val frameSize = 320 // 20ms frames at 16kHz
        var currentFrameEnergy = 0.0

        for (i in 0 until numSamples) {
            val low = pcmData[i * 2].toInt() and 0xFF
            val high = pcmData[i * 2 + 1].toInt()
            val sample = (high shl 8) or low
            val abs = Math.abs(sample)
            if (abs > maxAmplitude) {
                maxAmplitude = abs
            }
            sumSquares += sample.toDouble() * sample.toDouble()
            currentFrameEnergy += abs

            if ((i + 1) % frameSize == 0) {
                val avgFrameAmp = currentFrameEnergy / frameSize
                if (avgFrameAmp > 300) {
                    speechFramesCount++
                }
                currentFrameEnergy = 0.0
            }
        }

        val rms = if (numSamples > 0) Math.sqrt(sumSquares / numSamples) else 0.0
        DiagnosticLogger.log(tag, "VAD Energy Check: bytes=${pcmData.size}, maxAmp=$maxAmplitude, rms=$rms, speechFrames=$speechFramesCount")

        // If audio energy indicates absolute room silence or pure low-level mic hiss:
        if (maxAmplitude < 400 || (rms < 50.0 && speechFramesCount < 2)) {
            DiagnosticLogger.log(tag, "No active vocal speech detected (maxAmp=$maxAmplitude, rms=$rms, speechFrames=$speechFramesCount). Dismissed.")
            listener?.onNoSpeechDetected()
            listener?.onStopped()
            return
        }

        processingJob?.cancel()
        processingJob = scope.launch {
            try {
                listener?.onPartialResult("Transcribing with Whisper…")

                val wavBytes = CloudCleanupEngine.encodePcmToWav(
                    pcmData = pcmData,
                    sampleRate = 16000,
                    channels = 1,
                    bitsPerSample = 16
                )

                // Language hint and acoustic prompt for Whisper matched to active mode or user's selected Auto pool
                val enabledLangs = settings.getEnabledLanguages()
                val enabledCodes = settings.enabledLanguageCodes
                val (langHint, whisperPrompt) = if (settings.activeLanguageMode != "auto") {
                    val selected = SettingsManager.findLanguageByCode(settings.activeLanguageMode)
                    Pair(
                        selected?.whisperIsoCode ?: "en",
                        "${selected?.sampleWhisperPrompt ?: "Natural conversational dictation."} Spoken strictly in ${selected?.baseLanguageName ?: "English"}."
                    )
                } else if (enabledLangs.size == 1) {
                    val single = enabledLangs.first()
                    Pair(
                        single.whisperIsoCode,
                        "${single.sampleWhisperPrompt} Spoken strictly in ${single.baseLanguageName}."
                    )
                } else {
                    val distinctIsos = enabledLangs.map { it.whisperIsoCode }.distinct()
                    val isoHint = if (distinctIsos.size == 1) distinctIsos.first() else null
                    val namesList = enabledLangs.joinToString(", ") { it.fullName }
                    val samplesList = enabledLangs.joinToString(" | ") { it.sampleWhisperPrompt }
                    Pair(
                        isoHint,
                        "Conversational dictation strictly in ($namesList): $samplesList"
                    )
                }
                val topRegionalVocab = listOf(
                    "Gachibowli", "Hitec City", "Madhapur", "Jubilee Hills", "Banjara Hills", 
                    "Kondapur", "Kukatpally", "Indiranagar", "Koramangala", "Whitefield", 
                    "Venkatesh", "Pranay", "Anirudh", "Suresh Kumar", "Rajesh", "Rahul"
                ).joinToString(", ")
                val userHistoryContacts = settings.getHistory().mapNotNull { it.contactName }.distinct().take(10).joinToString(", ")
                val combinedBias = listOfNotNull(
                    settings.customVocabulary.ifBlank { null },
                    userHistoryContacts.ifBlank { null },
                    topRegionalVocab
                ).joinToString(", ")
                val vocabPrompt = "$whisperPrompt, $combinedBias"

                val sttResult = CloudCleanupEngine.transcribeAudio(
                    wavBytes = wavBytes,
                    provider = settings.cloudProvider,
                    apiKey = settings.cloudApiKey,
                    language = langHint,
                    prompt = vocabPrompt
                )

                if (sttResult.isFailure) {
                    val err = sttResult.exceptionOrNull()?.message ?: "Whisper transcription failed"
                    DiagnosticLogger.logError(tag, "Whisper STT failed: $err")
                    listener?.onError(err)
                    listener?.onStopped()
                    return@launch
                }

                val rawText = sttResult.getOrThrow()
                if (rawText.isBlank() || isWhisperHallucination(rawText)) {
                    DiagnosticLogger.log(tag, "Whisper returned blank or hallucination on low audio: '$rawText'")
                    listener?.onNoSpeechDetected()
                    listener?.onStopped()
                    return@launch
                }

                DiagnosticLogger.log(tag, "Whisper Raw Result: '$rawText'")
                listener?.onPartialResult("Polishing with Qwen…")

                // Detect Voice-Triggered Language Directive ("Romanized Telugu...", "Romanized Hindi...", "In Spanish...", etc.)
                val directive = CloudCleanupEngine.parseLanguageDirective(rawText, settings.activeLanguageMode, enabledCodes)
                val textToClean = directive.strippedText
                val effectiveLangMode = directive.effectiveLanguageMode

                // Polish with Groq Qwen (qwen/qwen3.8-27b)
                val historyNames = settings.getHistory().mapNotNull { it.contactName }.distinct()
                val regionalKeywords = listOf("Gachibowli", "Hitec City", "Madhapur", "Jubilee Hills", "Banjara Hills", "Kondapur", "Indiranagar", "Koramangala", "Whitefield", "Venkatesh", "Pranay", "Anirudh")
                val customVocabList = (settings.customVocabulary.split(",").map { it.trim() }.filter { it.isNotEmpty() } + historyNames + regionalKeywords).distinct()
                val replacementMap = settings.getReplacements().associate { it.heard to it.replacement }
                val detectedAppContext = if (settings.appContextAwarenessEnabled) {
                    val category = com.lilt.mobile.accessibility.LiltAccessibilityService.currentAppCategory
                    val pkg = com.lilt.mobile.accessibility.LiltAccessibilityService.currentForegroundPackage
                    if (category != com.lilt.mobile.accessibility.LiltAccessibilityService.AppContextCategory.GENERAL) {
                        "${category.label} ($pkg)"
                    } else null
                } else null

                var cleanedText = ""
                if (settings.cloudApiKey.isNotBlank()) {
                    val polishResult = CloudCleanupEngine.polish(
                        rawText = textToClean,
                        provider = settings.cloudProvider,
                        apiKey = settings.cloudApiKey,
                        model = settings.cloudModel,
                        tone = settings.activeTone,
                        customVocabulary = customVocabList,
                        replacements = replacementMap,
                        voiceCommands = settings.getVoiceCommands(),
                        languageMode = effectiveLangMode,
                        enabledLanguages = enabledCodes,
                        appContext = detectedAppContext
                    )

                    if (polishResult.isSuccess) {
                        cleanedText = polishResult.getOrThrow()
                    }
                }

                val shouldTransliterateTelugu = effectiveLangMode == "te_roman" ||
                    (effectiveLangMode == "auto" && enabledCodes.contains("te_roman") && !enabledCodes.contains("te"))

                if (cleanedText.isBlank()) {
                    val rawCleaned = if (shouldTransliterateTelugu && textToClean.any { it in '\u0C00'..'\u0C7F' }) {
                        CloudCleanupEngine.transliterateTeluguToRoman(textToClean)
                    } else textToClean

                    cleanedText = LiltCore.processText(
                        raw = rawCleaned,
                        cleanupLevel = settings.cleanupLevel,
                        numbersAsDigits = settings.spokenNumbers,
                        numberSymbols = settings.numberSymbols,
                        spokenEmoji = settings.spokenEmoji,
                        autoCapitalize = settings.autoCapitalize,
                        trailingSpace = settings.trailingSpace,
                        tone = settings.activeTone
                    )
                }

                if (shouldTransliterateTelugu && cleanedText.any { it in '\u0C00'..'\u0C7F' }) {
                    cleanedText = CloudCleanupEngine.transliterateTeluguToRoman(cleanedText)
                }

                cleanedText = settings.applyReplacementsAndSnippets(cleanedText)

                val wordCount = cleanedText.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                val rawWordCount = rawText.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                val hesitationsRemoved = (rawWordCount - wordCount).coerceAtLeast(0)
                settings.recordStats(words = wordCount, hesitations = hesitationsRemoved, stutters = 1)

                val durationSec = (pcmData.size / 32000).coerceAtLeast(1)
                val isNote = isNoteTrigger(rawText, cleanedText, settings.activeTone)
                val contact = ContactExtractor.extract(cleanedText, rawText)
                val textToPreserve = cleanedText.ifBlank { rawText }
                val item = DictationItem(
                    cleanedText = textToPreserve,
                    rawText = rawText,
                    durationSeconds = durationSec,
                    tone = if (isNote) "notes" else settings.activeTone,
                    isNote = isNote,
                    contactName = contact?.name,
                    contactPhone = contact?.formattedDisplayPhone
                )
                settings.addHistory(item)
                copyToClipboardIfEnabled(textToPreserve)
                checkAndScheduleReminder(textToPreserve, rawText)

                listener?.onFinalResult(cleanedText = cleanedText, rawText = rawText)
                listener?.onStopped()

            } catch (e: CancellationException) {
                // Job cancelled
            } catch (e: Exception) {
                DiagnosticLogger.logError(tag, "Error processing cloud dictation: ${e.message}", e)
                listener?.onError("Processing error: ${e.message}")
                listener?.onStopped()
            }
        }
    }

    private fun startSystemSpeechRecognizer(language: String) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            val err = "Speech recognition is not available on this device."
            DiagnosticLogger.logError(tag, err)
            listener?.onError(err)
            return
        }

        try {
            cleanupSpeechRecognizer()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        listener?.onReady()
                    }

                    override fun onBeginningOfSpeech() {
                        listener?.onBeginningOfSpeech()
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        listener?.onRmsChanged(rmsdB)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        listener?.onEndOfSpeech()
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        clearRecognizerSafetyTimeout()
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                            SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Hold and speak for a moment."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition engine busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Hold and speak for a moment."
                            else -> "Recording is too short. Hold and speak for a moment."
                        }
                        DiagnosticLogger.logError(tag, "SpeechRecognizer error $error: $message")
                        listener?.onError(message)
                        listener?.onStopped()
                        cleanupSpeechRecognizer()
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        clearRecognizerSafetyTimeout()
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val rawText = matches?.firstOrNull() ?: ""

                        DiagnosticLogger.log(tag, "SpeechRecognizer onResults: '$rawText'")

                        if (rawText.isBlank()) {
                            DiagnosticLogger.logError(tag, "SpeechRecognizer emitted empty raw speech transcript")
                            listener?.onError("No speech detected. Hold and speak for a moment.")
                            listener?.onStopped()
                            cleanupSpeechRecognizer()
                            return
                        }

                        scope.launch {
                            val customVocabList = settings.customVocabulary
                                .split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }

                            val replacementMap = settings.getReplacements().associate { it.heard to it.replacement }

                            // Detect Voice-Triggered Language Directive ("Romanized Telugu...", "Romanized Hindi...", "In Spanish...", etc.)
                            val enabledCodes = settings.enabledLanguageCodes
                            val directive = CloudCleanupEngine.parseLanguageDirective(rawText, settings.activeLanguageMode, enabledCodes)
                            val textToClean = directive.strippedText
                            val effectiveLangMode = directive.effectiveLanguageMode

                            var cleanedText = ""
                            if (settings.cloudAiEnabled && settings.cloudApiKey.isNotBlank()) {
                                listener?.onPartialResult("Polishing with ${settings.cloudProvider.uppercase()}…")
                                val detectedAppContext = if (settings.appContextAwarenessEnabled) {
                                    val category = com.lilt.mobile.accessibility.LiltAccessibilityService.currentAppCategory
                                    val pkg = com.lilt.mobile.accessibility.LiltAccessibilityService.currentForegroundPackage
                                    if (category != com.lilt.mobile.accessibility.LiltAccessibilityService.AppContextCategory.GENERAL) {
                                        "${category.label} ($pkg)"
                                    } else null
                                } else null

                                val cloudResult = CloudCleanupEngine.polish(
                                    rawText = textToClean,
                                    provider = settings.cloudProvider,
                                    apiKey = settings.cloudApiKey,
                                    model = settings.cloudModel,
                                    tone = settings.activeTone,
                                    customVocabulary = customVocabList,
                                    replacements = replacementMap,
                                    voiceCommands = settings.getVoiceCommands(),
                                    languageMode = effectiveLangMode,
                                    enabledLanguages = enabledCodes,
                                    appContext = detectedAppContext
                                )
                                if (cloudResult.isSuccess) {
                                    cleanedText = cloudResult.getOrThrow()
                                }
                            }

                            val shouldTransliterateTelugu = effectiveLangMode == "te_roman" ||
                                (effectiveLangMode == "auto" && enabledCodes.contains("te_roman") && !enabledCodes.contains("te"))

                            if (cleanedText.isBlank()) {
                                val rawCleaned = if (shouldTransliterateTelugu && textToClean.any { it in '\u0C00'..'\u0C7F' }) {
                                    CloudCleanupEngine.transliterateTeluguToRoman(textToClean)
                                } else textToClean

                                cleanedText = LiltCore.processText(
                                    raw = rawCleaned,
                                    cleanupLevel = settings.cleanupLevel,
                                    numbersAsDigits = settings.spokenNumbers,
                                    numberSymbols = settings.numberSymbols,
                                    spokenEmoji = settings.spokenEmoji,
                                    autoCapitalize = settings.autoCapitalize,
                                    trailingSpace = settings.trailingSpace,
                                    tone = settings.activeTone
                                )
                            }

                            if (shouldTransliterateTelugu && cleanedText.any { it in '\u0C00'..'\u0C7F' }) {
                                cleanedText = CloudCleanupEngine.transliterateTeluguToRoman(cleanedText)
                            }

                            cleanedText = settings.applyReplacementsAndSnippets(cleanedText)

                            val wordCount = cleanedText.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                            val rawWordCount = rawText.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                            val hesitationsRemoved = (rawWordCount - wordCount).coerceAtLeast(0)
                            settings.recordStats(words = wordCount, hesitations = hesitationsRemoved, stutters = 1)

                            val isNote = isNoteTrigger(rawText, cleanedText, settings.activeTone)
                            val contact = ContactExtractor.extract(cleanedText, rawText)
                            val item = DictationItem(
                                cleanedText = cleanedText,
                                rawText = rawText,
                                durationSeconds = 4,
                                tone = if (isNote) "notes" else settings.activeTone,
                                isNote = isNote,
                                contactName = contact?.name,
                                contactPhone = contact?.formattedDisplayPhone
                            )
                            settings.addHistory(item)
                            copyToClipboardIfEnabled(cleanedText)
                            checkAndScheduleReminder(cleanedText, rawText)

                            listener?.onFinalResult(cleanedText = cleanedText, rawText = rawText)
                            listener?.onStopped()
                            cleanupSpeechRecognizer()
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull() ?: ""
                        if (partial.isNotBlank()) {
                            listener?.onPartialResult(partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("te-IN", "en-IN", "hi-IN", "en-US"))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(tag, "Failed to start speech recognizer: ${e.message}", e)
            listener?.onError("Failed to start microphone: ${e.message}")
            listener?.onStopped()
            cleanupSpeechRecognizer()
        }
    }

    private fun stopSystemSpeechRecognizer() {
        if (!isListening) return
        isListening = false
        try {
            speechRecognizer?.stopListening()
            // Set 5-second safety timeout: if system recognizer hangs and never returns onResults, clean up
            recognizerSafetyTimeoutRunnable = Runnable {
                cleanupSpeechRecognizer()
                listener?.onError("No speech detected. Hold and speak for a moment.")
                listener?.onStopped()
            }
            mainHandler.postDelayed(recognizerSafetyTimeoutRunnable!!, 5000)
        } catch (e: Exception) {
            Log.e(tag, "Error stopping recognizer: ${e.message}")
            cleanupSpeechRecognizer()
        }
    }

    private fun clearRecognizerSafetyTimeout() {
        recognizerSafetyTimeoutRunnable?.let {
            mainHandler.removeCallbacks(it)
            recognizerSafetyTimeoutRunnable = null
        }
    }

    private fun cleanupSpeechRecognizer() {
        clearRecognizerSafetyTimeout()
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    fun stopListening() {
        if (isRecordingAudio) {
            stopCloudWhisperRecording()
        } else if (isListening) {
            stopSystemSpeechRecognizer()
        }
    }

    private fun copyToClipboardIfEnabled(text: String) {
        if (settings.autoCopyToClipboard && text.isNotBlank()) {
            try {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                    clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("Lilt Dictation", text))
                }
            } catch (e: Exception) {
                DiagnosticLogger.logError(tag, "Failed to auto-copy to clipboard: ${e.message}", e)
            }
        }
    }

    private fun checkAndScheduleReminder(cleanedText: String, rawText: String) {
        if (!settings.smartRemindersEnabled) return
        val textToScan = cleanedText.ifBlank { rawText }
        val parsed = TemporalParser.parse(textToScan) ?: TemporalParser.parse(rawText) ?: return

        if (settings.smartRemindersAskFirst) {
            // Safe opt-in mode: notify the listener to prompt the user (MainScreen chip or background action notification)
            mainHandler.post {
                listener?.onReminderDetected(parsed, textToScan)
            }
        } else {
            // Auto-schedule ONLY when explicit directive is used
            if (parsed.isExplicit) {
                val reminder = com.lilt.mobile.data.ReminderItem(
                    title = parsed.title,
                    originalText = textToScan,
                    eventEpochMs = parsed.eventEpochMs,
                    triggerEpochMs = parsed.triggerEpochMs,
                    leadMinutes = parsed.leadMinutes
                )
                settings.addReminder(reminder)
                com.lilt.mobile.reminder.ReminderScheduler.schedule(context, reminder)

                val timeStr = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date(parsed.eventEpochMs))
                val leadStr = if (parsed.leadMinutes >= 60) "${parsed.leadMinutes / 60}h" else "${parsed.leadMinutes}m"
                mainHandler.post {
                    android.widget.Toast.makeText(
                        context,
                        "⏰ Reminder set: ${parsed.title} at $timeStr ($leadStr alert)",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                mainHandler.post {
                    listener?.onReminderDetected(parsed, textToScan)
                }
            }
        }
    }

    private fun isNoteTrigger(rawText: String, cleanedText: String, activeTone: String): Boolean {
        if (activeTone.equals("notes", ignoreCase = true)) return true
        val lowerRaw = rawText.lowercase().trim()
        val lowerClean = cleanedText.lowercase().trim()
        val triggers = listOf(
            "take a note", "make a note", "note down", "note to self",
            "quick note", "save a note", "note that", "create a note", "new note"
        )
        return triggers.any { lowerRaw.startsWith(it) || lowerClean.startsWith(it) || lowerRaw.contains(it) }
    }
}
