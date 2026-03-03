package com.mindcheck.app.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.mindcheck.app.presentation.auth.LoginActivity
import com.mindcheck.app.presentation.auth.RegisterActivity

/**
 * Utility for showing upgrade prompts to guest users
 * Encourages account creation to unlock premium features
 */
object UpgradePrompt {

    /**
     * Show upgrade dialog for a specific feature
     */
    fun show(
        context: Context,
        feature: UserSession.Feature,
        onDismiss: (() -> Unit)? = null
    ) {
        val title = UserSession.getFeatureRestrictionTitle(feature)
        val message = UserSession.getFeatureRestrictionMessage(feature)

        AlertDialog.Builder(context)
            .setTitle("🔒 $title")
            .setMessage(message)
            .setPositiveButton("Buat Akun Gratis") { dialog, _ ->
                dialog.dismiss()
                navigateToRegister(context)
            }
            .setNeutralButton("Login") { dialog, _ ->
                dialog.dismiss()
                navigateToLogin(context)
            }
            .setNegativeButton("Nanti") { dialog, _ ->
                dialog.dismiss()
                onDismiss?.invoke()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Show generic upgrade prompt with custom message
     */
    fun showCustom(
        context: Context,
        title: String,
        message: String,
        positiveButtonText: String = "Buat Akun Gratis",
        onDismiss: (() -> Unit)? = null
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { dialog, _ ->
                dialog.dismiss()
                navigateToRegister(context)
            }
            .setNeutralButton("Login") { dialog, _ ->
                dialog.dismiss()
                navigateToLogin(context)
            }
            .setNegativeButton("Nanti") { dialog, _ ->
                dialog.dismiss()
                onDismiss?.invoke()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Show simple info dialog (no navigation)
     */
    fun showInfo(
        context: Context,
        title: String,
        message: String
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun navigateToRegister(context: Context) {
        val intent = Intent(context, RegisterActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        context.startActivity(intent)

        // Finish MainActivity if it's the context
        if (context is Activity) {
            context.finish()
        }
    }

    private fun navigateToLogin(context: Context) {
        val intent = Intent(context, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        context.startActivity(intent)

        // Finish MainActivity if it's the context
        if (context is Activity) {
            context.finish()
        }
    }
}
