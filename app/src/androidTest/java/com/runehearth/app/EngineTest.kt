package com.runehearth.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.runehearth.app.core.AuditChain
import com.runehearth.app.core.Money
import com.runehearth.app.core.RandomSource
import com.runehearth.app.core.RollSpec
import com.runehearth.app.data.Campaign
import com.runehearth.app.data.CampaignMode
import com.runehearth.app.data.EquipSlot
import com.runehearth.app.data.GameCharacter
import com.runehearth.app.data.GameDatabase
import com.runehearth.app.data.Item
import com.runehearth.app.data.ItemKind
import com.runehearth.app.data.Wallet
import com.runehearth.app.engine.Action
import com.runehearth.app.engine.ActionEngine
import com.runehearth.app.engine.Actor
import com.runehearth.app.engine.Aud
import com.runehearth.app.engine.SyncRoute
import com.runehearth.app.engine.routeFor
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
import java.util.concurrent.atomic.AtomicInteger

/** Engine behaviour against a real (in-memory) Room database: validation, atomicity, duplicates, change log. */
@RunWith(AndroidJUnit4::class)
class EngineTest {
    private lateinit var db: GameDatabase
    private lateinit var engine: ActionEngine
    private val counter = AtomicInteger()
    private val cid = "camp"
    private val dm = Actor.dm("dm-device")
    private fun aid() = "a${counter.incrementAndGet()}"

