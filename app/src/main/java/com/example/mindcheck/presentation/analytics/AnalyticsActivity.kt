package com.mindcheck.app.presentation.analytics

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.mindcheck.app.R
import com.mindcheck.app.data.local.database.AppDatabase
import com.mindcheck.app.data.local.entity.MoodLogEntity
import com.mindcheck.app.data.mock.DatabaseInitializer
import com.mindcheck.app.databinding.ActivityAnalyticsBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

/**
 * Analytics Activity - Phase 2
 * Visualize mood trends, sleep patterns, screening history
 */
class AnalyticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalyticsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Analitik"

        loadAnalytics()
    }

    private fun loadAnalytics() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@AnalyticsActivity)

                // Get userId on IO thread to avoid StrictMode violation
                val userId = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    DatabaseInitializer.getCurrentUserId(this@AnalyticsActivity)
                } ?: return@launch

                // Get all data
                val moodLogs = db.moodLogDao().getMoodLogsByUser(userId).first()
                val sleepLogs = db.sleepLogDao().getSleepLogsByUser(userId).first()
                val gratitudeEntries = db.gratitudeEntryDao().getGratitudeEntriesByUser(userId).first()
                val goals = db.goalDao().getGoalsByUser(userId).first()
                val screenings = db.screeningDao().getScreeningsByUser(userId).first()

                // Summary stats
                binding.tvMoodDays.text = "${moodLogs.size} hari"
                binding.tvGratitudeDays.text = "${gratitudeEntries.size} hari"

                val activeGoals = goals.count { it.status == "Aktif" }
                binding.tvActiveGoals.text = "$activeGoals target"

                val avgSleep = if (sleepLogs.isNotEmpty()) {
                    sleepLogs.map { it.durasiJam }.average()
                } else 0.0

                binding.tvAvgSleep.text = String.format("%.1f jam", avgSleep)

                // Calculate wellness score
                val wellnessScore = calculateWellnessScore(moodLogs.size, avgSleep.toFloat(), activeGoals, screenings.size)
                binding.tvWellnessScore.text = "$wellnessScore/100"
                binding.progressWellness.progress = wellnessScore

                val scoreLabel = when {
                    wellnessScore >= 80 -> "Sangat Baik!"
                    wellnessScore >= 60 -> "Baik"
                    wellnessScore >= 40 -> "Cukup"
                    else -> "Perlu Perhatian"
                }
                binding.tvScoreLabel.text = scoreLabel

                // Screening history
                if (screenings.isNotEmpty()) {
                    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
                    binding.tvScreeningHistory.text = buildString {
                        screenings.take(5).forEach { screening ->
                            val daysAgo = ((System.currentTimeMillis() - screening.tanggal) / (24 * 60 * 60 * 1000)).toInt()

                            // Create user-friendly time display
                            val timeDisplay = when {
                                daysAgo == 0 -> "Hari ini"
                                daysAgo == 1 -> "Kemarin"
                                daysAgo < 7 -> "$daysAgo hari lalu"
                                daysAgo < 30 -> "${daysAgo / 7} minggu lalu"
                                else -> dateFormat.format(Date(screening.tanggal))
                            }

                            // Ensure proper risk level display
                            val riskLevel = when (screening.tingkatRisiko) {
                                "Tidak Depresi", "Minimal", "Rendah" -> "Risiko Rendah"
                                "Ringan", "Sedang" -> "Risiko Sedang"
                                "Berat", "Sangat Berat", "Tinggi" -> "Risiko Tinggi"
                                else -> screening.tingkatRisiko
                            }

                            append("• $timeDisplay - $riskLevel")

                            // Add score if available
                            if (screening.skorPrediksi > 0) {
                                append(" (Skor: ${String.format("%.1f", screening.skorPrediksi)})")
                            }
                            append("\n")
                        }
                    }.trim()
                } else {
                    binding.tvScreeningHistory.text = "Belum ada riwayat screening.\nMulai screening pertama kamu untuk melacak kesehatan mental."
                }

                // Render mood chart
                renderMoodChart(moodLogs)

                Timber.d("Analytics loaded: $wellnessScore wellness score")

            } catch (e: Exception) {
                Timber.e(e, "Error loading analytics")
            }
        }
    }

    private fun renderMoodChart(moodLogs: List<MoodLogEntity>) {
        binding.moodChartContainer.removeAllViews()

        if (moodLogs.isEmpty()) {
            binding.tvMoodChartEmpty.isVisible = true
            return
        }

        binding.tvMoodChartEmpty.isVisible = false

        // Get last 7 days of mood logs
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val last7Days = mutableListOf<Pair<String, MoodLogEntity?>>()
        for (i in 6 downTo 0) {
            val dayTime = cal.timeInMillis
            val dayFormat = SimpleDateFormat("EEE", Locale("id", "ID"))
            val dayLabel = dayFormat.format(Date(dayTime))

            // Find mood for this day
            val mood = moodLogs.firstOrNull { moodLog ->
                val moodCal = Calendar.getInstance().apply { timeInMillis = moodLog.tanggal }
                moodCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                moodCal.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
            }

            last7Days.add(dayLabel to mood)
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }

        // Render bars
        last7Days.reversed().forEach { (dayLabel, mood) ->
            val barContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    1f
                )
            }

            // Bar
            val bar = androidx.appcompat.widget.AppCompatImageView(this).apply {
                val heightDp = if (mood != null) {
                    when (mood.tingkatMood) {
                        "Sangat Baik" -> 140
                        "Baik" -> 110
                        "Biasa" -> 80
                        "Rendah" -> 50
                        "Sangat Rendah" -> 30
                        else -> 0
                    }
                } else 0

                val heightPx = (heightDp * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(
                    32.dpToPx(),
                    heightPx
                )

                if (mood != null) {
                    val bgRes = when (mood.tingkatMood) {
                        "Sangat Baik" -> R.drawable.bg_mood_very_good
                        "Baik" -> R.drawable.bg_mood_good
                        "Biasa" -> R.drawable.bg_mood_normal
                        "Rendah" -> R.drawable.bg_mood_low
                        "Sangat Rendah" -> R.drawable.bg_mood_very_low
                        else -> R.drawable.bg_mood_normal
                    }
                    setBackgroundResource(bgRes)
                } else {
                    // Empty state - show subtle placeholder
                    setBackgroundColor(Color.parseColor("#20FFFFFF"))
                }
            }

            // Day label
            val label = TextView(this).apply {
                text = dayLabel
                textSize = 11f
                setTextColor(Color.parseColor("#B3FFFFFF"))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 4.dpToPx()
                }
            }

            barContainer.addView(bar)
            barContainer.addView(label)
            binding.moodChartContainer.addView(barContainer)
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun calculateWellnessScore(
        moodDays: Int,
        avgSleep: Float,
        activeGoals: Int,
        screeningCount: Int
    ): Int {
        var score = 0

        // Mood consistency (30 points)
        score += (moodDays.toFloat() / 30 * 30).toInt().coerceAtMost(30)

        // Sleep quality (30 points)
        score += when {
            avgSleep in 7f..9f -> 30
            avgSleep in 6f..10f -> 20
            else -> 10
        }

        // Goals progress (20 points)
        score += if (activeGoals > 0) 20 else 0

        // Screening (20 points)
        score += if (screeningCount > 0) 20 else 0

        return score.coerceIn(0, 100)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
