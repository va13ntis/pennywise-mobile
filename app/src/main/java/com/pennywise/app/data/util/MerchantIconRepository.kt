package com.pennywise.app.data.util

import android.content.Context
import com.pennywise.app.presentation.util.MerchantIconUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MerchantIconRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val cacheDir = File(context.cacheDir, "merchant_icons").apply { mkdirs() }

    fun loadCachedIcons(): Map<String, File> {
        if (!cacheDir.exists()) return emptyMap()
        return cacheDir.listFiles()
            ?.filter { it.isFile }
            ?.associateBy { it.nameWithoutExtension }
            ?: emptyMap()
    }

    fun getCachedIconFile(merchantName: String): File? {
        val key = MerchantIconUtils.normalizeMerchantKey(merchantName)
        if (key.isBlank()) return null
        val existing = File(cacheDir, "$key.img")
        return if (existing.exists()) existing else null
    }

    suspend fun fetchAndCacheIcon(merchantName: String): File? {
        val key = MerchantIconUtils.normalizeMerchantKey(merchantName)
        if (key.isBlank()) return null
        val target = File(cacheDir, "$key.img")
        if (target.exists()) return target

        val domain = "$key.com"
        val urls = listOf(
            "https://logo.clearbit.com/$domain",
            "https://icons.duckduckgo.com/ip3/$domain.ico"
        )

        for (url in urls) {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use
                val body = response.body ?: return@use
                target.outputStream().use { output ->
                    body.byteStream().copyTo(output)
                }
                if (target.length() > 0) {
                    return target
                }
            }
        }

        return null
    }
}
