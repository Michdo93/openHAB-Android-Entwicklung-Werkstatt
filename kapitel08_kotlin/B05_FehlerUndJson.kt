// Kapitel 8: Ausnahmen, Ergebnisse mit null, runCatching und JSON
// Start: ./starte.sh kapitel08_kotlin/B05_FehlerUndJson.kt  (braucht die Bibliothek)
package kapitel08_kotlin

import io.github.michdo93.openhab.Json

// Eine eigene Ausnahme
private class MesswertFehler(text: String) : RuntimeException(text)

private fun prozent(text: String): Double {
    // try ist ein Ausdruck: Es liefert den Wert des Blocks
    val wert = try {
        text.toDouble()
    } catch (e: NumberFormatException) {          // abfangen und übersetzen
        throw MesswertFehler("keine Zahl: $text")
    }
    if (wert !in 0.0..100.0) throw MesswertFehler("außerhalb 0–100: $wert")
    return wert
}

// Ein Ergebnis, das fehlen darf: einfach ein nullbarer Typ
private fun raumVon(item: String): String? =
    item.indexOf('_').takeIf { it > 0 }?.let { item.substring(0, it) }

fun main() {
    for (text in listOf("55", "120", "viel")) {
        try {
            println("OK: ${prozent(text)}")
        } catch (e: MesswertFehler) {
            println("Fehler: ${e.message}")
        } finally {
            println("  (geprüft: $text)")
        }
    }
    println("${raumVon("Wohnzimmer_Licht") ?: "?"} / ${raumVon("Licht") ?: "?"}")

    // runCatching macht aus einer Ausnahme ein Ergebnis
    val r = runCatching { prozent("120") }
    println("Erfolg: ${r.isSuccess}, Wert: ${r.getOrElse { -1.0 }}")

    // use schließt eine Ressource auch im Fehlerfall (wie try-with-resources)
    "Zeile 1\nZeile 2".reader().buffered().use { println("gelesen: ${it.readLine()}") }

    // JSON ohne Zusatzbibliothek: android-openhab-rest-client bringt Json mit
    val text = """{"name": "Werkstatt_Temperatur", "state": "18.5 °C",
                   "tags": ["Measurement", "Temperature"]}"""
    val item = Json.map(Json.parse(text)).toMutableMap()
    println("${item["name"]} hat ${Json.array(item["tags"]).size} Tags")
    item["state"] = "19 °C"
    println(Json.stringify(item))
    println(Json.stringify(mapOf("ok" to true), 2))
    try {
        Json.parse("{kaputt")
    } catch (e: IllegalArgumentException) {
        println("JSON-Fehler: ${e.message}")
    }
}
