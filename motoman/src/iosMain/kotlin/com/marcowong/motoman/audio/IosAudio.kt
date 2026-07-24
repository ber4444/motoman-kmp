package com.marcowong.motoman.audio

import platform.AVFAudio.*
import platform.Foundation.NSURL
import platform.Foundation.NSBundle
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import kotlin.math.log2

class IosAudio : Audio {
    private var engine: AVAudioEngine? = null
    private var engineReady = false

    init {
        // Configure audio session first — this establishes audio routes
        // so AVAudioEngine can find output nodes.
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, error = null)
        session.setActive(true, error = null)

        val isSimulator = platform.Foundation.NSProcessInfo.processInfo.environment["SIMULATOR_DEVICE_NAME"] != null

        // Check if there's actually an audio output route available, and skip if simulator
        // (Simulator audio HAL is often broken and AVAudioEngine throws a fatal NSException).
        val hasOutput = session.currentRoute.outputs.isNotEmpty()
        if (hasOutput && !isSimulator) {
            val e = AVAudioEngine()
            e.prepare()
            engineReady = e.startAndReturnError(null)
            if (engineReady) engine = e
        } else {
            println("WARNING: No audio output route available. Audio will be silent.")
        }
    }
    
    override fun newSound(path: String): Sound {
        val e = engine
        return if (e != null && engineReady) IosSound(e, path) else StubSound()
    }

    override fun newMusic(path: String): Music {
        return IosMusic(path)
    }
}

private class StubSound : Sound {
    override fun play(volume: Float, pitch: Float, pan: Float): Long = 0L
    override fun loop(volume: Float, pitch: Float, pan: Float): Long = 0L
    override fun stop(soundId: Long) {}
    override fun pause(soundId: Long) {}
    override fun resume(soundId: Long) {}
    override fun setVolume(soundId: Long, volume: Float) {}
    override fun setPitch(soundId: Long, pitch: Float) {}
    override fun setPan(soundId: Long, pan: Float, volume: Float) {}
    override fun dispose() {}
}

class IosSound(private val engine: AVAudioEngine, path: String) : Sound {
    private var soundIdCounter = 0L
    private val nodes = mutableMapOf<Long, Pair<AVAudioPlayerNode, AVAudioUnitTimePitch>>()
    private val buffer: AVAudioPCMBuffer
    
    init {
        val root = NSBundle.mainBundle.resourcePath ?: error("no resourcePath")
        val url = NSURL.fileURLWithPath("$root/$path")
        val file = AVAudioFile(forReading = url, error = null) ?: error("Failed to load sound $path")
        val format = file.processingFormat
        val frameCount = file.length.toUInt()
        buffer = AVAudioPCMBuffer(format, frameCount) ?: error("Failed to create PCM buffer")
        file.readIntoBuffer(buffer, error = null)
    }
    
    private fun getOrCreateNode(soundId: Long): Pair<AVAudioPlayerNode, AVAudioUnitTimePitch> {
        return nodes.getOrPut(soundId) {
            val playerNode = AVAudioPlayerNode()
            val pitchNode = AVAudioUnitTimePitch()
            
            engine.attachNode(playerNode)
            engine.attachNode(pitchNode)
            
            engine.connect(playerNode, to = pitchNode, format = buffer.format)
            engine.connect(pitchNode, to = engine.mainMixerNode, format = buffer.format)
            
            Pair(playerNode, pitchNode)
        }
    }
    
    private fun pitchToCents(pitch: Float): Float {
        // AVAudioUnitTimePitch pitch is in cents (1200 cents = 1 octave).
        return (1200.0 * log2(pitch.coerceAtLeast(0.01f).toDouble())).toFloat()
    }
    
    override fun play(volume: Float, pitch: Float, pan: Float): Long {
        val id = ++soundIdCounter
        val (player, pitchNode) = getOrCreateNode(id)
        player.volume = volume
        player.pan = pan
        pitchNode.pitch = pitchToCents(pitch)
        
        player.scheduleBuffer(buffer, atTime = null, options = 0u, completionHandler = null)
        player.play()
        return id
    }

    override fun loop(volume: Float, pitch: Float, pan: Float): Long {
        val id = ++soundIdCounter
        val (player, pitchNode) = getOrCreateNode(id)
        player.volume = volume
        player.pan = pan
        pitchNode.pitch = pitchToCents(pitch)
        
        // AVAudioPlayerNodeBufferLoops = 1UL << 0
        player.scheduleBuffer(buffer, atTime = null, options = 1u, completionHandler = null)
        player.play()
        return id
    }

    override fun stop(soundId: Long) {
        nodes[soundId]?.first?.stop()
    }

    override fun pause(soundId: Long) {
        nodes[soundId]?.first?.pause()
    }

    override fun resume(soundId: Long) {
        nodes[soundId]?.first?.play()
    }

    override fun setVolume(soundId: Long, volume: Float) {
        nodes[soundId]?.first?.volume = volume
    }

    override fun setPitch(soundId: Long, pitch: Float) {
        nodes[soundId]?.second?.pitch = pitchToCents(pitch)
    }

    override fun setPan(soundId: Long, pan: Float, volume: Float) {
        nodes[soundId]?.first?.pan = pan
        nodes[soundId]?.first?.volume = volume
    }

    override fun dispose() {
        nodes.values.forEach { (player, pitchNode) ->
            player.stop()
            engine.detachNode(player)
            engine.detachNode(pitchNode)
        }
        nodes.clear()
    }
}

class IosMusic(private val path: String) : Music {
    init {
        println("WARNING: IosMusic is stubbed. AVFoundation cannot natively decode Ogg Vorbis ($path).")
        println("Will need transcoding to .m4a/AAC to be supported on iOS.")
    }

    override fun play() {}
    override fun stop() {}
    override fun pause() {}
    override fun setVolume(volume: Float) {}
    override fun setLooping(isLooping: Boolean) {}
    override fun isPlaying(): Boolean = false
    override fun dispose() {}
}

class IosHaptics : Haptics {
    private val generator = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    
    init {
        generator.prepare()
    }
    
    override fun vibrate(milliseconds: Int) {
        // Haptics.vibrate(milliseconds) maps poorly to iOS, so a short impact pulse is the honest approximation.
        generator.impactOccurred()
    }
}
