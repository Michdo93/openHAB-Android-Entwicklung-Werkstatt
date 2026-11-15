// Kapitel 20: openHAB am Handgelenk – eine App für Wear OS
package de.openhabbuch.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHAB
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val openHAB =
        OpenHAB(BuildConfig.OPENHAB_URL, token = BuildConfig.OPENHAB_TOKEN)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Lichter(openHAB) } }
    }
}

@Composable
fun Lichter(openHAB: OpenHAB) {
    // Name -> (Beschriftung, Zustand); Compose zeichnet bei jeder Änderung neu
    var lichter by remember { mutableStateOf(mapOf<String, Pair<String, String>>()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val liste = openHAB.items.getItems(tags = "Light", fields = "name,label,type,state")
        // nur schaltbare Lampen, keine Messwerte wie die Helligkeit
        lichter = Json.list(liste).filter { it["type"] in setOf("Switch", "Dimmer") }
            .associate {
                it["name"].toString() to (it["label"].toString() to it["state"].toString())
            }
        // live: jede Änderung aus openHAB übernehmen (endet, wenn die App schließt)
        openHAB.itemEvents.ItemStateChangedEvent().events().collect { e ->
            val name = e["topic"].toString().split("/")[2]
            val alt = lichter[name] ?: return@collect
            val neu = Json.get(e, "payload", "value").toString()
            lichter = lichter + (name to (alt.first to neu))
        }
    }

    ScalingLazyColumn {
        item { ListHeader { Text("Lichter", textAlign = TextAlign.Center) } }
        items(lichter.entries.toList()) { (name, licht) ->
            val an = licht.second == "ON" || (licht.second.toIntOrNull() ?: 0) > 0
            ToggleChip(
                checked = an,
                onCheckedChange = { neu ->
                    val befehl = if (neu) "ON" else "OFF"
                    scope.launch { openHAB.items.sendCommand(name, befehl) }
                },
                label = { Text(licht.first) },
                toggleControl = {
                    Icon(ToggleChipDefaults.switchIcon(an), contentDescription = null)
                },
            )
        }
    }
}
