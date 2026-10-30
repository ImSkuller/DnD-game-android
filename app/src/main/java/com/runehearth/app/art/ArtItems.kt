package com.runehearth.app.art

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private fun Sketch.leaf(x: Float, y: Float, len: Float, w: Float, deg: Float, tone: Tone = Tone.LEAF) {
    val a = deg * PI.toFloat() / 180f
    val dx = cos(a); val dy = sin(a)
    val tx = x + dx * len; val ty = y + dy * len
    val mx = x + dx * len * 0.5f; val my = y + dy * len * 0.5f
    path(tone) {
        moveTo(x, y)
        quadraticTo(mx - dy * w, my + dx * w, tx, ty)
        quadraticTo(mx + dy * w, my - dx * w, x, y)
        close()
    }
    line(Tone.LEAF_DARK, x, y, tx, ty, 0.9f, 0.7f)
}

object AccessoryPainter : Painter {
    override fun paint(design: Int, s: Sketch) {
        s.scale = 0.95f
        when (design) {
            0 -> { s.circle(Tone.GOLD, 50f, 56f, 28f); s.strokePath(Tone.GOLD_DARK, 1.4f) { addOval(androidx.compose.ui.geometry.Rect(31f, 37f, 69f, 75f)) }
                s.path(Tone.DARK) { addOval(androidx.compose.ui.geometry.Rect(36f, 42f, 64f, 70f)) } }
            1 -> { ring(s); s.poly(Tone.GEM, 50f, 10f, 62f, 24f, 50f, 36f, 38f, 24f); s.poly(Tone.LIGHT, 50f, 10f, 62f, 24f, 50f, 24f, alpha = 0.8f) }
            2 -> { chain(s); s.circle(Tone.GOLD, 50f, 62f, 20f); s.circle(Tone.PRIMARY, 50f, 62f, 14f); s.circle(Tone.GEM, 50f, 62f, 6f) }
            3 -> { chain(s); s.path(Tone.GEM) { moveTo(50f, 38f); cubicTo(74f, 62f, 68f, 86f, 50f, 88f); cubicTo(32f, 86f, 26f, 62f, 50f, 38f); close() }; s.circle(Tone.WHITE, 42f, 62f, 4f, outline = false) }
            4 -> { chain(s); s.rect(Tone.GOLD, 45f, 40f, 10f, 46f); s.rect(Tone.GOLD, 32f, 52f, 36f, 9f); s.circle(Tone.GEM, 50f, 57f, 3f) }
            5 -> { s.strokePath(Tone.LEATHER_DARK, 1.6f) { moveTo(14f, 16f); quadraticTo(50f, 100f, 86f, 16f) }; for (i in 0 until 7) { val t = i / 6f; val x = 14f + 72f * t; val y = 16f + 68f * t * (1 - t); s.circle(if (i % 2 == 0) Tone.PRIMARY else Tone.GOLD, x, y, 5.5f) } }
            6 -> { s.rrect(Tone.LEATHER, 8f, 40f, 84f, 20f, 4f); s.rect(Tone.GOLD, 38f, 36f, 24f, 28f); s.rect(Tone.LEATHER_DARK, 43f, 41f, 14f, 18f); s.circle(Tone.GEM, 50f, 50f, 4f); for (i in 0 until 5) s.circle(Tone.GOLD, 14f + i * 5f, 50f, 1.4f) }
            7 -> { s.path(Tone.GOLD) { addOval(androidx.compose.ui.geometry.Rect(16f, 30f, 84f, 82f)) }; s.path(Tone.DARK) { addOval(androidx.compose.ui.geometry.Rect(26f, 38f, 74f, 74f)) }; s.circle(Tone.GEM, 50f, 30f, 8f); s.circle(Tone.GEM, 24f, 56f, 5f); s.circle(Tone.GEM, 76f, 56f, 5f) }
            8 -> { for (x in listOf(34f, 66f)) { s.strokePath(Tone.GOLD, 2.4f) { moveTo(x, 10f); lineTo(x, 28f) }; s.circle(Tone.GOLD, x, 30f, 4f); s.poly(Tone.GEM, x, 36f, x + 8f, 50f, x, 70f, x - 8f, 50f) } }
            9 -> { s.poly(Tone.GOLD, 14f, 78f, 14f, 30f, 32f, 52f, 50f, 20f, 68f, 52f, 86f, 30f, 86f, 78f); s.rect(Tone.GOLD_DARK, 14f, 70f, 72f, 8f); s.circle(Tone.GEM, 50f, 50f, 5f); s.circle(Tone.PRIMARY, 28f, 60f, 3.5f); s.circle(Tone.PRIMARY, 72f, 60f, 3.5f); for (x in listOf(14f, 50f, 86f)) s.circle(Tone.METAL_LIGHT, x, 24f + (if (x == 50f) -4f else 6f), 3.2f) }
            10 -> { s.strokePath(Tone.GOLD, 3f) { moveTo(12f, 68f); quadraticTo(50f, 30f, 88f, 68f) }; s.poly(Tone.GEM, 50f, 28f, 58f, 42f, 50f, 54f, 42f, 42f); s.circle(Tone.GEM, 24f, 56f, 3.5f); s.circle(Tone.GEM, 76f, 56f, 3.5f) }
            11 -> { s.poly(Tone.GOLD, *starPoints(50f, 50f, 34f, 20f, 8)); s.circle(Tone.PRIMARY, 50f, 50f, 15f); s.circle(Tone.GEM, 50f, 50f, 8f) }
            12 -> { s.rrect(Tone.GOLD, 40f, 14f, 20f, 8f, 3f); s.line(Tone.GOLD, 50f, 18f, 50f, 84f, 3f); s.poly(Tone.PRIMARY, 50f, 22f, 82f, 34f, 50f, 46f); s.circle(Tone.GOLD, 50f, 84f, 4f) }
            13 -> { chain(s); s.circle(Tone.PAPER, 50f, 58f, 18f); s.rect(Tone.PAPER, 40f, 66f, 20f, 14f); s.circle(Tone.DARK, 43f, 56f, 4.4f); s.circle(Tone.DARK, 57f, 56f, 4.4f); s.rect(Tone.DARK, 47f, 66f, 6f, 8f) }
            14 -> { chain(s); s.strokePath(Tone.GOLD, 4f) { addOval(androidx.compose.ui.geometry.Rect(38f, 38f, 62f, 62f)) }; s.rect(Tone.GOLD, 46f, 60f, 8f, 30f); s.rect(Tone.GOLD, 36f, 72f, 28f, 7f) }
            15 -> { s.rect(Tone.PRIMARY, 36f, 6f, 28f, 30f); s.poly(Tone.SHADE, 36f, 6f, 50f, 20f, 64f, 6f, alpha = 0.6f); s.circle(Tone.GOLD, 50f, 56f, 26f); s.circle(Tone.GOLD_DARK, 50f, 56f, 20f, alpha = 0.5f); s.poly(Tone.GEM, *starPoints(50f, 56f, 15f, 6f, 5)) }
            16 -> { chain(s); s.path(Tone.GOLD) { moveTo(50f, 38f); cubicTo(86f, 36f, 86f, 90f, 50f, 90f); cubicTo(14f, 90f, 14f, 36f, 50f, 38f); close() }; s.path(Tone.PRIMARY) { moveTo(50f, 60f); cubicTo(44f, 48f, 32f, 56f, 50f, 74f); cubicTo(68f, 56f, 56f, 48f, 50f, 60f); close() } }
            17 -> { s.circle(Tone.GOLD, 50f, 54f, 32f); s.circle(Tone.PAPER, 50f, 54f, 26f); s.poly(Tone.PRIMARY, 50f, 30f, 56f, 54f, 50f, 78f, 44f, 54f); s.circle(Tone.GOLD, 50f, 54f, 3f); s.rect(Tone.GOLD, 46f, 14f, 8f, 8f) }
            18 -> { s.rect(Tone.GOLD, 32f, 12f, 36f, 6f); s.rect(Tone.GOLD, 32f, 82f, 36f, 6f); s.poly(Tone.GLASS, 36f, 18f, 64f, 18f, 52f, 50f, 64f, 82f, 36f, 82f, 48f, 50f); s.poly(Tone.PRIMARY, 40f, 24f, 60f, 24f, 50f, 46f); s.poly(Tone.PRIMARY, 42f, 78f, 58f, 78f, 50f, 62f); s.rect(Tone.WOOD, 30f, 8f, 4f, 84f); s.rect(Tone.WOOD, 66f, 8f, 4f, 84f) }
            else -> { chain(s); s.path(Tone.GOLD) { moveTo(18f, 62f); quadraticTo(50f, 28f, 82f, 62f); quadraticTo(50f, 92f, 18f, 62f); close() }; s.circle(Tone.PAPER, 50f, 62f, 13f); s.circle(Tone.GEM, 50f, 62f, 8f); s.circle(Tone.DARK, 50f, 62f, 3.8f) }
        }
    }

