// Kapitel 17: Einstellungen dauerhaft speichern – mit Jetpack DataStore
package de.openhabbuch.ohapp.daten

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.speicher by preferencesDataStore(name = "einstellungen")
private val URL = stringPreferencesKey("url")
private val TOKEN = stringPreferencesKey("token")

class Einstellungen(private val context: Context) {
    /** Die gespeicherte Verbindung – ein Flow, der jede Änderung meldet. */
    val verbindung: Flow<Verbindung> = context.speicher.data.map {
        Verbindung(it[URL] ?: "", it[TOKEN] ?: "")
    }

    suspend fun speichern(v: Verbindung) {
        context.speicher.edit {
            it[URL] = v.url.trim().trimEnd('/')
            it[TOKEN] = v.token.trim()
        }
    }
}
