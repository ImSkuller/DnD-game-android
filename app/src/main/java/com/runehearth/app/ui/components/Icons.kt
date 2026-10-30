package com.runehearth.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Custom medieval line icons drawn in a 24×24 box (no icon library dependency). */
enum class RhIcon {
    CASTLE, SHIELD, COIN, DICE, SWORDS, SCROLL, USER, BAG, BOOK, GEAR, BACK, PLUS, MINUS, CLOSE, CHECK, SEARCH, HEART, STAR, FLAME,
    WIFI, CROWN, SKULL, TRASH, EDIT, ARROW, LOCK, EYE, POTION, WAND, HELM, GEM, MAP, CLOCK, SHARE
}

@Composable
fun RhIconView(icon: RhIcon, tint: Color, size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) { drawRhIcon(icon, tint) }
}

fun DrawScope.drawRhIcon(icon: RhIcon, c: Color) {
    val u = this.size.minDimension / 24f
    scale(u, u, Offset.Zero) {
        val st = Stroke(1.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(b: Path.() -> Unit) = Path().apply(b)
        when (icon) {
            RhIcon.CASTLE -> {
                drawPath(p { moveTo(3f, 21f); lineTo(3f, 9f); lineTo(5f, 9f); lineTo(5f, 11f); lineTo(7f, 11f); lineTo(7f, 9f); lineTo(9f, 9f); lineTo(9f, 6f); lineTo(11f, 6f); lineTo(11f, 4f); lineTo(13f, 4f); lineTo(13f, 6f); lineTo(15f, 6f); lineTo(15f, 9f); lineTo(17f, 9f); lineTo(17f, 11f); lineTo(19f, 11f); lineTo(19f, 9f); lineTo(21f, 9f); lineTo(21f, 21f); close() }, c, style = st)
                drawPath(p { moveTo(10.5f, 21f); lineTo(10.5f, 16f); quadraticTo(12f, 13f, 13.5f, 16f); lineTo(13.5f, 21f) }, c, style = st)
            }
            RhIcon.SHIELD -> drawPath(p { moveTo(12f, 3f); lineTo(20f, 6f); lineTo(20f, 12f); cubicTo(20f, 17f, 16f, 20f, 12f, 22f); cubicTo(8f, 20f, 4f, 17f, 4f, 12f); lineTo(4f, 6f); close() }, c, style = st)
            RhIcon.COIN -> { drawCircle(c, 9f, Offset(12f, 12f), style = st); drawPath(p { moveTo(12f, 8f); lineTo(16f, 12f); lineTo(12f, 16f); lineTo(8f, 12f); close() }, c, style = st) }
            RhIcon.DICE -> { drawPath(p { moveTo(12f, 2.5f); lineTo(20.5f, 7.5f); lineTo(20.5f, 16.5f); lineTo(12f, 21.5f); lineTo(3.5f, 16.5f); lineTo(3.5f, 7.5f); close() }, c, style = st); drawPath(p { moveTo(12f, 8f); lineTo(16f, 15f); lineTo(8f, 15f); close() }, c, style = st) }
            RhIcon.SWORDS -> { drawLine(c, Offset(5f, 19f), Offset(19f, 5f), 1.9f, StrokeCap.Round); drawLine(c, Offset(19f, 19f), Offset(5f, 5f), 1.9f, StrokeCap.Round); drawLine(c, Offset(4f, 14f), Offset(9f, 19f), 1.9f, StrokeCap.Round); drawLine(c, Offset(20f, 14f), Offset(15f, 19f), 1.9f, StrokeCap.Round) }
            RhIcon.SCROLL -> { drawPath(p { moveTo(7f, 4f); lineTo(19f, 4f); lineTo(19f, 17f); quadraticTo(19f, 20f, 16f, 20f); lineTo(6f, 20f) ; quadraticTo(4f, 20f, 4f, 18f); lineTo(4f, 17f); lineTo(7f, 17f); close() }, c, style = st); drawLine(c, Offset(10f, 9f), Offset(16f, 9f), 1.6f, StrokeCap.Round); drawLine(c, Offset(10f, 13f), Offset(16f, 13f), 1.6f, StrokeCap.Round) }
            RhIcon.USER -> { drawCircle(c, 4f, Offset(12f, 8f), style = st); drawPath(p { moveTo(4f, 21f); cubicTo(4f, 14f, 20f, 14f, 20f, 21f) }, c, style = st) }
            RhIcon.BAG -> { drawPath(p { moveTo(7f, 8f); cubicTo(2f, 14f, 3f, 21f, 12f, 21f); cubicTo(21f, 21f, 22f, 14f, 17f, 8f); close() }, c, style = st); drawLine(c, Offset(8f, 4f), Offset(16f, 4f), 1.9f, StrokeCap.Round); drawLine(c, Offset(8f, 4f), Offset(7f, 8f), 1.9f, StrokeCap.Round); drawLine(c, Offset(16f, 4f), Offset(17f, 8f), 1.9f, StrokeCap.Round) }
            RhIcon.BOOK -> { drawPath(p { moveTo(12f, 6f); quadraticTo(8f, 3.5f, 3f, 4.5f); lineTo(3f, 19f); quadraticTo(8f, 18f, 12f, 20.5f); quadraticTo(16f, 18f, 21f, 19f); lineTo(21f, 4.5f); quadraticTo(16f, 3.5f, 12f, 6f); close() }, c, style = st); drawLine(c, Offset(12f, 6f), Offset(12f, 20.5f), 1.6f) }
            RhIcon.GEAR -> { drawCircle(c, 3.2f, Offset(12f, 12f), style = st); for (i in 0 until 8) { val a = i * Math.PI.toFloat() / 4; drawLine(c, Offset(12f + 7f * kotlin.math.cos(a), 12f + 7f * kotlin.math.sin(a)), Offset(12f + 9.8f * kotlin.math.cos(a), 12f + 9.8f * kotlin.math.sin(a)), 2.6f, StrokeCap.Round) }; drawCircle(c, 6.6f, Offset(12f, 12f), style = st) }
            RhIcon.BACK -> drawPath(p { moveTo(15f, 4f); lineTo(7f, 12f); lineTo(15f, 20f) }, c, style = st)
            RhIcon.PLUS -> { drawLine(c, Offset(12f, 5f), Offset(12f, 19f), 2.2f, StrokeCap.Round); drawLine(c, Offset(5f, 12f), Offset(19f, 12f), 2.2f, StrokeCap.Round) }
            RhIcon.MINUS -> drawLine(c, Offset(5f, 12f), Offset(19f, 12f), 2.2f, StrokeCap.Round)
            RhIcon.CLOSE -> { drawLine(c, Offset(6f, 6f), Offset(18f, 18f), 2.2f, StrokeCap.Round); drawLine(c, Offset(18f, 6f), Offset(6f, 18f), 2.2f, StrokeCap.Round) }
            RhIcon.CHECK -> drawPath(p { moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 7f) }, c, style = Stroke(2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            RhIcon.SEARCH -> { drawCircle(c, 6.5f, Offset(10f, 10f), style = st); drawLine(c, Offset(15f, 15f), Offset(20f, 20f), 2.2f, StrokeCap.Round) }
            RhIcon.HEART -> drawPath(p { moveTo(12f, 20f); cubicTo(2f, 13f, 4f, 4f, 9f, 5f); cubicTo(11f, 5.4f, 12f, 7f, 12f, 7f); cubicTo(12f, 7f, 13f, 5.4f, 15f, 5f); cubicTo(20f, 4f, 22f, 13f, 12f, 20f); close() }, c, style = Fill)
            RhIcon.STAR -> drawPath(p { val pts = com.runehearth.app.art.starPoints(12f, 12.5f, 9.5f, 4f, 5); moveTo(pts[0], pts[1]); var i = 2; while (i < pts.size) { lineTo(pts[i], pts[i + 1]); i += 2 }; close() }, c, style = Fill)
            RhIcon.FLAME -> drawPath(p { moveTo(12f, 2f); cubicTo(17f, 8f, 19f, 11f, 19f, 15f); cubicTo(19f, 19f, 15.5f, 22f, 12f, 22f); cubicTo(8.5f, 22f, 5f, 19f, 5f, 15f); cubicTo(5f, 12f, 7f, 10f, 8.5f, 8f); cubicTo(9.5f, 10f, 10.5f, 10.5f, 11f, 10.5f); cubicTo(10.5f, 7f, 10.5f, 4.5f, 12f, 2f); close() }, c, style = Fill)
            RhIcon.WIFI -> { drawPath(p { moveTo(3f, 9f); quadraticTo(12f, 1f, 21f, 9f) }, c, style = st); drawPath(p { moveTo(6f, 13f); quadraticTo(12f, 7.5f, 18f, 13f) }, c, style = st); drawPath(p { moveTo(9f, 17f); quadraticTo(12f, 14.5f, 15f, 17f) }, c, style = st); drawCircle(c, 1.4f, Offset(12f, 20f)) }
            RhIcon.CROWN -> drawPath(p { moveTo(3f, 19f); lineTo(3f, 8f); lineTo(8f, 13f); lineTo(12f, 5f); lineTo(16f, 13f); lineTo(21f, 8f); lineTo(21f, 19f); close() }, c, style = st)
            RhIcon.SKULL -> { drawPath(p { moveTo(5f, 13f); cubicTo(2f, 4f, 22f, 4f, 19f, 13f); lineTo(16f, 15f); lineTo(16f, 20f); lineTo(8f, 20f); lineTo(8f, 15f); close() }, c, style = st); drawCircle(c, 1.6f, Offset(9f, 11f)); drawCircle(c, 1.6f, Offset(15f, 11f)) }
            RhIcon.TRASH -> { drawPath(p { moveTo(6f, 7f); lineTo(7f, 20f); lineTo(17f, 20f); lineTo(18f, 7f) }, c, style = st); drawLine(c, Offset(4f, 7f), Offset(20f, 7f), 1.9f, StrokeCap.Round); drawLine(c, Offset(9f, 4f), Offset(15f, 4f), 1.9f, StrokeCap.Round) }
            RhIcon.EDIT -> { drawPath(p { moveTo(4f, 20f); lineTo(5f, 15f); lineTo(16f, 4f); lineTo(20f, 8f); lineTo(9f, 19f); close() }, c, style = st) }
            RhIcon.ARROW -> drawPath(p { moveTo(9f, 4f); lineTo(17f, 12f); lineTo(9f, 20f) }, c, style = st)
            RhIcon.LOCK -> { drawPath(p { moveTo(7f, 11f); lineTo(7f, 8f); cubicTo(7f, 2f, 17f, 2f, 17f, 8f); lineTo(17f, 11f) }, c, style = st); drawRect(c, Offset(5f, 11f), androidx.compose.ui.geometry.Size(14f, 10f), style = st) }
            RhIcon.EYE -> { drawPath(p { moveTo(2f, 12f); quadraticTo(12f, 2f, 22f, 12f); quadraticTo(12f, 22f, 2f, 12f); close() }, c, style = st); drawCircle(c, 3f, Offset(12f, 12f), style = st) }
            RhIcon.POTION -> { drawPath(p { moveTo(9f, 3f); lineTo(15f, 3f); lineTo(15f, 9f); lineTo(20f, 19f); quadraticTo(21f, 21f, 19f, 21f); lineTo(5f, 21f); quadraticTo(3f, 21f, 4f, 19f); lineTo(9f, 9f); close() }, c, style = st); drawLine(c, Offset(7f, 15f), Offset(17f, 15f), 1.6f) }
            RhIcon.WAND -> { drawLine(c, Offset(4f, 20f), Offset(15f, 9f), 2.2f, StrokeCap.Round); drawPath(p { val pts = com.runehearth.app.art.starPoints(18f, 6f, 4f, 1.6f, 4); moveTo(pts[0], pts[1]); var i = 2; while (i < pts.size) { lineTo(pts[i], pts[i + 1]); i += 2 }; close() }, c, style = Fill) }
            RhIcon.HELM -> { drawPath(p { moveTo(4f, 20f); lineTo(4f, 11f); cubicTo(4f, 2f, 20f, 2f, 20f, 11f); lineTo(20f, 20f); lineTo(15f, 20f); lineTo(15f, 14f); lineTo(9f, 14f); lineTo(9f, 20f); close() }, c, style = st) }
            RhIcon.GEM -> { drawPath(p { moveTo(7f, 4f); lineTo(17f, 4f); lineTo(21f, 10f); lineTo(12f, 21f); lineTo(3f, 10f); close() }, c, style = st); drawLine(c, Offset(3f, 10f), Offset(21f, 10f), 1.5f) }
            RhIcon.MAP -> { drawPath(p { moveTo(3f, 6f); lineTo(9f, 4f); lineTo(15f, 6f); lineTo(21f, 4f); lineTo(21f, 18f); lineTo(15f, 20f); lineTo(9f, 18f); lineTo(3f, 20f); close() }, c, style = st); drawLine(c, Offset(9f, 4f), Offset(9f, 18f), 1.4f); drawLine(c, Offset(15f, 6f), Offset(15f, 20f), 1.4f) }
            RhIcon.CLOCK -> { drawCircle(c, 9f, Offset(12f, 12f), style = st); drawPath(p { moveTo(12f, 6f); lineTo(12f, 12f); lineTo(16f, 14f) }, c, style = st) }
            RhIcon.SHARE -> { drawCircle(c, 2.4f, Offset(6f, 12f)); drawCircle(c, 2.4f, Offset(18f, 6f)); drawCircle(c, 2.4f, Offset(18f, 18f)); drawLine(c, Offset(6f, 12f), Offset(18f, 6f), 1.6f); drawLine(c, Offset(6f, 12f), Offset(18f, 18f), 1.6f) }
        }
    }
}
