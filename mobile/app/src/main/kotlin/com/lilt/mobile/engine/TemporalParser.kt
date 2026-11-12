package com.lilt.mobile.engine

import java.util.Calendar

data class ParsedReminder(
    val title: String,
    val eventEpochMs: Long,
    val triggerEpochMs: Long,
    val leadMinutes: Int,
    val isExplicit: Boolean = false
)

/**
 * High-speed on-device Multilingual Temporal & Action-Item Parser.
 * Detects scheduled tasks, deadlines, meetings, and commitments across English, Tenglish, and Hinglish.
 * Calculates adaptive lead-time alerts (15m meetings, 1h medical, 2h travel, 30m default).
 */
object TemporalParser {

    private val intentKeywords = setOf(
        "remind", "reminder", "meeting", "call", "appointment", "dentist", "doctor",
        "hospital", "flight", "airport", "train", "interview", "schedule", "deadline",
        "pay", "bill", "zoom", "sync",
        // Telugu / Tenglish
        "vellali", "cheyali", "undi", "pettandi", "matladali", "podam",
        // Hindi / Hinglish
        "karna", "milna", "dena", "jaana"
    )

    fun parse(text: String, nowEpochMs: Long = System.currentTimeMillis()): ParsedReminder? {
        val trimmed = text.trim()
        if (trimmed.length < 5) return null
        val lower = trimmed.lowercase()

        val hasIntent = intentKeywords.any { lower.contains(it) }
        val hasTemporalMarker = Regex("(?i)\\b(in\\s+\\d+|tomorrow|repu|kal|today|tonight|at\\s+\\d+|\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)|half\\s+an\\s+hour|araganta|aadhe\\s+ghante)\\b").containsMatchIn(lower)

        if (!hasIntent && !hasTemporalMarker) {
            return null
        }

        val cal = Calendar.getInstance().apply { timeInMillis = nowEpochMs }

        // 1. Check relative duration ("in 15 minutes", "in 2 hours", "in half an hour")
        val relMinMatch = Regex("(?i)\\bin\\s+(\\d+)\\s*(?:mins|minutes)\\b").find(lower)
        if (relMinMatch != null) {
            val mins = relMinMatch.groupValues[1].toIntOrNull() ?: 15
            cal.add(Calendar.MINUTE, mins)
            return buildResult(trimmed, cal.timeInMillis, nowEpochMs)
        }

        val relHrMatch = Regex("(?i)\\bin\\s+(\\d+)\\s*(?:hour|hours|hrs)\\b").find(lower)
        if (relHrMatch != null) {
            val hrs = relHrMatch.groupValues[1].toIntOrNull() ?: 1
            cal.add(Calendar.HOUR_OF_DAY, hrs)
            return buildResult(trimmed, cal.timeInMillis, nowEpochMs)
        }

        if (lower.contains("half an hour") || lower.contains("araganta") || lower.contains("aadhe ghante")) {
            cal.add(Calendar.MINUTE, 30)
            return buildResult(trimmed, cal.timeInMillis, nowEpochMs)
        }

        // 2. Target Date resolution (tomorrow, repu, kal, day after tomorrow, ellundi, parso)
        if (Regex("(?i)\\b(day\\s+after\\s+tomorrow|ellundi|parso)\\b").containsMatchIn(lower)) {
            cal.add(Calendar.DAY_OF_YEAR, 2)
        } else if (Regex("(?i)\\b(tomorrow|repu|kal)\\b").containsMatchIn(lower)) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        } else {
            // Check day of week (e.g. "on Friday", "this Sunday")
            val dayOfWeekMap = mapOf(
                "sunday" to Calendar.SUNDAY, "aadavaram" to Calendar.SUNDAY, "ravivar" to Calendar.SUNDAY,
                "monday" to Calendar.MONDAY, "somavaram" to Calendar.MONDAY, "somwar" to Calendar.MONDAY,
                "tuesday" to Calendar.TUESDAY, "mangalavaram" to Calendar.TUESDAY, "mangalwar" to Calendar.TUESDAY,
                "wednesday" to Calendar.WEDNESDAY, "budhavaram" to Calendar.WEDNESDAY, "budhwar" to Calendar.WEDNESDAY,
                "thursday" to Calendar.THURSDAY, "guruvaram" to Calendar.THURSDAY, "guruwar" to Calendar.THURSDAY,
                "friday" to Calendar.FRIDAY, "sukravaram" to Calendar.FRIDAY, "shukrawar" to Calendar.FRIDAY,
                "saturday" to Calendar.SATURDAY, "sanivaram" to Calendar.SATURDAY, "shaniwar" to Calendar.SATURDAY
            )
            for ((dayName, dayCode) in dayOfWeekMap) {
                if (Regex("(?i)\\b$dayName\\b").containsMatchIn(lower)) {
                    val currentDay = cal.get(Calendar.DAY_OF_WEEK)
                    var diff = dayCode - currentDay
                    if (diff <= 0) diff += 7
                    cal.add(Calendar.DAY_OF_YEAR, diff)
                    break
                }
            }
        }

        // 3. Target Time resolution
        var hour = -1
        var minute = 0

