package com.runehearth.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.runehearth.app.core.Coin
import com.runehearth.app.data.GameDao
import com.runehearth.app.engine.Action
import com.runehearth.app.engine.ActionResult
import com.runehearth.app.session.GameSession
import com.runehearth.app.ui.components.LocalToaster
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
fun rememberDao(): GameDao {
    val app = LocalApp.current
    return remember(app) { app.db.dao() }
}

/** Collects a DAO flow created once per [key]. */
@Composable
fun <T> rememberFlow(key: Any?, initial: T, make: () -> Flow<T>): State<T> {
    val flow = remember(key) { make() }
    return flow.collectAsState(initial)
}

/** Returns a function that submits an action to the session and shows a toast if it was rejected. */
@Composable
fun rememberDispatch(onResult: ((ActionResult) -> Unit)? = null): (Action) -> Unit {
    val session: GameSession = LocalSession.current
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    return remember(session) {
        { a: Action ->
            scope.launch {
                val r = session.submit(a)
                if (!r.ok) toaster.show(r.message.ifEmpty { "Action rejected" }, true)
                onResult?.invoke(r)
            }
            Unit
        }
    }
}

fun Coin.shortLabel(): String = label
