package com.mindcheck.app.presentation.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.mindcheck.app.data.local.database.AppDatabase
import com.mindcheck.app.data.mock.DatabaseInitializer
import com.mindcheck.app.databinding.FragmentProfileBinding
import com.mindcheck.app.presentation.analytics.AnalyticsActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Profile Fragment - Phase 2
 * User info, analytics, settings
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadProfileData()
        setupClickListeners()
    }

    private fun loadProfileData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Check if fragment is still attached
                if (!isAdded) return@launch

                val db = AppDatabase.getDatabase(requireContext())
                val userId = DatabaseInitializer.getCurrentUserId(requireContext()) ?: return@launch

                val user = db.userDao().getUserById(userId)

                // Check again before updating UI
                if (!isAdded || _binding == null) return@launch

                binding.tvUserName.text = user?.nama ?: "Mahasiswa"
                binding.tvUserEmail.text = user?.email ?: "mahasiswa@example.com"

                // Load stats using COUNT queries instead of loading all data
                // This is MUCH faster - only reads count, not entire tables
                val moodCount = db.moodLogDao().getMoodLogCount(userId)
                val sleepCount = db.sleepLogDao().getSleepLogCount(userId)
                val journalCount = db.gratitudeEntryDao().getGratitudeCount(userId)
                val goalCount = db.goalDao().getGoalCount(userId)
                val screeningCount = db.screeningDao().getScreeningCount(userId)

                // Check again before updating UI
                if (!isAdded || _binding == null) return@launch

                binding.tvMoodCount.text = "$moodCount"
                binding.tvSleepCount.text = "$sleepCount"
                binding.tvJournalCount.text = "$journalCount"
                binding.tvGoalCount.text = "$goalCount"
                binding.tvScreeningCount.text = "$screeningCount"

                Timber.d("Profile loaded (optimized - counts only)")

            } catch (e: Exception) {
                Timber.e(e, "Error loading profile")
            }
        }
    }

    private fun setupClickListeners() {
        binding.cardAnalytics.setOnClickListener {
            val intent = Intent(requireContext(), AnalyticsActivity::class.java)
            startActivity(intent)
        }

        binding.cardSettings.setOnClickListener {
            val intent = Intent(requireContext(), com.mindcheck.app.presentation.settings.SettingsActivity::class.java)
            startActivity(intent)
        }

        binding.cardAbout.setOnClickListener {
            val intent = Intent(requireContext(), com.mindcheck.app.presentation.about.AboutActivity::class.java)
            startActivity(intent)
        }

        binding.btnLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun performLogout() {
        // Show confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Keluar")
            .setMessage("Apakah Anda yakin ingin keluar?")
            .setPositiveButton("Ya") { _, _ ->
                // Clear session
                val prefs = requireActivity().getSharedPreferences("mindcheck_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit()
                    .clear()
                    .commit()

                // Navigate to login
                val intent = Intent(requireContext(), com.mindcheck.app.presentation.auth.LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()

                Timber.d("User logged out successfully")
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadProfileData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
