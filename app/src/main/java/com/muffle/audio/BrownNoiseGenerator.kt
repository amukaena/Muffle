package com.muffle.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.min
import kotlin.math.max

class BrownNoiseGenerator {

    private var audioTrack: AudioTrack? = null
    private var playbackThread: Thread? = null

    @Volatile
    private var isPlaying = false

    @Volatile
    private var volume: Float = 0.7f

    fun start() {
        if (isPlaying) return
        isPlaying = true

        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
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
                    .setSampleRate(sampleRate)
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
            generateBrownNoise(track, bufferSize, sampleRate)
        }.apply {
            name = "BrownNoiseThread"
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun stop() {
        isPlaying = false
        playbackThread?.join(1000)
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

    private fun generateBrownNoise(track: AudioTrack, bufferSize: Int, @Suppress("UNUSED_PARAMETER") sampleRate: Int) {
        val buffer = ShortArray(bufferSize / 2)
        var lastSample = 0.0

        // 브라운 노이즈: 이전 샘플에 작은 랜덤 변화를 누적
        // leakFactor로 DC 드리프트 방지
        val leakFactor = 0.998
        val stepSize = 800.0

        while (isPlaying) {
            for (i in buffer.indices) {
                val white = (Math.random() * 2.0 - 1.0) * stepSize
                lastSample = lastSample * leakFactor + white
                lastSample = max(-32000.0, min(32000.0, lastSample))
                buffer[i] = lastSample.toInt().toShort()
            }
            if (isPlaying) {
                track.write(buffer, 0, buffer.size)
            }
        }
    }
}
