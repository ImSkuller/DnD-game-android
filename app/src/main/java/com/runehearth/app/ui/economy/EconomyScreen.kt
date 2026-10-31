package com.runehearth.app.ui.economy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runehearth.app.core.Coin
import com.runehearth.app.core.Money
import com.runehearth.app.data.GameCharacter
import com.runehearth.app.data.MoneyTx
import com.runehearth.app.data.Wallet
import com.runehearth.app.engine.Action
import com.runehearth.app.session.newActionId
import com.runehearth.app.ui.LocalSession
import com.runehearth.app.ui.components.BtnKind
import com.runehearth.app.ui.components.Chip
import com.runehearth.app.ui.components.CoinImage
import com.runehearth.app.ui.components.CoinRow
import com.runehearth.app.ui.components.EmptyState
import com.runehearth.app.ui.components.LocalToaster
import com.runehearth.app.ui.components.Panel
import com.runehearth.app.ui.components.RhIcon
import com.runehearth.app.ui.components.RhIconButton
import com.runehearth.app.ui.components.RhNumberField
import com.runehearth.app.ui.components.RhText
import com.runehearth.app.ui.components.RhTextField
import com.runehearth.app.ui.components.SectionHeader
import com.runehearth.app.ui.components.Surface
import com.runehearth.app.ui.components.Tag
import com.runehearth.app.ui.components.WoodButton
import com.runehearth.app.ui.rememberDao
import com.runehearth.app.ui.rememberDispatch
import com.runehearth.app.ui.rememberFlow
import com.runehearth.app.ui.theme.Theme
import java.text.DateFormat
import java.util.Date

/** Four-denomination amount entry that reports a single bronze total. */
@Composable
fun CoinInput(onChange: (Long) -> Unit, modifier: Modifier = Modifier, resetKey: Any? = null) {
    var holy by remember(resetKey) { mutableLongStateOf(0) }
    var gold by remember(resetKey) { mutableLongStateOf(0) }
    var silver by remember(resetKey) { mutableLongStateOf(0) }
    var bronze by remember(resetKey) { mutableLongStateOf(0) }
    fun emit() = onChange(runCatching { Money.of(holy, gold, silver, bronze) }.getOrDefault(0))
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for ((coin, getSet) in listOf(
            Coin.HOLY_GOLD to Pair({ holy }, { v: Long -> holy = v }), Coin.GOLD to Pair({ gold }, { v: Long -> gold = v }),
            Coin.SILVER to Pair({ silver }, { v: Long -> silver = v }), Coin.BRONZE to Pair({ bronze }, { v: Long -> bronze = v }),
        )) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                CoinImage(coin, 26.dp)
                RhNumberField(getSet.first(), { getSet.second(it.coerceAtMost(99_999_999)); emit() }, Modifier.fillMaxWidth(), max = 99_999_999)
            }
        }
    }
}

