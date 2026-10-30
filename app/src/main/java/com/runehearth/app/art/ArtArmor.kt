package com.runehearth.app.art

/* Armor painters: design = shape (design % 5) combined with decoration (design / 5) → 20 designs per slot. */

object HelmetPainter : Painter {
    override fun paint(design: Int, s: Sketch) {
        val shape = design % 5
        val decor = design / 5
        s.scale = 0.92f
        // decoration behind the dome
        when (decor) {
            1 -> { // horns
                s.path(Tone.PAPER) { moveTo(26f, 54f); cubicTo(6f, 52f, 6f, 28f, 14f, 14f); cubicTo(16f, 30f, 26f, 38f, 32f, 42f); close() }
                s.path(Tone.PAPER) { moveTo(74f, 54f); cubicTo(94f, 52f, 94f, 28f, 86f, 14f); cubicTo(84f, 30f, 74f, 38f, 68f, 42f); close() }
            }
            2 -> { // wings
                s.mpoly(Tone.METAL_LIGHT, 28f, 50f, 8f, 40f, 2f, 22f, 16f, 28f, 10f, 14f, 26f, 22f, 34f, 34f)
                s.mpoly(Tone.LIGHT, 28f, 50f, 14f, 42f, 10f, 32f, 24f, 36f, 32f, 44f)
            }
        }
        // dome
        when (shape) {
            0 -> { s.path(Tone.BLADE) { moveTo(24f, 72f); cubicTo(22f, 28f, 78f, 28f, 76f, 72f); close() }
                s.path(Tone.BLADE_DARK, alpha = 0.55f) { moveTo(50f, 31f); cubicTo(70f, 30f, 78f, 50f, 76f, 72f); lineTo(50f, 72f); close() } }
            1 -> { s.path(Tone.BLADE) { moveTo(26f, 72f); cubicTo(26f, 44f, 42f, 18f, 50f, 8f); cubicTo(58f, 18f, 74f, 44f, 74f, 72f); close() }
                s.path(Tone.BLADE_DARK, alpha = 0.55f) { moveTo(50f, 8f); cubicTo(58f, 18f, 74f, 44f, 74f, 72f); lineTo(50f, 72f); close() } }
            2 -> { s.rrect(Tone.BLADE, 26f, 22f, 48f, 52f, 7f); s.rrect(Tone.BLADE_DARK, 50f, 22f, 24f, 52f, 7f, alpha = 0.55f) }
            3 -> { s.path(Tone.BLADE) { moveTo(24f, 78f); cubicTo(20f, 26f, 80f, 26f, 76f, 78f); lineTo(64f, 84f); lineTo(36f, 84f); close() }
                s.path(Tone.BLADE_DARK, alpha = 0.55f) { moveTo(50f, 29f); cubicTo(72f, 28f, 78f, 52f, 76f, 78f); lineTo(64f, 84f); lineTo(50f, 84f); close() } }
            else -> { s.path(Tone.PRIMARY) { moveTo(20f, 86f); cubicTo(14f, 36f, 40f, 10f, 56f, 12f); cubicTo(64f, 24f, 82f, 50f, 80f, 86f); lineTo(64f, 78f); lineTo(36f, 78f); close() }
                s.path(Tone.SHADE, alpha = 0.6f) { moveTo(56f, 12f); cubicTo(64f, 24f, 82f, 50f, 80f, 86f); lineTo(64f, 78f); lineTo(52f, 78f); close() } }
        }
        // face opening / visor
        when (shape) {
            0 -> s.rrect(Tone.DARK, 36f, 58f, 28f, 8f, 3f)
            1 -> { s.rect(Tone.DARK, 36f, 54f, 28f, 14f); s.rect(Tone.BLADE, 47f, 40f, 6f, 28f) }
            2 -> { s.rect(Tone.DARK, 30f, 44f, 40f, 5f); s.rect(Tone.DARK, 47f, 44f, 6f, 28f); for (i in 0 until 3) s.circle(Tone.DARK, 36f + i * 14f, 62f, 1.6f) }
            3 -> { s.rect(Tone.DARK, 34f, 50f, 32f, 7f); s.rect(Tone.DARK, 46.5f, 50f, 7f, 28f) }
            else -> s.oval(Tone.DARK, 50f, 62f, 14f, 12f)
        }
        s.rrect(Tone.GOLD, 24f, 70f, 52f, 5f, 2f)
        // front decoration
        when (decor) {
            0 -> s.path(Tone.PRIMARY) { moveTo(50f, 12f); cubicTo(70f, -2f, 92f, 14f, 84f, 44f); cubicTo(80f, 26f, 66f, 22f, 52f, 26f); close() }
            3 -> { s.poly(Tone.GOLD, 50f, 2f, 56f, 24f, 44f, 24f); s.circle(Tone.GEM, 50f, 38f, 4f); s.mpoly(Tone.GOLD, 32f, 40f, 24f, 28f, 36f, 34f) }
            1 -> s.circle(Tone.GEM, 50f, 38f, 3.6f)
            else -> s.circle(Tone.GEM, 50f, 36f, 3.6f)
        }
        s.line(Tone.WHITE, 36f, 36f, 44f, 30f, 2f, 0.6f)
    }
}

