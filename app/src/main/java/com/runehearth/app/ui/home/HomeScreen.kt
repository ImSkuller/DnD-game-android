package com.runehearth.app.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runehearth.app.data.Campaign
import com.runehearth.app.data.CampaignMode
import com.runehearth.app.ui.LocalApp
import com.runehearth.app.ui.components.BtnKind
import com.runehearth.app.ui.components.Chip
import com.runehearth.app.ui.components.ConfirmDialog
import com.runehearth.app.ui.components.EmptyState
import com.runehearth.app.ui.components.LocalToaster
import com.runehearth.app.ui.components.Panel
import com.runehearth.app.ui.components.RhDialog
import com.runehearth.app.ui.components.RhIcon
import com.runehearth.app.ui.components.RhText
import com.runehearth.app.ui.components.RhTextField
import com.runehearth.app.ui.components.SectionHeader
import com.runehearth.app.ui.components.Tag
import com.runehearth.app.ui.components.WoodButton
import com.runehearth.app.ui.rememberDao
import com.runehearth.app.ui.rememberFlow
import com.runehearth.app.ui.theme.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

/** Vector emblem used on the home screen: gold ring, heater shield, hearth flame and sword. */
@Composable
fun RunehearthEmblem(size: Dp, modifier: Modifier = Modifier) {
    val gold = Theme.gold; val goldDark = Theme.goldDark
    Canvas(modifier.size(size)) {
        val u = this.size.minDimension / 108f
        scale(u, u, Offset.Zero) {
            drawCircle(Brush.radialGradient(listOf(Color(0x55FFB84D), Color.Transparent), Offset(54f, 56f), 60f), 60f, Offset(54f, 56f))
            drawCircle(gold, 38f, Offset(54f, 54f), style = Stroke(2.6f))
            drawCircle(goldDark, 34f, Offset(54f, 54f), style = Stroke(1f))
            val shield = Path().apply { moveTo(54f, 26f); lineTo(78f, 34f); lineTo(78f, 54f); cubicTo(78f, 69f, 67f, 78f, 54f, 84f); cubicTo(41f, 78f, 30f, 69f, 30f, 54f); lineTo(30f, 34f); close() }
            drawPath(shield, Color(0xFF3A2214)); drawPath(shield, gold, style = Stroke(2.2f, join = StrokeJoin.Round))
            val flame = Path().apply { moveTo(54f, 36f); cubicTo(60f, 44f, 65f, 48f, 65f, 57f); cubicTo(65f, 64f, 60f, 70f, 54f, 70f); cubicTo(48f, 70f, 43f, 64f, 43f, 57f); cubicTo(43f, 52f, 46f, 49f, 48.5f, 46f); cubicTo(50f, 49f, 52f, 50f, 53f, 50f); cubicTo(52f, 45f, 52f, 40f, 54f, 36f); close() }
            drawPath(flame, Color(0xFFE8651C))
            val inner = Path().apply { moveTo(54f, 49f); cubicTo(57.5f, 53f, 60f, 56f, 60f, 60f); cubicTo(60f, 64f, 57f, 66f, 54f, 66f); cubicTo(51f, 66f, 48f, 64f, 48f, 60f); cubicTo(48f, 57f, 50.5f, 55.5f, 51.5f, 54f); cubicTo(52.5f, 56f, 54f, 56f, 54f, 56f); cubicTo(53f, 53f, 53f, 51f, 54f, 49f); close() }
            drawPath(inner, Color(0xFFFFC247))
            val blade = Path().apply { moveTo(51.5f, 6f); lineTo(56.5f, 6f); lineTo(57.5f, 24f); lineTo(54f, 28f); lineTo(50.5f, 24f); close() }
            drawPath(blade, Color(0xFFD5DEE6)); drawPath(Path().apply { moveTo(54f, 6f); lineTo(56.5f, 6f); lineTo(57.5f, 24f); lineTo(54f, 28f); close() }, Color(0xFF9AA8B5))
            drawRect(gold, Offset(47f, 24f), androidx.compose.ui.geometry.Size(14f, 3.4f))
            for (sx in listOf(36f, 72f)) { val t = Path().apply { moveTo(sx - 4f, 32f); lineTo(sx, 24f); lineTo(sx + 4f, 32f); close() }; drawPath(t, gold) }
        }
    }
}

