// Kapitel 17: Was der Bildschirm zeigt – als versiegelte Klasse
package de.openhabbuch.ohapp.ui

import de.openhabbuch.ohapp.daten.Licht

sealed interface HausZustand {
    /** Noch keine Adresse oder kein Token gespeichert. */
    data object Einrichten : HausZustand

    data object Laedt : HausZustand

    data class Fehler(val text: String) : HausZustand

    data class Bereit(
        val lichter: List<Licht>,
        val temperatur: String,
        val haustuer: String,
    ) : HausZustand
}
