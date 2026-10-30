package com.runehearth.app.ui.dm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runehearth.app.core.SecureSource
import com.runehearth.app.data.Campaign
import com.runehearth.app.engine.Action
import com.runehearth.app.session.newActionId
import com.runehearth.app.ui.LocalApp
import com.runehearth.app.ui.components.BtnKind
import com.runehearth.app.ui.components.Chip
import com.runehearth.app.ui.components.LocalToaster
import com.runehearth.app.ui.components.RhDialog
import com.runehearth.app.ui.components.RhIcon
import com.runehearth.app.ui.components.RhText
import com.runehearth.app.ui.components.RhTextField
import com.runehearth.app.ui.components.SectionHeader
import com.runehearth.app.ui.components.Stepper
import com.runehearth.app.ui.components.WoodButton
import com.runehearth.app.ui.rememberDispatch
import com.runehearth.app.ui.sheet.ItemTemplates
import com.runehearth.app.ui.theme.Theme
import kotlinx.coroutines.launch

private val SYL_A = listOf("Ar", "Bal", "Cor", "Dra", "El", "Fen", "Gal", "Hel", "Ith", "Jor", "Kel", "Lor", "Mor", "Nyx", "Oth", "Per", "Quil", "Rav", "Sil", "Thor", "Ul", "Val", "Wyn", "Xan", "Yor", "Zer")
private val SYL_B = listOf("a", "en", "ia", "or", "wyn", "ric", "dis", "mir", "thas", "vyn", "dor", "lan", "ek", "ion", "ar")
private val SYL_C = listOf("", "n", "th", "s", "ra", "las", "ron", "dil", "gard", "mar", "wick", "brook")