    private fun ring(s: Sketch) {
        s.circle(Tone.GOLD, 50f, 66f, 26f); s.circle(Tone.DARK, 50f, 66f, 17f)
    }

    private fun chain(s: Sketch) {
        s.strokePath(Tone.GOLD_DARK, 2f) { moveTo(18f, 8f); quadraticTo(50f, 54f, 82f, 8f) }
        for (i in 0 until 8) { val t = i / 7f; s.circle(Tone.GOLD, 18f + 64f * t, 8f + 36f * (1 - (2 * t - 1) * (2 * t - 1)) * 0.92f, 2.2f, outline = false) }
    }
}

object PotionPainter : Painter {
    override fun paint(design: Int, s: Sketch) {
        val shape = design % 5
        val decor = design / 5
        s.scale = 0.95f
        val neckTop: Float
        when (shape) {
            0 -> { // round flask
                neckTop = 18f
                s.circle(Tone.GLASS, 50f, 66f, 28f); s.rect(Tone.GLASS, 42f, neckTop, 16f, 26f)
                s.path(Tone.PRIMARY) { moveTo(24f, 64f); quadraticTo(50f, 56f, 76f, 64f); cubicTo(78f, 88f, 60f, 94f, 50f, 94f); cubicTo(40f, 94f, 22f, 88f, 24f, 64f); close() }
            }
            1 -> { // tall vial
                neckTop = 8f
                s.rrect(Tone.GLASS, 36f, neckTop + 14f, 28f, 76f, 8f); s.rect(Tone.GLASS, 43f, neckTop, 14f, 20f)
                s.rrect(Tone.PRIMARY, 39f, 44f, 22f, 44f, 6f)
            }
            2 -> { // conical
                neckTop = 12f
                s.poly(Tone.GLASS, 42f, neckTop, 58f, neckTop, 58f, 38f, 82f, 90f, 18f, 90f, 42f, 38f)
                s.poly(Tone.PRIMARY, 33f, 60f, 67f, 60f, 79f, 88f, 21f, 88f)
            }
            3 -> { // square bottle
                neckTop = 12f
                s.rrect(Tone.GLASS, 24f, 40f, 52f, 52f, 8f); s.rect(Tone.GLASS, 42f, neckTop, 16f, 32f)
                s.rrect(Tone.PRIMARY, 27f, 56f, 46f, 33f, 6f)
            }
            else -> { // bulb with long neck
                neckTop = 6f
                s.circle(Tone.GLASS, 50f, 70f, 23f); s.rect(Tone.GLASS, 44f, neckTop, 12f, 42f)
                s.path(Tone.PRIMARY) { moveTo(29f, 66f); quadraticTo(50f, 58f, 71f, 66f); cubicTo(74f, 88f, 60f, 92f, 50f, 92f); cubicTo(40f, 92f, 26f, 88f, 29f, 66f); close() }
            }
        }
        // bubbles & highlights
        s.circle(Tone.LIGHT, 44f, 76f, 3.2f, outline = false, alpha = 0.9f); s.circle(Tone.LIGHT, 58f, 70f, 2f, outline = false, alpha = 0.9f); s.circle(Tone.LIGHT, 52f, 84f, 2.4f, outline = false, alpha = 0.8f)
        s.line(Tone.WHITE, 32f, 60f, 32f, 80f, 2.4f, 0.8f)
        // stopper
        val sw = if (shape == 1 || shape == 4) 14f else 18f
        val sx = 50f - sw / 2
        when (decor) {
            0 -> s.rrect(Tone.WOOD, sx, neckTop - 8f, sw, 12f, 3f)
            1 -> { s.rrect(Tone.SHADE, sx - 1, neckTop - 6f, sw + 2, 14f, 4f); s.circle(Tone.GOLD, 50f, neckTop + 1f, 3f) }
            2 -> { s.circle(Tone.PAPER, 50f, neckTop - 3f, 9f); s.circle(Tone.DARK, 46.5f, neckTop - 4f, 2f); s.circle(Tone.DARK, 53.5f, neckTop - 4f, 2f) }
            else -> { s.rect(Tone.GOLD, sx, neckTop, sw, 4f); s.poly(Tone.GEM, 50f, neckTop - 12f, 56f, neckTop - 4f, 50f, neckTop + 1f, 44f, neckTop - 4f) }
        }
    }
}

