package com.muffle.ui.screen

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.muffle.service.NoisePlaybackService
import com.muffle.ui.theme.SleepBlue
import com.muffle.ui.theme.StopRed
import com.muffle.viewmodel.MainUiState
import com.muffle.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var service by remember { mutableStateOf<NoisePlaybackService?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 거부해도 서비스는 동작, 알림만 안 보임 */ }

    val connection = rememberServiceConnection(
        onConnected = { svc ->
            svc.onStateChanged = { viewModel.setPlaying(svc.isPlaying) }
            viewModel.setPlaying(svc.isPlaying)
            service = svc
        },
        onDisconnected = { service = null }
    )

    DisposableEffect(Unit) {
        val intent = Intent(context, NoisePlaybackService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        onDispose {
            service?.onStateChanged = null
            context.unbindService(connection)
        }
    }

    LaunchedEffect(uiState.isPlaying) {
        while (uiState.isPlaying) {
            viewModel.updateRemainingTime()
            delay(60_000L)
        }
        viewModel.updateRemainingTime()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppTitle()
            Spacer(modifier = Modifier.height(48.dp))
            StopTimeSection(
                uiState = uiState,
                onTimeClick = { showTimePicker = true }
            )
            Spacer(modifier = Modifier.height(48.dp))
            PlayStopButton(
                isPlaying = uiState.isPlaying,
                onClick = {
                    val svc = service ?: return@PlayStopButton
                    if (svc.isPlaying) {
                        svc.stopPlayback()
                    } else {
                        requestNotificationPermissionIfNeeded(context, notificationPermissionLauncher)
                        val stopMillis = viewModel.calculateStopTimeMillis()
                        ContextCompat.startForegroundService(
                            context,
                            Intent(context, NoisePlaybackService::class.java)
                        )
                        svc.startPlayback(stopMillis, uiState.volume)
                    }
                }
            )
            Spacer(modifier = Modifier.height(48.dp))
            VolumeControl(
                volume = uiState.volume,
                onVolumeChange = { vol ->
                    viewModel.setVolume(vol)
                    service?.setVolume(vol)
                }
            )
        }
    }

    if (showTimePicker) {
        StopTimePickerDialog(
            initialHour = uiState.stopHour,
            initialMinute = uiState.stopMinute,
            onConfirm = { hour, minute ->
                viewModel.setStopTime(hour, minute)
                service?.updateStopTime(viewModel.calculateStopTimeMillis(hour, minute))
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
private fun AppTitle() {
    Text(
        text = "Muffle",
        fontSize = 32.sp,
        fontWeight = FontWeight.Light,
        color = MaterialTheme.colorScheme.onBackground,
        letterSpacing = 4.sp
    )
}

@Composable
private fun StopTimeSection(uiState: MainUiState, onTimeClick: () -> Unit) {
    Text(
        text = "종료 시각",
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))
    TextButton(onClick = onTimeClick) {
        Text(
            text = String.format("%02d:%02d", uiState.stopHour, uiState.stopMinute),
            fontSize = 48.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    if (uiState.isPlaying && uiState.remainingTimeText.isNotEmpty()) {
        Text(
            text = uiState.remainingTimeText,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PlayStopButton(isPlaying: Boolean, onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(80.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (isPlaying) StopRed else SleepBlue
        )
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "정지" else "재생",
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun VolumeControl(volume: Float, onVolumeChange: (Float) -> Unit) {
    Text(
        text = "${(volume * 100).toInt()}%",
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(onClick = { onVolumeChange((volume - VOLUME_STEP).coerceIn(0f, 1f)) }) {
            Icon(
                Icons.AutoMirrored.Filled.VolumeDown,
                contentDescription = "볼륨 줄이기",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        Slider(
            value = volume,
            onValueChange = onVolumeChange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        IconButton(onClick = { onVolumeChange((volume + VOLUME_STEP).coerceIn(0f, 1f)) }) {
            Icon(
                Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "볼륨 높이기",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StopTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}

@Composable
private fun rememberServiceConnection(
    onConnected: (NoisePlaybackService) -> Unit,
    onDisconnected: () -> Unit,
): ServiceConnection = remember {
    object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val svc = (binder as NoisePlaybackService.LocalBinder).getService()
            onConnected(svc)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            onDisconnected()
        }
    }
}

private fun requestNotificationPermissionIfNeeded(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<String>,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED
    ) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

private const val VOLUME_STEP = 0.05f
