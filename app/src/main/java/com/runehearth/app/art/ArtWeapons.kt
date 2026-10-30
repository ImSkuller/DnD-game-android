package com.runehearth.app.art

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal fun starPoints(cx: Float, cy: Float, r1: Float, r2: Float, n: Int, rot: Float = -90f): FloatArray {
    val out = FloatArray(n * 4)
    for (i in 0 until n * 2) {
        val r = if (i % 2 == 0) r1 else r2
        val a = (rot + i * 180f / n) * PI.toFloat() / 180f
        out[i * 2] = cx + r * cos(a); out[i * 2 + 1] = cy + r * sin(a)
    }
    return out
}

private fun Sketch.hilt(y: Float, guardW: Float, grip: Float, gem: Boolean = false, pommel: Float = 4f) {
    rrect(Tone.GOLD, 50 - guardW, y, guardW * 2, 5f, 2f)
    rect(Tone.LIGHT, 50 - guardW + 2, y + 1f, guardW * 2 - 4, 1.2f, outline = false, alpha = 0.7f)
    rrect(Tone.PRIMARY, 46.5f, y + 5f, 7f, grip, 2f)
    for (i in 0 until (grip / 4).toInt()) line(Tone.SHADE, 46.5f, y + 8f + i * 4f, 53.5f, y + 6f + i * 4f, 1.1f)
    circle(Tone.GOLD, 50f, y + 5f + grip + pommel - 1f, pommel)
    if (gem) circle(Tone.GEM, 50f, y + 2.5f, 2.2f)
}

private fun Sketch.straightBlade(top: Float, len: Float, w: Float, fuller: Boolean = true, tipLen: Float = 12f) {
    val bb = top + len
    poly(Tone.BLADE, 50f, top, 50f, top, 50f - w, top + tipLen, 50f - w, bb, 50f, bb)
    poly(Tone.BLADE_DARK, 50f, top, 50f + w, top + tipLen, 50f + w, bb, 50f, bb)
    if (fuller) line(Tone.METAL_LIGHT, 50f, top + tipLen + 2, 50f, bb - 3, 1.2f, 0.8f)
}

