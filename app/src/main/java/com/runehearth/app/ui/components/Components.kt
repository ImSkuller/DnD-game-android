package com.runehearth.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.runehearth.app.art.AssetRef
import com.runehearth.app.art.drawAsset
import com.runehearth.app.ui.theme.LocalGlowPhase
import com.runehearth.app.ui.theme.Textures
import com.runehearth.app.ui.theme.Theme
import kotlinx.coroutines.delay
import kotlin.math.sin

enum class Surface { PARCHMENT, WOOD, STONE, LEATHER }

/** Default text colour for the surface a composable currently sits on (ink on parchment, light on wood/stone). */
val LocalInk = compositionLocalOf { Color(0xFFF1E3C0) }

@Composable
fun RhText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = Theme.body,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    val c = if (color != Color.Unspecified) color else if (style == Theme.small) LocalInk.current.copy(alpha = 0.72f) else LocalInk.current
    androidx.compose.material3.Text(text, modifier, color = c, style = style, maxLines = maxLines, textAlign = textAlign, overflow = overflow)
}

/** Press feedback: scale-down spring, no ripple. */
fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.96f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "press")
    graphicsLayer { scaleX = s; scaleY = s }
        .clickable(src, null, enabled = enabled, onClick = onClick)
}

private fun surfaceBrush(s: Surface): Brush = when (s) {
    Surface.PARCHMENT -> Textures.parchment
    Surface.WOOD -> Textures.wood
    Surface.STONE -> Textures.stone
    Surface.LEATHER -> Textures.leather
}

