#!/usr/bin/env python3
"""Kleine UI-Fernbedienung für den Emulator über adb und uiautomator.

  ui.py dump NAME                Bildschirmbaum nach ergebnisse/ui/NAME.xml
  ui.py wait TEXT [SEKUNDEN]     warten, bis ein Element mit TEXT erscheint
  ui.py tap TEXT                 Element mit TEXT (Text oder content-desc) antippen
  ui.py tapcheckable [N]         N-tes ankreuzbares Element (Switch) antippen
  ui.py tapedit N                N-tes Eingabefeld antippen (0 = erstes)
  ui.py longdrag TEXT X Y        Element lange drücken und nach X,Y ziehen
  ui.py center TEXT              Mittelpunkt ausgeben
Rückgabecode 0 = gefunden, 1 = nicht gefunden.
"""
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

AUS = os.environ.get("UI_AUS", "ergebnisse/ui")


def adb(*args, check=False):
    return subprocess.run(["adb", *args], capture_output=True, text=True, check=check).stdout


def baum():
    for _ in range(3):
        adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
        xml = adb("shell", "cat", "/sdcard/ui.xml")
        if xml.strip().startswith("<?xml"):
            try:
                return ET.fromstring(xml[xml.index("<hierarchy"):]), xml
            except (ET.ParseError, ValueError):
                pass
        time.sleep(1)
    return None, ""


def mitte(node):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    return (x1 + x2) // 2, (y1 + y2) // 2


def finde(root, text):
    if root is None:
        return None
    exakt, teil = None, None
    for n in root.iter("node"):
        for wert in (n.get("text") or "", n.get("content-desc") or ""):
            if wert == text and exakt is None:
                exakt = n
            elif text.lower() in wert.lower() and teil is None:
                teil = n
    return exakt if exakt is not None else teil


def main():
    befehl, args = sys.argv[1], sys.argv[2:]
    if befehl == "dump":
        os.makedirs(AUS, exist_ok=True)
        _, xml = baum()
        with open(os.path.join(AUS, args[0] + ".xml"), "w") as f:
            f.write(xml)
        texte = re.findall(r'(?:text|content-desc)="([^"]+)"', xml)
        print(" | ".join(texte)[:1500])
        return 0
    if befehl == "wait":
        frist = time.time() + (float(args[1]) if len(args) > 1 else 30)
        while time.time() < frist:
            root, _ = baum()
            if finde(root, args[0]) is not None:
                print("gefunden:", args[0])
                return 0
            time.sleep(1.5)
        print("NICHT gefunden:", args[0])
        return 1
    root, _ = baum()
    if befehl in ("tap", "center", "longdrag"):
        n = finde(root, args[0])
        if n is None:
            print("NICHT gefunden:", args[0])
            return 1
        x, y = mitte(n)
        if befehl == "center":
            print(x, y)
        elif befehl == "tap":
            adb("shell", "input", "tap", str(x), str(y))
            print("getippt:", args[0], x, y)
        else:
            zx, zy = int(args[1]), int(args[2])
            adb("shell", "input", "motionevent", "DOWN", str(x), str(y))
            time.sleep(1.6)
            for i in range(1, 11):
                adb("shell", "input", "motionevent", "MOVE",
                    str(x + (zx - x) * i // 10), str(y + (zy - y) * i // 10))
                time.sleep(0.08)
            time.sleep(0.8)
            adb("shell", "input", "motionevent", "UP", str(zx), str(zy))
            print("gezogen:", args[0], (x, y), "->", (zx, zy))
        return 0
    if befehl in ("tapcheckable", "tapedit"):
        nr = int(args[0]) if args else 0
        if befehl == "tapcheckable":
            kandidaten = [n for n in root.iter("node") if n.get("checkable") == "true"]
        else:
            kandidaten = [n for n in root.iter("node")
                          if "EditText" in (n.get("class") or "")]
        if len(kandidaten) <= nr:
            print("NICHT gefunden:", befehl, nr)
            return 1
        x, y = mitte(kandidaten[nr])
        adb("shell", "input", "tap", str(x), str(y))
        print("getippt:", befehl, nr, x, y)
        return 0
    print("unbekannt:", befehl)
    return 2


if __name__ == "__main__":
    sys.exit(main())
