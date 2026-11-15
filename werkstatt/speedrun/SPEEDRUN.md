# Speedrun – openHAB & Android-Entwicklung

Nur für den Autor: Werkzeug und Anleitung, um das Begleit-Repository
[openHAB-Android-Entwicklung-Codes](https://github.com/Michdo93/openHAB-Android-Entwicklung-Codes)
komplett durchzutesten. **Gehört nicht in das Begleit-Repository.** Was hier scheitert, wird im
Repository *und* im Buch korrigiert.

Voraussetzungen: openHAB 5 mit der Übungskonfiguration aus `openhab-conf/` (Kapitel 1), JDK 21,
im Begleit-Repository eine `.env` mit `OPENHAB_URL` und `OPENHAB_TOKEN`. Für die Apps zusätzlich
das Android SDK (Android Studio) und `apps/local.properties` mit `sdk.dir`, `openhab.url` und
`openhab.token` – Android Studio legt `sdk.dir` beim ersten Öffnen von `apps/` selbst an.

## Alle Beispiele prüfen (Speedrun.java)

Ein Programm übersetzt die Beispiele mit Gradle, führt sie nacheinander gegen openHAB aus, lässt
die Integrationstests laufen, baut die vier Apps und führt deren Unit-Tests aus. Je Schritt meldet
es **OK**, **WARNUNG** oder **FEHLER**. Es braucht nur das JDK und startet direkt aus dem
Quelltext:

```bash
cd openHAB-Android-Entwicklung-Codes             # das Begleit-Repository
../speedrun/speedrun.sh                          # alles, etwa 3 Minuten plus Gradle
../speedrun/speedrun.sh kapitel14                # nur ein Kapitel
../speedrun/speedrun.sh apps                     # nur die Apps bauen und testen
../speedrun/speedrun.sh --ohne-apps              # ohne Android SDK
../speedrun/speedrun.sh --liste                  # anzeigen, was ausgeführt würde
```

Unter Windows heißt das Skript `..\speedrun\speedrun.bat` (gleiche Argumente). Der Ordner
`speedrun/` liegt **neben** dem Begleit-Repository, nicht darin. Beide Skripte laden die `.env`
des Repositorys. Liegt das Repository woanders, die Umgebungsvariable `REPO` setzen.

Jede Ausgabe steht danach in `speedrun/ergebnisse/<schritt>.txt` und lässt sich mit dem Listing
im Buch vergleichen (Zeitstempel und Messwerte weichen natürlich ab). Der erste Schritt
`kapitel08/gradle_bereit` lädt beim ersten Mal die Bibliotheken von JitPack und Maven Central
und dauert entsprechend; danach geht es schnell.

| Status | Bedeutung |
|---|---|
| OK | Rückgabecode 0, keine Ausnahme |
| WARNUNG | beendet, aber die Ausgabe enthält `error` – bitte die Datei ansehen |
| FEHLER | Stacktrace, Rückgabecode ≠ 0 oder Zeitüberschreitung |

Vier Beispiele zeigen **absichtlich** Fehler und gelten trotzdem als OK:
`kapitel09/B05_HttpRoh` (unbekanntes Item), `kapitel10/B05_FehlerBehandeln`,
`kapitel11/B01_ItemsCrud` (Item nach dem Löschen) und `kapitel14/B01_Persistenzdienste`
(rrd4j liefert keine Item-Liste).

> **Achtung:** Die Beispiele schalten Items und legen Items, Things, Regeln und Sitemaps an und
> löschen sie wieder. Nur gegen eine Übungsinstanz mit `openhab-conf/` ausführen, nie gegen das
> eigene Zuhause.

Zuletzt durchgelaufen (Kotlin-Beispiele, ohne Gradle und Apps) mit openHAB 5.2.3, JDK 21,
android-openhab-rest-client 2.0.0 und android-openhab-test-suite 2.0.0: 55 × OK, 0 × WARNUNG,
0 × FEHLER. Die Integrationstests aus Kapitel 19 laufen mit 3 von 3 Tests grün.

## Beispiele einzeln ausführen

Alle Programme werden aus dem Hauptordner des Repositorys gestartet:
`./starte.sh <datei.kt>` bzw. `starte.bat <datei.kt>` (übersetzt bei Bedarf, lädt `.env`).

| Kapitel | Aufruf | Hinweise |
|---|---|---|
| 8, 9 | `./starte.sh kapitel08_kotlin/B01_WerteUndTypen.kt` usw. | ohne openHAB; 09/B06 läuft bis Strg+C, `DEMO_SEKUNDEN=3` beendet nach 3 s |
| 10–13 | `./starte.sh kapitel10_erste_schritte/B01_Verbinden.kt` usw. | in der Reihenfolge der Nummern; 13/B03 ändert die Regel aus 13/B02 |
| 14 | `B02_Testdaten.kt`, dann `B03_Verlauf.kt` | B03 und B06 brauchen die Testdaten aus B02; nach `B04_DatenLoeschen.kt` B02 noch einmal ausführen. B05 installiert die MAP-Transformation (Internet nötig) |
| 15 | `B02_SitemapAnlegen.kt`, dann `B03_SitemapEreignisse.kt` | B03 braucht die Sitemap aus B02 |
| 16 | `./starte.sh kapitel16_ereignisse/B05_RobusterListener.kt --trennen --dauer=20` | ohne `--dauer` bis Strg+C – in einem zweiten Terminal oder in der Main UI ein Item schalten |
| 19 | `./gradlew test` | Integrationstests schalten Items wirklich; ohne `OPENHAB_TOKEN` werden sie übersprungen |
| 20 | `./starte.sh kapitel20_projekte/B01_MeldungenEinrichten.kt` | richtet die Metadaten `ohapp` für den Wächter der App ein |

## Die Apps prüfen (Emulator oder Smartphone)

Der Speedrun baut die Apps und führt die Unit-Tests aus; die Bedienung prüft man von Hand. Die
Bildschirmfotos für das Buch entstehen dabei gleich mit (Emulator: Pixel 8, Android 15,
helles Design; Kamera-Symbol in der Emulator-Leiste oder `adb exec-out screencap -p > bild.png`).

| App | Prüfen | Bild fürs Buch |
|---|---|---|
| `erste-app` | Schalter schaltet `Wohnzimmer_Licht`, Zustand stimmt mit der Main UI überein | `erste-app.png` |
| `ohapp` | Einstellungen: Adresse und Token speichern; Lichter schalten und dimmen; Änderungen aus der Main UI erscheinen sofort | `ohapp.png` |
| `ohapp` | Häkchen oben rechts: Vorab-Test, alle Prüfungen grün | `vorabtest.png` |
| `ohapp` | Wächter einschalten (Benachrichtigungen erlauben), vorher `kapitel20_projekte/B01_MeldungenEinrichten.kt`; Haustür in der Main UI öffnen → Benachrichtigung | `benachrichtigung.png` |
| `ohapp` | Widget auf den Startbildschirm ziehen, Licht umschalten; Kachel in den Schnelleinstellungen | `widget.png` |
| `geraet` | Dienst starten; in der Main UI erscheinen `gHandy` und die `Handy_*`-Items; `Handy_Aktion` auf `VIBRIEREN`, `SAGE:Hallo`, `MESSEN` setzen | `geraet.png` |
| `wear` | Wear-OS-Emulator (Wear OS 4+), Lichter schalten | `wear.png` |

Die Bilder kommen nach `book/bilder/` des Buchprojekts; das Buch zeigt bis dahin Platzhalter.
Nach dem Test mit `geraet` die Items `gHandy` und `Handy_*` in der Main UI löschen, damit die
Ausgaben der Kapitel 10 und 14 wieder zum Buch passen.

## Wenn etwas nicht klappt

1. `./starte.sh kapitel10_erste_schritte/B01_Verbinden.kt` – erreicht das Programm openHAB,
   stimmt das Token?
2. Ist die Übungskonfiguration geladen (`Wohnzimmer_Licht`, Regel `restbuch-1`, Thing
   `astro:sun:zuhause`)? `./gradlew test` prüft das in Sekunden.
3. `java -version` muss 21 oder neuer zeigen; Gradle nimmt sonst `JAVA_HOME`.
4. Die App erreicht openHAB nicht: im Emulator `http://10.0.2.2:8080`, auf dem Smartphone die
   Adresse im Heimnetz (nicht `127.0.0.1`); der Server muss Verbindungen von außen annehmen.
5. Den Ordner `speedrun/ergebnisse/` und die Ausgabe von `java -version` mitschicken – dann lässt
   sich der Fehler im Repository und, falls nötig, im Buch korrigieren.