/** Textured panel with a forged-gold double border and corner studs. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    surface: Surface = Surface.WOOD,
    padding: Dp = 12.dp,
    onClick: (() -> Unit)? = null,
    glow: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val t = Theme
    val ink = if (surface == Surface.PARCHMENT) t.ink else t.onDark
    val shape = RoundedCornerShape(10.dp)
    val flat = com.runehearth.app.ui.theme.LocalPerf.current.flatPanels
    val flatColor = when (surface) { Surface.PARCHMENT -> t.parchment; Surface.WOOD -> t.panel; Surface.STONE -> Color(0xFF4A4642); Surface.LEATHER -> Color(0xFF5A3920) }
    val base = modifier
        .let { if (onClick != null) it.pressable(onClick = onClick) else it }
        .clip(shape)
        .drawBehind {
            val r = CornerRadius(10.dp.toPx())
            if (flat) drawRoundRect(flatColor, cornerRadius = r) else {
                drawRoundRect(surfaceBrush(surface), cornerRadius = r)
                // soft vignette to give depth
                drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.07f), Color.Transparent, Color.Black.copy(alpha = 0.28f))), cornerRadius = r)
            }
            val outer = Stroke(2.2.dp.toPx())
            drawRoundRect(glow ?: t.goldDark, cornerRadius = r, style = outer)
            val inset = 4.dp.toPx()
            drawRoundRect((glow ?: t.gold).copy(alpha = 0.55f), Offset(inset, inset), androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2), CornerRadius(7.dp.toPx()), style = Stroke(0.8.dp.toPx()))
            val d = 3.2.dp.toPx()
            for (p in listOf(Offset(inset, inset), Offset(size.width - inset, inset), Offset(inset, size.height - inset), Offset(size.width - inset, size.height - inset))) {
                drawCircle(t.gold, d, p); drawCircle(t.goldDark, d * 0.45f, p)
            }
        }
        .padding(padding + 3.dp)
    CompositionLocalProvider(LocalInk provides ink) { Column(base, content = content) }
}

enum class BtnKind { PRIMARY, SECONDARY, DANGER, GOLD }

@Composable
fun WoodButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: RhIcon? = null,
    kind: BtnKind = BtnKind.PRIMARY,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    val t = Theme
    val (top, bottom, fg) = when (kind) {
        BtnKind.PRIMARY -> Triple(Color(0xFF6B4527), Color(0xFF3F2713), t.onDark)
        BtnKind.SECONDARY -> Triple(Color(0xFF4A4642), Color(0xFF2B2927), t.onDark)
        BtnKind.DANGER -> Triple(Color(0xFFA33A32), Color(0xFF5E1D18), Color(0xFFFFE9DD))
        BtnKind.GOLD -> Triple(Color(0xFFE3B24A), Color(0xFF9A6F1E), t.ink)
    }
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier
            .alpha(if (enabled) 1f else 0.45f)
            .pressable(enabled, onClick)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .border(1.6.dp, t.gold.copy(alpha = 0.85f), shape)
            .padding(horizontal = if (compact) 12.dp else 18.dp, vertical = if (compact) 7.dp else 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { RhIconView(icon, fg, if (compact) 16.dp else 20.dp); Spacer(Modifier.width(8.dp)) }
        RhText(text.uppercase(), style = Theme.label.copy(fontSize = if (compact) 11.sp else 13.sp, color = fg), color = fg, maxLines = 1)
    }
}

@Composable
fun RhIconButton(icon: RhIcon, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = Theme.gold, size: Dp = 24.dp) {
    Box(modifier.size(size + 20.dp).pressable(onClick = onClick), contentAlignment = Alignment.Center) { RhIconView(icon, tint, size) }
}

@Composable
fun OrnateDivider(modifier: Modifier = Modifier, color: Color = Theme.gold) {
    Canvas(modifier.fillMaxWidth().height(12.dp)) {
        val y = size.height / 2
        val gap = 14.dp.toPx()
        val cx = size.width / 2
        drawLine(Brush.horizontalGradient(listOf(Color.Transparent, color.copy(alpha = 0.7f))), Offset(0f, y), Offset(cx - gap, y), 1.2.dp.toPx())
        drawLine(Brush.horizontalGradient(listOf(color.copy(alpha = 0.7f), Color.Transparent)), Offset(cx + gap, y), Offset(size.width, y), 1.2.dp.toPx())
        val d = 4.5.dp.toPx()
        val p = androidx.compose.ui.graphics.Path().apply { moveTo(cx, y - d); lineTo(cx + d, y); lineTo(cx, y + d); lineTo(cx - d, y); close() }
        drawPath(p, color)
        drawCircle(color, 1.8.dp.toPx(), Offset(cx - gap * 0.6f, y)); drawCircle(color, 1.8.dp.toPx(), Offset(cx + gap * 0.6f, y))
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: (@Composable RowScope.() -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(top = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RhText(title.uppercase(), Modifier.weight(1f), style = Theme.label.copy(fontSize = 13.sp), color = Theme.gold)
            trailing?.invoke(this)
        }
        OrnateDivider()
    }
}

@Composable
fun StatBar(label: String, value: Int, max: Int, color: Color, modifier: Modifier = Modifier, showNumbers: Boolean = true) {
    val frac by animateFloatAsState(if (max <= 0) 0f else (value.toFloat() / max).coerceIn(0f, 1f), tween(450, easing = FastOutSlowInEasing), label = "bar")
    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            RhText(label.uppercase(), Modifier.weight(1f), style = Theme.label.copy(fontSize = 11.sp), color = color)
            if (showNumbers) RhText("$value / $max", style = Theme.small)
        }
        Spacer(Modifier.height(3.dp))
        Box(Modifier.fillMaxWidth().height(11.dp).clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.45f)).border(1.dp, Theme.goldDark.copy(alpha = 0.8f), RoundedCornerShape(6.dp))) {
            Box(Modifier.fillMaxWidth(frac).fillMaxSize().background(Brush.verticalGradient(listOf(lerp(color, Color.White, 0.35f), color, lerp(color, Color.Black, 0.35f)))))
        }
    }
}

/** Renders a procedural asset. Glow items read the shared shimmer phase in the draw phase only. */
@Composable
fun ItemArt(ref: AssetRef?, size: Dp, modifier: Modifier = Modifier) {
    val phase = LocalGlowPhase.current
    if (ref == null) {
        Box(modifier.size(size), contentAlignment = Alignment.Center) { RhIconView(RhIcon.BAG, Theme.goldDark.copy(alpha = 0.6f), size * 0.5f) }
    } else {
        Canvas(modifier.size(size)) { drawAsset(ref, if (ref.glow) phase.value else 0f) }
    }
}

