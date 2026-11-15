// Kapitel 10: Items lesen
package kapitel10_erste_schritte

import io.github.michdo93.openhab.Items
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val client = OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN"))
    val items = Items(client)

    // 1. Alle Items, auf drei Felder beschränkt (spart Daten)
    val alle = Json.list(items.getItems(fields = "name,type,state"))
    println("${alle.size} Items gefunden:")
    for (i in alle.sortedBy { it["name"].toString() }) {
        println("  %-28s %-22s %s".format(i["name"], i["type"], i["state"]))
    }

    // 2. Ein einzelnes Item mit allen Details
    val licht = Json.map(items.getItem("Wohnzimmer_Licht"))
    println("\nLabel: ${licht["label"]} | Gruppen: ${licht["groupNames"]}")
    println("Tags: ${licht["tags"]}")

    // 3. Nur der Zustand – als reiner Text
    println("Temperatur: ${items.getItemState("Wohnzimmer_Temperatur")}")

    // 4. Filter: alle Items eines Typs, alle mit einem Tag – gleichzeitig angefragt
    val dimmer = async { items.getItems(type = "Dimmer", fields = "name") }
    val lichter = async { items.getItems(tags = "Light", fields = "name") }
    println("Dimmer: ${namen(dimmer.await())}")
    println("Mit Tag 'Light': ${namen(lichter.await())}")
}

private fun namen(antwort: Any?) =
    Json.list(antwort).map { it["name"].toString() }.sorted().joinToString(", ")