private fun Sketch.torso(shape: Int) {
    when (shape) {
        0 -> { // breastplate
            poly(Tone.BLADE, 30f, 18f, 70f, 18f, 80f, 30f, 72f, 74f, 28f, 74f, 20f, 30f)
            poly(Tone.BLADE_DARK, 50f, 18f, 70f, 18f, 80f, 30f, 72f, 74f, 50f, 74f, alpha = 0.55f)
            circle(Tone.BLADE_DARK, 18f, 28f, 11f); circle(Tone.BLADE_DARK, 82f, 28f, 11f)
            rect(Tone.GOLD, 28f, 70f, 44f, 6f)
        }
        1 -> { // tunic
            poly(Tone.PRIMARY, 34f, 16f, 66f, 16f, 90f, 34f, 82f, 46f, 72f, 40f, 72f, 84f, 28f, 84f, 28f, 40f, 18f, 46f, 10f, 34f)
            poly(Tone.SHADE, 66f, 16f, 90f, 34f, 82f, 46f, 72f, 40f, 72f, 84f, 56f, 84f, 56f, 20f, alpha = 0.6f)
            rect(Tone.LEATHER, 28f, 58f, 44f, 7f); rect(Tone.GOLD, 46f, 57f, 8f, 9f)
            poly(Tone.SHADE, 42f, 16f, 50f, 28f, 58f, 16f)
        }
        2 -> { // chainmail with sleeves
            poly(Tone.METAL, 32f, 16f, 68f, 16f, 92f, 30f, 92f, 52f, 78f, 54f, 74f, 40f, 72f, 86f, 28f, 86f, 26f, 40f, 22f, 54f, 8f, 52f, 8f, 30f)
            poly(Tone.METAL_DARK, 68f, 16f, 92f, 30f, 92f, 52f, 78f, 54f, 74f, 40f, 72f, 86f, 52f, 86f, 52f, 20f, alpha = 0.5f)
            for (y in 0 until 7) for (x in 0 until 5) circle(Tone.METAL_LIGHT, 33f + x * 8.5f + (y % 2) * 4f, 28f + y * 8f, 1.5f, outline = false, alpha = 0.8f)
            poly(Tone.SHADE, 42f, 16f, 50f, 26f, 58f, 16f)
        }
        3 -> { // robe
            poly(Tone.PRIMARY, 36f, 12f, 64f, 12f, 76f, 24f, 90f, 60f, 82f, 62f, 78f, 50f, 88f, 94f, 12f, 94f, 22f, 50f, 18f, 62f, 10f, 60f, 24f, 24f)
            poly(Tone.SHADE, 64f, 12f, 76f, 24f, 90f, 60f, 82f, 62f, 78f, 50f, 88f, 94f, 52f, 94f, 52f, 16f, alpha = 0.55f)
            rect(Tone.GOLD, 42f, 12f, 16f, 82f, alpha = 0.9f); rect(Tone.LEATHER, 26f, 52f, 48f, 5f)
        }
        else -> { // plate with big pauldrons
            poly(Tone.BLADE, 32f, 22f, 68f, 22f, 74f, 80f, 26f, 80f)
            poly(Tone.BLADE_DARK, 50f, 22f, 68f, 22f, 74f, 80f, 50f, 80f, alpha = 0.55f)
            path(Tone.BLADE_DARK) { moveTo(6f, 40f); cubicTo(2f, 12f, 34f, 8f, 36f, 30f); lineTo(30f, 42f); close() }
            path(Tone.BLADE_DARK) { moveTo(94f, 40f); cubicTo(98f, 12f, 66f, 8f, 64f, 30f); lineTo(70f, 42f); close() }
            line(Tone.METAL_LIGHT, 12f, 24f, 28f, 20f, 1.6f, 0.8f); line(Tone.METAL_LIGHT, 88f, 24f, 72f, 20f, 1.6f, 0.8f)
            for (i in 0 until 3) rect(Tone.GOLD, 32f, 44f + i * 11f, 36f, 3f)
        }
    }
}

