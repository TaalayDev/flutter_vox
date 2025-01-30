package io.github.taalaydev.fluttervox.flutter_vox.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Manages audio recording and processing for voice detection.
 * Handles continuous audio recording with configurable parameters and buffer management.
 */
class AudioManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    companion object {
        const val SAMPLE_RATE = 16000 // 16kHz
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val BUFFER_SIZE_FACTOR = 2 // Multiply minimum buffer size by this factor
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var isRecording = false

    private val bufferSize = AudioRecord.getMinBufferSize(
        SAMPLE_RATE,
        CHANNEL_CONFIG,
        AUDIO_FORMAT
    ) * BUFFER_SIZE_FACTOR

    private val audioData = ByteBuffer.allocateDirect(bufferSize)
        .order(ByteOrder.LITTLE_ENDIAN)

    /**
     * Configuration class for audio recording parameters
     */
    data class AudioConfig(
        val sampleRate: Int = SAMPLE_RATE,
        val channelConfig: Int = CHANNEL_CONFIG,
        val audioFormat: Int = AUDIO_FORMAT,
        val bufferSizeFactor: Int = BUFFER_SIZE_FACTOR
    )

    /**
     * Starts audio recording if permissions are granted
     * @param onAudioBuffer Callback for processed audio buffer
     * @param onError Callback for error handling
     */
    fun startRecording(
        onAudioBuffer: (ByteArray) -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (isRecording) {
            Log.d("FlutterVox","Recording is already in progress")
            return
        }

        if (!hasAudioPermission()) {
            onError(SecurityException("Audio permission not granted"))
            return
        }

        try {
            initializeAudioRecord()
            startAudioRecording(onAudioBuffer, onError)
        } catch (e: Exception) {
            onError(e)
            releaseResources()
        }
    }

    /**
     * Stops audio recording and releases resources
     */
    fun stopRecording() {
        isRecording = false
        recordingJob?.cancel()
        releaseResources()
    }

    /**
     * Returns current recording state
     * @return Boolean indicating if recording is in progress
     */
    fun isRecording(): Boolean = isRecording

    /**
     * Returns current audio levels
     * @return Float representing current audio level (0.0 to 1.0)
     */
    fun getAudioLevel(): Float {
        if (!isRecording) return 0f

        val buffer = ShortArray(bufferSize / 2)
        audioRecord?.read(buffer, 0, buffer.size)

        var sum = 0.0
        for (sample in buffer) {
            sum += sample * sample
        }

        val rms = Math.sqrt(sum / buffer.size)
        return (rms / Short.MAX_VALUE).toFloat()
    }

    private fun initializeAudioRecord() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        ).apply {
            if (state != AudioRecord.STATE_INITIALIZED) {
                throw IllegalStateException("Failed to initialize AudioRecord")
            }
        }
    }

    private fun startAudioRecording(
        onAudioBuffer: (ByteArray) -> Unit,
        onError: (Exception) -> Unit
    ) {
        audioRecord?.startRecording()
        isRecording = true

        recordingJob = coroutineScope.launch(Dispatchers.IO) {
            val buffer = ByteArray(bufferSize)

            try {
                while (isRecording && isActive) {
                    val bytesRead = audioRecord?.read(buffer, 0, bufferSize) ?: 0

                    if (bytesRead > 0) {
                        // Process audio data
                        processAudioData(buffer, bytesRead, onAudioBuffer)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e)
                }
                stopRecording()
            }
        }
    }

    private fun processAudioData(
        buffer: ByteArray,
        bytesRead: Int,
        onAudioBuffer: (ByteArray) -> Unit
    ) {
        // Clear previous data
        audioData.clear()

        // Copy new data to direct buffer
        audioData.put(buffer, 0, bytesRead)
        audioData.flip()

        // Create processed buffer
        val processedBuffer = ByteArray(bytesRead)
        audioData.get(processedBuffer)

        // Apply audio processing if needed (e.g., noise reduction, normalization)
        applyAudioProcessing(processedBuffer)

        // Deliver processed buffer
        onAudioBuffer(processedBuffer)
    }

    private fun applyAudioProcessing(buffer: ByteArray) {
        // Add audio processing algorithms here
        // Examples:
        // - Noise reduction
        // - Voice activity detection
        // - Audio normalization
        // For now, we'll just pass through the raw data
    }

    private fun hasAudioPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun releaseResources() {
        try {
            audioRecord?.apply {
                stop()
                release()
            }
            audioRecord = null
        } catch (e: Exception) {
            Log.e("FlutterVox", "Error releasing AudioRecord resources")
        }
    }

    /**
     * Clean up resources when the manager is no longer needed
     */
    fun cleanup() {
        stopRecording()
        coroutineScope.cancel()
    }
}