        // Look for 12-hour clock (e.g. "5:30 PM", "7 PM", "11 AM")
        val clockMatch = Regex("(?i)\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b").find(lower)
        if (clockMatch != null) {
            var h = clockMatch.groupValues[1].toIntOrNull() ?: 12
            val m = clockMatch.groupValues[2].toIntOrNull() ?: 0
            val ampm = clockMatch.groupValues[3].lowercase()
            if (ampm == "pm" && h < 12) h += 12
            else if (ampm == "am" && h == 12) h = 0
            hour = h
            minute = m
        } else {
            // Look for "at 5", "5 o'clock", "5 baje", "5 ki", "10 ki"
            val numMatch = Regex("(?i)\\b(?:at|around|by)?\\s*(\\d{1,2})\\s*(?:o'?clock|baje|ki)\\b").find(lower)
            if (numMatch != null) {
                var h = numMatch.groupValues[1].toIntOrNull() ?: 5
                if (lower.contains("night") || lower.contains("evening") || lower.contains("shaam") ||
                    lower.contains("rathri") || lower.contains("sayantram") || lower.contains("tonight")) {
                    if (h < 12) h += 12
                } else if (lower.contains("afternoon") || lower.contains("madhyanam") || lower.contains("dopahar")) {
                    if (h < 12 && h != 12) h += 12
                } else if (h < 7) {
                    h += 12 // Default to PM for small numbers unless morning is specified
                }
                hour = h
                minute = 0
            }
        }

        // Fallback for general time-of-day phrases
        if (hour == -1) {
            if (lower.contains("morning") || lower.contains("podduna") || lower.contains("subah")) {
                hour = 10
            } else if (lower.contains("afternoon") || lower.contains("madhyanam") || lower.contains("dopahar")) {
                hour = 14
            } else if (lower.contains("evening") || lower.contains("sayantram") || lower.contains("shaam")) {
                hour = 18
            } else if (lower.contains("night") || lower.contains("rathri") || lower.contains("raat") || lower.contains("tonight")) {
                hour = 20
            } else {
                return null
            }
        }

        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // If target time is earlier today than current time, push to tomorrow
        if (cal.timeInMillis <= nowEpochMs) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return buildResult(trimmed, cal.timeInMillis, nowEpochMs)
    }

    private fun buildResult(originalText: String, eventEpochMs: Long, nowEpochMs: Long): ParsedReminder {
        val lower = originalText.lowercase()

        // Context-Adaptive Lead Time Calculation
        val leadMinutes = when {
            lower.contains("flight") || lower.contains("airport") || lower.contains("train") -> 120 // 2 hours for travel
            lower.contains("doctor") || lower.contains("dentist") || lower.contains("hospital") || lower.contains("clinic") -> 60 // 1 hour for healthcare
            lower.contains("meeting") || lower.contains("call") || lower.contains("interview") || lower.contains("zoom") || lower.contains("sync") -> 15 // 15 mins for calls/meetings
            else -> 30 // standard 30 minutes
        }

        var triggerEpochMs = eventEpochMs - (leadMinutes * 60 * 1000L)
        // If the lead time already elapsed (e.g. event is in 10 mins and lead was 30 mins), trigger in 10 seconds
        if (triggerEpochMs <= nowEpochMs) {
            triggerEpochMs = nowEpochMs + 10_000L
        }

        val cleanTitle = extractCleanTitle(originalText)
        val explicitRegex = Regex("(?i)^\\s*(?:please\\s+)?(?:remind\\s+(?:me\\s+)?|set\\s+(?:a\\s+)?(?:reminder|alarm)|alert\\s+me|schedule\\s+(?:a\\s+)?reminder|gurtu\\s+chey|yaad\\s+dilana)\\b")
        val isExplicit = explicitRegex.containsMatchIn(originalText)

        return ParsedReminder(
            title = cleanTitle,
            eventEpochMs = eventEpochMs,
            triggerEpochMs = triggerEpochMs,
            leadMinutes = leadMinutes,
            isExplicit = isExplicit
        )
    }

    private fun extractCleanTitle(original: String): String {
        var s = original.trim()

        // Strip command and spoken preamble
        val preambles = listOf(
            Regex("(?i)^\\s*(?:please\\s+)?remind\\s+(?:me\\s+)?(?:to\\s+|about\\s+)?"),
            Regex("(?i)^\\s*(?:set\\s+a\\s+)?reminder\\s+(?:to\\s+|for\\s+)?"),
            Regex("(?i)^\\s*don'?t\\s+forget\\s+to\\s+"),
            Regex("(?i)^\\s*(?:nenu|main|we\\s+need\\s+to|i\\s+need\\s+to|i\\s+have\\s+to)\\s+")
        )
        for (pattern in preambles) {
            s = pattern.replace(s, "")
        }

        // Remove date and day terms
        s = s.replace(Regex("(?i)\\b(day\\s+after\\s+tomorrow|ellundi|parso|tomorrow|repu|kal|today|tonight|ee\\s+roju|ivala|aaj)\\b"), "")
        // Remove time-of-day terms
        s = s.replace(Regex("(?i)\\b(morning|afternoon|evening|night|podduna|sayantram|rathri|subah|shaam|raat)\\b"), "")
        // Remove clock times and suffixes (e.g. "at 5:00 PM", "10 AM ki", "5 baje")
        s = s.replace(Regex("(?i)\\b(?:at|around|by|for)?\\s*\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?\\s*(?:ki|ko|baje|o'?clock)?\\b"), "")
        // Remove relative offset ("in 45 minutes", "in 2 hours")
        s = s.replace(Regex("(?i)\\bin\\s+\\d+\\s*(?:mins|minutes|hours|hrs)\\b"), "")
        // Remove trailing punctuation
        s = s.replace(Regex("[,.?!;:]+"), " ")
        s = s.replace(Regex("\\s+"), " ").trim()

        if (s.isBlank() || s.length < 3) {
            s = original.trim()
        }

        return s.replaceFirstChar { it.uppercaseChar() }
    }
}
