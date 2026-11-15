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
adb shell svc power stayon true
adb shell settings put global stay_on_while_plugged_in 7
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
$UI dump wear-vor-start
for versuch in 1 2 3 4; do
  adb shell input keyevent KEYCODE_WAKEUP
  adb shell am start -W -n de.openhabbuch.wear/.MainActivity
  if $UI wait "Deckenlicht" 30; then
    # ein ruhiges Bild abwarten: zwei gleiche Aufnahmen hintereinander, App noch vorn
    adb exec-out screencap -p > /tmp/w0.png
    for k in 1 2 3 4 5 6 7 8 9 10; do
      sleep 0.6
      adb exec-out screencap -p > /tmp/w1.png
      if cmp -s /tmp/w0.png /tmp/w1.png && $UI wait "Deckenlicht" 1 > /dev/null; then
        cp /tmp/w1.png "$OUT/wear.png"; echo "ruhiges Bild nach $k Aufnahmen"; break
      fi
      mv /tmp/w1.png /tmp/w0.png
    done
    $UI dump wear
    [ -f "$OUT/wear.png" ] && break
  fi
  $UI dump wear-versuch-$versuch
done
$UI tap "Leselampe" && sleep 3
echo "Leselampe nach Tippen: $(curl -s -H "$H" "$OPENHAB_URL/rest/items/Wohnzimmer_Dimmer/state")"
adb exec-out screencap -p > "$OUT/wear-nach-tippen.png"
adb logcat -d -v brief '*:E' | grep -i "openhabbuch\|AndroidRuntime" | head -60 > ergebnisse/wear-logcat-fehler.txt
adb logcat -d > ergebnisse/wear-logcat.txt
exit 0
