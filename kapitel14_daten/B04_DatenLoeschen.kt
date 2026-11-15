// Kapitel 14: Gespeicherte Werte gezielt löschen
package kapitel14_daten

import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import io.github.michdo93.openhab.Persistence
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.runBlocking

private const val DIENST = "inmemory"
private const val ITEM = "Werkstatt_Temperatur"

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val client = OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN"))
    val persistence = Persistence(client)
    val jetzt = Instant.now()
    suspend fun anzahl() = Json.get(persistence.getItemPersistenceData(ITEM, DIENST,
        jetzt.minus(3, ChronoUnit.DAYS).toString(), jetzt.toString()), "datapoints")

    println("vorher:  ${anzahl()}")
    // Nur die Nacht von 0 bis 6 Uhr von gestern löschen
    val gestern = LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault())
    val von = gestern.toInstant().toString()
    val bis = gestern.plusHours(6).toInstant().toString()
    println(persistence.deleteItemData(ITEM, von, bis, DIENST))
    println("nachher: ${anzahl()}")

    // rrd4j lehnt das Löschen ab – genau wie das Schreiben
    val antwort = persistence.deleteItemData(ITEM, von, jetzt.toString(), "rrd4j")
    val text = Json.error(antwort)!!
    println("rrd4j: ${text.substringAfterLast('(').removeSuffix(")")}")
}
