package com.remotecontrol.kiosk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AdminMenuScreen(
    onEditSettings: () -> Unit,
    onExitKiosk: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Admin menu", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        Button(onClick = onEditSettings) {
            Text("Edit URL & PIN")
        }
        Spacer(Modifier.height(12.dp))

        OutlinedButton(onClick = onExitKiosk) {
            Text("Exit kiosk mode")
        }
        Spacer(Modifier.height(24.dp))

        TextButton(onClick = onBack) {
            Text("Back to kiosk")
        }
    }
}
