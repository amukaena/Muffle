package com.muffle.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.random.Random

class BrownNoiseGenerator {

    private var audioTrack: AudioTrack? = null
    private var playbackThread: Thread? = null

    @Volatile
    private var isPlaying = false

    @Volatile
    private var volume: Float = DEFAULT_VOLUME

    fun start() {
        if (isPlaying) return
        isPlaying = true

        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack = track
        track.play()

        playbackThread = Thread {
            generateBrownNoise(track, bufferSize)
        }.apply {
            name = "BrownNoiseThread"
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun stop() {
        isPlaying = false
        playbackThread?.join(THREAD_JOIN_TIMEOUT)
        playbackThread = null
        audioTrack?.apply {
            stop()
            release()
        }
        audioTrack = null
    }

    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
        audioTrack?.setVolume(volume)
    }

    fun getVolume(): Float = volume

    private fun generateBrownNoise(track: AudioTrack, bufferSize: Int) {
        val buffer = ShortArray(bufferSize / 2)
        var lastSample = 0.0

        while (isPlaying) {
            for (i in buffer.indices) {
                val white = Random.nextDouble(-STEP_SIZE, STEP_SIZE)
                lastSample = (lastSample * LEAK_FACTOR + white).coerceIn(-SAMPLE_CLAMP, SAMPLE_CLAMP)
                buffer[i] = lastSample.toInt().toShort()
            }
            if (isPlaying) {
                track.write(buffer, 0, buffer.size)
            }
        }
    }

    companion object {
        const val DEFAULT_VOLUME = 0.7f

        private const val SAMPLE_RATE = 44100
        private const val LEAK_FACTOR = 0.998
        private const val STEP_SIZE = 800.0
        private const val SAMPLE_CLAMP = 32000.0
        private const val THREAD_JOIN_TIMEOUT = 1000L
    }
}
