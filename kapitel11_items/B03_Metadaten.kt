// Kapitel 11: Metadaten – Zusatzinformationen, die openHABs Verhalten ändern
package kapitel11_items

import io.github.michdo93.openhab.Items
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

private const val KLINGEL = "Klingel_Status"

fun main() = runBlocking<Unit> {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val items = Items(OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN")))
    items.addOrUpdateItem(KLINGEL, mapOf("type" to "String", "name" to KLINGEL,
        "label" to "Klingel", "groupNames" to listOf("gHaus")))

    // 1. stateDescription: Anzeigeformat und erlaubte Werte für Oberflächen
    val optionen = "RUHE=Ruhe,KLINGELT=Es klingelt!,GEOEFFNET=Tür geöffnet"
    println("stateDescription: " + items.addMetadata(KLINGEL, "stateDescription", mapOf(
        "value" to " ",
        "config" to mapOf("pattern" to "%s", "readOnly" to false, "options" to optionen))))

    // 2. expire: Zustand fällt nach zehn Sekunden automatisch zurück
    println("expire:           "
        + items.addMetadata(KLINGEL, "expire", mapOf("value" to "10s,state=RUHE")))

    // 3. eigener Namensraum: beliebige Daten für die eigene Anwendung
    println("eigener:          " + items.addMetadata(KLINGEL, "restbuch", mapOf(
        "value" to "klingel", "config" to mapOf("raum" to "Flur", "prioritaet" to 2))))
    println("Namensräume:      ${items.getMetadataNamespaces(KLINGEL)}")
    val item = items.getItem(KLINGEL, metadata = "stateDescription,expire,restbuch")
    println(Json.stringify(Json.get(item, "metadata"), 2))

    // Die Anzeige nutzt die Optionen aus stateDescription
    items.postUpdate(KLINGEL, "KLINGELT")
    delay(300)                              // openHAB verarbeitet Updates asynchron
    val klingel = Json.map(items.getItem(KLINGEL))
    val anzeige = Json.list(Json.get(klingel, "stateDescription", "options"))
        .firstOrNull { it["value"] == klingel["state"] }?.get("label") ?: "?"
    println("Zustand: ${klingel["state"]} | Anzeige: $anzeige")

    // expire setzt den Zustand nach zehn Sekunden zurück
    delay(11_000)
    println("nach 11 s: ${items.getItemState(KLINGEL)}")
    println("entfernen: ${items.removeMetadata(KLINGEL, "restbuch")}")
    items.deleteItem(KLINGEL)
}
