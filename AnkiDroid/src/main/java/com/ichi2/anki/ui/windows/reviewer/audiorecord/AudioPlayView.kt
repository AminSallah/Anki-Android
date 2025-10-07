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

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.ichi2.anki.R
import com.ichi2.anki.settings.Prefs

/**
 * Simple player with a progress bar, a play button and a cancel button
 */
class AudioPlayView : ConstraintLayout {
    private val progressBar: LinearProgressIndicator
    private val playIconView: ImageView

    private val speedButton: MaterialButton

    private val speedChoices = listOf("0.5", "1.0", "1.25" ,"1.5", "1.75","2.0", "2.25", "2.5")

    constructor(context: Context) : this(context, null, 0, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int,
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        LayoutInflater.from(context).inflate(R.layout.audio_play_view, this, true)
        progressBar = findViewById(R.id.progress_indicator)
        playIconView = findViewById(R.id.play_icon)
        findViewById<View>(R.id.play_button).setOnClickListener {
            buttonPressListener?.onPlayButtonPressed()
        }
        findViewById<View>(R.id.cancel_button).setOnClickListener {
            buttonPressListener?.onCancelButtonPressed()
        }

        speedButton = findViewById<MaterialButton>(R.id.speed_button)

        displaySpeedOnButton()

        speedButton.setOnClickListener {
            // cycle to next of the speedChoices
            val cur = currentSpeedString()
            val idx = speedChoices.indexOf(cur).let { if (it == -1) speedChoices.indexOf("1.0") else it }
            val next = speedChoices[(idx + 1) % speedChoices.size]

            // save as string in Prefs
            Prefs.putString(R.string.audio_playback_speed, next)

            // update ui
            speedButton.text = "${next}x"

            speedButtonListener?.onSpeedChanged(next)



        }
    }

    interface SpeedButtonListener {
        fun onSpeedChanged(newSpeedString: String)
    }

    private var speedButtonListener: SpeedButtonListener? = null

    fun setSpeedButtonListener(listener: SpeedButtonListener?) {
        speedButtonListener = listener
    }


    fun currentSpeedString(): String {
        return Prefs.getString(R.string.audio_playback_speed, "1.0") ?: "1.0"
    }

    fun displaySpeedOnButton() {
        val s = currentSpeedString()
        speedButton.text = "${s}x"
    }

    interface ButtonPressListener {
        fun onPlayButtonPressed()

        fun onCancelButtonPressed()
    }

    private var buttonPressListener: ButtonPressListener? = null

    fun setButtonPressListener(playListener: ButtonPressListener) {
        this.buttonPressListener = playListener
    }

    /**
     * Rotates the play icon 360º
     */
    fun rotateReplayIcon() {
        playIconView.rotation = 0F
        playIconView
            .animate()
            .rotation(-360F)
            .setDuration(400)
            .setInterpolator(
                DecelerateInterpolator(),
            ).start()
    }

    /**
     * Replaces the `Play` button icon with [iconRes] with a crossfade animation.
     */
    fun changePlayIcon(
        @DrawableRes iconRes: Int,
    ) {
        playIconView
            .animate()
            .alpha(0f)
            .setDuration(100)
            .withEndAction {
                playIconView.setImageResource(iconRes)
                playIconView
                    .animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            }.start()
    }

    fun setPlaybackProgress(progress: Int) {
        if (progress == 0) {
            progressBar.progress = 0 // `animate = false` wasn't working for some reason
        } else {
            progressBar.setProgress(progress, true)
        }
    }

    fun setPlaybackProgressBarMax(max: Int) {
        progressBar.max = max
    }
}
