package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.log10

class AudioAnalyzer {
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private var bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // State flows for UI observations
    private val _livePitch = MutableStateFlow(0f)
    val livePitch = _livePitch.asStateFlow()

    private val _liveVolume = MutableStateFlow(0f)
    val liveVolume = _liveVolume.asStateFlow()

    private val _pitchHistory = MutableStateFlow<List<Float>>(emptyList())
    val pitchHistory = _pitchHistory.asStateFlow()

    init {
        if (bufferSize < 2048) {
            bufferSize = 2048
        }
    }

    @SuppressLint("MissingPermission")
    fun startAnalyzing() {
        if (isRecording) return
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return
            }

            audioRecord?.startRecording()
            isRecording = true
            _pitchHistory.value = emptyList()

            recordingJob = scope.launch {
                val buffer = ShortArray(bufferSize)
                while (isRecording) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        // Calculate Volume in dB
                        var sum = 0.0
                        for (i in 0 until readSize) {
                            sum += buffer[i] * buffer[i]
                        }
                        val rms = Math.sqrt(sum / readSize)
                        val db = if (rms > 0) 20 * log10(rms) else 0.0
                        val volumeNorm = (db.toFloat() / 90f).coerceIn(0f, 1f)
                        _liveVolume.value = volumeNorm

                        // Detect Pitch using Autocorrelation
                        val pitch = detectPitch(buffer, readSize)
                        if (pitch in 50f..500f && volumeNorm > 0.1f) {
                            _livePitch.value = pitch
                            val currentHistory = _pitchHistory.value.toMutableList()
                            if (currentHistory.size > 50) {
                                currentHistory.removeAt(0)
                            }
                            currentHistory.add(pitch)
                            _pitchHistory.value = currentHistory
                        } else {
                            _livePitch.value = 0f
                        }
                    }
                    Thread.sleep(50) // Analyze roughly 20 times per second
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopAnalyzing() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioRecord = null
    }

    private fun detectPitch(buffer: ShortArray, size: Int): Float {
        var maxCorrelation = 0.0
        var bestLag = -1

        // Normal human vocal frequencies are typically between 75 Hz (deep male) and 400 Hz (high female/child)
        val minLag = sampleRate / 400
        val maxLag = sampleRate / 75

        if (minLag >= maxLag || maxLag >= size) return 0f

        // Autocorrelation algorithm
        for (lag in minLag..maxLag) {
            var correlation = 0.0
            for (i in 0 until (size - lag)) {
                correlation += abs(buffer[i].toDouble() * buffer[i + lag].toDouble())
            }
            if (correlation > maxCorrelation) {
                maxCorrelation = correlation
                bestLag = lag
            }
        }

        return if (bestLag > 0) {
            sampleRate.toFloat() / bestLag
        } else {
            0f
        }
    }
}
