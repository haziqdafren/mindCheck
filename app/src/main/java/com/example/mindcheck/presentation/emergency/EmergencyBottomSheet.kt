package com.mindcheck.app.presentation.emergency

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.mindcheck.app.R
import com.mindcheck.app.databinding.BottomSheetEmergencyBinding
import com.mindcheck.app.presentation.breathing.BreathingActivity
import com.mindcheck.app.utils.CustomDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import timber.log.Timber

/**
 * Emergency Tools Bottom Sheet - Phase 2
 * Quick access to breathing exercises and emergency contacts
 */
class EmergencyBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetEmergencyBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetEmergencyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Quick breathing
        binding.cardQuickBreathing.setOnClickListener {
            val intent = Intent(requireContext(), BreathingActivity::class.java)
            intent.putExtra("QUICK_MODE", true)
            startActivity(intent)
            dismiss()
        }

        // Grounding technique
        binding.cardGrounding.setOnClickListener {
            showGroundingExercise()
        }

        // Affirmations
        binding.cardAffirmations.setOnClickListener {
            showAffirmations()
        }

        // Hotline call
        binding.btnCallHotline.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:021500454")
            }
            startActivity(intent)
        }

        // Campus counselor
        binding.btnCampusCounselor.setOnClickListener {
            showCampusContacts()
        }
    }

    private fun showGroundingExercise() {
        CustomDialog.build(requireContext()) {
            setTitle("Teknik Grounding 5-4-3-2-1")
            setMessage("""
                Teknik ini membantu kamu kembali ke saat ini dan menenangkan pikiran.

                Cari dan sebutkan:

                🔵 5 hal yang bisa DILIHAT
                (contoh: lampu, meja, poster, tanaman, jendela)

                🟢 4 hal yang bisa DISENTUH
                (contoh: baju, kursi, dinding, lantai)

                🟡 3 hal yang bisa DIDENGAR
                (contoh: AC, suara lalu lintas, angin, musik)

                🟠 2 hal yang bisa DICIUM
                (contoh: parfum, udara segar, makanan)

                🔴 1 hal yang bisa DIKECAP/DIRASAKAN
                (contoh: minum air, permen, rasa di mulut)

                Lakukan perlahan dan fokus pada setiap sensasi. Tarik napas dalam di antara setiap langkah.
            """.trimIndent())
            setPositiveButton("Mulai Sekarang") {
                Toast.makeText(requireContext(), "Lakukan perlahan dan bernapas dalam 🌿", Toast.LENGTH_LONG).show()
                dismiss()
            }
            setNegativeButton("Nanti", null)
            setCancelable(true)
        }.show()
    }

    private fun showAffirmations() {
        val affirmations = listOf(
            "Aku cukup baik apa adanya 💚",
            "Aku pantas mendapat kebahagiaan dan kedamaian 🌸",
            "Aku kuat dan mampu melewati ini 💪",
            "Perasaanku valid dan aku boleh merasakannya ✨",
            "Aku sedang melakukan yang terbaik, dan itu sudah cukup 🌟",
            "Hari ini akan lebih baik, selangkah demi selangkah 🌈",
            "Aku tidak sendirian, ada yang peduli padaku 🤝",
            "Aku berhak untuk beristirahat dan merawat diri 🧘",
            "Kekuatanku lebih besar dari tantanganku 🔥",
            "Aku bangga dengan usahaku sejauh ini 🎯"
        )

        val randomAffirmation = affirmations.random()

        CustomDialog.build(requireContext()) {
            setTitle("Afirmasi Positif untuk Kamu")
            setMessage("""
                $randomAffirmation

                Ulangi afirmasi ini dalam hati atau keras. Tarik napas dalam, dan percayai kata-kata ini.

                Kamu layak mendapat kebaikan dan kesembuhan. 🌿
            """.trimIndent())
            setPositiveButton("Afirmasi Lain") {
                showAffirmations() // Show another one
            }
            setNegativeButton("Selesai") {
                Toast.makeText(requireContext(), "Semangat! Kamu luar biasa 💚", Toast.LENGTH_SHORT).show()
                dismiss()
            }
            setCancelable(true)
        }.show()
    }

    private fun showCampusContacts() {
        CustomDialog.build(requireContext()) {
            setTitle("Kontak Konselor Kampus")
            setMessage("""
                📞 Unit Konseling Mahasiswa
                Telp: (021) 1234-5678
                Email: konseling@kampus.ac.id

                📍 Lokasi:
                Gedung Rektorat Lt. 2
                Senin - Jumat: 08.00 - 16.00

                💚 Semua percakapan bersifat rahasia dan gratis untuk mahasiswa.

                Jangan ragu untuk mencari bantuan profesional ya!
            """.trimIndent())
            setPositiveButton("Hubungi Sekarang") {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:02112345678")
                }
                startActivity(intent)
            }
            setNegativeButton("Tutup", null)
            setCancelable(true)
        }.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
