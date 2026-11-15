// Kapitel 18: Das Smartphone als Gerät – es meldet States und führt Commands aus
package de.openhabbuch.geraet

import io.github.michdo93.openhab.ItemEvents
import io.github.michdo93.openhab.Items
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Geraet(
    client: OpenHABClient,
    private val hardware: Hardware,
    private val name: String = "Handy",              // Präfix aller Items
    private val takt: Long = 30_000,                  // alle 30 s messen
    private val protokoll: (String) -> Unit = ::println,
) {
    private val items = Items(client)
    private val ereignisse = ItemEvents(client)
    private val aktion = "${name}_Aktion"

    /** Die Items des Geräts: Name, Typ, Beschriftung, Tags, Metadaten. */
    private fun definitionen(): List<Map<String, Any?>> {
        fun item(n: String, typ: String, label: String, tags: List<String> = emptyList(),
                 vararg mehr: Pair<String, Any?>) =
            mapOf("type" to typ, "name" to "${name}_$n", "label" to label,
                  "groupNames" to listOf("g$name"), "tags" to tags) + mehr
        return listOf(
            mapOf("type" to "Group", "name" to "g$name", "label" to "Smartphone",
                "groupNames" to listOf("gHaus"), "tags" to listOf("MobilePhone")),
            item("Akku", "Number:Dimensionless", "Akku [%.0f %%]",
                listOf("Measurement", "Energy"), "category" to "battery"),
            item("Laedt", "Switch", "Lädt", listOf("Status", "Power")),
            item("Helligkeit", "Number:Illuminance", "Helligkeit [%.0f lx]",
                listOf("Measurement", "Light")),
            item("WLAN", "Number", "WLAN [%d dBm]", listOf("Measurement")),
            item("Online", "Switch", "Online", listOf("Status")),
            item("Aktion", "String", "Aktion", listOf("Control")),
            item("Antwort", "String", "Antwort [%s]", listOf("Status")),
        )
    }

    /** Legt die Items an (idempotent) und setzt die Metadaten. */
    suspend fun einrichten() {
        val antwort = items.addOrUpdateItems(definitionen())
        protokoll("Items eingerichtet: ${Json.list(antwort).size}")
        // Lebenszeichen: ohne neue Meldung fällt Online nach 2 Minuten auf OFF
        items.addMetadata("${name}_Online", "expire", mapOf("value" to "2m,state=OFF"))
        // Commands erst bestätigen, wenn das Gerät sie ausgeführt hat
        items.addMetadata(aktion, "autoupdate", mapOf("value" to "false"))
        items.addMetadata("${name}_Akku", "unit", mapOf("value" to "%"))
    }

    /** Eine Runde: alle Sensoren lesen und als States melden. */
    suspend fun melden() {
        val m = hardware.messen()
        items.postUpdate("${name}_Akku", "${m.akku} %")
        items.postUpdate("${name}_Laedt", if (m.laedt) "ON" else "OFF")
        m.helligkeit?.let { items.postUpdate("${name}_Helligkeit", "%.0f lx".format(it)) }
        m.wlan?.let { items.postUpdate("${name}_WLAN", "$it") }
        items.postUpdate("${name}_Online", "ON")
        protokoll("gemeldet: Akku ${m.akku} %, lädt ${m.laedt}, "
            + "${m.helligkeit} lx, ${m.wlan} dBm")
    }

    /** Einen Command ausführen; die Antwort ist zugleich die Bestätigung. */
    suspend fun ausfuehren(befehl: String) {
        val antwort = when {
            befehl == "LAMPE_AN" -> { hardware.lampe(true); "Taschenlampe an" }
            befehl == "LAMPE_AUS" -> { hardware.lampe(false); "Taschenlampe aus" }
            befehl == "VIBRIEREN" -> { hardware.vibrieren(500); "vibriert" }
            befehl.startsWith("SAGE:") -> { hardware.sagen(befehl.substring(5)); "gesagt" }
            befehl == "MESSEN" -> { melden(); "gemessen" }
            else -> "unbekannt: $befehl"
        }
        items.postUpdate(aktion, befehl)              // bestätigen: Zustand = Command
        items.postUpdate("${name}_Antwort", antwort)
        protokoll("Command $befehl -> $antwort")
    }

    /** Läuft, bis die Coroutine abgebrochen wird: messen im Takt, Commands sofort. */
    suspend fun laufen() = coroutineScope {
        einrichten()
        launch {
            while (true) {
                melden()
                delay(takt)
            }
        }
        // Commands kommen als ItemCommandEvent über den Ereignisstrom
        while (true) {
            val strom = ereignisse.ItemCommandEvent(aktion)
            if (!strom.isError) {
                strom.events().collect { e ->
                    ausfuehren(Json.get(e, "payload", "value").toString())
                }
            }
            delay(5000)                               // Strom abgerissen: neu verbinden
        }
    }
}
