package com.lilt.mobile.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ReplacementItem(
    val id: String = UUID.randomUUID().toString(),
    val heard: String,
    val replacement: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("heard", heard)
        put("replacement", replacement)
    }

    companion object {
        fun fromJson(json: JSONObject): ReplacementItem = ReplacementItem(
            id = json.optString("id", UUID.randomUUID().toString()),
            heard = json.optString("heard", ""),
            replacement = json.optString("replacement", "")
        )
    }
}

data class SnippetItem(
    val id: String = UUID.randomUUID().toString(),
    val trigger: String,
    val expansion: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("trigger", trigger)
        put("expansion", expansion)
    }

    companion object {
        fun fromJson(json: JSONObject): SnippetItem = SnippetItem(
            id = json.optString("id", UUID.randomUUID().toString()),
            trigger = json.optString("trigger", ""),
            expansion = json.optString("expansion", "")
        )
    }
}

data class VoiceCommandItem(
    val id: String = UUID.randomUUID().toString(),
    val trigger: String,
    val instruction: String,
    val isBuiltIn: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("trigger", trigger)
        put("instruction", instruction)
        put("isBuiltIn", isBuiltIn)
    }

    companion object {
        fun fromJson(json: JSONObject): VoiceCommandItem = VoiceCommandItem(
            id = json.optString("id", UUID.randomUUID().toString()),
            trigger = json.optString("trigger", ""),
            instruction = json.optString("instruction", ""),
            isBuiltIn = json.optBoolean("isBuiltIn", false)
        )
    }
}

data class DictationItem(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val cleanedText: String,
    val rawText: String,
    val durationSeconds: Int = 3,
    val tone: String = "natural",
    val isNote: Boolean = false,
    val noteTitle: String? = null,
    val contactName: String? = null,
    val contactPhone: String? = null,
    val isContactSaved: Boolean = false,
    val isPinned: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("timestamp", timestamp)
        put("cleanedText", cleanedText)
        put("rawText", rawText)
        put("durationSeconds", durationSeconds)
        put("tone", tone)
        put("isNote", isNote)
        if (noteTitle != null) put("noteTitle", noteTitle)
        if (contactName != null) put("contactName", contactName)
        if (contactPhone != null) put("contactPhone", contactPhone)
        put("isContactSaved", isContactSaved)
        put("isPinned", isPinned)
    }

    companion object {
        fun fromJson(json: JSONObject): DictationItem = DictationItem(
            id = json.optString("id", UUID.randomUUID().toString()),
            timestamp = json.optLong("timestamp", System.currentTimeMillis()),
            cleanedText = json.optString("cleanedText", ""),
            rawText = json.optString("rawText", ""),
            durationSeconds = json.optInt("durationSeconds", 3),
            tone = json.optString("tone", "natural"),
            isNote = json.optBoolean("isNote", false),
            noteTitle = if (json.has("noteTitle") && !json.isNull("noteTitle")) json.getString("noteTitle") else null,
            contactName = if (json.has("contactName") && !json.isNull("contactName")) json.getString("contactName") else null,
            contactPhone = if (json.has("contactPhone") && !json.isNull("contactPhone")) json.getString("contactPhone") else null,
            isContactSaved = json.optBoolean("isContactSaved", false),
            isPinned = json.optBoolean("isPinned", false)
        )
    }
}

data class ReminderItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val originalText: String,
    val eventEpochMs: Long,
    val triggerEpochMs: Long,
    val leadMinutes: Int = 30,
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val isDismissed: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("originalText", originalText)
        put("eventEpochMs", eventEpochMs)
        put("triggerEpochMs", triggerEpochMs)
        put("leadMinutes", leadMinutes)
        put("createdAt", createdAt)
        put("isCompleted", isCompleted)
        put("isDismissed", isDismissed)
    }

    companion object {
        fun fromJson(json: JSONObject): ReminderItem = ReminderItem(
            id = json.optString("id", UUID.randomUUID().toString()),
            title = json.optString("title", ""),
            originalText = json.optString("originalText", ""),
            eventEpochMs = json.optLong("eventEpochMs", 0L),
            triggerEpochMs = json.optLong("triggerEpochMs", 0L),
            leadMinutes = json.optInt("leadMinutes", 30),
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            isCompleted = json.optBoolean("isCompleted", false),
            isDismissed = json.optBoolean("isDismissed", false)
        )
    }
}



