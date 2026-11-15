// Kapitel 10: States melden
package kapitel10_erste_schritte

import io.github.michdo93.openhab.Items
import io.github.michdo93.openhab.OpenHABClient
import java.time.Instant
import kotlinx.coroutines.runBlocking

fun main() = runBlocking<Unit> {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val items = Items(OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN")))

    // Ein Messwert mit Einheit, eine Feuchte und ein Zeitstempel
    println(items.postUpdate("Werkstatt_Temperatur", "18.4 °C"))
    println(items.postUpdate("Werkstatt_Feuchte", "56 %"))
    println(items.postUpdate("Werkstatt_LetzteMeldung", Instant.now().toString()))

    // Ein Contact kennt keine Commands – nur States
    println(items.postUpdate("Flur_Haustuer", "OPEN"))

    // Einheiten werden umgerechnet: Fahrenheit rein, Celsius raus
    println(items.postUpdate("Wohnzimmer_Temperatur", "71.6 °F"))

    for (name in listOf("Werkstatt_Temperatur", "Werkstatt_Feuchte",
                        "Flur_Haustuer", "Wohnzimmer_Temperatur")) {
        println("%-22s = %s".format(name, items.getItemState(name)))
    }

    // Für die weiteren Beispiele wieder 21.5 °C setzen
    items.postUpdate("Wohnzimmer_Temperatur", "21.5 °C")
}
