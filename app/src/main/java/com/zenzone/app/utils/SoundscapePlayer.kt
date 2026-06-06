package com.zenzone.app.utils

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri

class SoundscapePlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentSound: String = SOUNDSCAPE_NONE

    fun playSound(soundType: String) {
        if (soundType == currentSound) return
        
        stop()
        currentSound = soundType
        if (soundType == SOUNDSCAPE_NONE) return

        val url = when (soundType) {
            SOUNDSCAPE_RAIN -> "https://www.soundjay.com/nature/sounds/rain-07.mp3"
            SOUNDSCAPE_FOREST -> "https://www.soundjay.com/nature/sounds/forest-wind-01.mp3"
            SOUNDSCAPE_BROWN_NOISE -> "https://www.soundjay.com/mechanical/sounds/air-conditioner-1.mp3"
            SOUNDSCAPE_LOFI -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3"
            else -> return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.parse(url))
                isLooping = true
                setOnPreparedListener { 
                    it.start() 
                }
                setOnErrorListener { mp, what, extra ->
                    mp.reset()
                    false
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resume() {
        try {
            if (mediaPlayer != null && !mediaPlayer!!.isPlaying && currentSound != SOUNDSCAPE_NONE) {
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
            currentSound = SOUNDSCAPE_NONE
        }
    }

    companion object {
        const val SOUNDSCAPE_NONE = "None"
        const val SOUNDSCAPE_RAIN = "Rain"
        const val SOUNDSCAPE_FOREST = "Forest"
        const val SOUNDSCAPE_BROWN_NOISE = "Brown Noise"
        const val SOUNDSCAPE_LOFI = "Lo-fi"
    }
}
