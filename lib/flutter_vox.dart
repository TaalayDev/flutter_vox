import 'package:flutter/services.dart';

import 'flutter_vox_platform_interface.dart';
import 'voice_assistant_callback.dart';

export 'voice_assistant_callback.dart';

class FlutterVox {
  static final FlutterVox _instance = FlutterVox._internal();
  factory FlutterVox() => _instance;
  FlutterVox._internal();

  final MethodChannel _channel = const MethodChannel('flutter_vox');
  VoiceAssistantCallback? _callback;

  Future<void> initialize({
    String? wakePath,
    String? wakeWord,
    bool? enableVoiceAssistant,
    Map<String, dynamic>? config,
    VoiceAssistantCallback? callback,
  }) async {
    _callback = callback;
    _setupMethodCallHandler();
    await FlutterVoxPlatform.instance.initialize(
      wakePath: wakePath,
      wakeWord: wakeWord,
      enableVoiceAssistant: enableVoiceAssistant,
      config: config,
    );
  }

  void _setupMethodCallHandler() {
    _channel.setMethodCallHandler((call) async {
      if (_callback == null) return;

      switch (call.method) {
        case 'onWakeWordDetected':
          _callback?.onWakeWordDetected();
          break;
        case 'onError':
          _callback?.onError(call.arguments as String);
          break;
      }
    });
  }

  Future<void> startListening() => FlutterVoxPlatform.instance.startListening();
  Future<void> stopListening() => FlutterVoxPlatform.instance.stopListening();
  Future<bool> isListening() => FlutterVoxPlatform.instance.isListening();

  Future<void> setLauncherMode(bool enabled) =>
      FlutterVoxPlatform.instance.setLauncherMode(enabled);

  Future<void> setBackgroundMode(bool enabled) =>
      FlutterVoxPlatform.instance.setBackgroundMode(enabled);

  void dispose() {
    _callback = null;
    _channel.setMethodCallHandler(null);
  }
}
