// Kapitel 14: Den Verlauf eines Items als CSV-Datei exportieren – in Zeitabschnitten
// Aufruf: ./starte.sh kapitel14_daten/B06_CsvExport.kt [item] [stunden] [dienst]
package kapitel14_daten

import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import io.github.michdo93.openhab.Persistence
import java.io.File
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val client = OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN"))
    val persistence = Persistence(client)
    val item = args.getOrElse(0) { "Wohnzimmer_Temperatur" }
    val stunden = args.getOrNull(1)?.toLong() ?: 48
    val dienst = args.getOrElse(2) { "inmemory" }

    // bufferedWriter: Zeilen gehen blockweise auf die Platte, nichts sammelt sich an
    val datei = File("$item.csv")
    var zeilen = 0
    datei.bufferedWriter().use { csv ->
        csv.write("zeit;wert;einheit\n")
        // Lange Zeiträume in Abschnitten von 6 Stunden abfragen: kleine Antworten
        val abschnitt = Duration.ofHours(6)
        val ende = Instant.now()
        var von = ende.minus(Duration.ofHours(stunden))
        while (von.isBefore(ende)) {
            val bis = minOf(von.plus(abschnitt), ende)
            val antwort = persistence.getItemPersistenceData(item, dienst,
                von.toString(), bis.minusMillis(1).toString())
            check(!Json.isError(antwort)) { Json.error(antwort)!! }
            val einheit = Json.get(antwort, "unit") ?: ""
            for (p in Json.list(Json.get(antwort, "data"))) {
                val zeit = Instant.ofEpochMilli(p["time"] as Long)
                csv.write("$zeit;${p["state"].toString().replace(".", ",")};$einheit\n")
                zeilen++
            }
            von = von.plus(abschnitt)
        }
    }
    println("$zeilen Werte ($stunden h, $dienst) nach ${datei.name} geschrieben")
}
