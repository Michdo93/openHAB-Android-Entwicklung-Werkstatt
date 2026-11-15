// Kapitel 20: Benachrichtigungen bei Ereignissen – ein Vordergrunddienst hört openHAB zu
package de.openhabbuch.ohapp.waechter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.glance.appwidget.updateAll
import de.openhabbuch.ohapp.OhApp
import de.openhabbuch.ohapp.daten.OpenHABSmartHome
import de.openhabbuch.ohapp.widget.LichtWidget
import de.openhabbuch.ohapp.widget.WIDGET_ITEM
import io.github.michdo93.openhab.Json
import io.github.michdo93.openhab.OpenHAB
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class Waechter : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var manager: NotificationManager
    private var nummer = 100

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("dienst", "Wächter läuft",
            NotificationManager.IMPORTANCE_MIN))
        manager.createNotificationChannel(NotificationChannel("meldungen", "Meldungen",
            NotificationManager.IMPORTANCE_HIGH))
        // Ein Vordergrunddienst muss sofort eine dauerhafte Benachrichtigung zeigen
        val laufend = Notification.Builder(this, "dienst")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Wächter hört openHAB zu").build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, laufend, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, laufend)
        }
        scope.launch { zuhoeren() }
    }

    private suspend fun zuhoeren() {
        val v = (application as OhApp).einstellungen.verbindung.first()
        if (!v.vollstaendig) return stopSelf()
        // Welche Items melden? Alle mit Metadaten im Namensraum "ohapp" (Kapitel 11)
        val antwort = OpenHAB(v.url, token = v.token).items
            .getItems(metadata = "ohapp", fields = "name,label,metadata")
        val regeln = Json.list(antwort).mapNotNull { item ->
            val m = Json.get(item, "metadata", "ohapp") ?: return@mapNotNull null
            if (Json.get(m, "value") != "melden") return@mapNotNull null
            item["name"].toString() to Regel(item["label"]?.toString() ?: "",
                Json.str(m, "config", "wenn"))
        }.toMap()

        OpenHABSmartHome(v).wechsel().collect { w ->
            val regel = regeln[w.item]
            if (regel != null && (regel.wenn == null || regel.wenn == w.neu)) {
                melden(regel.label, "ist jetzt ${w.neu}")
            }
            if (w.item == WIDGET_ITEM) LichtWidget().updateAll(this)   // Widget live halten
        }
    }

    private fun melden(titel: String, text: String) {
        manager.notify(nummer++, Notification.Builder(this, "meldungen")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titel).setContentText(text).setAutoCancel(true).build())
    }

    override fun onDestroy() {
        scope.cancel()                                    // schließt auch den Strom
        super.onDestroy()
    }

    private data class Regel(val label: String, val wenn: String?)
}
