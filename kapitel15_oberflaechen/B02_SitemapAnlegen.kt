// Kapitel 15: Eine Sitemap als UI-Komponente erzeugen
package kapitel15_oberflaechen

import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import io.github.michdo93.openhab.Sitemaps
import io.github.michdo93.openhab.UI
import kotlinx.coroutines.runBlocking

private const val TABLET = "buch_tablet"

private fun widget(component: String, item: String, vararg config: Pair<String, Any?>) =
    mapOf("component" to component, "config" to mapOf(*config) + ("item" to item))

private fun rahmen(label: String, vararg widgets: Map<String, Any?>) = mapOf(
    "component" to "Frame", "config" to mapOf("label" to label),
    "slots" to mapOf("widgets" to widgets.toList()))

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val client = OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN"))
    val ui = UI(client)
    val sitemaps = Sitemaps(client)

    val tablet = mapOf(
        "uid" to TABLET, "component" to "Sitemap",
        "config" to mapOf("label" to "Tablet im Flur"),
        "slots" to mapOf("widgets" to listOf(
            rahmen("Licht",
                widget("Switch", "Wohnzimmer_Licht", "label" to "Decke"),
                widget("Slider", "Wohnzimmer_Dimmer", "label" to "Leselampe"),
                widget("Switch", "gLichter", "label" to "Alle Lichter",
                    "mappings" to listOf("OFF=Alle aus"))),
            rahmen("Haus",
                widget("Text", "Flur_Haustuer", "label" to "Haustür"),
                widget("Selection", "Haus_Status", "label" to "Modus",
                    "mappings" to listOf("Alle zu Hause=Zu Hause", "Kino=Kino",
                                         "Gute Nacht=Nacht"))))),
    )
    ui.deleteUIComponent("system:sitemap", TABLET)              // falls schon vorhanden
    println("anlegen: ${Json.get(ui.addUIComponent("system:sitemap", tablet), "uid")}")
    println("Sitemaps jetzt: "
        + Json.list(sitemaps.getSitemaps()).joinToString(", ") { it["name"].toString() })
    for (frame in Json.list(Json.get(sitemaps.getSitemap(TABLET), "homepage", "widgets"))) {
        println("  ${frame["label"]}: " + Json.list(frame["widgets"])
            .joinToString(", ") { "${it["label"]} (${it["type"]})" })
    }
    println("UI-Komponenten: " + Json.list(ui.getUIComponents("system:sitemap"))
        .joinToString(", ") { it["uid"].toString() })
    // Die Sitemap bleibt bestehen – B03_SitemapEreignisse nutzt sie.
}
