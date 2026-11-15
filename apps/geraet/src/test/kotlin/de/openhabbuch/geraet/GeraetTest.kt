// Kapitel 19: Das Gerät gegen ein echtes openHAB testen – mit Hardware-Attrappe
// Läuft nur mit OPENHAB_URL und OPENHAB_TOKEN in der Umgebung (./gradlew :geraet:test)
package de.openhabbuch.geraet

import io.github.michdo93.openhab.Items
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import io.github.michdo93.openhab.testsuite.ItemTester
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class GeraetTest {
    private val url: String? = System.getenv("OPENHAB_URL")
    private val token: String? = System.getenv("OPENHAB_TOKEN")

    @Test
    fun meldetMesswerteUndFuehrtCommandsAus() = runBlocking {
        assumeTrue("OPENHAB_URL/OPENHAB_TOKEN fehlen", url != null && token != null)
        val client = OpenHABClient(url!!, token = token)
        val hardware = FakeHardware(Messung(42, true, 300f, -61))
        val geraet = launch { Geraet(client, hardware, takt = 60_000).laufen() }
        delay(2000)                                       // einrichten und erste Meldung

        val items = ItemTester(client)
        assertTrue(items.checkItemHasState("Handy_Akku", "42 %"))
        assertTrue(items.checkItemHasState("Handy_Laedt", "ON"))
        // Die Test-Suite sendet den Command und wartet, bis das Gerät ihn bestätigt
        assertTrue(items.testString("Handy_Aktion", "LAMPE_AN", "LAMPE_AN", 5))
        assertEquals(listOf("lampe true"), hardware.protokoll.take(1))
        geraet.cancelAndJoin()

        // aufräumen: die Items des Geräts wieder entfernen
        val alle = Items(client)
        Json.list(alle.getItems(fields = "name")).map { it["name"].toString() }
            .filter { it.startsWith("Handy_") || it == "gHandy" }
            .forEach { alle.deleteItem(it) }
    }
}
