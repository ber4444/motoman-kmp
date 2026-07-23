package com.marcowong.motoman.track

import java.util.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Proves the pure-Kotlin [BasicRandom] produces a bit-identical sequence to the
 * original `java.util.Random`-backed implementation. This guards the seeded,
 * reproducible track generation the verification plan relies on.
 */
class BasicRandomParityTest {

    @Test
    fun matchesJavaUtilRandomAcrossSeeds() {
        val seeds = intArrayOf(0, 1, -1, 100000, 999940, Int.MAX_VALUE, Int.MIN_VALUE, 42, -12345)
        for (seed in seeds) {
            val ours = BasicRandom(seed)
            val reference = Random(seed.toLong())
            repeat(10_000) { i ->
                assertEquals(
                    reference.nextFloat(),
                    ours.next(),
                    "Divergence at seed=$seed, draw=$i",
                )
            }
        }
    }
}
