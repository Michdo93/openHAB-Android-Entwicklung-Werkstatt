// Kapitel 16: Ein Strom für vieles – Items, Things und Links gleichzeitig beobachten
package kapitel16_ereignisse

import io.github.michdo93.openhab.Events
import io.github.michdo93.openhab.Items
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.Links
import io.github.michdo93.openhab.OpenHABClient
import io.github.michdo93.openhab.SSEConnection
import io.github.michdo93.openhab.Things
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private const val SONNE = "astro:sun:zuhause"
private const val KANAL = "$SONNE:position#azimuth"
private val THEMEN = listOf(
    "openhab/items/Sonne_Azimut/added",          // Item angelegt
    "openhab/items/Sonne_Azimut/removed",        // Item gelöscht
    "openhab/items/Sonne_Azimut/statechanged",   // Zustand geändert
    "openhab/things/*/statuschanged",            // Statuswechsel aller Things
    "openhab/links/*/*",                         // neue und gelöschte Links
).joinToString(",")

// Eine kurze Beschreibung je Ereignisart
private fun info(typ: String, p: Any?): String = when {
    typ == "ThingStatusInfoChangedEvent" -> {
        val neu = Json.get(p, 0)                 // Liste [neu, alt]
        val alt = Json.get(p, 1)
        "${Json.get(alt, "status")} -> ${Json.get(neu, "status")} " +
            "(${Json.get(neu, "statusDetail")})"
    }
    typ.startsWith("ItemChannelLink") ->
        "${Json.get(p, "itemName")} <-> " +
            Json.str(p, "channelUID")!!.substringAfterLast(':')
    p is Map<*, *> -> (p["value"] ?: p["name"] ?: "").toString()
    else -> ""
}

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val client = OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN"))
    val items = Items(client)
    val things = Things(client)
    val links = Links(client)

    val strom: SSEConnection = Events(client).getEvents(THEMEN)
    // Erzeugt nacheinander typische Ereignisse und schließt dann den Strom
    launch {
        delay(1000)
        items.addOrUpdateItem("Sonne_Azimut", mapOf("type" to "Number:Angle",
            "name" to "Sonne_Azimut", "label" to "Sonnenrichtung"))
        links.linkItemToChannel("Sonne_Azimut", KANAL, mapOf())
        delay(1000)
        things.disableThing(SONNE)
        delay(1000)
        things.enableThing(SONNE)
        delay(2000)
        links.unlinkItemFromChannel("Sonne_Azimut", KANAL)
        items.deleteItem("Sonne_Azimut")
        delay(1000)
        strom.close()                            // fertig: Strom schließen
    }
    strom.events().collect { e ->               // endet, wenn der Strom geschlossen wird
        val typ = e["type"].toString()
        println("%-34s %s".format(typ, info(typ, e["payload"])))
    }
}