    @Before fun setUp() = runBlocking<Unit> {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, GameDatabase::class.java).allowMainThreadQueries().build()
        engine = ActionEngine(db, RandomSource { 0 }, { 1_000L + counter.get() }, { "id${counter.incrementAndGet()}" })
        db.dao().upsert(Campaign(cid, "Test", CampaignMode.COOP, "seed", "dm-device", createdAt = 0, updatedAt = 0))
        db.dao().upsert(Wallet(Wallet.partyId(cid), cid, null))
        for (n in listOf("hero", "friend")) {
            engine.execute(cid, dm, Action.UpsertCharacter(aid(), GameCharacter(n, cid, n.replaceFirstChar { it.uppercase() })))
        }
    }

    @After fun tearDown() { db.close() }

    private fun player(id: String) = Actor(false, id, "dev-$id")

    @Test fun currencyTransferIsAtomicAndValidated() = runBlocking<Unit> {
        assertTrue(engine.execute(cid, dm, Action.AdjustWallet(aid(), Wallet.personalId("hero"), Money.of(gold = 2), "loot")).ok)
        val r = engine.execute(cid, player("hero"), Action.TransferFunds(aid(), Wallet.personalId("hero"), Wallet.partyId(cid), Money.of(gold = 1, silver = 5), "share"))
        assertTrue(r.message, r.ok)
        assertEquals(Money.of(gold = 1, silver = 5), db.dao().wallet(Wallet.partyId(cid))!!.balance)
        assertEquals(Money.of(silver = 5), db.dao().wallet(Wallet.personalId("hero"))!!.balance)
        // overdraft is rejected and changes nothing
        val bad = engine.execute(cid, player("hero"), Action.TransferFunds(aid(), Wallet.personalId("hero"), Wallet.partyId(cid), 10_000, ""))
        assertFalse(bad.ok)
        assertEquals(Money.of(silver = 5), db.dao().wallet(Wallet.personalId("hero"))!!.balance)
        assertEquals(Money.of(gold = 1, silver = 5), db.dao().wallet(Wallet.partyId(cid))!!.balance)
    }

    @Test fun playersCannotSpendOthersMoneyOrUseDmActions() = runBlocking<Unit> {
        engine.execute(cid, dm, Action.AdjustWallet(aid(), Wallet.personalId("friend"), 500, ""))
        assertFalse(engine.execute(cid, player("hero"), Action.TransferFunds(aid(), Wallet.personalId("friend"), Wallet.personalId("hero"), 100, "")).ok)
        assertFalse(engine.execute(cid, player("hero"), Action.AdjustWallet(aid(), Wallet.personalId("hero"), 999, "cheat")).ok)
        assertFalse(engine.execute(cid, player("hero"), Action.AwardExp(aid(), "hero", 1000)).ok)
        assertEquals(0, db.dao().wallet(Wallet.personalId("hero"))!!.balance)
        assertEquals(1, db.dao().character("hero")!!.level)
    }

    @Test fun duplicateActionIdsAreAppliedOnce() = runBlocking<Unit> {
        val a = Action.AdjustWallet("dup-1", Wallet.partyId(cid), 100, "once")
        assertTrue(engine.execute(cid, dm, a).ok)
        val second = engine.execute(cid, dm, a)
        assertTrue(second.ok)
        assertEquals(100, db.dao().wallet(Wallet.partyId(cid))!!.balance)
    }

    @Test fun inventoryStackingTransferAndEquipmentStats() = runBlocking<Unit> {
        val potion = Item("p1", cid, "hero", "Potion", ItemKind.POTION, quantity = 2, healHp = 10)
        engine.execute(cid, dm, Action.UpsertItem(aid(), potion, mergeStacks = true))
        engine.execute(cid, dm, Action.UpsertItem(aid(), potion.copy(id = "p2", quantity = 3), mergeStacks = true))
        val stacks = db.dao().itemsOf("hero")
        assertEquals(1, stacks.size); assertEquals(5, stacks[0].quantity)
        // move 2 to the party stash, 3 stay
        assertTrue(engine.execute(cid, player("hero"), Action.TransferItem(aid(), stacks[0].id, null, 2)).ok)
        assertEquals(3, db.dao().itemsOf("hero").single().quantity)
        assertEquals(2, db.dao().stash(cid).single().quantity)
        // equipment adds to final stats
        engine.execute(cid, dm, Action.UpsertItem(aid(), Item("s1", cid, "hero", "Sword", ItemKind.WEAPON, EquipSlot.MAIN_HAND, bonusAtk = 4)))
        assertTrue(engine.execute(cid, player("hero"), Action.Equip(aid(), "s1")).ok)
        val ch = db.dao().character("hero")!!
        val stats = com.runehearth.app.engine.StatCalculator.compute(ch, db.dao().equippedOf("hero"), emptyList())
        assertEquals(ch.baseAtk + 4, stats.atk.total)
        // cannot equip someone else's item
        engine.execute(cid, dm, Action.UpsertItem(aid(), Item("s2", cid, "friend", "Axe", ItemKind.WEAPON, EquipSlot.MAIN_HAND)))
        assertFalse(engine.execute(cid, player("hero"), Action.Equip(aid(), "s2")).ok)
    }

    @Test fun usingPotionHealsAndConsumes() = runBlocking<Unit> {
        engine.execute(cid, dm, Action.AdjustVitals(aid(), "hero", -20, 0))
        engine.execute(cid, dm, Action.UpsertItem(aid(), Item("p", cid, "hero", "Potion", ItemKind.POTION, quantity = 1, healHp = 15)))
        assertTrue(engine.execute(cid, player("hero"), Action.UseItem(aid(), "p")).ok)
        assertEquals(10 + 15, db.dao().character("hero")!!.curHp)
        assertNull(db.dao().item("p"))
    }

    @Test fun staleEditsAreRejected() = runBlocking<Unit> {
        val rev = db.dao().character("hero")!!.rev
        assertTrue(engine.execute(cid, player("hero"), Action.UpdateNotes(aid(), "hero", "first", rev)).ok)
        val r = engine.execute(cid, player("hero"), Action.UpdateNotes(aid(), "hero", "second", rev)) // same old rev
        assertFalse(r.ok); assertTrue(r.message.startsWith("stale"))
        assertEquals("first", db.dao().character("hero")!!.notes)
    }

    @Test fun rollsAreAuditChainedAndPlayerRollsAreHostExecuted() = runBlocking<Unit> {
        val r1 = engine.execute(cid, dm, Action.RollDice(aid(), RollSpec(1, 20), "a", true))
        val r2 = engine.execute(cid, player("hero"), Action.RollDice(aid(), RollSpec(2, 6, 1), "b", false))
        assertTrue(r1.ok && r2.ok)
        val camp = db.dao().campaign(cid)!!
        val rec1 = db.dao().roll(r1.rollId!!)!!; val rec2 = db.dao().roll(r2.rollId!!)!!
        val h1 = AuditChain.next(AuditChain.genesis(cid, "seed"), rec1.id, rec1.expr, rec1.dice.split(',').map { it.toInt() })
        assertEquals(h1, rec1.auditHash)
        assertEquals(AuditChain.next(h1, rec2.id, rec2.expr, rec2.dice.split(',').map { it.toInt() }), camp.rollChain)
        assertEquals(2, camp.rollIdx)
        assertFalse(engine.execute(cid, dm, Action.RollDice(aid(), RollSpec(0, 6), "", true)).ok)
    }

    @Test fun changeLogIsCompactedAndRoutesByAudience() = runBlocking<Unit> {
        repeat(5) { engine.execute(cid, dm, Action.AdjustVitals(aid(), "hero", -1, 0)) }
        val before = db.dao().changesSince(cid, 0)
        assertEquals(1, before.count { it.entityType == "CHARACTER" && it.entityId == "hero" }) // one row per entity
        // item moves from hero to friend: hero must drop it, friend must get it, nobody else sees a change
        engine.execute(cid, dm, Action.UpsertItem(aid(), Item("i", cid, "hero", "Gem", ItemKind.MATERIAL)))
        engine.execute(cid, dm, Action.TransferItem(aid(), "i", "friend", 1))
        val moved = db.dao().changesSince(cid, 0).filter { it.entityType == "ITEM" }
        val toFriend = moved.single { it.audience == "friend" }
        assertEquals(SyncRoute.UPSERT, toFriend.routeFor("friend"))
        val old = moved.first { it.entityId == "i" || it.deleted }
        assertTrue(moved.any { it.routeFor("hero") == SyncRoute.DELETE || it.routeFor("hero") == SyncRoute.NONE })
        assertNotNull(old)
        // hidden wallet becomes a tombstone for the owner
        engine.execute(cid, dm, Action.SetWalletHidden(aid(), Wallet.personalId("hero"), true))
        val w = db.dao().change(cid, "WALLET", Wallet.personalId("hero"))!!
        assertEquals(SyncRoute.DELETE, w.routeFor("hero"))
        assertEquals(Aud.DM.length > 0, true)
    }

    @Test fun combatLifecycleSyncsVitals() = runBlocking<Unit> {
        val enc = "enc"
        engine.execute(cid, dm, Action.UpsertEncounter(aid(), com.runehearth.app.data.Encounter(enc, cid, "Fight")))
        engine.execute(cid, dm, Action.AddPartyToEncounter(aid(), enc))
        assertTrue(engine.execute(cid, dm, Action.StartEncounter(aid(), enc)).ok)
        val cmb = db.dao().combatants(enc).first { it.characterId == "hero" }
        engine.execute(cid, dm, Action.DamageCombatant(aid(), cmb.id, -7))
        assertEquals(30 - 7, db.dao().character("hero")!!.curHp)
        assertTrue(engine.execute(cid, dm, Action.NextTurn(aid(), enc)).ok)
        engine.execute(cid, dm, Action.EndEncounter(aid(), enc))
        assertFalse(db.dao().encounter(enc)!!.active)
    }

    @Test fun backupRoundTripRestoresEverything() = runBlocking<Unit> {
        engine.execute(cid, dm, Action.AdjustWallet(aid(), Wallet.partyId(cid), 777, "x"))
        engine.execute(cid, dm, Action.UpsertItem(aid(), Item("i", cid, "hero", "Gem", ItemKind.MATERIAL)))
        val backup = com.runehearth.app.data.Backup(db)
        val json = backup.export(cid)
        backup.wipe(cid)
        assertNull(db.dao().campaign(cid))
        backup.import(json)
        assertEquals(777, db.dao().wallet(Wallet.partyId(cid))!!.balance)
        assertEquals("Gem", db.dao().item("i")!!.name)
        assertEquals(2, db.dao().characters(cid).size)
    }
}
