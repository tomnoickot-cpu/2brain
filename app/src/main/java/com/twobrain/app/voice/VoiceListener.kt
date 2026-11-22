package com.twobrain.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Keeps Android's SpeechRecognizer running in a loop so the app is always listening while open.
 * Every final result is handed to [onUtterance]; partial results stream to [onPartial].
 */
class VoiceListener(
    private val context: Context,
    private val onPartial: (String) -> Unit,
    private val onUtterance: (String) -> Unit,
    private val onLevel: (Float) -> Unit,
    private val onState: (Boolean) -> Unit, // true = actively hearing speech
    private val onUnavailable: () -> Unit,
) {
    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var running = false
    private var held = false // paused while the app itself is speaking

    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
    }

    fun start() {
        if (running) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) { onUnavailable(); return }
        running = true
        create()
        listen()
    }

    fun stop() {
        running = false
        main.removeCallbacksAndMessages(null)
        recognizer?.run { cancel(); destroy() }
        recognizer = null
        onState(false)
    }

    /** Stop the mic while we talk, so we don't hear ourselves. */
    fun hold() {
        held = true
        main.removeCallbacksAndMessages(null)
        recognizer?.cancel()
        onState(false)
    }

    fun release() {
        if (!held) return
        held = false
        if (running) restart(150)
    }

    private fun create() {
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply { setRecognitionListener(listener) }
    }

    private fun listen() {
        if (!running || held) return
        runCatching { recognizer?.startListening(intent) }.onFailure { restart(800, recreate = true) }
    }

    private fun restart(delayMs: Long, recreate: Boolean = false) {
        main.removeCallbacksAndMessages(null)
        main.postDelayed({
            if (recreate) create()
            listen()
        }, delayMs)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() = onState(true)
        override fun onRmsChanged(rmsdB: Float) = onLevel(((rmsdB + 2f) / 12f).coerceIn(0f, 1f))
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() = onState(false)
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onPartialResults(partialResults: Bundle?) {
            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                ?.takeIf { it.isNotBlank() }?.let(onPartial)
        }

        override fun onResults(results: Bundle?) {
            onState(false)
            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                ?.takeIf { it.isNotBlank() }?.let(onUtterance)
            restart(100)
        }

        override fun onError(error: Int) {
            onState(false)
            when (error) {
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> { running = false; onUnavailable() }
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                SpeechRecognizer.ERROR_CLIENT,
                SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> restart(600, recreate = true)
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_SERVER -> restart(2000)
                else -> restart(250) // no match / speech timeout: just keep listening
            }
        }
    }
}
