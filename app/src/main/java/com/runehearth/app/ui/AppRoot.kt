package com.runehearth.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.runehearth.app.ui.theme.LocalPerf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runehearth.app.RunehearthApp
import com.runehearth.app.ui.assets.AssetLibraryScreen
import com.runehearth.app.ui.components.Backdrop
import com.runehearth.app.ui.components.Toaster
import com.runehearth.app.ui.components.ToastHost
import com.runehearth.app.ui.components.LocalToaster
import com.runehearth.app.ui.dm.DmShell
import com.runehearth.app.ui.home.HomeScreen
import com.runehearth.app.ui.join.JoinScreen
import com.runehearth.app.ui.player.PlayerShell
import com.runehearth.app.ui.settings.SettingsScreen
import com.runehearth.app.ui.theme.RunehearthTheme

@Composable
fun AppRoot() {
    val app = RunehearthApp.of(LocalContext.current)
    val vm: AppViewModel = viewModel()
    val toaster = remember { Toaster() }
    val perf by app.perf.collectAsState()
    CompositionLocalProvider(LocalPerf provides perf) {
    RunehearthTheme {
        CompositionLocalProvider(LocalApp provides app, LocalToaster provides toaster) {
            BackHandler(enabled = vm.stack.size > 1) { vm.pop() }
            Backdrop(embers = vm.current is Route.Home && perf.embers) {
                Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                    AnimatedContent(
                        targetState = vm.current,
                        transitionSpec = {
                            val forward = vm.stack.size >= 1 && targetState != Route.Home
                            if (forward) (slideInHorizontally(tween(perf.transitionMs)) { it / 4 } + fadeIn(tween(perf.transitionMs))) togetherWith (fadeOut(tween(perf.transitionMs / 2)))
                            else fadeIn(tween(perf.transitionMs)) togetherWith (slideOutHorizontally(tween(perf.transitionMs)) { it / 4 } + fadeOut(tween(perf.transitionMs)))
                        },
                        label = "nav",
                    ) { route ->
                        when (route) {
                            Route.Home -> HomeScreen(
                                onOpen = { vm.openCampaignAsDm(it.id) },
                                onHostOnly = { vm.openHostConsole(it.id) },
                                onJoin = { vm.push(Route.Join) },
                                onSettings = { vm.push(Route.Settings) },
                                onAssets = { vm.push(Route.Assets) },
                            )
                            Route.Join -> JoinScreen(onBack = { vm.pop() }, onJoined = { s -> vm.adoptSession(s, if (s.isDm) Route.Dm(s.campaignId) else Route.Player(s.campaignId)) })
                            Route.Settings -> SettingsScreen(onBack = { vm.pop() }, onAssets = { vm.push(Route.Assets) })
                            Route.Assets -> AssetLibraryScreen(onBack = { vm.pop() })
                            is Route.Dm -> vm.session?.let { s -> CompositionLocalProvider(LocalSession provides s) { DmShell(route.campaignId, onExit = { vm.pop() }) } }
                            is Route.HostConsole -> (vm.session as? com.runehearth.app.net.HostSession)?.let { h -> CompositionLocalProvider(LocalSession provides h) { com.runehearth.app.ui.dm.HostConsole(h, onExit = { vm.pop() }) } }
                            is Route.Player -> vm.session?.let { s -> CompositionLocalProvider(LocalSession provides s) { PlayerShell(route.campaignId, onExit = { vm.pop() }) } }
                        }
                    }
                    ToastHost(toaster)
                }
            }
        }
    }
    }
}
