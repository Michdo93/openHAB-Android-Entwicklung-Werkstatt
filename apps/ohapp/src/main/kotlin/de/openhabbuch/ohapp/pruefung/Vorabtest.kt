// Kapitel 19: Der Vorab-Test – findet fehlende Items, Things und Regeln vor dem Einsatz
package de.openhabbuch.ohapp.pruefung

import io.github.michdo93.openhab.OpenHABClient
import io.github.michdo93.openhab.testsuite.ItemTester
import io.github.michdo93.openhab.testsuite.RuleTester
import io.github.michdo93.openhab.testsuite.ThingTester

/** Ein Prüfpunkt mit Ergebnis. */
data class Pruefung(val was: String, val ok: Boolean)

/** Was die App braucht: Item -> erwarteter Typ. */
val BENOETIGT = linkedMapOf(
    "Wohnzimmer_Licht" to "Switch",
    "Wohnzimmer_Dimmer" to "Dimmer",
    "Wohnzimmer_Temperatur" to "Number:Temperature",
    "Flur_Haustuer" to "Contact",
)

/** Prüft alles nacheinander; jede Prüfung meldet true oder false. */
suspend fun vorabtest(client: OpenHABClient): List<Pruefung> {
    val items = ItemTester(client)
    val ergebnis = mutableListOf(Pruefung("openHAB erreichbar", client.connect()))
    if (!ergebnis[0].ok) return ergebnis
    for ((name, typ) in BENOETIGT) {
        ergebnis += Pruefung("$name ist $typ", items.checkItemIsType(name, typ))
    }
    ergebnis += Pruefung("Thing astro:sun:zuhause ONLINE",
        ThingTester(client).isThingOnline("astro:sun:zuhause"))
    ergebnis += Pruefung("Regel restbuch-1 aktiv",
        RuleTester(client).isRuleActive("restbuch-1"))
    // Ein echter Funktionstest: Licht schalten, Zustand abwarten, alten Zustand zurück
    ergebnis += Pruefung("Deckenlicht schaltet",
        items.testSwitch("Wohnzimmer_Licht", "ON", "ON", timeout = 5))
    return ergebnis
}
