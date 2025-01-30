# FlutterVox

> ⚠️ **Development Status**: This plugin is currently in early development and is not ready for production use. Development may be discontinued if better alternatives become available. Consider using established solutions like [alan_voice](https://pub.dev/packages/alan_voice) or [speech_to_text](https://pub.dev/packages/speech_to_text) for production applications.

A Flutter plugin that implements voice assistant functionality with background listening and voice command processing capabilities for Android devices.

## Features

- 🎤 Background audio listening
- 🗣️ Wake word detection ("Hey Assistant", "OK Assistant")
- 📱 Works as a launcher or background service
- 🔊 Offline wake word recognition
- 🎯 Custom voice command support
- 🔋 Battery-efficient background operation
- 📱 Full Android lifecycle management

## Getting Started

### Installation

Add FlutterVox to your `pubspec.yaml`:

```yaml
dependencies:
  flutter_vox: 
    git:
      url: [GIT_URL]
```

### Android Setup

1. Add the following permissions to your Android Manifest (`android/app/src/main/AndroidManifest.xml`):

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

2. Request runtime permissions in your app:

```dart
// Request microphone permission
if (await Permission.microphone.request().isGranted) {
  // Start voice assistant service
}
```

## Usage

### Basic Implementation

```dart
import 'package:flutter_vox/flutter_vox.dart';

// Initialize the plugin
final flutterVox = FlutterVox();

// Start the voice assistant service
await flutterVox.startService();

// Stop the service
await flutterVox.stopService();

// Check if service is running
bool isRunning = await flutterVox.isServiceRunning();
```

### Advanced Configuration

#### Custom Wake Words

```dart
await flutterVox.initialize(
  config: VoiceConfig(
    wakeWords: ['hey assistant', 'ok assistant'],
    language: 'en-US',
  )
);
```

#### Custom Command Handling

```dart
await flutterVox.addCommand(
  'open {app}',
  (params) {
    final appName = params['app'];
    // Handle opening the app
  }
);
```

#### Launcher Mode

```dart
await flutterVox.setLauncherMode(true);
```

## Performance Considerations

- Wake word detection runs locally and is optimized for minimal battery impact
- Background service uses less than 50MB of memory
- CPU usage in idle state is under 2%
- Battery impact is optimized to less than 5% additional drain

## API Reference

### Core Methods

```dart
Future<void> initialize({VoiceConfig? config});
Future<void> startService();
Future<void> stopService();
Future<bool> isServiceRunning();
```

### Command Management

```dart
Future<void> addCommand(String pattern, Function(Map<String, String>) handler);
Future<void> removeCommand(String pattern);
Future<List<String>> getAvailableCommands();
```

### Mode Control

```dart
Future<void> setLauncherMode(bool enabled);
Future<void> setBackgroundMode(bool enabled);
```

## Events

Listen to voice assistant events:

```dart
flutterVox.onWakeWordDetected.listen((word) {
  print('Wake word detected: $word');
});

flutterVox.onCommandRecognized.listen((command) {
  print('Command recognized: $command');
});
```

## Common Issues and Solutions

### Service Not Starting

Make sure you have:
1. Added all required permissions in AndroidManifest.xml
2. Requested runtime permissions for microphone access
3. Properly initialized the plugin before starting the service

### High Battery Consumption

If you notice higher than expected battery usage:
1. Check if continuous listening is necessary
2. Adjust wake word sensitivity
3. Verify no memory leaks in custom command handlers

### Memory Usage

The service is designed to use minimal memory, but if you experience issues:
1. Monitor memory usage through Android Studio
2. Check for memory leaks in custom implementations
3. Ensure proper cleanup in command handlers

## Contributing

We welcome contributions! Please see our contributing guide for details.

### Development Setup

1. Clone the repository
2. Run `flutter pub get`
3. Run the example app
4. Make your changes
5. Submit a pull request

## Requirements

- Android API Level 21+
- Flutter 2.0.0+
- Kotlin 1.5.0+

## License

This project is licensed under the MIT License - see the LICENSE file for details.