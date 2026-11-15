// Kapitel 8: Funktionen, Vorgabewerte, benannte Argumente, Lambdas
// Start: ./starte.sh kapitel08_kotlin/B03_Funktionen.kt
package kapitel08_kotlin

// Eine Funktion mit Ergebnis – als Ausdruck geschrieben
fun celsiusZuFahrenheit(celsius: Double) = celsius * 9 / 5 + 32

// Vorgabewerte: Wer den Parameter weglässt, bekommt "ON".
// Genau so bildet die Bibliothek die optionalen Parameter von Python ab.
fun schalten(item: String, befehl: String = "ON", quelle: String? = null): String =
    "$item <- $befehl" + (quelle?.let { " (von $it)" } ?: "")

// Variable Anzahl von Argumenten
fun mittelwert(vararg werte: Double) = if (werte.isEmpty()) 0.0 else werte.average()

// Eine Funktion, die eine Funktion als Parameter bekommt
fun filtern(namen: List<String>, bedingung: (String) -> Boolean): List<String> {
    val treffer = mutableListOf<String>()
    for (n in namen) if (bedingung(n)) treffer += n
    return treffer
}

// Erweiterungsfunktion: String bekommt eine neue Methode
fun String.raum(): String = substringBefore('_')

fun main() {
    println("21.5 °C = ${celsiusZuFahrenheit(21.5)} °F")
    println(schalten("Wohnzimmer_Licht"))
    println(schalten("Wohnzimmer_Dimmer", "40"))
    println(schalten("Wohnzimmer_Dimmer", quelle = "app"))   // benanntes Argument
    println("Mittelwert: ${mittelwert(18.0, 21.0, 19.5)}")

    val items = listOf("Wohnzimmer_Licht", "Kueche_Rollladen", "Wohnzimmer_Dimmer")
    // Lambda als letztes Argument steht hinter der Klammer; it = der eine Parameter
    println(filtern(items) { it.startsWith("Wohnzimmer") })
    println("Räume: ${items.map { it.raum() }.distinct()}")

    // Funktionen als Werte und Funktionsreferenzen
    val laenge: (String) -> Int = String::length
    println("Länge: ${laenge("Flur_Haustuer")}")
    val meldung = { println("Fertig.") }      // Rückruffunktion ohne Parameter
    meldung()
}
