// Kapitel 14: Historische Werte abfragen, auswerten und als SVG-Datei zeichnen
package kapitel14_daten

import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import io.github.michdo93.openhab.Persistence
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking

private const val DIENST = "inmemory"                 // Testdaten aus B02_Testdaten
private const val ITEM = "Wohnzimmer_Temperatur"

private data class Punkt(val zeit: Long, val wert: Double)

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val client = OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN"))
    val persistence = Persistence(client)
    val ende = System.currentTimeMillis()
    val start = ende - 24 * 3600_000L
    fun iso(ms: Long) = java.time.Instant.ofEpochMilli(ms).toString()

    val antwort = persistence.getItemPersistenceData(ITEM, DIENST,
        startTime = iso(start), endTime = iso(ende))
    // Ein Datenpunkt der Antwort: Zeit in Millisekunden, Zustand als Text
    val punkte = Json.list(Json.get(antwort, "data"))
        .map { Punkt(it["time"] as Long, it["state"].toString().toDouble()) }
    val lo = punkte.minOf { it.wert }
    val hi = punkte.maxOf { it.wert }
    println("${Json.get(antwort, "datapoints")} Messwerte der letzten 24 Stunden")
    println("Minimum %.1f °C   Maximum %.1f °C   Mittel %.1f °C"
        .format(lo, hi, punkte.map { it.wert }.average()))
    val warm = punkte.maxBy { it.wert }
    println("am wärmsten um ${SimpleDateFormat("HH:mm").format(Date(warm.zeit))} Uhr")

    // Der Verlauf als SVG-Linie – ohne Diagramm-Bibliothek
    val b = 600
    val h = 160
    val linie = punkte.joinToString(" ") {
        val x = (it.zeit - start).toDouble() / (ende - start) * b
        val y = h - (it.wert - lo) / (hi - lo) * (h - 10) - 5
        String.format(Locale.ROOT, "%.1f,%.1f", x, y)
    }
    val svg = """
        <svg xmlns="http://www.w3.org/2000/svg" width="$b" height="$h"
             style="background:#fff">
          <polyline fill="none" stroke="#e64a19" stroke-width="2" points="$linie"/>
          <text x="4" y="14" font-size="12">${"%.1f".format(hi)} °C</text>
          <text x="4" y="${h - 4}" font-size="12">${"%.1f".format(lo)} °C</text></svg>
        """.trimIndent()
    File("verlauf.svg").writeText(svg)                 // im Browser öffnen
    println("verlauf.svg geschrieben (${svg.length} Zeichen)")

    // Dasselbe als Textdiagramm im Terminal: stündliche Werte als Zeichen
    val balken = " .:-=+*#%@"
    println(punkte.filterIndexed { i, _ -> i % 4 == 0 }
        .map { balken[((it.wert - lo) / (hi - lo) * 9).roundToInt()] }
        .joinToString(""))

    // Seitenweise abrufen: 10 Werte pro Seite – sofern der Dienst Seiten unterstützt
    val seite = persistence.getItemPersistenceData(ITEM, DIENST, startTime = iso(start),
        endTime = iso(ende), page = 0, pageLength = 10)
    println("Seite 0: " + Json.list(Json.get(seite, "data")).take(3)
        .joinToString(", ") { it["state"].toString() } + " …")

    // Mit boundary liefert openHAB zusätzlich den Wert vor und nach dem Zeitraum
    val rand = persistence.getItemPersistenceData(ITEM, DIENST,
        startTime = iso(ende - 5 * 60_000), endTime = iso(ende), boundary = true)
    println("letzte 5 Minuten mit Rand: ${Json.get(rand, "datapoints")} Werte")
}
