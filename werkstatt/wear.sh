#!/bin/bash
# Wear-OS-Emulator: App installieren, Lichter anzeigen, ein Licht schalten, Foto.
OUT=ergebnisse/bilder
mkdir -p "$OUT" ergebnisse/ui
exec > >(tee -a ergebnisse/wear.log) 2>&1
UI="python3 werkstatt/ui.py"
H="Authorization: Bearer $OPENHAB_TOKEN"
adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done
sleep 15
adb shell wm size
curl -s -X POST -H "$H" -H "Content-Type: text/plain" "$OPENHAB_URL/rest/items/Wohnzimmer_Licht" --data ON
curl -s -X POST -H "$H" -H "Content-Type: text/plain" "$OPENHAB_URL/rest/items/Wohnzimmer_Dimmer" --data 60
adb install -r -g apps/wear/build/outputs/apk/debug/wear-debug.apk
adb shell input keyevent KEYCODE_WAKEUP
adb shell am start -W -n de.openhabbuch.wear/.MainActivity
$UI wait "Deckenlicht" 60
sleep 3
adb exec-out screencap -p > "$OUT/wear.png"
$UI dump wear
$UI tap "Leselampe" && sleep 3
echo "Leselampe nach Tippen: $(curl -s -H "$H" "$OPENHAB_URL/rest/items/Wohnzimmer_Dimmer/state")"
adb exec-out screencap -p > "$OUT/wear-nach-tippen.png"
adb logcat -d -v brief '*:E' | grep -i "openhabbuch\|AndroidRuntime" | head -60 > ergebnisse/wear-logcat-fehler.txt
exit 0
