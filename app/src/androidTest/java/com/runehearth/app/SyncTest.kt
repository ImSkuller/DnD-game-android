package com.runehearth.app

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.runehearth.app.data.Backup
import com.runehearth.app.data.CampaignMode
import com.runehearth.app.data.GameCharacter
import com.runehearth.app.data.GameDatabase
import com.runehearth.app.data.Item
import com.runehearth.app.data.ItemKind
import com.runehearth.app.data.Wallet
import com.runehearth.app.engine.Action
import com.runehearth.app.engine.Actor
import com.runehearth.app.net.HostSession
import com.runehearth.app.net.RemoteSession
import com.runehearth.app.session.ConnectionState
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * End-to-end loopback test of the multiplayer stack: a real HostSession (the app database) and RemoteSessions with
 * their own in-memory mirror database talking over 127.0.0.1 TCP.
 */
@RunWith(AndroidJUnit4::class)
class SyncTest {
    private lateinit var app: RunehearthApp
    private lateinit var clientDb: GameDatabase
    private lateinit var host: HostSession
    private lateinit var cid: String
    private lateinit var heroId: String
    private val sessions = mutableListOf<RemoteSession>()
    private val run = UUID.randomUUID().toString().take(8)
    private fun u(s: String) = s + run

    @Before fun setUp() = runBlocking<Unit> {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        app = ctx.applicationContext as RunehearthApp
        clientDb = Room.inMemoryDatabaseBuilder(ctx, GameDatabase::class.java).allowMainThreadQueries().build()
        cid = app.createCampaign("SyncTest ${UUID.randomUUID()}", CampaignMode.COOP)
        heroId = UUID.randomUUID().toString()
        app.engine.execute(cid, Actor.dm("dm"), Action.UpsertCharacter(u("c1"), GameCharacter(heroId, cid, "Hero")))
        host = HostSession(app, cid, "dm")
        assertTrue(host.startHosting())
    }

    @After fun tearDown() {
        sessions.forEach { it.close() }
        host.close()
        clientDb.close()
        runBlocking { Backup(app.db).wipe(cid) }
    }

    private fun connect(device: String, code: String = host.code.value, claim: String? = heroId, newName: String? = null): RemoteSession {
        val port = host.address.value.substringAfterLast(':').toInt()
        return RemoteSession(clientDb, Backup(clientDb), "127.0.0.1", port, code, "Tester", device, claim, newName).also { sessions += it; it.start() }
    }

    private suspend fun <T : Any> waitFor(ms: Long = 5000, block: suspend () -> T?): T {
        val end = System.currentTimeMillis() + ms
        while (System.currentTimeMillis() < end) { block()?.let { return it }; kotlinx.coroutines.delay(20) }
        throw AssertionError("timed out; errors=${sessions.map { it.error.value }} state=${sessions.map { it.connection.value }}")
    }

