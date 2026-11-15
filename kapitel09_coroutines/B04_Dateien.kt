// Kapitel 9: Dateien lesen und schreiben
// Start: ./starte.sh kapitel09_coroutines/B04_Dateien.kt
package kapitel09_coroutines

import io.github.michdo93.openhab.Json
import java.io.File

fun main() {
    val ordner = File("berichte").apply { mkdirs() }
    val datei = File(ordner, "messwerte.csv")

    // schreiben und anhängen
    datei.writeText("zeit;item;wert\n")
    for ((i, w) in listOf(18.5, 19.0, 19.5).withIndex()) {
        datei.appendText("10:0$i;Werkstatt_Temperatur;$w\n")
    }
    println("${datei.path}: ${datei.length()} Bytes")

    // ganz lesen, zeilenweise lesen
    println(datei.readLines().first())
    val summe = datei.useLines { zeilen ->
        zeilen.drop(1).sumOf { it.split(";")[2].toDouble() }   // ohne Kopfzeile
    }
    println("Summe %.1f".format(summe))

    // JSON-Datei – z. B. Einstellungen
    val einstellungen = File(ordner, "einstellungen.json")
    einstellungen.writeText(Json.stringify(mapOf("url" to "http://127.0.0.1:8080",
        "items" to listOf("Wohnzimmer_Licht")), 2))
    val gelesen = Json.map(Json.parse(einstellungen.readText()))
    println("url = ${gelesen["url"]}, items = ${gelesen["items"]}")

    // Ordner auflisten
    ordner.listFiles()!!.sortedBy { it.name }.forEach { println("  ${it.name}") }
    println("existiert: ${File("gibtsnicht.txt").exists()}")
}
