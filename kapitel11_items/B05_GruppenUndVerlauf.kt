// Kapitel 11: Gruppen zur Laufzeit ändern und den Zustandsverlauf lesen
package kapitel11_items

import io.github.michdo93.openhab.Items
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

// Alle Ketten von Gruppen über einem Item, z. B. gLichter -> gHaus
private fun ketten(item: Any?): List<String> =
    Json.list(Json.get(item, "parents") ?: emptyList<Any>()).flatMap { p ->
        val weiter = ketten(p)
        if (weiter.isEmpty()) listOf(p["name"].toString())
        else weiter.map { "${p["name"]} -> $it" }
    }

// SimpleDateFormat statt java.time: läuft auf jedem Android
private fun uhrzeit(millis: Any?) =
    SimpleDateFormat("HH:mm:ss").format(Date(millis as Long))

fun main() = runBlocking<Unit> {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val items = Items(OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN")))

    // Ein per REST angelegtes Licht in die Gruppe aller Lichter aufnehmen
    items.addOrUpdateItem("Flur_Licht", mapOf("type" to "Switch", "name" to "Flur_Licht",
        "label" to "Flurlicht", "tags" to listOf("Switch", "Light")))
    println("aufnehmen: ${items.addGroupMember("gLichter", "Flur_Licht")}")
    val mitglieder = Json.list(Json.get(items.getItem("gLichter"), "members"))
    println("Mitglieder: ${mitglieder.map { it["name"] }}")

    // Ein Command an die Gruppe erreicht alle Mitglieder
    items.sendCommand("gLichter", "OFF")
    items.sendCommand("Flur_Licht", "ON")
    delay(300)
    println("gLichter: ${items.getItemState("gLichter")} (mindestens eines ist an)")

    // openHAB 5 merkt sich den vorherigen Zustand und die Zeitpunkte
    val licht = Json.map(items.getItem("Flur_Licht"))
    println("Flur_Licht: ${licht["state"]} (vorher ${licht["lastState"]}), "
        + "aktualisiert ${uhrzeit(licht["lastStateUpdate"])}, "
        + "geändert ${uhrzeit(licht["lastStateChange"])}")

    // In welchen Gruppen steckt das Item – bis ganz nach oben?
    val eltern = ketten(items.getItem("Flur_Licht", parents = true))
    println("Eltern: ${eltern.joinToString(" | ")}")

    println("entfernen: ${items.removeGroupMember("gLichter", "Flur_Licht")}")
    items.deleteItem("Flur_Licht")
}
