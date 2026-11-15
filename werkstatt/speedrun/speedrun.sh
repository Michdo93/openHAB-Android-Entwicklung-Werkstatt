#!/bin/sh
# Startet den Speedrun aus dem Begleit-Repository heraus:
#   cd openHAB-Android-Entwicklung-Codes && ../speedrun/speedrun.sh [kapitel14] [--liste]
HIER=$(cd "$(dirname "$0")" && pwd)
set -a
[ -f .env ] && . ./.env
set +a
exec java -Dspeedrun.log="$HIER/ergebnisse" "$HIER/Speedrun.java" "$@"
