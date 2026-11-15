// Kapitel 12: Thing-Actions – das Astro-Binding rechnet die Sonnenhöhe aus
package kapitel12_things

import io.github.michdo93.openhab.Actions
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHABClient
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.runBlocking

private const val SONNE = "astro:sun:zuhause"
private val FORMAT = DateTimeFormatter.ofPattern("dd.MM., HH:mm")

fun main() = runBlocking {
    val url = System.getenv("OPENHAB_URL") ?: "http://127.0.0.1:8080"
    val actions = Actions(OpenHABClient(url, token = System.getenv("OPENHAB_TOKEN")))

    // Die UID enthält einen Hash hinter "#" – wir merken sie uns über den Namen
    val uid = mutableMapOf<String, String>()
    for (a in Json.list(actions.getActions(SONNE, "de"))) {
        val name = a["actionUid"].toString().substringBefore('#')
        uid[name] = a["actionUid"].toString()
        val eingaben = Json.list(a["inputs"]).joinToString(", ") { it["name"].toString() }
        println("  %-26s (%s) – %s".format(name, eingaben, a["description"]))
    }

    suspend fun hoehe(zeit: ZonedDateTime): String {
        val antwort = actions.executeAction(SONNE, uid.getValue("astro.getElevation"),
            mapOf("date" to zeit.toInstant().toString()))
        val text = Json.str(antwort, "result") ?: "NaN"            // z. B. "-56.97 °"
        val grad = text.substringBefore(' ').toDouble()
        return "%6.1f".format(grad)
    }

    println("\nSonnenhöhe über den Tag (jetzt und die nächsten Stunden):")
    val jetzt = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS)
    for (stunden in 0 until 24 step 3) {
        val zeit = jetzt.plusHours(stunden.toLong())
        println("  ${FORMAT.format(zeit)}  ${hoehe(zeit)}°")
    }
    for (monat in listOf(3, 6, 9, 12)) {
        val tag = jetzt.withMonth(monat).withDayOfMonth(21).withHour(12)
        println("  ${FORMAT.format(tag)}  ${hoehe(tag)}°")
    }
}
