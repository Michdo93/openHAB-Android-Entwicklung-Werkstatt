// Kapitel 17: Adresse und Token – aus den Einstellungen der App
package de.openhabbuch.ohapp.daten

data class Verbindung(val url: String, val token: String) {
    val vollstaendig: Boolean get() = url.startsWith("http") && token.isNotBlank()
}
