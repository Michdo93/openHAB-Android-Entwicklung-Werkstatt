// Kapitel 18: Die echte Hardware des Smartphones – Akku, Lichtsensor, WLAN, Lampe, Ansage
package de.openhabbuch.geraet

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import java.util.Locale

class AndroidHardware(private val context: Context) : Hardware, SensorEventListener {
    private val akku = context.getSystemService(BatteryManager::class.java)
    private val sensoren = context.getSystemService(SensorManager::class.java)
    private val kamera = context.getSystemService(CameraManager::class.java)
    private val wlan = context.applicationContext.getSystemService(WifiManager::class.java)
    private var letzteHelligkeit: Float? = null
    private var sprache: TextToSpeech? = null

    init {
        // Der Lichtsensor meldet Änderungen – wir merken uns den letzten Wert
        sensoren.getDefaultSensor(Sensor.TYPE_LIGHT)?.let {
            sensoren.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        sprache = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) sprache?.language = Locale.GERMAN
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        letzteHelligkeit = event.values[0]
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    @Suppress("DEPRECATION")      // connectionInfo: der einfachste Weg zur Signalstärke
    override fun messen() = Messung(
        akku = akku.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY),
        laedt = akku.isCharging,
        helligkeit = letzteHelligkeit,
        wlan = wlan.connectionInfo?.rssi?.takeIf { it > -127 },
    )

    override fun lampe(an: Boolean) {
        // die Rückkamera mit Blitz suchen und ihre Lampe schalten
        val id = kamera.cameraIdList.firstOrNull {
            kamera.getCameraCharacteristics(it)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return
        kamera.setTorchMode(id, an)
    }

    override fun vibrieren(ms: Long) {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        val effekt = VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator?.vibrate(effekt)
    }

    override fun sagen(text: String) {
        sprache?.speak(text, TextToSpeech.QUEUE_ADD, null, "openhab")
    }

    fun beenden() {
        sensoren.unregisterListener(this)
        sprache?.shutdown()
    }
}
