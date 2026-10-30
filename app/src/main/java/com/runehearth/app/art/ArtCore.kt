package com.runehearth.app.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

/** Art categories. [designs] is the number of base designs; every design supports 20 colours × glow on/off. */
enum class ArtCategory(val code: String, val label: String, val designs: Int) {
    WEAPON("w", "Weapons", 20), OFFHAND("o", "Shields & Off-hand", 20), HELMET("h", "Helmets", 20),
    CHEST_ARMOR("c", "Chestplates", 20), GLOVES("g", "Gloves", 20), LEGGINGS("l", "Leggings", 20),
    BOOTS("b", "Boots", 20), ACCESSORY("a", "Accessories", 20), POTION("p", "Potions", 20),
    RUNE("r", "Runes", 30), HERB("e", "Herbs & Plants", 20), BOOK("k", "Books", 10),
    CHEST("x", "Chests", 20), MATERIAL("m", "Materials", 20), MISC("i", "Misc Items", 20),
    MONSTER("n", "Monsters", 20);

    companion object {
        private val byCode = entries.associateBy { it.code }
        fun fromCode(c: String): ArtCategory? = byCode[c]
    }
}

/** Compact reference to a rendered asset: design + colour + glow. Encodes to `"<cat>.<design>.<color>.<glow>"`. */
data class AssetRef(val category: ArtCategory, val design: Int, val color: Int = 0, val glow: Boolean = false) {
    fun encode(): String = "${category.code}.$design.$color.${if (glow) 1 else 0}"

    companion object {
        const val COLORS = 20
        fun decode(s: String): AssetRef? {
            val p = s.split('.')
            if (p.size != 4) return null
            val cat = ArtCategory.fromCode(p[0]) ?: return null
            val d = p[1].toIntOrNull() ?: return null
            val c = p[2].toIntOrNull() ?: return null
            return AssetRef(cat, d.coerceIn(0, cat.designs - 1), c.coerceIn(0, COLORS - 1), p[3] == "1")
        }
        fun default(cat: ArtCategory) = AssetRef(cat, 0, 0, false)
    }
}

/** One of 20 selectable colour ramps. */
class ArtColor(val name: String, val base: Color) {
    val shade: Color = lerp(base, Color(0xFF14080A), 0.42f)
    val light: Color = lerp(base, Color.White, 0.45f)
    val gem: Color = lerp(base, Color.White, 0.18f)
    val blade: Color = lerp(Color(0xFFD5DBE0), base, 0.5f)
    val bladeDark: Color = lerp(Color(0xFF7C8791), base, 0.55f)
}

object Palette {
    val colors: List<ArtColor> = listOf(
        ArtColor("Crimson", Color(0xFFC62828)), ArtColor("Scarlet", Color(0xFFE64A19)), ArtColor("Amber", Color(0xFFF59E0B)),
        ArtColor("Sungold", Color(0xFFE5C100)), ArtColor("Lime", Color(0xFF8BC34A)), ArtColor("Emerald", Color(0xFF1FA45B)),
        ArtColor("Forest", Color(0xFF2E7D32)), ArtColor("Teal", Color(0xFF00897B)), ArtColor("Cyan", Color(0xFF26C6DA)),
        ArtColor("Azure", Color(0xFF2196F3)), ArtColor("Sapphire", Color(0xFF1A47C4)), ArtColor("Indigo", Color(0xFF4527A0)),
        ArtColor("Violet", Color(0xFF8E44AD)), ArtColor("Magenta", Color(0xFFD81B8A)), ArtColor("Rose", Color(0xFFF48FB1)),
        ArtColor("Silver", Color(0xFFB0BEC5)), ArtColor("Obsidian", Color(0xFF37343F)), ArtColor("Ivory", Color(0xFFEDE3C8)),
        ArtColor("Copper", Color(0xFFB8672D)), ArtColor("Mythril", Color(0xFF7FE5D2)),
    )
    fun of(i: Int): ArtColor = colors[i.coerceIn(0, colors.lastIndex)]
}

/** Material slots of a shape; coloured tones follow the selected [ArtColor]. */
enum class Tone { PRIMARY, SHADE, LIGHT, GEM, BLADE, BLADE_DARK, METAL, METAL_DARK, METAL_LIGHT, GOLD, GOLD_DARK, WOOD, WOOD_DARK, LEATHER, LEATHER_DARK, OUTLINE, WHITE, GLASS, PAPER, SKIN, DARK, LEAF, LEAF_DARK }

