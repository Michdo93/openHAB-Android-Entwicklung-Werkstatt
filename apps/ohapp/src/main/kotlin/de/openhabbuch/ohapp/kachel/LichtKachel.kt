// Kapitel 20: Eine Kachel in den Schnelleinstellungen – das Licht aus der Statusleiste
package de.openhabbuch.ohapp.kachel

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import de.openhabbuch.ohapp.OhApp
import de.openhabbuch.ohapp.daten.OpenHABSmartHome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LichtKachel : TileService() {
    // eigener Scope: endet mit dem Dienst
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val item = "Wohnzimmer_Licht"

    private suspend fun home() = (application as OhApp).einstellungen.verbindung.first()
        .takeIf { it.vollstaendig }?.let { OpenHABSmartHome(it) }

    /** Die Schnelleinstellungen werden geöffnet: aktuellen Zustand zeigen. */
    override fun onStartListening() {
        scope.launch { anzeigen(home()?.zustand(item)?.getOrNull()) }
    }

    /** Antippen: umschalten und den neuen Zustand zeigen. */
    override fun onClick() {
        scope.launch {
            val home = home() ?: return@launch
            val neu = if (qsTile.state == Tile.STATE_ACTIVE) "OFF" else "ON"
            anzeigen(if (home.befehl(item, neu) == null) neu else null)
        }
    }

    private fun anzeigen(zustand: String?) {
        val kachel = qsTile ?: return
        kachel.label = "Deckenlicht"
        kachel.state = when (zustand) {
            "ON" -> Tile.STATE_ACTIVE
            "OFF" -> Tile.STATE_INACTIVE
            else -> Tile.STATE_UNAVAILABLE                // keine Verbindung
        }
        kachel.updateTile()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
