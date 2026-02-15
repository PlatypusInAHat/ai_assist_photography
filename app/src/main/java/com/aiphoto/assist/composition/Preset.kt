package com.aiphoto.assist.composition

/**
 * Base interface for all composition presets.
 * Each preset knows how to evaluate the current frame and produce hints + overlay.
 */
interface Preset {
    val id: String
    val displayName: String

    /**
     * Returns 0..1 indicating how applicable this preset is to the current frame.
     * Used by the auto-ranker to pick the best preset.
     */
    fun applicability(f: FrameFeatures): Float

    /**
     * Evaluate composition quality and return score + hints.
     */
    fun evaluate(f: FrameFeatures): Evaluation
}