object WeaponPainter : Painter {
    override fun paint(design: Int, s: Sketch) {
        s.rot = 40f; s.scale = 0.84f
        when (design) {
            0 -> { s.straightBlade(10f, 40f, 6f); s.hilt(50f, 12f, 12f) }
            1 -> { s.straightBlade(6f, 50f, 5.5f); s.hilt(56f, 14f, 13f, gem = true) }
            2 -> { s.straightBlade(8f, 46f, 9f, tipLen = 16f); s.hilt(54f, 17f, 13f, pommel = 5f) }
            3 -> { s.straightBlade(4f, 56f, 2.8f, fuller = false, tipLen = 10f); s.hilt(60f, 15f, 12f, pommel = 3.5f)
                s.strokePath(Tone.GOLD, 2f) { moveTo(36f, 62f); quadraticTo(50f, 78f, 64f, 62f) } }
            4 -> {
                s.path(Tone.BLADE) { moveTo(56f, 6f); cubicTo(74f, 22f, 70f, 44f, 54f, 58f); lineTo(46f, 58f); cubicTo(58f, 42f, 60f, 24f, 50f, 12f); close() }
                s.path(Tone.BLADE_DARK, alpha = 0.7f) { moveTo(56f, 6f); cubicTo(74f, 22f, 70f, 44f, 54f, 58f); lineTo(51f, 58f); cubicTo(64f, 42f, 66f, 24f, 56f, 6f); close() }
                s.hilt(58f, 11f, 12f)
            }
            5 -> { s.straightBlade(24f, 30f, 5f, tipLen = 10f); s.hilt(54f, 10f, 11f, pommel = 3.5f) }
            6 -> {
                s.path(Tone.BLADE) { moveTo(50f, 4f); cubicTo(62f, 20f, 60f, 44f, 52f, 62f); lineTo(46f, 62f); lineTo(46f, 14f); close() }
                s.path(Tone.BLADE_DARK) { moveTo(50f, 4f); cubicTo(62f, 20f, 60f, 44f, 52f, 62f); lineTo(50f, 62f); close() }
                s.oval(Tone.GOLD, 50f, 63f, 9f, 3.4f); s.rrect(Tone.PRIMARY, 46.5f, 66f, 7f, 22f, 2f)
                for (i in 0 until 5) s.line(Tone.WHITE, 46.5f, 68f + i * 4f, 53.5f, 72f + i * 4f, 1f)
            }
            7 -> axe(s, double = false, big = true)
            8 -> axe(s, double = true, big = true)
            9 -> axe(s, double = false, big = false)
            10 -> mace(s, spiked = true)
            11 -> mace(s, spiked = false)
            12 -> {
                s.rrect(Tone.WOOD, 47.5f, 24f, 5f, 68f, 2f)
                s.rrect(Tone.BLADE_DARK, 26f, 8f, 48f, 26f, 4f); s.rrect(Tone.BLADE, 28f, 10f, 44f, 8f, 3f)
                s.rect(Tone.GOLD, 44f, 6f, 12f, 30f); s.circle(Tone.GEM, 50f, 21f, 3f)
                s.rrect(Tone.PRIMARY, 46f, 66f, 8f, 22f, 2f)
            }
            13 -> {
                s.rrect(Tone.WOOD, 47.5f, 24f, 5f, 70f, 2f)
                s.poly(Tone.BLADE, 50f, 2f, 58f, 16f, 54f, 28f, 46f, 28f, 42f, 16f); s.poly(Tone.BLADE_DARK, 50f, 2f, 58f, 16f, 54f, 28f, 50f, 28f)
                s.rect(Tone.PRIMARY, 44f, 28f, 12f, 4f)
            }
            14 -> {
                s.rrect(Tone.WOOD, 47.5f, 16f, 5f, 78f, 2f)
                s.poly(Tone.BLADE, 50f, 2f, 54f, 14f, 50f, 22f, 46f, 14f)
                s.path(Tone.BLADE) { moveTo(53f, 20f); cubicTo(82f, 14f, 84f, 44f, 56f, 48f); lineTo(53f, 38f); close() }
                s.poly(Tone.BLADE_DARK, 47f, 26f, 36f, 36f, 47f, 38f)
                s.rect(Tone.PRIMARY, 44f, 40f, 12f, 4f)
            }
            15 -> {
                s.rrect(Tone.WOOD, 47.5f, 30f, 5f, 64f, 2f)
                s.strokePath(Tone.BLADE, 4f) { moveTo(30f, 8f); lineTo(30f, 28f); quadraticTo(30f, 38f, 50f, 38f); quadraticTo(70f, 38f, 70f, 28f); lineTo(70f, 8f) }
                s.strokePath(Tone.BLADE, 4f) { moveTo(50f, 6f); lineTo(50f, 38f) }
                s.poly(Tone.BLADE_DARK, 30f, 2f, 34f, 12f, 26f, 12f); s.poly(Tone.BLADE_DARK, 50f, 0f, 54f, 10f, 46f, 10f); s.poly(Tone.BLADE_DARK, 70f, 2f, 74f, 12f, 66f, 12f)
                s.circle(Tone.GEM, 50f, 38f, 3.5f)
            }
            16 -> {
                s.rot = 0f; s.scale = 0.92f
                s.strokePath(Tone.WOOD, 5f) { moveTo(66f, 6f); cubicTo(20f, 26f, 20f, 74f, 66f, 94f) }
                s.strokePath(Tone.PRIMARY, 2.2f) { moveTo(60f, 14f); cubicTo(28f, 30f, 28f, 70f, 60f, 86f) }
                s.line(Tone.METAL_LIGHT, 66f, 6f, 66f, 94f, 1.3f)
                s.line(Tone.WOOD_DARK, 30f, 50f, 84f, 50f, 2f); s.poly(Tone.BLADE, 90f, 50f, 82f, 46f, 82f, 54f); s.poly(Tone.PRIMARY, 30f, 50f, 24f, 45f, 28f, 50f, 24f, 55f)
            }
            17 -> {
                s.rot = 0f
                s.rrect(Tone.WOOD, 44f, 20f, 12f, 66f, 3f); s.rrect(Tone.WOOD_DARK, 46f, 26f, 8f, 40f, 2f)
                s.strokePath(Tone.BLADE_DARK, 5f) { moveTo(10f, 36f); quadraticTo(50f, 18f, 90f, 36f) }
                s.line(Tone.METAL_LIGHT, 10f, 36f, 50f, 54f, 1.2f); s.line(Tone.METAL_LIGHT, 90f, 36f, 50f, 54f, 1.2f)
                s.poly(Tone.BLADE, 50f, 6f, 54f, 30f, 46f, 30f); s.circle(Tone.GOLD, 50f, 56f, 3.5f); s.circle(Tone.PRIMARY, 50f, 86f, 4f)
            }
            18 -> {
                s.rrect(Tone.WOOD, 47f, 30f, 6f, 64f, 2.5f)
                for (i in 0 until 4) s.line(Tone.WOOD_DARK, 47f, 40f + i * 14f, 53f, 36f + i * 14f, 1.3f)
                s.strokePath(Tone.GOLD, 3.2f) { moveTo(40f, 8f); quadraticTo(36f, 30f, 50f, 34f); quadraticTo(64f, 30f, 60f, 8f) }
                s.circle(Tone.GEM, 50f, 20f, 11f); s.circle(Tone.LIGHT, 46f, 16f, 3.4f, outline = false, alpha = 0.8f); s.circle(Tone.WHITE, 45f, 15f, 1.6f, outline = false)
            }
            else -> {
                s.rot = 35f
                s.rrect(Tone.WOOD_DARK, 47f, 34f, 6f, 54f, 3f); s.rrect(Tone.GOLD, 45f, 30f, 10f, 6f, 2f)
                s.poly(Tone.GEM, 50f, 4f, 60f, 18f, 50f, 32f, 40f, 18f); s.poly(Tone.LIGHT, 50f, 4f, 60f, 18f, 50f, 18f, alpha = 0.8f)
                s.poly(Tone.GOLD, *starPoints(76f, 20f, 5f, 1.8f, 4)); s.poly(Tone.GOLD, *starPoints(24f, 30f, 4f, 1.4f, 4))
            }
        }
    }

