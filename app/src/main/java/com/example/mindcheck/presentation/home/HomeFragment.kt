package com.mindcheck.app.presentation.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.async
import com.mindcheck.app.R
import com.mindcheck.app.data.local.database.AppDatabase
import com.mindcheck.app.data.local.entity.SleepLogEntity
import com.mindcheck.app.data.mock.DatabaseInitializer
import com.mindcheck.app.data.firebase.FirestoreRepository
import com.mindcheck.app.databinding.FragmentHomeBinding
import com.mindcheck.app.presentation.screening.ScreeningFragment
import com.mindcheck.app.presentation.analytics.AnalyticsActivity
import com.mindcheck.app.presentation.breathing.BreathingActivity
import com.mindcheck.app.presentation.emergency.EmergencyBottomSheet
import com.mindcheck.app.utils.CustomDialog
import android.widget.EditText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

// Fragment halaman beranda dengan ringkasan aktivitas dan akses cepat ke fitur
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Untuk sinkronisasi data tidur ke Firestore
    private val firestoreRepository = FirestoreRepository()

    private var skeletonView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Tampilkan skeleton loading
        showLoading()

        setupClickListeners()

        // Muat data menggunakan viewLifecycleOwner.lifecycleScope
        // Otomatis dibatalkan jika view dihancurkan
        viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.delay(50) // Delay kecil untuk render UI
            loadHomeData()
        }
    }

    private fun showLoading() {
        // Inflate skeleton jika belum di-inflate
        if (skeletonView == null && binding.skeletonLoader.parent != null) {
            skeletonView = binding.skeletonLoader.inflate()
        }

        skeletonView?.visibility = View.VISIBLE
        binding.scrollContent.alpha = 0f
    }

    private fun hideLoading() {
        // Cek apakah fragment masih terpasang dan binding tidak null
        if (!isAdded || _binding == null) {
            Timber.w("Fragment not attached or binding null, skipping hideLoading")
            return
        }

        // Transisi fade yang mulus
        binding.scrollContent.animate()
            .alpha(1f)
            .setDuration(300)
            .start()

        skeletonView?.animate()
            ?.alpha(0f)
            ?.setDuration(200)
            ?.withEndAction {
                if (isAdded && _binding != null) {
                    skeletonView?.visibility = View.GONE
                }
            }
            ?.start()
    }

    private fun loadHomeData() {
        // Gunakan viewLifecycleOwner.lifecycleScope daripada lifecycleScope
        // Memastikan coroutine dibatalkan saat view dihancurkan
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Cek apakah fragment masih terpasang
                if (!isAdded) {
                    Timber.w("Fragment not attached, cancelling data load")
                    return@launch
                }

                val db = AppDatabase.getDatabase(requireContext())

                // Baca SharedPreferences di thread IO untuk menghindari pelanggaran StrictMode
                val (isGuestMode, userId) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val prefs = requireContext().getSharedPreferences("mindcheck_prefs", android.content.Context.MODE_PRIVATE)
                    val guest = prefs.getBoolean("is_guest_mode", false)
                    val id = DatabaseInitializer.getCurrentUserId(requireContext())
                    Pair(guest, id)
                }

                if (userId == null) return@launch

                // Muat semua data secara paralel dengan async/await untuk performa lebih baik
                val userDeferred = async {
                    if (isGuestMode) null else db.userDao().getUserById(userId)
                }
                val screeningDeferred = async { db.screeningDao().getLatestScreening(userId) }
                val moodLogsDeferred = async { db.moodLogDao().getRecentMoodLogs(userId, 30) }
                val sleepLogsDeferred = async { db.sleepLogDao().getRecentSleepLogs(userId, 30) }
                val gratitudeDatesDeferred = async { db.gratitudeEntryDao().getRecentGratitudeDates(userId, 60) }

                // Hitung jumlah secara paralel (lebih cepat dari memuat seluruh list)
                val moodCountDeferred = async { db.moodLogDao().getMoodLogCount(userId) }
                val sleepCountDeferred = async { db.sleepLogDao().getSleepLogCount(userId) }
                val goalCountDeferred = async { db.goalDao().getGoalCount(userId) }

                // Tunggu semua hasil
                val user = userDeferred.await()
                val screening = screeningDeferred.await()
                val moodLogs = moodLogsDeferred.await()
                val sleepLogs = sleepLogsDeferred.await()
                val gratitudeDates = gratitudeDatesDeferred.await()

                val moodCount = moodCountDeferred.await()
                val sleepCount = sleepCountDeferred.await()
                val goalCount = goalCountDeferred.await()

                // Cek lagi sebelum update UI
                if (!isAdded || _binding == null) {
                    Timber.w("Fragment destroyed during data load, cancelling UI update")
                    return@launch
                }

                // Update tampilan UI
                binding.tvGreeting.text = "Hai, ${user?.nama ?: "Tamu"}!"

                // Muat screening terakhir
                if (screening != null) {
                    binding.cardLastScreening.visibility = View.VISIBLE
                    val daysAgo = ((System.currentTimeMillis() - screening.tanggal) / (24 * 60 * 60 * 1000)).toInt()
                    binding.tvScreeningTime.text = "$daysAgo hari lalu"
                    binding.tvRiskLevel.text = screening.tingkatRisiko

                    // Set warna berdasarkan risiko
                    val color = when (screening.tingkatRisiko) {
                        "Rendah" -> requireContext().getColor(R.color.successGreen)
                        "Sedang" -> requireContext().getColor(R.color.warningOrange)
                        "Tinggi" -> requireContext().getColor(R.color.errorRed)
                        else -> requireContext().getColor(R.color.textSecondary)
                    }
                    binding.tvRiskLevel.setTextColor(color)
                } else {
                    binding.cardLastScreening.visibility = View.GONE
                }

                // Muat mood hari ini dengan status penyelesaian
                val today = System.currentTimeMillis()
                val todayMood = moodLogs.firstOrNull {
                    (it.tanggal / (24 * 60 * 60 * 1000)) == (today / (24 * 60 * 60 * 1000))
                }

                if (todayMood != null) {
                    binding.tvMoodLabel.text = "✓ ${todayMood.tingkatMood}"
                    binding.tvMoodLabel.setTextColor(requireContext().getColor(R.color.mintGreen))
                    binding.ivMoodIcon.setColorFilter(requireContext().getColor(R.color.mintGreen))
                    binding.tvMoodStatus.text = "Sudah dicatat"
                    binding.tvMoodStatus.visibility = View.VISIBLE

                    // Tampilkan kartu perayaan
                    binding.cardCompletionCelebration.visibility = View.VISIBLE
                    binding.tvCompletionMessage.text = "🌟 Hebat! Mood hari ini sudah dicatat!"
                } else {
                    binding.tvMoodLabel.text = "Catat Mood"
                    binding.tvMoodLabel.setTextColor(android.graphics.Color.parseColor("#B3FFFFFF"))
                    binding.ivMoodIcon.setColorFilter(android.graphics.Color.parseColor("#FFB74D"))
                    binding.tvMoodStatus.text = "Belum dicatat"
                    binding.tvMoodStatus.visibility = View.VISIBLE
                    binding.cardCompletionCelebration.visibility = View.GONE
                }

                // Muat data tidur hari ini
                val lastSleep = sleepLogs.firstOrNull()
                if (lastSleep != null) {
                    binding.tvSleepHours.text = String.format("%.1f jam", lastSleep.durasiJam)
                } else {
                    binding.tvSleepHours.text = "Belum dicatat"
                }

                // Hitung streak (hanya menggunakan tanggal untuk efisiensi)
                val streak = calculateStreak(gratitudeDates)
                if (streak > 0) {
                    binding.tvStreak.text = "Streak: $streak hari berturut-turut!"
                } else {
                    binding.tvStreak.text = "Mulai streak kamu hari ini!"
                }

                Timber.d("Home data loaded: $moodCount moods, $sleepCount sleep, $goalCount goals")

                // Sembunyikan skeleton loading dengan transisi mulus
                hideLoading()

            } catch (e: Exception) {
                Timber.e(e, "Error loading home data")
                // Tetap sembunyikan loading meskipun ada error
                hideLoading()
            }
        }
    }

    private fun setupClickListeners() {
        // Kartu AI Screening
        binding.btnStartScreening.setOnClickListener {
            // Navigasi ke screening
            val screeningFragment = ScreeningFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, screeningFragment)
                .addToBackStack(null)
                .commit()
        }

        // Kartu screening terakhir - lihat riwayat
        binding.cardLastScreening.setOnClickListener {
            val intent = Intent(requireContext(), com.mindcheck.app.presentation.screening.ScreeningHistoryActivity::class.java)
            startActivity(intent)
        }

        // Kartu mood - pindah ke tab Mood
        binding.cardMood.setOnClickListener {
            requireActivity().findViewById<android.widget.ImageButton>(R.id.btn_nav_mood)?.performClick()
        }

        // Kartu tidur - catat data tidur
        binding.cardSleep.setOnClickListener {
            showSleepLogDialog()
        }

        // Kartu darurat - tampilkan bottom sheet darurat
        binding.cardEmergency.setOnClickListener {
            val emergencySheet = EmergencyBottomSheet()
            emergencySheet.show(parentFragmentManager, "EmergencyBottomSheet")
        }

        // Kartu latihan pernapasan
        binding.cardBreathing.setOnClickListener {
            val intent = Intent(requireContext(), BreathingActivity::class.java)
            startActivity(intent)
        }

        // Lihat detail screening terakhir - navigasi ke riwayat screening
        binding.btnViewDetail.setOnClickListener {
            val intent = Intent(requireContext(), com.mindcheck.app.presentation.screening.ScreeningHistoryActivity::class.java)
            startActivity(intent)
        }

        // Mulai screening baru
        binding.btnScreenAgain.setOnClickListener {
            binding.btnStartScreening.performClick()
        }
    }

    private fun calculateStreak(dates: List<Long>): Int {
        if (dates.isEmpty()) return 0

        val oneDayMillis = 24 * 60 * 60 * 1000L
        val sortedDates = dates.map { it / oneDayMillis }.sorted().distinct()
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

    private fun showSleepLogDialog() {
        val input = EditText(requireContext())
        input.hint = "Jam tidur (contoh: 7.5)"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.setPadding(50, 30, 50, 30)

        CustomDialog.build(requireContext()) {
            setTitle("Catat Durasi Tidur")
            setMessage("Berapa jam kamu tidur tadi malam?")
            setCustomView(input)
            setPositiveButton("Simpan") {
                val hoursText = input.text.toString()
                if (hoursText.isNotBlank()) {
                    val hours = hoursText.toDoubleOrNull()
                    if (hours != null && hours > 0 && hours <= 24) {
                        saveSleepLog(hours)
                    } else {
                        android.widget.Toast.makeText(requireContext(), "Durasi tidak valid (0-24 jam)", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
            setNegativeButton("Batal", null)
            setCancelable(true)
        }.show()
    }

    private fun saveSleepLog(hours: Double) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val userId = DatabaseInitializer.getCurrentUserId(requireContext()) ?: return@launch

                val kategori = when {
                    hours < 5 -> "Kurang dari 5 jam"
                    hours < 7 -> "5-6 jam"
                    hours <= 8 -> "7-8 jam"
                    else -> "Lebih dari 8 jam"
                }

                val kualitas = when {
                    hours >= 7 -> 5 // Excellent
                    hours >= 6 -> 4 // Good
                    hours >= 5 -> 3 // Fair
                    else -> 2 // Poor
                }

                val sleepLog = SleepLogEntity(
                    userId = userId,
                    waktuTidur = "22:00", // Default sleep time
                    waktuBangun = "06:00", // Default wake time
                    durasiJam = hours.toFloat(),
                    kategoriDurasi = kategori,
                    kualitasTidur = kualitas,
                    tanggal = System.currentTimeMillis()
                )

                // Simpan ke Room (database lokal)
                db.sleepLogDao().insertSleepLog(sleepLog)
                Timber.d("Sleep log saved to Room: ${sleepLog.sleepId}")

                // Sinkronisasi ke Firestore (backup cloud)
                try {
                    val sleepData = mapOf(
                        "sleepId" to sleepLog.sleepId,
                        "userId" to sleepLog.userId,
                        "waktuTidur" to sleepLog.waktuTidur,
                        "waktuBangun" to sleepLog.waktuBangun,
                        "durasiJam" to sleepLog.durasiJam,
                        "kategoriDurasi" to sleepLog.kategoriDurasi,
                        "kualitasTidur" to sleepLog.kualitasTidur,
                        "tanggal" to sleepLog.tanggal,
                        "createdAt" to sleepLog.createdAt
                    )

                    val result = firestoreRepository.saveDocument(
                        FirestoreRepository.COLLECTION_SLEEP_LOGS,
                        sleepLog.sleepId,
                        sleepData
                    )

                    result.onSuccess {
                        Timber.d("✓ Sleep log synced to Firestore: ${sleepLog.sleepId}")
                    }.onFailure { error ->
                        Timber.e(error, "✗ Failed to sync sleep log (local data saved): ${sleepLog.sleepId}")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "✗ Firestore sync error (local data saved): ${sleepLog.sleepId}")
                }

                android.widget.Toast.makeText(requireContext(), "✓ Tidur $hours jam berhasil dicatat!", android.widget.Toast.LENGTH_SHORT).show()

                // Reload dashboard untuk tampilkan data terbaru
                loadHomeData()

            } catch (e: Exception) {
                Timber.e(e, "Error saving sleep log")
                android.widget.Toast.makeText(requireContext(), "Gagal menyimpan data tidur", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload data saat kembali ke fragment ini
        loadHomeData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
