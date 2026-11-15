// Kapitel 15: Sitemaps lesen – die Bedienseiten, die Apps und Basic UI zeigen
package kapitel15_oberflaechen

import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import io.github.michdo93.openhab.Sitemaps
import kotlinx.coroutines.runBlocking

// Rekursiv: jedes Widget mit Typ, Beschriftung und Zustand, Unter-Widgets eingerückt
private fun zeige(widgets: Any?, tiefe: Int) {
    for (w in Json.list(widgets)) {
        val wert = w["item"]?.let { "= ${Json.get(it, "state")}" } ?: ""
        println("%s%-12s %-28s %s".format("  ".repeat(tiefe), w["type"], w["label"], wert)
            .trimEnd())
        w["widgets"]?.let { zeige(it, tiefe + 1) }
    }
}

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val sitemaps = Sitemaps(OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN")))

    for (s in Json.list(sitemaps.getSitemaps())) {
        println("Sitemap ${s["name"]}: ${s["label"]}")
    }
    val seite = Json.get(sitemaps.getSitemap("restbuch", language = "de"), "homepage")
    println("\nStartseite »${Json.get(seite, "title")}«:")
    zeige(Json.get(seite, "widgets"), 1)
}