/** Item icon in a dark slot framed with the rarity colour. */
@Composable
fun ArtSlot(ref: AssetRef?, size: Dp, rarity: Color = Theme.goldDark, modifier: Modifier = Modifier, selected: Boolean = false) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier.size(size).clip(shape).background(Brush.radialGradient(listOf(Color(0xFF3A2A1E), Color(0xFF150E09)))).border(if (selected) 2.5.dp else 1.6.dp, if (selected) Theme.gold else rarity, shape),
        contentAlignment = Alignment.Center,
    ) { ItemArt(ref, size - 8.dp) }
}

@Composable
fun RhTextField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboard: KeyboardType = KeyboardType.Text,
    onDark: Boolean = LocalInk.current != Theme.ink,
) {
    val t = Theme
    val ink = LocalInk.current
    Column(modifier) {
        if (label != null) { RhText(label.uppercase(), style = t.label.copy(fontSize = 10.5.sp), color = if (onDark) t.gold else t.goldDark); Spacer(Modifier.height(3.dp)) }
        val kb = LocalSoftwareKeyboardController.current
        BasicTextField(
            value, onChange,
            Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = if (LocalInk.current == t.ink) 0.10f else 0.35f))
                .border(1.dp, t.goldDark.copy(alpha = 0.9f), RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 9.dp),
            textStyle = t.body.copy(color = ink), singleLine = singleLine, minLines = minLines,
            cursorBrush = SolidColor(t.gold), keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        )
        kb?.hashCode()
    }
}

/** Integer-only text field helper. */
@Composable
fun RhNumberField(value: Long, onChange: (Long) -> Unit, modifier: Modifier = Modifier, label: String? = null, max: Long = 9_999_999_999L) {
    var text by remember(value) { mutableStateOf(if (value == 0L) "" else value.toString()) }
    RhTextField(text, { s ->
        val digits = s.filter { it.isDigit() }.take(12)
        text = digits
        onChange((digits.toLongOrNull() ?: 0L).coerceAtMost(max))
    }, modifier, label, keyboard = KeyboardType.Number)
}

@Composable
fun Stepper(value: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier, min: Int = 0, max: Int = 9999, step: Int = 1, label: String? = null) {
    Column(modifier) {
        if (label != null) RhText(label.uppercase(), style = Theme.label.copy(fontSize = 10.5.sp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RhIconButton(RhIcon.MINUS, { onChange((value - step).coerceAtLeast(min)) }, size = 18.dp)
            RhText(value.toString(), Modifier.width(52.dp), style = Theme.heading.copy(fontSize = 18.sp), textAlign = TextAlign.Center)
            RhIconButton(RhIcon.PLUS, { onChange((value + step).coerceAtMost(max)) }, size = 18.dp)
        }
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Theme.gold) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier.pressable(onClick = onClick).clip(shape)
            .background(if (selected) color.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.3f))
            .border(1.2.dp, color.copy(alpha = if (selected) 1f else 0.55f), shape).padding(horizontal = 12.dp, vertical = 6.dp),
    ) { RhText(text, style = Theme.small.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = if (selected) Theme.ink else Theme.onDark, maxLines = 1) }
}

@Composable
fun Tag(text: String, color: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(4.dp)
    Box(modifier.clip(shape).background(color.copy(alpha = 0.22f)).border(1.dp, color, shape).padding(horizontal = 6.dp, vertical = 1.dp)) {
        RhText(text.uppercase(), style = Theme.label.copy(fontSize = 9.5.sp, letterSpacing = 1.sp), color = color, maxLines = 1)
    }
}

