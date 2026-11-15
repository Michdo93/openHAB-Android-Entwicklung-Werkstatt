// Kapitel 9: Abbrechen, Zeitlimits und Dispatchers
// Start: ./starte.sh kapitel09_coroutines/B02_Abbrechen.kt
package kapitel09_coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

// Blockierende Arbeit (Datei, Netz ohne suspend) gehört auf Dispatchers.IO
private suspend fun langsamLesen(): String = withContext(Dispatchers.IO) {
    Thread.sleep(200)                             // blockiert – aber nur einen IO-Thread
    "gelesen auf ${Thread.currentThread().name.substringBefore(" @")}"
}

fun main() = runBlocking {
    // Ein Job lässt sich jederzeit abbrechen
    val beobachter = launch {
        try {
            var runde = 0
            while (true) {
                println("  beobachte (${++runde})")
                delay(150)                        // hier wird der Abbruch bemerkt
            }
        } catch (e: CancellationException) {
            println("  abgebrochen")
            throw e                               // Abbruch nie verschlucken
        } finally {
            println("  aufgeräumt")               // läuft in jedem Fall
        }
    }
    delay(400)
    beobachter.cancel()
    beobachter.join()

    // Zeitlimits
    val schnell = withTimeoutOrNull(500) { delay(100); "rechtzeitig" }
    val langsam = withTimeoutOrNull(500) { delay(1000); "zu spät" }
    println("schnell: $schnell, langsam: $langsam")
    try {
        withTimeout(200) { delay(1000) }
    } catch (e: CancellationException) {
        println("Zeitüberschreitung: ${e.javaClass.simpleName}")
    }

    // Dispatchers: wo eine Coroutine läuft
    println(langsamLesen())
    withContext(Dispatchers.Default) {
        println("rechnen auf ${Thread.currentThread().name.substringBefore(" @")}")
    }
    println("zurück auf ${Thread.currentThread().name.substringBefore(" @")}")
}
