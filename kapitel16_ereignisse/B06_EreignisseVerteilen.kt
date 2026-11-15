// Kapitel 16: Ein Strom für das ganze Programm – Ereignisse mit SharedFlow verteilen
package kapitel16_ereignisse

import io.github.michdo93.openhab.ItemEvents
import io.github.michdo93.openhab.Items
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/** Ein Zustandswechsel: Item, alter und neuer Wert. */
data class Wechsel(val item: String, val alt: Any?, val neu: Any?)

/**
 * Liest EINEN Strom und verteilt ihn an beliebig viele Beobachter. shareIn öffnet die
 * Verbindung beim ersten Beobachter und schließt sie, wenn der letzte geht.
 */
class ItemVerteiler(client: OpenHABClient, scope: CoroutineScope) {
    val alle: SharedFlow<Wechsel> = flow {
        ItemEvents(client).ItemStateChangedEvent().events().collect { e ->
            val p = e["payload"]
            emit(Wechsel(e["topic"].toString().split("/")[2],
                Json.get(p, "oldValue"), Json.get(p, "value")))
        }
    }.shareIn(scope, SharingStarted.WhileSubscribed())

    /** Nur die Wechsel eines Items – für "seine" Anzeige. */
    fun item(name: String): Flow<Wechsel> = alle.filter { it.item == name }
}

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val client = OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN"))
    val items = Items(client)
    val verteiler = ItemVerteiler(client, this)

    // Beliebig viele Programmteile melden sich für "ihr" Item an
    fun beobachte(name: String, beschriftung: String) = launch {
        verteiler.item(name).collect {
            println("%-16s %s -> %s".format(beschriftung, it.alt, it.neu))
        }
    }
    val beobachter = listOf(
        beobachte("Wohnzimmer_Licht", "Deckenlicht"),
        beobachte("Wohnzimmer_Dimmer", "Leselampe"),
        beobachte("Wohnzimmer_Dimmer", "Leselampe (2)"),   // derselbe Strom
    )
    var anzahl = 0
    val zaehler = launch { verteiler.alle.collect { anzahl++ } }

    delay(1000)
    val licht = if (items.getItemState("Wohnzimmer_Licht") == "ON") "OFF" else "ON"
    items.sendCommand("Wohnzimmer_Licht", licht)
    val dimmer = if (items.getItemState("Wohnzimmer_Dimmer") == "45") "65" else "45"
    items.sendCommand("Wohnzimmer_Dimmer", dimmer)
    delay(3000)
    // der letzte Beobachter geht: shareIn schließt den Strom
    (beobachter + zaehler).forEach { it.cancel() }
    println("$anzahl Ereignisse über einen einzigen Strom verteilt")
    coroutineContext.cancelChildren()                  // auch shareIn selbst beenden
}
