package com.mindcheck.app.presentation.mood

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.mindcheck.app.data.local.database.AppDatabase
import com.mindcheck.app.data.local.entity.MoodLogEntity
import com.mindcheck.app.data.local.entity.MoodTriggerEntity
import com.mindcheck.app.data.mock.DatabaseInitializer
import com.mindcheck.app.data.firebase.FirebaseSyncService
import com.mindcheck.app.databinding.FragmentMoodBinding
import com.mindcheck.app.utils.UserSession
import com.mindcheck.app.utils.UpgradePrompt
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Mood Tracker Fragment - Phase 2
 * Log daily mood with notes and triggers
 */
class MoodFragment : Fragment() {

    private var _binding: FragmentMoodBinding? = null
    private val binding get() = _binding!!

    private var selectedMood: String? = null
    private val selectedTriggers = mutableSetOf<String>()

    private val triggerCategories = listOf(
        "Akademik", "Keuangan", "Tidur", "Kesehatan", "Keluarga", "Hubungan", "Lainnya"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMoodSelector()
        setupTriggerChips()
        setupSaveButton()
        loadMoodHistory()
    }

    private fun setupMoodSelector() {
        // Set click listeners for mood buttons
        binding.btnMoodVeryLow.setOnClickListener { selectMood("Sangat Rendah", binding.btnMoodVeryLow) }
        binding.btnMoodLow.setOnClickListener { selectMood("Rendah", binding.btnMoodLow) }
        binding.btnMoodNormal.setOnClickListener { selectMood("Biasa", binding.btnMoodNormal) }
        binding.btnMoodGood.setOnClickListener { selectMood("Baik", binding.btnMoodGood) }
        binding.btnMoodVeryGood.setOnClickListener { selectMood("Sangat Baik", binding.btnMoodVeryGood) }
    }

    private fun selectMood(mood: String, button: View) {
        selectedMood = mood

        // Reset all buttons - use scale instead of alpha for better visibility
        binding.btnMoodVeryLow.scaleX = 0.9f
        binding.btnMoodVeryLow.scaleY = 0.9f
        binding.btnMoodLow.scaleX = 0.9f
        binding.btnMoodLow.scaleY = 0.9f
        binding.btnMoodNormal.scaleX = 0.9f
        binding.btnMoodNormal.scaleY = 0.9f
        binding.btnMoodGood.scaleX = 0.9f
        binding.btnMoodGood.scaleY = 0.9f
        binding.btnMoodVeryGood.scaleX = 0.9f
        binding.btnMoodVeryGood.scaleY = 0.9f

        // Highlight selected - make it larger
        button.scaleX = 1.1f
        button.scaleY = 1.1f

        binding.btnSaveMood.isEnabled = true
    }

