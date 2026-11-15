// Kapitel 19: Das Roboter-Szenario als Integrationstest – prüft vorab, ob alles da ist
// Start: ./gradlew test – ohne OPENHAB_URL und OPENHAB_TOKEN werden die Tests
// übersprungen
package kapitel19_tests

import io.github.michdo93.openhab.Items
import io.github.michdo93.openhab.OpenHABClient
import io.github.michdo93.openhab.testsuite.ChannelTester
import io.github.michdo93.openhab.testsuite.ItemTester
import io.github.michdo93.openhab.testsuite.PersistenceTester
import io.github.michdo93.openhab.testsuite.RuleTester
import io.github.michdo93.openhab.testsuite.SitemapTester
import io.github.michdo93.openhab.testsuite.ThingTester
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Roboter-Szenario")
class RoboterSzenarioTest {
    private lateinit var client: OpenHABClient

    @BeforeAll
    fun verbinden() {
        val url = System.getenv("OPENHAB_URL")
        val token = System.getenv("OPENHAB_TOKEN")
        assumeTrue(url != null && token != null, "OPENHAB_URL/OPENHAB_TOKEN fehlen")
        client = OpenHABClient(url, token = token)
        assumeTrue(client.login(), "openHAB nicht erreichbar")
    }

    @Test
    @DisplayName("alle Items vorhanden und vom richtigen Typ")
    fun items() = runBlocking {
        val t = ItemTester(client)
        for ((name, typ) in mapOf("Wohnzimmer_Licht" to "Switch",
            "Wohnzimmer_Dimmer" to "Dimmer", "Kueche_Rollladen" to "Rollershutter",
            "Werkstatt_Temperatur" to "Number:Temperature", "Flur_Haustuer" to "Contact")) {
            assertTrue(t.checkItemIsType(name, typ), "$name ist kein $typ")
        }
        assertTrue(t.doesGroupContainMember("gLichter", "Wohnzimmer_Licht"))
    }

    @Test
    @DisplayName("Commands bewirken den erwarteten Zustand")
    fun commands() = runBlocking {
        val t = ItemTester(client)
        assertTrue(t.testSwitch("Wohnzimmer_Licht", "ON", "ON", 5))
        assertTrue(t.testDimmer("Wohnzimmer_Dimmer", 30, listOf("30", "30.0"), 5))
        assertTrue(t.testRollershutter("Kueche_Rollladen", "DOWN", "100", 5))
        assertFalse(t.testSwitch("Wohnzimmer_Licht", "HALB", "ON", 2))   // ungültig
    }

    @Test
    @DisplayName("Thing, Regeln, Sitemap und Persistenz bereit")
    fun umgebung() = runBlocking {
        assertTrue(ThingTester(client).isThingOnline("astro:sun:zuhause"))
        val regeln = RuleTester(client)
        assertTrue(regeln.isRuleActive("restbuch-1"))
        assertFalse(regeln.isRuleDisabled("restbuch-2"))
        val sitemaps = SitemapTester(client)
        assertTrue(sitemaps.doesSitemapContainItem("restbuch", "Wohnzimmer_Licht"))
        // Persistenz: einen Wert melden – er muss im Dienst ankommen
        Items(client).postUpdate("Werkstatt_Temperatur", "19.5 °C")
        delay(500)
        val persistenz = PersistenceTester(client)
        assertTrue(persistenz.isItemPersisted("inmemory", "Werkstatt_Temperatur"))
        assertFalse(ChannelTester(client).hasOrphanedLinks())
    }
}