@Composable
fun HomeScreen(onOpen: (Campaign) -> Unit, onJoin: () -> Unit, onSettings: () -> Unit, onAssets: () -> Unit) {
    val app = LocalApp.current
    val dao = rememberDao()
    val campaigns by rememberFlow("campaigns", emptyList()) { dao.observeCampaigns() }
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Campaign?>(null) }
    var exporting by remember { mutableStateOf<Campaign?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val c = exporting; exporting = null
        if (uri != null && c != null) scope.launch {
            runCatching {
                val text = app.backup.export(c.id)
                withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } }
            }.onSuccess { toaster.show("Campaign exported") }.onFailure { toaster.show("Export failed: ${it.message}", true) }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val text = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { String(it.readBytes()) } ?: error("empty file") }
                val id = app.backup.import(text)
                app.engine.rebuildChangeLog(id)
            }.onSuccess { toaster.show("Campaign imported") }.onFailure { toaster.show("Import failed: ${it.message}", true) }
        }
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            RunehearthEmblem(132.dp)
            RhText("RUNEHEARTH", style = Theme.title.copy(fontSize = 34.sp, letterSpacing = 6.sp), color = Theme.gold, textAlign = TextAlign.Center)
            RhText("Chronicle of the Dungeon Hearth", style = Theme.small, color = Theme.onDarkSoft, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WoodButton("New Campaign", { creating = true }, Modifier.weight(1f), icon = RhIcon.PLUS, kind = BtnKind.GOLD)
                WoodButton("Join Game", onJoin, Modifier.weight(1f), icon = RhIcon.WIFI)
            }
            Spacer(Modifier.height(6.dp))
            SectionHeader("Your Campaigns")
        }
        val mine = campaigns.filter { it.isHost }
        if (mine.isEmpty()) item {
            EmptyState(RhIcon.CASTLE, "No campaigns yet", "Forge a new campaign to begin your adventure, or import a saved one.") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WoodButton("Sample campaign", { scope.launch { val id = com.runehearth.app.data.DemoData.create(app); onOpen(dao.campaign(id)!!) } }, compact = true, kind = BtnKind.GOLD, icon = RhIcon.STAR)
                }
            }
        }
        items(mine, key = { it.id }) { c ->
            Panel(Modifier.fillMaxWidth(), onClick = { onOpen(c) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        RhText(c.name, style = Theme.heading, maxLines = 1)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Tag(if (c.mode == CampaignMode.COOP) "Local Co-op" else "Local Only", if (c.mode == CampaignMode.COOP) Color(0xFF5B9BE8) else Color(0xFF5CB85C))
                            RhText(DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(c.updatedAt)), style = Theme.small)
                        }
                    }
                    com.runehearth.app.ui.components.RhIconButton(RhIcon.SHARE, { exporting = c; exportLauncher.launch("${c.name.replace(Regex("[^A-Za-z0-9_-]"), "_")}.rhcamp.json") }, tint = Theme.onDarkSoft, size = 20.dp)
                    com.runehearth.app.ui.components.RhIconButton(RhIcon.TRASH, { deleting = c }, tint = Color(0xFFD35B52), size = 20.dp)
                }
            }
        }
        val joined = campaigns.filter { !it.isHost }
        if (joined.isNotEmpty()) {
            item { SectionHeader("Joined Games (offline copies)") }
            items(joined, key = { it.id }) { c ->
                Panel(Modifier.fillMaxWidth(), surface = com.runehearth.app.ui.components.Surface.STONE, onClick = onJoin) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { RhText(c.name, style = Theme.heading, maxLines = 1); RhText("Tap to rejoin the host", style = Theme.small) }
                        com.runehearth.app.ui.components.RhIconButton(RhIcon.TRASH, { deleting = c }, tint = Color(0xFFD35B52), size = 20.dp)
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WoodButton("Import", { importLauncher.launch(arrayOf("*/*")) }, Modifier.weight(1f), icon = RhIcon.SCROLL, kind = BtnKind.SECONDARY, compact = true)
                WoodButton("Assets", onAssets, Modifier.weight(1f), icon = RhIcon.GEM, kind = BtnKind.SECONDARY, compact = true)
                WoodButton("Settings", onSettings, Modifier.weight(1f), icon = RhIcon.GEAR, kind = BtnKind.SECONDARY, compact = true)
            }
        }
    }

    if (creating) NewCampaignDialog({ creating = false }) { name, mode ->
        scope.launch { val id = app.createCampaign(name, mode); creating = false; onOpen(dao.campaign(id)!!) }
    }
    deleting?.let { c ->
        ConfirmDialog("Delete campaign?", "\"${c.name}\" and everything in it will be erased from this device. Export it first if you may want it back.", "Delete", { scope.launch { app.backup.wipe(c.id); toaster.show("Campaign deleted") } }, { deleting = null })
    }
}

@Composable
private fun NewCampaignDialog(onDismiss: () -> Unit, onCreate: (String, CampaignMode) -> Unit) {
    var name by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(CampaignMode.LOCAL) }
    RhDialog(onDismiss, "Forge a New Campaign", actions = {
        WoodButton("Cancel", onDismiss, kind = BtnKind.SECONDARY, compact = true)
        WoodButton("Create", { onCreate(name, mode) }, kind = BtnKind.GOLD, compact = true, enabled = name.isNotBlank())
    }) {
        RhTextField(name, { name = it.take(60) }, label = "Campaign name", onDark = false)
        Spacer(Modifier.height(10.dp))
        RhText("MODE", style = Theme.label.copy(fontSize = 10.5.sp), color = Theme.goldDark)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Local Only", mode == CampaignMode.LOCAL, { mode = CampaignMode.LOCAL })
            Chip("Local Co-op", mode == CampaignMode.COOP, { mode = CampaignMode.COOP })
        }
        Spacer(Modifier.height(6.dp))
        RhText(
            if (mode == CampaignMode.LOCAL) "One device. You, the Dungeon Master, manage every character, coin and roll." else
                "You host on your phone; players join from theirs over Wi-Fi or a hotspot — no internet needed. Your device is the source of truth.",
            style = Theme.small,
        )
    }
}
