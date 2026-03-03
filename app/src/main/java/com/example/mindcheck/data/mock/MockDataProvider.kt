package com.mindcheck.app.data.mock

import com.mindcheck.app.data.local.entity.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Provides realistic mock data for Phase 2 development
 * Generates varied, consistent data for testing and demo
 */
object MockDataProvider {

    private val currentTime = System.currentTimeMillis()
    private val oneDayMillis = TimeUnit.DAYS.toMillis(1)
    private val oneMonthMillis = TimeUnit.DAYS.toMillis(30)

    // Mood levels for distribution
    private val moodLevels = listOf("Sangat Rendah", "Rendah", "Biasa", "Baik", "Sangat Baik")
    private val moodDistribution = listOf(0.05, 0.15, 0.30, 0.35, 0.15) // Realistic distribution

    private val triggerCategories = listOf(
        "Akademik", "Keuangan", "Tidur", "Kesehatan", "Keluarga", "Hubungan", "Lainnya"
    )

    private val moodNotes = listOf(
        "Hari yang produktif!",
        "Merasa sedikit lelah hari ini",
        "Presentasi berjalan lancar",
        "Banyak tugas yang harus dikerjakan",
        "Bertemu teman lama, menyenangkan",
        "Tidak tidur dengan baik semalam",
        "Olahraga pagi membuat segar",
        "Stres dengan deadline",
        "Hari biasa saja",
        ""
    )

    /**
     * Generate 20-25 mood logs over last 30 days
     */
    fun generateMoodLogs(userId: String): List<MoodLogEntity> {
        val logs = mutableListOf<MoodLogEntity>()
        val daysToGenerate = (20..25).random()
        val days = (0 until 30).shuffled().take(daysToGenerate).sorted()

        days.forEach { daysAgo ->
            val mood = selectWeightedMood()
            logs.add(
                MoodLogEntity(
                    userId = userId,
                    tingkatMood = mood,
                    catatan = if (Random.nextFloat() > 0.3) moodNotes.random() else "",
                    tanggal = currentTime - (daysAgo * oneDayMillis),
                    createdAt = currentTime - (daysAgo * oneDayMillis)
                )
            )
        }
        return logs
    }

    /**
     * Generate mood triggers for mood logs
     */
    fun generateMoodTriggers(moodLogs: List<MoodLogEntity>): List<MoodTriggerEntity> {
        val triggers = mutableListOf<MoodTriggerEntity>()
        moodLogs.forEach { log ->
            // Each mood log has 0-3 triggers
            val triggerCount = (0..3).random()
            val selectedTriggers = triggerCategories.shuffled().take(triggerCount)

            selectedTriggers.forEach { category ->
                triggers.add(
                    MoodTriggerEntity(
                        moodId = log.moodId,
                        kategoriPemicu = category,
                        createdAt = log.createdAt
                    )
                )
            }
        }
        return triggers
    }

    /**
     * Generate 25-28 sleep logs over last 30 days
     */
    fun generateSleepLogs(userId: String): List<SleepLogEntity> {
        val logs = mutableListOf<SleepLogEntity>()
        val daysToGenerate = (25..28).random()
        val days = (0 until 30).shuffled().take(daysToGenerate).sorted()

        days.forEach { daysAgo ->
            val sleepHour = (21..23).random()
            val wakeHour = (5..8).random()
            val duration = if (wakeHour >= sleepHour) {
                (wakeHour - sleepHour).toFloat()
            } else {
                (24 - sleepHour + wakeHour).toFloat() + (Random.nextFloat() * 0.5f)
            }

            val category = when {
                duration < 5f -> "Kurang dari 5 jam"
                duration < 7f -> "5-6 jam"
                duration <= 9f -> "7-8 jam"
                else -> "Lebih dari 8 jam"
            }

            val quality = when {
                duration < 6f -> (2..3).random()
                duration in 7f..9f -> (3..5).random()
                else -> (2..4).random()
            }

            logs.add(
                SleepLogEntity(
                    userId = userId,
                    waktuTidur = String.format("%02d:00", sleepHour),
                    waktuBangun = String.format("%02d:00", wakeHour),
                    durasiJam = String.format("%.1f", duration).toFloat(),
                    kategoriDurasi = category,
                    kualitasTidur = quality,
                    tanggal = currentTime - (daysAgo * oneDayMillis),
                    createdAt = currentTime - (daysAgo * oneDayMillis)
                )
            )
        }
        return logs
    }

