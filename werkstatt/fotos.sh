#!/bin/bash
# Bedient die Apps im Telefon-Emulator und macht die Bildschirmfotos fürs Buch.
# Läuft im Workflow innerhalb von android-emulator-runner; braucht OPENHAB_URL/-TOKEN.
# Nichts bricht ab: Jeder Schritt protokolliert, Fehler stehen in ergebnisse/fotos.log.
OUT=ergebnisse/bilder
mkdir -p "$OUT" ergebnisse/ui
LOG=ergebnisse/fotos.log
exec > >(tee -a "$LOG") 2>&1
UI="python3 werkstatt/ui.py"
APK=apps
H="Authorization: Bearer $OPENHAB_TOKEN"
rest() { curl -s -X "$1" -H "$H" -H "Content-Type: text/plain" "$OPENHAB_URL/rest$2" ${3:+--data "$3"}; echo; }
foto() { adb exec-out screencap -p > "$OUT/$1.png"; echo "FOTO $1: $(stat -c %s "$OUT/$1.png") Bytes"; $UI dump "$1" > /dev/null; }
schritt() { echo; echo "=== $* ==="; }

schritt "Gerät vorbereiten"
adb wait-for-device
adb root; sleep 3; adb wait-for-device
adb shell "setprop persist.sys.locale de-DE; setprop ctl.restart zygote"
sleep 20; adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done
sleep 10
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell settings put secure show_ime_with_hard_keyboard 0
adb shell settings put global sysui_demo_allowed 1
demo() { adb shell am broadcast -a com.android.systemui.demo -e command "$@" > /dev/null; }
demo enter
demo clock -e hhmm 1000
demo battery -e level 100 -e plugged false
demo network -e wifi show -e level 4 -e mobile show -e level 4 -e datatype none
demo notifications -e visible false
adb shell getprop persist.sys.locale
adb shell wm size

schritt "Zustände in openHAB setzen"
rest POST /items/Wohnzimmer_Licht ON
rest POST /items/Wohnzimmer_Dimmer 60
rest POST /items/Wohnzimmer_Farblicht OFF
rest PUT /items/Wohnzimmer_Temperatur/state "21.5 °C"
rest PUT /items/Flur_Haustuer/state CLOSED
rest POST /items/Kueche_Rollladen 0

schritt "Apps installieren"
for m in erste-app ohapp geraet; do
  adb install -r -g "$APK/$m/build/outputs/apk/debug/$m-debug.apk"
done
adb shell pm grant de.openhabbuch.ohapp android.permission.POST_NOTIFICATIONS
adb shell pm grant de.openhabbuch.geraet android.permission.POST_NOTIFICATIONS

schritt "Kapitel 10: erste App"
adb shell am start -W -n de.openhabbuch.ersteapp/.MainActivity
$UI wait "Deckenlicht: ON" 40
sleep 2
foto erste-app
adb shell am force-stop de.openhabbuch.ersteapp

schritt "Kapitel 17: ohapp einrichten"
adb shell am start -W -n de.openhabbuch.ohapp/.MainActivity
$UI wait "API-Token" 40
$UI dump ohapp-einstellungen-leer
$UI tapedit 0
adb shell input keyevent KEYCODE_MOVE_END
adb shell input text "10.0.2.2:8080"
$UI tapedit 1
adb shell input text "$OPENHAB_TOKEN"
adb shell input keyevent KEYCODE_BACK
sleep 1
$UI dump ohapp-einstellungen-ausgefuellt
$UI tap "Speichern"
$UI wait "Deckenlicht" 40
sleep 3
foto ohapp

schritt "Kapitel 17: Schalten aus der App"
$UI tapcheckable 0
sleep 3
echo "Wohnzimmer_Licht nach Tippen: $(rest GET /items/Wohnzimmer_Licht/state)"
$UI tapcheckable 0
sleep 3
echo "Wohnzimmer_Licht nach zweitem Tippen: $(rest GET /items/Wohnzimmer_Licht/state)"
rest POST /items/Wohnzimmer_Dimmer 35
sleep 3
$UI dump ohapp-nach-aenderung-in-openhab

