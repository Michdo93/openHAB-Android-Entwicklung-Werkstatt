// Kapitel 19: Das Ergebnis des Vorab-Tests anzeigen
package de.openhabbuch.ohapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.openhabbuch.ohapp.daten.Verbindung
import de.openhabbuch.ohapp.pruefung.Pruefung
import de.openhabbuch.ohapp.pruefung.vorabtest
import io.github.michdo93.openhab.OpenHABClient

@Composable
fun PruefScreen(verbindung: Verbindung, onFertig: () -> Unit) {
    var ergebnis by remember { mutableStateOf<List<Pruefung>?>(null) }
    LaunchedEffect(verbindung) {
        ergebnis = vorabtest(OpenHABClient(verbindung.url, token = verbindung.token))
    }
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Vorab-Test", style = MaterialTheme.typography.headlineSmall)
        val liste = ergebnis
        if (liste == null) {
            CircularProgressIndicator()
        } else {
            for (p in liste) Text((if (p.ok) "✔  " else "✘  ") + p.was)
            val alle = liste.all { it.ok }
            Text(if (alle) "Alles bereit." else "Bitte die openHAB-Konfiguration prüfen.",
                style = MaterialTheme.typography.titleMedium)
            Button(onClick = onFertig) { Text("Weiter") }
        }
    }
}
