// Kapitel 10: Die erste App – ein Knopf schaltet das Licht im Wohnzimmer
package de.openhabbuch.ersteapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.michdo93.openhab.Items
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val client =
        OpenHABClient(BuildConfig.OPENHAB_URL, token = BuildConfig.OPENHAB_TOKEN)
    private val items = Items(client)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) { Lichtschalter(items) }
            }
        }
    }
}

@Composable
fun Lichtschalter(items: Items) {
    // ändert sich der Zustand, zeichnet Compose die Oberfläche neu
    var zustand by remember { mutableStateOf("…") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {                              // einmal beim Start: Zustand lesen
        val antwort = items.getItemState("Wohnzimmer_Licht")
        zustand = if (Json.isError(antwort)) "Fehler: ${Json.error(antwort)}"
            else antwort.toString()
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Wohnzimmer", style = MaterialTheme.typography.headlineMedium)
        Text("Deckenlicht: $zustand", style = MaterialTheme.typography.titleLarge)
        Button(onClick = {
            scope.launch {                             // suspend-Aufrufe in einer Coroutine
                val neu = if (zustand == "ON") "OFF" else "ON"
                val antwort = items.sendCommand("Wohnzimmer_Licht", neu, source = "app")
                zustand = if (Json.isError(antwort)) "Fehler: ${Json.error(antwort)}"
                    else neu
            }
        }) {
            Text(if (zustand == "ON") "Ausschalten" else "Einschalten")
        }
    }
}
