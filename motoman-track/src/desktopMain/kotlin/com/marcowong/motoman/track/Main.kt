package com.marcowong.motoman.track

import java.awt.Canvas
import java.awt.Frame
import java.awt.Graphics
import java.awt.Event
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import kotlin.math.roundToInt

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val trackGenerator = TrackGenerator()
        trackGenerator.random = BasicRandom(999940)
        trackGenerator.trackLen = 300f
        trackGenerator.turnAngleZeroFactor = 0.3f
        trackGenerator.segLen = 2f
        trackGenerator.segWidth = 1f
        trackGenerator.segPad = 0.5f
        
        val tss = trackGenerator.generate()?.trackSegments ?: return
        TrackGuideGenerator().generate(tss)

        val frame = Frame("motoman-track")
        val canvas = object : Canvas() {
            private var ox = 0
            private var oy = 0

            override fun paint(g: Graphics) {
                super.paint(g)
                g.translate(width / 2 + ox, height / 2 + oy)

                for (ts in tss) {
                    val tsl = ts.attributes["lines"] as TrackSegLines
                    val dir = ts.attributes["directionNotice"] as? TrackDirection
                    val scale = 8f

                    g.drawLine((tsl.h.x1 * scale).roundToInt(), (tsl.h.y1 * scale).roundToInt(), (tsl.h.x2 * scale).roundToInt(), (tsl.h.y2 * scale).roundToInt())
                    g.drawLine((tsl.t.x1 * scale).roundToInt(), (tsl.t.y1 * scale).roundToInt(), (tsl.t.x2 * scale).roundToInt(), (tsl.t.y2 * scale).roundToInt())
                    g.drawLine((tsl.l.x1 * scale).roundToInt(), (tsl.l.y1 * scale).roundToInt(), (tsl.l.x2 * scale).roundToInt(), (tsl.l.y2 * scale).roundToInt())
                    g.drawLine((tsl.r.x1 * scale).roundToInt(), (tsl.r.y1 * scale).roundToInt(), (tsl.r.x2 * scale).roundToInt(), (tsl.r.y2 * scale).roundToInt())

                    if (dir != null) {
                        g.drawString(dir.name, (ts.x1 * scale).roundToInt(), (ts.y1 * scale).roundToInt())
                    }
                }
            }

            override fun keyUp(evt: Event, key: Int): Boolean {
                when (key.toChar()) {
                    'a' -> ox += 20
                    'd' -> ox -= 20
                    'w' -> oy += 20
                    's' -> oy -= 20
                }
                this.repaint()
                return super.keyUp(evt, key)
            }
        }
        frame.add(canvas)
        frame.setSize(640, 480)
        canvas.setSize(640, 480)
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(we: WindowEvent) {
                System.exit(0)
            }
        })
        frame.isVisible = true
        canvas.invalidate()
    }
}
