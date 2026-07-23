package com.marcowong.motoman.track

/**
 * Pure-Kotlin reimplementation of the exact `java.util.Random` linear congruential
 * generator used by the original engine. Kept bit-identical to preserve the seeded,
 * reproducible track generation the verification plan depends on, while removing the
 * JVM-only dependency so this code is valid in `commonMain` (Android, desktop, and a
 * future Kotlin/Native target).
 *
 * Algorithm per the `java.util.Random` specification:
 *   seed        = (initialSeed xor 0x5DEECE66D) and ((1 shl 48) - 1)
 *   next(bits)  : seed = (seed * 0x5DEECE66D + 0xB) and mask48; (seed ushr (48 - bits))
 *   nextFloat() : next(24) / (1 shl 24)
 */
class BasicRandom(seed: Int) : IRandom {
    private var state: Long = (seed.toLong() xor MULTIPLIER) and MASK_48

    private fun next(bits: Int): Int {
        state = (state * MULTIPLIER + INCREMENT) and MASK_48
        return (state ushr (48 - bits)).toInt()
    }

    override fun next(): Float = next(24) / FLOAT_UNIT

    private companion object {
        const val MULTIPLIER = 0x5DEECE66DL
        const val INCREMENT = 0xBL
        const val MASK_48 = (1L shl 48) - 1
        const val FLOAT_UNIT = (1 shl 24).toFloat()
    }
}
