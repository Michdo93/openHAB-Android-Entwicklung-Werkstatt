// Kapitel 8: Werte, Typen und Null-Sicherheit
// Start: ./starte.sh kapitel08_kotlin/B01_WerteUndTypen.kt
package kapitel08_kotlin

fun main() {
    // val: unveränderlich, var: veränderlich – der Typ wird abgeleitet
    val helligkeit = 70                        // Int
    val temperatur = 21.5                      // Double
    var an = true                              // Boolean
    an = !an
    println("Int $helligkeit, Double $temperatur, Boolean $an")

    // Zeichenketten-Vorlagen: $name und ${ausdruck}
    val name = "Wohnzimmer_Licht"
    val zustand = if (helligkeit > 0) "ON" else "OFF"
    println("$name = $zustand (${name.length} Zeichen)")
    val text = "  Küche  "
    println("[${text.trim()}] ${text.trim().uppercase()} beginnt mit K: "
        + text.trim().startsWith("K"))
    println("%-10s %5.1f °C".format("Werkstatt", 18.25))
    val json = """{"name": "$name", "state": "$zustand"}"""   // roher Text
    println(json)

    // Null-Sicherheit: String kann nie null sein, String? schon
    val beschriftung: String? = null
    println("Länge: ${beschriftung?.length}")             // ?. – sicherer Aufruf
    println("Anzeige: ${beschriftung ?: "(ohne Beschriftung)"}")   // ?: – Ersatzwert
    val zahl: Int? = "55".toIntOrNull()                    // null statt Ausnahme
    if (zahl != null) println("Zahl + 1 = ${zahl + 1}")    // hier ist zahl ein Int

    // == vergleicht den Inhalt, === die Identität
    val a = "ON"
    val b = StringBuilder("O").append("N").toString()
    println("== ${a == b}, === ${a === b}")

    // Listen, Mengen und Abbildungen – unveränderlich oder veränderlich
    val raeume = mutableListOf("Wohnzimmer", "Küche")
    raeume += "Werkstatt"
    val typen = setOf("Switch", "Dimmer")
    val item = mutableMapOf<String, Any?>("name" to "Wohnzimmer_Dimmer", "state" to 55)
    item["tags"] = listOf("Light")
    println("Liste $raeume, Dimmer dabei: ${"Dimmer" in typen}")
    println("Map $item, state = ${item["state"]}")
    println("getOrDefault: ${item["label"] ?: "(ohne Beschriftung)"}")

    // Typumwandlung: aus Text wird eine Zahl und umgekehrt
    val wert = "21.5".toDouble()
    val prozent = "55".toInt()
    println("${wert + 1} / ${prozent + 1} / ${prozent}1")
}
