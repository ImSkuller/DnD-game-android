package com.runehearth.app.ui.theme

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.compositionLocalOf

/** User choice in Settings. AUTO decides from the hardware. */
enum class PerfPreference { AUTO, FULL, LITE }

/** Effective rendering level. LITE never disables gameplay features, only decoration. */
enum class PerfLevel { FULL, LITE }

data class DeviceProfile(val lowRam: Boolean, val memoryClassMb: Int, val cores: Int, val sdk: Int) {
    companion object {
        fun detect(context: Context): DeviceProfile {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            return DeviceProfile(am.isLowRamDevice, am.memoryClass, Runtime.getRuntime().availableProcessors(), Build.VERSION.SDK_INT)
        }
    }
}

/** Pure decision logic (unit tested). */
object PerfPolicy {
    fun resolve(pref: PerfPreference, d: DeviceProfile): PerfLevel = when (pref) {
        PerfPreference.FULL -> PerfLevel.FULL
        PerfPreference.LITE -> PerfLevel.LITE
        PerfPreference.AUTO -> if (d.lowRam || d.memoryClassMb <= 128 || (d.cores <= 4 && d.sdk <= 28) || d.sdk <= 26) PerfLevel.LITE else PerfLevel.FULL
    }

    fun settings(level: PerfLevel) = if (level == PerfLevel.LITE) PerfSettings.LITE else PerfSettings.FULL
}

/** What decorative work the UI may do. */
data class PerfSettings(
    val level: PerfLevel,
    val embers: Boolean,
    val animatedGlow: Boolean,
    val transitionMs: Int,
    val diceSpinMs: Int,
    val textureSizePx: Int,
) {
    companion object {
        val FULL = PerfSettings(PerfLevel.FULL, embers = true, animatedGlow = true, transitionMs = 260, diceSpinMs = 950, textureSizePx = 192)
        val LITE = PerfSettings(PerfLevel.LITE, embers = false, animatedGlow = false, transitionMs = 90, diceSpinMs = 450, textureSizePx = 96)
    }
}

val LocalPerf = compositionLocalOf { PerfSettings.FULL }
