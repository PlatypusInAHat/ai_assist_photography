package com.aiphoto.assist.composition

/**
 * Manages all composition presets and provides auto-ranking.
 */
class PresetManager(
    val presets: List<Preset>
) {
    /** Select the most applicable preset for the current frame features */
    fun bestPreset(f: FrameFeatures): Preset =
        presets.maxBy { it.applicability(f) }

    /**
     * Evaluate the current frame.
     * @param selectedId User-selected preset id (null = auto)
     * @param auto If true, always pick the best preset automatically
     */
    fun evaluate(f: FrameFeatures, selectedId: String?, auto: Boolean): Pair<Preset, Evaluation> {
        val preset = if (auto || selectedId == null) {
            bestPreset(f)
        } else {
            presets.firstOrNull { it.id == selectedId } ?: bestPreset(f)
        }
        val eval = preset.evaluate(f)
        return preset to eval.copy(subjectBox = f.subjectBox)
    }
}
