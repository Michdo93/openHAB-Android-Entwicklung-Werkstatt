// Kapitel 9: Coroutines – launch, async und strukturierte Nebenläufigkeit
// Start: ./starte.sh kapitel09_coroutines/B01_Coroutines.kt
package kapitel09_coroutines

import kotlin.system.measureTimeMillis
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// suspend: Die Funktion darf warten, ohne einen Thread zu blockieren
private suspend fun messen(sensor: String, dauer: Long): Double {
    delay(dauer)                                 // wartet, gibt den Thread frei
    return 18.0 + sensor.length / 10.0
}

// coroutineScope: Die Funktion endet erst, wenn alle ihre Coroutines fertig sind
private suspend fun alleMessen(sensoren: List<String>): Map<String, Double> =
    coroutineScope {
        val laufend = sensoren.map { s -> async { s to messen(s, 300) } }  // gleichzeitig
        laufend.map { it.await() }.toMap()
    }

// runBlocking verbindet die normale Welt mit der Welt der Coroutines
fun main() = runBlocking {
    // launch: "starte das und mach weiter" – ohne Ergebnis
    val job = launch {
        repeat(3) { i ->
            println("  Hintergrund $i")
            delay(100)
        }
    }
    println("main läuft weiter")
    job.join()                                   // warten, bis der Job fertig ist

    // async: startet eine Berechnung, await liefert das Ergebnis
    val zeit = measureTimeMillis {
        val a = async { messen("Werkstatt_Temperatur", 500) }
        val b = async { messen("Wohnzimmer_Temperatur", 500) }
        println("Werkstatt %.1f, Wohnzimmer %.1f".format(a.await(), b.await()))
    }
    println("beide zusammen in etwa ${zeit / 100 * 100} ms statt 1000 ms")

    val alle = alleMessen(listOf("Bad", "Flur", "Küche"))
    println("alle: $alle")

    // 10 000 Coroutines sind kein Problem – sie sind viel leichter als Threads
    val zaehler = (1..10_000).map { async { delay(100); 1 } }.sumOf { it.await() }
    println("$zaehler Coroutines fertig")
}
