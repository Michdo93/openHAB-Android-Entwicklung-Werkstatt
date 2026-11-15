// Kapitel 18: Das Gerät läuft als Vordergrunddienst – auch bei dunklem Bildschirm
package de.openhabbuch.geraet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import io.github.michdo93.openhab.OpenHABClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class GeraetDienst : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var hardware: AndroidHardware

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("geraet", "Gerät",
            NotificationManager.IMPORTANCE_LOW))
        val hinweis = Notification.Builder(this, "geraet")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Smartphone ist ein openHAB-Gerät").build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, hinweis, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, hinweis)
        }
        hardware = AndroidHardware(this)
        val client =
            OpenHABClient(BuildConfig.OPENHAB_URL, token = BuildConfig.OPENHAB_TOKEN)
        scope.launch {
            Geraet(client, hardware, protokoll = { protokoll.value = it }).laufen()
        }
        laeuft.value = true
    }

    override fun onDestroy() {
        scope.cancel()                       // beendet Messen und Ereignisstrom
        hardware.beenden()
        laeuft.value = false
        super.onDestroy()
    }

    companion object {
        /** Für die Anzeige in der Activity: läuft der Dienst, was geschah zuletzt? */
        val laeuft: MutableStateFlow<Boolean> = MutableStateFlow(false)
        val protokoll: MutableStateFlow<String> = MutableStateFlow("–")
    }
}
