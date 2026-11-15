// Kapitel 18: Was das Smartphone als Gerät kann – als Schnittstelle
package de.openhabbuch.geraet

/** Eine Messung aller Sensoren. null: diesen Sensor gibt es nicht. */
data class Messung(
    val akku: Int,                 // Prozent
    val laedt: Boolean,
    val helligkeit: Float?,        // Lux, vom Umgebungslichtsensor
    val wlan: Int?,                // Signalstärke in dBm
)

interface Hardware {
    fun messen(): Messung
    fun lampe(an: Boolean)
    fun vibrieren(ms: Long)
    fun sagen(text: String)
}