private val METAL = Color(0xFFB9C1C9)
private val METAL_DARK = Color(0xFF69747F)
private val METAL_LIGHT = Color(0xFFEEF3F6)
private val GOLD = Color(0xFFE2B13C)
private val GOLD_DARK = Color(0xFF9A6B1C)
private val WOOD = Color(0xFF8B5A2B)
private val WOOD_DARK = Color(0xFF583519)
private val LEATHER = Color(0xFF8A5530)
private val LEATHER_DARK = Color(0xFF55321A)
private val OUTLINE = Color(0xFF1E120C)
private val PAPER = Color(0xFFEBDDB5)
private val SKIN = Color(0xFFD9B38C)
private val LEAF = Color(0xFF4E9A3E)
private val LEAF_DARK = Color(0xFF2E6428)

fun Tone.resolve(c: ArtColor): Color = when (this) {
    Tone.PRIMARY -> c.base; Tone.SHADE -> c.shade; Tone.LIGHT -> c.light; Tone.GEM -> c.gem
    Tone.BLADE -> c.blade; Tone.BLADE_DARK -> c.bladeDark
    Tone.METAL -> METAL; Tone.METAL_DARK -> METAL_DARK; Tone.METAL_LIGHT -> METAL_LIGHT
    Tone.GOLD -> GOLD; Tone.GOLD_DARK -> GOLD_DARK; Tone.WOOD -> WOOD; Tone.WOOD_DARK -> WOOD_DARK
    Tone.LEATHER -> LEATHER; Tone.LEATHER_DARK -> LEATHER_DARK; Tone.OUTLINE -> OUTLINE
    Tone.WHITE -> Color.White.copy(alpha = 0.55f); Tone.GLASS -> Color(0xFFDDF0FF).copy(alpha = 0.32f)
    Tone.PAPER -> PAPER; Tone.SKIN -> SKIN; Tone.LEAF -> LEAF; Tone.LEAF_DARK -> LEAF_DARK; Tone.DARK -> Color(0xFF2A2230)
}

class Layer(val path: Path, val tone: Tone, val stroke: Float, val outline: Boolean, val alpha: Float, val cap: Boolean = true)

/** Collects layered vector shapes in a 100×100 design space. */
class Sketch {
    val layers = ArrayList<Layer>(24)
    var rot = 0f
    var scale = 1f

    private fun pathOf(p: FloatArray, close: Boolean = true) = Path().apply {
        moveTo(p[0], p[1])
        var i = 2
        while (i + 1 < p.size) { lineTo(p[i], p[i + 1]); i += 2 }
        if (close) close()
    }

    fun poly(tone: Tone, vararg p: Float, outline: Boolean = true, alpha: Float = 1f) {
        layers += Layer(pathOf(p), tone, 0f, outline, alpha)
    }

    /** Polygon plus its mirror image about x = 50. */
    fun mpoly(tone: Tone, vararg p: Float, outline: Boolean = true, alpha: Float = 1f) {
        poly(tone, *p, outline = outline, alpha = alpha)
        val m = FloatArray(p.size)
        for (i in p.indices step 2) { m[i] = 100f - p[i]; m[i + 1] = p[i + 1] }
        poly(tone, *m, outline = outline, alpha = alpha)
    }

    fun circle(tone: Tone, cx: Float, cy: Float, r: Float, outline: Boolean = true, alpha: Float = 1f) {
        layers += Layer(Path().apply { addOval(androidx.compose.ui.geometry.Rect(cx - r, cy - r, cx + r, cy + r)) }, tone, 0f, outline, alpha)
    }

    fun oval(tone: Tone, cx: Float, cy: Float, rx: Float, ry: Float, outline: Boolean = true, alpha: Float = 1f) {
        layers += Layer(Path().apply { addOval(androidx.compose.ui.geometry.Rect(cx - rx, cy - ry, cx + rx, cy + ry)) }, tone, 0f, outline, alpha)
    }

    fun rect(tone: Tone, x: Float, y: Float, w: Float, h: Float, outline: Boolean = true, alpha: Float = 1f) {
        layers += Layer(Path().apply { addRect(androidx.compose.ui.geometry.Rect(x, y, x + w, y + h)) }, tone, 0f, outline, alpha)
    }

    fun rrect(tone: Tone, x: Float, y: Float, w: Float, h: Float, r: Float, outline: Boolean = true, alpha: Float = 1f) {
        layers += Layer(Path().apply {
            addRoundRect(androidx.compose.ui.geometry.RoundRect(x, y, x + w, y + h, androidx.compose.ui.geometry.CornerRadius(r, r)))
        }, tone, 0f, outline, alpha)
    }

    fun path(tone: Tone, outline: Boolean = true, alpha: Float = 1f, block: Path.() -> Unit) {
        layers += Layer(Path().apply(block), tone, 0f, outline, alpha)
    }

    fun line(tone: Tone, x1: Float, y1: Float, x2: Float, y2: Float, w: Float = 2f, alpha: Float = 1f) {
        layers += Layer(Path().apply { moveTo(x1, y1); lineTo(x2, y2) }, tone, w, false, alpha)
    }

