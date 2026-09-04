package com.example.voice

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
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
        val standardCheck = SpeechRecognizer.isRecognitionAvailable(context)
        if (standardCheck) return true

        // Fallback package manager check for recognition service or Google speech intent
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        val activities = context.packageManager.queryIntentActivities(intent, 0)
        return activities.isNotEmpty()
    }

    /**
     * Finds the best SpeechRecognizer ComponentName on the device (e.g. Google speech service)
     * if default createSpeechRecognizer fails or is unavailable.
     */
    private fun findSpeechServiceComponent(): ComponentName? {
        val serviceIntent = Intent("android.speech.RecognitionService")
        val services = context.packageManager.queryIntentServices(serviceIntent, 0)
        
        // Prioritize Google recognition service if installed
        val googleService = services.firstOrNull { 
            it.serviceInfo.packageName.contains("google", ignoreCase = true) 
        }
        if (googleService != null) {
            return ComponentName(googleService.serviceInfo.packageName, googleService.serviceInfo.name)
        }
        val firstService = services.firstOrNull()
        if (firstService != null) {
            return ComponentName(firstService.serviceInfo.packageName, firstService.serviceInfo.name)
        }
        return null
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

        try {
            _voiceState.value = VoiceState.Initializing
            _partialText.value = ""
            _liveRmsDb.value = 0f

            val recognizer: SpeechRecognizer = try {
                val serviceComponent = findSpeechServiceComponent()
                if (serviceComponent != null) {
                    SpeechRecognizer.createSpeechRecognizer(context, serviceComponent)
                } else {
                    SpeechRecognizer.createSpeechRecognizer(context)
                }
            } catch (e: Exception) {
                Log.w("VoiceInputManager", "createSpeechRecognizer failed", e)
                _voiceState.value = VoiceState.Error(
                    message = "Встроенная служба распознавания недоступна. Нажмите «Системный микрофон Google» ниже.",
                    canFallbackToSystem = true
                )
                return
            }

            speechRecognizer = recognizer

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

            recognizer.setRecognitionListener(object : RecognitionListener {
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
                    Log.d("VoiceInputManager", "SpeechRecognizer error: $error")
                    val (message, canFallback) = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> 
                            "Ошибка звука с микрофона. Проверьте микрофон устройства" to true
                        SpeechRecognizer.ERROR_CLIENT -> 
                            "Служба распознавания занята. Нажмите кнопку «Системный микрофон Google» ниже" to true
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> 
                            "Не предоставлен доступ к микрофону. Разрешите запись звука в настройках" to false
                        SpeechRecognizer.ERROR_NETWORK -> 
                            "Ошибка сети при распознавании. Проверьте интернет или нажмите «Системный микрофон Google»" to true
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> 
                            "Таймаут подключения к серверу речи. Нажмите «Системный микрофон Google»" to true
                        SpeechRecognizer.ERROR_NO_MATCH -> 
                            "Речь не распознана. Нажмите «Повторить» или используйте «Системный микрофон Google»" to true
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 
                            "Служба распознавания занята, попробуйте еще раз" to true
                        SpeechRecognizer.ERROR_SERVER -> 
                            "Ошибка сервера речи. Нажмите «Системный микрофон Google»" to true
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 
                            "Голос не обнаружен. Нажмите микрофон и говорите громче" to true
                        else -> 
                            "Ошибка голосовой службы ($error). Воспользуйтесь кнопкой «Системный микрофон Google»" to true
                    }
                    _voiceState.value = VoiceState.Error(message, canFallback)
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spokenText = matches?.firstOrNull()?.trim() ?: _partialText.value
                    if (spokenText.isNotBlank()) {
                        _voiceState.value = VoiceState.Success(spokenText)
                    } else {
                        _voiceState.value = VoiceState.Error(
                            "Речь не распознана. Нажмите «Повторить» или «Системный микрофон Google»",
                            canFallbackToSystem = true
                        )
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

            recognizer.startListening(intent)

        } catch (e: Exception) {
            Log.e("VoiceInputManager", "startListening exception", e)
            _voiceState.value = VoiceState.Error(
                "Не удалось запустить микрофон: ${e.localizedMessage}. Воспользуйтесь кнопкой «Системный микрофон Google».",
                canFallbackToSystem = true
            )
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
