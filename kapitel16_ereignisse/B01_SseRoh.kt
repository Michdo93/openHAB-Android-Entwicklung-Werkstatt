// Kapitel 16: Den Ereignisstrom direkt lesen – Zeile für Zeile
package kapitel16_ereignisse

import io.github.michdo93.openhab.ItemEvents
import io.github.michdo93.openhab.Items
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import io.github.michdo93.openhab.SSE
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val client = OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN"))

    // Zum Ausprobieren: nach einer Sekunde den Dimmer verstellen
    launch {
        delay(1000)
        Items(client).sendCommand("Wohnzimmer_Dimmer", "${Random.nextInt(20, 80)}")
    }

    // Alle Zustandsänderungen aller Items (Topic openhab/items/*/statechanged)
    val strom = ItemEvents(client).ItemStateChangedEvent()
    println("Warte auf Ereignisse …")
    withTimeoutOrNull(15_000) {                          // 15 Sekunden lang lesen
        SSE.iterSSELines(strom).collect { zeile ->       // jede Zeile, sobald sie da ist
            println("roh: ${zeile.take(70)}")
            if (!zeile.startsWith("data:")) return@collect   // event:-, id:-, Leerzeilen
            // 1. JSON: das Ereignis
            val ereignis = Json.map(Json.parse(zeile.substring(5)))
            if (ereignis["type"] == "ALIVE") return@collect      // Lebenszeichen
            // 2. JSON: die Nutzlast steckt als Text im Ereignis
            val payload = Json.parse(ereignis["payload"].toString())
            val name = ereignis["topic"].toString().split("/")[2]
            println("  -> $name: ${Json.get(payload, "oldValue")} -> "
                + Json.get(payload, "value"))
        }
    }
    println("beendet, Strom offen: ${strom.isOpen}")    // der Abbruch hat ihn geschlossen
}
