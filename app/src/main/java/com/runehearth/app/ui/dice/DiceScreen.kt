package com.runehearth.app.ui.dice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runehearth.app.core.Coin
import com.runehearth.app.core.RollMode
import com.runehearth.app.core.RollSpec
import com.runehearth.app.core.RollTier
import com.runehearth.app.core.Crit
import com.runehearth.app.data.RollRecord
import com.runehearth.app.engine.Action
import com.runehearth.app.session.newActionId
import com.runehearth.app.ui.LocalSession
import com.runehearth.app.ui.components.BtnKind
import com.runehearth.app.ui.components.Chip
import com.runehearth.app.ui.components.CoinImage
import com.runehearth.app.ui.components.EmptyState
import com.runehearth.app.ui.components.Panel
import com.runehearth.app.ui.components.RhIcon
import com.runehearth.app.ui.components.RhText
import com.runehearth.app.ui.components.RhTextField
import com.runehearth.app.ui.components.SectionHeader
import com.runehearth.app.ui.components.Stepper
import com.runehearth.app.ui.components.WoodButton
import com.runehearth.app.ui.components.pressable
import com.runehearth.app.ui.rememberDao
import com.runehearth.app.ui.rememberDispatch
import com.runehearth.app.ui.rememberFlow
import com.runehearth.app.ui.sheet.RollLine
import com.runehearth.app.ui.theme.Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

fun tierColor(t: RollTier): Color = when (t) {
    RollTier.LOW -> Color(0xFFD9453B); RollTier.LOW_MEDIUM -> Color(0xFFE48A3C); RollTier.MEDIUM -> Color(0xFFD9C14B)
    RollTier.HIGH_MEDIUM -> Color(0xFF8CCB5A); RollTier.HIGH -> Color(0xFF4CC9F0)
}

/** Dice roller. DM rolls can be private; players' rolls are executed by the host so they cannot be forged. */
@Composable
fun DiceScreen(campaignId: String, modifier: Modifier = Modifier, showHistoryOfAll: Boolean = true) {
    val session = LocalSession.current
    val dao = rememberDao()
    val rolls by rememberFlow(campaignId, emptyList()) { dao.observeRolls(campaignId, 60) }
    var count by rememberSaveable { mutableIntStateOf(1) }
    var sides by rememberSaveable { mutableIntStateOf(20) }
    var modifierV by rememberSaveable { mutableIntStateOf(0) }
    var mode by rememberSaveable { mutableStateOf(RollMode.NORMAL.name) }
    var label by rememberSaveable { mutableStateOf("") }
    var isPublic by rememberSaveable { mutableStateOf(true) }
    var shown by remember { mutableStateOf<RollRecord?>(null) }
    var rolling by remember { mutableStateOf(false) }
    var pendingId by remember { mutableStateOf<String?>(null) }
    val dispatch = rememberDispatch { r ->
        r.rollId?.let { id -> pendingId = id }
        if (!r.ok) rolling = false
    }
    LaunchedEffect(pendingId) {
        if (pendingId != null) {
            // the record arrives through the DB (immediately on the host, via delta on a player's device)
            var rec: RollRecord? = null
            repeat(40) { if (rec == null) { rec = dao.roll(pendingId!!); if (rec == null) delay(50) } }
            shown = rec; rolling = false; pendingId = null
        }
    }
    fun roll(spec: RollSpec) {
        rolling = true
        dispatch(Action.RollDice(newActionId(), spec, label.trim(), isPublic))
    }

    Column(modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                AnimatedVisibility(shown != null || rolling, enter = fadeIn() + scaleIn(initialScale = 0.9f), exit = fadeOut()) {
                    RollStage(shown, rolling)
                }
            }
            item {
                Panel(Modifier.fillMaxWidth()) {
                    SectionHeader("Quick Rolls")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            Column(Modifier.pressable { roll(RollSpec.COIN) }.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) { CoinImage(Coin.GOLD, 46.dp); RhText("Coin", style = Theme.small) }
                        }
                        items(listOf(6, 10, 20, 50, 100)) { s ->
                            Column(Modifier.pressable { roll(RollSpec(1, s, 0, RollMode.NORMAL)) }.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                DieFace(s, s.toString(), 46)
                                RhText("d$s", style = Theme.small)
                            }
                        }
                    }
                }
            }
            item {
                Panel(Modifier.fillMaxWidth()) {
                    SectionHeader("Custom Roll")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Stepper(count, { count = it }, min = 1, max = RollSpec.MAX_COUNT, label = "Dice")
                        Stepper(sides, { sides = it }, min = 2, max = RollSpec.MAX_SIDES, step = if (sides >= 20) 10 else 1, label = "Sides")
                        Stepper(modifierV, { modifierV = it }, min = -99, max = 99, label = "Modifier")
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Chip("Normal", mode == RollMode.NORMAL.name, { mode = RollMode.NORMAL.name })
                        Chip("Advantage", mode == RollMode.ADVANTAGE.name, { mode = RollMode.ADVANTAGE.name }, color = Color(0xFF5CB85C))
                        Chip("Disadvantage", mode == RollMode.DISADVANTAGE.name, { mode = RollMode.DISADVANTAGE.name }, color = Color(0xFFD9453B))
                    }
                    if (mode != RollMode.NORMAL.name && count > 1) RhText("Advantage/disadvantage applies to a single die.", style = Theme.small)
                    Spacer(Modifier.height(6.dp))
                    RhTextField(label, { label = it.take(40) }, label = "Label (optional)", modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Chip("Public", isPublic, { isPublic = true }); Chip("Private", !isPublic, { isPublic = false })
                        Spacer(Modifier.weight(1f))
                        WoodButton("Roll ${RollSpec(count, sides, modifierV, RollMode.valueOf(mode)).expression()}", { roll(RollSpec(count, sides, modifierV, RollMode.valueOf(mode))) }, kind = BtnKind.GOLD, icon = RhIcon.DICE, compact = true)
                    }
                }
            }
            item { SectionHeader("Roll History") }
            val list = if (showHistoryOfAll) rolls else rolls.filter { it.actor == session.characterId }
            if (list.isEmpty()) item { EmptyState(RhIcon.DICE, "No rolls yet", "Cast the dice and fate will be recorded here.") }
            items(list.take(40), key = { it.id }) { r ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.18f)).pressable { shown = r }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Column(Modifier.weight(1f)) {
                        RollLine(r)
                        RhText("${RollTier.valueOf(r.tier).label}${if (r.crit != "NONE") " · Critical ${if (r.crit == "SUCCESS") "Success" else "Failure"}" else ""}${if (!r.isPublic) " · private" else ""}${if (r.actor != "DM") " · player" else ""}", style = Theme.small, color = tierColor(RollTier.valueOf(r.tier)))
                    }
                }
            }
        }
    }
}

