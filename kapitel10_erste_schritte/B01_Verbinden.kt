// Kapitel 10: Verbindung zu openHAB herstellen – mit Token oder Basic Auth
package kapitel10_erste_schritte

import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import io.github.michdo93.openhab.Systeminfo
import io.github.michdo93.openhab.UUID
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val token = System.getenv("OPENHAB_TOKEN")

    val client = if (token != null) {
        OpenHABClient(url, token = token)                        // Variante 1: Token
    } else {
        OpenHABClient(url, System.getenv("OPENHAB_USER"),        // Variante 2:
            System.getenv("OPENHAB_PASSWORD"))                   // Basic Auth
    }
    if (!client.connect()) {                     // prüft, ob openHAB antwortet
        println("Keine Verbindung zu $url: ${client.loginError}")
        exitProcess(1)
    }

    // Die Wurzel der REST API: Version und Liste aller Endpunkte
    val wurzel = Json.map(client.get("/"))
    println("openHAB ${Json.get(wurzel, "runtimeInfo", "version")} unter $url")
    println("  Sprache/Land:   ${wurzel["locale"]}, "
        + "Maßsystem ${wurzel["measurementSystem"]}")
    println("  Endpunkte:      ${Json.array(wurzel["links"]).size}")

    val info = Json.map(Json.get(Systeminfo(client).getSystemInfo(), "systemInfo"))
    println("  Betriebssystem: ${info["osName"]} ${info["osVersion"]} "
        + "(${info["osArchitecture"]})")
    println("  Java (openHAB): ${info["javaVersion"]} von ${info["javaVendor"]}")
    println("  Konfiguration:  ${info["configFolder"]}")
    val frei = (info["freeMemory"] as Number).toLong() shr 20       // Bytes -> MiB
    val gesamt = (info["totalMemory"] as Number).toLong() shr 20
    println("  Speicher:       $frei von $gesamt MiB frei")
    println("  Instanz-UUID:   ${UUID(client).getUUID()}")
    println("  Java (hier):    ${System.getProperty("java.version")}"
        + " unter ${System.getProperty("os.name")}")
}
