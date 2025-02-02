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
    Map<String, dynamic>? config,
  }) async {
    try {
      await _channel.invokeMethod('initialize', {
        'wakePath': wakePath,
        'wakeWord': wakeWord,
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
  Future<void> addCommand(String command, List<String> parameters) async {
    try {
      await _channel.invokeMethod('addCommand', {
        'command': command,
        'parameters': parameters,
      });
    } on PlatformException catch (e) {
      throw VoiceAssistantException(
        'Failed to add command: ${e.message}',
        code: e.code,
      );
    }
  }

  @override
  Future<void> removeCommand(String command) async {
    try {
      await _channel.invokeMethod('removeCommand', {
        'command': command,
      });
    } on PlatformException catch (e) {
      throw VoiceAssistantException(
        'Failed to remove command: ${e.message}',
        code: e.code,
      );
    }
  }

  @override
  Future<List<String>> getAvailableCommands() async {
    try {
      final result =
          await _channel.invokeMethod<List<dynamic>>('getAvailableCommands');
      return result?.cast<String>() ?? [];
    } on PlatformException catch (e) {
      throw VoiceAssistantException(
        'Failed to get available commands: ${e.message}',
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