    private fun setupTriggerChips() {
        triggerCategories.forEach { category ->
            val chip = Chip(requireContext()).apply {
                text = category
                isCheckable = true
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedTriggers.add(category)
                    } else {
                        selectedTriggers.remove(category)
                    }
                }
            }
            binding.chipGroupTriggers.addView(chip)
        }
    }

    private fun setupSaveButton() {
        binding.btnSaveMood.isEnabled = false

        binding.btnSaveMood.setOnClickListener {
            saveMood()
        }
    }

    private fun saveMood() {
        // Check guest mode restrictions
        if (!UserSession.canAccessFeature(requireContext(), UserSession.Feature.MOOD_TRACKING)) {
            UpgradePrompt.show(requireContext(), UserSession.Feature.MOOD_TRACKING)
            return
        }

        val mood = selectedMood ?: return
        val notes = binding.etNotes.text.toString()

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val userId = DatabaseInitializer.getCurrentUserId(requireContext()) ?: return@launch

                // Create mood log
                val moodLog = MoodLogEntity(
                    userId = userId,
                    tingkatMood = mood,
                    catatan = notes,
                    tanggal = System.currentTimeMillis()
                )

                db.moodLogDao().insertMoodLog(moodLog)

                // Insert triggers
                if (selectedTriggers.isNotEmpty()) {
                    val triggerEntities = selectedTriggers.map { category ->
                        MoodTriggerEntity(
                            moodId = moodLog.moodId,
                            kategoriPemicu = category
                        )
                    }
                    db.moodTriggerDao().insertMoodTriggers(triggerEntities)
                }

                Toast.makeText(requireContext(), "Mood berhasil dicatat! ✨", Toast.LENGTH_SHORT).show()

                // Reset form
                resetForm()

                // Reload history
                loadMoodHistory()

                // Sync to Firestore in background (non-blocking)
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val syncService = FirebaseSyncService(requireContext())
                        syncService.syncToFirestore(userId)
                        Timber.d("Mood log synced to Firestore")
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to sync mood log to Firestore (offline or error)")
                    }
                }

                Timber.d("Mood saved: $mood with ${selectedTriggers.size} triggers")

            } catch (e: Exception) {
                Timber.e(e, "Error saving mood")
                Toast.makeText(requireContext(), "Gagal menyimpan mood", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetForm() {
        selectedMood = null
        selectedTriggers.clear()

        // Reset scale to normal size
        binding.btnMoodVeryLow.scaleX = 0.9f
        binding.btnMoodVeryLow.scaleY = 0.9f
        binding.btnMoodLow.scaleX = 0.9f
        binding.btnMoodLow.scaleY = 0.9f
        binding.btnMoodNormal.scaleX = 0.9f
        binding.btnMoodNormal.scaleY = 0.9f
        binding.btnMoodGood.scaleX = 0.9f
        binding.btnMoodGood.scaleY = 0.9f
        binding.btnMoodVeryGood.scaleX = 0.9f
        binding.btnMoodVeryGood.scaleY = 0.9f

        binding.etNotes.text?.clear()
        binding.chipGroupTriggers.clearCheck()
        binding.btnSaveMood.isEnabled = false
    }

    private fun loadMoodHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Check if fragment is still attached
                if (!isAdded) return@launch

                val db = AppDatabase.getDatabase(requireContext())
                val userId = DatabaseInitializer.getCurrentUserId(requireContext()) ?: return@launch

                // Load only recent mood logs (last 30 entries) for better performance
                val moodLogs = db.moodLogDao().getRecentMoodLogs(userId, 30)

                // Check again before updating UI
                if (!isAdded || _binding == null) return@launch

                // Show count
                val totalCount = db.moodLogDao().getMoodLogCount(userId)
                binding.tvMoodHistory.text = "Riwayat Mood ($totalCount catatan)"

                // Calculate this week's average
                val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                val thisWeekMoods = moodLogs.filter { it.tanggal >= weekAgo }

                if (thisWeekMoods.isNotEmpty()) {
                    val avgMood = thisWeekMoods.map { moodToNumber(it.tingkatMood) }.average()
                    binding.tvWeekAverage.text = "Rata-rata minggu ini: ${numberToMood(avgMood.toInt())}"
                } else {
                    binding.tvWeekAverage.text = "Belum ada data minggu ini"
                }

                // Display recent mood entries
                if (moodLogs.isNotEmpty()) {
                    val dateFormat = java.text.SimpleDateFormat("dd MMM, HH:mm", java.util.Locale("id", "ID"))
                    val entriesText = buildString {
                        moodLogs.take(7).forEach { log ->
                            val date = dateFormat.format(java.util.Date(log.tanggal))
                            val moodEmoji = when (log.tingkatMood) {
                                "Sangat Rendah" -> "😞"
                                "Rendah" -> "😟"
                                "Biasa" -> "😐"
                                "Baik" -> "🙂"
                                "Sangat Baik" -> "😊"
                                else -> "😐"
                            }
                            append("$moodEmoji $date - ${log.tingkatMood}\n")
                            if (log.catatan.isNotEmpty()) {
                                append("   ${log.catatan}\n")
                            }
                            append("\n")
                        }
                    }
                    binding.tvMoodEntries.text = entriesText.trim()
                    binding.cardMoodList.visibility = android.view.View.VISIBLE
                } else {
                    binding.tvMoodEntries.text = "Belum ada catatan mood"
                    binding.cardMoodList.visibility = android.view.View.VISIBLE
                }

                Timber.d("Loaded ${moodLogs.size} mood logs (optimized)")

            } catch (e: Exception) {
                Timber.e(e, "Error loading mood history")
            }
        }
    }

    private fun moodToNumber(mood: String): Int {
        return when (mood) {
            "Sangat Rendah" -> 1
            "Rendah" -> 2
            "Biasa" -> 3
            "Baik" -> 4
            "Sangat Baik" -> 5
            else -> 3
        }
    }

    private fun numberToMood(number: Int): String {
        return when (number) {
            1 -> "Sangat Rendah"
            2 -> "Rendah"
            3 -> "Biasa"
            4 -> "Baik"
            5 -> "Sangat Baik"
            else -> "Biasa"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
