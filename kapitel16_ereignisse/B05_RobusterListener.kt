// Kapitel 16: Ein robuster Ereignisstrom – Neuverbinden, Wachhund, sauberes Beenden
// Test: --trennen kappt die Verbindung nach 3 s, --dauer=20 beendet nach 20 s
package kapitel16_ereignisse

import io.github.michdo93.openhab.Events
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import io.github.michdo93.openhab.SSEConnection
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

private fun log(text: String) =
    println("${SimpleDateFormat("HH:mm:ss").format(Date())} $text")

/** Die aktuelle Verbindung – nur für den Test --trennen nach außen sichtbar. */
@Volatile
private var aktuell: SSEConnection? = null

/**
 * Ereignisse, die nie enden: verbindet bei jedem Abbruch neu (exponentielles Backoff
 * bis 30 s) und erkennt mit einem Wachhund Verbindungen, die still gestorben sind
 * (openHAB schickt alle 10 s ein ALIVE). Abbrechen beendet den Flow sofort.
 */
fun robusteEreignisse(events: Events, topics: String, wachhundMs: Long = 25_000):
        Flow<Map<String, Any?>> = flow {
    var pause = 1000L
    while (true) {
        val strom = events.getEvents(topics)
        if (strom.isError) {
            val text = Json.error(strom.error)!!.substringBefore(" (http")  // ohne Adresse
            log("keine Verbindung: $text – neuer Versuch in ${pause / 1000} s")
        } else {
            log("verbunden")
            pause = 1000                                  // Erfolg: Wartezeit zurücksetzen
            aktuell = strom
            coroutineScope {
                var letzte = System.currentTimeMillis()
                val wache = launch {                     // Wachhund: Stille = tot
                    while (true) {
                        delay(1000)
                        if (System.currentTimeMillis() - letzte > wachhundMs) {
                            log("Wachhund: Stille – Verbindung wird neu aufgebaut")
                            strom.close()
                        }
                    }
                }
                strom.events(includeAlive = true).collect { e ->
                    letzte = System.currentTimeMillis()
                    if (e["topic"] != null) emit(e)
                }
                wache.cancel()
            }
            log("Verbindung beendet")
        }
        delay(pause)
        pause = minOf(pause * 2, 30_000)
    }
}

fun main(args: Array<String>) = runBlocking<Unit> {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val events = Events(OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN")))
    val dauer = args.firstOrNull { it.startsWith("--dauer=") }?.substring(8)?.toLong()

    val listener = launch {
        try {
            withTimeoutOrNull(if (dauer != null) dauer * 1000 else Long.MAX_VALUE) {
                robusteEreignisse(events, "openhab/items/*/statechanged").collect { e ->
                    log("${e["topic"].toString().split("/")[2]} = "
                        + Json.get(e, "payload", "value"))
                }
            }
        } finally {
            log("Listener beendet")
        }
    }
    if ("--trennen" in args) launch {
        delay(3000)
        log("TEST: Verbindung gekappt")
        aktuell?.close()
    }
    // Strg+C oder kill: den Listener abbrechen und auf sein Ende warten
    val hook = Thread {
        if (listener.isActive) {
            log("Signal – beende den Listener")
            listener.cancel()
            runBlocking { withTimeoutOrNull(5000) { listener.join() } }
        }
    }
    Runtime.getRuntime().addShutdownHook(hook)
    listener.join()
    coroutineContext.cancelChildren()             // z. B. den Test --trennen
    // normales Ende: Hook abmelden (nicht möglich, wenn das Beenden schon läuft)
    runCatching { Runtime.getRuntime().removeShutdownHook(hook) }
}
