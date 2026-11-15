// Kapitel 17: Das Model spricht mit openHAB – über den android-openhab-rest-client
package de.openhabbuch.ohapp.daten

import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHAB
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class OpenHABSmartHome(verbindung: Verbindung) : SmartHome {
    private val openHAB = OpenHAB(verbindung.url, token = verbindung.token)

    override suspend fun lichter(): Result<List<Licht>> {
        val antwort = openHAB.items.getItems(tags = "Light",
            fields = "name,label,type,state")
        if (Json.isError(antwort)) return Result.failure(Exception(Json.error(antwort)))
        val lichter = Json.list(antwort).map {
            val name = it["name"].toString()
            Licht(name, (it["label"] ?: name).toString(), it["type"].toString(),
                it["state"].toString())
        }
        return Result.success(lichter.sortedBy { it.label })
    }

    override suspend fun zustand(item: String): Result<String> {
        val antwort = openHAB.items.getItemState(item)
        return if (Json.isError(antwort)) Result.failure(Exception(Json.error(antwort)))
        else Result.success(antwort.toString())
    }

    override suspend fun befehl(item: String, command: String): String? =
        openHAB.items.sendCommand(item, command, source = "ohapp").let { Json.error(it) }

    // Der robuste Strom aus Kapitel 16: neu verbinden mit Backoff, Wachhund für Stille
    override fun wechsel(): Flow<Wechsel> = flow {
        var pause = 1000L
        while (true) {
            val strom = openHAB.itemEvents.ItemStateChangedEvent()
            if (!strom.isError) {
                pause = 1000
                coroutineScope {
                    var letzte = System.currentTimeMillis()
                    val wache = launch {
                        while (true) {
                            delay(1000)
                            if (System.currentTimeMillis() - letzte > 25_000) strom.close()
                        }
                    }
                    strom.events(includeAlive = true).collect { e ->
                        letzte = System.currentTimeMillis()
                        val topic = e["topic"]?.toString() ?: return@collect
                        emit(Wechsel(topic.split("/")[2],
                            Json.get(e, "payload", "value").toString()))
                    }
                    wache.cancel()
                }
            }
            delay(pause)
            pause = minOf(pause * 2, 30_000)
        }
    }
}
