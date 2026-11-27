package com.matchtriple.pro

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.matchtriple.pro.databinding.ActivityGameBinding

class GameActivity : AppCompatActivity() {
    private lateinit var b: ActivityGameBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var speech: SpeechHelper
    private lateinit var vibrator: Vibrator
    private lateinit var engine: GameEngine
    private lateinit var adapter: TileAdapter
    private lateinit var config: LevelConfig

    private var theme = "fruit"
    private var score = 0
    private var level = 1
    private var boosters = intArrayOf(3, 3, 3)
    private var busy = false
    private var timerRunning = false
    private var timeLeft = 0
    private var resultDialog: AlertDialog? = null
    private var continueUsed = false

    private val handler = Handler(Looper.getMainLooper())

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (!timerRunning) return

            timeLeft--
            b.txtTimer.text = "⏱ ${timeLeft}s"

            if (timeLeft <= 0) {
                timerRunning = false
                gameOver("Waktu habis!")
            } else {
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityGameBinding.inflate(layoutInflater)
        setContentView(b.root)

        prefs = getSharedPreferences("matchTriplePro", MODE_PRIVATE)

        speech = SpeechHelper(this)
        speech.enabled = prefs.getBoolean("audioOn", true)
        speech.volume = prefs.getFloat("volume", 0.8f)

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        theme = intent.getStringExtra("theme") ?: prefs.getString("savedTheme", "fruit") ?: "fruit"
        val resumeLevel = intent.getIntExtra("resumeLevel", -1)
        val resumeScore = intent.getIntExtra("resumeScore", 0)

        if (resumeLevel > 0) {
            score = resumeScore
            startLevel(resumeLevel)
        } else {
            score = 0
            startLevel(1)
        }

        b.btnPause.setOnClickListener { showSettings() }
        b.btnUndo.setOnClickListener { useUndo() }
        b.btnShuffle.setOnClickListener { useShuffle() }
        b.btnHint.setOnClickListener { useHint() }
    }

    private fun startLevel(lvl: Int) {
        handler.removeCallbacks(timerRunnable)
        resultDialog?.dismiss()
        resultDialog = null

        level = lvl
        continueUsed = false
        config = LevelConfigs.forLevel(level)
        engine = GameEngine(theme, config)
        engine.generate()

        boosters = intArrayOf(
            config.boosterUndo,
            config.boosterShuffle,
            config.boosterHint
        )

        b.rvBoard.layoutManager = GridLayoutManager(this, config.cols)
        b.rvBoard.setHasFixedSize(true)

        adapter = TileAdapter(engine.board, config.cols) { idx ->
            onTileClick(idx)
        }

        b.rvBoard.adapter = adapter

        rebuildTray()
        updateHUD()
        updateBoosters()
        applyTierColor()
        saveProgress()

        timeLeft = config.timeLimitSec
        b.txtTimer.visibility = View.VISIBLE
        b.txtTimer.text = "⏱ ${timeLeft}s"
        timerRunning = true
        handler.postDelayed(timerRunnable, 1000)

        speech.speak("Level $level dimulai. Waktu ${config.timeLimitSec} detik.")
    }

    private fun saveProgress() {
        prefs.edit()
            .putBoolean("hasSavedGame", true)
            .putString("savedTheme", theme)
            .putInt("savedLevel", level)
            .putInt("savedScore", score)
            .apply()
    }

    private fun clearProgress() {
        prefs.edit().putBoolean("hasSavedGame", false).apply()
    }

    private fun applyTierColor() {
        val c = androidx.core.content.ContextCompat.getColor(this, config.tierColorRes)
        b.progressLevel.progressTintList = android.content.res.ColorStateList.valueOf(c)
    }

    private fun updateHUD() {
        b.txtScore.text = "⭐ Skor: $score"
        b.txtLevel.text = "📊 Lv $level • ${engine.remaining()} sisa"

        val total = config.totalTiles
        val done = total - engine.remaining()

        b.progressLevel.max = total
        b.progressLevel.progress = done
    }

    private fun updateBoosters() {
        b.btnUndo.text = "↩️\nUndo x${boosters[0]}"
        b.btnShuffle.text = "🔀\nShuffle x${boosters[1]}"
        b.btnHint.text = "💡\nHint x${boosters[2]}"

        b.btnUndo.isEnabled = boosters[0] > 0
        b.btnShuffle.isEnabled = boosters[1] > 0
        b.btnHint.isEnabled = boosters[2] > 0
    }

    private fun rebuildTray() {
        b.trayContainer.removeAllViews()

        for (i in 0 until config.traySlots) {
            val tv = TextView(this)
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            lp.marginEnd = 4

            tv.layoutParams = lp
            tv.gravity = Gravity.CENTER
            tv.textSize = 20f
            tv.setPadding(0, 12, 0, 12)

            val tile = engine.tray.getOrNull(i)

            if (tile != null) {
                tv.text = tile.emoji
                tv.setBackgroundResource(R.drawable.bg_slot_filled)
            } else {
                tv.text = ""
                tv.setBackgroundResource(R.drawable.bg_slot)
            }

            b.trayContainer.addView(tv)
        }
    }

    private fun onTileClick(idx: Int) {
        if (busy) return
        val tile = engine.board.getOrNull(idx) ?: return

        // Kalau kartu sudah terbuka, ketuk lagi untuk mengambilnya.
        if (tile.faceUp) {
            takeCard(idx)
            return
        }

        val first = engine.firstOpenIdx

        // Belum ada kartu terbuka: buka kartu ini saja.
        if (first == -1) {
            val opened = engine.reveal(idx) ?: return
            vibrate(15)
            speech.speak(opened.name)
            adapter.notifyItemChanged(idx)
            return
        }

        // Sudah ada satu kartu terbuka: buka kartu kedua lalu bandingkan.
        if (idx == first) return

        val second = engine.reveal(idx) ?: return
        adapter.notifyItemChanged(idx)
        vibrate(15)

        val firstEmoji = engine.emojiAt(first)
        val secondEmoji = second.emoji

        if (firstEmoji == secondEmoji) {
            // Cocok berdua: langsung ambil keduanya.
            busy = true
            handler.postDelayed({
                engine.pick(first)
                engine.pick(idx)
                afterTake()
            }, 250)
        } else {
            // Tidak cocok: perlihatkan sebentar, lalu tutup otomatis.
            busy = true
            speech.speak("Tidak cocok")
            handler.postDelayed({
                engine.closeCard(first)
                engine.closeCard(idx)
                adapter.notifyItemChanged(first)
                adapter.notifyItemChanged(idx)
                busy = false
            }, 800)
        }
    }

    private fun takeCard(idx: Int) {
        busy = true
        val tile = engine.pick(idx) ?: run {
            busy = false
            return
        }
        vibrate(20)
        speech.speak(tile.name)
        afterTake()
    }

    private fun afterTake() {
        adapter.notifyDataSetChanged()
        rebuildTray()
        updateHUD()

        handler.postDelayed({
            var matchedCount = 0

            while (engine.hasMatch()) {
                val matched = engine.checkMatch()
                if (matched != null) {
                    matchedCount++
                    score += 30 * config.scoreMultiplier
                } else break
            }

            if (matchedCount > 0) {
                vibrate(60)
                speech.speak("Cocok tiga! Skor $score")
            }

            adapter.notifyDataSetChanged()
            rebuildTray