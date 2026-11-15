// Kapitel 13: Regeln lesen – Übersicht, Module und Skript
package kapitel13_regeln

import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import io.github.michdo93.openhab.Rules
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val rules = Rules(OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN")))

    for (r in Json.list(rules.getRules(summary = true))) {
        println("%-12s %-14s %-6s Tags %s".format(r["uid"], r["name"],
            Json.get(r, "status", "status"), r["tags"] ?: emptyList<String>()))
    }

    // Der Aufbau einer Regel: Trigger, Bedingungen, Aktionen – jeweils Module
    val uid = "restbuch-1"
    println("\nRegel $uid:")
    val teile = linkedMapOf(
        "Trigger" to rules.getTriggers(uid),
        "Bedingung" to rules.getConditions(uid),
        "Aktion" to rules.getActions(uid),
    )
    for ((art, module) in teile) {
        for (m in Json.list(module)) {
            val cfg = Json.stringify(m["configuration"]).take(36)
            println("  %-10s %-7s %-26s %s…".format(art, m["id"], m["type"], cfg))
        }
    }
    val skript = rules.getModuleConfigParam(uid, "actions", "script", "script")
    val kurz = skript.toString().trim().replace("\n", " | ").take(66)
    println("\nSkript der Aktion: $kurz")
}
