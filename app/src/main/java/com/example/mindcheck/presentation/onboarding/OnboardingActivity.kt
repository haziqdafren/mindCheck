package com.mindcheck.app.presentation.onboarding

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.mindcheck.app.MainActivity
import com.mindcheck.app.R
import com.mindcheck.app.databinding.ActivityOnboardingBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Onboarding Activity - First-time user experience
 * Shows 3 screens: Welcome, AI Detection, Privacy/Friend
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    private val dotViews = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("OnboardingActivity", "=== ONBOARDING ACTIVITY CREATED ===")

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Note: SharedPreferences access deferred to avoid StrictMode violation
        // Check if onboarding already completed
        // DEMO MODE: Always show onboarding for demonstration purposes

        Log.d("OnboardingActivity", "Setting up onboarding screens...")
        setupViewPager()
        setupPageIndicators()
        setupClickListeners()
    }

    private fun setupViewPager() {
        val adapter = OnboardingPagerAdapter(this)
        binding.viewPager.adapter = adapter

        // Enable user swipe for better UX
        binding.viewPager.isUserInputEnabled = true
        Log.d("OnboardingActivity", "ViewPager isUserInputEnabled set to: ${binding.viewPager.isUserInputEnabled}")

        // Page change listener
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                Log.d("OnboardingActivity", "Page changed to position: $position")
                updatePageIndicators(position)
            }
        })
    }

    private fun setupPageIndicators() {
        // Create 3 dot indicators
        for (i in 0 until 3) {
            val dot = View(this).apply {
                layoutParams = ViewGroup.MarginLayoutParams(24, 24).apply {
                    setMargins(8, 0, 8, 0)
                }
                background = resources.getDrawable(
                    R.drawable.bg_button_pill_primary,
                    null
                )
                alpha = if (i == 0) 1f else 0.3f
            }
            dotViews.add(dot)
            binding.pageIndicator.addView(dot)
        }
    }

    private fun updatePageIndicators(position: Int) {
        dotViews.forEachIndexed { index, dot ->
            dot.alpha = if (index == position) 1f else 0.3f
        }
    }

    private fun setupClickListeners() {
        binding.btnSkip.setOnClickListener {
            completeOnboarding()
        }
    }

    private fun completeOnboarding() {
        lifecycleScope.launch {
            // Mark onboarding as completed on IO thread
            withContext(Dispatchers.IO) {
                val prefs = getSharedPreferences("mindcheck_prefs", MODE_PRIVATE)
                val success: Boolean = prefs.edit().putBoolean("onboarding_completed", true).commit()
                Timber.d("Onboarding completion saved: $success")
            }

            // Navigate to main activity
            navigateToMain()
        }
    }

    private suspend fun navigateToMain() {
        // Mark onboarding as completed for current user (on IO thread)
        withContext(Dispatchers.IO) {
            val prefs = getSharedPreferences("mindcheck_prefs", MODE_PRIVATE)
            val userId: String? = prefs.getString("current_user_id", null)
            if (userId != null) {
                val success: Boolean = prefs.edit().putBoolean("onboarding_completed_$userId", true).commit()
                Timber.d("User onboarding completion saved for $userId: $success")
            }
        }

        // Navigate on main thread
        val intent = Intent(this, MainActivity::class.java)
        // Add flags to ensure smooth transition
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        // Use modern activity transition
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        finish()
    }

    /**
     * ViewPager Adapter for onboarding fragments
     */
    private class OnboardingPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> WelcomeFragment()
                1 -> DetectionFragment()
                2 -> PrivacyFragment()
                else -> WelcomeFragment()
            }
        }
    }

    /**
     * Public method for fragments to navigate to next page
     */
    fun nextPage() {
        val currentPage = binding.viewPager.currentItem
        if (currentPage < 2) {
            binding.viewPager.currentItem = currentPage + 1
        } else {
            completeOnboarding()
        }
    }

    /**
     * Public method for fragments to navigate to previous page
     */
    fun previousPage() {
        val currentPage = binding.viewPager.currentItem
        if (currentPage > 0) {
            binding.viewPager.currentItem = currentPage - 1
        }
    }
}
