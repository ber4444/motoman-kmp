package com.marcowong.motoman

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Vibrator
import android.os.VibrationEffect
import com.marcowong.motoman.audio.Audio
import com.marcowong.motoman.audio.Music
import com.marcowong.motoman.audio.Sound
import com.marcowong.motoman.audio.Haptics

class AndroidAudio(private val context: Context) : Audio {
    private val soundPool: SoundPool

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(16)
            .setAudioAttributes(audioAttributes)
            .build()
    }

    override fun newSound(path: String): Sound {
        // Assume path is like "data/engineSoundIdle.wav"
        // Need to load from assets
        val afd = context.assets.openFd(path)
        val soundId = soundPool.load(afd, 1)
        return AndroidSound(soundPool, soundId)
    }

    override fun newMusic(path: String): Music {
        val afd = context.assets.openFd(path)
        val mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        mediaPlayer.prepare()
        return AndroidMusic(mediaPlayer)
    }
}

class AndroidSound(
    private val soundPool: SoundPool,
    private val soundId: Int
) : Sound {
    override fun play(volume: Float, pitch: Float, pan: Float): Long {
        val left = volume * (1f - pan)
        val right = volume * (1f + pan)
        return soundPool.play(soundId, left, right, 1, 0, pitch).toLong()
    }

    override fun loop(volume: Float, pitch: Float, pan: Float): Long {
        val left = volume * (1f - pan)
        val right = volume * (1f + pan)
        return soundPool.play(soundId, left, right, 1, -1, pitch).toLong()
    }

    override fun stop(soundId: Long) {
        soundPool.stop(soundId.toInt())
    }

    override fun pause(soundId: Long) {
        soundPool.pause(soundId.toInt())
    }

    override fun resume(soundId: Long) {
        soundPool.resume(soundId.toInt())
    }

    override fun setVolume(soundId: Long, volume: Float) {
        soundPool.setVolume(soundId.toInt(), volume, volume)
    }

    override fun setPitch(soundId: Long, pitch: Float) {
        soundPool.setRate(soundId.toInt(), pitch)
    }

    override fun setPan(soundId: Long, pan: Float, volume: Float) {
        val left = volume * (1f - pan)
        val right = volume * (1f + pan)
        soundPool.setVolume(soundId.toInt(), left, right)
    }

    override fun dispose() {
        soundPool.unload(soundId)
    }
}

class AndroidMusic(private val mediaPlayer: MediaPlayer) : Music {
    override fun play() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }
    }

    override fun stop() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
            mediaPlayer.prepare() // Prepare it again for next play
        }
    }

    override fun pause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }

    override fun setVolume(volume: Float) {
        mediaPlayer.setVolume(volume, volume)
    }

    override fun setLooping(isLooping: Boolean) {
        mediaPlayer.isLooping = isLooping
    }

    override fun isPlaying(): Boolean {
        return mediaPlayer.isPlaying
    }

    override fun dispose() {
        mediaPlayer.release()
    }
}

class AndroidHaptics(private val context: Context) : Haptics {
    override fun vibrate(milliseconds: Int) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(milliseconds.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(milliseconds.toLong())
            }
        }
    }
}
