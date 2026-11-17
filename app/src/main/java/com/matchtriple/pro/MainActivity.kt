package com.matchtriple.pro

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Switch
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var speech: SpeechHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("matchTriplePro", MODE_PRIVATE)
        speech = SpeechHelper(this)
        speech.enabled = prefs.getBoolean("audioOn", true)
        speech.volume = prefs.getFloat("volume", 0.8f)

        findViewById<TextView>(R.id.hiScoreMain).text = prefs.getInt("hiScore", 0).toString()

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            speech.speak("Pilih kategori")
            showCategoryDialog()
        }
        findViewById<Button>(R.id.btnSettings).setOnClickListener { showSettings() }
        findViewById<Button>(R.id.btnHelp).setOnClickListener { showHelp() }
    }

    private fun showCategoryDialog() {
        val cats = arrayOf("🍎 Buah-Buahan", "🐱 Hewan Lucu", "🌹 Bunga Indah")
        AlertDialog.Builder(this)
            .setTitle("Pilih Kategori")
            .setItems(cats) { _, which ->
                val theme = when (which) { 0 -> "fruit"; 1 -> "animal"; else -> "flower" }
                startActivity(Intent(this, GameActivity::class.java).putExtra("theme", theme))
            }
            .show()
    }

    private fun showSettings() {
        val view = layoutInflater.inflate(R.layout.dialog_settings, null)
        val swAudio = view.findViewById<Switch>(R.id.swAudio)
        val swDim = view.findViewById<Switch>(R.id.swDim)
        val swTimer = view.findViewById<Switch>(R.id.swTimer)
        val seek = view.findViewById<SeekBar>(R.id.seekVolume)

        swAudio.isChecked = prefs.getBoolean("audioOn", true)
        swDim.isChecked = prefs.getBoolean("dim", false)
        swTimer.isChecked = prefs.getBoolean("timerOn", false)
        seek.progress = (prefs.getFloat("volume", 0.8f) * 100).toInt()

        val dlg = AlertDialog.Builder(this).setView(view).create()
        view.findViewById<Button>(R.id.btnContinueSettings).setOnClickListener {
            prefs.edit()
                .putBoolean("audioOn", swAudio.isChecked)
                .putBoolean("dim", swDim.isChecked)
                .putBoolean("timerOn", swTimer.isChecked)
                .putFloat("volume", seek.progress / 100f)
                .apply()
            speech.enabled = swAudio.isChecked
            dlg.dismiss()
        }
        view.findViewById<Button>(R.id.btnQuitSettings).setOnClickListener { dlg.dismiss() }
        dlg.show()
    }

    private fun showHelp() {
        val view = layoutInflater.inflate(R.layout.dialog_help, null)
        val dlg = AlertDialog.Builder(this).setView(view).create()
        view.findViewById<Button>(R.id.btnUnderstand).setOnClickListener { dlg.dismiss() }
        dlg.show()
    }

    override fun onDestroy() {
        speech.shutdown()
        super.onDestroy()
    }
}