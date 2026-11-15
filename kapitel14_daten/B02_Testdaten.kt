// Kapitel 14: Werte mit eigenem Zeitstempel in einen Persistenzdienst schreiben
// 48 Stunden synthetische Temperaturen (alle 15 Minuten) für den Dienst "inmemory" –
// einschließlich eines Ausreißers.
package kapitel14_daten

import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import io.github.michdo93.openhab.Persistence
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.Random
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

private const val DIENST = "inmemory"

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val client = OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN"))
    val persistence = Persistence(client)
    val rnd = Random(42)   // fester Startwert: jeder Lauf erzeugt dieselben Daten
    val jetzt = Instant.now().truncatedTo(ChronoUnit.MINUTES)
    val basis = linkedMapOf("Wohnzimmer_Temperatur" to 21.0, "Werkstatt_Temperatur" to 17.0)

    for ((item, grund) in basis) {
        // alte Testdaten der letzten drei Tage entfernen
        persistence.deleteItemData(item, jetzt.minus(3, ChronoUnit.DAYS).toString(),
            jetzt.toString(), DIENST)
        // alle Werte vorbereiten und gleichzeitig schreiben
        val anfragen = (48 * 4 downTo 1).map { k ->
            val zeit = jetzt.minus(15L * k, ChronoUnit.MINUTES)
            val lokal = zeit.atZone(ZoneId.systemDefault())
            val stunde = lokal.hour + lokal.minute / 60.0
            var wert = grund + 1.5 * sin((stunde - 9) / 24 * 2 * PI)
                + 0.15 * rnd.nextGaussian()
            if (item == "Werkstatt_Temperatur" && k == 30) wert += 6.0     // ein Ausreißer
            val zustand = String.format(Locale.ROOT, "%.2f °C", wert)
            async { persistence.storeItemData(item, zeit.toString(), zustand, DIENST) }
        }.awaitAll()
        val fehler = anfragen.firstOrNull { Json.isError(it) }
        check(fehler == null) { "$item: ${Json.error(fehler)}" }
        println("$item: ${anfragen.size} Werte in »$DIENST« geschrieben")
    }

    // Gegenprobe: rrd4j nimmt keine Werte von außen an
    val antwort = persistence.storeItemData("Wohnzimmer_Temperatur", jetzt.toString(),
        "20 °C", "rrd4j")
    val text = Json.error(antwort)!!
    println("Gegenprobe rrd4j: ${text.substringAfterLast('(').removeSuffix(")")}")
}