    private fun axe(s: Sketch, double: Boolean, big: Boolean) {
        val k = if (big) 1f else 0.78f
        s.rrect(Tone.WOOD, 47.5f, 8f, 5f, 86f, 2f)
        for (i in 0 until 4) s.line(Tone.WOOD_DARK, 47.5f, 62f + i * 7f, 52.5f, 59f + i * 7f, 1.2f)
        s.path(Tone.BLADE) {
            moveTo(47f, 14f); cubicTo(50f - 36f * k, 2f, 50f - 40f * k, 40f * k + 6f, 50f - 22f * k, 50f * k + 6f)
            cubicTo(50f - 14f * k, 40f * k, 50f - 8f * k, 36f * k, 47f, 38f); close()
        }
        s.curve(Tone.METAL_LIGHT, 1.4f, 50f - 33f * k, 12f * k + 6f, 50f - 36f * k, 30f * k + 6f, alpha = 0.9f)
        if (double) {
            s.path(Tone.BLADE_DARK) {
                moveTo(53f, 14f); cubicTo(50f + 36f * k, 2f, 50f + 40f * k, 40f * k + 6f, 50f + 22f * k, 50f * k + 6f)
                cubicTo(50f + 14f * k, 40f * k, 50f + 8f * k, 36f * k, 53f, 38f); close()
            }
        } else s.poly(Tone.BLADE_DARK, 53f, 16f, 66f, 22f, 53f, 30f)
        s.rect(Tone.GOLD, 45f, 34f, 10f, 4f); s.circle(Tone.GEM, 50f, 26f, 2.6f)
    }

