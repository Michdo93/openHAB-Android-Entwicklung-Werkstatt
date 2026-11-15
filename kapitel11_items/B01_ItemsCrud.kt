// Kapitel 11: Ein Item anlegen, lesen, ändern und löschen (CRUD)
package kapitel11_items

import io.github.michdo93.openhab.Items
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import kotlinx.coroutines.runBlocking

private const val NAME = "Garten_Pumpe"

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val items = Items(OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN")))

    // Create – ein neues Item aus einer Map (wird als JSON gesendet)
    val definition = mutableMapOf<String, Any?>(
        "type" to "Switch", "name" to NAME, "label" to "Gartenpumpe",
        "category" to "water", "tags" to listOf("Switch"), "groupNames" to listOf("gHaus"),
    )
    val neu = Json.map(items.addOrUpdateItem(NAME, definition))
    println("angelegt:  ${neu["name"]} ${neu["type"]} ${neu["state"]}")

    // Read – und gleich bedienen: ein per REST angelegtes Item ist ein normales Item
    items.sendCommand(NAME, "ON")
    println("gelesen:   ${items.getItemState(NAME)}")

    // Update – PUT ersetzt die ganze Definition (idempotent)
    definition["label"] = "Gartenpumpe Zisterne"
    definition["groupNames"] = listOf("gHaus", "gWerkstatt")
    items.addOrUpdateItem(NAME, definition)
    val pumpe = Json.map(items.getItem(NAME))
    println("geändert:  ${pumpe["label"]} ${pumpe["groupNames"]} "
        + "Zustand: ${pumpe["state"]}")
    val editierbar = pumpe["editable"] == true
    println("verwaltet: ${if (editierbar) "per REST änderbar" else "aus einer Datei"}")

    // Delete – und wieder aufräumen
    println("löschen:   ${items.deleteItem(NAME)}")
    println("danach:    ${items.getItem(NAME)}")
}
