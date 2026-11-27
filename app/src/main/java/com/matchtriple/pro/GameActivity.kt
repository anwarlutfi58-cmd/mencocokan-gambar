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

        theme = intent.getStringExtra("theme") ?: "fruit"
        startLevel(1)

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

        if (config.timeLimitSec > 0 && prefs.getBoolean("timerOn", false)) {
            timeLeft = config.timeLimitSec
            b.txtTimer.visibility = View.VISIBLE
            b.txtTimer.text = "⏱ ${timeLeft}s"
            timerRunning = true
            handler.postDelayed(timerRunnable, 1000)
        } else {
            b.txtTimer.visibility = View.GONE
            timerRunning = false
        }

        speech.speak("Level $level dimulai. ${ThemeData.getName(theme)}. ${config.totalTiles} ubin.")
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

        // Ketuk pertama: buka kartu tertutup. Jangan sebut nama gambar.
        if (!engine.isFaceUp(idx)) {
            val opened = engine.reveal(idx) ?: return
            vibrate(15)
            speech.speak("Kartu dibuka")
            adapter.notifyItemChanged(idx)
            return
        }

        // Ketuk kedua: ambil kartu yang sudah terbuka.
        val tile = engine.pick(idx) ?: return

        busy = true
        vibrate(20)
        speech.speak(tile.name)

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
                } else {
                    break
                }
            }

            if (matchedCount > 0) {
                vibrate(60)
                speech.speak("Cocok! Skor $score")
            }

            adapter.notifyDataSetChanged()
            rebuildTray()
            updateHUD()

            busy = false
            afterMove()
        }, 160)
    }

    private fun afterMove() {
        engine.recomputeLocks()
        adapter.notifyDataSetChanged()
        updateHUD()

        if (engine.isWin()) {
            winLevel()
            return
        }

        if (engine.isLose()) {
            gameOver("Kotak penampung penuh!")
            return
        }
    }

    private fun winLevel() {
        timerRunning = false
        handler.removeCallbacks(timerRunnable)

        val bonus = 100 * config.scoreMultiplier
        score += bonus
        saveHi()

        val view = layoutInflater.inflate(R.layout.dialog_result, null)

        view.findViewById<TextView>(R.id.resTitle).text = "🎉 Level Selesai!"
        view.findViewById<TextView>(R.id.resText).text =
            "Skor: $score\nBonus +$bonus\nLevel berikutnya lebih banyak tumpukan."

        val btnContinue = view.findViewById<Button>(R.id.btnContinuePlay)
        val btnNext = view.findViewById<Button>(R.id.btnNextLevel)
        val btnRetry = view.findViewById<Button>(R.id.btnRetry)
        val btnMenu = view.findViewById<Button>(R.id.btnBackMenu)

        btnContinue.visibility = View.GONE
        btnRetry.visibility = View.GONE
        btnNext.visibility = View.VISIBLE
        btnNext.text = "▶️ Lanjutkan Bermain"

        val dlg = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()

        resultDialog = dlg

        btnNext.setOnClickListener {
            dlg.dismiss()
            resultDialog = null
            startLevel(level + 1)
        }

        btnMenu.setOnClickListener {
            dlg.dismiss()
            resultDialog = null
            finish()
        }

        dlg.show()
        speech.speak("Selamat! Level selesai. Tekan lanjutkan bermain untuk level berikutnya.")
    }

    private fun gameOver(reason: String) {
        timerRunning = false
        handler.removeCallbacks(timerRunnable)
        saveHi()

        val view = layoutInflater.inflate(R.layout.dialog_result, null)

        view.findViewById<TextView>(R.id.resTitle).text = "😢 Game Over"
        view.findViewById<TextView>(R.id.resText).text = "$reason\nSkor akhir: $score"

        val btnContinue = view.findViewById<Button>(R.id.btnContinuePlay)
        val btnNext = view.findViewById<Button>(R.id.btnNextLevel)
        val btnRetry = view.findViewById<Button>(R.id.btnRetry)
        val btnMenu = view.findViewById<Button>(R.id.btnBackMenu)

        btnNext.visibility = View.GONE
        btnRetry.visibility = View.VISIBLE
        btnContinue.visibility = if (!continueUsed) View.VISIBLE else View.GONE

        val dlg = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()

        resultDialog = dlg

        btnContinue.setOnClickListener {
            continueUsed = true
            dlg.dismiss()
            resultDialog = null
            continueAfterLose()
        }

        btnRetry.setOnClickListener {
            dlg.dismiss()
            resultDialog = null
            startLevel(level)
        }

        btnMenu.setOnClickListener {
            dlg.dismiss()
            resultDialog = null
            finish()
        }

        dlg.show()
        vibrate(300)
        speech.speak("Game over. Kamu bisa tekan lanjutkan bermain sekali, atau ulangi level.")
    }

    private fun continueAfterLose() {
        // Buang isi penampung supaya pemain bisa lanjut, lalu acak ulang papan.
        engine.tray.clear()
        engine.shuffle()

        boosters[0] = maxOf(boosters[0], 1)
        boosters[1] = maxOf(boosters[1], 1)
        boosters[2] = maxOf(boosters[2], 1)

        rebuildTray()
        updateBoosters()
        adapter.notifyDataSetChanged()
        updateHUD()

        if (config.timeLimitSec > 0 && prefs.getBoolean("timerOn", false)) {
            timeLeft = maxOf(timeLeft, 30)
            b.txtTimer.visibility = View.VISIBLE
            b.txtTimer.text = "⏱ ${timeLeft}s"
            timerRunning = true
            handler.postDelayed(timerRunnable, 1000)
        }

        vibrate(40)
        speech.speak("Lanjutkan bermain. Penampung dikosongkan dan papan diacak.")
    }

    private fun saveHi() {
        val hi = prefs.getInt("hiScore", 0)

        if (score > hi) {
            prefs.edit().putInt("hiScore", score).apply()
        }
    }

    private fun useUndo() {
        if (busy || boosters[0] <= 0) return

        if (engine.undo()) {
            boosters[0]--
            updateBoosters()
            adapter.notifyDataSetChanged()
            rebuildTray()
            updateHUD()
            vibrate(20)
            speech.speak("Undo berhasil")
        } else {
            speech.speak("Tidak bisa di-undo")
        }
    }

    private fun useShuffle() {
        if (busy || boosters[1] <= 0) return

        engine.shuffle()
        boosters[1]--

        updateBoosters()
        adapter.notifyDataSetChanged()
        vibrate(20)
        speech.speak("Papan diacak")
    }

    private fun useHint() {
        if (busy || boosters[2] <= 0) return

        val idx = engine.hintReveal()
        if (idx < 0) {
            speech.speak("Tidak ada kartu tertutup. Coba shuffle.")
            return
        }

        boosters[2]--
        updateBoosters()
        adapter.notifyItemChanged(idx)
        speech.speak("Satu kartu dibuka")

        val vh = b.rvBoard.findViewHolderForAdapterPosition(idx)
        if (vh != null) {
            val v = vh.itemView
            v.animate()
                .scaleX(1.15f)
                .scaleY(1.15f)
                .setDuration(200)
                .withEndAction {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start()
                }
                .start()
        }
    }

    private fun showSettings() {
        val view = layoutInflater.inflate(R.layout.dialog_settings, null)

        val swAudio = view.findViewById<android.widget.Switch>(R.id.swAudio)
        val swDim = view.findViewById<android.widget.Switch>(R.id.swDim)
        val swTimer = view.findViewById<android.widget.Switch>(R.id.swTimer)
        val seek = view.findViewById<android.widget.SeekBar>(R.id.seekVolume)

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
            speech.volume = seek.progress / 100f

            dlg.dismiss()
        }

        view.findViewById<Button>(R.id.btnQuitSettings).setOnClickListener {
            dlg.dismiss()
            finish()
        }

        dlg.show()
    }

    private fun vibrate(ms: Long) {
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(ms)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        resultDialog?.dismiss()
        resultDialog = null
        speech.shutdown()
        super.onDestroy()
    }
}