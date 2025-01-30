package io.github.taalaydev.fluttervox.flutter_vox.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.util.Log

class ServiceController(
    private val context: Context,
    private val channel: MethodChannel
) {
    private var voiceService: VoiceAssistantService? = null
    private var bound = false

    private val _serviceStatus = MutableStateFlow<ServiceStatus>(ServiceStatus.Stopped)
    val serviceStatus: StateFlow<ServiceStatus> = _serviceStatus

    sealed class ServiceStatus {
        object Running : ServiceStatus()
        object Stopped : ServiceStatus()
        data class Error(val message: String) : ServiceStatus()
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as VoiceAssistantService.LocalBinder
            voiceService = binder.getService()
            bound = true
            _serviceStatus.value = ServiceStatus.Running

            // Notify Flutter about service connection
            channel.invokeMethod("onServiceConnected", null)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            voiceService = null
            bound = false
            _serviceStatus.value = ServiceStatus.Stopped

            // Notify Flutter about service disconnection
            channel.invokeMethod("onServiceDisconnected", null)
        }
    }

    fun startService() {
        try {
            // Create notification channel for Android O and above
            createNotificationChannel()

            // Start the service
            val serviceIntent = Intent(context, VoiceAssistantService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }

            // Bind to the service
            bindService()
        } catch (e: Exception) {
            Log.e("FlutterVox", "Failed to start service")
            _serviceStatus.value = ServiceStatus.Error("Failed to start service: ${e.message}")
        }
    }

    fun stopService() {
        try {
            // Unbind from service
            if (bound) {
                context.unbindService(serviceConnection)
                bound = false
            }

            // Stop the service
            val serviceIntent = Intent(context, VoiceAssistantService::class.java)
            context.stopService(serviceIntent)

            _serviceStatus.value = ServiceStatus.Stopped
        } catch (e: Exception) {
            Log.e("FlutterVox", "Failed to stop service")
            _serviceStatus.value = ServiceStatus.Error("Failed to stop service: ${e.message}")
        }
    }

    fun bindService() {
        try {
            val serviceIntent = Intent(context, VoiceAssistantService::class.java)
            context.bindService(
                serviceIntent,
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
        } catch (e: Exception) {
            Log.e("FlutterVox", "Failed to bind service")
            _serviceStatus.value = ServiceStatus.Error("Failed to bind service: ${e.message}")
        }
    }

    fun unbindService() {
        if (bound) {
            context.unbindService(serviceConnection)
            bound = false
            _serviceStatus.value = ServiceStatus.Stopped
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = NOTIFICATION_CHANNEL_ID
            val channelName = "Voice Assistant Service"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Voice assistant background service"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createNotification(isListening: Boolean): NotificationCompat.Builder {
        // Create intent for notification click
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        // Create action buttons
        val toggleIntent = Intent(context, VoiceAssistantService::class.java).apply {
            action = if (isListening) ACTION_STOP_LISTENING else ACTION_START_LISTENING
        }
        val togglePendingIntent = PendingIntent.getService(
            context,
            1,
            toggleIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Voice Assistant")
            .setContentText(if (isListening) "Listening..." else "Ready")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(
                if (isListening) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isListening) "Stop" else "Start",
                togglePendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "voice_assistant_channel"
        const val NOTIFICATION_ID = 1

        const val ACTION_START_LISTENING = "com.example.fluttervox.action.START_LISTENING"
        const val ACTION_STOP_LISTENING = "com.example.fluttervox.action.STOP_LISTENING"
    }
}