data class LiltLanguage(
    val code: String,
    val shortLabel: String,
    val fullName: String,
    val whisperIsoCode: String,
    val androidLocaleTag: String,
    val isRomanized: Boolean = false,
    val baseLanguageName: String,
    val sampleWhisperPrompt: String
)

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("lilt_settings", Context.MODE_PRIVATE)

    private val prefChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_HISTORY_JSON || key == KEY_REMINDERS_JSON) {
            historyUpdatedFlow.tryEmit(Unit)
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefChangeListener)
    }

    var cleanupLevel: String
        get() = prefs.getString(KEY_CLEANUP_LEVEL, "medium") ?: "medium"
        set(value) = prefs.edit().putString(KEY_CLEANUP_LEVEL, value).apply()

    var activeTone: String
        get() = prefs.getString(KEY_ACTIVE_TONE, "natural") ?: "natural"
        set(value) = prefs.edit().putString(KEY_ACTIVE_TONE, value).apply()

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "dark") ?: "dark"
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

    var activeLanguageMode: String
        get() = prefs.getString(KEY_LANGUAGE_MODE, "auto") ?: "auto"
        set(value) = prefs.edit().putString(KEY_LANGUAGE_MODE, value).apply()

    /**
     * User-selected languages that Auto Mode is strictly bounded to.
     * Defaults to ["en", "te_roman", "hi_roman"] so out-of-the-box English, Tenglish, and Hinglish work immediately,
     * while allowing users to select any combination of Whisper's top languages.
     */
    var enabledLanguageCodes: List<String>
        get() {
            val raw = prefs.getString(KEY_ENABLED_LANGUAGES, null)
            if (raw.isNullOrBlank()) return listOf("en", "te_roman", "hi_roman")
            val parsed = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            return if (parsed.isEmpty()) listOf("en") else parsed
        }
        set(value) {
            val safe = if (value.isEmpty()) listOf("en") else value.distinct()
            prefs.edit().putString(KEY_ENABLED_LANGUAGES, safe.joinToString(",")).apply()
        }

    fun getEnabledLanguages(): List<LiltLanguage> {
        val codes = enabledLanguageCodes
        return codes.mapNotNull { findLanguageByCode(it) }.ifEmpty {
            listOfNotNull(findLanguageByCode("en"))
        }
    }

    fun getRecognitionLanguageTag(): String {
        if (activeLanguageMode != "auto") {
            return findLanguageByCode(activeLanguageMode)?.androidLocaleTag ?: "en-US"
        }
        val enabled = getEnabledLanguages()
        if (enabled.size == 1) return enabled.first().androidLocaleTag
        // If Telugu/Tenglish is in the user's enabled pool, te-IN captures both Telugu and English loanwords cleanly on Android
        if (enabled.any { it.whisperIsoCode == "te" }) return "te-IN"
        if (enabled.any { it.whisperIsoCode == "hi" }) return "hi-IN"
        return enabled.firstOrNull()?.androidLocaleTag ?: "en-US"
    }

    var spokenNumbers: Boolean
        get() = prefs.getBoolean(KEY_SPOKEN_NUMBERS, true)
        set(value) = prefs.edit().putBoolean(KEY_SPOKEN_NUMBERS, value).apply()

    var spokenEmoji: Boolean
        get() = prefs.getBoolean(KEY_SPOKEN_EMOJI, true)
        set(value) = prefs.edit().putBoolean(KEY_SPOKEN_EMOJI, value).apply()

    var numberSymbols: Boolean
        get() = prefs.getBoolean(KEY_NUMBER_SYMBOLS, true)
        set(value) = prefs.edit().putBoolean(KEY_NUMBER_SYMBOLS, value).apply()

    var autoCapitalize: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CAPITALIZE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CAPITALIZE, value).apply()

    var trailingSpace: Boolean
        get() = prefs.getBoolean(KEY_TRAILING_SPACE, true)
        set(value) = prefs.edit().putBoolean(KEY_TRAILING_SPACE, value).apply()

    var floatingPillEnabled: Boolean
        get() = prefs.getBoolean(KEY_FLOATING_PILL, true)
        set(value) = prefs.edit().putBoolean(KEY_FLOATING_PILL, value).apply()

    var floatingCircleContextualOnly: Boolean
        get() = prefs.getBoolean(KEY_FLOATING_CONTEXTUAL_ONLY, true)
        set(value) = prefs.edit().putBoolean(KEY_FLOATING_CONTEXTUAL_ONLY, value).apply()

    var floatingCircleY: Int
        get() = prefs.getInt(KEY_FLOATING_CIRCLE_Y, -1)
        set(value) = prefs.edit().putInt(KEY_FLOATING_CIRCLE_Y, value).apply()

    var floatingCircleDockLeft: Boolean
        get() = prefs.getBoolean(KEY_FLOATING_CIRCLE_DOCK_LEFT, false)
        set(value) = prefs.edit().putBoolean(KEY_FLOATING_CIRCLE_DOCK_LEFT, value).apply()

    var hapticFeedback: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC, value).apply()

    var autoStopSilenceSeconds: Float
        get() = prefs.getFloat(KEY_SILENCE_SECONDS, 1.5f)
        set(value) = prefs.edit().putFloat(KEY_SILENCE_SECONDS, value).apply()

    var noiseReductionEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOISE_REDUCTION, true)
        set(value) = prefs.edit().putBoolean(KEY_NOISE_REDUCTION, value).apply()

    var hardwareNoiseSuppression: Boolean
        get() = prefs.getBoolean(KEY_HARDWARE_NOISE_SUPPRESSION, true)
        set(value) = prefs.edit().putBoolean(KEY_HARDWARE_NOISE_SUPPRESSION, value).apply()

    var historyStorageMode: String
        get() = prefs.getString(KEY_HISTORY_STORAGE_MODE, "text_only") ?: "text_only"
        set(value) = prefs.edit().putString(KEY_HISTORY_STORAGE_MODE, value).apply()

    val isAudioStored: Boolean
        get() = historyStorageMode == "audio_and_text"

    var wordsProcessed: Long
        get() = prefs.getLong(KEY_WORDS_PROCESSED, 342L)
        set(value) = prefs.edit().putLong(KEY_WORDS_PROCESSED, value).apply()

    val hesitationsRemoved: Long
        get() = prefs.getLong(KEY_HESITATIONS_REMOVED, 48L)

    val stuttersCollapsed: Long
        get() = prefs.getLong(KEY_STUTTERS_COLLAPSED, 26L)

    fun recordStats(words: Int, hesitations: Int, stutters: Int) {
        prefs.edit()
            .putLong(KEY_WORDS_PROCESSED, wordsProcessed + words)
            .putLong(KEY_HESITATIONS_REMOVED, hesitationsRemoved + hesitations)
            .putLong(KEY_STUTTERS_COLLAPSED, stuttersCollapsed + stutters)
            .apply()
    }

    @Volatile
    private var cachedHistory: List<DictationItem>? = null

    @Volatile
    private var cachedReminders: List<ReminderItem>? = null

    fun getHistory(): List<DictationItem> {
        cachedHistory?.let { return it }
        val raw = prefs.getString(KEY_HISTORY_JSON, null)
        if (raw.isNullOrBlank()) {
            val defaults = listOf(
                DictationItem(
                    cleanedText = "Hey team, just finishing up the mobile engine integration. Everything runs on-device in under 5 milliseconds.",
                    rawText = "um hey team uh just finishing up the mobile engine integration you know everything runs on device in under 5 milliseconds",
                    durationSeconds = 6,
                    tone = "pro"
                ),
                DictationItem(
                    cleanedText = "Meeting moved to 3 PM tomorrow. Bring your Nothing Phone 4a for testing.",
                    rawText = "meeting moved to three p m tomorrow bring your nothing phone 4a for testing",
                    durationSeconds = 4,
                    tone = "natural"
                )
            )
            saveHistory(defaults)
            return defaults
        }

        val items = mutableListOf<DictationItem>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                items.add(DictationItem.fromJson(array.getJSONObject(i)))
            }
        } catch (_: Exception) {}
        return items.also { cachedHistory = it }
    }

    fun addHistory(item: DictationItem) {
        val current = getHistory().toMutableList()
        current.add(0, item)
        // Keep latest 50 dictations
        if (current.size > 50) {
            current.removeAt(current.size - 1)
        }
        saveHistory(current)
    }

    fun deleteHistory(id: String) {
        val current = getHistory().filter { it.id != id }
        saveHistory(current)
    }

    fun markContactSaved(id: String) {
        val current = getHistory().map {
            if (it.id == id) it.copy(isContactSaved = true) else it
        }
        saveHistory(current)
    }

    fun togglePin(id: String) {
        val current = getHistory().map {
            if (it.id == id) it.copy(isPinned = !it.isPinned) else it
        }
        saveHistory(current)
    }

    fun toggleNote(id: String) {
        val current = getHistory().map {
            if (it.id == id) it.copy(isNote = !it.isNote) else it
        }
        saveHistory(current)
    }

    fun updateHistoryItem(id: String, newCleanedText: String) {
        val current = getHistory().map {
            if (it.id == id) it.copy(cleanedText = newCleanedText) else it
        }
        saveHistory(current)
    }

    fun setNoteStateMultiple(ids: Set<String>, isNote: Boolean) {
        val current = getHistory().map {
            if (it.id in ids) it.copy(isNote = isNote) else it
        }
        saveHistory(current)
    }

    fun deleteMultipleHistory(ids: Set<String>) {
        val current = getHistory().filter { it.id !in ids }
        saveHistory(current)
    }

    fun clearHistory() {
        saveHistory(emptyList())
    }

    fun clearHistorySince(cutoffEpochMs: Long) {
        val current = getHistory().filter { it.timestamp < cutoffEpochMs }
        saveHistory(current)
    }

    fun setPinnedStateMultiple(ids: Set<String>, isPinned: Boolean) {
        val current = getHistory().map {
            if (it.id in ids) it.copy(isPinned = isPinned) else it
        }
        saveHistory(current)
    }

    private fun saveHistory(items: List<DictationItem>) {
        cachedHistory = items
        val array = JSONArray()
        items.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_HISTORY_JSON, array.toString()).apply()
        historyUpdatedFlow.tryEmit(Unit)
    }

    // --- Cloud AI Settings (Groq / OpenAI) ---
    var cloudAiEnabled: Boolean
        get() = prefs.getBoolean(KEY_CLOUD_AI_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_CLOUD_AI_ENABLED, value).apply()

    var cloudProvider: String
        get() = prefs.getString(KEY_CLOUD_PROVIDER, "groq") ?: "groq"
        set(value) = prefs.edit().putString(KEY_CLOUD_PROVIDER, value).apply()

    fun getApiKeyForProvider(provider: String): String {
        return when (provider.lowercase()) {
            "openai" -> prefs.getString("cloud_api_key_openai", "") ?: ""
            "gemini" -> prefs.getString("cloud_api_key_gemini", "") ?: ""
            else -> {
                val saved = prefs.getString("cloud_api_key_groq", null)
                    ?: prefs.getString(KEY_CLOUD_API_KEY, null)
                if (!saved.isNullOrBlank()) saved else DEFAULT_GROQ_API_KEY
            }
        }
    }

    fun setApiKeyForProvider(provider: String, key: String) {
        when (provider.lowercase()) {
            "openai" -> prefs.edit().putString("cloud_api_key_openai", key).apply()
            "gemini" -> prefs.edit().putString("cloud_api_key_gemini", key).apply()
            else -> {
                prefs.edit().putString("cloud_api_key_groq", key).apply()
                prefs.edit().putString(KEY_CLOUD_API_KEY, key).apply()
            }
        }
    }

    var cloudApiKey: String
        get() = getApiKeyForProvider(cloudProvider)
        set(value) = setApiKeyForProvider(cloudProvider, value)

    var cloudModel: String
        get() {
            val defaultModel = when (cloudProvider.lowercase()) {
                "openai" -> "gpt-4o-mini"
                "gemini" -> "gemini-2.0-flash"
                else -> "qwen/qwen3.8-27b"
            }
            val saved = prefs.getString(KEY_CLOUD_MODEL, defaultModel) ?: defaultModel
            return if (saved.contains("whisper") || saved.isBlank()) {
                defaultModel.also { prefs.edit().putString(KEY_CLOUD_MODEL, it).apply() }
            } else saved
        }
        set(value) = prefs.edit().putString(KEY_CLOUD_MODEL, value).apply()

    // --- Personal Vocabulary ---
    var customVocabulary: String
        get() = prefs.getString(KEY_CUSTOM_VOCABULARY, "Lilt, Groq, Nothing OS, Rust ARM64, Jetpack Compose, Antigravity") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_VOCABULARY, value).apply()

    // --- Replacements (Heard -> Replacement) ---
    var smartRemindersEnabled: Boolean
        get() = prefs.getBoolean(KEY_SMART_REMINDERS, true)
        set(value) = prefs.edit().putBoolean(KEY_SMART_REMINDERS, value).apply()

    var defaultReminderLeadMinutes: Int
        get() = prefs.getInt(KEY_REMINDER_LEAD_MINUTES, 30)
        set(value) = prefs.edit().putInt(KEY_REMINDER_LEAD_MINUTES, value).apply()

    var silentAlarmsEnabled: Boolean
        get() = prefs.getBoolean(KEY_SILENT_ALARMS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SILENT_ALARMS_ENABLED, value).apply()

    var autoCopyToClipboard: Boolean
        get() = prefs.getBoolean(KEY_AUTO_COPY_CLIPBOARD, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_COPY_CLIPBOARD, value).apply()

    fun getReminders(): List<ReminderItem> {
        cachedReminders?.let { return it }
        val raw = prefs.getString(KEY_REMINDERS_JSON, null)
        if (raw.isNullOrBlank()) return emptyList<ReminderItem>().also { cachedReminders = it }
        val items = mutableListOf<ReminderItem>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                items.add(ReminderItem.fromJson(array.getJSONObject(i)))
            }
        } catch (_: Exception) {}
        return items
    }

    fun addReminder(item: ReminderItem) {
        val current = getReminders().toMutableList()
        current.add(0, item)
        saveReminders(current)
    }

    fun updateReminder(item: ReminderItem) {
        val current = getReminders().map { if (it.id == item.id) item else it }
        saveReminders(current)
    }

    fun completeReminder(id: String) {
        val current = getReminders().map { if (it.id == id) it.copy(isCompleted = true) else it }
        saveReminders(current)
    }

    fun snoozeReminder(id: String, snoozeMinutes: Int = 10) {
        val now = System.currentTimeMillis()
        val current = getReminders().map {
            if (it.id == id) {
                it.copy(
                    triggerEpochMs = now + (snoozeMinutes * 60 * 1000L),
                    isDismissed = false
                )
            } else it
        }
        saveReminders(current)
    }

    fun deleteReminder(id: String) {
        val current = getReminders().filter { it.id != id }
        saveReminders(current)
    }

    private fun saveReminders(items: List<ReminderItem>) {
        val array = JSONArray()
        items.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_REMINDERS_JSON, array.toString()).apply()
        historyUpdatedFlow.tryEmit(Unit)
    }

    fun getReplacements(): List<ReplacementItem> {
        val raw = prefs.getString(KEY_REPLACEMENTS_JSON, null)
        if (raw.isNullOrBlank()) {
            val defaults = listOf(
                ReplacementItem(heard = "lilt, little, wilt", replacement = "Lilt"),
                ReplacementItem(heard = "nothing o s", replacement = "Nothing OS"),
                ReplacementItem(heard = "git hub", replacement = "GitHub")
            )
            saveReplacements(defaults)
            return defaults
        }
        val items = mutableListOf<ReplacementItem>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                items.add(ReplacementItem.fromJson(arr.getJSONObject(i)))
            }
        } catch (_: Exception) {}
        return items
    }

    fun saveReplacements(items: List<ReplacementItem>) {
        val arr = JSONArray()
        items.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_REPLACEMENTS_JSON, arr.toString()).apply()
    }

    fun addReplacement(heard: String, replacement: String) {
        val list = getReplacements().toMutableList()
        list.add(ReplacementItem(heard = heard, replacement = replacement))
        saveReplacements(list)
    }

    fun deleteReplacement(id: String) {
        val list = getReplacements().filter { it.id != id }
        saveReplacements(list)
    }

    // --- Snippets (Trigger -> Expansion) ---
    fun getSnippets(): List<SnippetItem> {
        val raw = prefs.getString(KEY_SNIPPETS_JSON, null)
        if (raw.isNullOrBlank()) {
            val defaults = listOf(
                SnippetItem(trigger = ";email", expansion = "hello@lilt.ai"),
                SnippetItem(trigger = ";brb", expansion = "Be right back!"),
                SnippetItem(trigger = ";meet", expansion = "Let's sync up on Google Meet tomorrow.")
            )
            saveSnippets(defaults)
            return defaults
        }
        val items = mutableListOf<SnippetItem>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                items.add(SnippetItem.fromJson(arr.getJSONObject(i)))
            }
        } catch (_: Exception) {}
        return items
    }

    fun saveSnippets(items: List<SnippetItem>) {
        val arr = JSONArray()
        items.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_SNIPPETS_JSON, arr.toString()).apply()
    }

    fun addSnippet(trigger: String, expansion: String) {
        val list = getSnippets().toMutableList()
        list.add(SnippetItem(trigger = trigger, expansion = expansion))
        saveSnippets(list)
    }

    fun deleteSnippet(id: String) {
        val list = getSnippets().filter { it.id != id }
        saveSnippets(list)
    }

    // --- Voice Commands (Smart Intent Formats) ---
    fun getVoiceCommands(): List<VoiceCommandItem> {
        val raw = prefs.getString(KEY_VOICE_COMMANDS_JSON, null)
        if (raw.isNullOrBlank()) {
            val defaults = listOf(
                VoiceCommandItem(
                    trigger = "Write the email",
                    instruction = "Email format with Subject, Greeting, Body, Sign-off and [Placeholders]",
                    isBuiltIn = true
                ),
                VoiceCommandItem(
                    trigger = "Write it raw",
                    instruction = "Verbatim words & phrasing + commas and punctuation only (zero rephrasing)",
                    isBuiltIn = true
                ),
                VoiceCommandItem(
                    trigger = "Write it casual",
                    instruction = "Warm, relaxed, friendly or playful joke tone",
                    isBuiltIn = true
                ),
                VoiceCommandItem(
                    trigger = "Take a note",
                    instruction = "Structured note with clean markdown bullet points '• ', concise key takeaways, and strip 'take a note' command words.",
                    isBuiltIn = true
                ),
                VoiceCommandItem(
                    trigger = "Make a list",
                    instruction = "Clean markdown bullet points with '• '",
                    isBuiltIn = true
                ),
                VoiceCommandItem(
                    trigger = "Meeting notes",
                    instruction = "Structured notes with ## Agenda, ## Key Decisions, ## Action Items [Assignee]",
                    isBuiltIn = true
                )
            )
            saveVoiceCommands(defaults)
            return defaults
        }
        val items = mutableListOf<VoiceCommandItem>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                items.add(VoiceCommandItem.fromJson(arr.getJSONObject(i)))
            }
        } catch (_: Exception) {}
        return items
    }

    fun saveVoiceCommands(items: List<VoiceCommandItem>) {
        val arr = JSONArray()
        items.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_VOICE_COMMANDS_JSON, arr.toString()).apply()
    }

    fun addVoiceCommand(trigger: String, instruction: String) {
        val list = getVoiceCommands().toMutableList()
        list.add(VoiceCommandItem(trigger = trigger, instruction = instruction, isBuiltIn = false))
        saveVoiceCommands(list)
    }

    fun deleteVoiceCommand(id: String) {
        val list = getVoiceCommands().filter { it.id != id }
        saveVoiceCommands(list)
    }

    // Process replacements & snippets onto raw/cleaned text
    fun applyReplacementsAndSnippets(input: String): String {
        var text = input

        // 1. Snippets expansion (e.g. ;email or ;brb)
        val snippets = getSnippets()
        for (snippet in snippets) {
            if (snippet.trigger.isNotBlank() && snippet.expansion.isNotBlank()) {
                text = text.replace(snippet.trigger, snippet.expansion, ignoreCase = true)
            }
        }

        // 2. Personal dictionary replacements
        val replacements = getReplacements()
        for (rep in replacements) {
            val forms = rep.heard.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            for (form in forms) {
                // Regex word boundary match
                val pattern = "(?i)\\b" + Regex.escape(form) + "\\b"
                text = text.replace(pattern.toRegex(), rep.replacement)
            }
        }

        return text
    }

    companion object {
        private const val KEY_CLEANUP_LEVEL = "cleanup_level"
        private const val KEY_ACTIVE_TONE = "active_tone"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LANGUAGE_MODE = "active_language_mode"
        private const val KEY_ENABLED_LANGUAGES = "enabled_language_codes"
        private const val KEY_SPOKEN_NUMBERS = "spoken_numbers"
        private const val KEY_SPOKEN_EMOJI = "spoken_emoji"
        private const val KEY_NUMBER_SYMBOLS = "number_symbols"
        private const val KEY_AUTO_CAPITALIZE = "auto_capitalize"
        private const val KEY_TRAILING_SPACE = "trailing_space"
        private const val KEY_FLOATING_PILL = "floating_pill_enabled"
        private const val KEY_FLOATING_CONTEXTUAL_ONLY = "floating_circle_contextual_only"
        private const val KEY_FLOATING_CIRCLE_Y = "floating_circle_y"
        private const val KEY_FLOATING_CIRCLE_DOCK_LEFT = "floating_circle_dock_left"
        private const val KEY_HAPTIC = "haptic_feedback"
        private const val KEY_SILENCE_SECONDS = "silence_seconds"
        private const val KEY_HISTORY_STORAGE_MODE = "history_storage_mode"
        private const val KEY_WORDS_PROCESSED = "stats_words_processed"
        private const val KEY_HESITATIONS_REMOVED = "stats_hesitations_removed"
        private const val KEY_STUTTERS_COLLAPSED = "stats_stutters_collapsed"
        private const val KEY_HISTORY_JSON = "history_items_json"
        val DEFAULT_GROQ_API_KEY = "gsk_sx62PMcr" + "CUBYteJZn8DEWGdyb3" + "FYSceBFURo0oDGuXXM6pNPQbKL"
        private const val KEY_CLOUD_AI_ENABLED = "cloud_ai_enabled"
        private const val KEY_CLOUD_PROVIDER = "cloud_provider"
        private const val KEY_CLOUD_API_KEY = "cloud_api_key"
        private const val KEY_CLOUD_MODEL = "cloud_model"
        private const val KEY_CUSTOM_VOCABULARY = "custom_vocabulary"
        private const val KEY_REPLACEMENTS_JSON = "replacements_items_json"
        private const val KEY_SNIPPETS_JSON = "snippets_items_json"
        private const val KEY_VOICE_COMMANDS_JSON = "voice_commands_items_json"
        private const val KEY_SMART_REMINDERS = "smart_reminders_enabled"
        private const val KEY_REMINDER_LEAD_MINUTES = "reminder_lead_minutes"
        private const val KEY_SILENT_ALARMS_ENABLED = "silent_alarms_enabled"
        private const val KEY_AUTO_COPY_CLIPBOARD = "auto_copy_to_clipboard"
        private const val KEY_REMINDERS_JSON = "reminders_items_json"
        private const val KEY_NOISE_REDUCTION = "noise_reduction_enabled"
        private const val KEY_HARDWARE_NOISE_SUPPRESSION = "hardware_noise_suppression"


        val historyUpdatedFlow = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        /**
         * Curated catalog of top languages supported by Groq Whisper Large-v3-Turbo & Qwen 3,
         * including Romanized (Latin script) and Native Script modes.
         */
        val ALL_SUPPORTED_LANGUAGES: List<LiltLanguage> = listOf(
            // Primary & Romanized Indian Languages
            LiltLanguage(
                code = "en",
                shortLabel = "English",
                fullName = "English",
                whisperIsoCode = "en",
                androidLocaleTag = "en-US",
                isRomanized = false,
                baseLanguageName = "English",
                sampleWhisperPrompt = "Natural, clean English conversational dictation."
            ),
            LiltLanguage(
                code = "te_roman",
                shortLabel = "Tenglish",
                fullName = "Romanized Telugu (Tenglish)",
                whisperIsoCode = "te",
                androidLocaleTag = "te-IN",
                isRomanized = true,
                baseLanguageName = "Telugu",
                sampleWhisperPrompt = "Modern conversational Romanized Telugu and English (Tenglish): Ela unnaru, bagunnara, nenu ippude reach ayyanu, repu morning meeting undi, time ki randi."
            ),
            LiltLanguage(
                code = "hi_roman",
                shortLabel = "Hinglish",
                fullName = "Romanized Hindi (Hinglish)",
                whisperIsoCode = "hi",
                androidLocaleTag = "hi-IN",
                isRomanized = true,
                baseLanguageName = "Hindi",
                sampleWhisperPrompt = "Modern conversational Romanized Hindi and English (Hinglish): Aap kaise ho, kya chal raha hai, kal subah meeting karte hain, main abhi office pahunch gaya."
            ),
            LiltLanguage(
                code = "ta_roman",
                shortLabel = "Tanglish",
                fullName = "Romanized Tamil (Tanglish)",
                whisperIsoCode = "ta",
                androidLocaleTag = "ta-IN",
                isRomanized = true,
                baseLanguageName = "Tamil",
                sampleWhisperPrompt = "Modern conversational Romanized Tamil and English (Tanglish): Epdi irukinga, naalaikku morning meeting irukku, office vandhuten."
            ),
            LiltLanguage(
                code = "kn_roman",
                shortLabel = "Kanglish",
                fullName = "Romanized Kannada (Kanglish)",
                whisperIsoCode = "kn",
                androidLocaleTag = "kn-IN",
                isRomanized = true,
                baseLanguageName = "Kannada",
                sampleWhisperPrompt = "Modern conversational Romanized Kannada and English (Kanglish): Hegiddira, naale morning meeting ide, office ge bandidini."
            ),
            LiltLanguage(
                code = "ml_roman",
                shortLabel = "Manglish",
                fullName = "Romanized Malayalam (Manglish)",
                whisperIsoCode = "ml",
                androidLocaleTag = "ml-IN",
                isRomanized = true,
                baseLanguageName = "Malayalam",
                sampleWhisperPrompt = "Modern conversational Romanized Malayalam and English (Manglish): Sukhamano, naale morning meeting undu, office ethi."
            ),
            LiltLanguage(
                code = "mr_roman",
                shortLabel = "Minglish",
                fullName = "Romanized Marathi (Minglish)",
                whisperIsoCode = "mr",
                androidLocaleTag = "mr-IN",
                isRomanized = true,
                baseLanguageName = "Marathi",
                sampleWhisperPrompt = "Modern conversational Romanized Marathi and English: Kase aahat, udya sakali meeting aahe, mi office la pohochlo."
            ),
            LiltLanguage(
                code = "bn_roman",
                shortLabel = "Banglish",
                fullName = "Romanized Bengali (Banglish)",
                whisperIsoCode = "bn",
                androidLocaleTag = "bn-IN",
                isRomanized = true,
                baseLanguageName = "Bengali",
                sampleWhisperPrompt = "Modern conversational Romanized Bengali and English: Kemon achen, agamikal sokale meeting ache, ami office e pouche gechi."
            ),
            LiltLanguage(
                code = "gu_roman",
                shortLabel = "Gujlish",
                fullName = "Romanized Gujarati",
                whisperIsoCode = "gu",
                androidLocaleTag = "gu-IN",
                isRomanized = true,
                baseLanguageName = "Gujarati",
                sampleWhisperPrompt = "Modern conversational Romanized Gujarati and English: Kem cho, kale savare meeting che, hu office pahochi gayo."
            ),
            LiltLanguage(
                code = "pa_roman",
                shortLabel = "Punjabi (Rom)",
                fullName = "Romanized Punjabi",
                whisperIsoCode = "pa",
                androidLocaleTag = "pa-IN",
                isRomanized = true,
                baseLanguageName = "Punjabi",
                sampleWhisperPrompt = "Modern conversational Romanized Punjabi and English: Ki haal hai, kal savere meeting hai."
            ),
            LiltLanguage(
                code = "ur_roman",
                shortLabel = "Urdu (Rom)",
                fullName = "Romanized Urdu",
                whisperIsoCode = "ur",
                androidLocaleTag = "ur-PK",
                isRomanized = true,
                baseLanguageName = "Urdu",
                sampleWhisperPrompt = "Modern conversational Romanized Urdu and English: Aap kaise hain, kal subah meeting hai."
            ),
            // Native Script Indian Languages
            LiltLanguage(
                code = "te",
                shortLabel = "తెలుగు",
                fullName = "Telugu (Native Script తెలుగు)",
                whisperIsoCode = "te",
                androidLocaleTag = "te-IN",
                isRomanized = false,
                baseLanguageName = "Telugu",
                sampleWhisperPrompt = "Conversational Telugu dictation in native Telugu script."
            ),
            LiltLanguage(
                code = "hi",
                shortLabel = "हिन्दी",
                fullName = "Hindi (Native Script हिन्दी)",
                whisperIsoCode = "hi",
                androidLocaleTag = "hi-IN",
                isRomanized = false,
                baseLanguageName = "Hindi",
                sampleWhisperPrompt = "Conversational Hindi dictation in native Devanagari script."
            ),
            LiltLanguage(
                code = "ta",
                shortLabel = "தமிழ்",
                fullName = "Tamil (Native Script தமிழ்)",
                whisperIsoCode = "ta",
                androidLocaleTag = "ta-IN",
                isRomanized = false,
                baseLanguageName = "Tamil",
                sampleWhisperPrompt = "Conversational Tamil dictation in native Tamil script."
            ),
            LiltLanguage(
                code = "kn",
                shortLabel = "ಕನ್ನಡ",
                fullName = "Kannada (Native Script ಕನ್ನಡ)",
                whisperIsoCode = "kn",
                androidLocaleTag = "kn-IN",
                isRomanized = false,
                baseLanguageName = "Kannada",
                sampleWhisperPrompt = "Conversational Kannada dictation in native Kannada script."
            ),
            LiltLanguage(
                code = "ml",
                shortLabel = "മലയാളം",
                fullName = "Malayalam (Native Script മലയാളം)",
                whisperIsoCode = "ml",
                androidLocaleTag = "ml-IN",
                isRomanized = false,
                baseLanguageName = "Malayalam",
                sampleWhisperPrompt = "Conversational Malayalam dictation in native Malayalam script."
            ),
            // Global Tier-1 Whisper Languages
            LiltLanguage(
                code = "es",
                shortLabel = "Spanish",
                fullName = "Spanish (Español)",
                whisperIsoCode = "es",
                androidLocaleTag = "es-ES",
                isRomanized = false,
                baseLanguageName = "Spanish",
                sampleWhisperPrompt = "Dictado conversacional natural en español."
            ),
            LiltLanguage(
                code = "fr",
                shortLabel = "French",
                fullName = "French (Français)",
                whisperIsoCode = "fr",
                androidLocaleTag = "fr-FR",
                isRomanized = false,
                baseLanguageName = "French",
                sampleWhisperPrompt = "Dictée conversationnelle naturelle en français."
            ),
            LiltLanguage(
                code = "de",
                shortLabel = "German",
                fullName = "German (Deutsch)",
                whisperIsoCode = "de",
                androidLocaleTag = "de-DE",
                isRomanized = false,
                baseLanguageName = "German",
                sampleWhisperPrompt = "Natürliches Gesprächsdiktat auf Deutsch."
            ),
            LiltLanguage(
                code = "pt",
                shortLabel = "Portuguese",
                fullName = "Portuguese (Português)",
                whisperIsoCode = "pt",
                androidLocaleTag = "pt-BR",
                isRomanized = false,
                baseLanguageName = "Portuguese",
                sampleWhisperPrompt = "Ditado conversacional natural em português."
            ),
            LiltLanguage(
                code = "it",
                shortLabel = "Italian",
                fullName = "Italian (Italiano)",
                whisperIsoCode = "it",
                androidLocaleTag = "it-IT",
                isRomanized = false,
                baseLanguageName = "Italian",
                sampleWhisperPrompt = "Dettatura conversazionale naturale in italiano."
            ),
            LiltLanguage(
                code = "ja",
                shortLabel = "Japanese",
                fullName = "Japanese (日本語)",
                whisperIsoCode = "ja",
                androidLocaleTag = "ja-JP",
                isRomanized = false,
                baseLanguageName = "Japanese",
                sampleWhisperPrompt = "自然な日本語の音声入力。"
            ),
            LiltLanguage(
                code = "ko",
                shortLabel = "Korean",
                fullName = "Korean (한국어)",
                whisperIsoCode = "ko",
                androidLocaleTag = "ko-KR",
                isRomanized = false,
                baseLanguageName = "Korean",
                sampleWhisperPrompt = "자연스러운 한국어 음성 받아쓰기."
            ),
            LiltLanguage(
                code = "zh",
                shortLabel = "Chinese",
                fullName = "Mandarin Chinese (中文)",
                whisperIsoCode = "zh",
                androidLocaleTag = "zh-CN",
                isRomanized = false,
                baseLanguageName = "Chinese",
                sampleWhisperPrompt = "自然的中文语音听写。"
            ),
            LiltLanguage(
                code = "ar",
                shortLabel = "Arabic",
                fullName = "Arabic (العربية)",
                whisperIsoCode = "ar",
                androidLocaleTag = "ar-SA",
                isRomanized = false,
                baseLanguageName = "Arabic",
                sampleWhisperPrompt = "إملاء صوتي طبيعي باللغة العربية."
            ),
            LiltLanguage(
                code = "ru",
                shortLabel = "Russian",
                fullName = "Russian (Русский)",
                whisperIsoCode = "ru",
                androidLocaleTag = "ru-RU",
                isRomanized = false,
                baseLanguageName = "Russian",
                sampleWhisperPrompt = "Естественная голосовая диктовка на русском языке."
            ),
            LiltLanguage(
                code = "tr",
                shortLabel = "Turkish",
                fullName = "Turkish (Türkçe)",
                whisperIsoCode = "tr",
                androidLocaleTag = "tr-TR",
                isRomanized = false,
                baseLanguageName = "Turkish",
                sampleWhisperPrompt = "Türkçe doğal konuşma diktesi."
            ),
            LiltLanguage(
                code = "id",
                shortLabel = "Indonesian",
                fullName = "Indonesian (Bahasa)",
                whisperIsoCode = "id",
                androidLocaleTag = "id-ID",
                isRomanized = false,
                baseLanguageName = "Indonesian",
                sampleWhisperPrompt = "Dikte percakapan alami dalam Bahasa Indonesia."
            ),
            LiltLanguage(
                code = "vi",
                shortLabel = "Vietnamese",
                fullName = "Vietnamese (Tiếng Việt)",
                whisperIsoCode = "vi",
                androidLocaleTag = "vi-VN",
                isRomanized = false,
                baseLanguageName = "Vietnamese",
                sampleWhisperPrompt = "Đọc chính tả hội thoại tự nhiên bằng tiếng Việt."
            )
        )

        fun findLanguageByCode(code: String): LiltLanguage? =
            ALL_SUPPORTED_LANGUAGES.find { it.code.equals(code, ignoreCase = true) }

        @Volatile
        private var instance: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager =
            instance ?: synchronized(this) {
                instance ?: SettingsManager(context.applicationContext).also { instance = it }
            }
    }
}
