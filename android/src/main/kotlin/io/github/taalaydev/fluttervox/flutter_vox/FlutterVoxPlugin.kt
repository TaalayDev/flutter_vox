package io.github.taalaydev.fluttervox.flutter_vox

import android.content.Context
import androidx.annotation.NonNull
import androidx.lifecycle.lifecycleScope
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.github.taalaydev.fluttervox.flutter_vox.wake.WakeWordDetector
import io.github.taalaydev.fluttervox.flutter_vox.service.ServiceController
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FlutterVoxPlugin: FlutterPlugin, MethodCallHandler {
  private lateinit var channel: MethodChannel
  private lateinit var context: Context
  private lateinit var wakeDetector: WakeWordDetector
  private lateinit var serviceController: ServiceController

  private val scope = CoroutineScope(Dispatchers.Main + Job())
  private var isInitialized = false

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    context = flutterPluginBinding.applicationContext
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_vox")
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when (call.method) {
      "initialize" -> handleInitialize(call, result)
      "startListening" -> handleStartListening(result)
      "stopListening" -> handleStopListening(result)
      "isListening" -> handleIsListening(result)
      "setLauncherMode" -> handleSetLauncherMode(call, result)
      "setBackgroundMode" -> handleSetBackgroundMode(call, result)
      else -> result.notImplemented()
    }
  }

  private fun handleInitialize(call: MethodCall, result: Result) {
    try {
      val wakePath = call.argument<String>("wakePath")
      val wakeWord = call.argument<String>("wakeWord")
      val config = call.argument<Map<String, Any>>("config")

      if (isInitialized) {
        result.error("already_initialized", "Plugin is already initialized", null)
        return
      }

      scope.launch {
        initializeComponents(wakePath, wakeWord, config)
        isInitialized = true
        result.success(null)
      }
    } catch (e: Exception) {
      result.error("initialization_failed", "Failed to initialize plugin: ${e.message}", null)
    }
  }

  private suspend fun initializeComponents(
    wakePath: String?,
    wakeWord: String?,
    config: Map<String, Any>?
  ) {

    wakeDetector = WakeWordDetector(
      context = context,
      coroutineScope = scope,
      config = WakeWordDetector.WakeWordConfig(
        wakeWord = wakeWord ?: "hey assistant",
        continuousListening = config?.get("continuousListening") as? Boolean ?: true
      )
    )
    serviceController = ServiceController(context, channel)

    wakeDetector.initialize()
  }


  @OptIn(InternalCoroutinesApi::class)
  private fun handleStartListening(result: Result) {
    if (!isInitialized) {
      result.error("not_initialized", "Plugin is not initialized", null)
      return
    }

    try {
      wakeDetector.startListening()
      scope.launch(Dispatchers.Main) {
        wakeDetector.wakeWordDetected.collect { _ ->
          channel.invokeMethod("onWakeWordDetected", null)
        }
      }

      result.success(null)
    } catch (e: Exception) {
      result.error("start_listening_failed", "Failed to start listening: ${e.message}", null)
    }
  }

  private fun handleStopListening(result: Result?) {
    if (!isInitialized) {
      result?.error("not_initialized", "Plugin is not initialized", null)
      return
    }

    try {
      wakeDetector.stopListening()
      result?.success(null)
    } catch (e: Exception) {
      result?.error("stop_listening_failed", "Failed to stop listening: ${e.message}", null)
    }
  }

  private fun handleIsListening(result: Result) {
    if (!isInitialized) {
      result.error("not_initialized", "Plugin is not initialized", null)
      return
    }

    try {
      val isListening = wakeDetector.isListening.value
      result.success(isListening)
    } catch (e: Exception) {
      result.error("status_check_failed", "Failed to check listening status: ${e.message}", null)
    }
  }

  private fun handleGetAvailableCommands(result: Result) {
    if (!isInitialized) {
      result.error("not_initialized", "Plugin is not initialized", null)
      return
    }

    try {
      // This would need to be implemented in CommandProcessor
      result.success(emptyList<String>())
    } catch (e: Exception) {
      result.error("command_list_failed", "Failed to get commands: ${e.message}", null)
    }
  }

  private fun handleSetLauncherMode(call: MethodCall, result: Result) {
    if (!isInitialized) {
      result.error("not_initialized", "Plugin is not initialized", null)
      return
    }

    try {
      val enabled = call.argument<Boolean>("enabled") ?: false
      // Implementation for launcher mode
      result.success(null)
    } catch (e: Exception) {
      result.error("launcher_mode_failed", "Failed to set launcher mode: ${e.message}", null)
    }
  }

  private fun handleSetBackgroundMode(call: MethodCall, result: Result) {
    if (!isInitialized) {
      result.error("not_initialized", "Plugin is not initialized", null)
      return
    }

    try {
      val enabled = call.argument<Boolean>("enabled") ?: false
      if (enabled) {
        serviceController.startService()
      } else {
        serviceController.stopService()
      }
      result.success(null)
    } catch (e: Exception) {
      result.error("background_mode_failed", "Failed to set background mode: ${e.message}", null)
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    cleanup()
    channel.setMethodCallHandler(null)
  }

  private fun cleanup() {
    if (isInitialized) {
      try {
        wakeDetector.destroy()
        serviceController.stopService()
        isInitialized = false
      } catch (e: Exception) {
        Log.e("FlutterVox", "Error during cleanup: ${e.message}")
      }
    }
  }
}