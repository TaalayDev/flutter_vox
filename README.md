# FlutterVox

A Flutter plugin that implements wake word functionality for Android and iOS devices.

> ⚠️ **Development Status**: This plugin is currently in early development and is not ready for production use. Development may be discontinued if better alternatives become available. Consider using established solutions like [alan_voice](https://pub.dev/packages/alan_voice) or [picovoice_flutter](https://pub.dev/packages/picovoice_flutter) for production applications.

## System Requirements

- Android API Level 21+ (Android 5.0 or higher)
- iOS 12.0 or higher
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

### iOS Setup

1. Add the following permissions to your Info.plist (`ios/Runner/Info.plist`):

```xml
<key>NSMicrophoneUsageDescription</key>
<string>Microphone access is required for voice assistant functionality.</string>
<key>NSSpeechRecognitionUsageDescription</key>
<string>Speech recognition is required for voice assistant functionality.</string>
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

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Thanks to CMU Sphinx for providing offline speech recognition capabilities
- Flutter team for the plugin development infrastructure