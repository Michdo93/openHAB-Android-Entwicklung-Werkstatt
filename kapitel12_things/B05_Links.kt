// Kapitel 12: Items mit Channels verknüpfen – mit und ohne Profil
package kapitel12_things

import io.github.michdo93.openhab.Items
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.Links
import io.github.michdo93.openhab.OpenHABClient
import io.github.michdo93.openhab.ProfileTypes
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

private const val SONNE = "astro:sun:zuhause"

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val client = OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN"))
    val items = Items(client)
    val links = Links(client)

    // Zwei neue Items für Sonnenhöhe und Tagesphase
    items.addOrUpdateItem("Sonne_Hoehe", mapOf("type" to "Number:Angle",
        "name" to "Sonne_Hoehe", "label" to "Sonnenhöhe", "groupNames" to listOf("gHaus")))
    items.addOrUpdateItem("Sonne_Phase", mapOf("type" to "String",
        "name" to "Sonne_Phase", "label" to "Tagesphase", "groupNames" to listOf("gHaus")))

    // Welche Profile passen zwischen diesen Channel und ein Number:Angle-Item?
    val profile = ProfileTypes(client).getProfileTypes("astro:elevation", "Number:Angle")
    println("passende Profile: ${Json.list(profile).map { it["uid"] }}")

    // Link ohne Profil und Link mit Profil "offset" (+90° -> 0° bis 180°)
    println("Link 1: "
        + links.linkItemToChannel("Sonne_Phase", "$SONNE:phase#name", mapOf()))
    println("Link 2: " + links.linkItemToChannel("Sonne_Hoehe", "$SONNE:position#elevation",
        mapOf("profile" to "system:offset", "offset" to "90 °")))
    delay(2000)  // das Binding liefert den Wert beim Verknüpfen
    println("Sonne_Phase = ${items.getItemState("Sonne_Phase")}")
    println("Sonne_Hoehe = ${items.getItemState("Sonne_Hoehe")} (Höhe + 90°)")
    for (link in Json.list(links.getLinks(itemName = "Sonne_Hoehe"))) {
        println("gespeichert: ${link["itemName"]} -> ${link["channelUID"]} "
            + "${link["configuration"]}")
    }

    // Aufräumen: Links lösen, Items löschen, verwaiste Links prüfen
    println("lösen: ${links.unlinkItemFromChannel("Sonne_Phase", "$SONNE:phase#name")}")
    println("alle Links des Items löschen: ${links.deleteAllLinks("Sonne_Hoehe")}")
    for (name in listOf("Sonne_Hoehe", "Sonne_Phase")) items.deleteItem(name)
    println("verwaiste Links: ${links.getOrphanLinks()}")
}
