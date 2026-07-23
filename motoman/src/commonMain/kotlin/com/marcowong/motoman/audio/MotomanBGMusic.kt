package com.marcowong.motoman.audio

class MotomanBGMusic(private val audio: Audio) {
    private val volume = 0.25f
    private val m1: Music = audio.newMusic("data/bgm1.ogg")

    init {
        m1.setVolume(volume)
        m1.setLooping(true)
    }

    fun play() {
        //m1.play()
    }

    fun stop() {
        //m1.stop()
    }

    fun gamePause() {
        //m1.stop()
    }

    fun gameResume() {
        //m1.play()
    }

    fun dispose() {
        stop()
        m1.stop()
        m1.dispose()
    }
}
