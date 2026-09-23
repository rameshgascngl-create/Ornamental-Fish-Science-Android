#!/usr/bin/env python3
"""Upgrade frozen v2.4.4 app.js / app.css / index.html to the v2.5.0 bridge."""
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
JS = ROOT / "app/src/main/assets/js/app.js"
CSS = ROOT / "app/src/main/assets/css/app.css"
HTML = ROOT / "app/src/main/assets/index.html"

def die(msg):
    print(msg, file=sys.stderr)
    sys.exit(1)

def patch_js(s: str) -> str:
    s = s.replace("/* Ornamental Fish App 2.4.1 responsive hotfix */",
                  "/* Ornamental Fish App 2.5.0 — native chrome + AndroidBridge */")
    s = s.replace('const VERSION = "2.4.3";', 'const VERSION = "2.5.0";')
    needle = '  const IMG = "fish_atlas/";\n'
    if needle not in s:
        die("app.js: IMG needle missing (already patched?)")
    bridge = '''
  const NATIVE_TAB = { home:"home", atlas:"atlas", india:"atlas", book:"book",
    science:"book", breed:"book", health:"book", lab:"book", quiz:"quiz",
    tools:"tools", marks:"home" };

  const AndroidBridge = {
    hasNative() {
      try { return !!(window.Android && typeof window.Android.pronounce === "function"); }
      catch (e) { return false; }
    },
    pronounce(text, lang) {
      const t = String(text || "").trim();
      if (!t) return;
      if (this.hasNative()) {
        try { window.Android.pronounce(t, lang || "en-IN"); return; } catch (e) {}
      }
      if (window.speechSynthesis) {
        window.speechSynthesis.cancel();
        const u = new SpeechSynthesisUtterance(t);
        u.lang = lang || "en-IN";
        window.speechSynthesis.speak(u);
      }
    },
    share(text) {
      const t = String(text || "").trim();
      if (!t) return;
      if (this.hasNative()) {
        try { window.Android.share(t); return; } catch (e) {}
      }
      if (navigator.share) {
        navigator.share({ title: "Ornamental Fish Science", text: t }).catch(function () {});
        return;
      }
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(t).catch(function () {});
      }
    },
    scheduleReminder(hours, title, body) {
      const h = Number(hours) || 24;
      if (this.hasNative()) {
        try { window.Android.scheduleReminder(h, String(title || ""), String(body || "")); return; } catch (e) {}
      }
      if ("Notification" in window) {
        const fire = function () {
          try { new Notification(title || "Revision reminder", { body: body || "" }); } catch (e) {}
        };
        if (Notification.permission === "granted") setTimeout(fire, Math.max(1, h) * 3600 * 1000);
        else if (Notification.permission !== "denied") {
          Notification.requestPermission().then(function (p) {
            if (p === "granted") setTimeout(fire, Math.max(1, h) * 3600 * 1000);
          });
        }
      }
    },
    exportBookmarks(text) {
      const t = String(text || "");
      if (this.hasNative()) {
        try { window.Android.exportBookmarks(t); return; } catch (e) {}
      }
      const blob = new Blob([t], { type: "text/plain;charset=utf-8" });
      const a = document.createElement("a");
      a.href = URL.createObjectURL(blob);
      a.download = "ornamental-fish-bookmarks.txt";
      document.body.appendChild(a);
      a.click();
      setTimeout(function () { URL.revokeObjectURL(a.href); a.remove(); }, 500);
    },
    notifyTab(tab) {
      const mapped = NATIVE_TAB[tab] || "home";
      if (!this.hasNative()) return;
      try {
        if (typeof window.Android.onTabChanged === "function") window.Android.onTabChanged(mapped);
      } catch (e) {}
    }
  };
  window.AndroidBridge = AndroidBridge;

'''
    s = s.replace(needle, needle + "\n" + bridge, 1)
    s = s.replace(
'''  function setTab(tab) {
    state.tab = tab;
    if (tab !== "atlas") state.fish = null;
    document.querySelectorAll("nav.tabs button").forEach((b) => {
      b.setAttribute("aria-current", b.dataset.tab === tab ? "page" : "false");
    });
    render();
  }''',
'''  function setTab(tab, fromNative) {
    state.tab = tab;
    if (tab !== "atlas") state.fish = null;
    document.querySelectorAll("nav.tabs button").forEach((b) => {
      b.setAttribute("aria-current", b.dataset.tab === tab ? "page" : "false");
    });
    if (!fromNative) AndroidBridge.notifyTab(tab);
    render();
  }''', 1)
    s = s.replace(
'''    const html = `<p><button type="button" class="go" data-go="atlas">← ${t("Atlas","அட்டவணை")}</button>
      <button type="button" class="go" data-mark="${s.id}">${saved ? "★" : "☆"} ${t("Bookmark","புத்தக்குறி")}</button></p>''',
'''    const html = `<p><button type="button" class="go" data-go="atlas">← ${t("Atlas","அட்டவணை")}</button>
      <button type="button" class="go" data-mark="${s.id}">${saved ? "★" : "☆"} ${t("Bookmark","புத்தக்குறி")}</button>
      <button type="button" class="go" data-pronounce="${s.id}">🔊 ${t("Pronounce","உச்சரி")}</button>
      <button type="button" class="go" data-share="${s.id}">⇪ ${t("Share","பகிர்")}</button></p>''', 1)
    s = s.replace(
'''      ${saved.length ? `<p><button type="button" class="go" data-clearmarks="1">${t("Clear all bookmarks","அனைத்துச் சேமிப்பையையும் அழி")}</button></p>` : ""}`;''',
'''      ${saved.length ? `<p>
        <button type="button" class="go" data-exportmarks="1">⇩ ${t("Export bookmarks","சேமிப்புகளை ஏற்று")}</button>
        <button type="button" class="go" data-clearmarks="1">${t("Clear all bookmarks","அனைத்துச் சேமிப்பையையும் அழி")}</button>
      </p>` : ""}`;''', 1)
    s = s.replace(
'''        <p>
          <button type="button" class="go" data-quizact="restart">${t("Restart quiz","மீண்டும் தொடங்கு")}</button>
          <button type="button" class="go" data-quizact="continue">${t("Continue unanswered","விடாத வினா")}</button>
        </p>`;''',
'''        <p>
          <button type="button" class="go" data-quizact="restart">${t("Restart quiz","மீண்டும் தொடங்கு")}</button>
          <button type="button" class="go" data-quizact="continue">${t("Continue unanswered","விடாத வினா")}</button>
          <button type="button" class="go" data-remind="24">⏰ ${t("Remind me to revise tomorrow","நாளை திரும்பப் படிக்க நினைவூட்டு")}</button>
        </p>`;''', 1)
    s = s.replace(
        'const el = ev.target.closest("[data-go],[data-fish],[data-turn],[data-ans],[data-nextq],[data-calc],[data-mark],[data-filter],[data-quizact],[data-clearmarks],[data-page],#langBtn");',
        'const el = ev.target.closest("[data-go],[data-fish],[data-turn],[data-ans],[data-nextq],[data-calc],[data-mark],[data-filter],[data-quizact],[data-clearmarks],[data-exportmarks],[data-pronounce],[data-share],[data-remind],[data-page],#langBtn");',
        1)
    s = s.replace(
        '    if (el.dataset.clearmarks) { state.marks = []; save(); render(); return; }',
'''    if (el.dataset.clearmarks) { state.marks = []; save(); render(); return; }
    if (el.dataset.exportmarks) { exportMarks(); return; }
    if (el.dataset.pronounce) { speakSpecies(el.dataset.pronounce); return; }
    if (el.dataset.share) { shareSpecies(el.dataset.share); return; }
    if (el.dataset.remind) {
      AndroidBridge.scheduleReminder(
        Number(el.dataset.remind) || 24,
        t("Revise ornamental fish quiz","அலங்கார மீன் வினாவை மீள்பார்"),
        t("Open Ornamental Fish Science and review the questions you missed.","அலங்கார மீன் அறிவியலைத் திறந்து தவறிய வினாக்களை மீள்பார்.")
      );
      return;
    }''', 1)
    s = s.replace(
        '      if (el.dataset.go === "india") { state.tab = "atlas"; state.filter = "indigenous"; state.fish = null; render(); return; }',
        '      if (el.dataset.go === "india") { state.tab = "atlas"; state.filter = "indigenous"; state.fish = null; AndroidBridge.notifyTab("atlas"); render(); return; }',
        1)
    s = s.replace(
        '      if (el.dataset.go === "science") { state.tab = "book"; state.bi = Math.max(0, BOOK.findIndex((p) => p.id === "iv1")); save(); render(); return; }',
        '      if (el.dataset.go === "science") { state.tab = "book"; state.bi = Math.max(0, BOOK.findIndex((p) => p.id === "iv1")); save(); AndroidBridge.notifyTab("book"); render(); return; }',
        1)
    s = s.replace(
        '      if (el.dataset.go === "breed") { state.tab = "book"; state.bi = Math.max(0, BOOK.findIndex((p) => p.id === "ii1")); save(); render(); return; }',
        '      if (el.dataset.go === "breed") { state.tab = "book"; state.bi = Math.max(0, BOOK.findIndex((p) => p.id === "ii1")); save(); AndroidBridge.notifyTab("book"); render(); return; }',
        1)
    s = s.replace(
        '      if (el.dataset.go === "health") { state.tab = "book"; state.bi = Math.max(0, BOOK.findIndex((p) => p.id === "iv6")); save(); render(); return; }',
        '      if (el.dataset.go === "health") { state.tab = "book"; state.bi = Math.max(0, BOOK.findIndex((p) => p.id === "iv6")); save(); AndroidBridge.notifyTab("book"); render(); return; }',
        1)
    s = s.replace(
        'if (el.dataset.fish) { state.fish = SPECIES.find((s) => s.id === el.dataset.fish); state.tab = "atlas"; render(); return; }',
        'if (el.dataset.fish) { state.fish = SPECIES.find((s) => s.id === el.dataset.fish); state.tab = "atlas"; AndroidBridge.notifyTab("atlas"); render(); return; }',
        1)
    helpers = '''  function speakSpecies(id) {
    const s = SPECIES.find((x) => x.id === id);
    if (!s) return;
    const name = state.ta ? s.ta : s.en;
    AndroidBridge.pronounce(name, state.ta ? "ta-IN" : "en-IN");
  }
  function shareSpecies(id) {
    const s = SPECIES.find((x) => x.id === id);
    if (!s) return;
    const sci = (s.sci || "").split("(")[0].trim();
    const lines = [
      (state.ta ? s.ta : s.en) + " (" + sci + ")",
      t("Family","குடும்பம்") + ": " + s.family,
      t("Native range","தாயகப் பரவல்") + ": " + val(s,"native","ta_native"),
      t("Habitat","வாழிடம்") + ": " + s.habitat,
      t("Temperament","தன்மை") + ": " + val(s,"temper","ta_temper"),
      t("Diet","உணவு") + ": " + val(s,"diet","ta_diet"),
      t("Breeding","இனப்பெருக்கம்") + ": " + val(s,"breed","ta_breed"),
      "",
      "Ornamental Fish Science v" + VERSION
    ];
    AndroidBridge.share(lines.join("\\n"));
  }
  function exportMarks() {
    const saved = SPECIES.filter((s) => state.marks.includes(s.id));
    const lines = ["Ornamental Fish Science — bookmarks v" + VERSION, ""];
    saved.forEach((s) => {
      lines.push((state.ta ? s.ta : s.en) + " — " + (s.sci || "").split("(")[0].trim());
    });
    if (!saved.length) lines.push("(none)");
    AndroidBridge.exportBookmarks(lines.join("\\n"));
  }

  window.__setNativeTab = function (tab) {
    const allowed = { home:1, atlas:1, book:1, quiz:1, tools:1 };
    if (!allowed[tab]) return;
    setTab(tab, true);
  };

  '''
    s = s.replace("  window.__appBack = function () {", helpers + "  window.__appBack = function () {", 1)
    s = s.replace(
'''  function boot() {
    applyLayout(false);
''',
'''  function boot() {
    if (AndroidBridge.hasNative()) {
      document.documentElement.classList.add("has-native");
      document.body.classList.add("has-native");
    }
    applyLayout(false);
''', 1)
    s = s.replace(
'''    render();
  }
  boot();
})();
''',
'''    render();
    AndroidBridge.notifyTab(state.tab);
  }
  boot();
})();
''', 1)
    return s

def main():
    if not JS.exists():
        die("missing app.js — run scripts/fetch-atlas.sh first")
    text = JS.read_text(encoding="utf-8")
    if 'const VERSION = "2.5.0"' in text and "AndroidBridge" in text:
        print("app.js already at 2.5.0")
    else:
        JS.write_text(patch_js(text), encoding="utf-8")
        print("patched app.js → 2.5.0")
    if CSS.exists():
        css = CSS.read_text(encoding="utf-8")
        if "html.has-native nav.tabs" not in css:
            css = css.replace(
                ".nav-label { max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }",
                ".nav-label { max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }\n\n"
                "html.has-native nav.tabs,\nbody.has-native nav.tabs { display: none !important; }",
                1)
            CSS.write_text(css, encoding="utf-8")
            print("patched app.css")
    if HTML.exists():
        html = HTML.read_text(encoding="utf-8")
        html = html.replace("2.4.4", "2.5.0")
        HTML.write_text(html, encoding="utf-8")
        print("patched index.html version strings")

if __name__ == "__main__":
    main()
