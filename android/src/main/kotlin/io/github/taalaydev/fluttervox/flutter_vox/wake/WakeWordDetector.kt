package io.github.taalaydev.fluttervox.flutter_vox.wake

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class WakeWordDetector(
    private val context: Context,
    private val config: WakeWordConfig = WakeWordConfig()
) {
    data class WakeWordConfig(
        val wakeWords: Set<String> = setOf("hey assistant", "ok assistant"),
        val language: String = "en-US",
        val partialResults: Boolean = true,
        val continuousListening: Boolean = true
    )

    private var speechRecognizer: SpeechRecognizer? = null
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    fun initialize() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            throw IllegalStateException("Speech recognition is not available on this device")
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createRecognitionListener())
        }
    }

    fun startListening(
        onWakeWordDetected: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (_isListening.value) {
            Log.d("FlutterVox", "Already listening")
            return
        }

        val recognizerIntent = createRecognizerIntent()
        try {
            speechRecognizer?.startListening(recognizerIntent)
            _isListening.value = true
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
    }

    fun destroy() {
        speechRecognizer?.apply {
            cancel()
            destroy()
        }
        speechRecognizer = null
        _isListening.value = false
    }

    private fun createRecognizerIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, config.language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, config.partialResults)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            // Disable beep sound
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 500)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 500)
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("FlutterVox","Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d("FlutterVox","Beginning of speech")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Can be used to show voice amplitude
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Raw audio buffer - not used in this implementation
            }

            override fun onEndOfSpeech() {
                Log.d("FlutterVox","End of speech")
                if (config.continuousListening) {
                    // Restart listening
                    speechRecognizer?.startListening(createRecognizerIntent())
                } else {
                    _isListening.value = false
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (!config.partialResults) return

                processResults(partialResults)
            }

            override fun onResults(results: Bundle?) {
                processResults(results)

                if (config.continuousListening) {
                    // Restart listening
                    speechRecognizer?.startListening(createRecognizerIntent())
                } else {
                    _isListening.value = false
                }
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error"
                }

                Log.e("FlutterVox","Speech recognition error: $errorMessage")

                if (config.continuousListening && error == SpeechRecognizer.ERROR_NO_MATCH) {
                    // Restart listening on no match
                    speechRecognizer?.startListening(createRecognizerIntent())
                } else {
                    _isListening.value = false
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Reserved for future use
            }
        }
    }

    private fun processResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        matches?.forEach { result ->
            val normalizedResult = result.toLowerCase()
            if (config.wakeWords.any { wakeWord ->
                    normalizedResult.contains(wakeWord)
                }) {
                Log.d("FlutterVox","Wake word detected: $result")
                onWakeWordDetected?.invoke()
                if (!config.continuousListening) {
                    stopListening()
                }
            }
        }
    }

    companion object {
        private var onWakeWordDetected: (() -> Unit)? = null
    }
}