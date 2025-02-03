//
//  VoiceAssistantService.swift
//  flutter_vox
//
//  Created by Taalay on 3/2/25.
//

import Foundation
import Speech
import AVFoundation

class VoiceAssistantManager: NSObject, SFSpeechRecognizerDelegate {
    private let speechRecognizer: SFSpeechRecognizer?
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
    private var recognitionTask: SFSpeechRecognitionTask?
    private let audioEngine = AVAudioEngine()
    
    private var wakeWord: String = "KIKO"
    private(set) var isListening: Bool = false
    private var isBackgroundMode: Bool = false
    
    var onWakeWordDetected: (() -> Void)?
    var onError: ((Error) -> Void)?
    
    override init() {
        self.speechRecognizer = SFSpeechRecognizer(locale: Locale(identifier: "en-US"))
        super.init()
        self.speechRecognizer?.delegate = self
    }
    
    func initialize(wakeWord: String, config: [String: Any], completion: @escaping (Error?) -> Void) {
        self.wakeWord = wakeWord.lowercased()
        
        SFSpeechRecognizer.requestAuthorization { [weak self] status in
            guard let self = self else { return }
            
            DispatchQueue.main.async {
                switch status {
                case .authorized:
                    completion(nil)
                case .denied:
                    completion(NSError(domain: "FlutterVox",
                                    code: 1,
                                    userInfo: [NSLocalizedDescriptionKey: "Speech recognition access denied"]))
                case .restricted:
                    completion(NSError(domain: "FlutterVox",
                                    code: 2,
                                    userInfo: [NSLocalizedDescriptionKey: "Speech recognition restricted on device"]))
                case .notDetermined:
                    completion(NSError(domain: "FlutterVox",
                                    code: 3,
                                    userInfo: [NSLocalizedDescriptionKey: "Speech recognition not determined"]))
                @unknown default:
                    completion(NSError(domain: "FlutterVox",
                                    code: 4,
                                    userInfo: [NSLocalizedDescriptionKey: "Unknown speech recognition error"]))
                }
            }
        }
    }
    
    func startListening(completion: @escaping (Error?) -> Void) {
        if isListening {
            completion(nil)
            return
        }
        
        do {
            try startAudioSession()
            try startRecognition(completion: completion)
            isListening = true
        } catch {
            completion(error)
        }
    }
    
    func stopListening() {
        audioEngine.stop()
        recognitionRequest?.endAudio()
        recognitionTask?.cancel()
        
        recognitionRequest = nil
        recognitionTask = nil
        isListening = false
        
        if !isBackgroundMode {
            try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        }
    }
    
    func setBackgroundMode(enabled: Bool, completion: @escaping (Error?) -> Void) {
        isBackgroundMode = enabled
        
        if enabled {
            do {
                try AVAudioSession.sharedInstance().setCategory(.playAndRecord,
                                                              mode: .default,
                                                              options: [.allowBluetooth, .mixWithOthers])
                completion(nil)
            } catch {
                completion(error)
            }
        } else {
            stopListening()
            completion(nil)
        }
    }
    
    private func startAudioSession() throws {
        try AVAudioSession.sharedInstance().setCategory(.record, mode: .measurement, options: .duckOthers)
        try AVAudioSession.sharedInstance().setActive(true, options: .notifyOthersOnDeactivation)
    }
    
    private func startRecognition(completion: @escaping (Error?) -> Void) throws {
        recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
        
        guard let recognitionRequest = recognitionRequest else {
            throw NSError(domain: "FlutterVox",
                         code: 5,
                         userInfo: [NSLocalizedDescriptionKey: "Unable to create recognition request"])
        }
        
        recognitionRequest.shouldReportPartialResults = true
        
        recognitionTask = speechRecognizer?.recognitionTask(with: recognitionRequest) { [weak self] result, error in
            guard let self = self else { return }
            
            if let error = error {
                self.onError?(error)
                return
            }
            
            if let result = result {
                let transcription = result.bestTranscription.formattedString.lowercased()
                if transcription.contains(self.wakeWord) {
                    self.onWakeWordDetected?()
                }
            }
            
            if result?.isFinal == true {
                self.restartRecognition()
            }
        }
        
        let inputNode = audioEngine.inputNode
        let recordingFormat = inputNode.outputFormat(forBus: 0)
        
        inputNode.installTap(onBus: 0,
                           bufferSize: 1024,
                           format: recordingFormat) { [weak self] buffer, when in
            self?.recognitionRequest?.append(buffer)
        }
        
        audioEngine.prepare()
        try audioEngine.start()
        
        completion(nil)
    }
    
    private func restartRecognition() {
        stopListening()
        try? startListening { _ in }
    }
}
