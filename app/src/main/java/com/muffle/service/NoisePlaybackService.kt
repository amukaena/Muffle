package com.muffle.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.muffle.MainActivity
import com.muffle.MuffleApplication
import com.muffle.R
import com.muffle.audio.BrownNoiseGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

class NoisePlaybackService : Service() {

    private val binder = LocalBinder()
    private val noiseGenerator = BrownNoiseGenerator()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var stopJob: Job? = null

    var isPlaying: Boolean = false
        private set

    var stopTimeMillis: Long = 0L
        private set

    var onStateChanged: (() -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): NoisePlaybackService = this@NoisePlaybackService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopPlayback()
        }
        return START_STICKY
    }

    fun startPlayback(stopAtMillis: Long, volume: Float) {
        if (isPlaying) return

        stopTimeMillis = stopAtMillis
        noiseGenerator.setVolume(volume)
        noiseGenerator.start()
        isPlaying = true

        startForeground(NOTIFICATION_ID, buildNotification())
        scheduleStop(stopAtMillis)
        onStateChanged?.invoke()
    }

    fun stopPlayback() {
        if (!isPlaying) return

        cancelStopJob()
        noiseGenerator.stop()
        isPlaying = false

        stopForeground(STOP_FOREGROUND_REMOVE)
        onStateChanged?.invoke()
    }

    fun setVolume(volume: Float) {
        noiseGenerator.setVolume(volume)
    }

    fun updateStopTime(stopAtMillis: Long) {
        stopTimeMillis = stopAtMillis
        if (isPlaying) {
            scheduleStop(stopAtMillis)
            updateNotification()
        }
    }

    private fun scheduleStop(stopAtMillis: Long) {
        cancelStopJob()
        val delayMs = stopAtMillis - System.currentTimeMillis()
        if (delayMs <= 0) {
            stopPlayback()
            return
        }
        stopJob = serviceScope.launch {
            delay(delayMs)
            stopPlayback()
        }
    }

    private fun cancelStopJob() {
        stopJob?.cancel()
        stopJob = null
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PENDING_INTENT_FLAGS
        )

        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, NoisePlaybackService::class.java).apply {
                action = ACTION_STOP
            },
            PENDING_INTENT_FLAGS
        )

        return NotificationCompat.Builder(this, MuffleApplication.CHANNEL_ID)
            .setContentTitle("Muffle 재생 중")
            .setContentText("${formatTime(stopTimeMillis)}에 종료 예정")
            .setSmallIcon(R.drawable.ic_noise)
            .setContentIntent(contentIntent)
            .addAction(R.drawable.ic_stop, "정지", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun formatTime(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return String.format(
            "%02d:%02d",
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE)
        )
    }

    override fun onDestroy() {
        stopPlayback()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.muffle.ACTION_STOP"
        private const val PENDING_INTENT_FLAGS =
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }
}