    @Test fun playerSyncsValidatesAndReceivesIncrementalUpdates() = runBlocking<Unit> {
        val s = connect("dev-1")
        waitFor { s.characterIdFlow.value }
        assertEquals(heroId, s.characterId)
        waitFor { clientDb.dao().character(heroId) }
        assertNotNull(clientDb.dao().wallet(Wallet.personalId(heroId)))

        // DM changes the economy -> mirrored to the player; measure latency
        val t0 = System.nanoTime()
        app.engine.execute(cid, Actor.dm("dm"), Action.AdjustWallet(u("w1"), Wallet.personalId(heroId), 1234, "reward"))
        waitFor { clientDb.dao().wallet(Wallet.personalId(heroId))?.takeIf { it.balance == 1234L } }
        val ms = (System.nanoTime() - t0) / 1_000_000
        Log.i("SyncTest", "host->client sync latency = $ms ms")
        assertTrue("latency $ms", ms < 1000)

        // player actions are validated by the host
        val bad = s.submit(Action.AdjustWallet(u("hack"), Wallet.personalId(heroId), 9999, "cheat"))
        assertFalse(bad.ok)
        assertEquals(1234L, app.db.dao().wallet(Wallet.personalId(heroId))!!.balance)
        val good = s.submit(Action.TransferFunds(u("t1"), Wallet.personalId(heroId), Wallet.partyId(cid), 34, "tip"))
        assertTrue(good.message, good.ok)
        waitFor { clientDb.dao().wallet(Wallet.personalId(heroId))?.takeIf { it.balance == 1200L } }
        // a duplicate action id is idempotent
        assertTrue(s.submit(Action.TransferFunds(u("t1"), Wallet.personalId(heroId), Wallet.partyId(cid), 34, "tip")).ok)
        assertEquals(1200L, app.db.dao().wallet(Wallet.personalId(heroId))!!.balance)
        val t1 = System.nanoTime()
        s.submit(Action.UpdateNotes(u("n1"), heroId, "hello", -1))
        Log.i("SyncTest", "player action round trip = ${(System.nanoTime() - t1) / 1_000_000} ms")
        // hidden wallet disappears from the player's mirror
        app.engine.execute(cid, Actor.dm("dm"), Action.SetWalletHidden(u("h1"), Wallet.personalId(heroId), true))
        waitFor { if (clientDb.dao().wallet(Wallet.personalId(heroId)) == null) true else null }
        Unit
    }

    @Test fun wrongPairingCodeIsDenied() = runBlocking<Unit> {
        val wrong = if (host.code.value == "000000") "111111" else "000000"
        val s = connect("dev-bad", code = wrong)
        val err = waitFor { s.error.value }
        assertTrue(err, err.contains("code", ignoreCase = true))
        assertNull(s.characterId)
        assertEquals(ConnectionState.DISCONNECTED, s.connection.value)
    }

    @Test fun reconnectResumesWithIncrementalDelta() = runBlocking<Unit> {
        val s = connect("dev-r")
        waitFor { s.characterIdFlow.value }
        waitFor { clientDb.dao().campaign(cid)?.lastSeq?.takeIf { it > 0 } }
        host.dropConnection("dev-r")
        waitFor { if (host.players.value.isEmpty()) true else null }
        app.engine.execute(cid, Actor.dm("dm"), Action.UpsertItem(u("it1"), Item("loot", cid, heroId, "Offline Loot", ItemKind.MISC)))
        // the client reconnects on its own and catches up
        waitFor(15000) { clientDb.dao().item("loot") }
        val hostSeq = app.db.dao().campaign(cid)!!.lastSeq
        waitFor { clientDb.dao().campaign(cid)?.lastSeq?.takeIf { it == hostSeq } }
        Unit
    }

    @Test fun digestMismatchTriggersAuthoritativeRepair() = runBlocking<Unit> {
        connect("dev-d")
        waitFor { clientDb.dao().character(heroId) }
        val ch = clientDb.dao().character(heroId)!!
        clientDb.dao().upsert(ch.copy(baseAtk = 999, level = 50)) // simulated tampering on the player's phone
        // the hello after a reconnect triggers a digest check; the host repairs with a full authoritative resend
        host.dropConnection("dev-d")
        waitFor(20000) { clientDb.dao().character(heroId)?.takeIf { it.level == 1 && it.baseAtk != 999 } }
        Unit
    }

    @Test fun newPlayerCanCreateCharacterOnJoin() = runBlocking<Unit> {
        val s = connect("dev-new", claim = null, newName = "Fresh Face")
        val id = waitFor { s.characterIdFlow.value }
        waitFor { clientDb.dao().character(id) }
        assertEquals("Fresh Face", app.db.dao().character(id)!!.name)
    }

    private fun connectDm(device: String, db: GameDatabase, code: String = host.dmCode.value): RemoteSession {
        val port = host.address.value.substringAfterLast(':').toInt()
        return RemoteSession(db, Backup(db), "127.0.0.1", port, code, "The DM", device, asDm = true).also { sessions += it; it.start() }
    }

