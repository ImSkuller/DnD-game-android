package com.runehearth.app

import com.runehearth.app.core.AuditChain
import com.runehearth.app.data.Campaign
import com.runehearth.app.data.CampaignBundle
import com.runehearth.app.data.CampaignMode
import com.runehearth.app.data.BundleValidator
import com.runehearth.app.data.GameCharacter
import com.runehearth.app.data.Item
import com.runehearth.app.data.RollRecord
import com.runehearth.app.data.Severity
import com.runehearth.app.data.Wallet
import com.runehearth.app.engine.Action
import com.runehearth.app.engine.ActionResult
import com.runehearth.app.net.Advice
import com.runehearth.app.net.Frames
import com.runehearth.app.net.HostAdvisor
import com.runehearth.app.net.HostMetrics
import com.runehearth.app.net.Msg
import com.runehearth.app.net.Pairing
import com.runehearth.app.net.WireChange
import com.runehearth.app.net.WireJson
import com.runehearth.app.ui.theme.DeviceProfile
import com.runehearth.app.ui.theme.PerfLevel
import com.runehearth.app.ui.theme.PerfPolicy
import com.runehearth.app.ui.theme.PerfPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class WireTest {
    private val key = Pairing.sessionKey("123456", "hostNonce", "clientNonce")

    private fun frame(payload: ByteArray, k: ByteArray?, counter: Long): ByteArray {
        val bos = ByteArrayOutputStream()
        Frames.write(DataOutputStream(bos), payload, k, counter)
        return bos.toByteArray()
    }

    @Test fun framesRoundTripAndAuthenticate() {
        val bytes = frame("hello".toByteArray(), key, 0)
        val f = Frames.read(DataInputStream(ByteArrayInputStream(bytes)), key, 0)
        assertEquals("hello", String(f.payload)); assertTrue(f.authenticated)
    }

    @Test fun tamperedFramesAndReplaysAreRejected() {
        val bytes = frame("pay 100 gold".toByteArray(), key, 5)
        val tampered = bytes.copyOf().also { it[7] = (it[7] + 1).toByte() }
        try { Frames.read(DataInputStream(ByteArrayInputStream(tampered)), key, 5); fail("tamper accepted") } catch (_: IOException) {}
        try { Frames.read(DataInputStream(ByteArrayInputStream(bytes)), key, 6); fail("replay accepted") } catch (_: IOException) {}
        val wrongKey = Pairing.sessionKey("654321", "hostNonce", "clientNonce")
        try { Frames.read(DataInputStream(ByteArrayInputStream(bytes)), wrongKey, 5); fail("wrong key accepted") } catch (_: IOException) {}
    }

    @Test fun oversizedFramesAreRejected() {
        val bos = ByteArrayOutputStream(); val o = DataOutputStream(bos); o.writeByte(0); o.writeInt(Frames.MAX_FRAME + 1)
        try { Frames.read(DataInputStream(ByteArrayInputStream(bos.toByteArray())), null, 0); fail() } catch (_: IOException) {}
    }

    @Test fun pairingProofDependsOnTheCodeAndNeverContainsIt() {
        val a = Pairing.proof("123456", "h", "c", "dev")
        assertEquals(a, Pairing.proof("123456", "h", "c", "dev"))
        assertFalse(a == Pairing.proof("123457", "h", "c", "dev"))
        assertFalse(a.contains("123456"))
    }

    @Test fun messagesSerialiseIncludingNestedActions() {
        val msgs = listOf<Msg>(
            Msg.Auth("dev", "Ann", "n", "p", 5, 1, claimId = "c", role = "dm"),
            Msg.Act(Action.TransferFunds("a1", "pc:x", "party:y", 150, "tip")),
            Msg.Act(Action.RollDice("a2", com.runehearth.app.core.RollSpec(2, 20, 3))),
            Msg.Delta(listOf(WireChange("ITEM", "i", 9, "{}")), 9, full = true),
            Msg.ActResult(ActionResult("a1", true, "ok", seq = 3)),
            Msg.Ping(1, 20), Msg.DmDelta(listOf(WireChange("CHARACTER", "c", 4, delete = true)), 4),
        )
        for (m in msgs) assertEquals(m, WireJson.decode(WireJson.encode(m)))
    }
}