object RunePainter : Painter {
    override fun paint(design: Int, s: Sketch) {
        val shape = design % 5
        val glyph = design / 5
        s.scale = 0.95f
        when (shape) {
            0 -> { s.rrect(Tone.SHADE, 22f, 8f, 56f, 84f, 10f); s.rrect(Tone.PRIMARY, 22f, 8f, 56f, 84f, 10f, outline = false, alpha = 0.25f) }
            1 -> { s.oval(Tone.SHADE, 50f, 50f, 38f, 44f); s.oval(Tone.PRIMARY, 44f, 40f, 22f, 26f, outline = false, alpha = 0.22f) }
            2 -> s.poly(Tone.SHADE, 50f, 6f, 88f, 28f, 88f, 72f, 50f, 94f, 12f, 72f, 12f, 28f)
            3 -> s.poly(Tone.SHADE, 50f, 4f, 86f, 50f, 50f, 96f, 14f, 50f)
            else -> { s.circle(Tone.SHADE, 50f, 50f, 42f); s.circle(Tone.PRIMARY, 50f, 50f, 34f, outline = false, alpha = 0.2f) }
        }
        s.circle(Tone.WHITE, 34f, 24f, 4f, outline = false, alpha = 0.3f)
        val g: Array<FloatArray> = when (glyph) {
            0 -> arrayOf(floatArrayOf(38f, 22f, 38f, 78f), floatArrayOf(38f, 40f, 64f, 24f), floatArrayOf(38f, 58f, 64f, 42f))
            1 -> arrayOf(floatArrayOf(40f, 22f, 40f, 78f), floatArrayOf(40f, 32f, 62f, 46f, 40f, 62f))
            2 -> arrayOf(floatArrayOf(50f, 22f, 50f, 78f), floatArrayOf(32f, 28f, 50f, 50f, 68f, 28f))
            3 -> arrayOf(floatArrayOf(50f, 22f, 70f, 42f, 50f, 62f, 30f, 42f, 50f, 22f), floatArrayOf(50f, 62f, 66f, 80f), floatArrayOf(50f, 62f, 34f, 80f))
            4 -> arrayOf(floatArrayOf(38f, 22f, 38f, 78f), floatArrayOf(38f, 26f, 64f, 40f), floatArrayOf(38f, 46f, 64f, 60f))
            else -> arrayOf(floatArrayOf(32f, 24f, 32f, 76f, 68f, 24f, 68f, 76f, 32f, 24f))
        }
        for (p in g) s.curve(Tone.OUTLINE, 6.5f, *p, alpha = 0.7f)
        for (p in g) s.curve(Tone.GEM, 4.2f, *p)
        for (p in g) s.curve(Tone.WHITE, 1.4f, *p, alpha = 0.9f)
    }
}

