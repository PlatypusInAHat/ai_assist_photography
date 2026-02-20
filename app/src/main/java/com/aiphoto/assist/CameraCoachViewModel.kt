package com.aiphoto.assist

import androidx.lifecycle.ViewModel
import com.aiphoto.assist.composition.CaptureMode
import com.aiphoto.assist.composition.OverlayState
import com.aiphoto.assist.composition.PresetId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel holding the unified [OverlayState] via StateFlow.
 * CameraScreen observes this; RealtimeCoachEngine writes to it.
 */
class CameraCoachViewModel : ViewModel() {

    private val _state = MutableStateFlow(OverlayState())
    val state: StateFlow<OverlayState> = _state.asStateFlow()

    fun setMode(mode: CaptureMode) {
        _state.update { it.copy(mode = mode) }
    }

    fun setPreset(preset: PresetId) {
        _state.update { it.copy(preset = preset) }
    }

    /** Called by RealtimeCoachEngine on every analysed frame */
    fun updateFromEngine(newState: OverlayState) {
        _state.value = newState
    }
}
