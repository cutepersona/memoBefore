package com.peng.power.memo.recognizer

enum class NaverRecognizerCallbackType {
    ClientInactive,
    ClientReady,
    AudioRecording,
    PartialResult,
    EndPointDetected,
    FinalResult,
    RecognitionError,
    EndPointDetectTypeSelected
}