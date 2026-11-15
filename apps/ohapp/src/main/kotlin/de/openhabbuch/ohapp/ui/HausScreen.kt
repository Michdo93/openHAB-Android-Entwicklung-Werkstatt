// Kapitel 17: Die View – Compose zeichnet, was das ViewModel als Zustand meldet
package de.openhabbuch.ohapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.openhabbuch.ohapp.daten.Licht

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HausScreen(
    zustand: HausZustand,
    meldung: String?,
    onSchalten: (Licht, Boolean) -> Unit,
    onDimmen: (Licht, Int) -> Unit,
    onMeldungGelesen: () -> Unit,
    onPruefen: () -> Unit,
    onEinstellungen: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(meldung) {                        // Meldungen kurz unten einblenden
        if (meldung != null) {
            snackbar.showSnackbar(meldung)
            onMeldungGelesen()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Zuhause") }, actions = {
                IconButton(onClick = onPruefen) {          // Kapitel 19: Vorab-Test
                    Icon(Icons.Default.Check, contentDescription = "Vorab-Test")
                }
                IconButton(onClick = onEinstellungen) {
                    Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                }
            })
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innen ->
        Box(Modifier.fillMaxSize().padding(innen)) {
            when (zustand) {                         // ein Zweig je Zustand
                HausZustand.Einrichten -> Mitte("Bitte Adresse und Token eintragen.")
                HausZustand.Laedt -> Mitte(null)
                is HausZustand.Fehler -> Mitte("Fehler: ${zustand.text}")
                is HausZustand.Bereit -> Uebersicht(zustand, onSchalten, onDimmen)
            }
        }
    }
}

@Composable
private fun Mitte(text: String?) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        if (text == null) CircularProgressIndicator() else Text(text)
    }
}

@Composable
private fun Uebersicht(
    z: HausZustand.Bereit,
    onSchalten: (Licht, Boolean) -> Unit,
    onDimmen: (Licht, Int) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Wohnzimmer ${z.temperatur}",
                        style = MaterialTheme.typography.titleLarge)
                    val tuer = if (z.haustuer == "OPEN") "offen" else "geschlossen"
                    Text("Haustür: $tuer")
                }
            }
        }
        items(z.lichter, key = { it.name }) { licht ->
            LichtKarte(licht, onSchalten, onDimmen)
        }
    }
}

@Composable
private fun LichtKarte(licht: Licht, onSchalten: (Licht, Boolean) -> Unit,
                       onDimmen: (Licht, Int) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(licht.label, Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium)
                Switch(checked = licht.an, onCheckedChange = { onSchalten(licht, it) })
            }
            if (licht.typ != "Switch") {
                // während des Ziehens nur lokal anzeigen, beim Loslassen senden
                var wert by remember(licht.helligkeit) {
                    mutableFloatStateOf(licht.helligkeit.toFloat())
                }
                Slider(value = wert, onValueChange = { wert = it }, valueRange = 0f..100f,
                    onValueChangeFinished = { onDimmen(licht, wert.toInt()) })
                Text("${wert.toInt()} %", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
