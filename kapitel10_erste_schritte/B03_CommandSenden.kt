// Kapitel 10: Commands senden
package kapitel10_erste_schritte

import io.github.michdo93.openhab.Items
import io.github.michdo93.openhab.OpenHABClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val items = Items(OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN")))

    val befehle = listOf(
        "Wohnzimmer_Licht" to "ON",               // Switch
        "Wohnzimmer_Dimmer" to "60",              // Dimmer: Prozent
        "Wohnzimmer_Farblicht" to "240,100,80",   // Color: Farbton,Sättigung,Helligkeit
        "Kueche_Rollladen" to "DOWN",             // Rollershutter
        "Haus_Status" to "Alle zu Hause",         // String
    )
    for ((item, befehl) in befehle) {
        val antwort = items.sendCommand(item, befehl, source = "buch:kapitel10")
        println("%-22s <- %-14s %s".format(item, befehl, antwort))
    }

    delay(500)                               // openHAB verarbeitet Commands asynchron
    for ((item, _) in befehle) println("%-22s = %s".format(item, items.getItemState(item)))
}