object HerbPainter : Painter {
    override fun paint(design: Int, s: Sketch) {
        s.scale = 0.96f
        when (design) {
            0 -> { s.curve(Tone.LEAF_DARK, 2.4f, 50f, 94f, 50f, 20f); for (i in 0 until 4) { s.leaf(50f, 80f - i * 16f, 24f, 8f, -150f + i * 8f); s.leaf(50f, 72f - i * 16f, 24f, 8f, -30f - i * 8f) }; s.leaf(50f, 24f, 18f, 6f, -90f) }
            1 -> { s.curve(Tone.LEAF_DARK, 2.4f, 50f, 92f, 50f, 58f); for (i in 0 until 4) { val a = (i * 90f + 45f) * PI.toFloat() / 180f; s.circle(Tone.PRIMARY, 50f + 13f * cos(a), 44f + 13f * sin(a), 11f) }; s.circle(Tone.LIGHT, 50f, 44f, 3f, outline = false) }
            2 -> { s.rrect(Tone.PAPER, 42f, 52f, 16f, 38f, 5f); s.path(Tone.PRIMARY) { moveTo(14f, 56f); cubicTo(14f, 14f, 86f, 14f, 86f, 56f); close() }; for (p in listOf(30f to 40f, 50f to 28f, 68f to 42f, 56f to 46f)) s.circle(Tone.WHITE, p.first, p.second, 4f, outline = false, alpha = 0.9f) }
            3 -> { s.rrect(Tone.PAPER, 44f, 40f, 12f, 52f, 5f); s.oval(Tone.PRIMARY, 50f, 34f, 22f, 26f); s.oval(Tone.LIGHT, 44f, 26f, 6f, 8f, outline = false, alpha = 0.6f) }
            4 -> { s.curve(Tone.LEAF_DARK, 2.4f, 50f, 94f, 50f, 50f); s.leaf(50f, 80f, 24f, 8f, -150f); s.leaf(50f, 70f, 24f, 8f, -30f); for (i in 0 until 8) { val a = (i * 45f) * PI.toFloat() / 180f; s.oval(Tone.LIGHT, 50f + 18f * cos(a), 34f + 18f * sin(a), 7f, 7f) }; s.circle(Tone.GOLD, 50f, 34f, 8f) }
            5 -> { s.curve(Tone.LEAF_DARK, 2.4f, 50f, 94f, 50f, 50f); s.leaf(50f, 90f, 30f, 8f, -140f); s.leaf(50f, 90f, 30f, 8f, -40f); s.path(Tone.PRIMARY) { moveTo(30f, 28f); quadraticTo(30f, 58f, 50f, 60f); quadraticTo(70f, 58f, 70f, 28f); lineTo(60f, 38f); lineTo(50f, 24f); lineTo(40f, 38f); close() } }
            6 -> { s.curve(Tone.LEAF_DARK, 2.4f, 50f, 94f, 50f, 56f); s.leaf(50f, 84f, 24f, 9f, -150f); s.leaf(50f, 76f, 24f, 9f, -30f); s.circle(Tone.PRIMARY, 50f, 40f, 17f); s.strokePath(Tone.SHADE, 2f) { moveTo(40f, 40f); quadraticTo(50f, 28f, 60f, 40f); quadraticTo(52f, 50f, 44f, 38f) }; s.circle(Tone.LIGHT, 44f, 32f, 3.4f, outline = false, alpha = 0.7f) }
            7 -> { s.curve(Tone.LEAF_DARK, 2.2f, 50f, 94f, 50f, 8f); for (i in 0 until 7) { val y = 86f - i * 11f; val len = 10f + (6 - i) * 2.6f; s.leaf(50f, y, len, 4f, -160f + i * 2f, Tone.PRIMARY); s.leaf(50f, y, len, 4f, -20f - i * 2f, Tone.PRIMARY) } }
            8 -> { for (i in 0 until 5) s.leaf(50f, 90f, 40f, 12f, -160f + i * 35f); for (p in listOf(36f to 48f, 54f to 36f, 66f to 56f, 46f to 62f, 30f to 66f)) s.circle(Tone.PRIMARY, p.first, p.second, 6f); s.circle(Tone.WHITE, 34f, 46f, 1.6f, outline = false) }
            9 -> { s.curve(Tone.GOLD_DARK, 2.4f, 50f, 94f, 50f, 22f); for (i in 0 until 6) { s.leaf(50f, 56f - i * 8f, 14f, 4f, -135f, Tone.PRIMARY); s.leaf(50f, 56f - i * 8f, 14f, 4f, -45f, Tone.PRIMARY) }; s.leaf(50f, 24f, 14f, 4f, -90f, Tone.PRIMARY) }
            10 -> { s.curve(Tone.LEAF_DARK, 2.6f, 50f, 94f, 50f, 46f); s.leaf(50f, 84f, 28f, 7f, -150f); s.leaf(50f, 74f, 28f, 7f, -30f); s.circle(Tone.PRIMARY, 50f, 34f, 14f); for (i in 0 until 7) s.line(Tone.LEAF_DARK, 50f + 14f * cos(i * 0.9f - 3f), 36f + 14f * sin(i * 0.9f - 3f), 50f + 19f * cos(i * 0.9f - 3f), 36f + 19f * sin(i * 0.9f - 3f), 1.6f) }
            11 -> { s.oval(Tone.LEAF, 50f, 84f, 40f, 9f); for (a in listOf(-70f, -40f, 0f, 40f, 70f)) s.path(Tone.PRIMARY) { val r = a * PI.toFloat() / 180f; moveTo(50f, 80f); quadraticTo(50f + 40f * sin(r) - 10f, 52f, 50f + 34f * sin(r), 80f - 40f * cos(r)); quadraticTo(50f + 40f * sin(r) + 10f, 52f, 50f, 80f); close() }; s.circle(Tone.GOLD, 50f, 62f, 6f) }
            12 -> { for (p in listOf(30f to 70f, 52f to 62f, 72f to 72f, 42f to 80f, 62f to 84f)) s.circle(Tone.PRIMARY, p.first, p.second, 16f); for (p in listOf(36f to 64f, 56f to 58f, 70f to 66f)) s.circle(Tone.LIGHT, p.first, p.second, 3.6f, outline = false, alpha = 0.7f) }
            13 -> { s.rrect(Tone.PRIMARY, 38f, 18f, 24f, 72f, 12f); s.rrect(Tone.PRIMARY, 14f, 40f, 14f, 30f, 7f); s.rrect(Tone.PRIMARY, 22f, 56f, 22f, 12f, 6f); s.rrect(Tone.PRIMARY, 72f, 34f, 14f, 28f, 7f); s.rrect(Tone.PRIMARY, 58f, 52f, 22f, 12f, 6f); for (y in 0 until 5) s.line(Tone.SHADE, 50f, 26f + y * 12f, 50f, 34f + y * 12f, 1.4f); s.circle(Tone.LIGHT, 50f, 16f, 5f) }
            14 -> { s.strokePath(Tone.LEAF_DARK, 2.4f) { moveTo(12f, 84f); cubicTo(30f, 40f, 60f, 100f, 88f, 20f) }; for (p in listOf(24f to 66f, 44f to 70f, 62f to 56f, 78f to 36f)) { s.leaf(p.first, p.second, 18f, 7f, -120f, Tone.PRIMARY); s.leaf(p.first, p.second, 18f, 7f, 20f, Tone.PRIMARY) } }
            15 -> { for (i in 0 until 5) s.leaf(50f, 80f, 50f, 7f, -150f + i * 15f, if (i % 2 == 0) Tone.LEAF else Tone.PRIMARY); s.rrect(Tone.LEATHER, 40f, 70f, 20f, 8f, 3f); s.curve(Tone.LEAF_DARK, 2.2f, 46f, 80f, 40f, 94f); s.curve(Tone.LEAF_DARK, 2.2f, 54f, 80f, 60f, 94f) }
            16 -> { s.path(Tone.PAPER) { moveTo(50f, 28f); cubicTo(70f, 28f, 74f, 52f, 64f, 64f); lineTo(70f, 90f); lineTo(60f, 90f); lineTo(50f, 72f); lineTo(40f, 90f); lineTo(30f, 90f); lineTo(36f, 64f); cubicTo(26f, 52f, 30f, 28f, 50f, 28f); close() }; s.circle(Tone.DARK, 43f, 44f, 2.4f); s.circle(Tone.DARK, 57f, 44f, 2.4f); for (i in 0 until 3) s.leaf(50f, 30f, 22f, 6f, -120f + i * 30f, Tone.PRIMARY) }
            17 -> { for (x in listOf(30f, 50f, 70f)) { s.rrect(Tone.PRIMARY, x - 6f, 10f, 12f, 82f, 4f); for (y in listOf(30f, 56f, 78f)) s.rect(Tone.SHADE, x - 7f, y, 14f, 4f) }; s.leaf(36f, 34f, 22f, 6f, -20f); s.leaf(64f, 60f, 22f, 6f, -160f) }
            18 -> { s.curve(Tone.WOOD_DARK, 3f, 50f, 94f, 50f, 8f); for (i in 0 until 8) { val y = 84f - i * 9.5f; val len = 30f - i * 2.8f; s.curve(Tone.PRIMARY, 4f, 50f, y, 50f - len, y - 8f); s.curve(Tone.PRIMARY, 4f, 50f, y, 50f + len, y - 8f) }; s.circle(Tone.WOOD, 40f, 30f, 4f); s.circle(Tone.WOOD, 62f, 52f, 4f) }
            else -> { for (p in listOf(34f to 76f, 56f to 70f, 72f to 80f, 46f to 54f)) { s.rrect(Tone.PAPER, p.first - 3f, p.second - 4f, 6f, 20f, 3f); s.oval(Tone.GEM, p.first, p.second - 6f, 12f, 9f); s.oval(Tone.WHITE, p.first - 4f, p.second - 9f, 3f, 2f, outline = false) } }
        }
    }
}