/** A polygonal die with its number. Shape depends on the number of sides. */
@Composable
fun DieFace(sides: Int, text: String, size: Int, modifier: Modifier = Modifier, tint: Color = Theme.gold, rotation: Float = 0f) {
    Box(modifier.size(size.dp).graphicsLayer { rotationZ = rotation }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size.dp)) {
            val c = Offset(this.size.width / 2, this.size.height / 2)
            val r = this.size.minDimension / 2 - 2.dp.toPx()
            val n = when (sides) { 6 -> 4; 10 -> 6; 20 -> 6; 50 -> 8; 100 -> 10; 4 -> 3; else -> 8 }
            val rot = if (n == 4) 45f else -90f
            val outer = Path().apply {
                for (i in 0 until n) { val a = Math.toRadians((rot + i * 360.0 / n)); val x = c.x + r * Math.cos(a).toFloat(); val y = c.y + r * Math.sin(a).toFloat(); if (i == 0) moveTo(x, y) else lineTo(x, y) }
                close()
            }
            drawPath(outer, androidx.compose.ui.graphics.Brush.linearGradient(listOf(tint.copy(alpha = 0.95f), Color(0xFF3A2410)), Offset(0f, 0f), Offset(this.size.width, this.size.height)))
            drawPath(outer, Color(0xFF1E120C), style = Stroke(2.dp.toPx()))
            // facet lines
            for (i in 0 until n step if (n > 6) 2 else 1) { val a = Math.toRadians((rot + i * 360.0 / n)); drawLine(Color.Black.copy(alpha = 0.25f), c, Offset(c.x + r * Math.cos(a).toFloat(), c.y + r * Math.sin(a).toFloat()), 1.dp.toPx()) }
        }
        RhText(text, style = Theme.heading.copy(fontSize = (size / 3).coerceAtMost(30).sp), color = Color(0xFF1E120C), textAlign = TextAlign.Center, maxLines = 1)
    }
}

