import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'flutter_vox_platform_interface.dart';
import 'voice_assistant_exception.dart';

/// An implementation of [FluttervoxPlatform] that uses method channels.
class MethodChannelFlutterVox extends FlutterVoxPlatform {
  final MethodChannel _channel = const MethodChannel('flutter_vox');

  @override
  Future<void> initialize({
    String? wakePath,
    String? wakeWord,
    bool? enableVoiceAssistant = true,
    Map<String, dynamic>? config,
  }) async {
    try {
      await _channel.invokeMethod('initialize', {
        'wakePath': wakePath,
        'wakeWord': wakeWord,
        'voiceAssistantEnabled': enableVoiceAssistant,
        'config': config,
      });
    } on PlatformException catch (e) {
      throw VoiceAssistantException(
        'Failed to initialize voice assistant: ${e.message}',
        code: e.code,
      );
    }
  }

  @override
  Future<void> startListening() async {
    try {
      await _channel.invokeMethod('startListening');
    } on PlatformException catch (e) {
      throw VoiceAssistantException(
        'Failed to start listening: ${e.message}',
        code: e.code,
      );
    }
  }

  @override
  Future<void> stopListening() async {
    try {
      await _channel.invokeMethod('stopListening');
    } on PlatformException catch (e) {
      throw VoiceAssistantException(
        'Failed to stop listening: ${e.message}',
        code: e.code,
      );
    }
  }

  @override
  Future<bool> isListening() async {
    try {
      final result = await _channel.invokeMethod<bool>('isListening');
      return result ?? false;
    } on PlatformException catch (e) {
      throw VoiceAssistantException(
        'Failed to get listening status: ${e.message}',
        code: e.code,
      );
    }
  }

  @override
  Future<void> setLauncherMode(bool enabled) async {
    try {
      await _channel.invokeMethod('setLauncherMode', {
        'enabled': enabled,
      });
    } on PlatformException catch (e) {
      throw VoiceAssistantException(
        'Failed to set launcher mode: ${e.message}',
        code: e.code,
      );
    }
  }

  @override
  Future<void> setBackgroundMode(bool enabled) async {
    try {
      await _channel.invokeMethod('setBackgroundMode', {
        'enabled': enabled,
      });
    } on PlatformException catch (e) {
      throw VoiceAssistantException(
        'Failed to set background mode: ${e.message}',
        code: e.code,
      );
    }
  }
}

class MethodChannelFlutterVoxService extends MethodChannelFlutterVox {
  final _serviceChannel = MethodChannel('flutter_vox.service');

  @override
  MethodChannel get _channel => _serviceChannel;
}
