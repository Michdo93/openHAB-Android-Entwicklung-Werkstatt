#!/bin/bash
# Startet ein frisches openHAB 5 mit der Übungskonfiguration aus openhab-conf/,
# legt einen Benutzer und ein API-Token an und schreibt OPENHAB_URL/OPENHAB_TOKEN
# nach $GITHUB_ENV.  Aufruf aus dem Hauptordner: bash werkstatt/openhab-start.sh
set -euo pipefail
VERSION=${OPENHAB_VERSION:-5.2.3}
ZIEL=${OPENHAB_DIR:-$HOME/openhab}
mkdir -p "$ZIEL"
cd "$ZIEL"
if [ ! -x start.sh ]; then
  curl -fsSL -o oh.zip "https://github.com/openhab/openhab-distro/releases/download/$VERSION/openhab-$VERSION.zip" \
    || curl -fsSL -o oh.zip "https://openhab.jfrog.io/artifactory/libs-release/org/openhab/distro/openhab/$VERSION/openhab-$VERSION.zip"
  unzip -q oh.zip && rm oh.zip
fi
cd - > /dev/null
for d in items things rules sitemaps persistence; do
  cp -r openhab-conf/$d/. "$ZIEL/conf/$d/"
done
cp werkstatt/openhab/addons.cfg "$ZIEL/conf/services/addons.cfg"
(cd "$ZIEL" && nohup ./start.sh server > "$ZIEL/openhab.out" 2>&1 &)
echo "warte auf openHAB ..."
for i in $(seq 1 120); do
  code=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/rest/ || true)
  [ "$code" = "200" ] && break
  sleep 5
done
echo "REST: $code"
# erster Administrator über die Anmeldeseite, dann ein API-Token über /createApiToken
PW=werkstatt-$RANDOM$RANDOM
R="http://localhost/werkstatt"
curl -s -o /dev/null -w "Benutzer anlegen: %{http_code}\n" -X POST http://127.0.0.1:8080/auth \
  --data-urlencode "username=admin" --data-urlencode "password=$PW" \
  --data-urlencode "passwordRepeat=$PW" --data-urlencode "response_type=code" \
  --data-urlencode "client_id=$R" --data-urlencode "redirect_uri=$R" \
  --data-urlencode "scope=admin" --data-urlencode "state=1"
SEITE=$(curl -s -X POST http://127.0.0.1:8080/createApiToken \
  --data-urlencode "username=admin" --data-urlencode "password=$PW" \
  --data-urlencode "token_name=werkstatt" --data-urlencode "token_scope=")
TOKEN=$(echo "$SEITE" | grep -o 'oh\.werkstatt\.[A-Za-z0-9]*' | head -1 || true)
if [ -z "$TOKEN" ]; then
  echo "$SEITE" | sed 's/<[^>]*>/ /g' | tr -s ' ' | head -c 600; echo
  konsole() { "$ZIEL/runtime/bin/client" -h 127.0.0.1 -a 8101 -u openhab -p habopen "$@"; }
  konsole "openhab:users add admin $PW administrator" || true
  TOKEN=$(konsole "openhab:users addApiToken admin werkstatt2 admin" | grep -o 'oh\.[A-Za-z0-9]*\.[A-Za-z0-9]*' | head -1 || true)
fi
[ -n "$TOKEN" ] || { echo "kein Token"; exit 1; }
echo "::add-mask::$TOKEN"
{ echo "OPENHAB_URL=http://127.0.0.1:8080"; echo "OPENHAB_TOKEN=$TOKEN"; } >> "${GITHUB_ENV:-/dev/null}"
printf 'OPENHAB_URL=http://127.0.0.1:8080\nOPENHAB_TOKEN=%s\n' "$TOKEN" > .env
# warten, bis Add-ons (astro, inmemory) und die Übungsitems geladen sind
for i in $(seq 1 60); do
  n=$(curl -s -H "Authorization: Bearer $TOKEN" "http://127.0.0.1:8080/rest/items?fields=name" | grep -o '"name"' | wc -l)
  s=$(curl -s -H "Authorization: Bearer $TOKEN" "http://127.0.0.1:8080/rest/persistence" | grep -c inmemory || true)
  t=$(curl -s -H "Authorization: Bearer $TOKEN" "http://127.0.0.1:8080/rest/things/astro:sun:zuhause" | grep -c ONLINE || true)
  [ "$n" -ge 16 ] && [ "$s" -ge 1 ] && [ "$t" -ge 1 ] && break
  sleep 5
done
echo "Items: $n, inmemory: $s, astro online: $t"
