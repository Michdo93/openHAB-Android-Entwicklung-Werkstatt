// Kapitel 17: Die Application – hält, was die ganze App teilt
package de.openhabbuch.ohapp

import android.app.Application
import de.openhabbuch.ohapp.daten.Einstellungen

class OhApp : Application() {
    val einstellungen by lazy { Einstellungen(this) }
}
