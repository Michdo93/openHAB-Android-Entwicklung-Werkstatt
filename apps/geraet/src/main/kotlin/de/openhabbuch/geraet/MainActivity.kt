// Kapitel 18: Ein Knopf startet und stoppt das Gerät
package de.openhabbuch.geraet

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val erlaubnis = registerForActivityResult(RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            erlaubnis.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val dienst = Intent(this, GeraetDienst::class.java)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    val laeuft by GeraetDienst.laeuft.collectAsStateWithLifecycle()
                    val zuletzt by GeraetDienst.protokoll.collectAsStateWithLifecycle()
                    Column(Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Smartphone als Gerät",
                            style = MaterialTheme.typography.headlineSmall)
                        Text(if (laeuft) "läuft – meldet an ${BuildConfig.OPENHAB_URL}"
                             else "gestoppt")
                        Text("zuletzt: $zuletzt",
                            style = MaterialTheme.typography.bodySmall)
                        Button(onClick = {
                            if (laeuft) stopService(dienst)
                            else startForegroundService(dienst)
                        }) { Text(if (laeuft) "Stoppen" else "Starten") }
                    }
                }
            }
        }
    }
}
