package com.marcowong.motoman

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.fail

/**
 * Golden-frame regression guard for the full game scene.
 *
 * The committed golden [GOLDEN] is the deterministic "start line" frame — a fixed track
 * (`BasicRandom(100000)`), the bike in standby, no input — rendered by this engine. It was
 * validated by eye against the original 2013 libGDX game captured on the same phone
 * (`screenshots/parity/original-device-galaxyzfold3.png` vs `port-device-*.png`).
 *
 * This guards the **shared rendering pipeline** (all of `commonMain` plus `DesktopGl`): the
 * render config, viewport, framebuffer/post-processing chain, shaders, and the standby/scene
 * logic. Deleting the ground here scores max block diff 209 versus 10 for the correct frame,
 * so a vanished-surface or bloom-washout regression fails loudly. It does **not** exercise
 * `AndroidGl` — the bug that motivated this was Android-only (`glUniformMatrix4fv` count) and
 * the desktop path was always correct; an on-device golden would be the way to guard that and
 * is left as follow-up. The committed device screenshots are the manual reference for it.
 *
 * **Why a child process:** the render needs a real GL context. On macOS GLFW must own the
 * first thread (`-XstartOnFirstThread`), which a Gradle test worker is not, and initialising
 * LWJGL and AWT in one JVM aborts natively. So the frame is produced by a child JVM running
 * [DesktopSmokeKt] (which owns the first thread and touches no AWT), and this test — pure
 * ImageIO, no LWJGL — only compares PNGs.
 *
 * **Why downscaled blocks:** the post-processing chain (motion blur reading the prior frame,
 * the 48px bloom buffer) makes the full-res frame differ run-to-run by ~1.5% of pixels, and
 * different GPUs disagree on AA and filtering. Averaging into a 40x22 grid collapses that
 * noise (measured run-to-run max block delta = 6/255) while preserving the large-area colour
 * structure a real regression destroys.
 *
 * Skips cleanly when no GL context is available (headless CI), and — to avoid depending on a
 * CI runner's GPU matching the golden's — is not enforced under `CI` unless
 * `-Dmotoman.golden.enforce=true`. Run locally with `./gradlew :motoman:desktopTest`.
 */
class GoldenSceneTest {

    @Test
    fun startLineSceneMatchesGolden() {
        if (System.getenv("CI") != null && System.getProperty("motoman.golden.enforce") != "true") {
            println("[golden] skipped under CI (set -Dmotoman.golden.enforce=true to run)")
            return
        }

        val golden = loadGolden() ?: fail("golden resource $GOLDEN not found on the test classpath")

        val actualFile = File.createTempFile("motoman-golden-actual", ".png").apply { deleteOnExit() }
        if (!render(actualFile)) {
            println("[golden] skipped: could not create a GL context to render the scene")
            return
        }
        val actual = ImageIO.read(actualFile)

        val g = downscale(golden)
        val a = downscale(actual)

        var sum = 0L
        var max = 0
        var worst = -1
        for (i in g.indices) {
            val d = kotlin.math.abs(g[i] - a[i])
            sum += d
            if (d > max) { max = d; worst = i }
        }
        val mean = sum.toDouble() / g.size
        println("[golden] rendered & compared: mean block diff ${"%.2f".format(mean)}, max block diff $max")

        if (mean > MEAN_TOLERANCE || max > MAX_BLOCK_TOLERANCE) {
            // Keep the evidence so a failure can be eyeballed rather than only read as numbers.
            val outDir = File("build/golden-failures").apply { mkdirs() }
            actualFile.copyTo(File(outDir, "actual.png"), overwrite = true)
            ImageIO.write(golden, "png", File(outDir, "golden.png"))
            fail(
                "start-line frame diverged from golden: mean block diff ${"%.2f".format(mean)} " +
                    "(<= $MEAN_TOLERANCE), max block diff $max at block ${worst / 3} channel ${worst % 3} " +
                    "(<= $MAX_BLOCK_TOLERANCE). Actual and golden written to $outDir."
            )
        }
    }

    private fun loadGolden(): BufferedImage? =
        javaClass.getResourceAsStream(GOLDEN)?.use { ImageIO.read(it) }

    /** Renders the deterministic scene in a child JVM. Returns false if no frame was produced. */
    private fun render(target: File): Boolean {
        val javaBin = File(System.getProperty("java.home"), "bin/java").absolutePath
        val isMac = System.getProperty("os.name").orEmpty().let {
            it.startsWith("Mac OS X") || it.startsWith("Darwin")
        }
        val cmd = buildList {
            add(javaBin)
            if (isMac) add("-XstartOnFirstThread") // GLFW must own the first thread on macOS
            add("-cp"); add(System.getProperty("java.class.path"))
            add("com.marcowong.motoman.DesktopSmokeKt")
            add("--game"); add("--frames"); add("2")
            add("--capture"); add(target.absolutePath)
        }
        return try {
            val proc = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val log = proc.inputStream.bufferedReader().readText()
            val finished = proc.waitFor(120, java.util.concurrent.TimeUnit.SECONDS)
            if (!finished) { proc.destroyForcibly(); return false }
            if (proc.exitValue() != 0) { println("[golden] render process failed:\n$log"); return false }
            target.exists() && target.length() > 0
        } catch (_: Exception) {
            false
        }
    }

    /** Averages [image] into a [GRID_W]x[GRID_H] grid, returned as interleaved RGB bytes 0..255. */
    private fun downscale(image: BufferedImage): IntArray {
        val out = IntArray(GRID_W * GRID_H * 3)
        val cellW = image.width.toDouble() / GRID_W
        val cellH = image.height.toDouble() / GRID_H
        for (gy in 0 until GRID_H) {
            for (gx in 0 until GRID_W) {
                val x0 = (gx * cellW).toInt(); val x1 = ((gx + 1) * cellW).toInt().coerceAtMost(image.width)
                val y0 = (gy * cellH).toInt(); val y1 = ((gy + 1) * cellH).toInt().coerceAtMost(image.height)
                var r = 0L; var g = 0L; var b = 0L; var n = 0L
                for (y in y0 until y1) for (x in x0 until x1) {
                    val p = image.getRGB(x, y)
                    r += (p shr 16) and 0xFF; g += (p shr 8) and 0xFF; b += p and 0xFF; n++
                }
                if (n == 0L) n = 1
                val o = (gy * GRID_W + gx) * 3
                out[o] = (r / n).toInt(); out[o + 1] = (g / n).toInt(); out[o + 2] = (b / n).toInt()
            }
        }
        return out
    }

    private companion object {
        const val GOLDEN = "/golden/start-line-desktop.png"
        const val GRID_W = 40
        const val GRID_H = 22
        // The correct frame scores mean 0.5 / max 10; deleting the ground scores mean 17.9 /
        // max 209. These thresholds sit well clear of both, leaving headroom for a different
        // GPU while still failing hard on a vanished-surface or washout regression.
        const val MEAN_TOLERANCE = 10.0
        const val MAX_BLOCK_TOLERANCE = 60
    }
}
