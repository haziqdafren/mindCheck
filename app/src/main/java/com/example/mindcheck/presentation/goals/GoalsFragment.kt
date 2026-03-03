package com.mindcheck.app.presentation.goals

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.mindcheck.app.R
import com.mindcheck.app.data.local.database.AppDatabase
import com.mindcheck.app.data.local.entity.GoalEntity
import com.mindcheck.app.data.local.entity.TaskEntity
import com.mindcheck.app.data.mock.DatabaseInitializer
import com.mindcheck.app.data.firebase.FirestoreRepository
import com.mindcheck.app.databinding.FragmentGoalsBinding
import com.mindcheck.app.utils.UserSession
import com.mindcheck.app.utils.UpgradePrompt
import com.mindcheck.app.utils.CustomDialog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

// Fragment untuk mengelola fitur Target/Tujuan pengguna
class GoalsFragment : Fragment() {

    private var _binding: FragmentGoalsBinding? = null
    private val binding get() = _binding!!

    // Untuk sinkronisasi data target ke Firestore
    private val firestoreRepository = FirestoreRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        loadDashboard()
    }

    private fun setupClickListeners() {
        binding.btnAddGoal.setOnClickListener {
            showAddGoalDialog()
        }
    }

    private fun loadDashboard() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val userId = DatabaseInitializer.getCurrentUserId(requireContext()) ?: return@launch

                val goals = db.goalDao().getGoalsByUser(userId).first()
                val activeGoals = goals.filter { it.status == "Aktif" }

                // Calculate stats - OPTIMIZED: Single query for all tasks
                val goalIds = activeGoals.map { it.goalId }
                val allTasks = if (goalIds.isNotEmpty()) {
                    db.taskDao().getTasksByGoalIds(goalIds)
                } else {
                    emptyList()
                }

                val completedTasks = allTasks.filter { it.selesai }

                val completionRate = if (allTasks.isNotEmpty()) {
                    (completedTasks.size * 100 / allTasks.size)
                } else 0

                // Update stats
                binding.tvActiveCount.text = activeGoals.size.toString()
                binding.tvCompletionRate.text = "$completionRate%"

                // Group tasks by goalId for efficient access
                val tasksByGoalId = allTasks.groupBy { it.goalId }

                // Load today's tasks - pass pre-loaded tasks
                loadTodayTasks(activeGoals, tasksByGoalId)

                // Load all goals - pass pre-loaded tasks
                renderGoalProgress(activeGoals, tasksByGoalId)

            } catch (e: Exception) {
                Timber.e(e, "Error loading goals dashboard")
            }
        }
    }

    private suspend fun loadTodayTasks(activeGoals: List<GoalEntity>, tasksByGoalId: Map<String, List<TaskEntity>>) {
        binding.containerTodayTasks.removeAllViews()

        val todayTasks = mutableListOf<Pair<TaskEntity, GoalEntity>>()
        val today = Calendar.getInstance()
        val dayName = getDayName(today)

        // Collect today's tasks based on tipePengulangan - using pre-loaded tasks
        activeGoals.forEach { goal ->
            val tasks = tasksByGoalId[goal.goalId] ?: emptyList()
            tasks.forEach { task ->
                val shouldShowToday = when (task.tipePengulangan) {
                    "Harian" -> true
                    "Mingguan" -> {
                        // Check if today's day is in hariPengulangan
                        task.hariPengulangan.split(",").contains(dayName)
                    }
                    else -> false // "Khusus" tasks are not scheduled
                }

                if (shouldShowToday) {
                    todayTasks.add(task to goal)
                }
            }
        }

        if (todayTasks.isEmpty()) {
            binding.tvTodayEmpty.isVisible = true
            binding.containerStreak.isVisible = false
        } else {
            binding.tvTodayEmpty.isVisible = false

            // Render each today's task as checkbox
            todayTasks.forEach { (task, goal) ->
                val taskRow = createTaskCheckboxRow(task, goal)
                binding.containerTodayTasks.addView(taskRow)
            }

            // Show streak if applicable
            calculateAndShowStreak()
        }
    }

    private fun createTaskCheckboxRow(task: TaskEntity, goal: GoalEntity): View {
        val row = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8.dpToPx()
            }
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(4.dpToPx(), 8.dpToPx(), 4.dpToPx(), 8.dpToPx())
        }

        val checkbox = CheckBox(requireContext()).apply {
            isChecked = task.selesai
            buttonTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#B4D96C"))
            setOnCheckedChangeListener { _, isChecked ->
                toggleTaskCompletion(task, isChecked)
            }
        }

        val textLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
            setPadding(12.dpToPx(), 0, 0, 0)
        }

        val taskTitle = TextView(requireContext()).apply {
            text = task.judulTugas
            setTextColor(if (task.selesai) Color.parseColor("#80FFFFFF") else Color.WHITE)
            textSize = 15f
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
            if (task.selesai) {
                paintFlags = paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            }
        }

        val goalLabel = TextView(requireContext()).apply {
            text = "dari: ${goal.judul}"
            setTextColor(Color.parseColor("#80FFFFFF"))
            textSize = 12f
            setPadding(0, 2.dpToPx(), 0, 0)
        }

        textLayout.addView(taskTitle)
        textLayout.addView(goalLabel)

        row.addView(checkbox)
        row.addView(textLayout)

        return row
    }

    private fun toggleTaskCompletion(task: TaskEntity, isCompleted: Boolean) {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val updatedTask = task.copy(
                    selesai = isCompleted,
                    waktuSelesai = if (isCompleted) System.currentTimeMillis() else null
                )
                db.taskDao().updateTask(updatedTask)

                // Show motivational feedback
                if (isCompleted) {
                    val messages = listOf(
                        "Keren! Satu langkah lebih dekat! 💪",
                        "Hebat! Konsistensi adalah kunci! 🌟",
                        "Yeay! Terus semangat! 🎯",
                        "Mantap! Kamu luar biasa! ✨"
                    )
                    Toast.makeText(requireContext(), messages.random(), Toast.LENGTH_SHORT).show()
                }

                // Reload dashboard to update stats and streak
                loadDashboard()

            } catch (e: Exception) {
                Timber.e(e, "Error toggling task")
                Toast.makeText(requireContext(), "Gagal mengupdate tugas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun calculateAndShowStreak() {
        try {
            val db = AppDatabase.getDatabase(requireContext())
            val userId = DatabaseInitializer.getCurrentUserId(requireContext()) ?: return

            // Get all goals and tasks once - OPTIMIZED
            val goals = db.goalDao().getGoalsByUser(userId).first()
            val goalIds = goals.map { it.goalId }
            val allTasks = if (goalIds.isNotEmpty()) {
                db.taskDao().getTasksByGoalIds(goalIds)
            } else {
                emptyList()
            }

            // Simple streak: count consecutive days with completed tasks
            var streak = 0
            var checkDate = System.currentTimeMillis()

            // Go back day by day and check if tasks were completed
            for (i in 0..29) { // Check last 30 days max
                val dayStart = getDayStart(checkDate)
                val dayEnd = getDayEnd(checkDate)

                val completedOnDay = allTasks.any { task ->
                    task.waktuSelesai != null && task.waktuSelesai in dayStart..dayEnd
                }

                if (completedOnDay) {
                    streak++
                    checkDate -= 24 * 60 * 60 * 1000 // Go back 1 day
                } else {
                    break // Streak broken
                }
            }

            if (streak > 0) {
                binding.containerStreak.isVisible = true
                binding.tvStreak.text = "🔥 Streak $streak hari! Pertahankan!"
            } else {
                binding.containerStreak.isVisible = false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error calculating streak")
            binding.containerStreak.isVisible = false
        }
    }

    private fun getDayStart(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getDayEnd(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    private fun getDayName(calendar: Calendar): String {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "Minggu"
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jumat"
            Calendar.SATURDAY -> "Sabtu"
            else -> ""
        }
    }

    private fun showAddGoalDialog() {
        // Check guest mode restrictions
        if (!UserSession.canAccessFeature(requireContext(), UserSession.Feature.GOALS)) {
            UpgradePrompt.show(requireContext(), UserSession.Feature.GOALS)
            return
        }

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 0)
        }

        val infoText = TextView(requireContext()).apply {
            text = "Buat target wellness dan tambahkan tugas harian/mingguan untuk mencapainya!"
            setTextColor(requireContext().getColor(R.color.textWhiteSecondary))
            textSize = 13f
            setPadding(0, 0, 0, 16)
        }

        val titleInput = com.google.android.material.textfield.TextInputEditText(requireContext()).apply {
            hint = "Judul target (contoh: Olahraga Rutin)"
            setPadding(16, 16, 16, 16)
            setTextColor(Color.WHITE)
            setHintTextColor(requireContext().getColor(R.color.textWhiteTertiary))
        }

        val categorySpinner = Spinner(requireContext()).apply {
            setPadding(16, 24, 16, 16)
            adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                listOf("Olahraga", "Tidur", "Stres", "Sosial", "Hobi", "Akademik", "Kesehatan")
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        }

        val descInput = com.google.android.material.textfield.TextInputEditText(requireContext()).apply {
            hint = "Deskripsi (opsional)"
            setPadding(16, 24, 16, 16)
            maxLines = 3
            setTextColor(Color.WHITE)
            setHintTextColor(requireContext().getColor(R.color.textWhiteTertiary))
        }

        layout.addView(infoText)
        layout.addView(titleInput)
        layout.addView(categorySpinner)
        layout.addView(descInput)

        CustomDialog.build(requireContext()) {
            setTitle("Buat Target Baru")
            setCustomView(layout)
            setPositiveButton("Lanjut: Tambah Tugas") {
                val title = titleInput.text.toString()
                val category = categorySpinner.selectedItem.toString()
                val desc = descInput.text.toString()

                if (title.isNotBlank()) {
                    saveGoalAndShowTaskDialog(title, category, desc)
                } else {
                    Toast.makeText(requireContext(), "Judul tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }
            }
            setNegativeButton("Batal", null)
            setCancelable(true)
        }.show()
    }

    private fun saveGoalAndShowTaskDialog(title: String, category: String, description: String) {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val userId = DatabaseInitializer.getCurrentUserId(requireContext()) ?: return@launch

                val goal = GoalEntity(
                    userId = userId,
                    judul = title,
                    kategori = category,
                    deskripsi = description,
                    status = "Aktif",
                    tanggalMulai = System.currentTimeMillis(),
                    tanggalTarget = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
                )

                // Simpan target ke database lokal (Room)
                db.goalDao().insertGoal(goal)
                Timber.d("Goal saved to Room database: ${goal.goalId}")

                // Sinkronisasi ke Firestore untuk backup cloud
                try {
                    val goalData = mapOf(
                        "goalId" to goal.goalId,
                        "userId" to goal.userId,
                        "judul" to goal.judul,
                        "kategori" to goal.kategori,
                        "deskripsi" to goal.deskripsi,
                        "status" to goal.status,
                        "tanggalMulai" to goal.tanggalMulai,
                        "tanggalTarget" to goal.tanggalTarget,
                        "createdAt" to goal.createdAt,
                        "updatedAt" to goal.updatedAt
                    )

                    val result = firestoreRepository.saveDocument(
                        FirestoreRepository.COLLECTION_GOALS,
                        goal.goalId,
                        goalData
                    )

                    result.onSuccess {
                        Timber.d("✓ Goal synced to Firestore successfully: ${goal.goalId}")
                    }.onFailure { error ->
                        Timber.e(error, "✗ Failed to sync goal to Firestore (local data saved): ${goal.goalId}")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "✗ Firestore sync error (local data saved): ${goal.goalId}")
                }

                // Show task selection dialog
                showTaskCreationDialog(goal, category)

            } catch (e: Exception) {
                Timber.e(e, "Error saving goal")
                Toast.makeText(requireContext(), "Gagal menyimpan target", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showTaskCreationDialog(goal: GoalEntity, category: String) {
        // Check if fragment is still attached
        if (!isAdded || context == null) {
            Timber.w("Fragment not attached, skipping dialog")
            return
        }

        val options = arrayOf(
            "Pilih dari saran",
            "Buat tugas custom",
            "Lewati (tambah nanti)"
        )

        CustomDialog.build(requireContext()) {
            setTitle("Tambah Tugas untuk \"${goal.judul}\"")
            setMessage("Tugas membantu kamu tracking progress harian atau mingguan.")
            setItems(options) { which ->
                when (which) {
                    0 -> showSuggestedTasksDialog(goal, category)
                    1 -> showCustomTaskDialog(goal)
                    2 -> {
                        Toast.makeText(requireContext(), "Target berhasil dibuat! 🎯\nJangan lupa tambahkan tugas.", Toast.LENGTH_LONG).show()
                        loadDashboard()
                    }
                }
            }
            setNegativeButton("Tutup") {
                Toast.makeText(requireContext(), "Target berhasil dibuat! 🎯", Toast.LENGTH_SHORT).show()
                loadDashboard()
            }
            setCancelable(true)
        }.show()
    }

    private fun showSuggestedTasksDialog(goal: GoalEntity, category: String) {
        // Check if fragment is still attached
        if (!isAdded || context == null) {
            Timber.w("Fragment not attached, skipping dialog")
            return
        }

        val suggestedTasks = getSuggestedTasks(category)
        val selectedTasks = mutableListOf<String>()
        val checkedItems = BooleanArray(suggestedTasks.size) { false }

        var dialog: android.app.Dialog? = null

        dialog = CustomDialog.build(requireContext()) {
            setTitle("Pilih Tugas untuk ${goal.judul}")
            setMessage("Pilih tugas yang ingin kamu lakukan:")
            setMultiChoiceItems(suggestedTasks.toTypedArray(), checkedItems) { which, isChecked ->
                if (isChecked) {
                    selectedTasks.add(suggestedTasks[which])
                } else {
                    selectedTasks.remove(suggestedTasks[which])
                }

                // Update button text dynamically
                dialog?.findViewById<android.widget.Button>(R.id.btn_positive)?.text =
                    "Tambahkan (${selectedTasks.size})"
            }
            setPositiveButton("Tambahkan (0)") {
                if (selectedTasks.isNotEmpty()) {
                    showScheduleSelectionDialog(goal, selectedTasks)
                } else {
                    Toast.makeText(requireContext(), "Pilih minimal 1 tugas", Toast.LENGTH_SHORT).show()
                }
            }
            setNegativeButton("Batal") {
                Toast.makeText(requireContext(), "Target berhasil dibuat! 🎯", Toast.LENGTH_SHORT).show()
                loadDashboard()
            }
            setCancelable(true)
        }

        dialog.show()
    }

    private fun showScheduleSelectionDialog(goal: GoalEntity, taskNames: List<String>) {
        // Check if fragment is still attached
        if (!isAdded || context == null) {
            Timber.w("Fragment not attached, skipping dialog")
            return
        }

        val schedules = arrayOf("Harian", "Mingguan", "Khusus (tidak terjadwal)")

        CustomDialog.build(requireContext()) {
            setTitle("Jadwal Tugas")
            setMessage("Kapan kamu ingin mengerjakan tugas ini?")
            setItems(schedules) { which ->
                when (which) {
                    0 -> {
                        // Harian - save directly
                        saveMultipleTasks(goal, taskNames, "Harian", "")
                    }
                    1 -> {
                        // Mingguan - ask for days
                        showDaySelectionDialog(goal, taskNames)
                    }
                    2 -> {
                        // Khusus
                        saveMultipleTasks(goal, taskNames, "Khusus", "")
                    }
                }
            }
            setCancelable(true)
        }.show()
    }

    private fun showDaySelectionDialog(goal: GoalEntity, taskNames: List<String>) {
        // Check if fragment is still attached
        if (!isAdded || context == null) {
            Timber.w("Fragment not attached, skipping dialog")
            return
        }

        val days = arrayOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
        val selectedDays = mutableListOf<String>()
        val checkedDays = BooleanArray(days.size) { false }

        CustomDialog.build(requireContext()) {
            setTitle("Pilih Hari")
            setMessage("Hari apa saja tugas ini dikerjakan?")
            setMultiChoiceItems(days, checkedDays) { which, isChecked ->
                if (isChecked) {
                    selectedDays.add(days[which])
                } else {
                    selectedDays.remove(days[which])
                }
            }
            setPositiveButton("Simpan") {
                if (selectedDays.isNotEmpty()) {
                    val hariPengulangan = selectedDays.joinToString(",")
                    saveMultipleTasks(goal, taskNames, "Mingguan", hariPengulangan)
                } else {
                    Toast.makeText(requireContext(), "Pilih minimal 1 hari", Toast.LENGTH_SHORT).show()
                    showDaySelectionDialog(goal, taskNames)
                }
            }
            setNegativeButton("Batal", null)
            setCancelable(true)
        }.show()
    }

    private fun saveMultipleTasks(goal: GoalEntity, taskNames: List<String>, tipe: String, hariPengulangan: String) {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                taskNames.forEach { taskName ->
                    val task = TaskEntity(
                        goalId = goal.goalId,
                        judulTugas = taskName,
                        tipePengulangan = tipe,
                        hariPengulangan = hariPengulangan,
                        selesai = false,
                        createdAt = System.currentTimeMillis()
                    )
                    db.taskDao().insertTask(task)
                }

                val scheduleText = when (tipe) {
                    "Harian" -> "setiap hari"
                    "Mingguan" -> "setiap ${hariPengulangan.replace(",", ", ")}"
                    else -> "tanpa jadwal"
                }

                Toast.makeText(
                    requireContext(),
                    "✅ ${taskNames.size} tugas berhasil ditambahkan ($scheduleText)!\nSelamat bekerja menuju target! 💪",
                    Toast.LENGTH_LONG
                ).show()
                loadDashboard()

            } catch (e: Exception) {
                Timber.e(e, "Error saving tasks")
                Toast.makeText(requireContext(), "Gagal menyimpan tugas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCustomTaskDialog(goal: GoalEntity) {
        val input = EditText(requireContext()).apply {
            hint = "Nama tugas..."
            setPadding(48, 32, 48, 32)
            setTextColor(android.graphics.Color.WHITE)
            setHintTextColor(requireContext().getColor(R.color.textWhiteTertiary))
        }

        CustomDialog.build(requireContext()) {
            setTitle("Tugas Custom")
            setCustomView(input)
            setPositiveButton("Lanjut") {
                val taskName = input.text.toString().trim()
                if (taskName.isNotBlank()) {
                    showScheduleSelectionDialog(goal, listOf(taskName))
                } else {
                    Toast.makeText(requireContext(), "Nama tugas tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }
            }
            setNegativeButton("Batal", null)
            setCancelable(true)
        }.show()
    }

    private fun getSuggestedTasks(category: String): List<String> {
        return when (category) {
            "Olahraga" -> listOf(
                "Jalan kaki 30 menit",
                "Yoga atau stretching",
                "Olahraga kardio",
                "Latihan kekuatan"
            )
            "Tidur" -> listOf(
                "Tidur sebelum jam 11 malam",
                "Hindari layar 1 jam sebelum tidur",
                "Tidur 7-8 jam",
                "Bangun di waktu yang sama"
            )
            "Stres" -> listOf(
                "Meditasi 10 menit",
                "Journaling",
                "Latihan pernapasan",
                "Dengarkan musik relaksasi"
            )
            "Sosial" -> listOf(
                "Hubungi teman",
                "Quality time keluarga",
                "Berbagi cerita",
                "Join komunitas"
            )
            "Hobi" -> listOf(
                "Luangkan waktu hobi 1 jam",
                "Coba sesuatu yang baru",
                "Ikut kelas/workshop",
                "Dokumentasi progress"
            )
            "Akademik" -> listOf(
                "Belajar rutin",
                "Selesaikan tugas tepat waktu",
                "Belajar kelompok",
                "Review materi"
            )
            "Kesehatan" -> listOf(
                "Minum air 8 gelas",
                "Makan sayur dan buah",
                "Cek kesehatan rutin",
                "Jaga kebersihan"
            )
            else -> listOf(
                "Buat langkah konkret",
                "Tetapkan jadwal",
                "Catat progress"
            )
        }
    }

    private suspend fun renderGoalProgress(activeGoals: List<GoalEntity>, tasksByGoalId: Map<String, List<TaskEntity>>) {
        binding.goalProgressContainer.removeAllViews()

        if (activeGoals.isEmpty()) {
            binding.tvProgressEmpty.isVisible = true
            return
        }

        binding.tvProgressEmpty.isVisible = false

        val dateFormat = SimpleDateFormat("dd MMM", Locale("id", "ID"))

        activeGoals.forEach { goal ->
            val tasks = tasksByGoalId[goal.goalId] ?: emptyList()
            val completedTasks = tasks.count { it.selesai }
            val totalTasks = tasks.size
            val progressPercent = if (totalTasks > 0) (completedTasks * 100 / totalTasks) else 0

            val card = com.google.android.material.card.MaterialCardView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12.dpToPx()
                }
                setBackgroundResource(R.drawable.bg_card_glass_simple)
                setCardBackgroundColor(Color.TRANSPARENT)
                radius = 16.dpToPx().toFloat()
                cardElevation = 0f
                isClickable = true
                isFocusable = true
                foreground = android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#20FFFFFF")),
                    null,
                    android.graphics.drawable.ColorDrawable(Color.WHITE)
                )
                setOnClickListener {
                    showGoalOptionsDialog(goal)
                }
            }

            val cardContent = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_card_glass_simple)
                setPadding(20.dpToPx(), 16.dpToPx(), 20.dpToPx(), 16.dpToPx())
            }

            cardContent.addView(TextView(requireContext()).apply {
                text = goal.judul
                setTextColor(Color.parseColor("#B4D96C"))
                textSize = 16f
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            })

            cardContent.addView(TextView(requireContext()).apply {
                text = "${goal.kategori} • Target: ${dateFormat.format(Date(goal.tanggalTarget))}"
                setTextColor(Color.parseColor("#B3FFFFFF"))
                textSize = 13f
                setPadding(0, 4.dpToPx(), 0, 12.dpToPx())
            })

            cardContent.addView(ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100
                progress = progressPercent
                progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#B4D96C"))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    12.dpToPx()
                )
            })

            val progressText = when {
                progressPercent == 100 -> "🎉 Selesai! $completedTasks/$totalTasks tugas"
                progressPercent >= 75 -> "💪 Hampir selesai! $completedTasks/$totalTasks tugas ($progressPercent%)"
                progressPercent >= 50 -> "⚡ Setengah jalan! $completedTasks/$totalTasks tugas ($progressPercent%)"
                progressPercent > 0 -> "🌱 $completedTasks/$totalTasks tugas selesai ($progressPercent%)"
                totalTasks > 0 -> "📋 $totalTasks tugas menunggu"
                else -> "➕ Tap untuk tambah tugas"
            }

            cardContent.addView(TextView(requireContext()).apply {
                text = progressText
                setTextColor(if (progressPercent == 100) Color.parseColor("#B4D96C") else Color.parseColor("#B3FFFFFF"))
                textSize = 13f
                typeface = if (progressPercent == 100) {
                    android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                } else typeface
                setPadding(0, 8.dpToPx(), 0, 0)
            })

            card.addView(cardContent)
            binding.goalProgressContainer.addView(card)
        }
    }

    private fun showGoalOptionsDialog(goal: GoalEntity) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val tasks = db.taskDao().getTasksByGoalId(goal.goalId).first()

            val options = mutableListOf<String>()
            if (tasks.isNotEmpty()) {
                options.add("📋 Lihat Semua Tugas (${tasks.size})")
            }
            options.add("➕ Tambah Tugas Baru")
            options.add("✅ Tandai Target Selesai")
            options.add("🗑️ Hapus Target")

            CustomDialog.build(requireContext()) {
                setTitle(goal.judul)
                setMessage("${goal.kategori}\n${goal.deskripsi}")
                setItems(options.toTypedArray()) { which ->
                    when (options[which]) {
                        options.find { it.startsWith("📋") } -> showAllTasksDialog(goal, tasks)
                        "➕ Tambah Tugas Baru" -> showCustomTaskDialog(goal)
                        "✅ Tandai Target Selesai" -> markGoalComplete(goal)
                        "🗑️ Hapus Target" -> confirmDeleteGoal(goal)
                    }
                }
                setNegativeButton("Tutup", null)
                setCancelable(true)
            }.show()
        }
    }

    private fun showAllTasksDialog(goal: GoalEntity, tasks: List<TaskEntity>) {
        val taskList = tasks.map { task ->
            val status = if (task.selesai) "✅" else "⭕"
            val schedule = when (task.tipePengulangan) {
                "Harian" -> "(Harian)"
                "Mingguan" -> "(${task.hariPengulangan})"
                else -> ""
            }
            "$status ${task.judulTugas} $schedule"
        }.toTypedArray()

        CustomDialog.build(requireContext()) {
            setTitle("Tugas: ${goal.judul}")
            setItems(taskList) { which ->
                val task = tasks[which]
                showTaskOptionsDialog(task)
            }
            setNegativeButton("Tutup", null)
            setCancelable(true)
        }.show()
    }

    private fun showTaskOptionsDialog(task: TaskEntity) {
        val options = arrayOf(
            if (task.selesai) "Tandai Belum Selesai" else "Tandai Selesai",
            "Hapus Tugas"
        )

        CustomDialog.build(requireContext()) {
            setTitle(task.judulTugas)
            setItems(options) { which ->
                when (which) {
                    0 -> toggleTaskCompletion(task, !task.selesai)
                    1 -> confirmDeleteTask(task)
                }
            }
            setNegativeButton("Batal", null)
            setCancelable(true)
        }.show()
    }

    private fun confirmDeleteTask(task: TaskEntity) {
        CustomDialog.build(requireContext()) {
            setTitle("Hapus Tugas?")
            setMessage("Yakin ingin menghapus \"${task.judulTugas}\"?")
            setPositiveButton("Hapus") {
                deleteTask(task)
            }
            setNegativeButton("Batal", null)
            setCancelable(true)
        }.show()
    }

    private fun deleteTask(task: TaskEntity) {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                db.taskDao().deleteTask(task)
                Toast.makeText(requireContext(), "Tugas dihapus", Toast.LENGTH_SHORT).show()
                loadDashboard()
            } catch (e: Exception) {
                Timber.e(e, "Error deleting task")
                Toast.makeText(requireContext(), "Gagal menghapus tugas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun markGoalComplete(goal: GoalEntity) {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val updatedGoal = goal.copy(
                    status = "Selesai",
                    updatedAt = System.currentTimeMillis()
                )

                // Update in Room (local database)
                db.goalDao().updateGoal(updatedGoal)
                Timber.d("Goal updated in Room database: ${updatedGoal.goalId}")

                // Sync to Firestore (cloud backup) - with error handling
                try {
                    val goalData = mapOf(
                        "goalId" to updatedGoal.goalId,
                        "userId" to updatedGoal.userId,
                        "judul" to updatedGoal.judul,
                        "kategori" to updatedGoal.kategori,
                        "deskripsi" to updatedGoal.deskripsi,
                        "status" to updatedGoal.status,
                        "tanggalMulai" to updatedGoal.tanggalMulai,
                        "tanggalTarget" to updatedGoal.tanggalTarget,
                        "createdAt" to updatedGoal.createdAt,
                        "updatedAt" to updatedGoal.updatedAt
                    )

                    val result = firestoreRepository.saveDocument(
                        FirestoreRepository.COLLECTION_GOALS,
                        updatedGoal.goalId,
                        goalData
                    )

                    result.onSuccess {
                        Timber.d("✓ Goal update synced to Firestore: ${updatedGoal.goalId}")
                    }.onFailure { error ->
                        Timber.e(error, "✗ Failed to sync goal update (local data saved): ${updatedGoal.goalId}")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "✗ Firestore sync error (local data saved): ${updatedGoal.goalId}")
                }

                Toast.makeText(requireContext(), "🎉 Selamat! Target berhasil diselesaikan!", Toast.LENGTH_LONG).show()
                loadDashboard()

            } catch (e: Exception) {
                Timber.e(e, "Error completing goal")
                Toast.makeText(requireContext(), "Gagal menyelesaikan target", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDeleteGoal(goal: GoalEntity) {
        CustomDialog.build(requireContext()) {
            setTitle("Hapus Target?")
            setMessage("Yakin ingin menghapus \"${goal.judul}\"?\nSemua tugas terkait juga akan dihapus.")
            setPositiveButton("Hapus") {
                deleteGoal(goal)
            }
            setNegativeButton("Batal", null)
            setCancelable(true)
        }.show()
    }

    private fun deleteGoal(goal: GoalEntity) {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())

                // Delete from Room (local database)
                db.goalDao().deleteGoal(goal)
                Timber.d("Goal deleted from Room database: ${goal.goalId}")

                // Delete from Firestore (cloud backup) - with error handling
                try {
                    val result = firestoreRepository.deleteDocument(
                        FirestoreRepository.COLLECTION_GOALS,
                        goal.goalId
                    )

                    result.onSuccess {
                        Timber.d("✓ Goal deletion synced to Firestore: ${goal.goalId}")
                    }.onFailure { error ->
                        Timber.e(error, "✗ Failed to delete from Firestore (local data deleted): ${goal.goalId}")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "✗ Firestore deletion error (local data deleted): ${goal.goalId}")
                }

                Toast.makeText(requireContext(), "Target dihapus", Toast.LENGTH_SHORT).show()
                loadDashboard()

            } catch (e: Exception) {
                Timber.e(e, "Error deleting goal")
                Toast.makeText(requireContext(), "Gagal menghapus target", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
