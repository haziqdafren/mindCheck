package com.mindcheck.app.presentation.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mindcheck.app.databinding.FragmentPrivacyBinding

/**
 * Privacy Fragment - Third onboarding screen
 * Explains app is a supportive friend
 */
class PrivacyFragment : Fragment() {

    private var _binding: FragmentPrivacyBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPrivacyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            (activity as? OnboardingActivity)?.previousPage()
        }

        // Start button (completes onboarding)
        binding.btnStart.setOnClickListener {
            (activity as? OnboardingActivity)?.nextPage() // This will complete onboarding
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
