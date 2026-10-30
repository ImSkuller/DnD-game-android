package com.runehearth.app.ui.assets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runehearth.app.art.ArtCategory
import com.runehearth.app.ui.components.ScreenTopBar

/** Browse every design in every colour, with and without glow. */
@Composable
fun AssetLibraryScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("Asset Library", onBack)
        AssetPickerPanel(null, ArtCategory.entries, Modifier.fillMaxSize().padding(12.dp))
    }
}