@Composable
fun RhDialog(
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    surface: Surface = Surface.PARCHMENT,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { shown = true }
        val s by animateFloatAsState(if (shown) 1f else 0.88f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "dlg")
        val a by animateFloatAsState(if (shown) 1f else 0f, tween(160), label = "dlgA")
        Box(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp), contentAlignment = Alignment.Center) {
            Panel(modifier.fillMaxWidth().graphicsLayer { scaleX = s; scaleY = s; alpha = a }, surface = surface, padding = 14.dp) {
                RhText(title, style = Theme.heading, color = if (surface == Surface.PARCHMENT) Theme.ink else Theme.gold)
                OrnateDivider(color = if (surface == Surface.PARCHMENT) Theme.goldDark else Theme.gold)
                Spacer(Modifier.height(6.dp))
                Column(Modifier.weight(1f, fill = false), content = content)
                if (actions != null) { Spacer(Modifier.height(12.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), content = actions) }
            }
        }
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit, danger: Boolean = true) {
    RhDialog(onDismiss, title, actions = {
        WoodButton("Cancel", onDismiss, kind = BtnKind.SECONDARY, compact = true)
        WoodButton(confirm, { onConfirm(); onDismiss() }, kind = if (danger) BtnKind.DANGER else BtnKind.PRIMARY, compact = true)
    }) { RhText(text) }
}

@Composable
fun EmptyState(icon: RhIcon, title: String, text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RhIconView(icon, Theme.goldDark, 52.dp)
        RhText(title, style = Theme.heading, textAlign = TextAlign.Center, color = Theme.onDark)
        RhText(text, style = Theme.small, textAlign = TextAlign.Center, color = Theme.onDarkSoft)
        action?.invoke()
    }
}

@Composable
fun LoadingRune(modifier: Modifier = Modifier, size: Dp = 48.dp) {
    val t = rememberInfiniteTransition(label = "load")
    val rot by t.animateFloat(0f, 360f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "rot")
    val pulse by t.animateFloat(0.5f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse")
    val gold = Theme.gold
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size).rotate(rot)) {
            drawCircle(gold.copy(alpha = pulse), this.size.minDimension / 2 - 3.dp.toPx(), style = Stroke(2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 12f))))
        }
        RhIconView(RhIcon.FLAME, Theme.ember.copy(alpha = pulse), size * 0.42f)
    }
}

/** Dark wooden backdrop. */
@Composable
fun Backdrop(modifier: Modifier = Modifier, embers: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(Theme.backgroundBrush)) {
        if (embers) Embers(Modifier.fillMaxSize())
        content()
    }
}

/** Drifting ember particles; positions are a pure function of time so nothing is allocated per frame. */
@Composable
fun Embers(modifier: Modifier = Modifier, count: Int = 26) {
    var time by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { while (true) androidx.compose.runtime.withFrameNanos { time = it / 1_000_000L } }
    val ember = Theme.ember
    Canvas(modifier) {
        val t = time / 1000f
        for (i in 0 until count) {
            val seed = i * 7.31f
            val speed = 0.035f + (i % 5) * 0.012f
            val prog = (t * speed + (i * 0.173f)) % 1f
            val x = size.width * ((i * 0.6180339f) % 1f) + sin(t * 0.6f + seed) * 18.dp.toPx()
            val y = size.height * (1f - prog)
            val fade = (1f - kotlin.math.abs(prog - 0.5f) * 2f).coerceIn(0f, 1f)
            val r = (1.2f + (i % 4) * 0.7f).dp.toPx()
            drawCircle(ember.copy(alpha = 0.55f * fade), r * 2.6f, Offset(x, y), alpha = 0.25f)
            drawCircle(Color(0xFFFFD27A).copy(alpha = 0.9f * fade), r, Offset(x, y))
        }
    }
}

@Stable
class Toaster {
    var message by mutableStateOf<String?>(null)
    var isError by mutableStateOf(false)
    fun show(text: String, error: Boolean = false) { isError = error; message = text }
}

val LocalToaster = compositionLocalOf { Toaster() }

