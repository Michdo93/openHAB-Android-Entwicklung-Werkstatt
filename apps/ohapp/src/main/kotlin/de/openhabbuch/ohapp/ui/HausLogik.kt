// Kapitel 17: Die Logik des ViewModels – reines Kotlin, ohne Android, gut testbar
package de.openhabbuch.ohapp.ui

import de.openhabbuch.ohapp.daten.SmartHome
import de.openhabbuch.ohapp.daten.Verbindung
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

const val TEMPERATUR = "Wohnzimmer_Temperatur"
const val HAUSTUER = "Flur_Haustuer"

/**
 * Macht aus der gespeicherten Verbindung einen Strom von Bildschirmzuständen:
 * erst laden, dann jeden Zustandswechsel einarbeiten. Ändert sich die Verbindung,
 * bricht flatMapLatest den alten Strom ab und beginnt mit dem neuen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun hausZustand(
    verbindung: Flow<Verbindung>,
    fabrik: (Verbindung) -> SmartHome,
    aktuell: (SmartHome) -> Unit = {},
): Flow<HausZustand> = verbindung.flatMapLatest { v ->
    if (!v.vollstaendig) flowOf(HausZustand.Einrichten)
    else beobachten(fabrik(v).also(aktuell))
}

private fun beobachten(home: SmartHome): Flow<HausZustand> = flow {
    emit(HausZustand.Laedt)
    val lichter = home.lichter().getOrElse {
        emit(HausZustand.Fehler(it.message ?: "unbekannter Fehler"))
        return@flow
    }
    var stand = HausZustand.Bereit(lichter,
        temperatur = home.zustand(TEMPERATUR).getOrDefault("–"),
        haustuer = home.zustand(HAUSTUER).getOrDefault("–"))
    emit(stand)
    // Observer: jeder Zustandswechsel aus openHAB ergibt einen neuen Bildschirmzustand
    home.wechsel().collect { w ->
        stand = when (w.item) {
            TEMPERATUR -> stand.copy(temperatur = w.neu)
            HAUSTUER -> stand.copy(haustuer = w.neu)
            else -> stand.copy(lichter = stand.lichter.map {
                if (it.name == w.item) it.copy(zustand = w.neu) else it
            })
        }
        emit(stand)
    }
}
