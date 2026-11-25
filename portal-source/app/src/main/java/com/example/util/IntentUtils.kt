package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.AppLanguage

/**
 * Centralized safe Activity/Intent launchers so missing handlers (browser, dialer, email, maps, chooser)
 * or malformed URIs never throw unhandled exceptions on any Android device configuration.
 */
object IntentUtils {

    fun safeDial(context: Context, rawPhone: String, language: AppLanguage = AppLanguage.BN) {
        val cleaned = rawPhone.trim()
        if (cleaned.isBlank()) {
            showToast(
                context,
                if (language == AppLanguage.BN) "সঠিক ফোন নম্বর পাওয়া যায়নি" else "Phone number is unavailable"
            )
            return
        }
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned")).apply {
                if (context !is android.app.Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            showToast(
                context,
                if (language == AppLanguage.BN) "ডায়ালার অ্যাপ পাওয়া যায়নি" else "No phone dialer app available"
            )
        } catch (_: Exception) {
            showToast(
                context,
                if (language == AppLanguage.BN) "কল শুরু করা যায়নি" else "Unable to start call"
            )
        }
    }

    fun safeSendEmail(context: Context, rawEmail: String, language: AppLanguage = AppLanguage.BN) {
        val cleaned = rawEmail.trim()
        if (cleaned.isBlank()) {
            showToast(
                context,
                if (language == AppLanguage.BN) "ইমেইল ঠিকানা পাওয়া যায়নি" else "Email address is unavailable"
            )
            return
        }
        try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$cleaned")).apply {
                if (context !is android.app.Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            showToast(
                context,
                if (language == AppLanguage.BN) "ইমেইল পাঠানোর কোনো অ্যাপ পাওয়া যায়নি" else "No email app found"
            )
        } catch (_: Exception) {
            showToast(
                context,
                if (language == AppLanguage.BN) "ইমেইল খোলা যায়নি" else "Unable to open email app"
            )
        }
    }

    fun safeOpenUrl(context: Context, rawUrl: String, language: AppLanguage = AppLanguage.BN) {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) {
            showToast(
                context,
                if (language == AppLanguage.BN) "লিংক পাওয়া যায়নি" else "Link is unavailable"
            )
            return
        }
        val normalized = if (
            trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("geo:", ignoreCase = true) ||
            trimmed.startsWith("tel:", ignoreCase = true) ||
            trimmed.startsWith("mailto:", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
                if (context !is android.app.Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            showToast(
                context,
                if (language == AppLanguage.BN) "লিংক খোলার উপযুক্ত অ্যাপ বা ব্রাউজার পাওয়া যায়নি" else "No browser or app found to open link"
            )
        } catch (_: Exception) {
            showToast(
                context,
                if (language == AppLanguage.BN) "লিংক খুলতে সমস্যা হয়েছে" else "Could not open link"
            )
        }
    }

    fun safeShareText(
        context: Context,
        text: String,
        subject: String = "",
        chooserTitle: String = "Share",
        language: AppLanguage = AppLanguage.BN
    ) {
        if (text.isBlank()) return
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                if (subject.isNotBlank()) {
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                }
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
                if (context !is android.app.Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(chooser)
        } catch (_: ActivityNotFoundException) {
            showToast(
                context,
                if (language == AppLanguage.BN) "শেয়ার করার কোনো অ্যাপ পাওয়া যায়নি" else "No sharing app available"
            )
        } catch (_: Exception) {
            showToast(
                context,
                if (language == AppLanguage.BN) "শেয়ার করা যায়নি" else "Unable to share right now"
            )
        }
    }

    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