object ChestPainter : Painter {
    override fun paint(design: Int, s: Sketch) {
        s.scale = 0.95f
        s.torso(design % 5)
        when (design / 5) {
            1 -> s.poly(Tone.GOLD, *starPoints(50f, 50f, 11f, 5f, 4)).also { s.circle(Tone.GEM, 50f, 50f, 3.4f) }
            2 -> for (i in 0 until 6) s.circle(Tone.GOLD, 34f + (i % 3) * 16f, 40f + (i / 3) * 14f, 2.2f)
            3 -> { s.rect(Tone.GOLD, 24f, 62f, 52f, 5f); s.rect(Tone.GOLD, 46f, 18f, 8f, 12f); s.circle(Tone.GEM, 50f, 64.5f, 4f) }
        }
        s.line(Tone.WHITE, 34f, 24f, 40f, 40f, 2f, 0.5f)
    }
}

object GlovePainter : Painter {
    override fun paint(design: Int, s: Sketch) {
        val shape = design % 5
        val decor = design / 5
        s.scale = 0.95f
        val body = when (shape) { 0 -> Tone.BLADE; 1 -> Tone.LEATHER; 2 -> Tone.PRIMARY; 3 -> Tone.BLADE_DARK; else -> Tone.LEATHER }
        val dark = when (shape) { 0 -> Tone.BLADE_DARK; 1 -> Tone.LEATHER_DARK; 2 -> Tone.SHADE; 3 -> Tone.METAL_DARK; else -> Tone.LEATHER_DARK }
        // fingers
        when (shape) {
            2 -> { s.rrect(body, 28f, 12f, 38f, 34f, 14f); s.rrect(body, 12f, 34f, 20f, 14f, 7f) } // mitten + thumb
            3 -> { // claws
                for (i in 0 until 4) { s.rrect(body, 30f + i * 9.5f, 20f + (if (i == 1 || i == 2) -6f else 0f), 8f, 30f, 3f); s.poly(Tone.METAL_LIGHT, 30f + i * 9.5f, 20f + (if (i == 1 || i == 2) -6f else 0f), 34f + i * 9.5f, 4f + (if (i == 1 || i == 2) -6f else 0f), 38f + i * 9.5f, 20f + (if (i == 1 || i == 2) -6f else 0f)) }
                s.poly(body, 30f, 50f, 14f, 34f, 20f, 28f, 34f, 42f)
            }
            4 -> { s.rrect(body, 28f, 12f, 40f, 56f, 6f) }                                           // bracer (no fingers)
            else -> {
                val tops = floatArrayOf(18f, 12f, 10f, 16f)
                for (i in 0 until 4) { s.rrect(body, 30f + i * 9.2f, tops[i], 8.4f, 40f, 4f); s.rrect(dark, 30f + i * 9.2f, tops[i] + 12f, 8.4f, 1.6f, 0.8f, outline = false, alpha = 0.6f) }
                s.poly(body, 32f, 56f, 14f, 38f, 22f, 30f, 36f, 46f)
            }
        }
        s.rrect(body, 28f, 40f, 40f, 34f, 8f)
        s.rrect(dark, 28f, 58f, 40f, 16f, 4f, outline = false, alpha = 0.35f)
        // cuff
        s.rrect(if (shape == 4) Tone.GOLD else Tone.LEATHER_DARK, 24f, 70f, 48f, 22f, 4f)
        s.rect(Tone.GOLD, 24f, 70f, 48f, 4f)
        when (decor) {
            0 -> s.rect(Tone.GOLD, 24f, 82f, 48f, 3f)
            1 -> for (i in 0 until 4) s.circle(Tone.GOLD, 34f + i * 9f, 52f, 2.2f)
            2 -> for (i in 0 until 4) s.poly(Tone.METAL_LIGHT, 31f + i * 9.2f, 42f, 34f + i * 9.2f, 34f, 37f + i * 9.2f, 42f)
            3 -> { s.circle(Tone.GOLD, 48f, 56f, 7f); s.circle(Tone.GEM, 48f, 56f, 4.4f) }
        }
        s.line(Tone.WHITE, 33f, 46f, 33f, 60f, 2f, 0.5f)
    }
}

