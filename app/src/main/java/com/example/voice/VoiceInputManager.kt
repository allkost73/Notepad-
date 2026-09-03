package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class VoiceState {
    object Idle : VoiceState()
    object Initializing : VoiceState()
    object Listening : VoiceState()
    data class Success(val recognizedText: String) : VoiceState()
    data class Error(val message: String, val canFallbackToSystem: Boolean = true) : VoiceState()
}

class VoiceInputManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _liveRmsDb = MutableStateFlow(0f)
    val liveRmsDb: StateFlow<Float> = _liveRmsDb.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    fun isSpeechAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * Creates an Intent for standard system-level speech dialog (Google Voice Search / Speech Dialog).
     * This provides 100% reliable Russian recognition with automatic punctuation on Android devices.
     */
    fun createSystemSpeechIntent(promptText: String = "Скажите задачу, заметку или команду..."): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru-RU")
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("ru-RU", "ru"))
            putExtra(RecognizerIntent.EXTRA_PROMPT, promptText)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }
    }

    fun startListening() {
        stopListening()

        if (!isSpeechAvailable()) {
            _voiceState.value = VoiceState.Error(
                message = "Служба распознавания речи недоступна в фоне. Воспользуйтесь системным микрофоном Google ниже.",
                canFallbackToSystem = true
            )
            return
        }

        try {
            _voiceState.value = VoiceState.Initializing
            _partialText.value = ""
            _liveRmsDb.value = 0f

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru-RU")
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("ru-RU", "ru"))
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            }

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _voiceState.value = VoiceState.Listening
                    }

                    override fun onBeginningOfSpeech() {
                        _voiceState.value = VoiceState.Listening
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        _liveRmsDb.value = rmsdB
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        // Wait for final results
                    }

                    override fun onError(error: Int) {
                        val (message, canFallback) = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Ошибка записи звука с микрофона" to true
                            SpeechRecognizer.ERROR_CLIENT -> "Служба распознавания занята или перегружена" to true
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Предоставьте разрешение на запись звука" to false
                            SpeechRecognizer.ERROR_NETWORK -> "Ошибка интернет-соединения при распознавании" to true
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Таймаут подключения к серверу распознавания" to true
                            SpeechRecognizer.ERROR_NO_MATCH -> "Речь не распознана. Попробуйте еще раз или используйте системный микрофон Google" to true
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Служба распознавания занята, повторите попытку" to true
                            SpeechRecognizer.ERROR_SERVER -> "Ошибка сервера распознавания речи" to true
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Не услышали речь. Попробуйте сказать громче" to true
                            else -> "Ошибка распознавания ($error)" to true
                        }
                        _voiceState.value = VoiceState.Error(message, canFallback)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spokenText = matches?.firstOrNull()?.trim() ?: _partialText.value
                        if (spokenText.isNotBlank()) {
                            _voiceState.value = VoiceState.Success(spokenText)
                        } else {
                            _voiceState.value = VoiceState.Error("Речь не распознана, скажите фразу еще раз")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spokenText = matches?.firstOrNull() ?: ""
                        if (spokenText.isNotBlank()) {
                            _partialText.value = spokenText
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                startListening(intent)
            }
        } catch (e: Exception) {
            _voiceState.value = VoiceState.Error("Не удалось запустить распознавание: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // Ignored
        } finally {
            speechRecognizer = null
        }
    }

    fun setResultDirectly(text: String) {
        if (text.isNotBlank()) {
            _voiceState.value = VoiceState.Success(text.trim())
        }
    }

    fun reset() {
        stopListening()
        _voiceState.value = VoiceState.Idle
        _partialText.value = ""
        _liveRmsDb.value = 0f
    }
}
