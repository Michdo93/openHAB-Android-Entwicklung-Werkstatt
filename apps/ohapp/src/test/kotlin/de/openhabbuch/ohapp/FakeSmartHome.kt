// Kapitel 19: Eine Attrappe für SmartHome – ohne Netz, vollständig steuerbar
package de.openhabbuch.ohapp

import de.openhabbuch.ohapp.daten.Licht
import de.openhabbuch.ohapp.daten.SmartHome
import de.openhabbuch.ohapp.daten.Wechsel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeSmartHome(
    private val lichterErgebnis: Result<List<Licht>> = Result.success(listOf(
        Licht("Wohnzimmer_Licht", "Deckenlicht", "Switch", "OFF"),
        Licht("Wohnzimmer_Dimmer", "Leselampe", "Dimmer", "40"))),
) : SmartHome {
    /** Was der Test "aus openHAB" melden lässt. */
    val ereignisse = MutableSharedFlow<Wechsel>()

    /** Welche Commands die App gesendet hat. */
    val gesendet = mutableListOf<Pair<String, String>>()

    override suspend fun lichter() = lichterErgebnis
    override suspend fun zustand(item: String) = Result.success(
        if (item == "Flur_Haustuer") "CLOSED" else "21.5 °C")

    override suspend fun befehl(item: String, command: String): String? {
        gesendet += item to command
        return if (item == "Gibtsnicht") "Item not found." else null
    }

    override fun wechsel(): Flow<Wechsel> = ereignisse
}
