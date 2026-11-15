// Kapitel 15: Änderungen auf einer Sitemap-Seite live verfolgen
package kapitel15_oberflaechen

import io.github.michdo93.openhab.Items
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import io.github.michdo93.openhab.Sitemaps
import io.github.michdo93.openhab.UI
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private const val SITEMAP = "buch_tablet"                    // aus B02_SitemapAnlegen

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val client = OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN"))
    val sitemaps = Sitemaps(client)
    val items = Items(client)

    // 1. Abonnement anlegen und an eine Seite binden: die Seite einmal mit der ID abrufen
    val abo = Json.str(sitemaps.subscribeToSitemapEvents(), "subscriptionID")!!
    val seite = sitemaps.getSitemapPage(SITEMAP, SITEMAP, abo)
    println("Abonnement ${abo.take(8)}… für Seite »${Json.get(seite, "title")}«")

    // 2. Etwas ändern, das auf der Seite zu sehen ist. Achtung: openHAB antwortet auf
    //    den Ereignisstrom erst mit dem ersten Ereignis – deshalb die Änderung vorher
    //    als eigene Coroutine einplanen.
    val dimmer = items.getItemState("Wohnzimmer_Dimmer").toString()
    val tuer = items.getItemState("Flur_Haustuer").toString()
    val aendern = launch {
        delay(1000)
        items.sendCommand("Wohnzimmer_Dimmer", if (dimmer == "35") "65" else "35")
        items.postUpdate("Flur_Haustuer", if (tuer == "OPEN") "CLOSED" else "OPEN")
        delay(4000)                                 // kam nichts an? Noch eine Änderung
        items.sendCommand("Wohnzimmer_Dimmer", "50")
    }

    // 3. Den Strom lesen: Sitemap-Ereignisse haben kein "topic" -> includeAlive = true
    val strom = sitemaps.getSitemapEvents(abo, SITEMAP, SITEMAP)
    strom.events(includeAlive = true)
        .filter { it["widgetId"] != null }
        .take(3)                                    // danach schließt der Strom
        .collect {
            println("  Widget %-5s %-24s Zustand %s".format(it["widgetId"], it["label"],
                Json.get(it, "item", "state")))
        }
    aendern.cancel()
    items.postUpdate("Flur_Haustuer", tuer)
    println("Sitemap löschen: ${UI(client).deleteUIComponent("system:sitemap", SITEMAP)}")
}
