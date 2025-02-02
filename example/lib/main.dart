import 'package:flutter/material.dart';
import 'package:flutter_vox/flutter_vox.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:speech_to_text/speech_to_text.dart';

void main() {
  runApp(const FlutterVoxExampleApp());
}

class FlutterVoxExampleApp extends StatelessWidget {
  const FlutterVoxExampleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'FlutterVox Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
        useMaterial3: true,
      ),
      home: const HomePage(),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> implements VoiceAssistantCallback {
  static const wakeWord = 'KIKO';

  final FlutterVox _voiceAssistant = FlutterVox();
  final List<CommandLog> _commandLogs = [];
  bool _isInitialized = false;
  bool _isListening = false;
  bool _isBackgroundMode = false;
  String _currentStatus = 'Initializing...';
  List<PermissionStatus> _permissionStatus = [];
  bool _isPermissionRequestInProgress = false;
  bool _isListeningForCommand = false;

  final SpeechToText _speech = SpeechToText();
  bool _speechEnabled = false;

  @override
  void initState() {
    super.initState();
    _initialize();
  }

  Future<void> _initialize() async {
    await _requestPermissions();
    await _initializeVoiceAssistant();
    await _initializeSpeechToText();
  }

  Future<void> _initializeSpeechToText() async {
    _speechEnabled = await _speech.initialize(
      onError: (error) => _handleError('Speech recognition error: $error'),
      onStatus: (status) => print('Speech recognition status: $status'),
    );
  }

  Future<void> _requestPermissions() async {
    if (_isPermissionRequestInProgress) return;
    setState(() {
      _isPermissionRequestInProgress = true;
    });

    final permissions = await [
      Permission.microphone,
      Permission.speech,
      Permission.notification,
      Permission.storage
    ].request();

    final statuses = permissions.values.toList();

    setState(() {
      _permissionStatus = statuses;
      _isPermissionRequestInProgress = false;
    });
  }

  Future<void> _initializeVoiceAssistant() async {
    try {
      await _voiceAssistant.initialize(
        wakeWord: wakeWord,
        config: {
          'language': 'en-US',
          'continuousListening': true,
          'timeout': 5000,
        },
        callback: this,
      );

      setState(() {
        _isInitialized = true;
        _currentStatus = 'Ready';
      });
    } catch (e) {
      setState(() {
        _currentStatus = 'Initialization failed: $e';
      });
    }
  }

  Future<void> _startListeningForCommand() async {
    if (!_speechEnabled) return;

    await _voiceAssistant.stopListening(); // Stop wake word detection

    setState(() {
      _isListeningForCommand = true;
      _currentStatus = 'Listening for command...';
    });

    await _speech.listen(
      onResult: (result) {
        if (result.finalResult) {
          _handleCommand(result.recognizedWords);
        }
      },
      pauseFor: const Duration(seconds: 3),
    );
  }

  void _handleCommand(String command) async {
    setState(() {
      _commandLogs.insert(
        0,
        CommandLog(
          type: LogType.command,
          message: 'Command received: $command',
          timestamp: DateTime.now(),
        ),
      );
    });

    // Interpret and execute the command here
    await _interpretCommand(command);

    // Stop speech recognition and resume wake word detection
    await _speech.stop();
    setState(() {
      _isListeningForCommand = false;
      _currentStatus = 'Ready for wake word';
    });

    await _voiceAssistant.startListening(); // Resume wake word detection
  }

  Future<void> _interpretCommand(String command) async {
    // Add your command interpretation logic here
    String response = 'Executing command: $command';

    // Example command interpretation
    command = command.toLowerCase();
    if (command.contains('turn on') || command.contains('enable')) {
      response = 'Turning on requested feature';
    } else if (command.contains('turn off') || command.contains('disable')) {
      response = 'Turning off requested feature';
    }

    setState(() {
      _commandLogs.insert(
        0,
        CommandLog(
          type: LogType.execution,
          message: response,
          timestamp: DateTime.now(),
        ),
      );
    });
  }

  void _handleError(String error) {
    setState(() {
      _commandLogs.insert(
        0,
        CommandLog(
          type: LogType.error,
          message: error,
          timestamp: DateTime.now(),
        ),
      );
      _currentStatus = 'Error occurred';
    });
  }

  Future<void> _toggleListening() async {
    if (!_isInitialized) return;

    try {
      if (_isListening) {
        await _voiceAssistant.stopListening();
        if (_isListeningForCommand) {
          await _speech.stop();
        }
      } else {
        await _voiceAssistant.startListening();
      }
      setState(() {
        _isListening = !_isListening;
        _isListeningForCommand = false;
        _currentStatus =
            _isListening ? 'Listening for wake word...' : 'Stopped';
      });
    } catch (e) {
      setState(() {
        _currentStatus = 'Error: $e';
      });
    }
  }

  Future<void> _toggleBackgroundMode() async {
    try {
      await _voiceAssistant.setBackgroundMode(!_isBackgroundMode);
      setState(() {
        _isBackgroundMode = !_isBackgroundMode;
        _currentStatus = _isBackgroundMode
            ? 'Background mode enabled'
            : 'Background mode disabled';
      });
    } catch (e) {
      setState(() {
        _currentStatus = 'Background mode error: $e';
      });
    }
  }

  @override
  void dispose() {
    _voiceAssistant.dispose();
    _speech.stop();
    super.dispose();
  }

  // VoiceAssistantCallback implementations
  @override
  void onWakeWordDetected() {
    setState(() {
      _commandLogs.insert(
        0,
        CommandLog(
          type: LogType.wake,
          message: 'Wake word detected',
          timestamp: DateTime.now(),
        ),
      );
    });
    _startListeningForCommand();
  }

  @override
  void onError(String error) {
    _handleError(error);
  }

  // UI Widgets remain the same as in the original code
  @override
  Widget build(BuildContext context) {
    if (_permissionStatus.any((status) => !status.isGranted)) {
      return Scaffold(
        appBar: AppBar(
          title: const Text('FlutterVox Demo'),
        ),
        body: _isPermissionRequestInProgress
            ? const Center(child: CircularProgressIndicator())
            : Center(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Text('Permissions required'),
                    ElevatedButton(
                      onPressed: _requestPermissions,
                      child: const Text('Request Permissions'),
                    ),
                  ],
                ),
              ),
      );
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('FlutterVox Demo'),
        actions: [
          IconButton(
            icon: Icon(_isBackgroundMode ? Icons.archive : Icons.unarchive),
            onPressed: _isInitialized ? _toggleBackgroundMode : null,
            tooltip: 'Toggle background mode',
          ),
        ],
      ),
      body: Column(
        children: [
          _buildStatusBar(),
          Expanded(child: _buildCommandLog()),
          _buildControlPanel(),
        ],
      ),
    );
  }

  Widget _buildStatusBar() {
    return Container(
      padding: const EdgeInsets.all(16),
      color: Theme.of(context).colorScheme.primaryContainer,
      child: Row(
        children: [
          Icon(
            _getStatusIcon(),
            color: Theme.of(context).colorScheme.onPrimaryContainer,
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Text(
              _currentStatus,
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    color: Theme.of(context).colorScheme.onPrimaryContainer,
                  ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCommandLog() {
    if (_commandLogs.isEmpty) {
      return const Center(
        child: Text('No commands yet. Try saying "$wakeWord"!'),
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.all(8),
      itemCount: _commandLogs.length,
      itemBuilder: (context, index) {
        final log = _commandLogs[index];
        return Card(
          margin: const EdgeInsets.symmetric(vertical: 4),
          child: ListTile(
            leading: Icon(_getLogTypeIcon(log.type)),
            title: Text(log.message),
            subtitle: Text(
              _formatTimestamp(log.timestamp),
              style: Theme.of(context).textTheme.bodySmall,
            ),
            tileColor: _getLogTypeColor(log.type),
          ),
        );
      },
    );
  }

  Widget _buildControlPanel() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surface,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.1),
            blurRadius: 4,
            offset: const Offset(0, -2),
          ),
        ],
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: [
          Expanded(
            child: ElevatedButton.icon(
              onPressed: _isInitialized ? _toggleListening : null,
              icon: Icon(_isListening ? Icons.mic_off : Icons.mic),
              label: Text(_isListening ? 'Stop Listening' : 'Start Listening'),
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.all(16),
              ),
            ),
          ),
        ],
      ),
    );
  }

  IconData _getStatusIcon() {
    if (!_isInitialized) return Icons.error_outline;
    if (_isListeningForCommand) return Icons.record_voice_over;
    if (_isListening) return Icons.mic;
    return Icons.mic_none;
  }

  IconData _getLogTypeIcon(LogType type) {
    switch (type) {
      case LogType.wake:
        return Icons.notifications_active;
      case LogType.command:
        return Icons.record_voice_over;
      case LogType.execution:
        return Icons.done;
      case LogType.error:
        return Icons.error_outline;
    }
  }

  Color _getLogTypeColor(LogType type) {
    switch (type) {
      case LogType.wake:
        return Colors.blue.withOpacity(0.1);
      case LogType.command:
        return Colors.green.withOpacity(0.1);
      case LogType.execution:
        return Colors.purple.withOpacity(0.1);
      case LogType.error:
        return Colors.red.withOpacity(0.1);
    }
  }

  String _formatTimestamp(DateTime timestamp) {
    return '${timestamp.hour.toString().padLeft(2, '0')}:'
        '${timestamp.minute.toString().padLeft(2, '0')}:'
        '${timestamp.second.toString().padLeft(2, '0')}';
  }
}

enum LogType {
  wake,
  command,
  execution,
  error,
}

class CommandLog {
  final LogType type;
  final String message;
  final DateTime timestamp;

  CommandLog({
    required this.type,
    required this.message,
    required this.timestamp,
  });
}
