package com.lilt.mobile.engine

import java.util.regex.Matcher
import java.util.regex.Pattern

data class ExtractedContact(
    val name: String,
    val phone: String,
    val formattedDisplayPhone: String
)

object ContactExtractor {

    private val WORD_TO_DIGIT = mapOf(
        "zero" to "0", "one" to "1", "two" to "2", "three" to "3", "four" to "4",
        "five" to "5", "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9"
    )

    private val MULTIPLIERS = mapOf(
        "double" to 2,
        "triple" to 3
    )

    private val STOP_WORDS = setOf(
        "the", "a", "an", "this", "that", "number", "phone", "contact", "new", "details",
        "save", "note", "take", "make", "call", "down", "my", "his", "her", "their",
        "cell", "mobile", "chey", "chesko", "karo", "karna", "undi", "please", "to", "for", "name", "person"
    )

    fun normalizeSpokenDigits(text: String): String {
        val tokens = text.split("\\s+".toRegex())
        val result = mutableListOf<String>()
        var i = 0
        while (i < tokens.size) {
            val token = tokens[i].lowercase()
            if (MULTIPLIERS.containsKey(token) && i + 1 < tokens.size) {
                val count = MULTIPLIERS[token] ?: 1
                val nextToken = tokens[i + 1].lowercase()
                val digit = WORD_TO_DIGIT[nextToken] ?: if (nextToken.all { it.isDigit() }) nextToken else null
                if (digit != null) {
                    repeat(count) { result.add(digit) }
                    i += 2
                    continue
                }
            }
            if (WORD_TO_DIGIT.containsKey(token)) {
                result.add(WORD_TO_DIGIT[token]!!)
            } else {
                result.add(tokens[i])
            }
            i++
        }
        return result.joinToString(" ")
    }

    fun extract(cleanedText: String, rawText: String = ""): ExtractedContact? {
        if (cleanedText.isBlank() && rawText.isBlank()) return null

        val normClean = normalizeSpokenDigits(cleanedText)
        val normRaw = if (rawText.isNotBlank()) normalizeSpokenDigits(rawText) else ""

        val phonePattern = Pattern.compile("(\\+?\\d{1,3}[\\s-]?)?(\\d[\\s-]?){9,12}\\d")
        var mPhone = phonePattern.matcher(normClean)
        var sourceText = normClean
        var foundInClean = mPhone.find()

        if (!foundInClean && normRaw.isNotBlank()) {
            mPhone = phonePattern.matcher(normRaw)
            sourceText = normRaw
            foundInClean = mPhone.find()
        }

        if (!foundInClean) return null

        val rawPhoneMatch = mPhone.group().trim()
        val digitsOnly = rawPhoneMatch.replace("\\D".toRegex(), "")
        if (digitsOnly.length < 10) return null

        val hasPlus = rawPhoneMatch.startsWith("+")
        val normalizedPhone = if (hasPlus) "+$digitsOnly" else digitsOnly
        val formattedPhone = formatPhoneNumber(normalizedPhone)

        // 1. Try extracting name from cleanedText
        var nameCandidate: String? = null
        if (normClean.isNotBlank()) {
            val mClean = phonePattern.matcher(normClean)
            if (mClean.find()) {
                nameCandidate = extractNameFromContext(normClean, mClean)
            }
        }

        // 2. If name not found or defaulted, try extracting from rawText
        if (nameCandidate == null && normRaw.isNotBlank()) {
            val mRaw = phonePattern.matcher(normRaw)
            if (mRaw.find()) {
                nameCandidate = extractNameFromContext(normRaw, mRaw)
            } else {
                nameCandidate = extractNameFromLooseText(normRaw)
            }
        }

        val finalName = nameCandidate?.capitalizeWords() ?: "Contact"

        return ExtractedContact(
            name = finalName,
            phone = normalizedPhone,
            formattedDisplayPhone = formattedPhone
        )
    }

