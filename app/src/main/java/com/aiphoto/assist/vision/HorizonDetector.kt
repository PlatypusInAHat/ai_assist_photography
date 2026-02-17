package com.aiphoto.assist.vision

import kotlin.math.abs
import kotlin.math.max

/**
 * Lightweight horizon-line detector.
 *
 * Algorithm:
 * 1. Downsample the Y (luminance) plane to [DS_W]×[DS_H]
 * 2. Apply a vertical Sobel kernel to detect horizontal edges
 * 3. Accumulate edge magnitudes per row → 1-D profile
 * 4. Find the peak row = likely horizon
 * 5. Smooth across frames with EMA to prevent jitter
 * 6. Gate output with a confidence threshold to avoid false positives
 */
class HorizonDetector(
    private val emaAlpha: Float = 0.3f,
    private val confidenceThreshold: Float = 12f
) {

    companion object {
        /** Downsample target dimensions — ~10K pixels is plenty for edge voting */
        private const val DS_W = 120
        private const val DS_H = 90

        /** Ignore the top/bottom 5 % of the image to avoid vignette & UI artifacts */
        private const val MARGIN_RATIO = 0.05f
    }

    /** EMA-smoothed horizon Y position (normalized 0..1), null = no history yet */
    @Volatile
    private var smoothedY: Float? = null

    // ──────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Detect the horizon line in the given luminance plane.
     *
     * @param yBytes  Raw Y-plane bytes from CameraX (row-major, one byte per pixel).
     * @param width   Full-resolution width of the Y-plane.
     * @param height  Full-resolution height of the Y-plane.
     * @param rowStride Row stride of the Y-plane (may be > width due to padding).
     * @return Normalized Y position (0.0 = top, 1.0 = bottom), or `null` if no
     *         confident horizon was found (e.g. indoor/textured scene).
     */
    fun detect(yBytes: ByteArray, width: Int, height: Int, rowStride: Int): Float? {
        // 1. Downsample
        val ds = downsampleY(yBytes, width, height, rowStride)

        // 2. Sobel-Y → edge magnitude buffer
        val edges = sobelHorizontalEdges(ds)

        // 3. Row accumulation
        val rowStrengths = accumulateRows(edges)

        // 4. Peak detection (ignoring margins)
        val (peakRow, peakStrength) = findPeakRow(rowStrengths)

        // 5. Confidence gate
        if (peakStrength < confidenceThreshold) {
            // Low confidence → decay smoothed value gradually
            smoothedY?.let { prev ->
                smoothedY = prev * (1f - emaAlpha * 0.5f) + 0.5f * (emaAlpha * 0.5f)
            }
            return null
        }

        // 6. Convert row index → normalized Y and apply EMA
        val rawY = peakRow.toFloat() / (DS_H - 1).toFloat()
        val prev = smoothedY
        val filtered = if (prev == null) rawY else prev * (1f - emaAlpha) + rawY * emaAlpha
        smoothedY = filtered

        return filtered
    }

    /**
     * Reset internal state. Call when camera restarts or preset changes.
     */
    fun reset() {
        smoothedY = null
    }

    // ──────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Nearest-neighbour downsample of Y-plane to [DS_W]×[DS_H].
     * Returns an IntArray with unsigned luma values (0..255).
     */
    private fun downsampleY(
        src: ByteArray,
        srcW: Int,
        srcH: Int,
        srcRowStride: Int
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
     * Apply a simplified vertical Sobel kernel to detect **horizontal** edges.
     *
     * Kernel (Sobel-Y):
     * ```
     *  -1  -2  -1
     *   0   0   0
     *  +1  +2  +1
     * ```
     *
     * Returns edge magnitude array of size [DS_W]×[DS_H] (border pixels set to 0).
     */
    private fun sobelHorizontalEdges(luma: IntArray): IntArray {
        val out = IntArray(DS_W * DS_H) // defaults to 0

        for (y in 1 until DS_H - 1) {
            for (x in 1 until DS_W - 1) {
                val topLeft = luma[(y - 1) * DS_W + (x - 1)]
                val topMid  = luma[(y - 1) * DS_W + x]
                val topRight = luma[(y - 1) * DS_W + (x + 1)]
                val botLeft = luma[(y + 1) * DS_W + (x - 1)]
                val botMid  = luma[(y + 1) * DS_W + x]
                val botRight = luma[(y + 1) * DS_W + (x + 1)]

                val gy = (-topLeft - 2 * topMid - topRight +
                           botLeft + 2 * botMid + botRight)

                out[y * DS_W + x] = abs(gy)
            }
        }
        return out
    }

    /**
     * Sum edge magnitudes per row → 1-D strength profile.
     * Normalized by row width so the value is **average** edge strength per pixel.
     */
    private fun accumulateRows(edges: IntArray): FloatArray {
        val profile = FloatArray(DS_H)
        val innerW = (DS_W - 2).toFloat() // exclude border columns

        for (y in 1 until DS_H - 1) {
            var sum = 0
            for (x in 1 until DS_W - 1) {
                sum += edges[y * DS_W + x]
            }
            profile[y] = sum / innerW
        }
        return profile
    }

    /**
     * Find the row with the maximum accumulated edge strength,
     * ignoring top/bottom margins.
     *
     * @return Pair(rowIndex, peakStrength)
     */
    private fun findPeakRow(profile: FloatArray): Pair<Int, Float> {
        val marginRows = (DS_H * MARGIN_RATIO).toInt().coerceAtLeast(1)
        var bestRow = DS_H / 2
        var bestVal = 0f

        for (y in marginRows until DS_H - marginRows) {
            if (profile[y] > bestVal) {
                bestVal = profile[y]
                bestRow = y
            }
        }
        return bestRow to bestVal
    }
}
