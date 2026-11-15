// Kapitel 8: Klassen, Vererbung, Schnittstellen, Data Classes und Aufzählungen
// Start: ./starte.sh kapitel08_kotlin/B04_Klassen.kt
package kapitel08_kotlin

// Eine Aufzählung: genau diese Werte gibt es
private enum class Zustand { AN, AUS, UNBEKANNT }

// Data Class: Konstruktor, equals, hashCode, toString und copy schreibt der Compiler
private data class Messwert(val item: String, val wert: Double, val einheit: String) {
    init {                                       // Prüfung im Konstruktor
        require(item.isNotBlank()) { "item fehlt" }
    }

    fun anzeige() = "%s: %.1f %s".format(item, wert, einheit)
}

// Eine Schnittstelle legt fest, was eine Klasse können muss
private interface Geraet {
    val name: String
    fun beschreibung() = "Gerät $name"           // Methode mit Rumpf
}

// Klassen sind in Kotlin "final" – abstract bzw. open erlaubt Unterklassen
private abstract class Sensor(override val name: String) : Geraet {
    protected var messungen = 0
    abstract fun messen(): Messwert
}

private class Temperatursensor(name: String, private val basis: Double) : Sensor(name) {
    override fun messen(): Messwert {
        messungen++
        return Messwert(name, basis + messungen * 0.1, "°C")
    }
}

private class Schalter(override val name: String) : Geraet {
    var zustand = Zustand.UNBEKANNT
        private set                              // lesen ja, setzen nur hier drin

    fun schalten(an: Boolean) {
        zustand = if (an) Zustand.AN else Zustand.AUS
    }

    override fun beschreibung() = "Schalter $name ($zustand)"
}

// object: genau eine Instanz – praktisch für Einstellungen und Hilfsfunktionen
private object Einstellungen {
    const val ADRESSE = "http://127.0.0.1:8080"
    var zugriffe = 0
}

fun main() {
    val werkstatt = Temperatursensor("Werkstatt_Temperatur", 18.0)
    val licht = Schalter("Wohnzimmer_Licht")
    licht.schalten(true)
    val geraete: List<Geraet> = listOf(werkstatt, licht)   // beide sind Geräte
    for (g in geraete) println(g.beschreibung())

    val m1 = werkstatt.messen()
    val m2 = werkstatt.messen()
    println(m1)                                  // toString der Data Class
    println("${m2.anzeige()}, Wert ${m2.wert}")
    println("gleich? ${m1 == Messwert("Werkstatt_Temperatur", 18.1, "°C")}")
    println("Kopie: ${m2.copy(wert = 20.0)}")
    val (item, wert) = m1                         // Zerlegung
    println("zerlegt: $item / $wert")

    println("Zustand ${licht.zustand}, alle: ${Zustand.entries}")
    Einstellungen.zugriffe++
    println("${Einstellungen.ADRESSE}, Zugriffe ${Einstellungen.zugriffe}")
    try {
        Messwert(" ", 1.0, "°C")
    } catch (e: IllegalArgumentException) {
        println("abgelehnt: ${e.message}")
    }
}