object BookPainter : Painter {
    override fun paint(design: Int, s: Sketch) {
        s.scale = 0.95f
        if (design == 7) { // open book
            s.poly(Tone.PRIMARY, 6f, 30f, 50f, 40f, 94f, 30f, 94f, 78f, 50f, 88f, 6f, 78f)
            s.poly(Tone.PAPER, 10f, 26f, 48f, 36f, 48f, 82f, 10f, 74f); s.poly(Tone.PAPER, 90f, 26f, 52f, 36f, 52f, 82f, 90f, 74f)
            for (i in 0 until 5) { s.line(Tone.WOOD_DARK, 16f, 36f + i * 8f, 42f, 42f + i * 8f, 1f, 0.5f); s.line(Tone.WOOD_DARK, 84f, 36f + i * 8f, 58f, 42f + i * 8f, 1f, 0.5f) }
            s.circle(Tone.GEM, 50f, 60f, 4f); return
        }
        if (design == 8) { // scroll
            s.rrect(Tone.PAPER, 22f, 16f, 56f, 68f, 3f); s.rrect(Tone.WOOD, 14f, 10f, 72f, 10f, 5f); s.rrect(Tone.WOOD, 14f, 80f, 72f, 10f, 5f)
            for (i in 0 until 5) s.line(Tone.WOOD_DARK, 30f, 30f + i * 10f, 70f - (i % 2) * 14f, 30f + i * 10f, 1.4f, 0.55f)
            s.circle(Tone.PRIMARY, 50f, 70f, 7f); s.circle(Tone.SHADE, 50f, 70f, 3.4f); return
        }
        if (design == 9) { // stack of books
            s.rrect(Tone.SHADE, 16f, 62f, 68f, 24f, 3f); s.rect(Tone.PAPER, 22f, 70f, 62f, 8f); s.rect(Tone.GOLD, 16f, 66f, 6f, 16f)
            s.rrect(Tone.PRIMARY, 22f, 38f, 60f, 24f, 3f); s.rect(Tone.PAPER, 28f, 46f, 54f, 8f); s.rect(Tone.GOLD, 22f, 42f, 6f, 16f)
            s.rrect(Tone.LEATHER, 26f, 16f, 48f, 22f, 3f); s.rect(Tone.PAPER, 32f, 23f, 42f, 8f); s.circle(Tone.GEM, 50f, 27f, 3.4f); return
        }
        // closed tome
        s.rrect(Tone.PAPER, 32f, 14f, 46f, 76f, 3f)
        s.rrect(Tone.PRIMARY, 24f, 10f, 48f, 80f, 5f)
        s.rrect(Tone.SHADE, 24f, 10f, 10f, 80f, 4f)
        s.line(Tone.GOLD, 34f, 10f, 34f, 90f, 1.6f)
        when (design) {
            0 -> { s.rect(Tone.GOLD, 66f, 44f, 12f, 12f); s.circle(Tone.GEM, 72f, 50f, 2.6f); s.rect(Tone.GOLD, 40f, 22f, 26f, 3f); s.rect(Tone.GOLD, 40f, 76f, 26f, 3f) }
            1 -> for (p in listOf(24f to 10f, 62f to 10f, 24f to 80f, 62f to 80f)) s.rect(Tone.GOLD, p.first, p.second, 10f, 10f)
            2 -> { s.circle(Tone.GOLD, 54f, 50f, 14f); s.circle(Tone.GEM, 54f, 50f, 9f) }
            3 -> { s.rect(Tone.GOLD, 51f, 28f, 6f, 44f); s.rect(Tone.GOLD, 40f, 38f, 28f, 6f) }
            4 -> { s.path(Tone.GOLD) { moveTo(38f, 50f); quadraticTo(54f, 30f, 70f, 50f); quadraticTo(54f, 70f, 38f, 50f); close() }; s.circle(Tone.GEM, 54f, 50f, 7f); s.circle(Tone.DARK, 54f, 50f, 3f) }
            5 -> s.poly(Tone.GOLD, *starPoints(54f, 50f, 20f, 8f, 5))
            else -> { s.rect(Tone.LEATHER_DARK, 24f, 44f, 54f, 8f); s.rect(Tone.GOLD, 62f, 42f, 8f, 12f); s.circle(Tone.GEM, 54f, 26f, 5f) }
        }
        s.line(Tone.WHITE, 40f, 16f, 40f, 40f, 2f, 0.4f)
    }
}

object ChestBoxPainter : Painter {
    override fun paint(design: Int, s: Sketch) {
        val shape = design % 5
        val decor = design / 5
        s.scale = 0.96f
        val open = decor == 1
        if (shape == 2) { // barrel
            s.path(Tone.WOOD) { moveTo(22f, 28f); cubicTo(10f, 50f, 10f, 70f, 22f, 90f); lineTo(78f, 90f); cubicTo(90f, 70f, 90f, 50f, 78f, 28f); close() }
            s.oval(Tone.WOOD_DARK, 50f, 28f, 28f, 8f)
            if (open) for (i in 0 until 5) s.circle(Tone.GOLD, 34f + i * 8f, 26f - (i % 2) * 3f, 5f)
            for (y in listOf(40f, 62f, 80f)) s.rect(Tone.PRIMARY, 12f, y, 76f, 5f)
            for (x in listOf(34f, 50f, 66f)) s.line(Tone.WOOD_DARK, x, 36f, x, 90f, 1f, 0.6f)
            if (decor == 3) s.circle(Tone.GEM, 50f, 60f, 6f)
            if (decor == 2) s.rrect(Tone.GOLD, 42f, 54f, 16f, 14f, 3f)
            return
        }
        val bodyT = if (shape == 3) Tone.BLADE_DARK else Tone.WOOD
        val lidT = if (shape == 3) Tone.BLADE else Tone.WOOD
        val feet = shape == 4
        val by = 50f; val bh = if (feet) 32f else 40f
        if (open) {
            s.poly(Tone.WOOD_DARK, 16f, 52f, 84f, 52f, 78f, 14f, 22f, 14f)
        }
        s.rrect(bodyT, 12f, by, 76f, bh, 5f)
        s.rect(Tone.WOOD_DARK, 12f, by + bh - 8f, 76f, 8f, outline = false, alpha = 0.35f)
        if (open) {
            for (i in 0 until 7) s.circle(Tone.GOLD, 22f + i * 9.4f, 48f - (i % 3) * 3f, 5.2f)
            s.circle(Tone.GEM, 40f, 44f, 4f); s.circle(Tone.PRIMARY, 62f, 42f, 3.6f)
            s.rrect(bodyT, 12f, 56f, 76f, bh - 6f, 5f)
        } else {
            if (shape == 0 || shape == 4) s.path(lidT) { moveTo(12f, by + 2f); lineTo(12f, 38f); cubicTo(12f, 8f, 88f, 8f, 88f, 38f); lineTo(88f, by + 2f); close() }
            else s.rrect(lidT, 12f, 22f, 76f, 30f, 5f)
            s.rect(Tone.PRIMARY, 12f, by - 4f, 76f, 7f)
            s.rect(Tone.PRIMARY, 28f, 14f, 8f, bh + 36f, alpha = 0.95f); s.rect(Tone.PRIMARY, 64f, 14f, 8f, bh + 36f, alpha = 0.95f)
        }
        s.rect(Tone.PRIMARY, 12f, by + bh - 12f, 76f, 5f)
        if (feet) { s.rect(Tone.WOOD_DARK, 16f, by + bh, 10f, 8f); s.rect(Tone.WOOD_DARK, 74f, by + bh, 10f, 8f) }
        when (decor) {
            3 -> s.poly(Tone.GEM, 50f, by - 14f, 60f, by - 2f, 50f, by + 10f, 40f, by - 2f)
            2 -> { s.strokePath(Tone.METAL, 3f) { moveTo(42f, by + 2f); lineTo(42f, by - 8f); quadraticTo(50f, by - 18f, 58f, by - 8f); lineTo(58f, by + 2f) }; s.rrect(Tone.GOLD, 40f, by + 2f, 20f, 16f, 3f); s.circle(Tone.DARK, 50f, by + 9f, 2.4f) }
            0 -> { s.rrect(Tone.GOLD, 44f, by - 4f, 12f, 16f, 3f); s.circle(Tone.DARK, 50f, by + 4f, 2f) }
        }
    }
}

