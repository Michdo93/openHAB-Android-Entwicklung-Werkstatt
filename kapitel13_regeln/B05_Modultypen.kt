// Kapitel 13: Welche Bausteine gibt es für Regeln?
package kapitel13_regeln

import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.ModuleTypes
import io.github.michdo93.openhab.OpenHABClient
import io.github.michdo93.openhab.Templates
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val client = OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN"))
    val module = ModuleTypes(client)

    val nachArt = Json.list(module.getModuleTypes(language = "de"))
        // nur sichtbare Typen, ohne Thing-Actions (deren UID enthält "#")
        .filter { it["visibility"] == "VISIBLE" && '#' !in it["uid"].toString() }
        .groupBy {
            val uid = it["uid"].toString()
            when {
                "Trigger" in uid -> "Trigger"
                "Condition" in uid -> "Bedingung"
                else -> "Aktion"
            }
        }
    for (art in listOf("Trigger", "Bedingung", "Aktion")) {
        val liste = nachArt[art].orEmpty()
        println("$art (${liste.size}):")
        for (m in liste.sortedBy { it["uid"].toString() }.take(7)) {
            println("  %-30s %s".format(m["uid"], m["label"]))
        }
    }

    val typ = Json.map(module.getModuleType("core.ItemCommandAction", "de"))
    println("\n${typ["uid"]} – ${typ["description"]}")
    for (p in Json.list(typ["configDescriptions"])) {
        val pflicht = if (p["required"] == true) "Pflicht" else "optional"
        println("  %-10s %-6s %-8s %s".format(p["name"], p["type"], pflicht, p["label"]))
    }
    val vorlagen = Json.list(Templates(client).getTemplates("de"))
    println("\nRegelvorlagen installiert: ${vorlagen.size}")
}
