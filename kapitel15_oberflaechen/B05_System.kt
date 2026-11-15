// Kapitel 15: Systemdienste, Protokollierung, Zugänge und Symbole
package kapitel15_oberflaechen

import io.github.michdo93.openhab.Auth
import io.github.michdo93.openhab.Iconsets
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.Logging
import io.github.michdo93.openhab.OpenHABClient
import io.github.michdo93.openhab.Services
import io.github.michdo93.openhab.Systeminfo
import kotlinx.coroutines.runBlocking

private const val LOGGER = "org.openhab.binding.astro"

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val client = OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN"))
    val services = Services(client)
    val logging = Logging(client)
    val auth = Auth(client)
    suspend fun level() = Json.get(logging.getLogger(LOGGER), "loggers", 0, "level")

    // 1. Systemdienste und ihre Einstellungen (dieselben wie in der Main UI)
    for (s in Json.list(services.getServices("de")).filter { it["category"] == "system" }) {
        println("  %-26s %s".format(s["id"], s["label"]))
    }
    println("Regionale Einstellungen: ${services.getServiceConfig("org.openhab.i18n")}")

    // 2. Log-Level eines Bindings zur Fehlersuche anheben – und zurücksetzen
    println("\nvorher: ${level()}")
    logging.modifyOrAddLogger(LOGGER, "DEBUG")
    println("jetzt:  ${level()}")
    logging.removeLogger(LOGGER)                          // zurück auf den Standard
    println("danach: ${level()}")

    // 3. Wer hat Zugang? API-Tokens und Sitzungen des angemeldeten Benutzers
    for (t in Json.list(auth.getAPITokens())) {
        println("\nToken \"%s\" (Bereich %s) seit %s".format(t["name"], t["scope"],
            t["createdTime"].toString().take(10)))
    }
    println("offene Sitzungen: ${Json.list(auth.getSessions()).size}")

    // 4. Symbolsätze und Maßeinheiten
    println("Symbolsätze: " + Json.list(Iconsets(client).getIconsets("de"))
        .joinToString(", ") { it["label"].toString() })
    val uom = Systeminfo(client).getUoMInfo()
    val groessen = Json.list(Json.get(uom, "uomInfo", "dimensions"))
    println("${groessen.size} physikalische Größen, z. B. " + groessen.take(3)
        .joinToString(", ") { "${it["dimension"]} [${it["systemUnit"]}]" })
}