@Composable
fun ToastHost(toaster: Toaster, modifier: Modifier = Modifier) {
    val msg = toaster.message
    LaunchedEffect(msg) { if (msg != null) { delay(2600); if (toaster.message == msg) toaster.message = null } }
    Box(modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(msg != null, enter = slideInVertically { -it } + fadeIn(), exit = slideOutVertically { -it } + fadeOut()) {
            val c = if (toaster.isError) Theme.crimson else Theme.emerald
            Row(
                Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(10.dp)).background(Theme.panelDark).border(1.6.dp, c, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RhIconView(if (toaster.isError) RhIcon.CLOSE else RhIcon.CHECK, c, 18.dp)
                RhText(msg ?: "", style = Theme.body, color = Theme.onDark)
            }
        }
    }
}

data class TabSpec(val label: String, val icon: RhIcon)

/** Forged wooden bottom bar with a gold selection rune. */
@Composable
fun RuneTabBar(tabs: List<TabSpec>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val t = Theme
    Row(
        modifier.fillMaxWidth().drawBehind {
            drawRect(Textures.wood)
            drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.1f), Color.Black.copy(alpha = 0.45f))))
            drawLine(t.gold, Offset(0f, 0f), Offset(size.width, 0f), 2.dp.toPx())
            drawLine(t.goldDark, Offset(0f, 4.dp.toPx()), Offset(size.width, 4.dp.toPx()), 0.8.dp.toPx())
        }.padding(top = 6.dp, bottom = 4.dp),
    ) {
        tabs.forEachIndexed { i, tab ->
            val sel = i == selected
            val lift by animateFloatAsState(if (sel) -3f else 0f, spring(Spring.DampingRatioMediumBouncy), label = "lift")
            Column(Modifier.weight(1f).pressable { onSelect(i) }.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.graphicsLayer { translationY = lift.dp.toPx() }, contentAlignment = Alignment.Center) {
                    if (sel) Canvas(Modifier.size(38.dp)) { drawCircle(Brush.radialGradient(listOf(t.gold.copy(alpha = 0.45f), Color.Transparent)), size.minDimension / 2) }
                    RhIconView(tab.icon, if (sel) t.gold else t.onDarkSoft, 24.dp)
                }
                RhText(tab.label.uppercase(), style = t.label.copy(fontSize = 9.sp, letterSpacing = 0.8.sp), color = if (sel) t.gold else t.onDarkSoft, maxLines = 1)
            }
        }
    }
}

/** Standard screen frame: back button, carved title plaque, optional actions. */
@Composable
fun ScreenTopBar(title: String, onBack: (() -> Unit)?, modifier: Modifier = Modifier, actions: (@Composable RowScope.() -> Unit)? = null) {
    val t = Theme
    Row(
        modifier.fillMaxWidth().drawBehind {
            drawRect(Textures.wood)
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))))
            drawLine(t.gold, Offset(0f, size.height), Offset(size.width, size.height), 2.dp.toPx())
        }.padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) RhIconButton(RhIcon.BACK, onBack) else Spacer(Modifier.width(12.dp))
        RhText(title, Modifier.weight(1f), style = t.heading, color = t.onDark, maxLines = 1)
        actions?.invoke(this)
    }
}

@Composable
fun KeyValue(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RhText(label, Modifier.weight(1f), style = Theme.small)
        RhText(value, style = Theme.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
    }
}

fun contentPadding(h: Dp = 14.dp, v: Dp = 10.dp) = PaddingValues(horizontal = h, vertical = v)

/** Section hidden behind a header until tapped: keeps advanced options out of the way. */
@Composable
fun Collapsible(title: String, modifier: Modifier = Modifier, startOpen: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    var open by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(startOpen) }
    val rot by animateFloatAsState(if (open) 90f else 0f, tween(180), label = "chev")
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().pressable { open = !open }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            RhText(title.uppercase(), Modifier.weight(1f), style = Theme.label.copy(fontSize = 13.sp), color = Theme.gold)
            Box(Modifier.rotate(rot)) { RhIconView(RhIcon.ARROW, Theme.gold, 18.dp) }
        }
        OrnateDivider()
        androidx.compose.animation.AnimatedVisibility(open) { Column(Modifier.fillMaxWidth().padding(top = 6.dp), content = content) }
    }
}