/** The big animated stage: tumbling dice → settle → total, classification and flavour text. */
@Composable
private fun RollStage(record: RollRecord?, rolling: Boolean) {
    val spec = record?.expr ?: ""
    val tier = record?.let { RollTier.valueOf(it.tier) }
    val crit = record?.let { Crit.valueOf(it.crit) } ?: Crit.NONE
    val dice = record?.dice?.split(',')?.mapNotNull { it.toIntOrNull() } ?: emptyList()
    val isCoin = spec == "Coin toss"
    val glow = tier?.let { tierColor(it) } ?: Theme.gold
    var tumbling by remember(record?.id) { mutableStateOf(true) }
    var display by remember(record?.id) { mutableIntStateOf(Random.nextInt(1, 21)) }
    val spin = remember(record?.id) { Animatable(0f) }
    val pop = remember(record?.id) { Animatable(0.6f) }
    val shake = remember(record?.id) { Animatable(0f) }
    LaunchedEffect(record?.id, rolling) {
        if (record == null) return@LaunchedEffect
        tumbling = true
        val maxFace = if (isCoin) 2 else (record.expr.substringAfter('d', "20").takeWhile { it.isDigit() }.toIntOrNull() ?: 20)
        spin.snapTo(0f)
        kotlinx.coroutines.coroutineScope {
            launch { spin.animateTo(if (isCoin) 1800f else 720f, tween(950, easing = FastOutSlowInEasing)) }
            val end = System.currentTimeMillis() + 900
            while (System.currentTimeMillis() < end) { display = Random.nextInt(1, maxFace + 1); delay(55) }
        }
        tumbling = false
        pop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
        if (crit == Crit.FAILURE) { repeat(4) { shake.animateTo(10f, tween(40)); shake.animateTo(-10f, tween(40)) }; shake.animateTo(0f, tween(40)) }
    }
    Panel(Modifier.fillMaxWidth(), glow = glow, surface = com.runehearth.app.ui.components.Surface.STONE) {
        Column(Modifier.fillMaxWidth().graphicsLayer { translationX = shake.value }, horizontalAlignment = Alignment.CenterHorizontally) {
            if (record == null) {
                RhText("The dice tumble…", style = Theme.heading, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp)); DieFace(20, "?", 80, rotation = spin.value)
            } else {
                RhText(spec + if (record.label.isNotBlank()) " · ${record.label}" else "", style = Theme.small, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                if (isCoin) {
                    val heads = dice.firstOrNull() == 1
                    Box(Modifier.size(96.dp).graphicsLayer { rotationX = spin.value; cameraDistance = 24f * density }, contentAlignment = Alignment.Center) {
                        CoinImage(if (tumbling) Coin.GOLD else if (heads) Coin.GOLD else Coin.SILVER, 96.dp)
                    }
                    if (!tumbling) { Spacer(Modifier.height(6.dp)); RhText(if (heads) "HEADS" else "TAILS", style = Theme.title.copy(fontSize = 26.sp), color = Theme.gold) }
                } else {
                    val keptIdx = when { spec.contains("(adv)") -> dice.indices.maxByOrNull { dice[it] }; spec.contains("(dis)") -> dice.indices.minByOrNull { dice[it] }; else -> null }
                    val sidesN = spec.substringAfter('d').takeWhile { it.isDigit() }.toIntOrNull() ?: 20
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val shownDice = if (tumbling) listOf(display) + List((dice.size - 1).coerceIn(0, 5)) { Random.nextInt(1, sidesN + 1) } else dice.take(12)
                        shownDice.forEachIndexed { i, d ->
                            val dim = !tumbling && keptIdx != null && i != keptIdx
                            DieFace(sidesN, d.toString(), if (shownDice.size > 3) 52 else 72, Modifier.graphicsLayer { alpha = if (dim) 0.35f else 1f; if (!tumbling) { scaleX = pop.value; scaleY = pop.value } }, rotation = if (tumbling) spin.value + i * 40 else 0f)
                        }
                    }
                    if (dice.size > 12) RhText("+${dice.size - 12} more dice", style = Theme.small)
                    if (!tumbling) {
                        Spacer(Modifier.height(6.dp))
                        if (record.modifier != 0) RhText("Modifier ${if (record.modifier > 0) "+" else ""}${record.modifier}", style = Theme.small)
                        RhText(record.total.toString(), style = Theme.title.copy(fontSize = 52.sp), color = glow, modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value })
                    }
                }
                if (!tumbling && tier != null) {
                    RhText(tier.label.uppercase(), style = Theme.heading.copy(fontSize = 20.sp, letterSpacing = 2.sp), color = glow)
                    RhText(tier.flavor, style = Theme.small, textAlign = TextAlign.Center)
                    if (crit == Crit.SUCCESS) RhText("★ CRITICAL SUCCESS ★", style = Theme.heading, color = Color(0xFFFFD54A), modifier = Modifier.padding(top = 4.dp))
                    if (crit == Crit.FAILURE) RhText("☠ CRITICAL FAILURE ☠", style = Theme.heading, color = Color(0xFFE5534B), modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

