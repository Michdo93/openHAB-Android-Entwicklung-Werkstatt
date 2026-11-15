// Kapitel 19: Eine Attrappe der Hardware – das Gerät lässt sich ohne Smartphone testen
package de.openhabbuch.geraet

class FakeHardware(var messung: Messung = Messung(87, false, 120f, -58)) : Hardware {
    val protokoll = mutableListOf<String>()
    override fun messen() = messung
    override fun lampe(an: Boolean) { protokoll += "lampe $an" }
    override fun vibrieren(ms: Long) { protokoll += "vibrieren $ms" }
    override fun sagen(text: String) { protokoll += "sagen $text" }
}
