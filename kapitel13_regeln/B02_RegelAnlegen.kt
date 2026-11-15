// Kapitel 13: Eine Regel anlegen – Trigger, Bedingung, Aktionen
package kapitel13_regeln

import io.github.michdo93.openhab.Items
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import io.github.michdo93.openhab.Rules
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

private const val KINO = "buch_kinoabend"
private val NAMEN = listOf("Wohnzimmer_Licht", "Wohnzimmer_Dimmer", "Kueche_Rollladen")

// Ein Aktionsmodul: "sende Command an Item"
private fun befehl(id: String, itemName: String, command: String) = mapOf(
    "id" to id, "type" to "core.ItemCommandAction",
    "configuration" to mapOf("itemName" to itemName, "command" to command))

private suspend fun zustand(items: Items) =
    NAMEN.map { "$it=${items.getItemState(it)}" }.joinToString(", ")

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val client = OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN"))
    val items = Items(client)
    val rules = Rules(client)

    val regel = mapOf(
        "uid" to KINO,
        "name" to "Kinoabend",
        "description" to "Licht dimmen und Rollladen schließen, wenn die Haustür zu ist.",
        "tags" to listOf("Buch", "Szene"),
        "triggers" to listOf(mapOf("id" to "1", "type" to "core.ItemCommandTrigger",
            "configuration" to mapOf("itemName" to "Haus_Status", "command" to "Kino"))),
        "conditions" to listOf(mapOf("id" to "2", "type" to "core.ItemStateCondition",
            "configuration" to mapOf("itemName" to "Flur_Haustuer",
                                     "operator" to "=", "state" to "CLOSED"))),
        "actions" to listOf(befehl("3", "Wohnzimmer_Licht", "OFF"),
                            befehl("4", "Wohnzimmer_Dimmer", "20"),
                            befehl("5", "Kueche_Rollladen", "DOWN")),
    )
    rules.deleteRule(KINO)                       // falls vom letzten Lauf noch vorhanden
    println("anlegen: ${rules.createRule(regel)}")
    println("Status:  ${Json.get(rules.getRule(KINO), "status", "status")}")

    // Ausgangslage herstellen
    items.sendCommand("Wohnzimmer_Licht", "ON")
    items.sendCommand("Wohnzimmer_Dimmer", "80")
    items.sendCommand("Kueche_Rollladen", "UP")

    // 1. Tür offen: die Bedingung verhindert die Aktionen
    items.postUpdate("Flur_Haustuer", "OPEN")
    items.sendCommand("Haus_Status", "Kino")
    delay(1000)
    println("Tür offen:  ${zustand(items)}")

    // 2. Tür zu: die Regel greift
    items.postUpdate("Flur_Haustuer", "CLOSED")
    items.sendCommand("Haus_Status", "Kino")
    delay(1000)
    println("Tür zu:     ${zustand(items)}")
}