object LeggingsPainter : Painter {
    override fun paint(design: Int, s: Sketch) {
        val shape = design % 5
        val decor = design / 5
        s.scale = 0.95f
        val cloth = shape == 1 || shape == 4
        val main = if (cloth) Tone.PRIMARY else Tone.BLADE
        val dark = if (cloth) Tone.SHADE else Tone.BLADE_DARK
        val hem = if (shape == 4) 62f else 92f
        // legs
        s.poly(main, 24f, 12f, 49f, 12f, 47f, hem, 28f, hem)
        s.poly(main, 51f, 12f, 76f, 12f, 72f, hem, 53f, hem)
        s.poly(dark, 38f, 12f, 49f, 12f, 47f, hem, 38f, hem, alpha = 0.5f)
        s.poly(dark, 66f, 12f, 76f, 12f, 72f, hem, 66f, hem, alpha = 0.5f)
        when (shape) {
            0 -> { s.circle(Tone.GOLD, 36f, 52f, 7f); s.circle(Tone.GOLD, 64f, 52f, 7f); s.rect(Tone.GOLD, 28f, 84f, 19f, 5f); s.rect(Tone.GOLD, 53f, 84f, 19f, 5f) }
            2 -> { s.poly(Tone.BLADE_DARK, 22f, 12f, 78f, 12f, 82f, 40f, 18f, 40f); for (i in 0 until 5) s.line(Tone.METAL_LIGHT, 28f + i * 11f, 14f, 26f + i * 12f, 38f, 1.2f, 0.7f) }
            3 -> { s.poly(Tone.PRIMARY, 20f, 12f, 80f, 12f, 90f, 70f, 10f, 70f); s.poly(Tone.SHADE, 50f, 12f, 80f, 12f, 90f, 70f, 50f, 70f, alpha = 0.55f); for (i in 0 until 6) s.line(Tone.GOLD, 16f + i * 14f, 66f, 20f + i * 12f, 24f, 1f, 0.6f) }
            1 -> { s.line(Tone.SHADE, 38f, 30f, 42f, 80f, 1.2f, 0.5f); s.line(Tone.SHADE, 62f, 30f, 58f, 80f, 1.2f, 0.5f) }
        }
        s.rrect(Tone.LEATHER, 22f, 10f, 56f, 9f, 3f); s.rect(Tone.GOLD, 45f, 9f, 10f, 11f)
        when (decor) {
            1 -> { s.circle(Tone.GEM, 36f, 48f, 3.4f); s.circle(Tone.GEM, 64f, 48f, 3.4f) }
            2 -> { s.rect(Tone.GOLD, 28f, 70f, 19f, 2.5f); s.rect(Tone.GOLD, 53f, 70f, 19f, 2.5f) }
            3 -> for (i in 0 until 3) { s.circle(Tone.GOLD, 36f, 26f + i * 14f, 1.8f); s.circle(Tone.GOLD, 64f, 26f + i * 14f, 1.8f) }
        }
    }
}

