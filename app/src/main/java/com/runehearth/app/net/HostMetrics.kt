package com.runehearth.app.net

import android.os.Debug
import android.os.Process
import com.runehearth.app.ui.theme.DeviceProfile

/** A sample of how hard the hosting device is working. */
data class HostMetrics(
    val memUsedMb: Int = 0,
    val memMaxMb: Int = 0,
    val nativeMb: Int = 0,
    /** CPU time of this process as a percentage of one core over the last interval. */
    val cpuPercent: Int = 0,
    /** Average / worst time to validate and commit one action (database latency). */
    val dbAvgMs: Double = 0.0,
    val dbMaxMs: Double = 0.0,
    val actions: Int = 0,
    /** Time to push one update to every connected player. */
    val syncMs: Double = 0.0,
    val clients: Int = 0,
    /** Worst round-trip time reported by a client, -1 if unknown. */
    val worstRttMs: Long = -1,
)

enum class Advice { MEMORY, CPU, DATABASE, NETWORK, SYNC, LOW_END_MANY_PLAYERS }

/** Pure rules that turn metrics into recommendations (unit tested). */
object HostAdvisor {
    fun advise(m: HostMetrics, device: DeviceProfile): List<Advice> = buildList {
        if (m.memMaxMb > 0 && m.memUsedMb.toFloat() / m.memMaxMb > 0.8f) add(Advice.MEMORY)
        if (m.cpuPercent > 85) add(Advice.CPU)
        if (m.actions >= 8 && m.dbAvgMs > 60.0) add(Advice.DATABASE)
        if (m.worstRttMs > 250) add(Advice.NETWORK)
        if (m.syncMs > 150.0) add(Advice.SYNC)
        if ((device.lowRam || device.memoryClassMb <= 128) && m.clients >= 5) add(Advice.LOW_END_MANY_PLAYERS)
    }

    fun text(a: Advice): String = when (a) {
        Advice.MEMORY -> "Memory is running low. Close other apps, or move hosting to a device with more RAM."
        Advice.CPU -> "The processor is saturated. Consider hosting from a faster phone."
        Advice.DATABASE -> "Saving actions is slow on this device. A faster host (or free storage space) will help."
        Advice.NETWORK -> "A player has a slow connection. Move closer to the Wi-Fi or use this phone's hotspot."
        Advice.SYNC -> "Updates reach players slowly. Reduce the number of connected devices or use a stronger host."
        Advice.LOW_END_MANY_PLAYERS -> "This is a low-memory phone hosting several players. A more capable host device is recommended."
    }
}

/** Samples process-level resource usage. Cheap: a few reads every couple of seconds while hosting. */
class MetricsSampler {
    private var lastCpu = Process.getElapsedCpuTime()
    private var lastWall = System.nanoTime()

    fun sample(dbAvg: Double, dbMax: Double, actions: Int, syncMs: Double, clients: Int, worstRtt: Long): HostMetrics {
        val rt = Runtime.getRuntime()
        val cpu = Process.getElapsedCpuTime()
        val wall = System.nanoTime()
        val pct = ((cpu - lastCpu) * 1_000_000.0 / (wall - lastWall).coerceAtLeast(1) * 100).toInt().coerceIn(0, 800)
        lastCpu = cpu; lastWall = wall
        return HostMetrics(
            ((rt.totalMemory() - rt.freeMemory()) / 1_048_576).toInt(), (rt.maxMemory() / 1_048_576).toInt(),
            (Debug.getNativeHeapAllocatedSize() / 1_048_576).toInt(), pct, dbAvg, dbMax, actions, syncMs, clients, worstRtt,
        )
    }
}
