package com.mindcheck.app.presentation.screening

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mindcheck.app.data.local.database.AppDatabase
import com.mindcheck.app.data.mock.DatabaseInitializer
import com.mindcheck.app.databinding.ActivityScreeningHistoryBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class ScreeningHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScreeningHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScreeningHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Riwayat Screening"

        loadScreeningHistory()
    }

    private fun loadScreeningHistory() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@ScreeningHistoryActivity)
                val userId = DatabaseInitializer.getCurrentUserId(this@ScreeningHistoryActivity)
                    ?: return@launch

                val screenings = db.screeningDao().getScreeningsByUser(userId).first()

                if (screenings.isEmpty()) {
                    binding.tvEmptyState.visibility = android.view.View.VISIBLE
                    binding.scrollView.visibility = android.view.View.GONE
                } else {
                    binding.tvEmptyState.visibility = android.view.View.GONE
                    binding.scrollView.visibility = android.view.View.VISIBLE

                    // Build history text
                    val historyText = buildString {
                        screenings.sortedByDescending { it.tanggal }.forEach { screening ->
                            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
                            val date = dateFormat.format(Date(screening.tanggal))

                            append("📋 $date\n")
                            append("Tingkat Risiko: ${screening.tingkatRisiko}\n")
                            append("Skor: ${screening.skorPrediksi}%\n")
                            append("Rekomendasi: ${screening.rekomendasi}\n")
                            append("\n")
                        }
                    }

                    binding.tvHistory.text = historyText
                    binding.tvCount.text = "Total ${screenings.size} screening"
                }

                Timber.d("Loaded ${screenings.size} screening records")

            } catch (e: Exception) {
                Timber.e(e, "Error loading screening history")
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
