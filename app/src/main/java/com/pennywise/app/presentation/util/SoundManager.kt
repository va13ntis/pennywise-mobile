package com.pennywise.app.presentation.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sound manager for playing UI sounds in the app
 */
@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var soundPool: SoundPool? = null
    private var kachingSoundId: Int = 0
    private var isInitialized = false

    init {
        initializeSoundPool()
    }

    /**
     * Initialize the sound pool and load sounds
     */
    private fun initializeSoundPool() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(audioAttributes)
                .build()

            // Load the kaching sound (if available)
            // For now, we'll skip loading the sound file since it doesn't exist yet
            // When you add the actual kaching.mp3 file, uncomment the lines below:
            /*
            try {
                kachingSoundId = soundPool?.load(context, R.raw.kaching, 1) ?: 0
                if (kachingSoundId == 0) {
                    Log.w("SoundManager", "Failed to load kaching sound - file may not exist")
                }
            } catch (e: Exception) {
                Log.w("SoundManager", "Could not load kaching sound file", e)
                kachingSoundId = 0
            }
            */
            
            // Temporary: Set to 0 until sound file is added
            kachingSoundId = 0
            Log.d("SoundManager", "Sound loading skipped - add kaching.mp3 to enable sound feature")
            
            isInitialized = true
            Log.d("SoundManager", "Sound pool initialized successfully")
        } catch (e: Exception) {
            Log.e("SoundManager", "Failed to initialize sound pool", e)
            isInitialized = false
        }
    }

    /**
     * Play the kaching sound when an expense is added
     */
    fun playKachingSound() {
        if (!isInitialized || kachingSoundId == 0) {
            Log.w("SoundManager", "Sound pool not initialized or sound not loaded")
            return
        }

        try {
            soundPool?.play(
                kachingSoundId,
                0.7f, // left volume
                0.7f, // right volume
                1,    // priority
                0,    // loop (0 = no loop)
                1f    // rate
            )
            Log.d("SoundManager", "Kaching sound played successfully")
        } catch (e: Exception) {
            Log.e("SoundManager", "Failed to play kaching sound", e)
        }
    }

    /**
     * Release resources when the sound manager is no longer needed
     */
    fun release() {
        try {
            soundPool?.release()
            soundPool = null
            isInitialized = false
            Log.d("SoundManager", "Sound pool released")
        } catch (e: Exception) {
            Log.e("SoundManager", "Failed to release sound pool", e)
        }
    }
}
