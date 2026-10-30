package com.runehearth.app.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.random.Random

/**
 * Procedural, tileable surface textures (one small 192² bitmap each, generated lazily on first use and shared).
 * No texture files are shipped; total memory ≈ 4 × 144 KB once used.
 */
object Textures {
    private const val SIZE = 192

    private fun make(seed: Int, draw: androidx.compose.ui.graphics.drawscope.DrawScope.(Random) -> Unit): Brush {
        val bmp = ImageBitmap(SIZE, SIZE)
        val scope = CanvasDrawScope()
        scope.draw(Density(1f), LayoutDirection.Ltr, Canvas(bmp), Size(SIZE.toFloat(), SIZE.toFloat())) { draw(Random(seed)) }
        return ShaderBrush(ImageShader(bmp, TileMode.Repeated, TileMode.Repeated))
    }

    val parchment: Brush by lazy {
        make(11) { r ->
            drawRect(Color(0xFFE9D9AE))
            repeat(900) { drawCircle(Color(0xFF8A6A32).copy(alpha = r.nextFloat() * 0.07f), r.nextFloat() * 1.6f + 0.4f, Offset(r.nextFloat() * SIZE, r.nextFloat() * SIZE)) }
            repeat(260) { drawCircle(Color.White.copy(alpha = r.nextFloat() * 0.10f), r.nextFloat() * 1.4f + 0.3f, Offset(r.nextFloat() * SIZE, r.nextFloat() * SIZE)) }
            repeat(40) { val x = r.nextFloat() * SIZE; val y = r.nextFloat() * SIZE; drawLine(Color(0xFF8A6A32).copy(alpha = 0.06f), Offset(x, y), Offset(x + r.nextFloat() * 14 - 7, y + r.nextFloat() * 14 - 7), 1f) }
        }
    }

    val wood: Brush by lazy {
        make(23) { r ->
            drawRect(Color(0xFF3A2415))
            repeat(46) {
                val y = r.nextFloat() * SIZE
                val c = if (r.nextBoolean()) Color(0xFF1D1008) else Color(0xFF5D3B20)
                val amp = r.nextFloat() * 3f
                var px = 0f; var py = y
                var x = 0f
                while (x < SIZE) { x += 12f; val ny = y + kotlin.math.sin(x * 0.05f + y) * amp; drawLine(c.copy(alpha = 0.22f + r.nextFloat() * 0.2f), Offset(px, py), Offset(x, ny), 1f + r.nextFloat()); px = x; py = ny }
            }
            repeat(2) { val cx = r.nextFloat() * SIZE; val cy = r.nextFloat() * SIZE; for (k in 1..4) drawCircle(Color(0xFF150A04).copy(alpha = 0.18f), k * 2.6f, Offset(cx, cy), style = Stroke(1f)) }
        }
    }

    val stone: Brush by lazy {
        make(37) { r ->
            drawRect(Color(0xFF4A4642))
            repeat(1100) { val v = r.nextFloat(); drawCircle((if (v > 0.5f) Color.White else Color.Black).copy(alpha = r.nextFloat() * 0.12f), r.nextFloat() * 2.2f + 0.4f, Offset(r.nextFloat() * SIZE, r.nextFloat() * SIZE)) }
            repeat(7) { var x = r.nextFloat() * SIZE; var y = r.nextFloat() * SIZE; repeat(5) { val nx = x + r.nextFloat() * 22 - 11; val ny = y + r.nextFloat() * 22 - 11; drawLine(Color.Black.copy(alpha = 0.22f), Offset(x, y), Offset(nx, ny), 1f); x = nx; y = ny } }
        }
    }

    val leather: Brush by lazy {
        make(51) { r ->
            drawRect(Color(0xFF5A3920))
            repeat(700) { drawCircle(Color(0xFF2B190C).copy(alpha = r.nextFloat() * 0.25f), r.nextFloat() * 2.2f + 0.8f, Offset(r.nextFloat() * SIZE, r.nextFloat() * SIZE), style = Stroke(0.8f)) }
            repeat(300) { drawCircle(Color(0xFFA06B3C).copy(alpha = r.nextFloat() * 0.12f), r.nextFloat() * 1.6f + 0.4f, Offset(r.nextFloat() * SIZE, r.nextFloat() * SIZE)) }
        }
    }
}
