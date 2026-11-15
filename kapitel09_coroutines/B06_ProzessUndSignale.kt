// Kapitel 9: Umgebung, Kindprozess und sauberes Beenden
// Läuft, bis Strg+C gedrückt wird; DEMO_SEKUNDEN=3 beendet nach 3 Sekunden.
package kapitel09_coroutines

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() = runBlocking<Unit> {
    // Umgebungsvariablen und Systemeigenschaften
    println("openHAB:  ${System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"}")
    val token = System.getenv("OPENHAB_TOKEN")
    println("Token:    ${if (token != null) "gesetzt" else "fehlt"}")
    val java = System.getProperty("java.version")
    val kerne = Runtime.getRuntime().availableProcessors()
    println("Java $java auf ${System.getProperty("os.name")}, $kerne Prozessorkern(e)")

    // ein anderes Programm starten und seine Ausgabe lesen
    val p = ProcessBuilder("uname", "-sr").redirectErrorStream(true).start()
    val ausgabe = p.inputStream.bufferedReader().readText().trim()
    println("uname:    $ausgabe (Code ${p.waitFor()})")

    // die eigentliche Arbeit läuft als Coroutine ...
    val sekunden = System.getenv("DEMO_SEKUNDEN")?.toInt() ?: 0
    val dienst = launch {
        var runde = 0
        try {
            while (isActive && (sekunden == 0 || runde < sekunden)) {
                println("Runde ${++runde}")
                delay(1000)
            }
        } finally {
            println("Dienst beendet nach $runde Runden")
        }
    }
    // ... und Strg+C (SIGINT) oder kill (SIGTERM) bricht sie ab
    val hook = Thread {
        println("Signal empfangen – räume auf ...")
        dienst.cancel()
        Thread.sleep(500)                          // dem Dienst Zeit zum Aufräumen geben
    }
    Runtime.getRuntime().addShutdownHook(hook)
    dienst.join()
    // normales Ende: Hook abmelden (nicht möglich, wenn das Beenden schon läuft)
    runCatching { Runtime.getRuntime().removeShutdownHook(hook) }
}
