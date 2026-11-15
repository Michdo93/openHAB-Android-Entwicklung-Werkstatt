# openHAB & Android-Entwicklung – Werkstatt

Werkzeug des Autors, **nicht** das Begleit-Repository zum Buch. Bei jedem Push baut ein
GitHub-Actions-Workflow (`.github/workflows/werkstatt.yml`) die Kotlin-Beispiele und die
Android-Apps, testet alles gegen ein frisch gestartetes openHAB 5, bedient die Apps im
Emulator (Telefon und Wear OS) und schreibt Bildschirmfotos, Protokolle und Testberichte in
den Branch `ergebnisse`.

- `werkstatt/openhab-start.sh` – openHAB 5 mit `openhab-conf/` starten, Token anlegen
- `werkstatt/speedrun/` – Speedrun aller Beispiele (wie im Speedrun-Paket)
- `werkstatt/fotos.sh`, `werkstatt/wear.sh`, `werkstatt/ui.py` – Emulator bedienen, Fotos
- alles andere – Stand des Begleit-Repos `openHAB-Android-Entwicklung-Codes`
