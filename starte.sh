#!/bin/sh
# Startet ein Beispiel – übersetzt bei Bedarf und lädt die Zugangsdaten aus .env:
#   ./starte.sh kapitel10_erste_schritte/B01_Verbinden.kt [Argumente]
set -e
datei=${1:?Aufruf: ./starte.sh kapitel10_erste_schritte/B01_Verbinden.kt}
shift
klasse=$(echo "${datei%.kt}" | tr / .)Kt
./gradlew -q bereit
set -a
[ -f .env ] && . ./.env
set +a
exec java -cp "build/classes/kotlin/main:build/lib/*" "$klasse" "$@"
