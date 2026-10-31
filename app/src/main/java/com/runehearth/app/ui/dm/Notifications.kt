package com.runehearth.app.ui.dm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runehearth.app.ui.LocalApp
import com.runehearth.app.ui.components.BtnKind
import com.runehearth.app.ui.components.EmptyState
import com.runehearth.app.ui.components.LocalToaster
import com.runehearth.app.ui.components.RhDialog
import com.runehearth.app.ui.components.RhIcon
import com.runehearth.app.ui.components.RhIconButton
import com.runehearth.app.ui.components.RhText
import com.runehearth.app.ui.components.WoodButton
import com.runehearth.app.ui.rememberDao
import com.runehearth.app.ui.rememberFlow
import com.runehearth.app.ui.theme.Theme
import java.text.DateFormat
import java.util.Date

/**
 * Bell in the DM's top bar. Players may edit their own stats and inventory; every such edit arrives here as a
 * notification ("Aelwyn changed Attack 5 → 7"), with an unread badge and a toast while the DM is online.
 */
@Composable
fun NotificationBell(campaignId: String) {
    val app = LocalApp.current
    val dao = rememberDao()
    val toaster = LocalToaster.current
    val events by rememberFlow(campaignId, emptyList()) { dao.observeEvents(campaignId, 200) }
    val notes = remember(events) { events.filter { it.type == "notify" } }
    var seen by remember { mutableLongStateOf(app.notifySeen(campaignId)) }
    var open by remember { mutableStateOf(false) }
    val unread = notes.count { it.seq > seen }
    val latest = notes.firstOrNull()?.seq ?: 0L
    var announced by remember { mutableLongStateOf(-1L) }
    LaunchedEffect(latest) {
        if (announced == -1L) announced = latest // don't toast history on first load
        else if (latest > announced) { announced = latest; notes.firstOrNull()?.let { toaster.show("🔔 ${it.text}") } }
    }
    Box {
        RhIconButton(RhIcon.BELL, { open = true }, tint = if (unread > 0) Theme.gold else Theme.onDarkSoft, size = 22.dp)
        if (unread > 0) Box(Modifier.align(Alignment.TopEnd).offset(x = (-6).dp, y = 8.dp).size(16.dp).background(Color(0xFFD9453B), CircleShape), contentAlignment = Alignment.Center) {
            RhText(if (unread > 9) "9+" else unread.toString(), style = Theme.label.copy(fontSize = 9.sp, letterSpacing = 0.sp), color = Color.White)
        }
    }
    if (open) {
        RhDialog({ open = false; seen = latest; app.setNotifySeen(campaignId, latest) }, "Player changes", modifier = Modifier.height(520.dp), actions = {
            WoodButton("Mark all read", { seen = latest; app.setNotifySeen(campaignId, latest) }, compact = true, kind = BtnKind.SECONDARY, enabled = unread > 0)
            WoodButton("Close", { open = false; seen = latest; app.setNotifySeen(campaignId, latest) }, compact = true)
        }) {
            if (notes.isEmpty()) EmptyState(RhIcon.BELL, "All quiet", "When a player changes their stats or inventory, you'll see it here.")
            LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(notes, key = { it.seq }) { n ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(8.dp).background(if (n.seq > seen) Color(0xFFD9453B) else Color.Transparent, CircleShape))
                        Column(Modifier.weight(1f)) {
                            RhText(n.text)
                            RhText(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(n.ts)), style = Theme.small)
                        }
                    }
                }
            }
        }
    }
}
