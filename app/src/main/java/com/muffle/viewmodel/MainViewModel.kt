package com.muffle.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

data class MainUiState(
    val isPlaying: Boolean = false,
    val stopHour: Int = 7,
    val stopMinute: Int = 0,
    val volume: Float = 0.7f,
    val remainingTimeText: String = "",
)

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun setStopTime(hour: Int, minute: Int) {
        _uiState.value = _uiState.value.copy(stopHour = hour, stopMinute = minute)
    }

    fun setVolume(volume: Float) {
        _uiState.value = _uiState.value.copy(volume = volume)
    }

    fun setPlaying(playing: Boolean) {
        _uiState.value = _uiState.value.copy(isPlaying = playing)
    }

    fun updateRemainingTime() {
        val state = _uiState.value
        if (!state.isPlaying) {
            _uiState.value = state.copy(remainingTimeText = "")
            return
        }
        val stopMillis = calculateStopTimeMillis(state.stopHour, state.stopMinute)
        val remaining = stopMillis - System.currentTimeMillis()
        if (remaining <= 0) {
            _uiState.value = state.copy(remainingTimeText = "종료 시간 도달")
            return
        }
        val hours = remaining / (1000 * 60 * 60)
        val minutes = (remaining % (1000 * 60 * 60)) / (1000 * 60)
        _uiState.value = state.copy(
            remainingTimeText = "${hours}시간 ${minutes}분 남음"
        )
    }

    fun calculateStopTimeMillis(hour: Int = _uiState.value.stopHour, minute: Int = _uiState.value.stopMinute): Long {
        val now = Calendar.getInstance()
        val stop = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // 설정 시각이 현재보다 이전이면 다음 날로
        if (stop.timeInMillis <= now.timeInMillis) {
            stop.add(Calendar.DAY_OF_YEAR, 1)
        }
        return stop.timeInMillis
    }
}
