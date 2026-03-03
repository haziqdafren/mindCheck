package com.mindcheck.app.presentation.settings

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mindcheck.app.databinding.ActivitySettingsBinding
import timber.log.Timber

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val prefs by lazy { getSharedPreferences("mindcheck_prefs", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        setupClickListeners()
        loadSettings()
    }

    private fun setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Mood Reminder Toggle
        binding.switchMoodReminder.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("mood_reminder_enabled", isChecked).apply()
            Timber.d("Mood reminder ${if (isChecked) "enabled" else "disabled"}")
            showToast(if (isChecked) "Reminder mood diaktifkan" else "Reminder mood dinonaktifkan")
        }

        // Journal Reminder Toggle
        binding.switchJournalReminder.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("journal_reminder_enabled", isChecked).apply()
            Timber.d("Journal reminder ${if (isChecked) "enabled" else "disabled"}")
            showToast(if (isChecked) "Reminder jurnal diaktifkan" else "Reminder jurnal dinonaktifkan")
        }

        // Reminder Time Picker
        binding.cardReminderTime.setOnClickListener {
            showTimePicker()
        }
    }

    private fun loadSettings() {
        // Load Notification settings
        binding.switchMoodReminder.isChecked = prefs.getBoolean("mood_reminder_enabled", true)
        binding.switchJournalReminder.isChecked = prefs.getBoolean("journal_reminder_enabled", true)

        // Load Reminder Time
        val reminderHour = prefs.getInt("reminder_hour", 20)
        val reminderMinute = prefs.getInt("reminder_minute", 0)
        updateReminderTimeDisplay(reminderHour, reminderMinute)
    }

    private fun showTimePicker() {
        val currentHour = prefs.getInt("reminder_hour", 20)
        val currentMinute = prefs.getInt("reminder_minute", 0)

        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                // Save reminder time
                prefs.edit()
                    .putInt("reminder_hour", hourOfDay)
                    .putInt("reminder_minute", minute)
                    .apply()

                updateReminderTimeDisplay(hourOfDay, minute)
                showToast("Waktu reminder diatur: ${String.format("%02d:%02d", hourOfDay, minute)}")
                Timber.d("Reminder time set to $hourOfDay:$minute")
            },
            currentHour,
            currentMinute,
            true // 24-hour format
        ).show()
    }

    private fun updateReminderTimeDisplay(hour: Int, minute: Int) {
        val timeText = String.format("%02d:%02d", hour, minute)
        binding.tvReminderTime.text = timeText
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
