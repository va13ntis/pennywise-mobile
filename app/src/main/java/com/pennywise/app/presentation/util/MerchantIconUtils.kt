package com.pennywise.app.presentation.util

object MerchantIconUtils {
    fun normalizeMerchantKey(merchantName: String): String {
        return merchantName
            .trim()
            .lowercase()
            .replace("[^a-z0-9]".toRegex(), "")
    }

    fun merchantInitial(merchantName: String): String {
        return merchantName.trim().take(1).uppercase().ifBlank { "?" }
    }
}
