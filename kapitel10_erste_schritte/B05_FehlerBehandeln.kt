// Kapitel 10: Fehler behandeln
package kapitel10_erste_schritte

import io.github.michdo93.openhab.Items
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import kotlinx.coroutines.runBlocking

fun main() = runBlocking<Unit> {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val items = Items(OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN")))

    // Alle Methoden melden Ablehnungen als Ergebnis {error=…} – sie werfen nicht
    val versuche = listOf(
        "Wohnzimer_Licht" to "ON",       // Tippfehler im Namen
        "Wohnzimmer_Licht" to "HALB",    // ungültiger Command
        "Flur_Haustuer" to "OPEN",       // Contact: kein Command
        "Wohnzimmer_Licht" to "OFF",
    )
    for ((item, befehl) in versuche) {
        val antwort = items.sendCommand(item, befehl)
        val ergebnis = if (Json.isError(antwort)) "FEHLER  ${Json.error(antwort)}" else "OK"
        println("$item <- $befehl: $ergebnis")
    }

    // Ein Item aus einer .items-Datei lässt sich steuern, aber nicht löschen
    println("\nlöschen: ${items.deleteItem("Wohnzimmer_Licht")}")

    // Falsches Token: openHAB lehnt ab, die Bibliothek reicht die Begründung weiter
    val fremd = OpenHABClient(url, token = "oh.falsch.123")
    println("Verbindung: ${fremd.connect()} – ${fremd.loginError}")
    println("falsches Token: ${Json.error(Items(fremd).getItemState("Wohnzimmer_Licht"))}")

    // Netzwerkfehler: ein Server, den es nicht gibt, zwei Sekunden Wartezeit
    val weg = OpenHABClient("http://127.0.0.1:9", timeout = 2.0)
    println("anderer Server: ${Items(weg).getItemState("Wohnzimmer_Licht")}")

    // Wer lieber mit Ausnahmen arbeitet: Json.map wirft bei einem Fehlerergebnis
    try {
        Json.map(items.getItem("Gibtsnicht"))
    } catch (e: IllegalStateException) {
        println("Ausnahme: ${e.message}")
    }
}
