// Kapitel 14: Persistenzdienste und die Items, die sie speichern
package kapitel14_daten

import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import io.github.michdo93.openhab.Persistence
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val client = OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN"))
    val persistence = Persistence(client)

    for (dienst in Json.list(persistence.getServices("de"))) {
        println("%-9s %-10s %s".format(dienst["id"], dienst["type"], dienst["label"]))
    }
    // Was liegt im Arbeitsspeicher-Dienst – und seit wann?
    for (e in Json.list(persistence.getItemsFromService("inmemory")).take(6)) {
        println("  %-24s %4s Werte, ältester %s".format(e["name"], e["count"] ?: "?",
            e["earliest"] ?: "–"))
    }
    // rrd4j kann keine Itemliste liefern – die Bibliothek erklärt den Grund
    println("rrd4j: ${persistence.getItemsFromService("rrd4j")}")
}
