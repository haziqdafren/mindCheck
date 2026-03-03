package com.mindcheck.app.presentation.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mindcheck.app.databinding.FragmentWelcomeBinding

/**
 * Welcome Fragment - First onboarding screen
 */
class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Primary button - Next
        binding.btnPrimary.setOnClickListener {
            (activity as? OnboardingActivity)?.nextPage()
        }

        // Secondary button - Learn more (also goes to next for now)
        binding.btnSecondary.setOnClickListener {
            (activity as? OnboardingActivity)?.nextPage()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
