package com.marcowong.motoman.audio

import com.marcowong.motoman.assets.Assets
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl

class DesktopAudio(private val assets: Assets) : Audio {
    override fun newSound(path: String): Sound {
        return DesktopSound(assets.readBytes(path))
    }

    override fun newMusic(path: String): Music {
        return DesktopMusic(assets.readBytes(path))
    }
}

class DesktopSound(private val audioData: ByteArray) : Sound {
    private var clip: Clip? = null
    private var baseSampleRate: Float = 44100f

    private fun loadClip() {
        if (clip != null) return
        try {
            val audioIn = AudioSystem.getAudioInputStream(ByteArrayInputStream(audioData))
            clip = AudioSystem.getClip()
            clip?.open(audioIn)
            baseSampleRate = clip?.format?.sampleRate ?: 44100f
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun play(volume: Float, pitch: Float, pan: Float): Long {
        loadClip()
        clip?.framePosition = 0
        applyVolume(volume)
        applyPitch(pitch)
        clip?.start()
        return 1L
    }

    override fun loop(volume: Float, pitch: Float, pan: Float): Long {
        loadClip()
        clip?.framePosition = 0
        applyVolume(volume)
        applyPitch(pitch)
        clip?.loop(Clip.LOOP_CONTINUOUSLY)
        return 1L
    }

    override fun stop(soundId: Long) {
        clip?.stop()
    }

    override fun pause(soundId: Long) {
        clip?.stop()
    }

    override fun resume(soundId: Long) {
        clip?.start()
    }

    override fun setVolume(soundId: Long, volume: Float) {
        applyVolume(volume)
    }

    override fun setPitch(soundId: Long, pitch: Float) {
        applyPitch(pitch)
    }

    override fun setPan(soundId: Long, pan: Float, volume: Float) {
        applyVolume(volume)
        // Ignoring pan for now
    }

    override fun dispose() {
        clip?.close()
        clip = null
    }

    private fun applyVolume(volume: Float) {
        val c = clip ?: return
        if (c.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            val gainControl = c.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            val clamped = volume.coerceIn(0f, 1f)
            val db = 20f * kotlin.math.log10(if (clamped > 0.0001f) clamped else 0.0001f)
            gainControl.value = db
        }
    }

    private fun applyPitch(pitch: Float) {
        val c = clip ?: return
        if (c.isControlSupported(FloatControl.Type.SAMPLE_RATE)) {
            val rateControl = c.getControl(FloatControl.Type.SAMPLE_RATE) as FloatControl
            rateControl.value = baseSampleRate * pitch
        }
    }
}

class DesktopMusic(private val audioData: ByteArray) : Music {
    private var clip: Clip? = null
    
    private fun loadClip() {
        if (clip != null) return
        try {
            val audioIn = AudioSystem.getAudioInputStream(ByteArrayInputStream(audioData))
            clip = AudioSystem.getClip()
            clip?.open(audioIn)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun play() {
        loadClip()
        clip?.framePosition = 0
        clip?.start()
    }

    override fun stop() {
        clip?.stop()
    }

    override fun pause() {
        clip?.stop()
    }

    override fun setVolume(volume: Float) {
        val c = clip ?: return
        if (c.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            val gainControl = c.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            val clamped = volume.coerceIn(0f, 1f)
            val db = 20f * kotlin.math.log10(if (clamped > 0.0001f) clamped else 0.0001f)
            gainControl.value = db
        }
    }

    override fun setLooping(isLooping: Boolean) {
        loadClip()
        if (isLooping) {
            clip?.loop(Clip.LOOP_CONTINUOUSLY)
        } else {
            clip?.loop(0)
        }
    }

    override fun isPlaying(): Boolean {
        return clip?.isActive ?: false
    }

    override fun dispose() {
        clip?.close()
        clip = null
    }
}

class DesktopHaptics : Haptics {
    override fun vibrate(milliseconds: Int) {
        // No haptics on desktop usually
    }
}
