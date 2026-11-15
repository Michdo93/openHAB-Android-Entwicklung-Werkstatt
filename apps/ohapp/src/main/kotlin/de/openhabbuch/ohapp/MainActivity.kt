// Kapitel 17: Die Activity verdrahtet ViewModel und Bildschirme
package de.openhabbuch.ohapp

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.openhabbuch.ohapp.daten.OpenHABSmartHome
import de.openhabbuch.ohapp.daten.Verbindung
import de.openhabbuch.ohapp.ui.EinstellungenScreen
import de.openhabbuch.ohapp.ui.HausScreen
import de.openhabbuch.ohapp.ui.HausViewModel
import de.openhabbuch.ohapp.ui.HausZustand
import de.openhabbuch.ohapp.ui.PruefScreen
import de.openhabbuch.ohapp.waechter.Waechter

class MainActivity : ComponentActivity() {
    // Kapitel 20: ab Android 13 muss der Nutzer Benachrichtigungen erlauben
    private val erlaubnis = registerForActivityResult(RequestPermission()) {}

    private fun waechter(an: Boolean) {
        val dienst = Intent(this, Waechter::class.java)
        if (!an) {
            stopService(dienst)
            return
        }
        if (Build.VERSION.SDK_INT >= 33) {
            erlaubnis.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        startForegroundService(dienst)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val einstellungen = (application as OhApp).einstellungen
        // Fabrik: Das ViewModel bekommt seine Abhängigkeiten von außen
        val fabrik = viewModelFactory {
            initializer {
                HausViewModel(einstellungen.verbindung, ::OpenHABSmartHome, einstellungen)
            }
        }
        setContent {
            MaterialTheme {
                Surface {
                    val vm: HausViewModel = viewModel(factory = fabrik)
                    // nur sammeln, solange die App sichtbar ist
                    val zustand by vm.zustand.collectAsStateWithLifecycle()
                    val meldung by vm.meldung.collectAsStateWithLifecycle()
                    val verbindung by einstellungen.verbindung
                        .collectAsState(Verbindung("", ""))
                    // welche Seite gerade sichtbar ist: "haus", "einstellungen", "pruefen"
                    var seite by rememberSaveable { mutableStateOf("haus") }
                    BackHandler(enabled = seite != "haus") { seite = "haus" }

                    when {
                        seite == "einstellungen" || zustand == HausZustand.Einrichten ->
                            EinstellungenScreen(verbindung, onSpeichern = {
                                vm.speichern(it)
                                seite = "haus"
                            }, onWaechter = ::waechter)
                        seite == "pruefen" -> PruefScreen(verbindung) { seite = "haus" }
                        else -> HausScreen(zustand, meldung, vm::schalten, vm::dimmen,
                            vm::meldungGelesen, onPruefen = { seite = "pruefen" },
                            onEinstellungen = { seite = "einstellungen" })
                    }
                }
            }
        }
    }
}
