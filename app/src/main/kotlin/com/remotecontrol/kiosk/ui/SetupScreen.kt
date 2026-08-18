package com.remotecontrol.kiosk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import java.net.MalformedURLException
import java.net.URL

@Composable
fun SetupScreen(
    initialUrl: String = "",
    initialPin: String = "",
    onSave: (url: String, pin: String) -> Unit,
) {
    var urlInput by remember { mutableStateOf(initialUrl) }
    var pinInput by remember { mutableStateOf(initialPin) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Remote Control setup", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text("POS URL") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )

        OutlinedTextField(
            value = pinInput,
            onValueChange = { if (it.length <= 6) pinInput = it.filter(Char::isDigit) },
            label = { Text("Admin PIN (4–6 digits)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(24.dp))

        Button(onClick = {
            val trimmed = urlInput.trim()
            when {
                trimmed.startsWith("http://") ->
                    error = "http:// isn't supported — this app only allows secure https:// connections"
                pinInput.length < 4 -> error = "PIN must be at least 4 digits"
                else -> {
                    val normalizedUrl = normalizeUrl(trimmed)
                    if (normalizedUrl == null) error = "Enter a valid URL" else onSave(normalizedUrl, pinInput)
                }
            }
        }) {
            Text("Save & lock")
        }
    }
}

// Callers must reject plain "http://" input before calling this — it treats
// any other input as (or defaults it to) https, so it must never see a URL
// whose scheme hasn't already been validated as secure.
private fun normalizeUrl(input: String): String? {
    if (input.isEmpty()) return null
    val withScheme = if (input.startsWith("https://")) input else "https://$input"
    return try {
        val parsed = URL(withScheme)
        if (parsed.host.isNullOrBlank()) null else withScheme
    } catch (e: MalformedURLException) {
        null
    }
}
