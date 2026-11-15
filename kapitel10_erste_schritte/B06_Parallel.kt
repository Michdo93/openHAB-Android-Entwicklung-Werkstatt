// Kapitel 10: Viele Anfragen gleichzeitig – mit async und awaitAll
package kapitel10_erste_schritte

import io.github.michdo93.openhab.Items
import io.github.michdo93.openhab.OpenHABClient
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val items = Items(OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN")))
    val namen = listOf("Wohnzimmer_Licht", "Wohnzimmer_Dimmer", "Wohnzimmer_Temperatur",
        "Kueche_Rollladen", "Flur_Haustuer", "Werkstatt_Temperatur", "Haus_Status")
    repeat(3) { namen.map { async { items.getItemState(it) } }.awaitAll() }   // aufwärmen

    // nacheinander: jede Anfrage wartet auf die vorige
    val nacheinander: List<Any?>
    val zeit1 = measureTimeMillis { nacheinander = namen.map { items.getItemState(it) } }

    // gleichzeitig: eine Coroutine je Anfrage, awaitAll sammelt die Ergebnisse
    val gleichzeitig: List<Any?>
    val zeit2 = measureTimeMillis {
        gleichzeitig = namen.map { async { items.getItemState(it) } }.awaitAll()
    }

    for (i in namen.indices) {
        println("%-24s %-16s %s".format(namen[i], nacheinander[i], gleichzeitig[i]))
    }
    println("nacheinander: $zeit1 ms")
    println("gleichzeitig: $zeit2 ms")
}
