package com.runehearth.app.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runehearth.app.core.Coin
import com.runehearth.app.core.Money
import com.runehearth.app.ui.theme.Theme

/** Four coin denominations with tweened counts, the breakdown of a bronze-denominated balance. */
@Composable
fun CoinRow(balance: Long, modifier: Modifier = Modifier, coinSize: Dp = 34.dp) {
    val b = Money.breakdown(balance)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        for (coin in listOf(Coin.HOLY_GOLD, Coin.GOLD, Coin.SILVER, Coin.BRONZE)) {
            val target = b.amountOf(coin).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            val shown by animateIntAsState(target, tween(550), label = "coin")
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CoinImage(coin, coinSize)
                RhText(shown.toString(), style = Theme.heading.copy(fontSize = 16.sp), textAlign = TextAlign.Center)
                RhText(coin.label, style = Theme.small.copy(fontSize = 9.5.sp), textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}

@Composable
fun BalanceLine(label: String, balance: Long, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        CoinImage(Coin.GOLD, 22.dp)
        Spacer(Modifier.width(8.dp))
        RhText(label, Modifier.weight(1f), style = Theme.small)
        RhText(Money.format(balance), style = Theme.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
    }
}
