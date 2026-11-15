// Kapitel 11: Einen ganzen Raum mit einem einzigen Aufruf anlegen
package kapitel11_items

import io.github.michdo93.openhab.Items
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import kotlinx.coroutines.runBlocking

// Hilfsfunktion: die Felder, die jedes Item hat, plus beliebige weitere
private fun item(
    type: String, name: String, label: String, gruppen: List<String>,
    tags: List<String> = emptyList(), vararg mehr: Pair<String, Any?>,
): Map<String, Any?> =
    mapOf("type" to type, "name" to name, "label" to label,
          "groupNames" to gruppen, "tags" to tags) + mehr

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val items = Items(OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN")))

    val gartenhaus = listOf(
        item("Group", "gGartenhaus", "Gartenhaus", listOf("gHaus"), listOf("Garden")),
        item("Switch", "Gartenhaus_Licht", "Licht",
            listOf("gGartenhaus", "gGartenhaus_Lichter"),
            listOf("Switch", "Light"), "category" to "light"),
        item("Number:Temperature", "Gartenhaus_Temperatur", "Temperatur",
            listOf("gGartenhaus"),
            listOf("Measurement", "Temperature"), "category" to "temperature"),
        item("Contact", "Gartenhaus_Tuer", "Tür", listOf("gGartenhaus"),
            listOf("OpenState", "Door"), "category" to "door"),
        item("Group", "gGartenhaus_Lichter", "Lichter im Gartenhaus", listOf("gGartenhaus"),
            emptyList(), "groupType" to "Switch",
            "function" to mapOf("name" to "OR", "params" to listOf("ON", "OFF"))),
    )
    for (r in Json.list(items.addOrUpdateItems(gartenhaus))) {
        println("anlegen: %-24s %s".format(r["name"], r["status"]))
    }
    items.postUpdate("Gartenhaus_Temperatur", "12.5 °C")
    items.postUpdate("Gartenhaus_Tuer", "CLOSED")
    items.sendCommand("Gartenhaus_Licht", "ON")

    val raum = items.getItem("gGartenhaus", recursive = true)
    for (m in Json.list(Json.get(raum, "members")).sortedBy { it["name"].toString() }) {
        println("  %-24s %-20s %s".format(m["name"], m["type"], m["state"]))
    }
    // Die Gruppenfunktion OR(ON,OFF) fasst die Mitglieder zusammen
    println("Lichter im Gartenhaus: ${items.getItemState("gGartenhaus_Lichter")}")

    // Aufräumen: zuerst die Mitglieder, dann die Gruppen
    for (i in gartenhaus.reversed()) items.deleteItem(i["name"].toString())
    println("aufgeräumt: ${Json.isError(items.getItem("gGartenhaus"))}")
}
