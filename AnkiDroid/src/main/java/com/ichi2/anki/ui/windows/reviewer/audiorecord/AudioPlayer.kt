/*
 * Copyright (c) 2025 Brayan Oliveira <69634269+brayandso@users.noreply.github.com>
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.ichi2.anki.ui.windows.reviewer.audiorecord

import android.media.MediaPlayer
import com.ichi2.anki.R
import timber.log.Timber
import java.io.Closeable
import android.media.PlaybackParams
import com.ichi2.anki.settings.Prefs
import java.io.IOException

class AudioPlayer : Closeable {
    private val mediaPlayer = MediaPlayer()

    var isPlaying = false
        private set
    private var isPrepared = false

    var onCompletion: (() -> Unit)? = null

    @Volatile
    private var playbackSpeed: Float = 1.0f

    val duration: Int
        get() = if (isPrepared) mediaPlayer.duration else 0
    val currentPosition: Int
        get() = if (isPrepared) mediaPlayer.currentPosition else 0

    init {
        mediaPlayer.setOnCompletionListener {
            isPlaying = false
            onCompletion?.invoke()
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        if (isPlaying) {
            val clamped = speed.coerceIn(0.25f, 3.0f)
            playbackSpeed = clamped
            try {
                val params = mediaPlayer.playbackParams
                params.speed = clamped
                params.pitch = 1.0f
                mediaPlayer.playbackParams = params
                Timber.i("AudioPlayer::setPlaybackSpeed applied %.2f", clamped)

            } catch (e: Exception) {
                Timber.w(e, "Failed to set playback speed")
            }
        }
    }

    private fun loadPlaybackSpeedFromPrefs() {
        try {
            val speedStr = Prefs.getString(R.string.audio_playback_speed, "1.0")
            val speed = speedStr?.toFloatOrNull() ?: 1.0f
            playbackSpeed = speed.coerceIn(0.5f, 2.5f) // reasonable bounds
            Timber.i("AudioPlayer::loadPlaybackSpeedFromPrefs speed=%f", playbackSpeed)
        } catch (e: Exception) {
            Timber.w(e, "Failed to read playback speed from prefs")
            playbackSpeed = 1.0f
        }
    }

    fun play(
        filePath: String,
        onPrepared: () -> Unit,
    ) {
        Timber.i("AudioPlayer::play (isPlaying %b)", isPlaying)
        try {
            mediaPlayer.reset()
            isPrepared = false
            isPlaying = false

            // Load speed from preferences
            loadPlaybackSpeedFromPrefs()

            mediaPlayer.setDataSource(filePath)
            mediaPlayer.setOnPreparedListener { mp ->
                isPrepared = true

                val params = PlaybackParams()
                params.speed = playbackSpeed
                params.pitch = 1.0f
                mp.playbackParams = params

                mp.start()
                isPlaying = true
                onPrepared()
            }
            mediaPlayer.prepareAsync()
        } catch (exception: IOException) {
            Timber.w(exception, "Could not play file %s", filePath)
            close()
        }
    }

    fun replay() {
        Timber.i("AudioPlayer::replay (isPlaying %b) (isPrepared %b)", isPlaying, isPrepared)
        if (isPrepared) {
            mediaPlayer.seekTo(0)
            mediaPlayer.start()
            isPlaying = true
        }
    }

    override fun close() {
        Timber.i("AudioPlayer::close (isPlaying %b) (isPrepared %b)", isPlaying, isPrepared)
        mediaPlayer.reset()
        isPrepared = false
        isPlaying = false
    }
}
