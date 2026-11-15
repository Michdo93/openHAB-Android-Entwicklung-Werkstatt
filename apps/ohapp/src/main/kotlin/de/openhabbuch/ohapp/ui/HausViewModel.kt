// Kapitel 17: Das ViewModel – hält den Zustand des Bildschirms und nimmt Befehle an
package de.openhabbuch.ohapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.openhabbuch.ohapp.daten.Einstellungen
import de.openhabbuch.ohapp.daten.Licht
import de.openhabbuch.ohapp.daten.SmartHome
import de.openhabbuch.ohapp.daten.Verbindung
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HausViewModel(
    verbindung: Flow<Verbindung>,
    fabrik: (Verbindung) -> SmartHome,
    private val einstellungen: Einstellungen? = null,
) : ViewModel() {

    @Volatile
    private var home: SmartHome? = null

    /**
     * Der Zustand des Bildschirms. WhileSubscribed(5000): Ist die App fünf Sekunden
     * unsichtbar, endet der Strom – und mit ihm die Verbindung zu openHAB.
     */
    val zustand: StateFlow<HausZustand> = hausZustand(verbindung, fabrik) { home = it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HausZustand.Laedt)

    private val _meldung = MutableStateFlow<String?>(null)

    /** Eine kurze Meldung für die Oberfläche, z. B. ein abgelehnter Command. */
    val meldung: StateFlow<String?> = _meldung.asStateFlow()

    /** Command per REST – den neuen Zustand meldet danach der Ereignisstrom. */
    fun schalten(licht: Licht, an: Boolean) = senden(licht.name, if (an) "ON" else "OFF")

    fun dimmen(licht: Licht, prozent: Int) = senden(licht.name, prozent.toString())

    fun meldungGelesen() {
        _meldung.value = null
    }

    fun speichern(v: Verbindung) {
        viewModelScope.launch { einstellungen?.speichern(v) }
    }

    private fun senden(item: String, command: String) {
        val h = home ?: return
        viewModelScope.launch {
            h.befehl(item, command)?.let { _meldung.value = "$item: $it" }
        }
    }
}
