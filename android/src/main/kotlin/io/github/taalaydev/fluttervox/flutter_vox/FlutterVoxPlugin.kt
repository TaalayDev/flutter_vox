package io.github.taalaydev.fluttervox.flutter_vox

import android.content.Context
import androidx.annotation.NonNull
import androidx.lifecycle.lifecycleScope
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.github.taalaydev.fluttervox.flutter_vox.audio.AudioManager
import io.github.taalaydev.fluttervox.flutter_vox.command.CommandProcessor
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
  private lateinit var audioManager: AudioManager
  private lateinit var wakeDetector: WakeWordDetector
  private lateinit var commandProcessor: CommandProcessor
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
      "addCommand" -> handleAddCommand(call, result)
      "removeCommand" -> handleRemoveCommand(call, result)
      "getAvailableCommands" -> handleGetAvailableCommands(result)
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
    audioManager = AudioManager(context)

    wakeDetector = WakeWordDetector(
      context = context,
      coroutineScope = scope,
      config = WakeWordDetector.WakeWordConfig(
        wakeWord = wakeWord ?: "hey assistant",
        continuousListening = config?.get("continuousListening") as? Boolean ?: true
      )
    )

    commandProcessor = CommandProcessor(
      context = context,
      config = CommandProcessor.CommandConfig(
        language = config?.get("language") as? String ?: "en-US",
        timeout = config?.get("timeout") as? Long ?: 5000L
      )
    )

    serviceController = ServiceController(context, channel)

    wakeDetector.initialize()
    commandProcessor.initialize()
    setupDefaultCommands()
  }

  private fun setupDefaultCommands() {
    commandProcessor.addCommand("stop listening") {
      scope.launch(Dispatchers.Main) {
        handleStopListening(null)
      }
    }
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
          startCommandDetection()
        }
      }

      result.success(null)
    } catch (e: Exception) {
      result.error("start_listening_failed", "Failed to start listening: ${e.message}", null)
    }
  }

  private fun startCommandDetection() {
    commandProcessor.startListening(
      onCommandRecognized = { command ->
        channel.invokeMethod("onCommandRecognized", command)
      },
      onError = { error ->
        channel.invokeMethod("onError", "Command recognition error: ${error.message}")
      }
    )
  }

  private fun handleStopListening(result: Result?) {
    if (!isInitialized) {
      result?.error("not_initialized", "Plugin is not initialized", null)
      return
    }

    try {
      wakeDetector.stopListening()
      commandProcessor.stopListening()
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
      val isListening = wakeDetector.isListening.value || commandProcessor.isListening.value
      result.success(isListening)
    } catch (e: Exception) {
      result.error("status_check_failed", "Failed to check listening status: ${e.message}", null)
    }
  }

  private fun handleAddCommand(call: MethodCall, result: Result) {
    if (!isInitialized) {
      result.error("not_initialized", "Plugin is not initialized", null)
      return
    }

    try {
      val command = call.argument<String>("command")
      val parameters = call.argument<List<String>>("parameters") ?: emptyList()

      if (command == null) {
        result.error("invalid_argument", "Command string is required", null)
        return
      }

      commandProcessor.addCommand(command, parameters) { params ->
        channel.invokeMethod("onCommandExecuted", mapOf(
          "command" to command,
          "parameters" to params
        ))
      }
      result.success(null)
    } catch (e: Exception) {
      result.error("add_command_failed", "Failed to add command: ${e.message}", null)
    }
  }

  private fun handleRemoveCommand(call: MethodCall, result: Result) {
    if (!isInitialized) {
      result.error("not_initialized", "Plugin is not initialized", null)
      return
    }

    try {
      val command = call.argument<String>("command")
      if (command == null) {
        result.error("invalid_argument", "Command string is required", null)
        return
      }

      commandProcessor.removeCommand(command)
      result.success(null)
    } catch (e: Exception) {
      result.error("remove_command_failed", "Failed to remove command: ${e.message}", null)
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
        commandProcessor.destroy()
        audioManager.cleanup()
        serviceController.stopService()
        isInitialized = false
      } catch (e: Exception) {
        Log.e("FlutterVox", "Error during cleanup: ${e.message}")
      }
    }
  }
}