/** Campaign settings, loot generator, name generator and manual snapshot. */
@Composable
fun DmTools(camp: Campaign, onDismiss: () -> Unit) {
    val app = LocalApp.current
    val dispatch = rememberDispatch()
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    val rng = remember { SecureSource() }
    var name by remember { mutableStateOf(camp.name) }
    var tLow by remember { mutableStateOf(camp.tLow) }
    var tLm by remember { mutableStateOf(camp.tLowMedium) }
    var tM by remember { mutableStateOf(camp.tMedium) }
    var tHm by remember { mutableStateOf(camp.tHighMedium) }
    var weight by remember { mutableStateOf(camp.weightLimit) }
    var names by remember { mutableStateOf(emptyList<String>()) }
    var lootCount by remember { mutableStateOf(3) }
    val isRemote = com.runehearth.app.ui.LocalSession.current is com.runehearth.app.net.RemoteSession
    var restoring by remember { mutableStateOf<java.io.File?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val text = app.backup.export(camp.id)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } }
            }.onSuccess { toaster.show("Migration package saved") }.onFailure { toaster.show("Export failed: ${it.message}", true) }
        }
    }
    restoring?.let { f ->
        com.runehearth.app.ui.components.ConfirmDialog("Restore this snapshot?", "The campaign returns to the state saved in this file. It is validated first, then this device becomes the host and all players resync.", "Restore", {
            scope.launch {
                runCatching {
                    val id = app.backup.import(f.readText(), takeOverHostFor = app.prefs.deviceId())
                    app.engine.rebuildChangeLog(id)
                }.onSuccess { toaster.show("Snapshot restored") }.onFailure { toaster.show("Restore refused: ${it.message}", true) }
            }
        }, { restoring = null })
    }
    RhDialog(onDismiss, "Dungeon Master Tools", modifier = Modifier.height(640.dp), actions = { WoodButton("Close", onDismiss, kind = BtnKind.SECONDARY, compact = true) }) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader("Loot Generator")
            Stepper(lootCount, { lootCount = it }, min = 1, max = 10, label = "Items")
            WoodButton("Roll treasure into the party stash", {
                repeat(lootCount) {
                    // rarer items are less likely: weight by inverse rarity rank
                    val pool = ItemTemplates.all
                    val pick = pool.filter { t -> rng.nextInt(1 + t.rarity.ordinal * 2) == 0 }.ifEmpty { pool }
                    val t = pick[rng.nextInt(pick.size)]
                    dispatch(Action.UpsertItem(newActionId(), t.toItem(camp.id, null), mergeStacks = true))
                }
                toaster.show("$lootCount item(s) added to the party stash")
            }, Modifier.fillMaxWidth(), icon = RhIcon.GEM, kind = BtnKind.GOLD, compact = true)
            SectionHeader("Name Generator")
            WoodButton("Generate names", { names = List(6) { SYL_A[rng.nextInt(SYL_A.size)] + SYL_B[rng.nextInt(SYL_B.size)] + SYL_C[rng.nextInt(SYL_C.size)] } }, Modifier.fillMaxWidth(), icon = RhIcon.SCROLL, kind = BtnKind.SECONDARY, compact = true)
            names.forEach { RhText(it, style = Theme.heading.copy(fontSize = 17.sp)) }
            SectionHeader("Campaign Rules")
            RhTextField(name, { name = it.take(60) }, label = "Campaign name", onDark = false)
            RhText("ROLL CLASSIFICATION THRESHOLDS (% of possible range)", style = Theme.label.copy(fontSize = 10.sp), color = Theme.goldDark)
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Stepper(tLow, { tLow = it }, Modifier, 1, 98, 5, "Low <")
                Stepper(tLm, { tLm = it }, Modifier, 2, 98, 5, "Low-Med <")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Stepper(tM, { tM = it }, Modifier, 3, 98, 5, "Medium <")
                Stepper(tHm, { tHm = it }, Modifier, 4, 99, 5, "High-Med <")
            }
            RhText("At or above the last value is a High Roll.", style = Theme.small)
            Stepper(weight, { weight = it }, min = 0, max = 99999, step = 5, label = "Carry weight limit (0 = off)")
            WoodButton("Save rules", {
                dispatch(Action.UpdateCampaign(newActionId(), camp.copy(name = name, tLow = tLow, tLowMedium = tLm, tMedium = tM, tHighMedium = tHm, weightLimit = weight)))
            }, Modifier.fillMaxWidth(), compact = true, enabled = tLow < tLm && tLm < tM && tM < tHm)
            SectionHeader("Recovery & Host Migration")
            RhText("If the host phone dies, nothing committed is lost: every device keeps its data. Choose a new host explicitly — it is never elected automatically — and the campaign is validated before it becomes authoritative.", style = Theme.small)
            WoodButton("Export migration package (.json)", { exportLauncher.launch("${camp.name.replace(Regex("[^A-Za-z0-9_-]"), "_")}-migration.json") }, Modifier.fillMaxWidth(), compact = true, kind = BtnKind.SECONDARY, icon = RhIcon.SHARE)
            RhText("On the new host phone: Home → Import. It validates the file, takes over as host and every player resyncs cleanly.", style = Theme.small)
            if (!isRemote) {
                val snaps = remember { app.backup.snapshots(app.snapshotDir, camp.id) }
                if (snaps.isEmpty()) RhText("No local snapshot yet (one is written whenever you close a session).", style = Theme.small)
                snaps.forEach { f ->
                    WoodButton("Restore snapshot from ${java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(java.util.Date(f.lastModified()))}", { restoring = f }, Modifier.fillMaxWidth(), compact = true, kind = BtnKind.DANGER)
                }
            }
            SectionHeader("Safety")
            WoodButton("Save local snapshot now", { scope.launch { runCatching { app.backup.snapshot(app.snapshotDir, camp.id) }.onSuccess { toaster.show("Snapshot saved") }.onFailure { toaster.show("Snapshot failed", true) } } }, Modifier.fillMaxWidth(), compact = true, kind = BtnKind.SECONDARY)
        }
    }
}
