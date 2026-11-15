// Kapitel 15: Text wie einen Sprachbefehl auswerten lassen – ohne Mikrofon
// Eigene Sätze:
//   ./starte.sh kapitel15_oberflaechen/B04_Sprache.kt "Schalte die Leselampe ein"
package kapitel15_oberflaechen

import io.github.michdo93.openhab.Audio
import io.github.michdo93.openhab.Items
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import io.github.michdo93.openhab.Services
import io.github.michdo93.openhab.Voice
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val client = OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN"))
    val voice = Voice(client)
    val items = Items(client)
    val services = Services(client)

    // Er erkennt Items an ihrem Label und antwortet in der gewählten Sprache
    suspend fun sage(satz: String): String {
        val antwort = voice.interpretText(satz, "de")
        return if (!Json.isError(antwort)) antwort.toString()
        else Json.error(antwort)!!.substringAfterLast('(').removeSuffix(")")
    }

    for (i in Json.list(voice.getInterpreters("de"))) {
        println("Interpreter %-8s %-26s Sprachen %s"
            .format(i["id"], i["label"], i["locales"]))
    }
    println("Audio-Ausgänge: "
        + Json.list(Audio(client).getSinks()).joinToString(", ") { it["id"].toString() })

    // Den eingebauten Interpreter zum Standard machen – eine Systemeinstellung
    val config = Json.map(services.getServiceConfig("org.openhab.voice")) +
        ("defaultHLI" to "system")
    services.updateServiceConfig("org.openhab.voice", config)
    println("Standard-Interpreter: "
        + Json.get(services.getServiceConfig("org.openhab.voice"), "defaultHLI"))

    items.sendCommand("Wohnzimmer_Licht", "OFF")
    items.sendCommand("Wohnzimmer_Dimmer", "60")
    for (satz in listOf("Schalte das Deckenlicht ein", "Schalte das Deckenlicht ein",
                        "Schalte die Leselampe aus", "Mach mir einen Kaffee")) {
        println("  »$satz« -> ${sage(satz)}")
    }
    delay(500)
    println("Deckenlicht: ${items.getItemState("Wohnzimmer_Licht")}"
        + " | Leselampe: ${items.getItemState("Wohnzimmer_Dimmer")}")

    // Einen bestimmten Interpreter wählen, unabhängig vom Standard
    println(voice.interpretTextBatch("turn off the Deckenlicht", listOf("system"), "en"))

    for (satz in args) println("  »$satz« -> ${sage(satz)}")    // eigene Sätze
}