    /**
     * Generate 23 gratitude entries for current month
     */
    fun generateGratitudeEntries(userId: String): List<GratitudeEntryEntity> {
        val entries = mutableListOf<GratitudeEntryEntity>()
        val daysToGenerate = 23
        val days = (0 until 30).shuffled().take(daysToGenerate).sorted()

        val gratitudeItems = listOf(
            "Kesehatan keluarga", "Teman yang mendukung", "Cuaca cerah hari ini",
            "Makanan enak", "Tempat tinggal yang nyaman", "Kesempatan belajar",
            "Lulus ujian", "Proyek selesai tepat waktu", "Mendapat beasiswa",
            "Olahraga pagi", "Musik favorit", "Buku yang bagus",
            "Kopi pagi", "Tidur yang nyenyak", "Internet stabil",
            "Transportasi mudah", "Dosen yang pengertian", "Hobi yang menyenangkan",
            "Koneksi dengan alam", "Momen tenang", "Senyum orang lain",
            "Kesempatan berkembang", "Tubuh yang sehat", "Pikiran yang jernih"
        )

        days.forEach { daysAgo ->
            val items = gratitudeItems.shuffled().take(3)
            entries.add(
                GratitudeEntryEntity(
                    userId = userId,
                    item1 = items[0],
                    item2 = items.getOrNull(1) ?: "",
                    item3 = items.getOrNull(2) ?: "",
                    tanggal = currentTime - (daysAgo * oneDayMillis),
                    createdAt = currentTime - (daysAgo * oneDayMillis)
                )
            )
        }
        return entries
    }

    /**
     * Generate 3-4 goals with varying progress
     */
    fun generateGoals(userId: String): List<GoalEntity> {
        val goals = mutableListOf<GoalEntity>()

        // Goal 1: Active with high progress
        goals.add(
            GoalEntity(
                userId = userId,
                judul = "Olahraga Rutin",
                deskripsi = "Jogging 30 menit atau gym 3x seminggu",
                kategori = "Olahraga",
                tanggalMulai = currentTime - (20 * oneDayMillis),
                tanggalTarget = currentTime + (10 * oneDayMillis),
                status = "Aktif",
                createdAt = currentTime - (20 * oneDayMillis)
            )
        )

        // Goal 2: Active with medium progress
        goals.add(
            GoalEntity(
                userId = userId,
                judul = "Tidur 8 Jam Setiap Hari",
                deskripsi = "Tidur dan bangun di waktu yang sama setiap hari",
                kategori = "Tidur",
                tanggalMulai = currentTime - (15 * oneDayMillis),
                tanggalTarget = currentTime + (15 * oneDayMillis),
                status = "Aktif",
                createdAt = currentTime - (15 * oneDayMillis)
            )
        )

        // Goal 3: Active with low progress (just started)
        goals.add(
            GoalEntity(
                userId = userId,
                judul = "Meditasi Pagi 10 Menit",
                deskripsi = "Meditasi mindfulness setiap pagi sebelum aktivitas",
                kategori = "Stres",
                tanggalMulai = currentTime - (3 * oneDayMillis),
                tanggalTarget = currentTime + (27 * oneDayMillis),
                status = "Aktif",
                createdAt = currentTime - (3 * oneDayMillis)
            )
        )

        // Goal 4: Completed
        goals.add(
            GoalEntity(
                userId = userId,
                judul = "Baca 1 Buku Self-Help",
                deskripsi = "Menyelesaikan buku 'Atomic Habits'",
                kategori = "Belajar",
                tanggalMulai = currentTime - (45 * oneDayMillis),
                tanggalTarget = currentTime - (5 * oneDayMillis),
                status = "Selesai",
                createdAt = currentTime - (45 * oneDayMillis),
                updatedAt = currentTime - (5 * oneDayMillis)
            )
        )

        return goals
    }

