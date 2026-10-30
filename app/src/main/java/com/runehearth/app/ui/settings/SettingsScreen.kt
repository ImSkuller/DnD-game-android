package com.runehearth.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runehearth.app.ui.LocalApp
import com.runehearth.app.ui.components.BtnKind
import com.runehearth.app.ui.components.Chip
import com.runehearth.app.ui.components.Panel
import com.runehearth.app.ui.components.RhText
import com.runehearth.app.ui.components.RhTextField
import com.runehearth.app.ui.components.ScreenTopBar
import com.runehearth.app.ui.components.SectionHeader
import com.runehearth.app.ui.components.Surface
import com.runehearth.app.ui.components.WoodButton
import com.runehearth.app.ui.theme.Theme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var dbSize by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        name = app.prefs.playerName.first()
        dbSize = app.getDatabasePath("runehearth.db").length() + (app.snapshotDir.listFiles()?.sumOf { it.length() } ?: 0L)
    }
    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("Settings", onBack)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Panel(Modifier.fillMaxWidth()) {
                    SectionHeader("Theme")
                    Chip("Medieval Fantasy", true, {})
                    Spacer(Modifier.height(4.dp))
                    RhText("More themes can be added later without changing any screen.", style = Theme.small)
                }
            }
            item {
                Panel(Modifier.fillMaxWidth()) {
                    SectionHeader("Player Name")
                    RhTextField(name, { name = it.take(40) }, label = "Shown to the host when you join")
                    Spacer(Modifier.height(6.dp))
                    WoodButton("Save", { scope.launch { app.prefs.setPlayerName(name) } }, compact = true, kind = BtnKind.SECONDARY)
                }
            }
            item {
                Panel(Modifier.fillMaxWidth(), surface = Surface.STONE) {
                    SectionHeader("Storage")
                    RhText("Game data on this device: ${"%.1f".format(dbSize / 1024.0)} KB. Records are updated in place; history is capped.", style = Theme.small)
                    RhText("A local snapshot file is written whenever you close a session.", style = Theme.small)
                }
            }
            item {
                Panel(Modifier.fillMaxWidth(), surface = Surface.PARCHMENT) {
                    SectionHeader("About Runehearth")
                    RhText("Offline companion for tabletop dungeon crawls. The Dungeon Master's phone is the single source of truth; players sync over Wi-Fi or hotspot with no internet and no server.", style = Theme.body)
                    Spacer(Modifier.height(6.dp))
                    RhText("Dice use the device's cryptographically secure random generator with unbiased sampling.", style = Theme.small)
                }
            }
        }
    }
}
