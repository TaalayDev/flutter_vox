import Flutter
import UIKit
import Speech

public class FlutterVoxPlugin: NSObject, FlutterPlugin {
    private var channel: FlutterMethodChannel
    private let voiceAssistantManager: VoiceAssistantManager
    
    init(channel: FlutterMethodChannel) {
        self.channel = channel
        self.voiceAssistantManager = VoiceAssistantManager()
        super.init()
        
        voiceAssistantManager.onWakeWordDetected = { [weak self] in
            self?.channel.invokeMethod("onWakeWordDetected", arguments: nil)
        }
        
        voiceAssistantManager.onError = { [weak self] error in
            self?.channel.invokeMethod("onError", arguments: error.localizedDescription)
        }
    }
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "flutter_vox", binaryMessenger: registrar.messenger())
        let instance = FlutterVoxPlugin(channel: channel)
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
        case "initialize":
            handleInitialize(call, result: result)
        case "startListening":
            handleStartListening(result)
        case "stopListening":
            handleStopListening(result)
        case "isListening":
            handleIsListening(result)
        case "setBackgroundMode":
            handleSetBackgroundMode(call, result: result)
        default:
            result(FlutterMethodNotImplemented)
        }
    }
    
    private func handleInitialize(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let args = call.arguments as? [String: Any] else {
            result(FlutterError(code: "invalid_arguments",
                              message: "Invalid arguments for initialize",
                              details: nil))
            return
        }
        
        let wakeWord = args["wakeWord"] as? String ?? "hey assistant"
        let config = args["config"] as? [String: Any] ?? [:]
        
        voiceAssistantManager.initialize(wakeWord: wakeWord, config: config) { error in
            if let error = error {
                result(FlutterError(code: "initialization_failed",
                                  message: error.localizedDescription,
                                  details: nil))
            } else {
                result(nil)
            }
        }
    }
    
    private func handleStartListening(_ result: @escaping FlutterResult) {
        voiceAssistantManager.startListening { error in
            if let error = error {
                result(FlutterError(code: "start_listening_failed",
                                  message: error.localizedDescription,
                                  details: nil))
            } else {
                result(nil)
            }
        }
    }
    
    private func handleStopListening(_ result: @escaping FlutterResult) {
        voiceAssistantManager.stopListening()
        result(nil)
    }
    
    private func handleIsListening(_ result: @escaping FlutterResult) {
        result(voiceAssistantManager.isListening)
    }
    
    private func handleSetBackgroundMode(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let args = call.arguments as? [String: Any],
              let enabled = args["enabled"] as? Bool else {
            result(FlutterError(code: "invalid_arguments",
                              message: "Invalid arguments for setBackgroundMode",
                              details: nil))
            return
        }
        
        voiceAssistantManager.setBackgroundMode(enabled: enabled) { error in
            if let error = error {
                result(FlutterError(code: "background_mode_failed",
                                  message: error.localizedDescription,
                                  details: nil))
            } else {
                result(nil)
            }
        }
    }
}
