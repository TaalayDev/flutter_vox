import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'package:flutter/services.dart';

import 'flutter_vox_method_channel.dart';

abstract class FlutterVoxPlatform extends PlatformInterface {
  FlutterVoxPlatform() : super(token: _token);

  static final Object _token = Object();
  static FlutterVoxPlatform _instance = MethodChannelFlutterVox();

  static FlutterVoxPlatform get instance => _instance;

  static set instance(FlutterVoxPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<void> initialize({
    String? wakePath,
    String? wakeWord,
    bool? enableVoiceAssistant = true,
    Map<String, dynamic>? config,
  }) {
    throw UnimplementedError('initialize() has not been implemented.');
  }

  Future<void> startListening() {
    throw UnimplementedError('startListening() has not been implemented.');
  }

  Future<void> stopListening() {
    throw UnimplementedError('stopListening() has not been implemented.');
  }

  Future<bool> isListening() {
    throw UnimplementedError('isListening() has not been implemented.');
  }

  Future<void> setLauncherMode(bool enabled) {
    throw UnimplementedError('setLauncherMode() has not been implemented.');
  }

  Future<void> setBackgroundMode(bool enabled) {
    throw UnimplementedError('setBackgroundMode() has not been implemented.');
  }
}