schritt "Kapitel 19: Vorab-Test"
$UI tap "Vorab-Test"
$UI wait "Weiter" 120
sleep 2
foto vorabtest
$UI tap "Weiter"
sleep 2

schritt "Kapitel 20: Wächter und Benachrichtigung"
java -cp "build/classes/kotlin/main:build/lib/*" kapitel20_projekte.B01_MeldungenEinrichtenKt
$UI tap "Einstellungen"
$UI wait "Wächter" 20
$UI tapcheckable 0
sleep 3
adb shell input keyevent KEYCODE_BACK
adb shell input keyevent KEYCODE_HOME
sleep 5
adb shell dumpsys activity services de.openhabbuch.ohapp | grep -i "ServiceRecord" | head -3
rest PUT /items/Flur_Haustuer/state OPEN
sleep 6
adb shell cmd statusbar expand-notifications
sleep 3
foto benachrichtigung
adb shell cmd statusbar collapse
rest PUT /items/Flur_Haustuer/state CLOSED

schritt "Kapitel 20: Kachel in den Schnelleinstellungen"
adb shell cmd statusbar add-tile de.openhabbuch.ohapp/.kachel.LichtKachel
sleep 2
adb shell cmd statusbar expand-settings
sleep 3
foto kachel
adb shell cmd statusbar collapse

schritt "Kapitel 20: Widget auf dem Startbildschirm"
adb shell input keyevent KEYCODE_HOME
sleep 2
read W H < <(adb shell wm size | grep -o '[0-9]*x[0-9]*' | tail -1 | tr x ' ')
adb shell input swipe $((W/2)) $((H/2)) $((W/2)) $((H/2)) 1500
sleep 2
$UI dump launcher-menue
$UI tap "Widgets" || $UI tap "widgets"
sleep 3
$UI dump widget-liste
for i in 1 2 3 4 5 6; do
  $UI tap "Zuhause" && break
  adb shell input swipe $((W/2)) $((H*3/4)) $((W/2)) $((H/4)) 400
  sleep 1
done
sleep 2
$UI dump widget-app
# neuere Launcher: Vorschau antippen, dann "Hinzufügen"; sonst lange drücken und ziehen
$UI tap "Widget „Deckenlicht“"
sleep 2
$UI dump widget-angetippt
if $UI tap "Hinzufügen" || $UI tap "Zum Startbildschirm hinzufügen" || $UI tap "Add"; then
  sleep 3
else
  $UI longdrag "Widget „Deckenlicht“" $((W/2)) $((H/3))
  sleep 4
fi
$UI dump widget-abgelegt
adb shell input keyevent KEYCODE_HOME
sleep 3
foto widget
$UI tap "Deckenlicht" && sleep 3 && echo "Licht nach Widget: $(rest GET /items/Wohnzimmer_Licht/state)"
foto widget-nach-tippen

schritt "Kapitel 18: das Smartphone als Gerät"
adb shell am start -W -n de.openhabbuch.geraet/.MainActivity
$UI wait "Starten" 30
$UI tap "Starten"
$UI wait "läuft" 30
sleep 8
rest POST /items/Handy_Aktion VIBRIEREN
sleep 3
rest POST /items/Handy_Aktion MESSEN
sleep 3
echo "Handy-Items: $(curl -s -H "$H" "$OPENHAB_URL/rest/items?tags=&fields=name,state" | tr '{' '\n' | grep Handy)"
foto geraet
$UI tap "Stoppen"
sleep 3
adb shell am force-stop de.openhabbuch.geraet
for i in Handy_Akku Handy_Aktion Handy_Antwort Handy_Helligkeit Handy_Laedt Handy_Online Handy_WLAN gHandy; do
  rest DELETE /items/$i > /dev/null
done

schritt "Logcat (Fehler der Apps)"
adb logcat -d -v brief '*:E' | grep -i "openhabbuch\|AndroidRuntime" | head -80 > ergebnisse/logcat-fehler.txt
adb logcat -d > ergebnisse/logcat.txt
ls -la "$OUT"
exit 0
