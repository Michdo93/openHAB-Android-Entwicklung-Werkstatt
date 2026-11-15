// Kapitel 17: Das Model der App – was sie über das Haus wissen und tun kann
package de.openhabbuch.ohapp.daten

import kotlinx.coroutines.flow.Flow

/** Ein schaltbares Licht, wie die Oberfläche es braucht. */
data class Licht(
    val name: String, val label: String, val typ: String, val zustand: String,
) {
    /** An oder aus – bei Dimmer und Farblicht zählt die Helligkeit. */
    val an: Boolean get() = zustand == "ON" || helligkeit > 0

    /** Helligkeit 0–100; beim Farblicht ist sie der dritte Wert von "H,S,B". */
    val helligkeit: Int
        get() = when {
            zustand == "ON" -> 100
            typ == "Color" -> zustand.substringAfterLast(',').toDoubleOrNull()?.toInt() ?: 0
            else -> zustand.toDoubleOrNull()?.toInt() ?: 0
        }
}

/** Ein Zustandswechsel aus dem Ereignisstrom. */
data class Wechsel(val item: String, val neu: String)

/**
 * Alles, was die App vom Smart Home braucht – als Schnittstelle. Die echte Umsetzung
 * spricht mit openHAB (OpenHABSmartHome), die Tests verwenden eine Attrappe.
 */
interface SmartHome {
    /** Alle Items mit dem Tag "Light". */
    suspend fun lichter(): Result<List<Licht>>

    /** Der Zustand eines Items als Text, z. B. "21.5 °C". */
    suspend fun zustand(item: String): Result<String>

    /** Sendet einen Command; liefert bei Erfolg null, sonst die Fehlermeldung. */
    suspend fun befehl(item: String, command: String): String?

    /** Alle Zustandswechsel – ein Flow, der nach Abbrüchen von selbst neu verbindet. */
    fun wechsel(): Flow<Wechsel>
}
