// Kapitel 8: Verzweigungen, Schleifen und when
// Start: ./starte.sh kapitel08_kotlin/B02_Kontrollfluss.kt
package kapitel08_kotlin

fun main() {
    val temperatur = 23.4
    // if ist ein Ausdruck: Es liefert einen Wert
    val gefuehl = if (temperatur > 24) "warm"
        else if (temperatur > 19) "angenehm"
        else "kühl"
    println(gefuehl)

    // when: die Fallunterscheidung von Kotlin – liefert ebenfalls einen Wert
    val zustand = "OPEN"
    val anzeige = when (zustand) {
        "OPEN" -> "offen"
        "CLOSED" -> "geschlossen"
        "NULL", "UNDEF" -> "unbekannt"
        else -> "? $zustand"
    }
    println("Haustür: $anzeige")

    // Schleifen über Sammlungen und Bereiche
    val raeume = listOf("Wohnzimmer", "Küche", "Werkstatt")
    for (raum in raeume) println("- $raum")
    for (i in raeume.indices step 2) println("$i: ${raeume[i]}")
    for ((i, raum) in raeume.withIndex()) if (raum.length > 8) println("lang: $i $raum")
    var versuch = 0
    while (versuch < 3) {
        versuch++
        if (versuch == 2) continue                // nächster Durchlauf
        println("Versuch $versuch")
    }

    // Bereiche und Bedingungen in when
    val helligkeit = 55
    println(when (helligkeit) {
        0 -> "aus"
        in 1..49 -> "gedimmt"
        in 50..99 -> "hell"
        else -> "voll"
    })

    // is prüft den Typ – danach kennt der Compiler ihn ("Smart Cast")
    val wert: Any = 55L                            // Zahlen aus JSON sind Long
    if (wert is Long && wert > 50) println("Long über 50: $wert")
    for (o in listOf<Any?>("ON", 21.5, 3L, listOf("Light"), null)) {
        val art = when (o) {
            is String -> "Text $o"
            is Double -> "Kommazahl $o"
            is Long -> if (o < 10) "kleine Zahl $o" else "Zahl $o"
            is List<*> -> "Liste mit ${o.size} Element(en)"
            null -> "nichts"
            else -> "etwas anderes"
        }
        println(art)
    }
}
