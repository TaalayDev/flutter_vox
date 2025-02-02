# FlutterVox

A Flutter plugin that implements voice assistant functionality with background listening and voice command processing capabilities for Android devices. FlutterVox provides both launcher and background service modes, making it suitable for a wide range of voice-controlled applications.

> ⚠️ **Development Status**: This plugin is currently in early development and is not ready for production use. Development may be discontinued if better alternatives become available. Consider using established solutions like [alan_voice](https://pub.dev/packages/alan_voice) or [picovoice_flutter](https://pub.dev/packages/picovoice_flutter) for production applications.

## Features

- 🎤 Background audio listening with minimal battery impact
- 🗣️ Offline wake word detection and customization
- 📱 Dual-mode operation (launcher or background service)
- 🎯 Custom voice command support with contextual processing
- 🔋 Battery-efficient background operation (<5% additional drain)
- 🔒 Built-in security features and permission handling
- 📱 Full Android lifecycle management
- 🔄 Automatic service restoration after device reboot

## System Requirements

- Android API Level 21+ (Android 5.0 or higher)
- Flutter 3.3.0 or higher
- Dart SDK 3.6.0 or higher
- Kotlin 1.8.22 or higher

## Installation

Add FlutterVox to your `pubspec.yaml`:

```yaml
dependencies:
  flutter_vox: 
    git:
      url: [REPOSITORY_URL]
```

### Android Setup

1. Add the following permissions to your Android Manifest (`android/app/src/main/AndroidManifest.xml`):

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
    android:maxSdkVersion="32" />

<queries>
    <intent>
        <action android:name="android.speech.RecognitionService" />
    </intent>
    <intent>
        <action android:name="android.service.voice.VoiceInteractionService" />
    </intent>
</queries>
```

2. Request runtime permissions in your app:

```dart
// Request required permissions
final permissions = await [
  Permission.microphone,
  Permission.speech,
  Permission.notification,
  Permission.storage
].request();

if (permissions.values.every((status) => status.isGranted)) {
  // Initialize voice assistant
}
```

## Basic Usage

### Initialization

```dart
import 'package:flutter_vox/flutter_vox.dart';

final flutterVox = FlutterVox();

// Initialize with basic configuration
await flutterVox.initialize(
  wakeWord: 'hey assistant',
  config: {
    'language': 'en-US',
    'continuousListening': true,
  },
  callback: this, // Implement VoiceAssistantCallback
);
```

### Control Methods

```dart
// Start voice assistant
await flutterVox.startListening();

// Check if currently listening
bool isActive = await flutterVox.isListening();

// Stop voice assistant
await flutterVox.stopListening();

// Clean up resources
flutterVox.dispose();
```

### Callback Implementation

```dart
class MyVoiceHandler implements VoiceAssistantCallback {
  @override
  void onWakeWordDetected() {
    print('Wake word detected!');
    // Start command processing
  }

  @override
  void onError(String error) {
    print('Error occurred: $error');
    // Handle error
  }
}
```

## Advanced Features

### Background Service Mode

```dart
// Enable background service mode
await flutterVox.setBackgroundMode(true);

// The service will continue running even when the app is in background
// A notification will be shown to indicate the service status
```

### Launcher Mode

```dart
// Enable launcher mode
await flutterVox.setLauncherMode(true);

// The app can now be set as the default launcher
// Voice commands will be available from the home screen
```

## Performance Considerations

FlutterVox is designed with performance and battery life in mind:

- Wake word detection runs locally for minimal latency
- Background service uses less than 50MB of memory
- CPU usage in idle state stays under 2%
- Battery impact is optimized to less than 5% additional drain
- Automatic service management to prevent resource leaks

## Error Handling

FlutterVox provides dedicated error handling through exceptions:

```dart
try {
  await flutterVox.startListening();
} on VoiceAssistantException catch (e) {
  print('Error code: ${e.code}');
  print('Error message: ${e.message}');
}
```

## Common Issues and Solutions

### Service Not Starting

If the voice assistant service fails to start:

1. Verify all required permissions are granted
2. Check Android Manifest configuration
3. Ensure device meets minimum API level requirement
4. Verify background service restrictions on the device

### High Battery Usage

If experiencing higher than expected battery drain:

1. Check if continuous listening is necessary
2. Verify wake word sensitivity settings
3. Monitor for wake word false positives
4. Ensure proper cleanup when stopping the service

### Memory Management

To maintain optimal memory usage:

1. Call `dispose()` when the voice assistant is no longer needed
2. Monitor memory usage through Android Studio
3. Implement proper lifecycle management
4. Clear resources in background service when stopped

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Thanks to CMU Sphinx for providing offline speech recognition capabilities
- Flutter team for the plugin development infrastructure