    private fun mace(s: Sketch, spiked: Boolean) {
        s.rrect(Tone.WOOD, 47.5f, 28f, 5f, 66f, 2f)
        if (spiked) {
            val pts = starPoints(50f, 22f, 23f, 14f, 8)
            s.poly(Tone.BLADE_DARK, *pts)
            s.circle(Tone.BLADE, 50f, 22f, 13f); s.circle(Tone.LIGHT, 46f, 18f, 4f, outline = false, alpha = 0.6f)
        } else {
            for (i in 0 until 6) {
                val a = (i * 60f) * PI.toFloat() / 180f
                val cx = 50f + 14f * cos(a); val cy = 22f + 14f * sin(a)
                s.circle(Tone.BLADE_DARK, cx, cy, 7f)
            }
            s.circle(Tone.BLADE, 50f, 22f, 12f); s.circle(Tone.GEM, 50f, 22f, 4f)
        }
        s.rect(Tone.GOLD, 44f, 40f, 12f, 4f); s.rrect(Tone.PRIMARY, 46f, 66f, 8f, 20f, 2f)
    }
}

object ShieldPainter : Painter {
    override fun paint(design: Int, s: Sketch) {
        s.scale = 0.92f
        val shape = design % 5
        val emblem = design / 5
        when (shape) {
            0 -> { // round
                s.circle(Tone.METAL_DARK, 50f, 50f, 42f); s.circle(Tone.PRIMARY, 50f, 50f, 36f)
            }
            1 -> { // heater
                s.path(Tone.METAL_DARK) { moveTo(12f, 10f); lineTo(88f, 10f); lineTo(88f, 48f); cubicTo(88f, 74f, 66f, 88f, 50f, 96f); cubicTo(34f, 88f, 12f, 74f, 12f, 48f); close() }
                s.path(Tone.PRIMARY) { moveTo(18f, 16f); lineTo(82f, 16f); lineTo(82f, 47f); cubicTo(82f, 70f, 64f, 82f, 50f, 89f); cubicTo(36f, 82f, 18f, 70f, 18f, 47f); close() }
            }
            2 -> { // kite
                s.poly(Tone.METAL_DARK, 50f, 4f, 84f, 28f, 74f, 64f, 50f, 96f, 26f, 64f, 16f, 28f)
                s.poly(Tone.PRIMARY, 50f, 11f, 77f, 30f, 68f, 62f, 50f, 88f, 32f, 62f, 23f, 30f)
            }
            3 -> { // tower
                s.rrect(Tone.METAL_DARK, 20f, 6f, 60f, 88f, 8f); s.rrect(Tone.PRIMARY, 26f, 12f, 48f, 76f, 5f)
            }
            else -> { // hex
                s.poly(Tone.METAL_DARK, 50f, 4f, 88f, 26f, 88f, 70f, 50f, 94f, 12f, 70f, 12f, 26f)
                s.poly(Tone.PRIMARY, 50f, 12f, 80f, 30f, 80f, 66f, 50f, 86f, 20f, 66f, 20f, 30f)
            }
        }
        when (emblem) {
            0 -> { s.rect(Tone.GOLD, 46f, 22f, 8f, 56f); s.rect(Tone.GOLD, 28f, 40f, 44f, 8f) }                       // cross
            1 -> { s.poly(Tone.GOLD, 24f, 30f, 50f, 50f, 76f, 30f, 76f, 42f, 50f, 62f, 24f, 42f); s.poly(Tone.LIGHT, 24f, 54f, 50f, 72f, 76f, 54f, 76f, 62f, 50f, 80f, 24f, 62f) } // chevrons
            2 -> { s.poly(Tone.GOLD, *starPoints(50f, 50f, 24f, 10f, 5)); s.circle(Tone.GEM, 50f, 50f, 5f) }          // star
            else -> { s.circle(Tone.GOLD, 50f, 50f, 12f); s.circle(Tone.METAL_LIGHT, 50f, 50f, 7f); s.circle(Tone.GEM, 50f, 50f, 3.5f); s.rect(Tone.GOLD, 48f, 14f, 4f, 20f); s.rect(Tone.GOLD, 48f, 66f, 4f, 20f) } // boss
        }
        s.circle(Tone.WHITE, 36f, 26f, 5f, outline = false, alpha = 0.5f)
    }
}
