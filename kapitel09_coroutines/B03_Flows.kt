// Kapitel 9: Flows – Datenströme und das Beobachter-Muster mit StateFlow
// Start: ./starte.sh kapitel09_coroutines/B03_Flows.kt
package kapitel09_coroutines

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// Ein "kalter" Flow: Er erzeugt seine Werte erst, wenn jemand sammelt
private fun messwerte(): Flow<Double> = flow {
    for (w in listOf(19.5, 21.0, 22.5, 18.0)) {
        delay(100)
        emit(w)
    }
}

// Ein Sensor mit Beobachtern: StateFlow hält den aktuellen Wert,
// SharedFlow verteilt Ereignisse an alle, die gerade zuhören
private class Sensor(val name: String) {
    private val _wert = MutableStateFlow(0.0)
    val wert: StateFlow<Double> = _wert.asStateFlow()     // nur lesen von außen
    private val _warnungen = MutableSharedFlow<String>()
    val warnungen = _warnungen.asSharedFlow()

    suspend fun melden(neu: Double) {
        _wert.value = neu
        if (neu > 20) _warnungen.emit("$name zu warm: $neu °C")
    }
}

fun main() = runBlocking {
    // Operatoren wie bei Listen – aber Wert für Wert, sobald er kommt
    messwerte().filter { it > 20 }.map { "%.1f °C".format(it) }
        .collect { println("warm: $it") }
    println("erste zwei: ${messwerte().take(2).toList()}")

    val sensor = Sensor("Werkstatt_Temperatur")
    val anzeige = launch { sensor.wert.collect { println("Anzeige:  $it °C") } }
    val warnung = launch { sensor.warnungen.collect { println("Warnung:  $it") } }
    delay(50)                                     // Beobachter sind angemeldet
    // 21.0 zweimal: StateFlow meldet nur Änderungen
    for (w in listOf(19.5, 21.0, 21.0, 22.0)) {
        sensor.melden(w)
        delay(50)
    }
    warnung.cancel()                              // abmelden
    sensor.melden(23.0)
    delay(50)
    anzeige.cancel()
    println("aktueller Wert: ${sensor.wert.value}, erster über 22: "
        + messwerte().first { it > 22 })
}
