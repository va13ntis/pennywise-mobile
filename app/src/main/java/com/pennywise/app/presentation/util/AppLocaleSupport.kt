package com.pennywise.app.presentation.util

import android.content.Context
import android.os.Build
import java.util.Locale

/**
 * Single source of truth for app-supported locales and RTL behavior.
 *
 * Keeping language canonicalization, fallback mapping, and RTL detection here
 * prevents locale drift between application startup, runtime locale updates,
 * and Compose layout direction.
 */
object AppLocaleSupport {
    const val ENGLISH = "en"
    const val HEBREW = "iw"
    const val RUSSIAN = "ru"

    private const val HEBREW_MODERN = "he"

    private val supportedLanguageCodes = setOf(ENGLISH, HEBREW, RUSSIAN)
    private val rtlLanguageCodes = setOf(HEBREW)

    fun resolveSupportedLocale(languageCode: String?, context: Context? = null): Locale {
        val canonicalCode = canonicalLanguageCode(languageCode)
        val resolvedCode = canonicalCode ?: detectSupportedSystemLanguageCode(context)
        return Locale(resolvedCode)
    }

    fun detectSupportedSystemLanguageCode(context: Context?): String {
        val systemLanguage = getSystemLanguageCode(context)
        return canonicalLanguageCode(systemLanguage) ?: ENGLISH
    }

    fun isRtlLanguage(languageCode: String?): Boolean {
        val canonicalCode = canonicalLanguageCode(languageCode)
        return canonicalCode != null && canonicalCode in rtlLanguageCodes
    }

    fun isLanguageSupported(languageCode: String): Boolean {
        val canonicalCode = canonicalLanguageCode(languageCode)
        return canonicalCode != null && canonicalCode in supportedLanguageCodes
    }

    fun canonicalLanguageCode(languageCode: String?): String? {
        return when {
            languageCode.equals(ENGLISH, ignoreCase = true) -> ENGLISH
            languageCode.equals(HEBREW, ignoreCase = true) ||
                languageCode.equals(HEBREW_MODERN, ignoreCase = true) -> HEBREW
            languageCode.equals(RUSSIAN, ignoreCase = true) -> RUSSIAN
            else -> null
        }
    }

    fun supportedLanguageCodes(): Set<String> = supportedLanguageCodes

    private fun getSystemLanguageCode(context: Context?): String? {
        return context?.let { safeContext ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                safeContext.resources.configuration.locales[0]?.language
            } else {
                @Suppress("DEPRECATION")
                safeContext.resources.configuration.locale?.language
            }
        } ?: Locale.getDefault().language
    }
}
