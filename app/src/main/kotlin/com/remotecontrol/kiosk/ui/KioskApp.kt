package com.remotecontrol.kiosk.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.remotecontrol.kiosk.KioskPrefs

private sealed interface Screen {
    data object Kiosk : Screen
    data object Setup : Screen
    data object AdminMenu : Screen
    data object Maintenance : Screen
}

// Taps on the hidden corner zone required to bring up the admin PIN prompt,
// and the window they must land inside — fast enough that a cashier idly
// tapping the corner won't trigger it by accident.
private const val TAPS_TO_PROMPT_PIN = 5
private const val TAP_WINDOW_MS = 2000L

@Composable
fun KioskApp(
    prefs: KioskPrefs,
    onExitKiosk: () -> Unit,
    onResumeKiosk: () -> Unit,
    onSettingsSaved: () -> Unit,
) {
    var screen by remember {
        mutableStateOf<Screen>(
            when {
                prefs.isMaintenanceMode() -> Screen.Maintenance
                prefs.getUrl() == null -> Screen.Setup
                else -> Screen.Kiosk
            },
        )
    }
    var showPinDialog by remember { mutableStateOf(false) }
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (screen) {
            Screen.Setup -> SetupScreen(
                initialUrl = prefs.getUrl().orEmpty(),
                initialPin = "",
                onSave = { url, pin ->
                    prefs.save(url, pin)
                    onSettingsSaved()
                    screen = Screen.Kiosk
                },
            )

            Screen.Kiosk -> Box(modifier = Modifier.fillMaxSize()) {
                KioskWebView(url = prefs.getUrl().orEmpty(), modifier = Modifier.fillMaxSize())

                // Invisible admin-access tap zone, top-left corner.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(48.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) {
                            val now = System.currentTimeMillis()
                            tapCount = if (now - lastTapTime > TAP_WINDOW_MS) 1 else tapCount + 1
                            lastTapTime = now
                            if (tapCount >= TAPS_TO_PROMPT_PIN) {
                                tapCount = 0
                                showPinDialog = true
                            }
                        },
                )
            }

            Screen.AdminMenu -> AdminMenuScreen(
                onEditSettings = { screen = Screen.Setup },
                onExitKiosk = {
                    onExitKiosk()
                    screen = Screen.Maintenance
                },
                onBack = { screen = Screen.Kiosk },
            )

            Screen.Maintenance -> MaintenanceScreen(
                onResumeKiosk = {
                    onResumeKiosk()
                    screen = Screen.Kiosk
                },
            )
        }
    }

    if (showPinDialog) {
        PinDialog(
            expectedPin = prefs.getPin(),
            onSuccess = {
                showPinDialog = false
                screen = Screen.AdminMenu
            },
            onDismiss = { showPinDialog = false },
        )
    }
}
