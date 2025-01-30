package io.github.taalaydev.fluttervox.flutter_vox.command

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CommandProcessor(
    private val context: Context,
    private val config: CommandConfig = CommandConfig()
) {
    data class CommandConfig(
        val language: String = "en-US",
        val timeout: Long = 5000L, // Command timeout in milliseconds
        val commandThreshold: Float = 0.8f // Confidence threshold for commands
    )

    private var speechRecognizer: SpeechRecognizer? = null
    private val commands = mutableMapOf<CommandPattern, CommandAction>()
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    // Functional interface for command actions
    fun interface CommandAction {
        operator fun invoke(params: Map<String, String>)
    }

    // Command pattern with parameter extraction
    data class CommandPattern(
        val pattern: String,
        val parameterNames: List<String> = emptyList()
    ) {
        private val regex = pattern
            .replace("""\{(\w+)\}""".toRegex()) { matchResult ->
                "(?<${matchResult.groupValues[1]}>\\w+)"
            }
            .toRegex(RegexOption.IGNORE_CASE)

        fun matches(input: String): Boolean = regex.matches(input)

        fun extractParams(input: String): Map<String, String> {
            val matchResult = regex.matchEntire(input) ?: return emptyMap()
            return parameterNames.associateWith { param ->
                matchResult.groups[param]?.value ?: ""
            }
        }
    }

    fun initialize() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            throw IllegalStateException("Speech recognition is not available on this device")
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createRecognitionListener())
        }
    }

    fun addCommand(pattern: String, parameterNames: List<String> = emptyList(), action: CommandAction) {
        commands[CommandPattern(pattern, parameterNames)] = action
    }

    fun removeCommand(pattern: String) {
        commands.keys.find { it.pattern == pattern }?.let { commands.remove(it) }
    }

    fun startListening(
        onCommandRecognized: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (_isListening.value) {
            Log.d("FlutterVox", "Already listening for commands")
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
        commands.clear()
    }

    private fun createRecognizerIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, config.language)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            // Disable beep sound
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 500)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 500)
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("FlutterVox", "Ready for command")
            }

            override fun onBeginningOfSpeech() {
                Log.d("FlutterVox", "Beginning of command")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Can be used to show voice amplitude
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Raw audio buffer - not used in this implementation
            }

            override fun onEndOfSpeech() {
                Log.d("FlutterVox", "End of command")
                _isListening.value = false
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val scores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)

                matches?.forEachIndexed { index, result ->
                    val confidence = scores?.getOrNull(index) ?: 0f
                    if (confidence >= config.commandThreshold) {
                        processCommand(result)
                    }
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

                Log.e("FlutterVox", "Command recognition error: $errorMessage")
                _isListening.value = false
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // Not used in command processing
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Reserved for future use
            }
        }
    }

    private fun processCommand(input: String) {
        for ((pattern, action) in commands) {
            if (pattern.matches(input)) {
                val params = pattern.extractParams(input)
                action(params)
                return
            }
        }
        Log.d("FlutterVox", "No matching command found for: $input")
    }
}