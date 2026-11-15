// Kapitel 17: Adresse und Token eingeben
package de.openhabbuch.ohapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import de.openhabbuch.ohapp.daten.Verbindung

@Composable
fun EinstellungenScreen(
    start: Verbindung,
    onSpeichern: (Verbindung) -> Unit,
    onWaechter: (Boolean) -> Unit = {},
) {
    // rememberSaveable: Eingaben überstehen das Drehen des Bildschirms
    var url by rememberSaveable { mutableStateOf(start.url.ifEmpty { "http://" }) }
    var token by rememberSaveable { mutableStateOf(start.token) }
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Verbindung zu openHAB")
        OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(),
            label = { Text("Adresse, z. B. http://192.168.0.10:8080") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
        OutlinedTextField(token, { token = it }, Modifier.fillMaxWidth(),
            label = { Text("API-Token") },
            visualTransformation = PasswordVisualTransformation())
        Button(onClick = { onSpeichern(Verbindung(url, token)) },
            enabled = Verbindung(url, token).vollstaendig) { Text("Speichern") }
        // Kapitel 20: Benachrichtigungen auch bei geschlossener App
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Wächter (Benachrichtigungen)", Modifier.weight(1f))
            var an by rememberSaveable { mutableStateOf(false) }
            Switch(checked = an, onCheckedChange = { an = it; onWaechter(it) })
        }
    }
}