    @Test fun dmControllerOnASeparateDeviceHasFullControl() = runBlocking<Unit> {
        val npcId = UUID.randomUUID().toString()
        app.engine.execute(cid, Actor.dm("dm"), Action.UpsertCharacter(u("npc"), GameCharacter(npcId, cid, "Secret NPC", isNpc = true)))
        app.engine.execute(cid, Actor.dm("dm"), Action.SetWalletHidden(u("hide"), Wallet.personalId(heroId), true))
        val dmDb = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), GameDatabase::class.java).allowMainThreadQueries().build()
        try {
            val s = connectDm("dm-ctrl", dmDb)
            waitFor { s.ready.value.takeIf { it } }
            assertTrue(s.isDm)
            // the controller sees DM-only data that players never receive
            assertNotNull(dmDb.dao().character(npcId))
            assertNotNull(dmDb.dao().wallet(Wallet.personalId(heroId)))
            assertFalse(dmDb.dao().campaign(cid)!!.isHost)
            // its actions run with DM rights on the host and flow back into its mirror
            val t0 = System.nanoTime()
            val r = s.submit(Action.AdjustWallet(u("adj"), Wallet.partyId(cid), 700, "dm reward"))
            assertTrue(r.message, r.ok)
            assertEquals(700L, app.db.dao().wallet(Wallet.partyId(cid))!!.balance)
            waitFor { dmDb.dao().wallet(Wallet.partyId(cid))?.takeIf { it.balance == 700L } }
            Log.i("SyncTest", "DM controller action + mirror update = ${(System.nanoTime() - t0) / 1_000_000} ms")
            // a player editing their own stats notifies the DM controller (events travel DM-only)
            val hero = app.db.dao().character(heroId)!!
            app.engine.execute(cid, Actor(false, heroId, "player-dev"), Action.UpsertCharacter(u("edit"), hero.copy(baseLuck = 9), hero.rev))
            waitFor { dmDb.dao().recentEvents(cid).firstOrNull { it.type == "notify" && it.text.contains("Luck 5 → 9") } }
            // a player cannot become DM with the player code
            val bad = connectDm("dm-evil", dmDb, code = host.code.value)
            assertTrue(waitFor { bad.error.value }.contains("code", ignoreCase = true))
        } finally { dmDb.close() }
        Unit
    }

    @Test fun hostMigrationValidatesBumpsEpochAndKeepsAcknowledgedActions() = runBlocking<Unit> {
        val actionId = u("pay")
        assertTrue(app.engine.execute(cid, Actor.dm("dm"), Action.AdjustWallet(actionId, Wallet.partyId(cid), 1000, "x")).ok)
        val backup = Backup(app.db)
        val text = backup.export(cid)
        val epochBefore = app.db.dao().campaign(cid)!!.epoch
        // the old host dies; a new device imports the validated package and becomes the host
        host.stopHosting()
        backup.wipe(cid)
        backup.import(text, takeOverHostFor = "new-host-device")
        val camp = app.db.dao().campaign(cid)!!
        assertEquals(epochBefore + 1, camp.epoch); assertTrue(camp.isHost); assertEquals("new-host-device", camp.hostDeviceId)
        // a player re-sending an action the old host had already acknowledged must not pay twice
        val again = app.engine.execute(cid, Actor.dm("dm"), Action.AdjustWallet(actionId, Wallet.partyId(cid), 1000, "x"))
        assertTrue(again.ok)
        assertEquals(1000L, app.db.dao().wallet(Wallet.partyId(cid))!!.balance)
        // a tampered package is refused and existing data is untouched
        val forged = text.replace("\"balance\":1000", "\"balance\":-5")
        try { backup.import(forged); throw AssertionError("forged package accepted") } catch (e: com.runehearth.app.data.ImportException) { /* expected */ }
        assertEquals(1000L, app.db.dao().wallet(Wallet.partyId(cid))!!.balance)
    }
}