object MaterialPainter : Painter {
    override fun paint(design: Int, s: Sketch) {
        s.scale = 0.95f
        when (design) {
            0 -> { s.poly(Tone.BLADE, 12f, 76f, 24f, 40f, 76f, 40f, 88f, 76f); s.poly(Tone.BLADE_DARK, 50f, 40f, 76f, 40f, 88f, 76f, 50f, 76f, alpha = 0.6f); s.poly(Tone.METAL_LIGHT, 28f, 46f, 70f, 46f, 72f, 50f, 26f, 50f, alpha = 0.7f) }
            1 -> { for ((i, p) in listOf(10f to 80f, 52f to 80f, 31f to 58f).withIndex()) { s.poly(if (i == 2) Tone.BLADE else Tone.BLADE_DARK, p.first, p.second, p.first + 6f, p.second - 22f, p.first + 36f, p.second - 22f, p.first + 42f, p.second); s.poly(Tone.METAL_LIGHT, p.first + 8f, p.second - 19f, p.first + 34f, p.second - 19f, p.first + 35f, p.second - 16f, p.first + 7f, p.second - 16f, alpha = 0.6f) } }
            2 -> { s.poly(Tone.METAL_DARK, 16f, 70f, 12f, 44f, 30f, 20f, 58f, 14f, 84f, 34f, 88f, 64f, 66f, 86f, 34f, 84f); s.poly(Tone.METAL, 30f, 20f, 58f, 14f, 50f, 40f, 24f, 46f, alpha = 0.55f); s.poly(Tone.GEM, 44f, 54f, 58f, 50f, 62f, 66f, 48f, 70f); s.poly(Tone.LIGHT, 44f, 54f, 58f, 50f, 52f, 60f, alpha = 0.8f) }
            3 -> { s.poly(Tone.PRIMARY, 50f, 86f, 12f, 40f, 28f, 16f, 72f, 16f, 88f, 40f); s.poly(Tone.LIGHT, 28f, 16f, 72f, 16f, 62f, 34f, 38f, 34f, alpha = 0.85f); s.poly(Tone.SHADE, 50f, 86f, 88f, 40f, 62f, 34f, alpha = 0.7f); s.line(Tone.WHITE, 12f, 40f, 88f, 40f, 1.2f, 0.6f) }
            4 -> { for (d in listOf(Triple(30f, 90f, 40f), Triple(70f, 90f, 36f), Triple(50f, 92f, 64f))) { val (x, y, h) = d; s.poly(Tone.PRIMARY, x - 11f, y, x - 11f, y - h + 12f, x, y - h, x + 11f, y - h + 12f, x + 11f, y); s.poly(Tone.LIGHT, x - 11f, y, x - 11f, y - h + 12f, x, y - h, x, y, alpha = 0.55f) } }
            5 -> { s.circle(Tone.PRIMARY, 50f, 52f, 36f); s.circle(Tone.LIGHT, 50f, 52f, 26f, outline = false, alpha = 0.35f); s.circle(Tone.WHITE, 38f, 38f, 8f, outline = false, alpha = 0.7f) }
            6 -> { s.path(Tone.LEATHER) { moveTo(18f, 20f); lineTo(40f, 14f); lineTo(60f, 20f); lineTo(84f, 14f); lineTo(88f, 44f); lineTo(76f, 56f); lineTo(88f, 84f); lineTo(58f, 88f); lineTo(44f, 80f); lineTo(14f, 88f); lineTo(20f, 56f); lineTo(10f, 44f); close() }; for (i in 0 until 6) s.circle(Tone.LEATHER_DARK, 28f + (i % 3) * 22f, 34f + (i / 3) * 28f, 3f, outline = false, alpha = 0.6f) }
            7 -> { s.rrect(Tone.PAPER, 38f, 16f, 24f, 68f, 9f); s.circle(Tone.PAPER, 34f, 18f, 11f); s.circle(Tone.PAPER, 66f, 18f, 11f); s.circle(Tone.PAPER, 34f, 84f, 11f); s.circle(Tone.PAPER, 66f, 84f, 11f); s.line(Tone.WOOD_DARK, 46f, 28f, 46f, 70f, 1.2f, 0.35f) }
            8 -> { s.curve(Tone.WOOD, 2.6f, 50f, 94f, 50f, 8f); for (i in 0 until 9) { s.path(Tone.PRIMARY) { moveTo(50f, 90f - i * 9f); quadraticTo(22f, 80f - i * 9f - 8f, 16f + i * 2f, 70f - i * 9f); quadraticTo(36f, 84f - i * 9f, 50f, 88f - i * 9f); close() }; s.path(Tone.LIGHT, alpha = 0.7f) { moveTo(50f, 90f - i * 9f); quadraticTo(78f, 80f - i * 9f - 8f, 84f - i * 2f, 70f - i * 9f); quadraticTo(64f, 84f - i * 9f, 50f, 88f - i * 9f); close() } } }
            9 -> { s.rrect(Tone.PRIMARY, 12f, 34f, 76f, 34f, 17f); s.rrect(Tone.SHADE, 12f, 50f, 76f, 18f, 9f, outline = false, alpha = 0.5f); for (i in 0 until 5) s.line(Tone.SHADE, 20f + i * 14f, 36f, 28f + i * 14f, 66f, 1.2f, 0.5f); s.rrect(Tone.LIGHT, 18f, 38f, 64f, 8f, 4f, outline = false, alpha = 0.6f) }
            10 -> { s.rrect(Tone.WOOD, 8f, 38f, 84f, 28f, 14f); s.oval(Tone.WOOD_DARK, 86f, 52f, 8f, 14f); s.oval(Tone.PAPER, 86f, 52f, 5f, 9f, outline = false, alpha = 0.7f); for (i in 0 until 4) s.line(Tone.WOOD_DARK, 20f + i * 16f, 40f, 24f + i * 16f, 64f, 1.2f, 0.6f) }
            11 -> { for (i in 0 until 3) s.rrect(Tone.WOOD, 10f, 22f + i * 22f, 80f, 18f, 3f); for (i in 0 until 3) { s.line(Tone.WOOD_DARK, 30f, 28f + i * 22f, 70f, 28f + i * 22f, 1f, 0.6f); s.circle(Tone.METAL_DARK, 18f, 31f + i * 22f, 1.6f, outline = false) } }
            12 -> { s.poly(Tone.DARK, 18f, 74f, 14f, 50f, 34f, 28f, 58f, 24f, 82f, 40f, 86f, 66f, 66f, 84f, 38f, 84f); s.poly(Tone.METAL_DARK, 34f, 28f, 58f, 24f, 50f, 46f, 30f, 48f, alpha = 0.5f); s.circle(Tone.WHITE, 62f, 44f, 3f, outline = false, alpha = 0.4f) }
            13 -> { s.path(Tone.PRIMARY) { moveTo(50f, 10f); cubicTo(84f, 24f, 88f, 64f, 50f, 92f); cubicTo(12f, 64f, 16f, 24f, 50f, 10f); close() }; s.path(Tone.LIGHT, alpha = 0.6f) { moveTo(50f, 18f); cubicTo(70f, 30f, 72f, 56f, 50f, 78f); cubicTo(30f, 56f, 32f, 30f, 50f, 18f); close() }; s.line(Tone.SHADE, 50f, 14f, 50f, 88f, 1.2f, 0.6f) }
            14 -> { s.path(Tone.PAPER) { moveTo(24f, 20f); cubicTo(70f, 20f, 82f, 60f, 70f, 90f); cubicTo(66f, 62f, 40f, 40f, 24f, 20f); close() }; s.path(Tone.WHITE, alpha = 0.5f) { moveTo(30f, 24f); cubicTo(62f, 28f, 72f, 58f, 68f, 76f); cubicTo(60f, 56f, 44f, 40f, 30f, 24f); close() } }
            15 -> { for (i in 0 until 3) s.path(Tone.PAPER) { moveTo(30f + i * 14f, 84f); cubicTo(14f + i * 14f, 56f, 28f + i * 14f, 22f, 52f + i * 8f, 10f); cubicTo(50f + i * 14f, 40f, 54f + i * 10f, 64f, 44f + i * 14f, 84f); close() } }
            16 -> { s.circle(Tone.WHITE, 50f, 56f, 28f, alpha = 0.95f); s.circle(Tone.LIGHT, 50f, 56f, 20f, outline = false, alpha = 0.3f); s.circle(Tone.WHITE, 40f, 44f, 8f, outline = false, alpha = 0.9f); s.circle(Tone.PRIMARY, 50f, 56f, 28f, outline = false, alpha = 0.2f) }
            17 -> { s.path(Tone.LEATHER) { moveTo(30f, 38f); cubicTo(0f, 70f, 14f, 92f, 50f, 92f); cubicTo(86f, 92f, 100f, 70f, 70f, 38f); close() }; s.rrect(Tone.LEATHER_DARK, 32f, 30f, 36f, 12f, 5f); s.circle(Tone.GEM, 50f, 66f, 9f); s.curve(Tone.WHITE, 2f, 50f, 20f, 46f, 12f, 54f, 6f, alpha = 0.6f) }
            18 -> { s.rrect(Tone.WOOD, 24f, 16f, 52f, 6f, 2f); s.rrect(Tone.WOOD, 24f, 78f, 52f, 6f, 2f); s.rect(Tone.PRIMARY, 30f, 22f, 40f, 56f); for (i in 0 until 7) s.line(Tone.SHADE, 30f, 26f + i * 8f, 70f, 28f + i * 8f, 1.4f, 0.8f); s.curve(Tone.PRIMARY, 2f, 70f, 40f, 88f, 50f, 92f, 70f) }
            else -> { s.oval(Tone.PAPER, 50f, 54f, 28f, 36f); for (p in listOf(40f to 40f, 58f to 56f, 42f to 66f)) s.circle(Tone.PRIMARY, p.first, p.second, 6f, outline = false, alpha = 0.85f); s.oval(Tone.WHITE, 40f, 36f, 6f, 9f, outline = false, alpha = 0.7f) }
        }
    }
}

