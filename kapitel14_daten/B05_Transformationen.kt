// Kapitel 14: Ein Add-on installieren und eine Transformation anlegen
package kapitel14_daten

import io.github.michdo93.openhab.Addons
import io.github.michdo93.openhab.Items
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import io.github.michdo93.openhab.Transformations
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

private const val TUER = "config:map:tuer"

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val client = OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN"))
    val addons = Addons(client)
    val items = Items(client)
    val trafos = Transformations(client)

    // 1. Den MAP-Transformationsdienst installieren (falls noch nicht geschehen)
    if (Json.get(addons.getAddon("transformation-map"), "installed") != true) {
        println("installieren: ${addons.installAddon("transformation-map")}")
        while ("MAP" !in Json.array(trafos.getTransformationServices())) delay(1000)
    }
    println("Transformationsdienste: ${trafos.getTransformationServices()}")

    // 2. Eine eigene MAP-Transformation anlegen
    println("anlegen: " + trafos.updateTransformation(TUER, mapOf(
        "uid" to TUER, "label" to "Türzustand", "type" to "map",
        "configuration" to mapOf(
            "function" to "OPEN=offen\nCLOSED=geschlossen\nNULL=unbekannt\n"))))
    for (t in Json.list(trafos.getTransformations())) {
        println("  %-16s %-4s %s".format(t["uid"], t["type"],
            if (t["editable"] == true) "änderbar" else "aus Datei"))
    }

    // 3. Im Anzeigeformat eines Items verwenden
    items.addMetadata("Flur_Haustuer", "stateDescription", mapOf("value" to " ",
        "config" to mapOf("pattern" to "MAP($TUER):%s")))
    for (zustand in listOf("OPEN", "CLOSED")) {
        items.postUpdate("Flur_Haustuer", zustand)
        delay(300)                         // postUpdate wird asynchron verarbeitet
        val tuer = Json.map(items.getItem("Flur_Haustuer"))
        println("Zustand %-6s -> Anzeige %s"
            .format(tuer["state"], tuer["transformedState"]))
    }

    // Aufräumen – das Add-on bleibt installiert
    items.removeMetadata("Flur_Haustuer", "stateDescription")
    println("löschen: ${trafos.deleteTransformation(TUER)}")
}
