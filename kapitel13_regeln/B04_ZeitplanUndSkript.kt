// Kapitel 13: Zeitplan-Trigger, ein Rules-DSL-Skript und die Vorschau des Zeitplans
package kapitel13_regeln

import io.github.michdo93.openhab.Items
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import io.github.michdo93.openhab.Rules
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

private const val LUEFTEN = "buch_lueften"
// openHAB liefert z. B. "Nov 16, 2026, 7:30:00 AM"
private val EIN = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm:ss a", Locale.US)
private val AUS = DateTimeFormatter.ofPattern("EE dd.MM., HH:mm")

// Rules-DSL-Skript als Aktion: läuft werktags um 7:30 und 17:30
private val SKRIPT = """
    val feuchte = Werkstatt_Feuchte.state
    if (feuchte instanceof QuantityType
            && (feuchte as QuantityType<?>).toUnit("%").doubleValue > 60) {
        Werkstatt_Luefter.sendCommand(ON)
        Haus_Status.postUpdate("Werkstatt wird gelüftet")
    }
    """.trimIndent()

private fun cron(id: String, ausdruck: String) = mapOf(
    "id" to id, "type" to "timer.GenericCronTrigger",
    "configuration" to mapOf("cronExpression" to ausdruck))

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val client = OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN"))
    val items = Items(client)
    val rules = Rules(client)

    rules.deleteRule(LUEFTEN)
    println(rules.createRule(mapOf(
        "uid" to LUEFTEN, "name" to "Werkstatt lüften",
        "tags" to listOf("Buch", "Schedule"),
        "triggers" to listOf(cron("morgens", "0 30 7 ? * MON-FRI"),
                             cron("abends", "0 30 17 ? * MON-FRI")),
        "actions" to listOf(mapOf("id" to "skript", "type" to "script.ScriptAction",
            "configuration" to mapOf("type" to "application/vnd.openhab.dsl.rule",
                                     "script" to SKRIPT))))))

    // Wann läuft was? openHAB simuliert alle Regeln mit dem Tag "Schedule"
    val zone = ZoneId.systemDefault()
    val heute = LocalDate.now()
    val von = heute.plusDays(1).atStartOfDay(zone).toInstant().toString()
    val bis = heute.plusDays(8).atStartOfDay(zone).toInstant().toString()
    for (eintrag in Json.list(rules.simulateSchedule(von, bis))) {
        if (Json.get(eintrag, "rule", "uid") != LUEFTEN) continue
        val text = eintrag["date"].toString().replace(Regex("\\s+"), " ")
        val zeit = LocalDateTime.parse(text, EIN)
        println("  ${AUS.format(zeit)}  ${Json.get(eintrag, "rule", "name")}")
    }

    // Nicht bis morgen warten: Werkstatt feucht machen und die Regel sofort ausführen
    items.postUpdate("Werkstatt_Feuchte", "72 %")
    rules.runNow(LUEFTEN)
    delay(2000)  // DSL-Skripte werden beim ersten Lauf übersetzt
    println("Status: ${items.getItemState("Haus_Status")}")
    println("Lüfter: ${items.getItemState("Werkstatt_Luefter")}"
        + " (autoupdate=false: erst das Gerät bestätigt den Command)")
    println("löschen: ${rules.deleteRule(LUEFTEN)}")
}
