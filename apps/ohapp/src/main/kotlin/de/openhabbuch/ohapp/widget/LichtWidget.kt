// Kapitel 20: Ein Home-Screen-Widget mit Jetpack Glance – antippen schaltet das Licht
package de.openhabbuch.ohapp.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import de.openhabbuch.ohapp.OhApp
import de.openhabbuch.ohapp.daten.OpenHABSmartHome
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

const val WIDGET_ITEM = "Wohnzimmer_Licht"

/** Liest den Zustand frisch aus openHAB; null, wenn noch nichts eingerichtet ist. */
private suspend fun home(context: Context) =
    (context.applicationContext as OhApp).einstellungen.verbindung.first()
        .takeIf { it.vollstaendig }?.let { OpenHABSmartHome(it) }

class LichtWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val zustand = home(context)?.zustand(WIDGET_ITEM)?.getOrNull() ?: "?"
        provideContent {
            GlanceTheme {
                Column(
                    GlanceModifier.fillMaxSize().cornerRadius(16.dp)
                        .background(GlanceTheme.colors.primaryContainer).padding(12.dp)
                        .clickable(actionRunCallback<Umschalten>()),
                ) {
                    Text("Deckenlicht", style = TextStyle(fontSize = 14.sp,
                        color = GlanceTheme.colors.onPrimaryContainer))
                    Text(when (zustand) { "ON" -> "an"; "OFF" -> "aus"; else -> "?" },
                        style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onPrimaryContainer))
                }
            }
        }
    }
}

/** Läuft beim Antippen: Command senden, dann das Widget neu zeichnen. */
class Umschalten : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId,
                                  parameters: ActionParameters) {
        val home = home(context) ?: return
        val neu = if (home.zustand(WIDGET_ITEM).getOrNull() == "ON") "OFF" else "ON"
        home.befehl(WIDGET_ITEM, neu)
        // kurz warten, bis openHAB den neuen Zustand übernommen hat
        withTimeoutOrNull(2000) {
            while (home.zustand(WIDGET_ITEM).getOrNull() != neu) delay(200)
        }
        LichtWidget().update(context, glanceId)
    }
}

/** Der Empfänger, den Android für das Widget aufruft (im Manifest eingetragen). */
class LichtWidgetEmpfaenger : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = LichtWidget()
}
