package com.mindcheck.app.presentation.breathing

import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import com.mindcheck.app.databinding.ActivityBreathingBinding
import timber.log.Timber

/**
 * Breathing Exercises Activity - Phase 2
 * Guided breathing techniques
 */
class BreathingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBreathingBinding
    private var currentPhase = BreathingPhase.INHALE
    private var currentCycle = 0
    private val totalCycles = 5
    private var timer: CountDownTimer? = null

    enum class BreathingPhase(val duration: Long, val text: String) {
        INHALE(4000, "TARIK NAPAS"),
        HOLD(7000, "TAHAN"),
        EXHALE(8000, "HEMBUSKAN")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBreathingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnStart.setOnClickListener {
            startBreathing()
            binding.btnStart.isEnabled = false
            binding.btnStop.isEnabled = true
        }

        binding.btnStop.setOnClickListener {
            stopBreathing()
        }

        binding.btnClose.setOnClickListener {
            finish()
        }
    }

    private fun startBreathing() {
        currentCycle = 0
        startPhase(BreathingPhase.INHALE)
    }

    private fun startPhase(phase: BreathingPhase) {
        currentPhase = phase
        binding.tvInstruction.text = phase.text
        binding.tvCycleCount.text = "Siklus: ${currentCycle + 1}/$totalCycles"

        // Animate circle
        when (phase) {
            BreathingPhase.INHALE -> {
                binding.breathingCircle.animate()
                    .scaleX(1.5f)
                    .scaleY(1.5f)
                    .setDuration(phase.duration)
                    .start()
            }
            BreathingPhase.HOLD -> {
                // Stay at current size
            }
            BreathingPhase.EXHALE -> {
                binding.breathingCircle.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(phase.duration)
                    .start()
            }
        }

        // Countdown timer
        timer = object : CountDownTimer(phase.duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                binding.tvCountdown.text = secondsLeft.toString()
            }

            override fun onFinish() {
                moveToNextPhase()
            }
        }.start()
    }

    private fun moveToNextPhase() {
        when (currentPhase) {
            BreathingPhase.INHALE -> startPhase(BreathingPhase.HOLD)
            BreathingPhase.HOLD -> startPhase(BreathingPhase.EXHALE)
            BreathingPhase.EXHALE -> {
                currentCycle++
                if (currentCycle < totalCycles) {
                    startPhase(BreathingPhase.INHALE)
                } else {
                    finishBreathing()
                }
            }
        }
    }

    private fun finishBreathing() {
        binding.tvInstruction.text = "Latihan Selesai!"
        binding.tvCountdown.text = "✨"
        binding.btnStart.isEnabled = true
        binding.btnStop.isEnabled = false

        Timber.d("Breathing exercise completed")
    }

    private fun stopBreathing() {
        timer?.cancel()
        binding.breathingCircle.animate().scaleX(1.0f).scaleY(1.0f).setDuration(0).start()
        binding.tvInstruction.text = "Dihentikan"
        binding.tvCountdown.text = ""
        binding.btnStart.isEnabled = true
        binding.btnStop.isEnabled = false
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}