/** Dungeon Master economy: purses, funds adjustments, transfers, conversion helper and history. */
@Composable
fun EconomyScreen(campaignId: String, modifier: Modifier = Modifier) {
    val dao = rememberDao()
    val dispatch = rememberDispatch()
    val toaster = LocalToaster.current
    val wallets by rememberFlow(campaignId, emptyList()) { dao.observeWallets(campaignId) }
    val chars by rememberFlow(campaignId, emptyList()) { dao.observeCharacters(campaignId) }
    val tx by rememberFlow(campaignId, emptyList()) { dao.observeTx(campaignId, 60) }
    val party = wallets.firstOrNull { it.ownerId == null }
    val byOwner = wallets.associateBy { it.ownerId }
    val players = chars.filter { !it.isNpc }
    var target by rememberSaveable { mutableStateOf(Wallet.partyId(campaignId)) }
    var amount by remember { mutableLongStateOf(0) }
    var reason by rememberSaveable { mutableStateOf("") }
    var resetKey by remember { mutableStateOf(0) }
    var from by rememberSaveable { mutableStateOf(Wallet.partyId(campaignId)) }
    var to by rememberSaveable { mutableStateOf("") }
    var xferAmount by remember { mutableLongStateOf(0) }
    var xferKey by remember { mutableStateOf(0) }
    var convAmount by remember { mutableLongStateOf(0) }
    var convCoin by rememberSaveable { mutableStateOf(Coin.GOLD.name) }

    LazyColumn(modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Panel(Modifier.fillMaxWidth(), surface = Surface.LEATHER) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RhText("PARTY PURSE", Modifier.weight(1f), style = Theme.label.copy(fontSize = 13.sp))
                    party?.let { p -> HiddenToggle(p.hidden) { dispatch(Action.SetWalletHidden(newActionId(), p.id, it)) } }
                }
                CoinRow(party?.balance ?: 0, coinSize = 40.dp)
                RhText("Total ${Money.format(party?.balance ?: 0)}", Modifier.fillMaxWidth(), style = Theme.small, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                if (party?.hidden == true) RhText("Hidden from players", style = Theme.small, color = Theme.crimson)
            }
        }
        item { SectionHeader("Personal Purses") }
        if (players.isEmpty()) item { EmptyState(RhIcon.COIN, "No adventurers yet", "Add characters in the Party tab to give them purses.") }
        items(players, key = { it.id }) { c ->
            val w = byOwner[c.id]
            Panel(Modifier.fillMaxWidth(), padding = 10.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        RhText(c.name, style = Theme.heading.copy(fontSize = 16.sp), maxLines = 1)
                        RhText(Money.format(w?.balance ?: 0), style = Theme.small, color = Theme.gold)
                    }
                    if (w != null) HiddenToggle(w.hidden) { dispatch(Action.SetWalletHidden(newActionId(), w.id, it)) }
                    Chip("Select", target == Wallet.personalId(c.id), { target = Wallet.personalId(c.id) })
                }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth(), surface = Surface.PARCHMENT) {
                SectionHeader("Add or Remove Funds")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item { Chip("Party", target == Wallet.partyId(campaignId), { target = Wallet.partyId(campaignId) }) }
                        items(players) { c -> Chip(c.name, target == Wallet.personalId(c.id), { target = Wallet.personalId(c.id) }) }
                    }
                }
                Spacer(Modifier.height(6.dp))
                CoinInput({ amount = it }, resetKey = resetKey)
                Spacer(Modifier.height(6.dp))
                RhTextField(reason, { reason = it.take(80) }, label = "Reason (e.g. quest reward, shop purchase)", onDark = false)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WoodButton("Add ${Money.format(amount)}", { dispatch(Action.AdjustWallet(newActionId(), target, amount, reason)); resetKey++; amount = 0 }, Modifier.weight(1f), icon = RhIcon.PLUS, kind = BtnKind.GOLD, compact = true, enabled = amount > 0)
                    WoodButton("Remove", { dispatch(Action.AdjustWallet(newActionId(), target, -amount, reason)); resetKey++; amount = 0 }, Modifier.weight(1f), icon = RhIcon.MINUS, kind = BtnKind.DANGER, compact = true, enabled = amount > 0)
                }
                if (players.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    WoodButton("Reward every adventurer", { players.forEach { p -> dispatch(Action.AdjustWallet(newActionId(), Wallet.personalId(p.id), amount, reason.ifBlank { "Reward" })) }; resetKey++; amount = 0 }, Modifier.fillMaxWidth(), icon = RhIcon.CROWN, kind = BtnKind.SECONDARY, compact = true, enabled = amount > 0)
                }
            }
        }
        item {
            com.runehearth.app.ui.components.Collapsible("Transfer between purses") {
            Panel(Modifier.fillMaxWidth()) {
                
                RhText("FROM", style = Theme.label.copy(fontSize = 10.sp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { Chip("Party", from == Wallet.partyId(campaignId), { from = Wallet.partyId(campaignId) }) }
                    items(players) { c -> Chip(c.name, from == Wallet.personalId(c.id), { from = Wallet.personalId(c.id) }) }
                }
                Spacer(Modifier.height(6.dp))
                RhText("TO", style = Theme.label.copy(fontSize = 10.sp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { Chip("Party", to == Wallet.partyId(campaignId), { to = Wallet.partyId(campaignId) }) }
                    items(players) { c -> Chip(c.name, to == Wallet.personalId(c.id), { to = Wallet.personalId(c.id) }) }
                }
                Spacer(Modifier.height(6.dp))
                CoinInput({ xferAmount = it }, resetKey = xferKey)
                Spacer(Modifier.height(6.dp))
                WoodButton("Transfer ${Money.format(xferAmount)}", { dispatch(Action.TransferFunds(newActionId(), from, to, xferAmount, "Transfer")); xferKey++; xferAmount = 0 }, Modifier.fillMaxWidth(), icon = RhIcon.ARROW, compact = true, enabled = xferAmount > 0 && to.isNotEmpty() && from != to)
            }
        }
        }
        item {
            com.runehearth.app.ui.components.Collapsible("Currency converter") {
            Panel(Modifier.fillMaxWidth(), surface = Surface.STONE) {
                
                RhText("10 Bronze = 1 Silver · 10 Silver = 1 Gold · 20 Gold = 1 Holy Gold", style = Theme.small)
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(Coin.entries) { c -> Chip(c.label, convCoin == c.name, { convCoin = c.name }) } }
                RhNumberField(convAmount, { convAmount = it }, Modifier.fillMaxWidth(), "Amount", max = 999_999_999)
                Spacer(Modifier.height(4.dp))
                val from = Coin.valueOf(convCoin)
                for (to in Coin.entries.filter { it != from }) {
                    val (n, rem) = runCatching { Money.convert(convAmount, from, to) }.getOrDefault(0L to 0L)
                    Row(verticalAlignment = Alignment.CenterVertically) { CoinImage(to, 22.dp); Spacer(Modifier.width(8.dp)); RhText("$n ${to.label}" + if (rem > 0) " (+$rem bronze left over)" else "") }
                }
            }
        }
        }
        item { SectionHeader("Transaction History") }
        if (tx.isEmpty()) item { RhText("No transactions yet.", style = Theme.small, color = Theme.onDarkSoft) }
        items(tx, key = { it.id }) { t -> TxRow(t, chars, campaignId) }
    }
}

