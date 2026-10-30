package com.runehearth.app.ui.assets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runehearth.app.art.ArtCategory
import com.runehearth.app.art.AssetCatalog
import com.runehearth.app.art.AssetRef
import com.runehearth.app.art.DesignNames
import com.runehearth.app.art.Palette
import com.runehearth.app.ui.components.BtnKind
import com.runehearth.app.ui.components.Chip
import com.runehearth.app.ui.components.ItemArt
import com.runehearth.app.ui.components.RhText
import com.runehearth.app.ui.components.RhTextField
import com.runehearth.app.ui.components.SectionHeader
import com.runehearth.app.ui.components.WoodButton
import com.runehearth.app.ui.components.pressable
import com.runehearth.app.ui.theme.Theme

/**
 * Searchable asset browser + customiser: pick a design, a colour (20) and glow on/off with a live preview.
 * Used as the item/skill/character image picker (with [onPick]) and as the Asset Library (browse-only).
 */
@Composable
fun AssetPickerPanel(
    initial: AssetRef?,
    categories: List<ArtCategory>,
    modifier: Modifier = Modifier,
    onPick: ((AssetRef) -> Unit)? = null,
) {
    var cat by rememberSaveable { mutableStateOf((initial?.category ?: categories.first()).name) }
    val category = ArtCategory.valueOf(cat)
    var design by rememberSaveable { mutableIntStateOf(if (initial?.category == category) initial.design else 0) }
    var color by rememberSaveable { mutableIntStateOf(initial?.color ?: 0) }
    var glow by rememberSaveable { mutableStateOf(initial?.glow ?: false) }
    var query by rememberSaveable { mutableStateOf("") }
    val names = remember(category) { DesignNames.of(category) }
    val visible = remember(category, query) {
        val q = query.trim().lowercase()
        (0 until category.designs).filter { q.isEmpty() || names[it].lowercase().contains(q) || Palette.of(it % 20).name.lowercase() == q || "#${it + 1}" == q }
    }
    val ref = AssetRef(category, design.coerceIn(0, category.designs - 1), color, glow)

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(104.dp).clip(RoundedCornerShape(12.dp)).background(Brush.radialGradient(listOf(Color(0xFF45301F), Color(0xFF140D08))))
                    .border(2.dp, Theme.gold, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { ItemArt(ref, 92.dp) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RhText(names[ref.design], style = Theme.heading.copy(fontSize = 17.sp), maxLines = 2)
                RhText("${category.label} · ${Palette.of(color).name}${if (glow) " · Glowing" else ""}", style = Theme.small)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlowSwitch(glow) { glow = it }
                    RhText("Enchant glow", style = Theme.small)
                }
            }
        }
        // colour row
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)) {
            items(Palette.colors.size) { i ->
                val sel = i == color
                Box(
                    Modifier.size(if (sel) 34.dp else 28.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Palette.of(i).light, Palette.of(i).base, Palette.of(i).shade)))
                        .border(if (sel) 2.5.dp else 1.dp, if (sel) Theme.gold else Color.Black.copy(alpha = 0.6f), CircleShape).pressable { color = i },
                )
            }
        }
        if (categories.size > 1) LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(categories) { c -> Chip(c.label, c == category, { cat = c.name; design = 0 }) }
        }
        RhTextField(query, { query = it }, label = "Search designs", modifier = Modifier.fillMaxWidth())
        LazyVerticalGrid(GridCells.Adaptive(76.dp), Modifier.weight(1f, fill = true).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(visible, key = { category.ordinal * 100 + it }) { d ->
                val sel = d == design
                Column(Modifier.pressable { design = d }.clip(RoundedCornerShape(8.dp)).background(if (sel) Color(0x33D9A441) else Color.Black.copy(alpha = 0.25f))
                    .border(if (sel) 2.dp else 1.dp, if (sel) Theme.gold else Theme.goldDark.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) { ItemArt(AssetRef(category, d, color, false), 64.dp) }
                    RhText(names[d], style = Theme.small.copy(fontSize = 10.sp), maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
        if (onPick != null) WoodButton("Use this image", { onPick(ref) }, Modifier.fillMaxWidth(), kind = BtnKind.GOLD)
    }
}

@Composable
fun GlowSwitch(on: Boolean, onChange: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier.width(46.dp).height(26.dp).clip(shape).background(if (on) Theme.gold.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.5f))
            .border(1.5.dp, Theme.goldDark, shape).pressable { onChange(!on) }.padding(3.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) { Box(Modifier.size(20.dp).clip(CircleShape).background(if (on) Color(0xFFFFF3C4) else Theme.onDarkSoft)) }
}

/** Full-screen dialog wrapper used by editors. */
@Composable
fun AssetPickerDialog(initial: AssetRef?, categories: List<ArtCategory>, onDismiss: () -> Unit, onPick: (AssetRef) -> Unit) {
    com.runehearth.app.ui.components.RhDialog(onDismiss, "Choose an Image", modifier = Modifier.height(640.dp)) {
        AssetPickerPanel(initial, categories, Modifier.fillMaxSize(), onPick = { onPick(it); onDismiss() })
    }
}

/** Compact image selector button: shows the current art and opens the picker. */
@Composable
fun ArtField(label: String, value: String, categories: List<ArtCategory>, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val ref = AssetRef.decode(value)
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.pressable { open = true }) { com.runehearth.app.ui.components.ArtSlot(ref, 60.dp) }
        Column(Modifier.weight(1f)) {
            RhText(label.uppercase(), style = Theme.label.copy(fontSize = 10.5.sp))
            RhText(if (ref == null) "No image — tap to choose" else DesignNames.name(ref.category, ref.design), style = Theme.small)
        }
        WoodButton("Choose", { open = true }, compact = true, kind = BtnKind.SECONDARY)
    }
    if (open) AssetPickerDialog(ref, categories, { open = false }) { onChange(it.encode()) }
}
