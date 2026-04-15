package com.muffle.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.muffle.audio.BrownNoiseGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Calendar

data class MainUiState(
    val isPlaying: Boolean = false,
    val stopHour: Int = DEFAULT_STOP_HOUR,
    val stopMinute: Int = DEFAULT_STOP_MINUTE,
    val volume: Float = BrownNoiseGenerator.DEFAULT_VOLUME,
    val remainingTimeText: String = "",
) {
    companion object {
        const val DEFAULT_STOP_HOUR = 7
        const val DEFAULT_STOP_MINUTE = 0
    }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        MainUiState(
            stopHour = prefs.getInt(KEY_STOP_HOUR, MainUiState.DEFAULT_STOP_HOUR),
            stopMinute = prefs.getInt(KEY_STOP_MINUTE, MainUiState.DEFAULT_STOP_MINUTE),
            volume = prefs.getFloat(KEY_VOLUME, BrownNoiseGenerator.DEFAULT_VOLUME),
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun setStopTime(hour: Int, minute: Int) {
        _uiState.update { it.copy(stopHour = hour, stopMinute = minute) }
        prefs.edit()
            .putInt(KEY_STOP_HOUR, hour)
            .putInt(KEY_STOP_MINUTE, minute)
            .apply()
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _uiState.update { it.copy(volume = clamped) }
        prefs.edit().putFloat(KEY_VOLUME, clamped).apply()
    }

    fun setPlaying(playing: Boolean) {
        _uiState.update { it.copy(isPlaying = playing) }
    }

    fun updateRemainingTime() {
        val state = _uiState.value
        if (!state.isPlaying) {
            _uiState.update { it.copy(remainingTimeText = "") }
            return
        }

        val remaining = calculateStopTimeMillis(state.stopHour, state.stopMinute) - System.currentTimeMillis()
        val text = if (remaining <= 0) {
            "종료 시간 도달"
        } else {
            val hours = remaining / MILLIS_PER_HOUR
            val minutes = (remaining % MILLIS_PER_HOUR) / MILLIS_PER_MINUTE
            "${hours}시간 ${minutes}분 남음"
        }
        _uiState.update { it.copy(remainingTimeText = text) }
    }

    fun calculateStopTimeMillis(
        hour: Int = _uiState.value.stopHour,
        minute: Int = _uiState.value.stopMinute,
    ): Long {
        val now = Calendar.getInstance()
        val stop = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return stop.timeInMillis
    }

    companion object {
        private const val PREFS_NAME = "muffle_settings"
        private const val KEY_STOP_HOUR = "stop_hour"
        private const val KEY_STOP_MINUTE = "stop_minute"
        private const val KEY_VOLUME = "volume"
        private const val MILLIS_PER_MINUTE = 1000L * 60
        private const val MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60
    }
}
