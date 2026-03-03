package com.mindcheck.app.presentation.journal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.mindcheck.app.data.local.database.AppDatabase
import com.mindcheck.app.data.local.entity.GratitudeEntryEntity
import com.mindcheck.app.data.mock.DatabaseInitializer
import com.mindcheck.app.data.firebase.FirebaseSyncService
import com.mindcheck.app.databinding.FragmentJournalBinding
import com.mindcheck.app.utils.UserSession
import com.mindcheck.app.utils.UpgradePrompt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

/**
 * Gratitude Journal Fragment - Phase 2
 * Daily gratitude entries (3 items)
 */
class JournalFragment : Fragment() {

    private var _binding: FragmentJournalBinding? = null
    private val binding get() = _binding!!

    private val gratitudeQuotes = listOf(
        "Gratitude turns what we have into enough.",
        "Bersyukur adalah kunci kebahagiaan.",
        "Setiap hari adalah anugerah.",
        "Syukuri apa yang ada, nikmati apa yang tersisa.",
        "Kebahagiaan dimulai dengan rasa syukur."
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJournalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showRandomQuote()
        setupSaveButton()
        loadJournalStats()
    }

    private fun showRandomQuote() {
        binding.tvQuote.text = "\"${gratitudeQuotes.random()}\" 💚"
    }

    private fun setupSaveButton() {
        binding.btnSaveJournal.setOnClickListener {
            saveJournal()
        }
    }

    private fun saveJournal() {
        // Check guest mode restrictions
        if (!UserSession.canAccessFeature(requireContext(), UserSession.Feature.JOURNAL)) {
            UpgradePrompt.show(requireContext(), UserSession.Feature.JOURNAL)
            return
        }

        val item1 = binding.etItem1.text.toString().trim()
        val item2 = binding.etItem2.text.toString().trim()
        val item3 = binding.etItem3.text.toString().trim()

        if (item1.isEmpty()) {
            Toast.makeText(requireContext(), "Minimal isi 1 item", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val userId = DatabaseInitializer.getCurrentUserId(requireContext()) ?: return@launch

                val entry = GratitudeEntryEntity(
                    userId = userId,
                    item1 = item1,
                    item2 = item2,
                    item3 = item3,
                    tanggal = System.currentTimeMillis()
                )

                db.gratitudeEntryDao().insertGratitudeEntry(entry)

                Toast.makeText(requireContext(), "Jurnal disimpan! ✨", Toast.LENGTH_SHORT).show()

                // Clear form
                binding.etItem1.text?.clear()
                binding.etItem2.text?.clear()
                binding.etItem3.text?.clear()

                // Show new quote
                showRandomQuote()

                // Reload stats
                loadJournalStats()

                // Sync to Firestore in background (non-blocking)
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val syncService = FirebaseSyncService(requireContext())
                        syncService.syncToFirestore(userId)
                        Timber.d("Gratitude entry synced to Firestore")
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to sync gratitude entry to Firestore (offline or error)")
                    }
                }

                Timber.d("Gratitude entry saved")

            } catch (e: Exception) {
                Timber.e(e, "Error saving gratitude entry")
                Toast.makeText(requireContext(), "Gagal menyimpan jurnal", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadJournalStats() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val userId = DatabaseInitializer.getCurrentUserId(requireContext()) ?: return@launch

                val entries = db.gratitudeEntryDao().getGratitudeEntriesByUser(userId).first()

                // Count this month
                val startOfMonth = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
                val thisMonthCount = entries.count { it.tanggal >= startOfMonth }

                binding.tvMonthlyCount.text = "Kamu sudah menulis $thisMonthCount hari bulan ini!"

                if (thisMonthCount >= 20) {
                    binding.tvMotivation.text = "Luar biasa! Pertahankan! 🔥"
                } else if (thisMonthCount >= 10) {
                    binding.tvMotivation.text = "Bagus! Terus lanjutkan! 💪"
                } else {
                    binding.tvMotivation.text = "Ayo mulai kebiasaan menulis jurnal! 📝"
                }

                // Calculate and display streak
                val streak = calculateGratitudeStreak(entries)
                if (streak >= 3) {
                    binding.cardStreak.visibility = View.VISIBLE
                    binding.tvStreakCount.text = "$streak Hari Streak!"
                } else {
                    binding.cardStreak.visibility = View.GONE
                }

                // Load recent entries
                loadRecentEntries(entries)

                Timber.d("Loaded ${entries.size} gratitude entries, streak: $streak")

            } catch (e: Exception) {
                Timber.e(e, "Error loading journal stats")
            }
        }
    }

    private fun calculateGratitudeStreak(entries: List<GratitudeEntryEntity>): Int {
        if (entries.isEmpty()) return 0

        val oneDayMillis = 24 * 60 * 60 * 1000L
        val sortedDates = entries.map { it.tanggal / oneDayMillis }.sorted().distinct()
        val today = System.currentTimeMillis() / oneDayMillis

        var streak = 0
        var currentDay = today

        for (date in sortedDates.reversed()) {
            if (date == currentDay) {
                streak++
                currentDay--
            } else if (date < currentDay - 1) {
                break
            }
        }

        return streak
    }

    private fun loadRecentEntries(entries: List<GratitudeEntryEntity>) {
        if (entries.isEmpty()) {
            binding.tvRecentEntries.text = "Belum ada entri"
            return
        }

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
        val recentText = buildString {
            entries.sortedByDescending { it.tanggal }
                .take(10)
                .forEach { entry ->
                    val date = dateFormat.format(Date(entry.tanggal))
                    append("📝 $date\n")
                    append("  • ${entry.item1}\n")
                    if (entry.item2.isNotBlank()) append("  • ${entry.item2}\n")
                    if (entry.item3.isNotBlank()) append("  • ${entry.item3}\n")
                    append("\n")
                }
        }

        binding.tvRecentEntries.text = recentText.trim()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
