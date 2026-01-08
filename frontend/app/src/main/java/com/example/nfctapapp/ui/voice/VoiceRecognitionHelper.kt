package com.example.nfctapapp.ui.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.*

/**
 * 음성 인식 헬퍼 클래스
 *
 * Android SpeechRecognizer API를 간단하게 사용하기 위한 래퍼
 */
class VoiceRecognitionHelper(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onListeningStart: () -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("음성 인식을 사용할 수 없습니다")
            return
        }

        // 기존 recognizer 정리
        stopListening()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN.toString())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "말씀해주세요")
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onListeningStart()
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "오디오 에러"
                    SpeechRecognizer.ERROR_CLIENT -> "클라이언트 에러"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "권한이 없습니다"
                    SpeechRecognizer.ERROR_NETWORK -> "네트워크 에러"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트워크 타임아웃"
                    SpeechRecognizer.ERROR_NO_MATCH -> "인식된 음성이 없습니다"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "음성 인식기가 바쁩니다"
                    SpeechRecognizer.ERROR_SERVER -> "서버 에러"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "음성 입력 시간 초과"
                    else -> "알 수 없는 에러"
                }
                onError(errorMessage)
                cleanup()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onResult(matches[0])
                } else {
                    onError("음성을 인식하지 못했습니다")
                }
                cleanup()
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        cleanup()
    }

    private fun cleanup() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
