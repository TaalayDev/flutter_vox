abstract class VoiceAssistantCallback {
  void onWakeWordDetected();
  void onCommandRecognized(String command);
  void onCommandExecuted(String command, Map<String, String> parameters);
  void onError(String error);
}
