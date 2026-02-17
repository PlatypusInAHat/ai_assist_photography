package com.aiphoto.assist.vision

import kotlin.math.abs

/**
 * Lightweight horizon-line detector.
 *
 * Improved algorithm:
 * 1. Downsample Y-plane to [DS_W]×[DS_H] (~10 K pixels)
 * 2. 1-D Gaussian row blur to suppress sensor noise
 * 3. Sobel-Y kernel → horizontal edge magnitudes
 * 4. Row accumulation → 1-D edge profile
 * 5. Sliding-window smoothing on profile → reduce spikes
 * 6. Peak detection with **width validation** (reject narrow texture spikes)
 * 7. EMA temporal smoothing + confidence gating
 * 8. Frame-skip to save battery (process every N-th frame)
 */
class HorizonDetector(
    /** EMA blending factor (0 = ignore new data, 1 = no smoothing). */
    private val emaAlpha: Float = 0.3f,
    /** Minimum average edge strength to consider a valid horizon. */
    private val confidenceThreshold: Float = 12f,
    /** Only run detection on every N-th frame. */
    private val frameSkip: Int = 3
) {

    companion object {
        private const val DS_W = 120
        private const val DS_H = 90
        private const val MARGIN_RATIO = 0.05f

        /** Half-width of the sliding window on the 1-D profile. */
        private const val PROFILE_WINDOW = 2

        /**
         * Minimum number of rows around the peak that must exceed
         * 50 % of peak strength. Horizon edges are wide; texture spikes are narrow.
         */
        private const val MIN_PEAK_WIDTH = 3

        /** After this many consecutive low-confidence frames, reset EMA. */
        private const val MAX_MISS_STREAK = 5

        /** Simple 1-D Gaussian-ish kernel [1, 2, 1] / 4. */
        private val GAUSS_KERNEL = floatArrayOf(0.25f, 0.5f, 0.25f)
    }

    @Volatile private var smoothedY: Float? = null
    private var frameCounter = 0
    private var lastResult: Float? = null
    private var missStreak = 0

    // ──────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────

    /**
     * @param yBytes   Raw Y-plane bytes (row-major).
     * @param width    Full-resolution width.
     * @param height   Full-resolution height.
     * @param rowStride Row stride (may be > width due to padding).
     * @return Normalised horizon Y (0 = top, 1 = bottom), or null.
     */
    fun detect(yBytes: ByteArray, width: Int, height: Int, rowStride: Int): Float? {
        // Frame-skip: reuse last result for non-processing frames
        frameCounter++
        if (frameCounter % frameSkip != 0) return lastResult

        val result = detectInternal(yBytes, width, height, rowStride)
        lastResult = result
        return result
    }

    fun reset() {
        smoothedY = null
        lastResult = null
        frameCounter = 0
        missStreak = 0
    }

    // ──────────────────────────────────────────────────────────────────
    // Core pipeline
    // ──────────────────────────────────────────────────────────────────

    private fun detectInternal(
        yBytes: ByteArray, width: Int, height: Int, rowStride: Int
    ): Float? {
        // 1. Downsample
        val ds = downsampleY(yBytes, width, height, rowStride)

        // 2. Gaussian row-blur (noise suppression)
        gaussianBlurRows(ds)

        // 3. Sobel-Y → horizontal edges
        val edges = sobelHorizontalEdges(ds)

        // 4. Row accumulation
        val rawProfile = accumulateRows(edges)

        // 5. Sliding-window smooth on profile
        val profile = smoothProfile(rawProfile)

        // 6. Peak detection with width validation
        val peak = findValidPeak(profile) ?: run {
            handleMiss()
            return null
        }

        // 7. Confidence gate
        if (peak.second < confidenceThreshold) {
            handleMiss()
            return null
        }

        // Good detection → reset miss streak
        missStreak = 0

        // 8. EMA temporal smoothing
        val rawY = peak.first.toFloat() / (DS_H - 1).toFloat()
        val prev = smoothedY
        val filtered = if (prev == null) rawY else prev * (1f - emaAlpha) + rawY * emaAlpha
        smoothedY = filtered
        return filtered
    }

    private fun handleMiss() {
        missStreak++
        if (missStreak >= MAX_MISS_STREAK) {
            smoothedY = null   // clean reset after sustained misses
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────

    private fun downsampleY(
        src: ByteArray, srcW: Int, srcH: Int, srcRowStride: Int
    ): IntArray {
        val dst = IntArray(DS_W * DS_H)
        val xRatio = srcW.toFloat() / DS_W
        val yRatio = srcH.toFloat() / DS_H
        for (dy in 0 until DS_H) {
            val sy = (dy * yRatio).toInt().coerceIn(0, srcH - 1)
            for (dx in 0 until DS_W) {
                val sx = (dx * xRatio).toInt().coerceIn(0, srcW - 1)
                dst[dy * DS_W + dx] = src[sy * srcRowStride + sx].toInt() and 0xFF
            }
        }
        return dst
    }

    /**
     * In-place 1-D Gaussian blur along each row [1,2,1]/4.
     * Suppresses per-pixel sensor noise without affecting strong horizontal edges.
     */
    private fun gaussianBlurRows(buf: IntArray) {
        val tmp = IntArray(DS_W)
        for (y in 0 until DS_H) {
            val rowOff = y * DS_W
            // copy row
            for (x in 0 until DS_W) tmp[x] = buf[rowOff + x]
            // convolve inner pixels
            for (x in 1 until DS_W - 1) {
                buf[rowOff + x] = (
                    tmp[x - 1] * GAUSS_KERNEL[0] +
                    tmp[x]     * GAUSS_KERNEL[1] +
                    tmp[x + 1] * GAUSS_KERNEL[2]
                ).toInt()
            }
        }
    }

    private fun sobelHorizontalEdges(luma: IntArray): IntArray {
        val out = IntArray(DS_W * DS_H)
        for (y in 1 until DS_H - 1) {
            for (x in 1 until DS_W - 1) {
                val tl = luma[(y - 1) * DS_W + (x - 1)]
                val tm = luma[(y - 1) * DS_W + x]
                val tr = luma[(y - 1) * DS_W + (x + 1)]
                val bl = luma[(y + 1) * DS_W + (x - 1)]
                val bm = luma[(y + 1) * DS_W + x]
                val br = luma[(y + 1) * DS_W + (x + 1)]

                val gy = (-tl - 2 * tm - tr + bl + 2 * bm + br)
                out[y * DS_W + x] = abs(gy)
            }
        }
        return out
    }

    private fun accumulateRows(edges: IntArray): FloatArray {
        val profile = FloatArray(DS_H)
        val innerW = (DS_W - 2).toFloat()
        for (y in 1 until DS_H - 1) {
            var sum = 0
            for (x in 1 until DS_W - 1) sum += edges[y * DS_W + x]
            profile[y] = sum / innerW
        }
        return profile
    }

    /** Sliding-window average on the 1-D row profile to smooth spikes. */
    private fun smoothProfile(raw: FloatArray): FloatArray {
        val out = FloatArray(DS_H)
        for (y in 0 until DS_H) {
            var sum = 0f
            var count = 0
            for (dy in -PROFILE_WINDOW..PROFILE_WINDOW) {
                val idx = y + dy
                if (idx in 0 until DS_H) {
                    sum += raw[idx]
                    count++
                }
            }
            out[y] = sum / count
        }
        return out
    }

    /**
     * Find the peak row and validate that it is a **wide** peak
     * (at least [MIN_PEAK_WIDTH] adjacent rows above 50 % of peak).
     *
     * Returns Pair(rowIndex, peakStrength) or null if no valid peak.
     */
    private fun findValidPeak(profile: FloatArray): Pair<Int, Float>? {
        val margin = (DS_H * MARGIN_RATIO).toInt().coerceAtLeast(1)
        var bestRow = -1
        var bestVal = 0f

        for (y in margin until DS_H - margin) {
            if (profile[y] > bestVal) {
                bestVal = profile[y]
                bestRow = y
            }
        }
        if (bestRow < 0 || bestVal <= 0f) return null

        // Width validation: count adjacent rows >= 50% of peak
        val halfPeak = bestVal * 0.5f
        var width = 0
        for (dy in -MIN_PEAK_WIDTH..MIN_PEAK_WIDTH) {
            val idx = bestRow + dy
            if (idx in 0 until DS_H && profile[idx] >= halfPeak) width++
        }
        if (width < MIN_PEAK_WIDTH) return null   // narrow spike → reject

        return bestRow to bestVal
    }
}
