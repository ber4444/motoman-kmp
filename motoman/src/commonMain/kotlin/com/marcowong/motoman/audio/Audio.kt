package com.marcowong.motoman.audio

interface Audio {
    fun newSound(path: String): Sound
    fun newMusic(path: String): Music
}

interface Sound {
    fun play(volume: Float = 1f, pitch: Float = 1f, pan: Float = 0f): Long
    fun loop(volume: Float = 1f, pitch: Float = 1f, pan: Float = 0f): Long
    fun stop(soundId: Long)
    fun pause(soundId: Long)
    fun resume(soundId: Long)
    fun setVolume(soundId: Long, volume: Float)
    fun setPitch(soundId: Long, pitch: Float)
    fun setPan(soundId: Long, pan: Float, volume: Float)
    fun dispose()
}

interface Music {
    fun play()
    fun stop()
    fun pause()
    fun setVolume(volume: Float)
    fun setLooping(isLooping: Boolean)
    fun isPlaying(): Boolean
    fun dispose()
}

interface Haptics {
    fun vibrate(milliseconds: Int)
}