object MiscPainter : Painter {
    override fun paint(design: Int, s: Sketch) {
        s.scale = 0.95f
        when (design) {
            0 -> { s.circle(Tone.GOLD, 50f, 24f, 15f); s.circle(Tone.DARK, 50f, 24f, 7f); s.rect(Tone.GOLD, 46f, 38f, 8f, 50f); s.rect(Tone.GOLD, 54f, 70f, 14f, 6f); s.rect(Tone.GOLD, 54f, 82f, 10f, 6f); s.circle(Tone.GEM, 50f, 24f, 3f) }
            1 -> { s.rrect(Tone.PAPER, 30f, 14f, 40f, 72f, 4f); s.rrect(Tone.PAPER, 24f, 8f, 52f, 12f, 6f); s.rrect(Tone.PAPER, 24f, 80f, 52f, 12f, 6f); s.circle(Tone.PRIMARY, 50f, 56f, 9f); for (i in 0 until 3) s.line(Tone.WOOD_DARK, 36f, 28f + i * 8f, 64f, 28f + i * 8f, 1.4f, 0.6f) }
            2 -> { s.rrect(Tone.WOOD, 44f, 40f, 12f, 54f, 3f); s.path(Tone.PRIMARY) { moveTo(50f, 4f); cubicTo(70f, 22f, 70f, 38f, 50f, 44f); cubicTo(30f, 38f, 30f, 22f, 50f, 4f); close() }; s.path(Tone.GOLD) { moveTo(50f, 18f); cubicTo(60f, 28f, 58f, 38f, 50f, 40f); cubicTo(42f, 38f, 40f, 28f, 50f, 18f); close() }; s.rect(Tone.METAL_DARK, 38f, 40f, 24f, 6f) }
            3 -> { s.strokePath(Tone.METAL_DARK, 3f) { moveTo(38f, 22f); quadraticTo(50f, 2f, 62f, 22f) }; s.rrect(Tone.METAL_DARK, 32f, 20f, 36f, 8f, 3f); s.rrect(Tone.GLASS, 34f, 28f, 32f, 46f, 6f); s.path(Tone.GOLD) { moveTo(50f, 34f); cubicTo(60f, 46f, 58f, 62f, 50f, 66f); cubicTo(42f, 62f, 40f, 46f, 50f, 34f); close() }; s.rrect(Tone.METAL_DARK, 30f, 72f, 40f, 10f, 3f) }
            4 -> { s.path(Tone.LEATHER) { moveTo(30f, 38f); cubicTo(2f, 70f, 14f, 92f, 50f, 92f); cubicTo(86f, 92f, 98f, 70f, 70f, 38f); close() }; s.path(Tone.LEATHER_DARK) { moveTo(28f, 36f); lineTo(36f, 22f); lineTo(64f, 22f); lineTo(72f, 36f); close() }; s.rect(Tone.GOLD, 34f, 38f, 32f, 4f); s.circle(Tone.GOLD, 50f, 66f, 11f); s.poly(Tone.DARK, 50f, 60f, 56f, 66f, 50f, 72f, 44f, 66f) }
            5 -> { s.poly(Tone.PAPER, 8f, 22f, 36f, 14f, 64f, 24f, 92f, 14f, 92f, 78f, 64f, 88f, 36f, 78f, 8f, 86f); s.line(Tone.WOOD_DARK, 36f, 14f, 36f, 78f, 1f, 0.5f); s.line(Tone.WOOD_DARK, 64f, 24f, 64f, 88f, 1f, 0.5f); s.curve(Tone.PRIMARY, 2.4f, 16f, 66f, 30f, 46f, 48f, 56f, 70f, 34f, 84f, 40f); s.line(Tone.PRIMARY, 72f, 28f, 84f, 40f, 2.4f); s.line(Tone.PRIMARY, 84f, 28f, 72f, 40f, 2.4f) }
            6 -> { s.rrect(Tone.PAPER, 38f, 28f, 24f, 60f, 4f); s.oval(Tone.DARK, 50f, 28f, 12f, 4f, alpha = 0.5f); s.line(Tone.DARK, 50f, 26f, 50f, 16f, 1.6f); s.path(Tone.GOLD) { moveTo(50f, 2f); cubicTo(60f, 10f, 58f, 16f, 50f, 18f); cubicTo(42f, 16f, 40f, 10f, 50f, 2f); close() }; s.rect(Tone.METAL_DARK, 30f, 86f, 40f, 6f) }
            7 -> { s.rect(Tone.GOLD, 28f, 10f, 44f, 8f); s.rect(Tone.GOLD, 28f, 82f, 44f, 8f); s.poly(Tone.GLASS, 34f, 18f, 66f, 18f, 54f, 50f, 66f, 82f, 34f, 82f, 46f, 50f); s.poly(Tone.PAPER, 40f, 24f, 60f, 24f, 50f, 44f); s.poly(Tone.PAPER, 38f, 78f, 62f, 78f, 50f, 58f); s.rect(Tone.WOOD, 26f, 8f, 4f, 84f); s.rect(Tone.WOOD, 70f, 8f, 4f, 84f) }
            8 -> { for (i in 0 until 7) s.path(Tone.PAPER) { addOval(androidx.compose.ui.geometry.Rect(16f, 14f + i * 10.5f, 84f, 28f + i * 10.5f)) }; for (i in 0 until 14) s.line(Tone.WOOD_DARK, 20f + i * 4.6f, 14f, 24f + i * 4.6f, 88f, 0.8f, 0.45f) }
            9 -> { s.path(Tone.PAPER) { moveTo(22f, 52f); cubicTo(14f, 8f, 86f, 8f, 78f, 52f); lineTo(70f, 68f); lineTo(70f, 84f); lineTo(30f, 84f); lineTo(30f, 68f); close() }; s.oval(Tone.DARK, 38f, 48f, 9f, 11f); s.oval(Tone.DARK, 62f, 48f, 9f, 11f); s.poly(Tone.DARK, 50f, 58f, 45f, 68f, 55f, 68f); for (i in 0 until 4) s.line(Tone.DARK, 38f + i * 8f, 74f, 38f + i * 8f, 84f, 1.8f) }
            10 -> { s.path(Tone.PAPER) { moveTo(8f, 30f); cubicTo(40f, 24f, 78f, 40f, 90f, 80f); cubicTo(80f, 90f, 66f, 82f, 62f, 72f); cubicTo(54f, 52f, 30f, 40f, 8f, 38f); close() }; s.rect(Tone.GOLD, 78f, 66f, 14f, 18f); s.curve(Tone.WOOD_DARK, 1.6f, 14f, 32f, 50f, 40f, 62f, 66f, alpha = 0.6f) }
            11 -> { s.path(Tone.WOOD) { addOval(androidx.compose.ui.geometry.Rect(16f, 50f, 68f, 94f)) }; s.rrect(Tone.WOOD, 40f, 6f, 10f, 56f, 3f); s.circle(Tone.DARK, 42f, 72f, 8f); for (i in 0 until 4) s.line(Tone.METAL_LIGHT, 36f + i * 3f, 70f, 36f + i * 3f, 8f, 0.9f, 0.8f); s.rect(Tone.WOOD_DARK, 38f, 4f, 16f, 8f) }
            12 -> { s.path(Tone.GOLD) { moveTo(18f, 78f); cubicTo(18f, 30f, 82f, 30f, 82f, 78f); lineTo(90f, 84f); lineTo(10f, 84f); close() }; s.circle(Tone.GOLD, 50f, 26f, 6f); s.circle(Tone.GOLD_DARK, 50f, 88f, 7f); s.path(Tone.LIGHT, alpha = 0.6f) { moveTo(28f, 74f); cubicTo(28f, 44f, 44f, 38f, 44f, 38f); cubicTo(36f, 54f, 36f, 66f, 36f, 78f); close() } }
            13 -> { s.rrect(Tone.WOOD, 20f, 30f, 48f, 56f, 8f); s.path(Tone.WOOD_DARK) { moveTo(68f, 40f); cubicTo(92f, 40f, 92f, 76f, 68f, 76f); lineTo(68f, 68f); cubicTo(80f, 68f, 80f, 48f, 68f, 48f); close() }; s.path(Tone.PAPER) { moveTo(18f, 30f); cubicTo(18f, 14f, 34f, 22f, 44f, 16f); cubicTo(54f, 22f, 70f, 14f, 70f, 30f); close() }; s.rect(Tone.GOLD, 20f, 50f, 48f, 4f) }
            14 -> { s.path(Tone.PRIMARY) { moveTo(50f, 30f); cubicTo(90f, 8f, 100f, 64f, 70f, 86f); cubicTo(60f, 94f, 56f, 88f, 50f, 88f); cubicTo(44f, 88f, 40f, 94f, 30f, 86f); cubicTo(0f, 64f, 10f, 8f, 50f, 30f); close() }; s.curve(Tone.WOOD_DARK, 3f, 50f, 30f, 52f, 14f); s.leaf(54f, 18f, 22f, 7f, -30f); s.circle(Tone.WHITE, 32f, 52f, 5f, outline = false, alpha = 0.5f) }
            15 -> { s.path(Tone.LEATHER) { moveTo(8f, 62f); cubicTo(8f, 26f, 92f, 26f, 92f, 62f); cubicTo(92f, 84f, 8f, 84f, 8f, 62f); close() }; s.path(Tone.PAPER, alpha = 0.7f) { moveTo(30f, 42f); cubicTo(40f, 36f, 60f, 36f, 70f, 42f); cubicTo(58f, 40f, 42f, 40f, 30f, 42f); close() }; for (i in 0 until 4) s.line(Tone.LEATHER_DARK, 24f + i * 16f, 44f, 24f + i * 16f, 70f, 1.2f, 0.6f) }
            16 -> { s.path(Tone.PRIMARY) { moveTo(18f, 70f); cubicTo(2f, 40f, 30f, 20f, 50f, 28f); cubicTo(70f, 20f, 98f, 40f, 82f, 70f); cubicTo(66f, 86f, 34f, 86f, 18f, 70f); close() }; s.circle(Tone.PAPER, 38f, 44f, 8f); s.circle(Tone.DARK, 38f, 44f, 3.4f); s.poly(Tone.PRIMARY, 76f, 40f, 98f, 34f, 98f, 56f, 82f, 62f); s.curve(Tone.SHADE, 2f, 48f, 30f, 52f, 70f) }
            17 -> { s.rrect(Tone.PAPER, 18f, 18f, 64f, 64f, 12f); for (p in listOf(34f to 34f, 50f to 50f, 66f to 66f, 34f to 66f, 66f to 34f)) s.circle(Tone.DARK, p.first, p.second, 5f, outline = false); s.rrect(Tone.PRIMARY, 18f, 18f, 64f, 64f, 12f, outline = false, alpha = 0.18f) }
            else -> { s.rrect(Tone.GOLD, 24f, 76f, 52f, 14f, 4f); s.rect(Tone.GOLD_DARK, 36f, 66f, 28f, 12f); s.circle(Tone.GLASS, 50f, 40f, 30f); s.circle(Tone.PRIMARY, 50f, 40f, 24f, outline = false, alpha = 0.45f); s.circle(Tone.LIGHT, 40f, 30f, 7f, outline = false, alpha = 0.8f); s.poly(Tone.WHITE, *starPoints(56f, 46f, 8f, 3f, 4)) }
        }
    }
}

