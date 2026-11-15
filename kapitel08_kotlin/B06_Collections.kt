// Kapitel 8: Sammlungen filtern, umwandeln, sortieren und gruppieren
// Start: ./starte.sh kapitel08_kotlin/B06_Collections.kt
package kapitel08_kotlin

private data class Item(
    val name: String, val typ: String, val zustand: String, val raum: String,
)

fun main() {
    val items = listOf(
        Item("Wohnzimmer_Licht", "Switch", "ON", "Wohnzimmer"),
        Item("Wohnzimmer_Dimmer", "Dimmer", "40", "Wohnzimmer"),
        Item("Kueche_Licht", "Switch", "OFF", "Küche"),
        Item("Kueche_Rollladen", "Rollershutter", "100", "Küche"),
        Item("Werkstatt_Luefter", "Switch", "ON", "Werkstatt"),
    )
    // filtern und umwandeln
    val eingeschaltet = items
        .filter { it.typ == "Switch" && it.zustand == "ON" }
        .map { it.name }
    println("an: $eingeschaltet")

    // zählen, prüfen, erstes Element
    val schalter = items.count { it.typ == "Switch" }
    val rollladen = items.any { it.typ == "Rollershutter" }
    println("$schalter Schalter, Rollladen vorhanden: $rollladen")
    println(items.firstOrNull { it.raum == "Keller" } ?: "kein Item im Keller")

    // sortieren
    items.sortedWith(compareBy({ it.raum }, { it.name }))
        .forEach { println("  ${it.raum} / ${it.name}") }

    // gruppieren: Raum -> Namen
    val proRaum = items.groupBy({ it.raum }, { it.name }).toSortedMap()
    proRaum.forEach { (raum, namen) -> println("$raum: $namen") }

    // zählen je Typ und zu einer Zeichenkette verbinden
    val zaehler = items.groupingBy { it.typ }.eachCount().toSortedMap()
    println(zaehler)
    println(zaehler.keys.joinToString(", "))

    // associate: eine Map aus einer Liste; sumOf / maxOf rechnen direkt
    val zustaende = items.associate { it.name to it.zustand }
    println("Licht: ${zustaende["Wohnzimmer_Licht"]}, Summe der Zahlen: "
        + items.sumOf { it.zustand.toIntOrNull() ?: 0 })
}
