package com.mindcheck.app.presentation.screening

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
// Removed unused navigation import
import com.mindcheck.app.R
import com.mindcheck.app.data.remote.RetrofitClient
import com.mindcheck.app.databinding.FragmentScreeningQuestionBinding
import com.mindcheck.app.domain.model.ScreeningQuestion
import com.google.android.material.slider.Slider
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment for displaying screening questions one at a time
 * Dynamically renders different input types based on question
 */
class ScreeningFragment : Fragment() {

    private var _binding: FragmentScreeningQuestionBinding? = null
    private val binding get() = _binding!!

    // ViewModel with factory
    private val viewModel: ScreeningViewModel by activityViewModels {
        ScreeningViewModelFactory(RetrofitClient.apiService, requireContext())
    }

    private var currentSelectedCard: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScreeningQuestionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupClickListeners()

        // Display first question
        renderQuestion()
    }

    private fun setupObservers() {
        // Observe current question changes
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentQuestionIndex.collect { // Use collect instead of collectLatest
                renderQuestion()
            }
        }

        // Observe answers changes (to update button states)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.answers.collect { // Use collect instead of collectLatest
                updateNavigationButtons()
                // Progress is already updated in ViewModel, just update UI
                binding.progressBar.progress = (viewModel.progress.value * 100).toInt()
                binding.tvProgressPercentage.text = "${(viewModel.progress.value * 100).toInt()}%"
            }
        }

        // Observe loading state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.btnNext.isEnabled = !isLoading && viewModel.isCurrentQuestionAnswered()
                binding.btnNext.text = if (isLoading) "Memproses..." else getNextButtonText()
            }
        }

        // Observe prediction result
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.predictionResult.collect { result ->
                Log.d("ScreeningFragment", "Prediction result collected: $result")
                if (result != null) {
                    Log.d("ScreeningFragment", "Non-null result received")
                    Log.d("ScreeningFragment", "Result details - prediction: ${result.prediction}, confidence: ${result.confidence}, advice count: ${result.advice.size}")

                    // Small delay to ensure StateFlow value is fully propagated
                    kotlinx.coroutines.delay(100)

                    Log.d("ScreeningFragment", "Navigating to result screen now")
                    navigateToResult()
                } else {
                    Log.d("ScreeningFragment", "Null result received (initial state), continuing to wait")
                }
            }
        }

        // Observe errors
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                if (error != null) {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            if (viewModel.isFirstQuestion()) {
                // Go back to home fragment
                parentFragmentManager.popBackStack()
            } else {
                viewModel.previousQuestion()
            }
        }

        binding.btnNext.setOnClickListener {
            Log.d("ScreeningFragment", "Next button clicked. Is answered: ${viewModel.isCurrentQuestionAnswered()}")

            if (viewModel.isLastQuestion()) {
                // Submit screening
                if (viewModel.isCurrentQuestionAnswered()) {
                    viewModel.submitScreening()
                } else {
                    Toast.makeText(requireContext(), "Mohon jawab pertanyaan ini terlebih dahulu", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Move to next question
                if (viewModel.isCurrentQuestionAnswered()) {
                    viewModel.nextQuestion()
                } else {
                    Toast.makeText(requireContext(), "Mohon jawab pertanyaan ini terlebih dahulu", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Render the current question based on its type
     */
    private fun renderQuestion() {
        val question = viewModel.getCurrentQuestion()
        val currentAnswer = viewModel.getCurrentAnswer()

        // Update question icon based on question type
        val iconRes = when (question) {
            is ScreeningQuestion.Gender -> R.drawable.ic_gender
            is ScreeningQuestion.Age -> R.drawable.ic_age
            is ScreeningQuestion.AcademicPressure -> R.drawable.ic_academic
            is ScreeningQuestion.StudySatisfaction -> R.drawable.ic_satisfaction
            is ScreeningQuestion.SleepDuration -> R.drawable.ic_sleep
            is ScreeningQuestion.DietaryHabits -> R.drawable.ic_diet
            is ScreeningQuestion.SuicidalThoughts -> R.drawable.ic_thoughts
            is ScreeningQuestion.StudyHours -> R.drawable.ic_study_hours
            is ScreeningQuestion.FinancialStress -> R.drawable.ic_financial
            is ScreeningQuestion.FamilyHistory -> R.drawable.ic_family
        }
        binding.ivQuestionIcon.setImageResource(iconRes)

        // Update question text
        binding.tvQuestion.text = question.question

        if (question.description != null) {
            binding.tvDescription.text = question.description
            binding.tvDescription.isVisible = true
        } else {
            binding.tvDescription.isVisible = false
        }

        // Clear previous answer views
        binding.answerContainer.removeAllViews()
        currentSelectedCard = null

        // Render appropriate input type
        when (question) {
            is ScreeningQuestion.Gender -> renderCardOptions(question.options, currentAnswer as? String)
            is ScreeningQuestion.Age -> renderNumberInput(question.minAge, question.maxAge, currentAnswer as? Int)
            is ScreeningQuestion.AcademicPressure -> renderSlider(question.min, question.max, question.labels, currentAnswer as? Int)
            is ScreeningQuestion.StudySatisfaction -> renderSlider(question.min, question.max, question.labels, currentAnswer as? Int)
            is ScreeningQuestion.SleepDuration -> renderCardOptions(question.options, currentAnswer as? String)
            is ScreeningQuestion.DietaryHabits -> renderCardOptions(question.options, currentAnswer as? String)
            is ScreeningQuestion.SuicidalThoughts -> renderCardOptions(question.options, currentAnswer as? String)
            is ScreeningQuestion.StudyHours -> renderNumberInput(question.min, question.max, currentAnswer as? Int)
            is ScreeningQuestion.FinancialStress -> renderSlider(question.min, question.max, question.labels, currentAnswer as? Int)
            is ScreeningQuestion.FamilyHistory -> renderCardOptions(question.options, currentAnswer as? String)
        }

        // Update progress immediately (both text and bar)
        binding.tvProgressText.text = "Pertanyaan ${viewModel.getProgressText()}"
        val currentProgress = (viewModel.progress.value * 100).toInt()
        binding.progressBar.progress = currentProgress
        binding.tvProgressPercentage.text = "$currentProgress%"
        updateNavigationButtons()
    }

    /**
     * Render card-based options (for choice questions)
     */
    private fun renderCardOptions(options: List<String>, selectedOption: String?) {
        options.forEach { option ->
            val cardView = layoutInflater.inflate(
                R.layout.item_answer_card,
                binding.answerContainer,
                false
            )

            val tvOptionText = cardView.findViewById<TextView>(R.id.tvOptionText)
            tvOptionText.text = option

            // Set selected state
            val isSelected = option == selectedOption
            if (isSelected) {
                cardView.setBackgroundResource(R.drawable.bg_answer_card_selected)
                currentSelectedCard = cardView
            } else {
                cardView.setBackgroundResource(R.drawable.bg_glass_dark)
            }

            // Click listener
            cardView.setOnClickListener {
                // Update UI
                currentSelectedCard?.setBackgroundResource(R.drawable.bg_glass_dark)
                cardView.setBackgroundResource(R.drawable.bg_answer_card_selected)
                currentSelectedCard = cardView

                // Save answer
                viewModel.saveAnswer(option)
            }

            binding.answerContainer.addView(cardView)
        }
    }

    /**
     * Render slider input (for scale questions 1-5)
     */
    private fun renderSlider(min: Int, max: Int, labels: Map<Int, String>, currentValue: Int?) {
        val sliderView = layoutInflater.inflate(
            R.layout.view_slider_input,
            binding.answerContainer,
            false
        )

        val slider = sliderView.findViewById<Slider>(R.id.slider)
        val tvValue = sliderView.findViewById<TextView>(R.id.tvSliderValue)
        val tvLabel = sliderView.findViewById<TextView>(R.id.tvSliderLabel)
        val tvMinLabel = sliderView.findViewById<TextView>(R.id.tvMinLabel)
        val tvMaxLabel = sliderView.findViewById<TextView>(R.id.tvMaxLabel)

        // Set slider range
        slider.valueFrom = min.toFloat()
        slider.valueTo = max.toFloat()
        slider.stepSize = 1f
        slider.value = (currentValue ?: 3).toFloat()

        // Set labels
        tvMinLabel.text = labels[min] ?: ""
        tvMaxLabel.text = labels[max] ?: ""

        // Update value display
        fun updateValueDisplay(value: Int) {
            tvValue.text = value.toString()
            tvLabel.text = labels[value] ?: ""
        }

        updateValueDisplay(slider.value.toInt())

        // Slider change listener
        slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val intValue = value.toInt()
                updateValueDisplay(intValue)
                viewModel.saveAnswer(intValue)
            }
        }

        binding.answerContainer.addView(sliderView)
    }

    /**
     * Render number input (for age and study hours)
     */
    private fun renderNumberInput(min: Int, max: Int, currentValue: Int?) {
        val numberView = layoutInflater.inflate(
            R.layout.view_number_input,
            binding.answerContainer,
            false
        )

        val tvNumber = numberView.findViewById<TextView>(R.id.tvNumberValue)
        val btnDecrease = numberView.findViewById<TextView>(R.id.btnDecrease)
        val btnIncrease = numberView.findViewById<TextView>(R.id.btnIncrease)
        val etInput = numberView.findViewById<EditText>(R.id.etNumberInput)

        // Set initial value
        var currentNum = currentValue ?: ((min + max) / 2)
        tvNumber.text = currentNum.toString()
        etInput.setText(currentNum.toString())

        // Update function
        fun updateValue(newValue: Int) {
            currentNum = newValue.coerceIn(min, max)
            tvNumber.text = currentNum.toString()
            etInput.setText(currentNum.toString())
            viewModel.saveAnswer(currentNum)
        }

        // Decrease button
        btnDecrease.setOnClickListener {
            updateValue(currentNum - 1)
        }

        // Increase button
        btnIncrease.setOnClickListener {
            updateValue(currentNum + 1)
        }

        // Direct input
        etInput.setOnEditorActionListener { _, _, _ ->
            val inputValue = etInput.text.toString().toIntOrNull()
            if (inputValue != null) {
                updateValue(inputValue)
            }
            false
        }

        binding.answerContainer.addView(numberView)
    }

    /**
     * Update navigation button states
     */
    private fun updateNavigationButtons() {
        // Back button text
        if (viewModel.isFirstQuestion()) {
            binding.btnBack.text = "← Batal"
        } else {
            binding.btnBack.text = "← Kembali"
        }

        // Next button text
        binding.btnNext.text = getNextButtonText()

        // Enable/disable next button based on answer
        binding.btnNext.isEnabled = viewModel.isCurrentQuestionAnswered()
    }

    /**
     * Get text for next button
     */
    private fun getNextButtonText(): String {
        return if (viewModel.isLastQuestion()) {
            "Selesai ✓"
        } else {
            "Lanjut →"
        }
    }

    /**
     * Navigate to result screen
     */
    private fun navigateToResult() {
        val resultFragment = com.mindcheck.app.presentation.result.ResultFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, resultFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
