/*
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 *                         2026 zylhdrXP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.speaker

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.lineageos.settings.R

class ClearSpeakerViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var audioFocusRequest: AudioFocusRequest? = null

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        if (focusChange != AudioManager.AUDIOFOCUS_GAIN) {
            stopPlaying()
        }
    }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val stopRunnable = Runnable {
        stopPlaying()
    }

    fun toggleClearSpeaker(enabled: Boolean) {
        if (enabled) {
            if (startPlaying()) {
                handler.postDelayed(stopRunnable, 30000)
            }
        } else {
            stopPlaying()
        }
    }

    private fun startPlaying(): Boolean {
        try {
            val attributes =
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()

            val request =
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(attributes)
                    .setWillPauseWhenDucked(true)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener, handler)
                    .build()

            if (audioManager.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                return false
            }
            audioFocusRequest = request

            audioManager.setParameters("status_earpiece_clean=on")
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(attributes)
                setLooping(true)
                val file = context.resources.openRawResourceFd(R.raw.clear_speaker_sound)
                setDataSource(file.fileDescriptor, file.startOffset, file.length)
                file.close()
                setVolume(1.0f, 1.0f)
                prepare()
                start()
            }
            _isPlaying.value = true
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play speaker clean sound!", e)
            stopPlaying()
            return false
        }
    }

    fun stopPlaying() {
        handler.removeCallbacks(stopRunnable)
        audioFocusRequest?.let {
            audioManager.abandonAudioFocusRequest(it)
            audioFocusRequest = null
        }
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.reset()
            it.release()
        }
        mediaPlayer = null
        audioManager.setParameters("status_earpiece_clean=off")
        _isPlaying.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopPlaying()
    }

    companion object {
        private const val TAG = "ClearSpeakerViewModel"
    }
}
