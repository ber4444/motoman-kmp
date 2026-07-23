package com.marcowong.motoman.audio

import com.marcowong.motoman.scene.Motorcycle
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

class MotorcycleSFX(
    private val motorcycle: Motorcycle,
    private val backfireReporter: BackfireReporter,
    private val audio: Audio,
    private val haptics: Haptics
) {
    interface BackfireReporter {
        fun reportBackfire(size: Float)
    }

    private var soundIdle: Sound
    private var soundExhaust: Sound
    private var soundCrankShaft: Sound
    private var soundBackfire: Sound
    private var soundKneeDragging: Sound
    private var soundKneeDragging2: Sound
    private var soundCrash: Sound
    private var soundDryClutch: Sound
    private var soundBrakeDisc: Sound

    private var gearBoxBase = 0.75f
    private var gearBoxRangesCombined: FloatArray

    init {
        val gearBoxRanges = floatArrayOf(1f, 1.4f, 1.8f, 2.2f, 2.6f, 3f)
        var gearBoxRangesT = 0f
        for (i in gearBoxRanges.indices) gearBoxRangesT += gearBoxRanges[i]
        for (i in gearBoxRanges.indices) gearBoxRanges[i] /= gearBoxRangesT
        gearBoxRangesCombined = FloatArray(gearBoxRanges.size)
        for (i in gearBoxRanges.indices) gearBoxRangesCombined[i] = gearBoxRanges[i]
        for (i in 1 until gearBoxRanges.size) gearBoxRangesCombined[i] *= (1f - gearBoxBase)
        gearBoxRangesT = 0f
        for (i in gearBoxRanges.indices) gearBoxRangesT += gearBoxRangesCombined[i]
        for (i in gearBoxRanges.indices) gearBoxRangesCombined[i] /= gearBoxRangesT
        gearBoxRangesCombined[gearBoxRangesCombined.size - 1] += 0.000001f

        soundIdle = audio.newSound("data/engineSoundIdle.wav")
        soundDryClutch = audio.newSound("data/dryClutchSound.wav")
        soundExhaust = audio.newSound("data/engineSoundC.wav")
        soundCrankShaft = audio.newSound("data/engineSound2B.wav")
        soundBrakeDisc = audio.newSound("data/brakeDiscSound.wav")
        soundBackfire = audio.newSound("data/backfireSound.wav")
        soundKneeDragging = audio.newSound("data/kneeDraggingSound.wav")
        soundKneeDragging2 = audio.newSound("data/kneeDraggingSound2.wav")
        soundCrash = audio.newSound("data/crashSound.wav")
    }

    private var soundIdleInst = soundIdle.loop(0f, 1f, 0f)
    private var soundExhaustInst = soundExhaust.loop(0f, 1f, 0f)
    private var soundCrankShaftInst = soundCrankShaft.loop(0f, 0.5f, 0f)
    private var soundDryClutchInst = soundDryClutch.loop(0f, 1f, 0f)
    private var soundBrakeDiscInst = soundBrakeDisc.loop(0f, 1f, 0f)
    private var soundBackfireInst: Long = -1
    private var soundKneeDraggingInst: Long = -1
    private var soundKneeDragging2Inst: Long = -1
    private var soundCrashInst: Long = -1

    private fun getEngineGearOrder(e: Float): Int {
        var range = 0
        var rangeStart = 0f
        var rangeEnd = 0f
        for (i in gearBoxRangesCombined.indices) {
            rangeEnd += gearBoxRangesCombined[i]
            if (rangeStart <= e && e <= rangeEnd) {
                range = i
            }
            rangeStart += gearBoxRangesCombined[i]
        }
        return range
    }

    private fun getEngineOutputGearboxed(e: Float): Float {
        var range = 0
        var rangeStart = 0f
        var rangeEnd = 0f
        var rangedE = 0f
        for (i in gearBoxRangesCombined.indices) {
            rangeEnd += gearBoxRangesCombined[i]
            if (rangeStart <= e && e <= rangeEnd) {
                range = i
                rangedE = (e - rangeStart) / gearBoxRangesCombined[i]
            }
            rangeStart += gearBoxRangesCombined[i]
        }

        return if (range == 0) {
            rangedE
        } else {
            rangedE * (1f - gearBoxBase) + gearBoxBase
        }
    }

    fun gamePause() {
        muteMainSFX()
    }

    fun gameResume() {}

    private fun muteMainSFX() {
        soundIdle.setVolume(soundIdleInst, 0f)
        soundExhaust.setVolume(soundExhaustInst, 0f)
        soundCrankShaft.setVolume(soundCrankShaftInst, 0f)
        soundDryClutch.setVolume(soundDryClutchInst, 0f)
        soundBrakeDisc.setVolume(soundBrakeDiscInst, 0f)
        if (soundKneeDraggingInst != -1L) soundKneeDragging.stop(soundKneeDraggingInst)
        if (soundKneeDragging2Inst != -1L) soundKneeDragging2.stop(soundKneeDragging2Inst)
        soundKneeDraggingInst = -1L
        soundKneeDragging2Inst = -1L
    }

    private var lastEngineOutputNoise = 0f
    private var lastEngineOutput = 0f
    private var lastEngineMeter = 0f
    private var lastEngineGearOrder = 0
    private var engineAcceBackfireTime = -1f
    private var engineNoOutputTime = 0f
    private var backFireTime = 0f
    private var backFireSize = 0f
    private var lastKneeDragging = false
    private var timeSinceStandBy = 0f
    private var halfClutchPitch = 0f
    private var lastExhaustPitch = 0f
    private var lastMotorcycleStandBy = false

    fun update(delta: Float) {
        if (motorcycle.logic.state.isCrashed) {
            muteMainSFX()
            timeSinceStandBy = 0f
        } else {
            val engineOutput = motorcycle.logic.getEngineOutputPercentage()
            val engineOutputRaw = motorcycle.logic.getRawEngineAndBrakeMeter()
            val engineGearOrder = getEngineGearOrder(engineOutput)

            if (motorcycle.logic.state.isStandBy) {
                timeSinceStandBy = 0f
                engineNoOutputTime = 0f
            } else {
                timeSinceStandBy += delta
                if (engineOutput == 0f)
                    engineNoOutputTime = 0f
                else
                    engineNoOutputTime += delta
            }

            val halfClutchPitchMax = 1f
            val halfClutchPitchIncreaseFactor = 10f
            val halfClutchPitchDecreaseFactor = 2f
            if (engineNoOutputTime <= 0.5f && engineOutputRaw > 0f)
                halfClutchPitch += halfClutchPitchIncreaseFactor * delta
            else
                halfClutchPitch -= halfClutchPitchDecreaseFactor * delta
            if (halfClutchPitch > 1f) halfClutchPitch = 1f
            if (halfClutchPitch < 0f) halfClutchPitch = 0f

            if (motorcycle.logic.state.isStandBy) {
                if (!lastMotorcycleStandBy) {
                    if (lastExhaustPitch > 1f)
                        halfClutchPitch = (lastExhaustPitch - 1f) / halfClutchPitchMax
                    else
                        halfClutchPitch = 0f
                }
                if (halfClutchPitch > 0f) {
                    soundIdle.setVolume(soundIdleInst, 0f)
                    soundExhaust.setVolume(soundExhaustInst, 1f)
                    soundExhaust.setPitch(soundExhaustInst, 1f + halfClutchPitch * halfClutchPitchMax)
                    lastExhaustPitch = 1f + halfClutchPitch * halfClutchPitchMax
                    soundCrankShaft.setVolume(soundCrankShaftInst, 1f)
                    soundCrankShaft.setPitch(soundCrankShaftInst, 1f + halfClutchPitch * halfClutchPitchMax)
                    soundDryClutch.setVolume(soundDryClutchInst, 0f)
                    soundBrakeDisc.setVolume(soundBrakeDiscInst, 0f)
                } else {
                    soundIdle.setVolume(soundIdleInst, 1f)
                    soundExhaust.setVolume(soundExhaustInst, 0f)
                    lastExhaustPitch = 0f
                    soundCrankShaft.setVolume(soundCrankShaftInst, 0f)
                    soundDryClutch.setVolume(soundDryClutchInst, 1f)
                    soundDryClutch.setPitch(soundDryClutchInst, 1f)
                    soundBrakeDisc.setVolume(soundBrakeDiscInst, 0f)
                }
            } else {
                val customCrankShaftVolume: Boolean
                if (engineOutput == 0f && timeSinceStandBy >= 1f) {
                    soundIdle.setVolume(soundIdleInst, 1f)
                    soundExhaust.setVolume(soundExhaustInst, 0f)
                    soundDryClutch.setVolume(soundDryClutchInst, 1f)
                    soundDryClutch.setPitch(soundDryClutchInst, 1.5f)
                    customCrankShaftVolume = false
                } else {
                    soundIdle.setVolume(soundIdleInst, 0f)
                    soundExhaust.setVolume(soundExhaustInst, 1f)
                    soundDryClutch.setVolume(soundDryClutchInst, 0f)
                    soundDryClutch.setPitch(soundDryClutchInst, 1f)
                    customCrankShaftVolume = true
                }

                if (engineOutput == 0f)
                    engineNoOutputTime = 0f
                else
                    engineNoOutputTime += delta

                var engineOutputGeared = getEngineOutputGearboxed(engineOutput)
                val engineOutputNoiseRange = 0.3f
                val engineOutputNoise = Random.nextFloat() * engineOutputNoiseRange - engineOutputNoiseRange * 0.5f
                lastEngineOutputNoise = engineOutputNoise * 0.1f + lastEngineOutputNoise * 0.9f
                engineOutputGeared += lastEngineOutputNoise
                val leanFactor = (90f - abs(motorcycle.logic.state.leanAngle) * 0.333f) / 90f
                var engineSoundPitch = max(
                    0.5f + (engineOutputGeared * 1.5f * leanFactor).toFloat(),
                    1f + halfClutchPitch * halfClutchPitchMax
                )
                if (engineSoundPitch < 0.5f) engineSoundPitch = 0.5f
                if (engineSoundPitch > 2f) engineSoundPitch = 2f
                soundExhaust.setPitch(soundExhaustInst, engineSoundPitch)
                lastExhaustPitch = engineSoundPitch

                val soundCrankShaftPitch = engineSoundPitch
                soundCrankShaft.setPitch(soundCrankShaftInst, soundCrankShaftPitch)
                if (!customCrankShaftVolume)
                    soundCrankShaft.setVolume(soundCrankShaftInst, 0f)
                else if (engineOutput > lastEngineOutput)
                    soundCrankShaft.setVolume(soundCrankShaftInst, 1f)
                else
                    soundCrankShaft.setVolume(soundCrankShaftInst, 0.5f)
                
                if (lastEngineOutput > engineOutput && abs(motorcycle.logic.state.leanAngle) <= 30f)
                    soundBrakeDisc.setVolume(soundBrakeDiscInst, engineOutput)
                else
                    soundBrakeDisc.setVolume(soundBrakeDiscInst, 0f)
                lastEngineOutput = engineOutput
            }

            if (engineGearOrder < lastEngineGearOrder) {
                backFireTime = 0.5f
                backFireSize = 0.25f
                if (soundBackfireInst != -1L) {
                    soundBackfire.stop(soundBackfireInst)
                    soundBackfireInst = -1L
                }
                soundBackfireInst = soundBackfire.play(1f, 1f, 0f)
                backfireReporter.reportBackfire(0.5f)
                haptics.vibrate(50)
            }
            lastEngineGearOrder = engineGearOrder

            val engineMeter = motorcycle.logic.getEngineAndBrakeMeter()
            if (engineMeter > 0f && lastEngineMeter <= 0f) engineAcceBackfireTime = 0f
            if (engineMeter <= 0f && engineAcceBackfireTime >= 0f) {
                if (engineAcceBackfireTime > 1f) {
                    backFireTime = 1f
                    backFireSize = 0.5f
                    if (soundBackfireInst != -1L) {
                        soundBackfire.stop(soundBackfireInst)
                        soundBackfireInst = -1L
                    }
                    soundBackfireInst = soundBackfire.play(2f, 1f, 0f)
                    backfireReporter.reportBackfire(1f)
                    haptics.vibrate(100)
                }
                engineAcceBackfireTime = -1f
            }
            if (engineAcceBackfireTime >= 0f) engineAcceBackfireTime += delta
            lastEngineMeter = engineMeter

            if (backFireTime > 0f) {
                if (Random.nextFloat() < 0.025f) {
                    if (soundBackfireInst != -1L) {
                        soundBackfire.stop(soundBackfireInst)
                        soundBackfireInst = -1L
                    }
                    soundBackfireInst = soundBackfire.play(backFireSize * 2f, 1f, 0f)
                    backfireReporter.reportBackfire(backFireSize)
                    haptics.vibrate(50)
                }
                backFireTime -= delta
                if (backFireTime < 0f) backFireTime = 0f
            }

            val kneeDragging = motorcycle.rider?.logic?.isKneeDragging() ?: false
            if (kneeDragging) {
                if (!lastKneeDragging) {
                    soundKneeDraggingInst = soundKneeDragging.play(0.75f)
                    soundKneeDragging2Inst = soundKneeDragging.loop(0.75f)
                }
            } else {
                if (soundKneeDraggingInst != -1L) soundKneeDragging.stop(soundKneeDraggingInst)
                if (soundKneeDragging2Inst != -1L) soundKneeDragging2.stop(soundKneeDragging2Inst)
                soundKneeDraggingInst = -1L
                soundKneeDragging2Inst = -1L
            }

            lastKneeDragging = kneeDragging
            lastMotorcycleStandBy = motorcycle.logic.state.isStandBy
        }
    }

    fun playCrashSound() {
        if (soundCrashInst != -1L) {
            soundCrash.stop(soundCrashInst)
            soundCrashInst = -1L
        }
        soundCrashInst = soundCrash.play()
        haptics.vibrate(100)
    }

    fun clear() {
        lastEngineOutput = 0f
        lastEngineMeter = 0f
        lastEngineGearOrder = 0
        engineAcceBackfireTime = -1f
        engineNoOutputTime = 0f
        backFireTime = 0f
        backFireSize = 0f
        lastKneeDragging = false
        timeSinceStandBy = 0f
        halfClutchPitch = 0f
        lastExhaustPitch = 0f
        lastMotorcycleStandBy = false

        if (soundKneeDraggingInst != -1L) soundKneeDragging.stop(soundKneeDraggingInst)
        if (soundKneeDragging2Inst != -1L) soundKneeDragging2.stop(soundKneeDragging2Inst)
        soundKneeDraggingInst = -1L
        soundKneeDragging2Inst = -1L

        if (soundBackfireInst != -1L) soundBackfire.stop(soundBackfireInst)
        soundBackfireInst = -1L

        if (soundCrashInst != -1L) soundCrash.stop(soundCrashInst)
        soundCrashInst = -1L
    }

    fun dispose() {
        clear()
        soundIdle.stop(soundIdleInst)
        soundExhaust.stop(soundExhaustInst)
        soundCrankShaft.stop(soundCrankShaftInst)
        soundDryClutch.stop(soundDryClutchInst)
        soundBrakeDisc.stop(soundBrakeDiscInst)
        
        soundIdle.dispose()
        soundDryClutch.dispose()
        soundExhaust.dispose()
        soundCrankShaft.dispose()
        soundBrakeDisc.dispose()
        soundBackfire.dispose()
        soundKneeDragging.dispose()
        soundKneeDragging2.dispose()
        soundCrash.dispose()
    }
}
