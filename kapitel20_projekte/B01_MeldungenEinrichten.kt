// Kapitel 20: Festlegen, welche Items der Wächter der App meldet – mit Metadaten
// Start: ./starte.sh kapitel20_projekte/B01_MeldungenEinrichten.kt
package kapitel20_projekte

import io.github.michdo93.openhab.Items
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val items = Items(OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN")))

    // Namensraum "ohapp", Wert "melden"; "wenn" schränkt auf einen Zustand ein
    println(items.addMetadata("Flur_Haustuer", "ohapp",
        mapOf("value" to "melden", "config" to mapOf("wenn" to "OPEN"))))
    println(items.addMetadata("Haus_Status", "ohapp", mapOf("value" to "melden")))

    // Was der Wächter beim Start liest:
    val antwort = items.getItems(metadata = "ohapp", fields = "name,label,metadata")
    for (i in Json.list(antwort).filter { Json.get(it, "metadata", "ohapp") != null }) {
        val m = Json.get(i, "metadata", "ohapp")
        println("%-14s %-8s wenn=%s".format(i["name"], Json.get(m, "value"),
            Json.get(m, "config", "wenn") ?: "(jede Änderung)"))
    }
}