    /** Open polyline stroke. */
    fun curve(tone: Tone, w: Float, vararg p: Float, alpha: Float = 1f) {
        layers += Layer(pathOf(p, close = false), tone, w, false, alpha)
    }

    fun strokePath(tone: Tone, w: Float, alpha: Float = 1f, block: Path.() -> Unit) {
        layers += Layer(Path().apply(block), tone, w, false, alpha)
    }
}

fun interface Painter { fun paint(design: Int, s: Sketch) }

object AssetCatalog {
    private val painters: Map<ArtCategory, Painter> by lazy {
        mapOf(
            ArtCategory.WEAPON to WeaponPainter, ArtCategory.OFFHAND to ShieldPainter, ArtCategory.HELMET to HelmetPainter,
            ArtCategory.CHEST_ARMOR to ChestPainter, ArtCategory.GLOVES to GlovePainter, ArtCategory.LEGGINGS to LeggingsPainter,
            ArtCategory.BOOTS to BootPainter, ArtCategory.ACCESSORY to AccessoryPainter, ArtCategory.POTION to PotionPainter,
            ArtCategory.RUNE to RunePainter, ArtCategory.HERB to HerbPainter, ArtCategory.BOOK to BookPainter,
            ArtCategory.CHEST to ChestBoxPainter, ArtCategory.MATERIAL to MaterialPainter, ArtCategory.MISC to MiscPainter,
            ArtCategory.MONSTER to MonsterPainter,
        )
    }
    private val cache = HashMap<Int, Sketch>()

    /** Built sketches are cached per (category, design): paths are created once and shared by every colour/glow variant. */
    fun sketch(cat: ArtCategory, design: Int): Sketch {
        val d = design.coerceIn(0, cat.designs - 1)
        val key = cat.ordinal * 1000 + d
        return cache.getOrPut(key) { Sketch().also { painters.getValue(cat).paint(d, it) } }
    }

    fun clearCache() = cache.clear()

    /** Search by category label, design number or colour name, e.g. "sword", "rune 12", "crimson". */
    fun search(query: String): List<ArtCategory> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return ArtCategory.entries
        return ArtCategory.entries.filter { it.label.lowercase().contains(q) || it.name.lowercase().contains(q) || designNames(it).any { n -> n.contains(q) } }
    }

    fun designNames(cat: ArtCategory): List<String> = DesignNames.of(cat)
}

/** Draws [ref] into the current scope, fitting its smaller dimension. [glowPhase] 0..1 animates the enchant shimmer. */
fun DrawScope.drawAsset(ref: AssetRef, glowPhase: Float = 0.35f) {
    val sk = AssetCatalog.sketch(ref.category, ref.design)
    val col = Palette.of(ref.color)
    val unit = size.minDimension / 100f
    val ox = (size.width - size.minDimension) / 2f
    val oy = (size.height - size.minDimension) / 2f
    withTransform({
        translate(ox, oy)
        scale(unit, unit, Offset.Zero)
    }) {
        if (ref.glow) {
            val pulse = 0.75f + 0.25f * kotlin.math.sin(glowPhase * 6.2831855f)
            drawCircle(
                Brush.radialGradient(listOf(col.light.copy(alpha = 0.55f * pulse), col.base.copy(alpha = 0.18f * pulse), Color.Transparent), Offset(50f, 50f), 56f),
                radius = 56f, center = Offset(50f, 50f),
            )
        }
        val body: DrawScope.() -> Unit = {
            rotate(sk.rot, Offset(50f, 50f)) {
                scale(sk.scale, sk.scale, Offset(50f, 50f)) {
                    for (l in sk.layers) {
                        val c = l.tone.resolve(col).let { if (l.alpha < 1f) it.copy(alpha = it.alpha * l.alpha) else it }
                        if (l.stroke > 0f) {
                            drawPath(l.path, c, style = Stroke(l.stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        } else {
                            drawPath(l.path, c, style = Fill)
                            if (l.outline) drawPath(l.path, OUTLINE.copy(alpha = 0.55f), style = Stroke(1.1f, join = StrokeJoin.Round))
                        }
                    }
                }
            }
        }
        if (ref.glow) {
            // enchant shimmer: a diagonal light band composited only over the item's own pixels
            drawIntoCanvas { canvas ->
                val bounds = androidx.compose.ui.geometry.Rect(-10f, -10f, 110f, 110f)
                val paint = androidx.compose.ui.graphics.Paint()
                canvas.saveLayer(bounds, paint)
                body()
                val x = -60f + 220f * glowPhase
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, col.light.copy(alpha = 0.85f), Color.Transparent),
                        Offset(x - 25f, 0f), Offset(x + 25f, 70f),
                    ),
                    Offset(-10f, -10f), Size(120f, 120f), blendMode = BlendMode.SrcAtop,
                )
                canvas.restore()
            }
        } else body()
    }
}
