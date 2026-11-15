// Kapitel 19: Unit-Tests für die Logik der App – laufen in Millisekunden, ohne openHAB
package de.openhabbuch.ohapp

import de.openhabbuch.ohapp.daten.Verbindung
import de.openhabbuch.ohapp.daten.Wechsel
import de.openhabbuch.ohapp.ui.HausZustand
import de.openhabbuch.ohapp.ui.hausZustand
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HausLogikTest {
    private val ok = Verbindung("http://openhab:8080", "oh.test.1")

    @Test
    fun ohneTokenWirdEingerichtet() = runBlocking {
        val zustaende = hausZustand(flowOf(Verbindung("http://x", "")), { FakeSmartHome() })
            .toList()
        assertEquals(listOf(HausZustand.Einrichten), zustaende)
    }

    @Test
    fun erstLadenDannBereit() = runBlocking {
        val zustaende = hausZustand(flowOf(ok), { FakeSmartHome() }).take(2).toList()
        assertEquals(HausZustand.Laedt, zustaende[0])
        val bereit = zustaende[1] as HausZustand.Bereit
        assertEquals(listOf("Deckenlicht", "Leselampe"), bereit.lichter.map { it.label })
        assertEquals("21.5 °C", bereit.temperatur)
    }

    @Test
    fun fehlerBeimLadenWirdAngezeigt() = runBlocking {
        val fake = FakeSmartHome(Result.failure(Exception("Connection refused")))
        val zustaende = hausZustand(flowOf(ok), { fake }).toList()
        assertEquals(HausZustand.Fehler("Connection refused"), zustaende.last())
    }

    @Test
    fun ereignisseAusOpenHABAendernDieAnzeige() = runBlocking {
        val fake = FakeSmartHome()
        val zustaende = async { hausZustand(flowOf(ok), { fake }).take(5).toList() }
        delay(100)                                    // Laden ist durch, Strom läuft
        fake.ereignisse.emit(Wechsel("Wohnzimmer_Licht", "ON"))
        fake.ereignisse.emit(Wechsel("Wohnzimmer_Dimmer", "75"))
        fake.ereignisse.emit(Wechsel("Flur_Haustuer", "OPEN"))
        val letzter = zustaende.await().last() as HausZustand.Bereit
        assertTrue(letzter.lichter[0].an)
        assertEquals(75, letzter.lichter[1].helligkeit)
        assertEquals("OPEN", letzter.haustuer)
    }

    @Test
    fun helligkeitDesFarblichts() {
        val farbe = de.openhabbuch.ohapp.daten.Licht("F", "Farblicht", "Color", "240,100,30")
        assertEquals(30, farbe.helligkeit)
        assertTrue(farbe.an)
    }
}
