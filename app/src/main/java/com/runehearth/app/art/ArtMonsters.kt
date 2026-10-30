package com.runehearth.app.art

private fun Sketch.eyes(y: Float, gap: Float = 14f, r: Float = 5f, tone: Tone = Tone.GOLD) {
    circle(Tone.DARK, 50f - gap, y, r + 1.6f); circle(Tone.DARK, 50f + gap, y, r + 1.6f)
    circle(tone, 50f - gap, y, r); circle(tone, 50f + gap, y, r)
    circle(Tone.DARK, 50f - gap, y, r * 0.42f); circle(Tone.DARK, 50f + gap, y, r * 0.42f)
}

private fun Sketch.fangs(x: Float, y: Float, w: Float, n: Int, len: Float = 8f) {
    for (i in 0 until n) { val cx = x + i * w; poly(Tone.PAPER, cx, y, cx + w * 0.8f, y, cx + w * 0.4f, y + len) }
}

object MonsterPainter : Painter {
    override fun paint(design: Int, s: Sketch) {
        s.scale = 0.96f
        when (design) {
            0 -> { // slime
                s.path(Tone.PRIMARY) { moveTo(10f, 84f); cubicTo(6f, 50f, 30f, 14f, 50f, 14f); cubicTo(70f, 14f, 94f, 50f, 90f, 84f); quadraticTo(50f, 96f, 10f, 84f); close() }
                s.path(Tone.SHADE, alpha = 0.5f) { moveTo(50f, 14f); cubicTo(70f, 14f, 94f, 50f, 90f, 84f); quadraticTo(70f, 92f, 50f, 92f); close() }
                s.eyes(56f, 15f, 6f, Tone.WHITE); s.curve(Tone.DARK, 2.4f, 40f, 72f, 50f, 78f, 60f, 72f); s.oval(Tone.WHITE, 30f, 34f, 7f, 4f, outline = false, alpha = 0.7f)
            }
            1 -> { // goblin
                s.mpoly(Tone.PRIMARY, 22f, 46f, 2f, 38f, 16f, 60f, 24f, 62f)
                s.oval(Tone.PRIMARY, 50f, 52f, 28f, 32f); s.oval(Tone.SHADE, 50f, 70f, 20f, 10f, outline = false, alpha = 0.4f)
                s.eyes(46f, 12f, 5.5f, Tone.GOLD); s.poly(Tone.SHADE, 50f, 50f, 46f, 62f, 54f, 62f); s.curve(Tone.DARK, 2.4f, 36f, 72f, 50f, 78f, 64f, 72f); s.fangs(42f, 72f, 6f, 3, 6f)
            }
            2 -> { // orc
                s.rrect(Tone.PRIMARY, 20f, 24f, 60f, 62f, 22f)
                s.rect(Tone.DARK, 22f, 30f, 56f, 6f, alpha = 0.55f); s.eyes(46f, 14f, 4.6f, Tone.GEM)
                s.rrect(Tone.SHADE, 34f, 56f, 32f, 12f, 4f); s.poly(Tone.PAPER, 34f, 70f, 28f, 54f, 40f, 68f); s.poly(Tone.PAPER, 66f, 70f, 72f, 54f, 60f, 68f)
                s.mpoly(Tone.SHADE, 20f, 40f, 8f, 34f, 20f, 52f)
            }
            3 -> { // skeleton
                s.path(Tone.PAPER) { moveTo(22f, 54f); cubicTo(14f, 10f, 86f, 10f, 78f, 54f); lineTo(70f, 66f); lineTo(70f, 90f); lineTo(30f, 90f); lineTo(30f, 66f); close() }
                s.oval(Tone.DARK, 36f, 48f, 10f, 12f); s.oval(Tone.DARK, 64f, 48f, 10f, 12f); s.circle(Tone.GEM, 36f, 49f, 3.4f); s.circle(Tone.GEM, 64f, 49f, 3.4f)
                s.poly(Tone.DARK, 50f, 58f, 44f, 70f, 56f, 70f); for (i in 0 until 5) s.line(Tone.DARK, 36f + i * 7f, 78f, 36f + i * 7f, 90f, 2f)
            }
            4 -> { // ghost
                s.path(Tone.PRIMARY, alpha = 0.85f) { moveTo(18f, 90f); lineTo(18f, 44f); cubicTo(18f, -2f, 82f, -2f, 82f, 44f); lineTo(82f, 90f); lineTo(70f, 78f); lineTo(58f, 90f); lineTo(46f, 78f); lineTo(34f, 90f); lineTo(30f, 80f); close() }
                s.oval(Tone.DARK, 38f, 42f, 7f, 10f); s.oval(Tone.DARK, 62f, 42f, 7f, 10f); s.oval(Tone.DARK, 50f, 64f, 7f, 9f)
                s.oval(Tone.WHITE, 34f, 24f, 8f, 5f, outline = false, alpha = 0.7f)
            }
            5 -> { // bat
                s.path(Tone.SHADE) { moveTo(46f, 44f); cubicTo(30f, 10f, 6f, 14f, 2f, 30f); quadraticTo(16f, 26f, 18f, 44f); quadraticTo(28f, 36f, 32f, 56f); quadraticTo(40f, 50f, 46f, 64f); close() }
                s.path(Tone.SHADE) { moveTo(54f, 44f); cubicTo(70f, 10f, 94f, 14f, 98f, 30f); quadraticTo(84f, 26f, 82f, 44f); quadraticTo(72f, 36f, 68f, 56f); quadraticTo(60f, 50f, 54f, 64f); close() }
                s.oval(Tone.PRIMARY, 50f, 56f, 15f, 22f); s.mpoly(Tone.PRIMARY, 38f, 40f, 34f, 22f, 46f, 34f)
                s.eyes(52f, 7f, 3.4f, Tone.GEM); s.fangs(45f, 64f, 5f, 2, 6f)
            }
            6 -> { // spider
                for (i in 0 until 4) { val y = 30f + i * 12f; s.curve(Tone.DARK, 3f, 42f, 50f, 20f, y - 14f, 6f, y + 8f); s.curve(Tone.DARK, 3f, 58f, 50f, 80f, y - 14f, 94f, y + 8f) }
                s.oval(Tone.PRIMARY, 50f, 66f, 20f, 24f); s.circle(Tone.SHADE, 50f, 66f, 9f, outline = false, alpha = 0.6f); s.circle(Tone.PRIMARY, 50f, 40f, 14f)
                for (p in listOf(44f to 36f, 56f to 36f, 41f to 42f, 59f to 42f)) s.circle(Tone.GEM, p.first, p.second, 3f)
            }
            7 -> { // wolf
                s.poly(Tone.PRIMARY, 22f, 44f, 20f, 8f, 42f, 28f); s.poly(Tone.PRIMARY, 78f, 44f, 80f, 8f, 58f, 28f)
                s.path(Tone.PRIMARY) { moveTo(18f, 50f); cubicTo(22f, 26f, 78f, 26f, 82f, 50f); cubicTo(84f, 70f, 64f, 78f, 62f, 90f); lineTo(38f, 90f); cubicTo(36f, 78f, 16f, 70f, 18f, 50f); close() }
                s.path(Tone.LIGHT, alpha = 0.7f) { moveTo(36f, 62f); quadraticTo(50f, 54f, 64f, 62f); lineTo(60f, 90f); lineTo(40f, 90f); close() }
                s.eyes(46f, 14f, 4.4f, Tone.GOLD); s.oval(Tone.DARK, 50f, 80f, 7f, 5f); s.fangs(40f, 86f, 7f, 3, 4f)
            }
            8 -> { // dragon
                s.path(Tone.PAPER) { moveTo(24f, 36f); cubicTo(10f, 30f, 6f, 10f, 12f, 2f); cubicTo(22f, 8f, 28f, 20f, 36f, 28f); close() }
                s.path(Tone.PAPER) { moveTo(76f, 36f); cubicTo(90f, 30f, 94f, 10f, 88f, 2f); cubicTo(78f, 8f, 72f, 20f, 64f, 28f); close() }
                s.path(Tone.PRIMARY) { moveTo(16f, 46f); cubicTo(20f, 20f, 80f, 20f, 84f, 46f); cubicTo(86f, 60f, 78f, 70f, 70f, 76f); lineTo(70f, 92f); lineTo(30f, 92f); lineTo(30f, 76f); cubicTo(22f, 70f, 14f, 60f, 16f, 46f); close() }
                s.mpoly(Tone.SHADE, 18f, 46f, 6f, 54f, 18f, 60f, alpha = 0.8f)
                s.eyes(42f, 15f, 4.6f, Tone.GOLD); s.oval(Tone.DARK, 42f, 78f, 3.4f, 4f); s.oval(Tone.DARK, 58f, 78f, 3.4f, 4f); s.fangs(34f, 86f, 8f, 4, 6f)
                s.line(Tone.SHADE, 50f, 22f, 50f, 40f, 2f, 0.6f)
            }
            9 -> { // imp
                s.path(Tone.DARK) { moveTo(24f, 38f); cubicTo(12f, 30f, 14f, 10f, 22f, 4f); cubicTo(26f, 16f, 34f, 24f, 40f, 28f); close() }
                s.path(Tone.DARK) { moveTo(76f, 38f); cubicTo(88f, 30f, 86f, 10f, 78f, 4f); cubicTo(74f, 16f, 66f, 24f, 60f, 28f); close() }
                s.oval(Tone.PRIMARY, 50f, 56f, 30f, 32f); s.mpoly(Tone.PRIMARY, 22f, 54f, 6f, 46f, 20f, 62f)
                s.eyes(48f, 13f, 5f, Tone.WHITE); s.line(Tone.DARK, 36f, 38f, 46f, 44f, 2.6f); s.line(Tone.DARK, 64f, 38f, 54f, 44f, 2.6f)
                s.path(Tone.DARK) { moveTo(34f, 68f); quadraticTo(50f, 90f, 66f, 68f); quadraticTo(50f, 74f, 34f, 68f); close() }; s.fangs(40f, 70f, 6f, 3, 5f)
            }
            10 -> { // ogre
                s.oval(Tone.PRIMARY, 50f, 54f, 38f, 38f); s.oval(Tone.SHADE, 50f, 80f, 26f, 10f, outline = false, alpha = 0.4f)
                s.rect(Tone.SHADE, 20f, 34f, 60f, 8f, alpha = 0.7f); s.eyes(48f, 15f, 3.6f, Tone.GEM); s.oval(Tone.SHADE, 50f, 60f, 6f, 8f)
                s.rrect(Tone.DARK, 28f, 72f, 44f, 12f, 4f); s.poly(Tone.PAPER, 34f, 72f, 30f, 60f, 40f, 72f); s.poly(Tone.PAPER, 66f, 72f, 70f, 60f, 60f, 72f); s.mpoly(Tone.PRIMARY, 14f, 50f, 2f, 46f, 12f, 62f)
            }
            11 -> { // wraith
                s.path(Tone.DARK) { moveTo(10f, 94f); cubicTo(14f, 56f, 20f, 14f, 50f, 6f); cubicTo(80f, 14f, 86f, 56f, 90f, 94f); lineTo(76f, 84f); lineTo(62f, 94f); lineTo(50f, 82f); lineTo(38f, 94f); lineTo(24f, 84f); close() }
                s.path(Tone.SHADE, alpha = 0.7f) { moveTo(50f, 6f); cubicTo(80f, 14f, 86f, 56f, 90f, 94f); lineTo(76f, 84f); lineTo(62f, 94f); lineTo(50f, 82f); close() }
                s.oval(Tone.DARK, 50f, 46f, 22f, 26f); s.oval(Tone.GEM, 40f, 44f, 5.4f, 3.6f); s.oval(Tone.GEM, 60f, 44f, 5.4f, 3.6f)
                s.oval(Tone.WHITE, 40f, 44f, 2.4f, 1.6f, outline = false)
            }
            12 -> { // golem
                s.rrect(Tone.METAL_DARK, 18f, 14f, 64f, 70f, 8f); s.rrect(Tone.PRIMARY, 22f, 18f, 56f, 62f, 6f, alpha = 0.35f)
                s.rect(Tone.DARK, 28f, 36f, 44f, 12f); s.rect(Tone.GEM, 31f, 39f, 12f, 6f); s.rect(Tone.GEM, 57f, 39f, 12f, 6f)
                s.rect(Tone.DARK, 34f, 62f, 32f, 8f); for (i in 0 until 4) s.line(Tone.METAL_DARK, 40f + i * 7f, 62f, 40f + i * 7f, 70f, 1.6f)
                s.line(Tone.GEM, 24f, 24f, 40f, 30f, 1.6f, 0.9f); s.line(Tone.GEM, 72f, 74f, 60f, 62f, 1.6f, 0.9f); s.rrect(Tone.METAL_DARK, 38f, 84f, 24f, 8f, 3f)
            }
            13 -> { // snake
                s.strokePath(Tone.PRIMARY, 14f) { moveTo(22f, 90f); cubicTo(-8f, 70f, 40f, 62f, 66f, 66f); cubicTo(96f, 70f, 86f, 40f, 54f, 38f) }
                s.strokePath(Tone.SHADE, 3f, alpha = 0.6f) { moveTo(22f, 90f); cubicTo(-8f, 70f, 40f, 62f, 66f, 66f); cubicTo(96f, 70f, 86f, 40f, 54f, 38f) }
                s.oval(Tone.PRIMARY, 46f, 30f, 17f, 20f); s.eyes(26f, 7f, 3.6f, Tone.GOLD); s.curve(Tone.GEM, 1.8f, 46f, 48f, 46f, 58f, 40f, 64f); s.curve(Tone.GEM, 1.8f, 46f, 58f, 52f, 64f)
            }
            14 -> { // mimic
                s.rrect(Tone.WOOD, 12f, 46f, 76f, 40f, 5f); s.path(Tone.WOOD) { moveTo(12f, 46f); lineTo(12f, 24f); cubicTo(12f, 0f, 88f, 0f, 88f, 24f); lineTo(88f, 46f); close() }
                s.rect(Tone.PRIMARY, 28f, 6f, 8f, 80f, alpha = 0.9f); s.rect(Tone.PRIMARY, 64f, 6f, 8f, 80f, alpha = 0.9f)
                s.path(Tone.DARK) { moveTo(14f, 46f); lineTo(86f, 46f); lineTo(80f, 60f); lineTo(20f, 60f); close() }
                s.fangs(18f, 46f, 9f, 8, 9f); for (i in 0 until 7) s.poly(Tone.PAPER, 22f + i * 9f, 60f, 26f + i * 9f, 60f, 24f + i * 9f, 51f)
                s.path(Tone.GEM) { moveTo(36f, 58f); quadraticTo(50f, 82f, 64f, 58f); close() }; s.eyes(30f, 14f, 4.4f, Tone.GOLD)
            }
            15 -> { // treant
                s.path(Tone.WOOD) { moveTo(22f, 92f); lineTo(30f, 40f); lineTo(70f, 40f); lineTo(78f, 92f); close() }
                s.path(Tone.PRIMARY) { moveTo(6f, 44f); cubicTo(-4f, 14f, 30f, -2f, 50f, 8f); cubicTo(70f, -2f, 104f, 14f, 94f, 44f); cubicTo(80f, 52f, 20f, 52f, 6f, 44f); close() }
                for (p in listOf(22f to 24f, 50f to 18f, 78f to 24f, 36f to 38f, 64f to 38f)) s.circle(Tone.LIGHT, p.first, p.second, 5f, outline = false, alpha = 0.5f)
                s.eyes(60f, 11f, 4f, Tone.GOLD); s.oval(Tone.DARK, 50f, 78f, 9f, 6f); s.line(Tone.WOOD_DARK, 34f, 50f, 40f, 90f, 1.4f, 0.7f); s.line(Tone.WOOD_DARK, 66f, 50f, 60f, 90f, 1.4f, 0.7f)
            }
            16 -> { // cyclops
                s.oval(Tone.PRIMARY, 50f, 54f, 36f, 38f); s.circle(Tone.WHITE, 50f, 46f, 19f, alpha = 1f); s.circle(Tone.GEM, 50f, 46f, 11f); s.circle(Tone.DARK, 50f, 46f, 5.2f)
                s.line(Tone.DARK, 28f, 28f, 72f, 30f, 3.4f); s.path(Tone.DARK) { moveTo(30f, 70f); quadraticTo(50f, 88f, 70f, 70f); quadraticTo(50f, 76f, 30f, 70f); close() }; s.fangs(40f, 72f, 7f, 3, 5f)
                s.mpoly(Tone.PRIMARY, 16f, 54f, 4f, 50f, 14f, 66f)
            }
            17 -> { // vampire
                s.path(Tone.DARK) { moveTo(12f, 94f); lineTo(20f, 40f); lineTo(80f, 40f); lineTo(88f, 94f); close() }
                s.path(Tone.PAPER) { moveTo(24f, 44f); cubicTo(22f, 16f, 78f, 16f, 76f, 44f); cubicTo(76f, 66f, 62f, 78f, 50f, 78f); cubicTo(38f, 78f, 24f, 66f, 24f, 44f); close() }
                s.poly(Tone.DARK, 24f, 40f, 50f, 36f, 76f, 40f, 76f, 30f, 50f, 20f, 24f, 30f); s.poly(Tone.DARK, 50f, 20f, 56f, 36f, 50f, 40f, 44f, 36f)
                s.eyes(50f, 11f, 3.6f, Tone.GEM); s.curve(Tone.PRIMARY, 2f, 40f, 64f, 50f, 68f, 60f, 64f); s.poly(Tone.WHITE, 43f, 64f, 46f, 74f, 48f, 65f); s.poly(Tone.WHITE, 57f, 64f, 54f, 74f, 52f, 65f)
            }
            18 -> { // troll
                s.mpoly(Tone.PRIMARY, 16f, 50f, 0f, 36f, 8f, 62f)
                s.oval(Tone.PRIMARY, 50f, 52f, 34f, 38f); s.oval(Tone.SHADE, 50f, 58f, 12f, 14f); for (p in listOf(36f to 36f, 66f to 70f, 60f to 30f)) s.circle(Tone.SHADE, p.first, p.second, 2.6f, outline = false)
                s.eyes(42f, 13f, 3.4f, Tone.GOLD); s.rrect(Tone.DARK, 30f, 74f, 40f, 12f, 4f); s.poly(Tone.PAPER, 34f, 76f, 30f, 62f, 40f, 76f); s.poly(Tone.PAPER, 66f, 76f, 70f, 62f, 60f, 76f)
                s.path(Tone.SHADE, alpha = 0.7f) { moveTo(30f, 22f); quadraticTo(50f, 6f, 70f, 22f); lineTo(70f, 30f); lineTo(30f, 30f); close() }
            }
            else -> { // kraken
                for (i in 0 until 5) { val x = 14f + i * 18f; s.strokePath(Tone.PRIMARY, 8f) { moveTo(x, 54f); cubicTo(x - 14f, 70f, x + 14f, 80f, x + (if (i % 2 == 0) -10f else 10f), 94f) } }
                s.path(Tone.PRIMARY) { moveTo(14f, 56f); cubicTo(6f, 4f, 94f, 4f, 86f, 56f); cubicTo(70f, 64f, 30f, 64f, 14f, 56f); close() }
                s.path(Tone.SHADE, alpha = 0.5f) { moveTo(50f, 6f); cubicTo(94f, 4f, 94f, 40f, 86f, 56f); cubicTo(70f, 64f, 50f, 64f, 50f, 64f); close() }
                s.eyes(42f, 14f, 6f, Tone.WHITE); s.circle(Tone.LIGHT, 36f, 20f, 3f, outline = false, alpha = 0.7f)
                for (p in listOf(20f to 78f, 38f to 80f, 62f to 80f, 80f to 78f)) s.circle(Tone.LIGHT, p.first, p.second, 1.8f, outline = false, alpha = 0.7f)
            }
        }
    }
}