    /**
     * Generate tasks for goals
     */
    fun generateTasks(goals: List<GoalEntity>): List<TaskEntity> {
        val tasks = mutableListOf<TaskEntity>()

        goals.forEach { goal ->
            when (goal.judul) {
                "Olahraga Rutin" -> {
                    tasks.add(
                        TaskEntity(
                            goalId = goal.goalId,
                            judulTugas = "Jogging pagi",
                            tipePengulangan = "Mingguan",
                            hariPengulangan = "Senin,Rabu,Jumat",
                            selesai = false,
                            createdAt = goal.createdAt
                        )
                    )
                    tasks.add(
                        TaskEntity(
                            goalId = goal.goalId,
                            judulTugas = "Gym sore",
                            tipePengulangan = "Mingguan",
                            hariPengulangan = "Selasa,Kamis",
                            selesai = false,
                            createdAt = goal.createdAt
                        )
                    )
                }
                "Tidur 8 Jam Setiap Hari" -> {
                    tasks.add(
                        TaskEntity(
                            goalId = goal.goalId,
                            judulTugas = "Tidur jam 22:00",
                            tipePengulangan = "Harian",
                            selesai = false,
                            createdAt = goal.createdAt
                        )
                    )
                    tasks.add(
                        TaskEntity(
                            goalId = goal.goalId,
                            judulTugas = "Bangun jam 06:00",
                            tipePengulangan = "Harian",
                            selesai = false,
                            createdAt = goal.createdAt
                        )
                    )
                }
                "Meditasi Pagi 10 Menit" -> {
                    tasks.add(
                        TaskEntity(
                            goalId = goal.goalId,
                            judulTugas = "Meditasi mindfulness 10 menit",
                            tipePengulangan = "Harian",
                            selesai = false,
                            createdAt = goal.createdAt
                        )
                    )
                }
                "Baca 1 Buku Self-Help" -> {
                    tasks.add(
                        TaskEntity(
                            goalId = goal.goalId,
                            judulTugas = "Baca 20 halaman per hari",
                            tipePengulangan = "Harian",
                            selesai = true,
                            waktuSelesai = currentTime - (5 * oneDayMillis),
                            createdAt = goal.createdAt
                        )
                    )
                }
            }
        }

        return tasks
    }

    /**
     * Generate 2-3 emergency logs
     */
    fun generateEmergencyLogs(userId: String): List<EmergencyLogEntity> {
        val logs = mutableListOf<EmergencyLogEntity>()

        logs.add(
            EmergencyLogEntity(
                userId = userId,
                jenisLatihan = "Pernapasan",
                selesai = true,
                feedback = "Lebih Baik",
                createdAt = currentTime - (7 * oneDayMillis)
            )
        )

        logs.add(
            EmergencyLogEntity(
                userId = userId,
                jenisLatihan = "Grounding",
                selesai = true,
                feedback = "Lebih Baik",
                createdAt = currentTime - (15 * oneDayMillis)
            )
        )

        return logs
    }

    /**
     * Generate sample screening result
     */
    fun generateSampleScreening(userId: String): ScreeningEntity {
        return ScreeningEntity(
            userId = userId,
            tekananAkademik = 3.5f,
            kepuasanStudi = 4.0f,
            durasiTidur = "7-8 jam",
            polaMakan = "Cukup",
            pikiranBunuhDiri = "Tidak Pernah",
            jamBelajar = 6,
            stresKeuangan = 3.0f,
            skorPrediksi = 0.15f,
            tingkatRisiko = "Rendah",
            rekomendasi = "Kondisi mental Anda terlihat baik. Tetap jaga pola hidup sehat dengan tidur cukup, olahraga rutin, dan kelola stres dengan baik.",
            tanggal = currentTime - (3 * oneDayMillis),
            createdAt = currentTime - (3 * oneDayMillis)
        )
    }

    /**
     * Helper function to select mood based on distribution
     */
    private fun selectWeightedMood(): String {
        val random = Random.nextFloat()
        var cumulative = 0.0

        moodDistribution.forEachIndexed { index, probability ->
            cumulative += probability
            if (random <= cumulative) {
                return moodLevels[index]
            }
        }
        return moodLevels[2] // Default to "Biasa"
    }

    /**
     * Calculate streak days for gamification
     */
    fun calculateStreak(dates: List<Long>): Int {
        if (dates.isEmpty()) return 0

        val sortedDates = dates.map { it / oneDayMillis }.sorted().distinct()
        val today = currentTime / oneDayMillis

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
}
