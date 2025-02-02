# Changelog

All notable changes to FlutterVox will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.0.1] - 2025-02-02

### Added
- Initial release with core voice assistant functionality
- Background audio listening capability
- Offline wake word detection using CMU Sphinx
- Two operation modes:
  - Background service mode
  - Launcher mode (experimental)
- Basic voice command framework
- Android service lifecycle management
- Permission handling system
- Error handling and logging
- Battery optimization features
- Auto-restart capability after device reboot
- Basic example application

### Dependencies
- Minimum Android API level: 21
- Flutter SDK: ^3.6.0
- Kotlin version: 1.8.22
- CMU Sphinx for wake word detection
- AndroidX core libraries

### Known Issues
- Wake word detection may have false positives in noisy environments
- Background service may be terminated on some Android devices with aggressive battery optimization
- Launcher mode is experimental and may have stability issues
- Limited to Android platform only

### Security Notes
- All voice processing is done locally on device
- No data is transmitted to external servers
- Proper runtime permissions are required for functionality

### Documentation
- Basic usage instructions in README.md
- API documentation with examples
- Integration guide for Android
- Troubleshooting guide

[0.0.1]: https://github.com/username/fluttervox/releases/tag/v0.0.1