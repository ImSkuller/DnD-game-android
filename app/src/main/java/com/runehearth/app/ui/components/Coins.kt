package com.runehearth.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.runehearth.app.core.Coin
import com.runehearth.app.ui.theme.LocalGlowPhase

private class CoinStyle(val light: Color, val mid: Color, val dark: Color, val rim: Color, val glyph: Color)

private fun styleOf(c: Coin) = when (c) {
    Coin.BRONZE -> CoinStyle(Color(0xFFE0A56A), Color(0xFFB0713A), Color(0xFF6B3E1B), Color(0xFF8C5527), Color(0xFF4E2B12))
    Coin.SILVER -> CoinStyle(Color(0xFFF4F7FA), Color(0xFFBCC6CF), Color(0xFF6E7B87), Color(0xFF93A0AB), Color(0xFF47525C))
    Coin.GOLD -> CoinStyle(Color(0xFFFFE699), Color(0xFFE2B13C), Color(0xFF8E6118), Color(0xFFC08E26), Color(0xFF6B470F))
    Coin.HOLY_GOLD -> CoinStyle(Color(0xFFFFFFF0), Color(0xFFFFE27A), Color(0xFFD59B2E), Color(0xFFF2C94C), Color(0xFFA56F14))
}

/**
 * Ancient-Chinese "cash" coin: round body, raised rim, diamond-shaped (rotated square) hole, four glyph strokes.
 * Holy gold additionally has radiating light rays and an animated shine.
 */
fun DrawScope.drawCoin(coin: Coin, phase: Float = 0f) {
    val st = styleOf(coin)
    val u = size.minDimension / 100f
    scale(u, u, Offset.Zero) {
        val c = Offset(50f, 50f)
        if (coin == Coin.HOLY_GOLD) {
            val pulse = 0.7f + 0.3f * kotlin.math.sin(phase * 6.2831855f)
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF2B0).copy(alpha = 0.75f * pulse), Color.Transparent), c, 52f), 52f, c)
            for (i in 0 until 12) {
                val a = i * Math.PI.toFloat() / 6f
                drawLine(Color(0xFFFFF2B0).copy(alpha = 0.55f * pulse), Offset(50f + 40f * kotlin.math.cos(a), 50f + 40f * kotlin.math.sin(a)), Offset(50f + 50f * kotlin.math.cos(a), 50f + 50f * kotlin.math.sin(a)), 2f, StrokeCap.Round)
            }
        }
        drawCircle(Color.Black.copy(alpha = 0.35f), 40f, Offset(52f, 54f))
        drawCircle(Brush.radialGradient(listOf(st.light, st.mid, st.dark), Offset(38f, 34f), 70f), 40f, c)
        drawCircle(st.rim, 40f, c, style = Stroke(2.2f))
        drawCircle(st.dark.copy(alpha = 0.55f), 33.5f, c, style = Stroke(1.4f))
        drawCircle(st.light.copy(alpha = 0.7f), 31.8f, c, style = Stroke(1f))
        // glyph strokes (stylised characters) top / bottom / left / right of the hole
        fun glyph(cx: Float, cy: Float, vertical: Boolean) {
            val p = Path().apply {
                if (vertical) { moveTo(cx - 5f, cy - 4f); lineTo(cx + 5f, cy - 4f); moveTo(cx, cy - 6f); lineTo(cx, cy + 6f); moveTo(cx - 4f, cy + 1f); lineTo(cx + 4f, cy + 4f) }
                else { moveTo(cx - 4f, cy - 5f); lineTo(cx - 4f, cy + 5f); moveTo(cx - 4f, cy); lineTo(cx + 5f, cy); moveTo(cx + 1f, cy - 5f); lineTo(cx + 5f, cy + 5f) }
            }
            drawPath(p, st.glyph.copy(alpha = 0.85f), style = Stroke(2f, cap = StrokeCap.Round))
        }
        glyph(50f, 22f, true); glyph(50f, 78f, true); glyph(22f, 50f, false); glyph(78f, 50f, false)
        // diamond hole
        val hole = Path().apply { moveTo(50f, 36f); lineTo(64f, 50f); lineTo(50f, 64f); lineTo(36f, 50f); close() }
        drawPath(hole, Color(0xFF120A05))
        drawPath(hole, st.rim, style = Stroke(2.4f))
        drawPath(Path().apply { moveTo(50f, 33f); lineTo(67f, 50f); lineTo(50f, 67f); lineTo(33f, 50f); close() }, st.light.copy(alpha = 0.55f), style = Stroke(1f))
        // highlight
        drawCircle(Color.White.copy(alpha = if (coin == Coin.HOLY_GOLD) 0.5f else 0.3f), 40f, c, style = Stroke(1.4f))
        drawArcHighlight(st.light)
        if (coin == Coin.HOLY_GOLD) {
            val x = -20f + 140f * phase
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent), Offset(x, 40f), 18f), 18f, Offset(x, 40f), alpha = 0.6f)
        }
    }
}

private fun DrawScope.drawArcHighlight(light: Color) {
    drawArc(Color.White.copy(alpha = 0.55f), 200f, 60f, false, Offset(14f, 14f), androidx.compose.ui.geometry.Size(72f, 72f), style = Stroke(2.4f, cap = StrokeCap.Round))
    drawArc(light.copy(alpha = 0.4f), 20f, 40f, false, Offset(14f, 14f), androidx.compose.ui.geometry.Size(72f, 72f), style = Stroke(1.6f, cap = StrokeCap.Round))
}

@Composable
fun CoinImage(coin: Coin, size: Dp = 32.dp, modifier: Modifier = Modifier) {
    val phase: State<Float> = LocalGlowPhase.current
    Canvas(modifier.size(size)) { drawCoin(coin, if (coin == Coin.HOLY_GOLD) phase.value else 0f) }
}