    private fun extractNameFromContext(text: String, matcher: Matcher): String? {
        val beforeText = text.substring(0, matcher.start()).trim()
        val afterText = text.substring(matcher.end()).trim()
        val fullContext = "$afterText $beforeText"

        // 1. Structured label: "name: Suresh Kumar" or "• Name: Suresh Kumar" or "Contact: Suresh Kumar"
        val labelPattern = Pattern.compile("\\b(?:name|contact|person)\\s*[:\\-]\\s*([A-Za-z]+(?:\\s+[A-Za-z]+)?)\\b", Pattern.CASE_INSENSITIVE)
        val mLabel = labelPattern.matcher(fullContext)
        if (mLabel.find()) {
            cleanCandidate(mLabel.group(1))?.let { return it }
        }

        // 2. "for <Name>" or "to <Name>" in afterText, then beforeText
        val forPattern = Pattern.compile("\\b(?:for|to)\\s+([A-Za-z]+(?:\\s+[A-Za-z]+)?)\\b", Pattern.CASE_INSENSITIVE)
        val mForAfter = forPattern.matcher(afterText)
        if (mForAfter.find()) {
            cleanCandidate(mForAfter.group(1))?.let { return it }
        }
        val mForBefore = forPattern.matcher(beforeText)
        if (mForBefore.find()) {
            cleanCandidate(mForBefore.group(1))?.let { return it }
        }

        // 3. "save/note down [contact] <Name>'s number"
        val possessivePattern = Pattern.compile(
            "\\b(?:save|note\\s+down|add|note)\\s+(?:contact\\s+)?([A-Za-z]+(?:\\s+[A-Za-z]+)?)(?:'s|s)?\\s+(?:phone\\s+number|mobile\\s+number|cell\\s+number|number|phone|mobile)\\b",
            Pattern.CASE_INSENSITIVE
        )
        val mPoss = possessivePattern.matcher(beforeText)
        if (mPoss.find()) {
            cleanCandidate(mPoss.group(1))?.let { return it }
        }

        // 4. Indic: "<Name> [ka/ki/yokka] [phone number/number] [save chey/note karo]"
        val indicPattern = Pattern.compile(
            "\\b([A-Za-z]+(?:\\s+[A-Za-z]+)?)\\s+(?:ka|ki|yokka|ani)?\\s*(?:phone\\s+number|mobile\\s+number|cell\\s+number|number|phone|mobile)\\b",
            Pattern.CASE_INSENSITIVE
        )
        val mIndic = indicPattern.matcher(beforeText)
        if (mIndic.find()) {
            cleanCandidate(mIndic.group(1))?.let { return it }
        }

        // 5. Command: "save contact <Name>" or "add contact <Name>"
        val cmdPattern = Pattern.compile(
            "\\b(?:save\\s+contact|add\\s+contact|contact\\s+name|contact)\\s+([A-Za-z]+(?:\\s+[A-Za-z]+)?)\\b",
            Pattern.CASE_INSENSITIVE
        )
        val mCmd = cmdPattern.matcher(beforeText)
        if (mCmd.find()) {
            cleanCandidate(mCmd.group(1))?.let { return it }
        }

        // 6. Trailing residual after phone: e.g. "9876543210 Suresh Kumar" or "• name: suresh kumar"
        val trailPattern = Pattern.compile(
            "^[•\\-\\*:\\s]*(?:name\\s*[:\\-]\\s*)?([A-Za-z]+(?:\\s+[A-Za-z]+)?)\\b",
            Pattern.CASE_INSENSITIVE
        )
        val mTrail = trailPattern.matcher(afterText)
        if (mTrail.find()) {
            cleanCandidate(mTrail.group(1))?.let { return it }
        }

        // 7. Leading residual before phone: e.g. "Suresh Kumar: 9876543210"
        val leadPattern = Pattern.compile("([A-Za-z]+(?:\\s+[A-Za-z]+)?)\\s*[:\\-]?\\s*$", Pattern.CASE_INSENSITIVE)
        val mLead = leadPattern.matcher(beforeText)
        if (mLead.find()) {
            cleanCandidate(mLead.group(1))?.let { return it }
        }

        return null
    }

    private fun extractNameFromLooseText(text: String): String? {
        val patterns = listOf(
            Pattern.compile("\\b(?:for|to)\\s+([A-Za-z]+(?:\\s+[A-Za-z]+)?)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b([A-Za-z]+(?:\\s+[A-Za-z]+)?)\\s+(?:ka|ki|yokka|ani)?\\s*(?:phone|number)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:save|note\\s+down)\\s+(?:contact\\s+)?([A-Za-z]+(?:\\s+[A-Za-z]+)?)", Pattern.CASE_INSENSITIVE)
        )
        for (pat in patterns) {
            val m = pat.matcher(text)
            if (m.find()) {
                val cand = cleanCandidate(m.group(1))
                if (cand != null) return cand
            }
        }
        return null
    }

    private fun cleanCandidate(rawCandidate: String?): String? {
        if (rawCandidate.isNullOrBlank()) return null
        var cleaned = rawCandidate.trim()
        // Strip symbols like bullets, colons, hyphens
        cleaned = cleaned.replace("^[•\\-\\*:\\s]+|[•\\-\\*:\\s]+$".toRegex(), "").trim()
        // Strip trailing suffixes
        cleaned = cleaned.replace("(?i)\\s+(?:phone|mobile|cell|number|ka|ki|yokka|ani)$".toRegex(), "").trim()
        // Strip leading indicators
        cleaned = cleaned.replace("(?i)^(?:phone|mobile|cell|number|contact|name|person)\\s*[:\\-]\\s*".toRegex(), "").trim()

        if (cleaned.isBlank()) return null

        val words = cleaned.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (words.all { STOP_WORDS.contains(it) }) return null

        val nonStopWords = words.filter { !STOP_WORDS.contains(it) }
        if (nonStopWords.isEmpty()) return null

        return nonStopWords.joinToString(" ") { it.replaceFirstChar { char -> char.titlecase() } }
    }

    private fun formatPhoneNumber(phone: String): String {
        val digits = phone.replace("\\D".toRegex(), "")
        return when {
            phone.startsWith("+91") && digits.length >= 12 -> {
                val national = digits.substring(2)
                if (national.length == 10) {
                    "+91 ${national.substring(0, 5)} ${national.substring(5)}"
                } else {
                    "+91 $national"
                }
            }
            digits.length == 10 -> {
                "${digits.substring(0, 5)} ${digits.substring(5)}"
            }
            digits.length == 11 && digits.startsWith("0") -> {
                val national = digits.substring(1)
                "${national.substring(0, 5)} ${national.substring(5)}"
            }
            else -> phone
        }
    }

    private fun String.capitalizeWords(): String {
        return this.split("\\s+".toRegex()).joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
