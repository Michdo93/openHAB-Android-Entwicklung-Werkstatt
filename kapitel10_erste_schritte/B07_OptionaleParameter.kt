// Kapitel 10: Optionale Parameter – Vorgabewerte und benannte Argumente
package kapitel10_erste_schritte

import io.github.michdo93.openhab.Items
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHAB
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    // OpenHAB: ein Client und alle Klassen als Eigenschaften (openHAB.items, …)
    val openHAB = OpenHAB(url, token = System.getenv("OPENHAB_TOKEN"))
    val items: Items = openHAB.items

    // 1. nur der erste Parameter, der Rest hat Vorgabewerte
    println("Switch-Items:  ${Json.list(items.getItems("Switch")).size}")
    // 2. Parameter in der Reihenfolge der Python-Bibliothek
    //    getItems(type, tags, metadata, recursive, fields, staticDataOnly, language,
    //             parents)
    println("Licht, Reihe:  ${items.getItems(null, "Light", ".*", false, "name")}")
    // 3. benannte Argumente: nur, was man braucht – in beliebiger Reihenfolge
    println("Licht, Namen:  ${items.getItems(fields = "name", tags = "Light")}")
    // items.getItems(tag = "Light")  -> Übersetzungsfehler: "No parameter with name 'tag'"

    // Json hilft beim Auswerten: Pfade, Typen, Fehler
    val item = items.getItem("Wohnzimmer_Temperatur", metadata = "unit")
    println("Muster:        ${Json.get(item, "stateDescription", "pattern")}")
    println("erstes Tag:    ${Json.get(item, "tags", 0)}")
    println("Einheit:       ${Json.get(item, "unitSymbol")}")
    println("fehlt:         ${Json.get(item, "metadata", "unit")}")
}
