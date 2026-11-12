package com.lilt.mobile.engine

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * LiltAudioDSP: Lightweight on-device Digital Signal Processor for real-time speech enhancement
 * and background noise suppression prior to Whisper transcription.
 *
 * Implements:
 * 1. 2nd-order Butterworth High-Pass Filter (85 Hz cutoff @ 16kHz) to eliminate sub-audible table taps,
 *    AC hum (50/60Hz), laptop fan rumble, and mechanical vibrations.
 * 2. Adaptive Soft-Knee Spectral Noise Gate that tracks ambient background floor noise and attenuates
 *    non-vocal room hiss and murmur by 12–18 dB without clipping vocal consonants.
 * 3. Leading & Trailing Silence/Noise Trimmer (VAD) to prevent Whisper from hallucinating on empty noise tails.
 */
object LiltAudioDSP {

    private const val SAMPLE_RATE = 16000
    private const val HIGH_PASS_CUTOFF_HZ = 85.0
    private const val FRAME_SIZE_SAMPLES = 320 // 20ms frames at 16kHz

    /**
     * Processes raw 16-bit PCM byte array and returns noise-filtered, speech-enhanced PCM data.
     */
    fun processPcm(
        pcmData: ByteArray,
        sampleRate: Int = SAMPLE_RATE,
        enableHighPass: Boolean = true,
        enableNoiseGate: Boolean = true
    ): ByteArray {
        if (pcmData.size < 640) return pcmData

        val numSamples = pcmData.size / 2
        val samples = ShortArray(numSamples)

        // Decode 16-bit little-endian PCM to Short
        for (i in 0 until numSamples) {
            val low = pcmData[i * 2].toInt() and 0xFF
            val high = pcmData[i * 2 + 1].toInt()
            samples[i] = ((high shl 8) or low).toShort()
        }

        // Step 1: High-Pass Filter (Eliminate low-frequency rumble, AC 50/60Hz hum, wind/thump)
        if (enableHighPass) {
            applyHighPassFilter(samples, sampleRate, HIGH_PASS_CUTOFF_HZ)
        }

        // Step 2: Adaptive Noise Gate (Attenuate steady-state background floor & room murmur)
        if (enableNoiseGate) {
            applySoftKneeNoiseGate(samples, FRAME_SIZE_SAMPLES)
        }

        // Step 3: Trim Leading and Trailing Low-Energy Silence/Hiss (VAD)
        val (trimmedStart, trimmedEnd) = calculateVadBoundaries(samples, FRAME_SIZE_SAMPLES)
        val finalSamples = if (trimmedStart < trimmedEnd && (trimmedEnd - trimmedStart) >= 4000) {
            samples.copyOfRange(trimmedStart, trimmedEnd)
        } else {
            samples
        }

        // Encode back to 16-bit little-endian PCM
        val output = ByteArray(finalSamples.size * 2)
        for (i in finalSamples.indices) {
            val sample = finalSamples[i].toInt()
            output[i * 2] = (sample and 0xFF).toByte()
            output[i * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
        }

        return output
    }

    /**
     * 2nd-order IIR Butterworth High-Pass Filter
     */
    private fun applyHighPassFilter(samples: ShortArray, sampleRate: Int, cutoffHz: Double) {
        val omega = 2.0 * Math.PI * cutoffHz / sampleRate
        val cosOmega = cos(omega)
        val sinOmega = sin(omega)
        val alpha = sinOmega / (2.0 * sqrt(2.0))

        val b0 = (1.0 + cosOmega) / 2.0
        val b1 = -(1.0 + cosOmega)
        val b2 = (1.0 + cosOmega) / 2.0
        val a0 = 1.0 + alpha
        val a1 = -2.0 * cosOmega
        val a2 = 1.0 - alpha

        var x1 = 0.0
        var x2 = 0.0
        var y1 = 0.0
        var y2 = 0.0

        for (i in samples.indices) {
            val x0 = samples[i].toDouble()
            val y0 = (b0 / a0) * x0 + (b1 / a0) * x1 + (b2 / a0) * x2 - (a1 / a0) * y1 - (a2 / a0) * y2

            x2 = x1
            x1 = x0
            y2 = y1
            y1 = y0

            samples[i] = y0.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    /**
     * Adaptive Soft-Knee Noise Gate.
     * Computes frame-by-frame RMS, estimates ambient floor during the first 160ms,
     * and applies a smooth gain curve (0.15x to 1.0x) so background hiss and murmur
     * are suppressed by ~16 dB without sharp cutting of words.
     */
    private fun applySoftKneeNoiseGate(samples: ShortArray, frameSize: Int) {
        val numFrames = samples.size / frameSize
        if (numFrames < 4) return

        // Estimate baseline noise floor from the quietest early frames (up to first 8 frames)
        var estimatedNoiseFloor = 400.0
        val earlyFrames = numFrames.coerceAtMost(8)
        var minFrameRms = Double.MAX_VALUE

        for (f in 0 until earlyFrames) {
            var sum = 0.0
            val offset = f * frameSize
            for (i in 0 until frameSize) {
                val s = samples[offset + i].toDouble()
                sum += s * s
            }
            val frameRms = sqrt(sum / frameSize)
            if (frameRms < minFrameRms) {
                minFrameRms = frameRms
            }
        }
        if (minFrameRms in 50.0..1800.0) {
            estimatedNoiseFloor = minFrameRms
        }

        val speechThreshold = (estimatedNoiseFloor * 2.2).coerceIn(350.0, 2200.0)
        val attenuationFloor = 0.18 // ~15 dB reduction of steady noise

        var smoothedGain = 1.0
        val attackCoeff = 0.35  // fast transition when speech starts (35% toward target)
        val releaseCoeff = 0.08 // smooth fade out when speech pauses

        for (f in 0 until numFrames) {
            val offset = f * frameSize
            var sum = 0.0
            for (i in 0 until frameSize) {
                val s = samples[offset + i].toDouble()
                sum += s * s
            }
            val frameRms = sqrt(sum / frameSize)

            // Target gain via soft-knee curve
            val targetGain = when {
                frameRms >= speechThreshold -> 1.0
                frameRms <= estimatedNoiseFloor -> attenuationFloor
                else -> {
                    val ratio = (frameRms - estimatedNoiseFloor) / (speechThreshold - estimatedNoiseFloor)
                    attenuationFloor + ratio * (1.0 - attenuationFloor)
                }
            }

            // Smooth gain envelope across frames to prevent audible pumping or clicks
            val coeff = if (targetGain > smoothedGain) attackCoeff else releaseCoeff
            smoothedGain += coeff * (targetGain - smoothedGain)

            for (i in 0 until frameSize) {
                val processed = (samples[offset + i] * smoothedGain).toInt()
                samples[offset + i] = processed.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
    }

    /**
     * Voice Activity Detection (VAD) boundary calculation.
     * Identifies active speech frames and returns the [startSample, endSample] range,
     * including a 160ms pre-speech and post-speech margin so consonants are never truncated.
     */
    private fun calculateVadBoundaries(samples: ShortArray, frameSize: Int): Pair<Int, Int> {
        val numFrames = samples.size / frameSize
        if (numFrames < 5) return Pair(0, samples.size)

        var firstSpeechFrame = -1
        var lastSpeechFrame = -1

        for (f in 0 until numFrames) {
            val offset = f * frameSize
            var sum = 0.0
            for (i in 0 until frameSize) {
                val s = samples[offset + i].toDouble()
                sum += s * s
            }
            val frameRms = sqrt(sum / frameSize)
            if (frameRms > 450.0) {
                if (firstSpeechFrame == -1) firstSpeechFrame = f
                lastSpeechFrame = f
            }
        }

        if (firstSpeechFrame == -1 || lastSpeechFrame == -1) {
            return Pair(0, samples.size)
        }

        val marginFrames = 8 // ~160ms safety padding
        val startFrame = (firstSpeechFrame - marginFrames).coerceAtLeast(0)
        val endFrame = (lastSpeechFrame + marginFrames).coerceAtMost(numFrames)

        val startSample = startFrame * frameSize
        val endSample = (endFrame * frameSize).coerceAtMost(samples.size)

        return Pair(startSample, endSample)
    }
}
