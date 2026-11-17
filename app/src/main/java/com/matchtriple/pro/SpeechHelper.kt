package com.matchtriple.pro

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class SpeechHelper(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech = TextToSpeech(context.applicationContext, this)
    private var ready = false
    var enabled = true
    var volume = 0.8f

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("id", "ID")
            ready = true
        }
    }

    fun speak(text: String) {
        if (!enabled || !ready) return
        tts.setSpeechRate(1.0f)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun stop() { if (ready) tts.stop() }
    fun shutdown() { tts.stop(); tts.shutdown() }
}