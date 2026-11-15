// Kapitel 12: Geräte suchen lassen und aus der Inbox übernehmen
package kapitel12_things

import io.github.michdo93.openhab.Discovery
import io.github.michdo93.openhab.Inbox
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import io.github.michdo93.openhab.Services
import io.github.michdo93.openhab.Things
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val client = OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN"))
    val things = Things(client)

    // Das Astro-Binding findet "Geräte" für den eingestellten Standort.
    // Der Standort ist eine Systemeinstellung – auch sie lässt sich per REST setzen.
    val services = Services(client)
    val config = Json.map(services.getServiceConfig("org.openhab.i18n")).toMutableMap()
    config["location"] = "48.0950,8.6630"         // Breite, Länge (Aldingen)
    services.updateServiceConfig("org.openhab.i18n", config)
    val standort = Json.get(services.getServiceConfig("org.openhab.i18n"), "location")
    println("Standort: $standort")

    val discovery = Discovery(client)
    val inbox = Inbox(client)
    println("Bindings mit Gerätesuche: ${discovery.getDiscoveryBindings()}")
    println("Suche läuft, Dauer in s: ${discovery.startBindingScan("astro")}")
    delay(3000)
    for (e in Json.list(inbox.getDiscoveredThings())) {
        println("  %-8s %-18s %s  %s".format(e["flag"], e["thingUID"], e["label"],
            e["properties"]))
    }

    // Den Mond übernehmen: aus dem Inbox-Eintrag wird ein Thing
    println("\nübernehmen: "
        + inbox.approveDiscoveryResult("astro:moon:local", "Mond über Aldingen"))
    delay(2000)
    val mond = Json.map(things.getThing("astro:moon:local"))
    println("${mond["label"]} ${Json.get(mond, "statusInfo", "status")} "
        + "${Json.list(mond["channels"]).size} Channels")

    // Wieder entfernen – der Eintrag erscheint bei der nächsten Suche erneut
    val geloescht = things.deleteThing("astro:moon:local")
    println("löschen: ${Json.get(geloescht, "statusInfo", "status")}")
}
