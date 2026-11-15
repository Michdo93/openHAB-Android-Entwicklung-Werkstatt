// Kapitel 12: Baupläne – Thing-Typen, Channel-Gruppen, Konfiguration, Channel-Typen
package kapitel12_things

import io.github.michdo93.openhab.ChannelTypes
import io.github.michdo93.openhab.ConfigDescriptions
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import io.github.michdo93.openhab.ThingTypes
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val client = OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN"))

    val typen = ThingTypes(client)
    for (t in Json.list(typen.getThingTypes("astro", "de"))) {
        println("%-12s %s".format(t["UID"], t["label"]))
    }
    val mond = Json.map(typen.getThingType("astro:moon", "de"))
    println("\n${mond["label"]} – Channel-Gruppen:")
    for (gruppe in Json.list(mond["channelGroups"])) {
        val kanaele = Json.list(gruppe["channels"])
            .joinToString(", ") { it["id"].toString() }
        println("  %-10s %s".format(gruppe["id"], kanaele))
    }

    // Welche Konfiguration braucht ein Mond-Thing?
    val beschreibung = ConfigDescriptions(client)
        .getConfigDescription("thing-type:astro:moonconfig", "de")
    println("\nKonfiguration:")
    for (p in Json.list(Json.get(beschreibung, "parameters"))) {
        val pflicht = if (p["required"] == true) "Pflicht"
            else "Standard ${p["default"] ?: "–"}"
        println("  %-12s %-8s %-16s %s".format(p["name"], p["type"], pflicht, p["label"]))
    }

    // Ein Channel-Typ verrät, zu welchem Item-Typ er passt
    val kanal = Json.map(ChannelTypes(client).getChannelType("astro:phaseName", "de"))
    println("\nChannel-Typ ${kanal["UID"]}: ${kanal["label"]}, "
        + "Item-Typ ${kanal["itemType"]}")
    val optionen = Json.list(Json.get(kanal, "stateDescription", "options"))
        .take(8).joinToString(", ") { it["label"].toString() }
    println("Optionen: $optionen")
}