class SafetyTest {
    private fun bundle(block: (CampaignBundle) -> CampaignBundle = { it }): CampaignBundle {
        val camp = Campaign("c", "Camp", CampaignMode.COOP, "seed", "dev", createdAt = 0, updatedAt = 0)
        val hero = GameCharacter("h", "c", "Hero")
        return block(CampaignBundle(campaign = camp, characters = listOf(hero), wallets = listOf(Wallet("party:c", "c", null, 10), Wallet("pc:h", "c", "h", 5))))
    }

    @Test fun validBundlePasses() {
        assertTrue(BundleValidator.validate(bundle()).none { it.severity == Severity.ERROR })
    }

    @Test fun corruptedStateIsRefused() {
        fun errors(b: CampaignBundle) = BundleValidator.validate(b).filter { it.severity == Severity.ERROR }
        assertTrue(errors(bundle { it.copy(wallets = it.wallets + Wallet("pc:h", "c", "h", -1)) }).isNotEmpty())
        assertTrue(errors(bundle { it.copy(items = listOf(Item("i", "c", "ghost", "Orphan"))) }).isNotEmpty())
        assertTrue(errors(bundle { it.copy(characters = listOf(GameCharacter("h", "c", "Hero", level = 400))) }).isNotEmpty())
        assertTrue(errors(bundle { it.copy(format = 99) }).isNotEmpty())
    }

    @Test fun rollHistoryTamperingIsDetected() {
        var prev = AuditChain.genesis("c", "seed")
        val rolls = (1..3).map { i ->
            val h = AuditChain.next(prev, "r$i", "1d20", listOf(i)); prev = h
            RollRecord("r$i", "c", i, 0, "DM", "", "1d20", "$i", 0, i, "LOW", "NONE", true, h)
        }
        val ok = bundle { it.copy(campaign = it.campaign.copy(rollChain = prev, rollIdx = 3), rolls = rolls) }
        assertTrue(BundleValidator.validate(ok).none { it.severity == Severity.ERROR })
        val forged = ok.copy(rolls = rolls.map { if (it.idx == 2) it.copy(dice = "20", total = 20) else it })
        assertTrue(BundleValidator.validate(forged).any { it.severity == Severity.ERROR && it.message.contains("audit chain") })
    }
}

class PerfAndAdviceTest {
    private val modern = DeviceProfile(lowRam = false, memoryClassMb = 256, cores = 8, sdk = 35)
    private val lowEnd = DeviceProfile(lowRam = true, memoryClassMb = 96, cores = 4, sdk = 27)

    @Test fun autoModePicksLiteOnLowEndHardware() {
        assertEquals(PerfLevel.FULL, PerfPolicy.resolve(PerfPreference.AUTO, modern))
        assertEquals(PerfLevel.LITE, PerfPolicy.resolve(PerfPreference.AUTO, lowEnd))
        assertEquals(PerfLevel.LITE, PerfPolicy.resolve(PerfPreference.AUTO, modern.copy(memoryClassMb = 128)))
        assertEquals(PerfLevel.FULL, PerfPolicy.resolve(PerfPreference.FULL, lowEnd))
        assertEquals(PerfLevel.LITE, PerfPolicy.resolve(PerfPreference.LITE, modern))
    }

    @Test fun liteNeverTouchesGameplayOnlyDecoration() {
        val lite = PerfPolicy.settings(PerfLevel.LITE); val full = PerfPolicy.settings(PerfLevel.FULL)
        assertFalse(lite.embers); assertFalse(lite.animatedGlow)
        assertTrue(lite.textureSizePx < full.textureSizePx); assertTrue(lite.transitionMs < full.transitionMs)
    }

    @Test fun hostAdvisorRecommendsAStrongerHostWhenStrained() {
        assertTrue(HostAdvisor.advise(HostMetrics(memUsedMb = 20, memMaxMb = 256, cpuPercent = 10, dbAvgMs = 5.0, actions = 20, clients = 3), modern).isEmpty())
        val strained = HostMetrics(memUsedMb = 230, memMaxMb = 256, cpuPercent = 95, dbAvgMs = 90.0, actions = 30, syncMs = 400.0, clients = 6, worstRttMs = 600)
        val advice = HostAdvisor.advise(strained, lowEnd)
        assertTrue(advice.containsAll(listOf(Advice.MEMORY, Advice.CPU, Advice.DATABASE, Advice.SYNC, Advice.NETWORK, Advice.LOW_END_MANY_PLAYERS)))
    }
}
