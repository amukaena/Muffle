package com.muffle.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.muffle.MainActivity
import com.muffle.MuffleApplication
import com.muffle.R
import com.muffle.audio.BrownNoiseGenerator
import java.util.Timer
import java.util.TimerTask

class NoisePlaybackService : Service() {

    private val binder = LocalBinder()
    private val noiseGenerator = BrownNoiseGenerator()
    private var stopTimer: Timer? = null

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
        when (intent?.action) {
            ACTION_STOP -> stopPlayback()
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

        cancelTimer()
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
            cancelTimer()
            scheduleStop(stopAtMillis)
            updateNotification()
        }
    }

    private fun scheduleStop(stopAtMillis: Long) {
        cancelTimer()
        val delay = stopAtMillis - System.currentTimeMillis()
        if (delay <= 0) {
            stopPlayback()
            return
        }
        stopTimer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    stopPlayback()
                }
            }, delay)
        }
    }

    private fun cancelTimer() {
        stopTimer?.cancel()
        stopTimer = null
    }

    private fun buildNotification(): android.app.Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, NoisePlaybackService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopTimeText = formatTime(stopTimeMillis)

        return NotificationCompat.Builder(this, MuffleApplication.CHANNEL_ID)
            .setContentTitle("Muffle 재생 중")
            .setContentText("${stopTimeText}에 종료 예정")
            .setSmallIcon(R.drawable.ic_noise)
            .setContentIntent(contentIntent)
            .addAction(R.drawable.ic_stop, "정지", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun formatTime(millis: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = cal.get(java.util.Calendar.MINUTE)
        return String.format("%02d:%02d", hour, minute)
    }

    override fun onDestroy() {
        stopPlayback()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.muffle.ACTION_STOP"
    }
}
