// Kapitel 16: Welche Ereignisse löst ein einziger Command aus?
package kapitel16_ereignisse

import io.github.michdo93.openhab.Events
import io.github.michdo93.openhab.Items
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

private const val ZIEL = "Wohnzimmer_Licht"

fun main() = runBlocking<Unit> {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val client = OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN"))
    val items = Items(client)

    launch {
        delay(1000)
        val neu = if (items.getItemState(ZIEL) == "ON") "OFF" else "ON"
        println(">>> sende Command $neu\n")
        items.sendCommand(ZIEL, neu)
    }

    // Alle Ereignisse dieses Items: Topic openhab/items/Wohnzimmer_Licht/*
    val strom = Events(client).getEvents("openhab/items/$ZIEL/*")
    withTimeoutOrNull(3000) {                             // drei Sekunden lang
        strom.events().collect { e ->                     // payload ist schon entpackt
            val topic = e["topic"].toString().split("/")
            println("%-24s %-13s %s".format(e["type"], topic.last(),
                Json.get(e, "payload", "value")))
        }
    }
}
