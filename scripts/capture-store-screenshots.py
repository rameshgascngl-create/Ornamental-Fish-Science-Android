#!/usr/bin/env python3
import os
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

OUT = Path(os.environ.get("SCREENSHOT_DIR", "store-screenshots"))
OUT.mkdir(parents=True, exist_ok=True)

def run(*args, check=True, capture=True):
    cmd = ["adb", *args]
    if capture:
        return subprocess.run(cmd, check=check, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT).stdout
    subprocess.run(cmd, check=check)
    return ""

def wait(seconds=1.4):
    time.sleep(seconds)

def dump_nodes():
    run("shell", "uiautomator", "dump", "/sdcard/window.xml")
    xml = run("shell", "cat", "/sdcard/window.xml")
    root = ET.fromstring(xml)
    return list(root.iter("node"))

def bounds_center(bounds):
    m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds or "")
    if not m:
        return None
    x1,y1,x2,y2 = map(int,m.groups())
    return ((x1+x2)//2, (y1+y2)//2)

def find_text(target, contains=False):
    for n in dump_nodes():
        text = (n.attrib.get("text") or "").strip()
        desc = (n.attrib.get("content-desc") or "").strip()
        vals = [text, desc]
        for v in vals:
            if (v == target) or (contains and target in v):
                c = bounds_center(n.attrib.get("bounds"))
                if c:
                    return c
    return None

def tap_text(target, contains=False, retries=3):
    for _ in range(retries):
        c=find_text(target, contains)
        if c:
            run("shell","input","tap",str(c[0]),str(c[1]))
            wait()
            return True
        wait(0.8)
    raise RuntimeError(f"UI text not found: {target!r}")

def scroll_up():
    run("shell","input","swipe","540","1550","540","620","350")
    wait()

def ensure_visible(target, contains=False, max_scrolls=6):
    c=find_text(target, contains)
    if c:
        return c
    for _ in range(max_scrolls):
        scroll_up()
        c=find_text(target, contains)
        if c:
            return c
    raise RuntimeError(f"Could not make visible: {target!r}")

def tap_visible(target, contains=False):
    c=ensure_visible(target, contains)
    run("shell","input","tap",str(c[0]),str(c[1]))
    wait()

def screenshot(name):
    path=OUT/f"{name}.png"
    with open(path,"wb") as f:
        subprocess.run(["adb","exec-out","screencap","-p"],check=True,stdout=f)
    print(path)

def press_back():
    run("shell","input","keyevent","KEYCODE_BACK")
    wait()

# Stabilise screen and app.
run("shell","wm","size","1080x1920")
run("shell","wm","density","420")
run("shell","settings","put","system","font_scale","1.0")
run("shell","settings","put","system","accelerometer_rotation","0")
run("shell","settings","put","system","user_rotation","0")
run("shell","am","force-stop","com.tnfisheries.ornamentalfish")
run("shell","monkey","-p","com.tnfisheries.ornamentalfish","-c","android.intent.category.LAUNCHER","1")

# Wait for the real Compose Home screen rather than capturing the Android splash screen.
for _ in range(30):
    if find_text("Explore the science of ornamental fishes") is not None:
        break
    wait(0.7)
else:
    raise RuntimeError("Home screen did not become ready after launch")
wait(1)

# 1. Home
screenshot("01_home_native")

# 2. Atlas
tap_text("Atlas")
wait(1)
screenshot("02_species_atlas")

# 3. Oranda detail
tap_visible("Oranda (goldfish variety)")
wait(1)
screenshot("03_oranda_species_detail")

# Return to Atlas and then Learn.
press_back()
tap_text("Learn")
wait(1)

# 4. English lesson
screenshot("04_learn_english")

# 5. Tamil lesson
tap_text("தமிழ்")
wait(1)
screenshot("05_learn_tamil")

# Return to English for remaining listing screens.
tap_text("English")
wait(0.8)

# 6. Quiz
tap_text("Quiz")
wait(1)
screenshot("06_self_assessment_quiz")

# 7. Tools
tap_text("Tools")
wait(1)
screenshot("07_scientific_calculators")

# 8. About & Privacy
tap_text("Home")
wait(1)
tap_visible("About & Privacy")
wait(1)
screenshot("08_about_privacy")

print("Captured", len(list(OUT.glob("*.png"))), "screenshots")
