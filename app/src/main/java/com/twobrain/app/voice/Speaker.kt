package com.twobrain.app.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

/** Thin wrapper over TextToSpeech that reports when speech starts and ends. */
class Speaker(context: Context, private val onSpeaking: (Boolean) -> Unit) {
    private var ready = false
    private var pending: String? = null
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts.language = Locale.getDefault()
            tts.setSpeechRate(1.05f)
            pending?.let { speak(it) }
            pending = null
        }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = onSpeaking(true)
            override fun onDone(utteranceId: String?) = onSpeaking(false)
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = onSpeaking(false)
            override fun onError(utteranceId: String?, errorCode: Int) = onSpeaking(false)
        })
    }

    fun speak(text: String) {
        if (!ready) { pending = text; return }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    fun shutdown() { tts.stop(); tts.shutdown() }
}
