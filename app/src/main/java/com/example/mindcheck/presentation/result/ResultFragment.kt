package com.mindcheck.app.presentation.result

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
// Removed unused navigation import
import com.mindcheck.app.R
import com.mindcheck.app.data.remote.dto.PredictionResponse
import com.mindcheck.app.data.remote.RetrofitClient
import com.mindcheck.app.databinding.FragmentResultBinding
import com.mindcheck.app.presentation.screening.ScreeningViewModel
import com.mindcheck.app.presentation.screening.ScreeningViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment for displaying screening results and personalized advice
 */
class ResultFragment : Fragment() {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    // Shared ViewModel to get prediction result
    private val screeningViewModel: ScreeningViewModel by activityViewModels {
        ScreeningViewModelFactory(RetrofitClient.apiService, requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("ResultFragment", "onViewCreated called")
        setupObservers()
        setupClickListeners()
        Log.d("ResultFragment", "onViewCreated finished")
    }

    private fun setupObservers() {
        // Get the current prediction result - it should already be set before navigation
        val currentResult = screeningViewModel.predictionResult.value
        Log.d("ResultFragment", "setupObservers - Current result value: $currentResult")

        if (currentResult != null) {
            Log.d("ResultFragment", "Displaying result immediately")
            displayResult(currentResult)
        } else {
            Log.e("ResultFragment", "ERROR: No prediction result available!")
            // This should not happen - navigation should only occur after result is set
            // Fallback: observe for changes (in case of race condition)
            viewLifecycleOwner.lifecycleScope.launch {
                screeningViewModel.predictionResult
                    .collect { result ->
                        Log.d("ResultFragment", "Fallback collect received: $result")
                        if (result != null) {
                            Log.d("ResultFragment", "Displaying result from fallback")
                            displayResult(result)
                            return@collect // Stop collecting after first non-null result
                        }
                    }
            }
        }
    }

    private fun displayResult(result: PredictionResponse) {
        Log.d("ResultFragment", "========== displayResult START ==========")
        Log.d("ResultFragment", "displayResult called with: prediction=${result.prediction}, confidence=${result.confidence}, risk=${result.riskLevel}, advice count=${result.advice.size}")

        // Display result based on prediction (0 = No Depression, 1 = Depression)
        Log.d("ResultFragment", "Checking prediction value: ${result.prediction}")
        when (result.prediction) {
            0 -> {
                Log.d("ResultFragment", "Calling displayNoDepressionResult")
                displayNoDepressionResult(result)
            }
            1 -> {
                Log.d("ResultFragment", "Calling displayDepressionDetectedResult")
                displayDepressionDetectedResult(result)
            }
            else -> {
                Log.d("ResultFragment", "Calling displayDepressionDetectedResult (default)")
                displayDepressionDetectedResult(result)
            }
        }

        // Display confidence
        val confidencePercentage = "${(result.confidence * 100).toInt()}%"
        Log.d("ResultFragment", "Setting confidence text to: $confidencePercentage")
        binding.tvConfidence.text = confidencePercentage
        Log.d("ResultFragment", "Confidence text after set: ${binding.tvConfidence.text}")

        // Display risk level with color
        Log.d("ResultFragment", "Setting risk level text to: ${result.riskLevel}")
        binding.tvRiskLevel.text = result.riskLevel
        binding.tvRiskLevel.setTextColor(getRiskLevelColor(result.riskLevel))
        Log.d("ResultFragment", "Risk level text after set: ${binding.tvRiskLevel.text}")

        // Display advice (personalized from Flask API)
        Log.d("ResultFragment", "Calling displayAdvice with ${result.advice.size} items")
        displayAdvice(result.advice)
        Log.d("ResultFragment", "========== displayResult END ==========")
    }

    /**
     * Display result for no depression detected
     */
    private fun displayNoDepressionResult(result: PredictionResponse) {
        binding.ivResultIcon.setImageResource(R.drawable.ic_success_sparkle)
        binding.ivResultIcon.setColorFilter(Color.WHITE)
        binding.tvResultTitle.text = "Kondisi Mental\nKamu Baik"
    }

    /**
     * Display result for depression risk detected
     */
    private fun displayDepressionDetectedResult(result: PredictionResponse) {
        // Choose icon based on risk level
        val iconRes = when (result.riskLevel) {
            "Tinggi" -> R.drawable.ic_alert_high
            "Sedang" -> R.drawable.ic_alert_medium
            else -> R.drawable.ic_alert_low
        }
        binding.ivResultIcon.setImageResource(iconRes)
        // Alert icons have their own colors, so don't apply tint
        binding.ivResultIcon.clearColorFilter()

        binding.tvResultTitle.text = "Risiko Depresi\nTerdeteksi"
    }

    /**
     * Get color for depression detection status
     */
    private fun getRiskLevelColor(riskLevel: String): Int {
        return when (riskLevel) {
            "Tidak" -> Color.parseColor("#B4D96C")  // Green - No depression
            "Ya" -> Color.parseColor("#E76F51")  // Warm red-orange - Depression detected
            else -> Color.WHITE
        }
    }

    /**
     * Display advice items dynamically
     */
    private fun displayAdvice(adviceList: List<String>) {
        Log.d("ResultFragment", "displayAdvice called with ${adviceList.size} items")
        binding.adviceContainer.removeAllViews()

        adviceList.forEachIndexed { index, advice ->
            Log.d("ResultFragment", "Adding advice #${index + 1}: $advice")
            val adviceCard = layoutInflater.inflate(
                R.layout.item_advice_card,
                binding.adviceContainer,
                false
            )

            val tvAdviceText = adviceCard.findViewById<TextView>(R.id.tvAdviceText)
            tvAdviceText.text = advice

            binding.adviceContainer.addView(adviceCard)
        }
        Log.d("ResultFragment", "Finished adding all advice cards. Container child count: ${binding.adviceContainer.childCount}")
    }

    /**
     * Setup button click listeners
     */
    private fun setupClickListeners() {
        // Back to home button
        binding.btnHome.setOnClickListener {
            // Navigate back to home fragment (pop entire back stack)
            parentFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }

        // Retake screening button
        binding.btnRetake.setOnClickListener {
            // Reset the screening in ViewModel
            screeningViewModel.resetScreening()

            // Navigate back to screening start
            val screeningFragment = com.mindcheck.app.presentation.screening.ScreeningFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, screeningFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
