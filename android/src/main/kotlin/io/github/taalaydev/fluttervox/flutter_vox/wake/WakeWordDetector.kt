package io.github.taalaydev.fluttervox.flutter_vox.wake

import android.Manifest
import android.content.Context
import android.util.Log
import edu.cmu.pocketsphinx.Assets
import edu.cmu.pocketsphinx.Hypothesis
import edu.cmu.pocketsphinx.RecognitionListener
import edu.cmu.pocketsphinx.SpeechRecognizer
import edu.cmu.pocketsphinx.SpeechRecognizerSetup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class WakeWordDetector(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val config: WakeWordConfig = WakeWordConfig()
): RecognitionListener {
    data class WakeWordConfig(
        val wakeWord: String = "assistant",
        val language: String = "en-US",
        val partialResults: Boolean = true,
        val continuousListening: Boolean = true
    )

    private var recognizer: SpeechRecognizer? = null
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _wakeWordDetected = MutableSharedFlow<Unit>()
    val wakeWordDetected: SharedFlow<Unit> = _wakeWordDetected

    private var isInitialized = false

    suspend fun initialize() {
        setup()
    }

    fun startListening() {
        recognizer?.startListening(WAKE_WORD_SEARCH, 10000)
    }

    fun stopListening() {
        recognizer?.stop()
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "onBeginningOfSpeech")
    }

    override fun onEndOfSpeech() {
        Log.d(TAG, "onEndOfSpeech")
    }

    override fun onPartialResult(hypothesis: Hypothesis?) {
        if (hypothesis == null) {
            return
        }

        val text = hypothesis.hypstr
        if (text.contains(config.wakeWord)) {
            coroutineScope.launch {
                _wakeWordDetected.emit(Unit)
            }
        }
    }

    override fun onResult(hypothesis: Hypothesis?) {
        if (hypothesis == null) {
            return
        }

        val text = hypothesis.hypstr
        if (text.contains(config.wakeWord)) {
            coroutineScope.launch {
                // _wakeWordDetected.emit(Unit)
            }
        }
    }

    override fun onError(error: Exception?) {
        error?.printStackTrace()
    }

    override fun onTimeout() {}

    fun destroy() {
        recognizer?.shutdown()
        recognizer = null
        isInitialized = false
        _isListening.value = false
    }

    private suspend fun setup() = withContext(Dispatchers.IO) {
        if (isInitialized) {
            Log.w(TAG, "setup() called - already initialized (this is ok if wake word changed)")
        } else {
            Log.d(TAG, "setup() called")
            isInitialized = true
        }

        // Load the wake word from shared prefs
        val wakeWordString = config.wakeWord
        val keywordThreshold = ("1.e-" + 2 * DEFAULT_SENSITIVITY).toFloat()
        Log.d(
            TAG,
            "setup: wake word is: $wakeWordString, sensitivity is: $keywordThreshold [$DEFAULT_SENSITIVITY]"
        )
        try {
            if (recognizer != null) {
                // Investigate if seen
                Log.e(
                    TAG,
                    "setup: warning already running: " + recognizer!!.searchName,
                    RuntimeException()
                )
                destroy()
            }

            val assets = Assets(context)
            val assetsDir = assets.syncAssets()
            val speechRecognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(File(assetsDir, "en-us-ptm"))
                .setDictionary(File(assetsDir, "lm/words.dic"))
                .setBoolean("-remove_noise", false)
                .setFloat("-vad_threshold", 2.7)
                .setKeywordThreshold(KWS_THRESHOLD) // Uncomment for a lot of raw logging (takes up a lot of space on the device)
                // .setRawLogDir(assetsDir)
                .recognizer
            speechRecognizer.addKeyphraseSearch(WAKE_WORD_SEARCH, wakeWordString)
            speechRecognizer.addListener(this@WakeWordDetector)
            speechRecognizer.startListening(WAKE_WORD_SEARCH)
            recognizer = speechRecognizer
            Log.d(TAG, "setup: listening...")
        } catch (e: IOException) {
            Log.e(TAG, "Caught: $e")
        }
    }

    companion object {
        private val WAKE_WORD_SEARCH = "WAKE_WORD_SEARCH"
        private val KWS_THRESHOLD = 1e-15f
        private val DEFAULT_SENSITIVITY = 3
        private const val TAG = "WakeWordDetector"
    }
}