package com.example.todoapp

import android.animation.ValueAnimator
import android.content.Context
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import java.io.File
import kotlin.math.max
import kotlin.math.min

class VoiceWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val waves = mutableListOf<View>()
    private var mediaRecorder: MediaRecorder? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isAnimating = false
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isAnimating) {
                updateWaveform()
                handler.postDelayed(this, 100) // 每100ms更新一次
            }
        }
    }

    init {
        orientation = HORIZONTAL
        gravity = android.view.Gravity.CENTER

        LayoutInflater.from(context).inflate(R.layout.voice_wave_view, this, true)

        waves.add(findViewById(R.id.wave1))
        waves.add(findViewById(R.id.wave2))
        waves.add(findViewById(R.id.wave3))
        waves.add(findViewById(R.id.wave4))
        waves.add(findViewById(R.id.wave5))

        waves.forEach { wave ->
            val params = wave.layoutParams as LayoutParams
            params.marginStart = 6
            params.marginEnd = 6
            wave.layoutParams = params
            wave.scaleY = 0.3f
        }
    }

    fun startAnimation() {
        if (isAnimating) return

        try {
            val tempFile = File(context.cacheDir, "temp_audio_record.tmp")

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(tempFile.absolutePath)
                prepare()
                start()
            }

            isAnimating = true
            handler.post(updateRunnable)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopAnimation() {
        isAnimating = false
        handler.removeCallbacks(updateRunnable)

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        waves.forEach { it.scaleY = 0.3f }
    }

    private fun updateWaveform() {
        try {
            val amplitude = mediaRecorder?.maxAmplitude ?: 0
            val normalized = min(max(amplitude / 32768.0f, 0f), 1f)

            // 根据音量大小设置波形高度
            waves.forEachIndexed { index, wave ->
                val targetScale = when (index) {
                    0, 4 -> 0.3f + normalized * 0.7f
                    1, 3 -> 0.3f + normalized * 1.0f
                    2 -> 0.3f + normalized * 1.3f
                    else -> 0.3f
                }

                wave.animate()
                    .scaleY(targetScale)
                    .setDuration(100)
                    .start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }
}