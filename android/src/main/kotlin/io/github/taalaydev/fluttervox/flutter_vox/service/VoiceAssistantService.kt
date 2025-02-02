package io.github.taalaydev.fluttervox.flutter_vox.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import io.github.taalaydev.fluttervox.flutter_vox.command.CommandProcessor
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
    private lateinit var commandProcessor: CommandProcessor
    private lateinit var serviceController: ServiceController
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    inner class LocalBinder : Binder() {
        fun getService(): VoiceAssistantService = this@VoiceAssistantService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("FlutterVox","VoiceAssistantService created")

        initializeComponents()
        startForeground()
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

        // Initialize command processor
        commandProcessor = CommandProcessor(
            context = this,
            config = CommandProcessor.CommandConfig(
                timeout = 5000L
            )
        )

        serviceScope.launch {
            try {
                wakeWordDetector.initialize()
                commandProcessor.initialize()
                registerCommands()

                // Monitor listening states
                monitorListeningStates()
            } catch (e: Exception) {
                Log.d("FlutterVox", "Failed to initialize voice components")
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

        serviceScope.launch {
            // Monitor command processor state
            commandProcessor.isListening.collectLatest { isListening ->
                updateNotification(isListening)
            }
        }
    }

    private fun updateNotification(isListening: Boolean) {
        val notification = serviceController.createNotification(isListening)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(ServiceController.NOTIFICATION_ID, notification.build())
    }

    private fun registerCommands() {
        // System commands
        commandProcessor.addCommand("stop listening") {
            stopListening()
        }

        // Add more default commands here
        commandProcessor.addCommand(
            pattern = "open {app}",
            parameterNames = listOf("app")
        ) { params ->
            val appName = params["app"] ?: return@addCommand
            launchApp(appName)
        }

        commandProcessor.addCommand(
            pattern = "set volume to {level}",
            parameterNames = listOf("level")
        ) { params ->
            val level = params["level"]?.toIntOrNull() ?: return@addCommand
            setVolume(level)
        }
    }

    fun startListening() {
        wakeWordDetector.startListening()
        serviceScope.launch {
            wakeWordDetector.wakeWordDetected.collect {
                // Wake word detected, start listening for commands
                startCommandDetection()
            }
        }
    }

    fun stopListening() {
        wakeWordDetector.stopListening()
        commandProcessor.stopListening()
    }

    private fun startCommandDetection() {
        // Play sound or vibrate to indicate active listening
        playListeningIndicator()

        commandProcessor.startListening(
            onCommandRecognized = { command ->
                Log.d("FlutterVox","Command recognized: $command")
            },
            onError = { error ->
                Log.e("FlutterVox", "Command recognition error")
            }
        )
    }

    private fun playListeningIndicator() {
        // Implement sound/vibration feedback
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
        commandProcessor.destroy()
    }

    private fun launchApp(appName: String) {
        val packageManager = applicationContext.packageManager
        try {
            val intent = packageManager.getLaunchIntentForPackage(appName)
                ?: packageManager.getLeanbackLaunchIntentForPackage(appName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } else {
                Log.e("FlutterVox","No launch intent for app: $appName")
            }
        } catch (e: Exception) {
            Log.e("FlutterVox", "Error launching app: $appName")
        }
    }

    private fun setVolume(level: Int) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val scaledLevel = (level.coerceIn(0, 100) * maxVolume / 100)
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            scaledLevel,
            AudioManager.FLAG_SHOW_UI
        )
    }
}