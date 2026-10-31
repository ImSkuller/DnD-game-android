package com.runehearth.app

import android.app.Application
import com.runehearth.app.art.AssetCatalog
import com.runehearth.app.data.Backup
import com.runehearth.app.ui.theme.DeviceProfile
import com.runehearth.app.ui.theme.PerfPolicy
import com.runehearth.app.ui.theme.PerfPreference
import com.runehearth.app.ui.theme.PerfSettings
import com.runehearth.app.ui.theme.Textures
import kotlinx.coroutines.flow.MutableStateFlow
import com.runehearth.app.data.Campaign
import com.runehearth.app.data.CampaignMode
import com.runehearth.app.data.GameDatabase
import com.runehearth.app.data.Prefs
import com.runehearth.app.data.Wallet
import com.runehearth.app.engine.ActionEngine
import java.io.File
import java.security.SecureRandom
import java.util.UUID

/** Manual DI container. Everything heavy is lazy so cold start only pays for what the first screen needs. */
class RunehearthApp : Application() {
    val db: GameDatabase by lazy { GameDatabase.build(this) }
    val engine: ActionEngine by lazy { ActionEngine(db) }
    val prefs: Prefs by lazy { Prefs(this) }
    val backup: Backup by lazy { Backup(db) }
    val snapshotDir: File by lazy { File(filesDir, "snapshots") }

    // ---- adaptive performance (stored in SharedPreferences so it can be read synchronously before the first frame)
    val deviceProfile: DeviceProfile by lazy { DeviceProfile.detect(this) }
    private val boot by lazy { getSharedPreferences("boot", MODE_PRIVATE) }
    val perf: MutableStateFlow<PerfSettings> by lazy { MutableStateFlow(PerfPolicy.settings(PerfPolicy.resolve(perfPreference, deviceProfile))) }

    var perfPreference: PerfPreference
        get() = runCatching { PerfPreference.valueOf(boot.getString("perf", "AUTO")!!) }.getOrDefault(PerfPreference.AUTO)
        set(v) {
            boot.edit().putString("perf", v.name).apply()
            perf.value = PerfPolicy.settings(PerfPolicy.resolve(v, deviceProfile))
        }

    /** Highest player-edit notification the DM has already seen, per campaign. */
    fun notifySeen(campaignId: String): Long = boot.getLong("seen_$campaignId", 0L)
    fun setNotifySeen(campaignId: String, seq: Long) { boot.edit().putLong("seen_$campaignId", seq).apply() }

    override fun onCreate() {
        super.onCreate()
        Textures.SIZE = perf.value.textureSizePx
    }

    /** Drop rebuildable caches when the system is short of memory. */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_LOW) AssetCatalog.clearCache()
    }

    /** Creates a campaign (+ party wallet) and seeds its change log. Returns the new campaign id. */
    suspend fun createCampaign(name: String, mode: CampaignMode): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val seed = ByteArray(16).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
        db.dao().upsert(Campaign(id, name.trim().take(60).ifEmpty { "Unnamed Campaign" }, mode, seed, prefs.deviceId(), createdAt = now, updatedAt = now))
        db.dao().upsert(Wallet(Wallet.partyId(id), id, null))
        engine.rebuildChangeLog(id)
        return id
    }

    companion object {
        fun of(context: android.content.Context): RunehearthApp = context.applicationContext as RunehearthApp
    }
}
