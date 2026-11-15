// Kapitel 13: Eine Regel steuern – Parameter ändern, ausführen, abschalten, ersetzen
package kapitel13_regeln

import io.github.michdo93.openhab.Items
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import io.github.michdo93.openhab.Rules
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

private const val REGEL = "buch_kinoabend"             // aus B02_RegelAnlegen

private suspend fun status(rules: Rules): String {
    val s = Json.get(rules.getRule(REGEL), "status")
    return "${Json.get(s, "status")} / ${Json.get(s, "statusDetail")}"
}

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val client = OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN"))
    val items = Items(client)
    val rules = Rules(client)
    check(!Json.isError(rules.getRule(REGEL))) { "Zuerst B02_RegelAnlegen.kt ausführen." }

    // 1. Einen einzelnen Parameter eines Moduls ändern: Dimmer auf 10 statt 20 %
    rules.setModuleConfigParam(REGEL, "actions", "4", "command", "10")
    val dimmwert = rules.getModuleConfigParam(REGEL, "actions", "4", "command")
    println("Dimmwert jetzt: $dimmwert")

    // 2. Sofort ausführen – runNow überspringt Trigger UND Bedingungen
    items.sendCommand("Wohnzimmer_Dimmer", "80")
    items.postUpdate("Flur_Haustuer", "OPEN")
    rules.runNow(REGEL)
    delay(1000)
    println("runNow trotz offener Tür: ${items.getItemState("Wohnzimmer_Dimmer")}")

    // 3. Abschalten: der Trigger wird ignoriert
    rules.disable(REGEL)
    println("abgeschaltet: ${status(rules)}")
    items.postUpdate("Flur_Haustuer", "CLOSED")
    items.sendCommand("Wohnzimmer_Dimmer", "80")
    items.sendCommand("Haus_Status", "Kino")
    delay(1000)
    println("Trigger bei abgeschalteter Regel: ${items.getItemState("Wohnzimmer_Dimmer")}")
    rules.enable(REGEL)
    println("eingeschaltet: ${status(rules)}")

    // 4. Die ganze Regel ersetzen: eine zusätzliche Aktion
    val regel = Json.map(rules.getRule(REGEL))
    val aktionen = Json.array(regel["actions"]) + mapOf(
        "id" to "6", "type" to "core.ItemStateUpdateAction",
        "configuration" to mapOf("itemName" to "Haus_Status", "state" to "Film läuft"))
    rules.updateRule(REGEL, regel + ("actions" to aktionen))
    items.sendCommand("Haus_Status", "Kino")
    delay(1000)
    println("nach updateRule: ${items.getItemState("Haus_Status")}")
    println("löschen: ${rules.deleteRule(REGEL)}")
}
