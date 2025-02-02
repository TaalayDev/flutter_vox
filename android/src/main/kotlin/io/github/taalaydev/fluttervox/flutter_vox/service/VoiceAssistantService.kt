package io.github.taalaydev.fluttervox.flutter_vox.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.annotation.NonNull
import io.flutter.FlutterInjector
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.Result
import io.github.taalaydev.fluttervox.flutter_vox.wake.WakeWordDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VoiceAssistantService : Service() {
    private val binder = LocalBinder()
    private lateinit var wakeWordDetector: WakeWordDetector
    private lateinit var serviceController: ServiceController
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private val CHANNEL_NAME = "flutter_vox.service"
    private var flutterEngine: FlutterEngine? = null
    private var methodChannel: MethodChannel? = null

    inner class LocalBinder : Binder() {
        fun getService(): VoiceAssistantService = this@VoiceAssistantService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("FlutterVox","VoiceAssistantService created")

        setupFlutterEngine()
        initializeComponents()
        startForeground()
    }

    private fun setupFlutterEngine() {
        flutterEngine = FlutterEngine(this)

        // Initialize Flutter engine with your Dart entrypoint
        flutterEngine?.let { engine ->
            val dartEntrypoint = DartExecutor.DartEntrypoint(
                FlutterInjector.instance().flutterLoader().findAppBundlePath(),
                "backgroundMain" // This is the Dart entrypoint function name
            )
            engine.dartExecutor.executeDartEntrypoint(dartEntrypoint)

            // Setup method channel for communication
            methodChannel = MethodChannel(engine.dartExecutor.binaryMessenger, CHANNEL_NAME)
            methodChannel?.setMethodCallHandler { call, result ->
                onMethodCall(call, result)
            }
        }
    }

    private fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "initialize" -> handleInitialize(call, result)
            "startListening" -> startListening(result)
            "stopListening" -> stopListening(result)
            "isListening" -> handleIsListening(result)
            else -> result.notImplemented()
        }
    }

    private fun initializeComponents() {
        // Initialize wake word detector
        wakeWordDetector = WakeWordDetector(
            context = this,
            coroutineScope = serviceScope,
            config = WakeWordDetector.WakeWordConfig(
                wakeWord = "hey assistant",
                continuousListening = true
            )
        )
    }

    private fun handleInitialize(call: MethodCall, result: Result) {
        serviceScope.launch {
            try {
                wakeWordDetector.initialize()
                // Monitor listening states
                monitorListeningStates()
                result.success(null)
            } catch (e: Exception) {
                Log.d("FlutterVox", "Failed to initialize voice components")
                result.error("initialization_failed", "Failed to initialize voice components", null)
            }
        }
    }

    private fun startForeground() {
        val notification = serviceController.createNotification(false)
        startForeground(ServiceController.NOTIFICATION_ID, notification.build())
    }

    private fun monitorListeningStates() {
        serviceScope.launch {
            // Monitor wake word detector state
            wakeWordDetector.isListening.collectLatest { isListening ->
                updateNotification(isListening)
            }
        }
    }

    private fun updateNotification(isListening: Boolean) {
        val notification = serviceController.createNotification(isListening)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(ServiceController.NOTIFICATION_ID, notification.build())
    }

    fun startListening(result: Result? = null) {
        wakeWordDetector.startListening()
        serviceScope.launch {
            wakeWordDetector.wakeWordDetected.collect {
                methodChannel?.invokeMethod("onWakeWordDetected", null)

                result?.success(null)
            }
        }
    }

    fun stopListening(result: Result? = null) {
        wakeWordDetector.stopListening()

        result?.success(null)
    }

    private fun handleIsListening(result: Result) {
        result.success(wakeWordDetector.isListening.value)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ServiceController.ACTION_START_LISTENING -> startListening()
            ServiceController.ACTION_STOP_LISTENING -> stopListening()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListening()
        wakeWordDetector.destroy()
    }

}