@Composable
fun HiddenToggle(hidden: Boolean, onChange: (Boolean) -> Unit) {
    RhIconButton(if (hidden) RhIcon.LOCK else RhIcon.EYE, { onChange(!hidden) }, tint = if (hidden) Theme.crimson else Theme.onDarkSoft, size = 20.dp)
}

@Composable
fun TxRow(t: MoneyTx, chars: List<GameCharacter>, campaignId: String) {
    fun name(id: String?): String = when {
        id == null -> "the treasury"
        id == Wallet.partyId(campaignId) -> "Party"
        else -> chars.firstOrNull { Wallet.personalId(it.id) == id }?.name ?: "Adventurer"
    }
    val credit = t.fromWallet == null
    val debit = t.toWallet == null
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RhIcon.COIN.let { com.runehearth.app.ui.components.RhIconView(it, if (credit) Color(0xFF5CB85C) else if (debit) Color(0xFFD9453B) else Theme.gold, 20.dp) }
        Column(Modifier.weight(1f)) {
            RhText(when { credit -> "+ to ${name(t.toWallet)}"; debit -> "− from ${name(t.fromWallet)}"; else -> "${name(t.fromWallet)} → ${name(t.toWallet)}" }, style = Theme.body, maxLines = 1)
            RhText((if (t.reason.isNotBlank()) t.reason + " · " else "") + DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(t.ts)), style = Theme.small, maxLines = 1)
        }
        RhText(Money.format(t.amount), style = Theme.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = if (credit) Color(0xFF5CB85C) else if (debit) Color(0xFFD9453B) else Theme.onDark)
    }
}

/** Player view of money: own purse, party purse (if the DM shows it), transfers and history. */
@Composable
fun PlayerPurse(campaignId: String, characterId: String, modifier: Modifier = Modifier) {
    val dao = rememberDao()
    val dispatch = rememberDispatch()
    val wallets by rememberFlow(campaignId, emptyList()) { dao.observeWallets(campaignId) }
    val tx by rememberFlow(campaignId, emptyList()) { dao.observeTx(campaignId, 40) }
    val chars by rememberFlow(campaignId, emptyList()) { dao.observeCharacters(campaignId) }
    val mine = wallets.firstOrNull { it.ownerId == characterId }
    val party = wallets.firstOrNull { it.ownerId == null }
    var amount by remember { mutableLongStateOf(0) }
    var key by remember { mutableStateOf(0) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Panel(Modifier.fillMaxWidth(), surface = Surface.LEATHER) {
                SectionHeader("My Purse")
                if (mine == null) RhText("The Dungeon Master keeps your funds secret.", style = Theme.small) else { CoinRow(mine.balance, coinSize = 40.dp); RhText("Total ${Money.format(mine.balance)}", Modifier.fillMaxWidth(), style = Theme.small, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                SectionHeader("Party Purse")
                if (party == null) RhText("Hidden by the Dungeon Master.", style = Theme.small) else { CoinRow(party.balance, coinSize = 34.dp); RhText("Total ${Money.format(party.balance)}", Modifier.fillMaxWidth(), style = Theme.small, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
            }
        }
        if (mine != null && party != null) item {
            Panel(Modifier.fillMaxWidth(), surface = Surface.PARCHMENT) {
                SectionHeader("Contribute to the Party")
                CoinInput({ amount = it }, resetKey = key)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WoodButton("Give to party", { dispatch(Action.TransferFunds(newActionId(), mine.id, party.id, amount, "Contribution")); key++; amount = 0 }, Modifier.weight(1f), icon = RhIcon.ARROW, compact = true, enabled = amount in 1..mine.balance)
                    WoodButton("Take from party", { dispatch(Action.TransferFunds(newActionId(), party.id, mine.id, amount, "Withdrawal")); key++; amount = 0 }, Modifier.weight(1f), kind = BtnKind.SECONDARY, compact = true, enabled = false)
                }
                RhText("Only the Dungeon Master can pay out from the party purse.", style = Theme.small)
            }
        }
        item { SectionHeader("History") }
        if (tx.isEmpty()) item { RhText("No transactions yet.", style = Theme.small, color = Theme.onDarkSoft) }
        items(tx, key = { it.id }) { t -> TxRow(t, chars, campaignId) }
    }
}
