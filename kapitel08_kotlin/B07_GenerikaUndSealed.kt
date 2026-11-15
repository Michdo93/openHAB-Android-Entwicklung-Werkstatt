// Kapitel 8: Generische Typen und versiegelte Klassen
// Start: ./starte.sh kapitel08_kotlin/B07_GenerikaUndSealed.kt
package kapitel08_kotlin

// Eine generische Klasse: T steht für einen beliebigen Typ
private class Puffer<T>(private val groesse: Int) {
    private val werte = ArrayDeque<T>()

    fun hinzu(wert: T) {
        if (werte.size == groesse) werte.removeFirst()   // ältesten Wert verwerfen
        werte.addLast(wert)
    }

    fun inhalt(): List<T> = werte.toList()
}

// Eine generische Funktion mit Einschränkung
private fun <T : Comparable<T>> groesster(werte: List<T>): T =
    werte.reduce { a, b -> maxOf(a, b) }

// sealed: Es gibt genau diese drei Arten von Ereignissen
private sealed interface Ereignis
private data class Zustandswechsel(val item: String, val alt: String, val neu: String) :
    Ereignis
private data class Befehl(val item: String, val befehl: String) : Ereignis
private data object Lebenszeichen : Ereignis

// when über eine versiegelte Hierarchie braucht kein else:
// der Compiler kennt alle Fälle
private fun beschreiben(e: Ereignis) = when (e) {
    is Zustandswechsel -> "${e.item}: ${e.alt} -> ${e.neu}"
    is Befehl -> "${e.item} soll ${e.befehl}"
    Lebenszeichen -> "(lebt)"
}

fun main() {
    val puffer = Puffer<Double>(3)
    for (t in listOf(18.0, 18.5, 19.0, 19.5)) puffer.hinzu(t)
    println("Puffer: ${puffer.inhalt()}, größter: ${groesster(puffer.inhalt())}")
    println("größter Name: ${groesster(listOf("Flur", "Wohnzimmer", "Küche"))}")

    val ereignisse = listOf(
        Befehl("Wohnzimmer_Licht", "ON"),
        Zustandswechsel("Wohnzimmer_Licht", "OFF", "ON"),
        Lebenszeichen,
    )
    ereignisse.forEach { println(beschreiben(it)) }
}
