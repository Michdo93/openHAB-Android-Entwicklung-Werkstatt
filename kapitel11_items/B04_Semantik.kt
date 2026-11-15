// Kapitel 11: Das semantische Modell – Orte, Ausstattung und Messpunkte
package kapitel11_items

import io.github.michdo93.openhab.Items
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import io.github.michdo93.openhab.Tags
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val client = OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN"))
    val items = Items(client)
    val tags = Tags(client)

    // 1. Der Tag-Katalog: Hunderte vordefinierte semantische Tags, hier auf Deutsch
    val katalog = Json.list(tags.getTags("de"))
    val orte = katalog.filter { it["uid"].toString().startsWith("Location_") }
    println("${katalog.size} Tags, davon ${orte.size} Orte, z. B.: "
        + orte.take(6).joinToString(", ") { it["label"].toString() })

    // 2. openHAB leitet aus den Tags Metadaten im Namensraum "semantics" ab
    val alle = items.getItems(metadata = "semantics", fields = "name,label,metadata")
    for (item in Json.list(alle)) {
        val sem = Json.get(item, "metadata", "semantics") ?: continue
        val bezug = Json.get(sem, "config", "relatesTo") ?: ""
        val ort = Json.get(sem, "config", "hasLocation") ?: ""
        println("  %-24s %-26s %-22s %s"
            .format(item["name"], Json.get(sem, "value"), bezug, ort))
    }

    // 3. Wo befindet sich ein Messpunkt? openHAB folgt dem Modell nach oben.
    val ort = items.getSemanticItem("Wohnzimmer_Temperatur", "Location")
    println("\nWohnzimmer_Temperatur liegt in: ${Json.get(ort, "name")} "
        + "(${Json.get(ort, "label")})")

    // 4. Ein eigener Ort: das Tag wird per REST angelegt und sofort nutzbar
    val tag = tags.createTag(mapOf(
        "uid" to "Location_Outdoor_Garden_Gartenhaus", "name" to "Gartenhaus",
        "label" to "Gartenhaus", "description" to "Hütte im Garten",
        "synonyms" to listOf("Schuppen")))
    println("\nTag angelegt: ${Json.get(tag, "uid")} "
        + "(editierbar: ${Json.get(tag, "editable")})")
    items.addOrUpdateItem("gGartenhaus", mapOf("type" to "Group", "name" to "gGartenhaus",
        "label" to "Gartenhaus", "tags" to listOf("Gartenhaus"),
        "groupNames" to listOf("gHaus")))
    items.addOrUpdateItem("Gartenhaus_Temperatur", mapOf("type" to "Number:Temperature",
        "name" to "Gartenhaus_Temperatur", "label" to "Temperatur",
        "tags" to listOf("Measurement", "Temperature"),
        "groupNames" to listOf("gGartenhaus")))
    val sem = Json.get(items.getItem("Gartenhaus_Temperatur", metadata = "semantics"),
        "metadata", "semantics")
    println("Semantik: ${Json.get(sem, "value")} ${Json.get(sem, "config")}")

    for (name in listOf("Gartenhaus_Temperatur", "gGartenhaus")) items.deleteItem(name)
    println("Tag löschen: ${tags.deleteTag("Location_Outdoor_Garden_Gartenhaus")}")
}
