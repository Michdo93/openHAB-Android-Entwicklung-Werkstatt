// Kapitel 9: Unter der Haube – die REST API von Hand und mit der Bibliothek
// Start: ./starte.sh kapitel09_coroutines/B05_HttpRoh.kt
package kapitel09_coroutines

import io.github.michdo93.openhab.Items
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import java.net.HttpURLConnection
import java.net.URI
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val token = System.getenv("OPENHAB_TOKEN")

    // 1. von Hand: Verbindung öffnen, Kopfzeilen setzen, Status prüfen, Text zerlegen
    val adresse = URI("$url/rest/items/Wohnzimmer_Licht").toURL()
    val c = adresse.openConnection() as HttpURLConnection
    c.setRequestProperty("Authorization", "Bearer $token")
    c.setRequestProperty("Accept", "application/json")
    println("Status ${c.responseCode}, ${c.contentType}")
    val item = Json.map(Json.parse(c.inputStream.bufferedReader().readText()))
    println("von Hand:   ${item["name"]} = ${item["state"]}")

    val befehl = adresse.openConnection() as HttpURLConnection
    befehl.requestMethod = "POST"
    befehl.doOutput = true
    befehl.setRequestProperty("Authorization", "Bearer $token")
    befehl.setRequestProperty("Content-Type", "text/plain")
    befehl.outputStream.use { it.write("OFF".toByteArray()) }
    println("Command:    Status ${befehl.responseCode}")

    // 2. dasselbe mit der Bibliothek – suspend-Funktionen, daher in runBlocking
    val items = Items(OpenHABClient(url, token = token))
    println("Bibliothek: ${Json.str(items.getItem("Wohnzimmer_Licht"), "state")}")
    println("Command:    ${items.sendCommand("Wohnzimmer_Licht", "ON")}")
    println("Fehler:     ${items.getItem("Gibtsnicht")}")
}
