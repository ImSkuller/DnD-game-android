package com.runehearth.app.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.runehearth.app.data.Rarity

/**
 * Theme tokens. Screens only ever read colours/brushes/text styles from [LocalTheme], so adding another theme means
 * implementing a new [ThemeTokens] — no screen needs to change.
 */
@Immutable
interface ThemeTokens {
    val id: String
    val name: String
    val background: Color
    val backgroundBrush: Brush
    val panel: Color
    val panelDark: Color
    val parchment: Color
    val parchmentDark: Color
    val ink: Color
    val inkSoft: Color
    val gold: Color
    val goldDark: Color
    val crimson: Color
    val emerald: Color
    val sapphire: Color
    val steel: Color
    val ember: Color
    val onDark: Color
    val onDarkSoft: Color
    val title: TextStyle
    val heading: TextStyle
    val body: TextStyle
    val small: TextStyle
    val label: TextStyle
    fun rarity(r: Rarity): Color
}

object MedievalTheme : ThemeTokens {
    override val id = "medieval"
    override val name = "Medieval Fantasy"
    override val background = Color(0xFF1B120C)
    override val backgroundBrush = Brush.verticalGradient(listOf(Color(0xFF2A1A10), Color(0xFF1B120C), Color(0xFF120C08)))
    override val panel = Color(0xFF3A2415)
    override val panelDark = Color(0xFF26160D)
    override val parchment = Color(0xFFE9D9AE)
    override val parchmentDark = Color(0xFFCDB682)
    override val ink = Color(0xFF2B1B10)
    override val inkSoft = Color(0xFF5B4330)
    override val gold = Color(0xFFD9A441)
    override val goldDark = Color(0xFF8E6A24)
    override val crimson = Color(0xFF9B2B25)
    override val emerald = Color(0xFF2F7A41)
    override val sapphire = Color(0xFF2A5FA8)
    override val steel = Color(0xFF8A96A3)
    override val ember = Color(0xFFFF9A3C)
    override val onDark = Color(0xFFF1E3C0)
    override val onDarkSoft = Color(0xFFB9A57F)

    private val serif = FontFamily.Serif
    override val title = TextStyle(fontFamily = serif, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = 3.sp, color = Color(0xFFF1E3C0))
    override val heading = TextStyle(fontFamily = serif, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 1.2.sp, color = Color(0xFFF1E3C0))
    override val body = TextStyle(fontFamily = serif, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp, color = Color(0xFFF1E3C0))
    override val small = TextStyle(fontFamily = serif, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 16.sp, color = Color(0xFFB9A57F))
    override val label = TextStyle(fontFamily = serif, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.6.sp, color = Color(0xFFD9A441))

    override fun rarity(r: Rarity): Color = when (r) {
        Rarity.COMMON -> Color(0xFFB8AD94)
        Rarity.UNCOMMON -> Color(0xFF5CB85C)
        Rarity.RARE -> Color(0xFF4A90E2)
        Rarity.EPIC -> Color(0xFFA855F7)
        Rarity.LEGENDARY -> Color(0xFFF59E0B)
        Rarity.MYTHIC -> Color(0xFFEF4444)
    }
}

val LocalTheme = staticCompositionLocalOf<ThemeTokens> { MedievalTheme }

/** Shared 0..1 looping phase for glow shimmer; read inside draw lambdas so only glowing items redraw. */
val LocalGlowPhase = compositionLocalOf<State<Float>> { error("no glow phase") }

val Theme: ThemeTokens @Composable get() = LocalTheme.current

@Composable
fun RunehearthTheme(tokens: ThemeTokens = MedievalTheme, content: @Composable () -> Unit) {
    val perf = LocalPerf.current
    val phase: State<Float> = if (perf.animatedGlow) {
        val transition = rememberInfiniteTransition(label = "glow")
        transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "glowPhase")
    } else remember { mutableFloatStateOf(0.3f) }
    val scheme = darkColorScheme(
        primary = tokens.gold, onPrimary = tokens.ink, secondary = tokens.emerald, background = tokens.background, onBackground = tokens.onDark,
        surface = tokens.panel, onSurface = tokens.onDark, error = tokens.crimson,
    )
    CompositionLocalProvider(LocalTheme provides tokens, LocalGlowPhase provides phase) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
