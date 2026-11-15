// Kapitel 16: Der State-Tracker – nur ausgewählte Items, fertig formatiert
package kapitel16_ereignisse

import io.github.michdo93.openhab.Events
import io.github.michdo93.openhab.Items
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import io.github.michdo93.openhab.SSE
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val SEITE_1 = listOf("Wohnzimmer_Dimmer", "Wohnzimmer_Temperatur")
private val SEITE_2 = listOf("Werkstatt_Temperatur", "Werkstatt_Feuchte")

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val client = OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN"))
    val events = Events(client)
    val items = Items(client)
    // Die aktuelle Anzeige je Item – eine App würde hier ihren Bildschirm neu zeichnen
    val anzeigen = linkedMapOf<String, Any?>()

    val strom = events.initiateStateTracker()
    var verbindung: String? = null
    strom.lines().collect { zeile ->
        if (verbindung == null && zeile.startsWith("data:")) {
            // 1. Nachricht: die Verbindungskennung
            val id = zeile.substring(5).trim()
            verbindung = id
            println("Verbindung: $id")
            events.updateSSEConnectionItems(id, SEITE_1)
            launch {                                    // die "Bedienung"
                delay(1000)
                items.sendCommand("Wohnzimmer_Dimmer", "55")
                // (noch) nicht abonniert:
                items.postUpdate("Werkstatt_Temperatur", "19.3 °C")
                delay(1000)
                println("--- Ansicht gewechselt: neue Item-Liste")
                events.updateSSEConnectionItems(id, SEITE_2)
                delay(1000)
                items.postUpdate("Werkstatt_Temperatur", "19.8 °C")
                delay(1000)
                strom.close()
            }
            return@collect
        }
        val daten = SSE.parseSSEData(zeile) ?: return@collect
        if (daten["type"] == "ALIVE") return@collect
        for ((name, z) in daten) {
            // displayState: formatiert wie in der UI
            val anzeige = Json.get(z, "displayState") ?: Json.get(z, "state")
            println("  %-22s state=%-12s anzeige=%s"
                .format(name, Json.get(z, "state"), anzeige))
            anzeigen[name] = anzeige
        }
    }
    println("zuletzt angezeigt: ${anzeigen.values}")
}
