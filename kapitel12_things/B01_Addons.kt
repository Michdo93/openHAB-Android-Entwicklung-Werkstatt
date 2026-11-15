// Kapitel 12: Welche Add-ons gibt es, welche sind installiert?
package kapitel12_things

import io.github.michdo93.openhab.Addons
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val addons = Addons(OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN")))

    val alle = Json.list(addons.getAddons(language = "de"))
    val installiert = alle.filter { it["installed"] == true }
        .sortedBy { it["uid"].toString() }
    println("${alle.size} Add-ons verfügbar, ${installiert.size} installiert:")
    for (a in installiert) {
        println("  %-24s %-8s %s".format(a["uid"], a["version"], a["label"]))
    }

    // Wie viele gibt es von jeder Art?
    val arten = alle.groupingBy { it["type"].toString() }.eachCount().toSortedMap()
    println("\nVerfügbar nach Art: $arten")

    val astro = Json.map(addons.getAddon("binding-astro", language = "de"))
    println("\n${astro["label"]}: ${astro["description"].toString().take(70)}…")
    println("Logger-Pakete: ${astro["loggerPackages"]}")
    val quellen = Json.list(addons.getAddonServices()).map { it["id"] }
    println("Installationsquellen: $quellen")
}
