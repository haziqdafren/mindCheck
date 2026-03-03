package com.mindcheck.app

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.mindcheck.app.databinding.ActivityMainBinding
import com.mindcheck.app.presentation.home.HomeFragment
import com.mindcheck.app.presentation.mood.MoodFragment
import com.mindcheck.app.presentation.journal.JournalFragment
import com.mindcheck.app.presentation.goals.GoalsFragment
import com.mindcheck.app.presentation.profile.ProfileFragment
import com.mindcheck.app.presentation.emergency.EmergencyBottomSheet
import com.mindcheck.app.presentation.screening.ScreeningFragment
import com.mindcheck.app.presentation.result.ResultFragment
import timber.log.Timber

// Activity utama dengan 5 fragment: Home, Mood, Journal, Goals, Profile
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("=== MAIN ACTIVITY CREATED (Phase 2 - Optimized) ===")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Sembunyikan action bar untuk pengalaman layar penuh
        supportActionBar?.hide()

        setupBottomNavigation()
        setupFAB()
        setupFragmentListener()

        // Muat fragment home secara default dengan delay untuk render UI lebih lancar
        if (savedInstanceState == null) {
            binding.root.postDelayed({
                loadFragment(HomeFragment())
                setActiveNavButton(binding.btnNavHome)
            }, 50) // Delay 50ms untuk startup yang lebih mulus
        }
    }

    private fun setupFragmentListener() {
        supportFragmentManager.addOnBackStackChangedListener {
            updateNavbarVisibility()
        }
    }

    private fun updateNavbarVisibility() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        // Sembunyikan navbar dan FAB untuk fragment tertentu
        val shouldHideNav = currentFragment is ScreeningFragment || currentFragment is ResultFragment

        binding.bottomNavigation.isVisible = !shouldHideNav
        binding.fabEmergency.isVisible = !shouldHideNav
    }

    private fun setupBottomNavigation() {
        // Setup listener klik untuk setiap tombol navigasi
        binding.btnNavMood.setOnClickListener {
            loadFragment(MoodFragment())
            setActiveNavButton(binding.btnNavMood)
        }

        binding.btnNavJournal.setOnClickListener {
            loadFragment(JournalFragment())
            setActiveNavButton(binding.btnNavJournal)
        }

        binding.btnNavHome.setOnClickListener {
            loadFragment(HomeFragment())
            setActiveNavButton(binding.btnNavHome)
        }

        binding.btnNavGoals.setOnClickListener {
            loadFragment(GoalsFragment())
            setActiveNavButton(binding.btnNavGoals)
        }

        binding.btnNavProfile.setOnClickListener {
            loadFragment(ProfileFragment())
            setActiveNavButton(binding.btnNavProfile)
        }

        Timber.d("Custom navbar setup complete")
    }

    private fun setActiveNavButton(activeButton: ImageButton) {
        val accentColor = ContextCompat.getColorStateList(this, R.color.accentPrimary)
        val whiteColor = ContextCompat.getColorStateList(this, R.color.white)

        // Reset semua tombol ke warna putih
        ImageViewCompat.setImageTintList(binding.btnNavMood, whiteColor)
        ImageViewCompat.setImageTintList(binding.btnNavJournal, whiteColor)
        ImageViewCompat.setImageTintList(binding.btnNavHome, whiteColor)
        ImageViewCompat.setImageTintList(binding.btnNavGoals, whiteColor)
        ImageViewCompat.setImageTintList(binding.btnNavProfile, whiteColor)

        // Set tombol aktif ke warna aksen
        ImageViewCompat.setImageTintList(activeButton, accentColor)
    }

    private fun setupFAB() {
        binding.fabEmergency.setOnClickListener {
            // Tampilkan bottom sheet alat bantu darurat
            val bottomSheet = EmergencyBottomSheet()
            bottomSheet.show(supportFragmentManager, "EmergencyBottomSheet")
            Timber.d("Emergency FAB clicked - showing bottom sheet")
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()

        // Update visibilitas navbar setelah transaksi fragment
        binding.root.post {
            updateNavbarVisibility()
        }
    }
}
