// Kapitel 12: Ein Thing anlegen, umkonfigurieren, abschalten und löschen
package kapitel12_things

import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import io.github.michdo93.openhab.Things
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

private const val MOND = "astro:moon:buch"

private suspend fun status(things: Things): String {
    val s = things.getThingStatus(MOND)
    val detail = Json.str(s, "statusDetail")
    return Json.str(s, "status") + if (detail == "NONE") "" else " / $detail"
}

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val things = Things(OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN")))

    val neu = Json.map(things.createThing(mapOf(
        "UID" to MOND, "thingTypeUID" to "astro:moon", "label" to "Mond (per REST)",
        "configuration" to mapOf("geolocation" to "48.0950,8.6630", "interval" to 300))))
    println("angelegt: ${neu["UID"]} mit ${Json.list(neu["channels"]).size} Channels")
    delay(2000)
    println("Status:   ${status(things)}")

    // Konfiguration ändern – openHAB initialisiert das Thing neu
    things.updateThingConfiguration(MOND,
        mapOf("geolocation" to "52.5200,13.4050", "interval" to 60))
    println("Ort:      ${Json.get(things.getThing(MOND), "configuration", "geolocation")}"
        + " (Berlin)")

    // Label ändern: updateThing ersetzt die ganze Definition
    val thing = Json.map(things.getThing(MOND)) + ("label" to "Mond über Berlin")
    println("Label:    ${Json.get(things.updateThing(MOND, thing), "label")}")

    // Abschalten und wieder einschalten
    things.disableThing(MOND)
    delay(1000)
    println("aus:      ${status(things)}")
    things.enableThing(MOND)
    delay(2000)
    println("an:       ${status(things)}")

    println("löschen:  ${Json.get(things.deleteThing(MOND), "statusInfo", "status")}")
    println("Things:   ${Json.list(things.getThings(true)).map { it["UID"] }}")
}