object BootPainter : Painter {
    override fun paint(design: Int, s: Sketch) {
        val shape = design % 5
        val decor = design / 5
        s.scale = 0.95f
        val main = if (shape == 2) Tone.BLADE else Tone.LEATHER
        val dark = if (shape == 2) Tone.BLADE_DARK else Tone.LEATHER_DARK
        val shaftTop = when (shape) { 3 -> 42f; 4 -> 4f; 1 -> 52f; else -> 20f }
        when (shape) {
            1 -> { // sandal
                s.path(dark) { moveTo(14f, 82f); lineTo(80f, 82f); cubicTo(92f, 82f, 94f, 92f, 80f, 92f); lineTo(14f, 92f); close() }
                for (i in 0 until 4) s.line(Tone.LEATHER, 32f, 54f + i * 8f, 60f, 62f + i * 3f, 3.2f)
                s.path(main) { moveTo(30f, 62f); lineTo(60f, 62f); lineTo(70f, 82f); lineTo(30f, 82f); close() }
            }
            else -> {
                s.path(main) {
                    moveTo(32f, shaftTop); lineTo(62f, shaftTop); lineTo(62f, 66f)
                    cubicTo(80f, 68f, 94f, 76f, 94f, 86f); lineTo(94f, 92f); lineTo(24f, 92f); lineTo(30f, 66f); close()
                }
                s.path(dark, alpha = 0.55f) { moveTo(52f, shaftTop); lineTo(62f, shaftTop); lineTo(62f, 66f); cubicTo(80f, 68f, 94f, 76f, 94f, 86f); lineTo(94f, 92f); lineTo(52f, 92f); close() }
                s.rect(Tone.LEATHER_DARK, 22f, 86f, 74f, 7f)
                s.rrect(Tone.GOLD, 30f, shaftTop, 34f, 5f, 2f)
                if (shape == 2) s.poly(Tone.BLADE, 70f, 70f, 96f, 82f, 96f, 88f, 70f, 80f)
            }
        }
        when (decor) {
            0 -> { s.rect(Tone.GOLD, 36f, 46f, 22f, 5f); s.rect(Tone.GEM, 44f, 45f, 6f, 7f) }
            1 -> { s.poly(Tone.METAL_LIGHT, 26f, 82f, 14f, 78f, 18f, 88f); s.circle(Tone.GOLD, 14f, 78f, 3f) }
            2 -> { s.path(Tone.PAPER) { moveTo(28f, shaftTop - 3f); cubicTo(40f, shaftTop - 12f, 54f, shaftTop - 12f, 66f, shaftTop - 3f); lineTo(64f, shaftTop + 8f); lineTo(30f, shaftTop + 8f); close() } }
            3 -> { s.mpoly(Tone.METAL_LIGHT, 28f, 56f, 6f, 46f, 10f, 56f, 4f, 64f, 14f, 66f, 28f, 66f) }
        }
        s.line(Tone.WHITE, 36f, shaftTop + 10f, 36f, 60f, 2f, 0.45f)
    }
}
