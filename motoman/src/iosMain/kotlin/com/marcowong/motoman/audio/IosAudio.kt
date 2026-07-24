package com.marcowong.motoman.audio

import platform.AVFAudio.*
import platform.Foundation.NSURL
import platform.Foundation.NSBundle
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import kotlin.math.log2

val isSimulator = NSProcessInfo.processInfo.environment.containsKey("SIMULATOR_DEVICE_NAME")

class IosAudio : Audio {
    private val engine = AVAudioEngine()
    
    init {
        if (!isSimulator) {
            try {
                engine.prepare()
                engine.startAndReturnError(null)
            } catch (e: Exception) {
                println("WARNING: AVAudioEngine failed to start: ${e.message}")
            }
        } else {
            println("WARNING: Skipping AVAudioEngine start in simulator to prevent NSException crashes")
        }
    }
    
    override fun newSound(path: String): Sound {
        return IosSound(engine, path)
    }

    override fun newMusic(path: String): Music {
        return IosMusic(path)
    }
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
    
    private fun getOrCreateNode(soundId: Long): Pair<AVAudioPlayerNode, AVAudioUnitTimePitch>? {
        if (isSimulator) return null
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
        val nodes = getOrCreateNode(id) ?: return id
        val player = nodes.first
        val pitchNode = nodes.second
        player.volume = volume
        player.pan = pan
        pitchNode.pitch = pitchToCents(pitch)
        
        player.scheduleBuffer(buffer, atTime = null, options = 0u, completionHandler = null)
        player.play()
        return id
    }

    override fun loop(volume: Float, pitch: Float, pan: Float): Long {
        val id = ++soundIdCounter
        val nodes = getOrCreateNode(id) ?: return id
        val player = nodes.first
        val pitchNode = nodes.second
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
