package com.mindcheck.app.utils

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import timber.log.Timber
import java.net.UnknownHostException
import java.io.IOException

/**
 * Centralized error handling utility
 * Provides user-friendly error messages and retry mechanisms
 */
object ErrorHandler {

    /**
     * Show user-friendly error message based on exception type
     */
    fun handle(context: Context, error: Throwable, action: String = "melakukan aksi") {
        Timber.e(error, "Error during: $action")

        val (title, message) = getErrorMessage(error, action)

        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Show error dialog with retry option
     */
    fun handleWithRetry(
        context: Context,
        error: Throwable,
        action: String = "melakukan aksi",
        onRetry: () -> Unit
    ) {
        Timber.e(error, "Error during: $action")

        val (title, message) = getErrorMessage(error, action)

        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Coba Lagi") { dialog, _ ->
                dialog.dismiss()
                onRetry()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Show simple info dialog
     */
    fun showInfo(context: Context, title: String, message: String) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Get user-friendly error message based on exception type
     */
    private fun getErrorMessage(error: Throwable, action: String): Pair<String, String> {
        return when (error) {
            is UnknownHostException -> {
                "⚠️ Tidak Ada Koneksi" to "Pastikan kamu terhubung ke internet untuk $action"
            }
            is IOException -> {
                "⚠️ Koneksi Bermasalah" to "Terjadi masalah saat $action. Periksa koneksi internet kamu."
            }
            is IllegalStateException -> {
                "❌ Kesalahan Aplikasi" to "Terjadi kesalahan internal. Silakan restart aplikasi."
            }
            is NullPointerException -> {
                "❌ Data Tidak Lengkap" to "Terjadi kesalahan saat memproses data. Silakan coba lagi."
            }
            else -> {
                "❌ Terjadi Kesalahan" to "Gagal $action: ${error.message ?: "Kesalahan tidak diketahui"}"
            }
        }
    }

    /**
     * Show loading toast (for quick operations)
     */
    fun showLoading(context: Context, message: String = "Memproses...") {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Show success toast
     */
    fun showSuccess(context: Context, message: String) {
        Toast.makeText(context, "✅ $message", Toast.LENGTH_SHORT).show()
    }

    /**
     * Show warning toast
     */
    fun showWarning(context: Context, message: String) {
        Toast.makeText(context, "⚠️ $message", Toast.LENGTH_SHORT).show()
    }
}
