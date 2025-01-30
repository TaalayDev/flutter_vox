class VoiceAssistantException implements Exception {
  final String message;
  final String code;

  VoiceAssistantException(this.message, {this.code = 'unknown'});

  @override
  String toString() => 'VoiceAssistantException($code): $message';
}
