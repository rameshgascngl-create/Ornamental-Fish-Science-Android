#!/usr/bin/env python3
"""Build adaptive + density launcher icons from drawable/app_icon.png."""
from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/res/drawable/app_icon.png"
RES = ROOT / "app/src/main/res"
NAVY = (22, 52, 69, 255)
DENS = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}


def _pil():
    try:
        from PIL import Image
        return Image
    except ImportError:
        pass
    for extra in ([], ["--break-system-packages"]):
        try:
            subprocess.check_call(
                [sys.executable, "-m", "pip", "install", "-q", "pillow"] + extra
            )
            from PIL import Image
            return Image
        except Exception:
            continue
    return None


def copy_fallback() -> None:
    fg = RES / "drawable/ic_launcher_fg.png"
    shutil.copyfile(SRC, fg)
    for name in DENS:
        d = RES / f"mipmap-{name}"
        d.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(SRC, d / "ic_launcher.png")
        shutil.copyfile(SRC, d / "ic_launcher_round.png")
    print("launcher icons copied without resize")


def main() -> None:
    if not SRC.exists():
        print("make-launcher-icons: no app_icon.png yet (run fetch-atlas.sh)")
        return
    Image = _pil()
    if Image is None:
        copy_fallback()
        return
    src = Image.open(SRC).convert("RGBA")

    def fit(size, scale, bg=None):
        canvas = Image.new("RGBA", (size, size), bg if bg else (0, 0, 0, 0))
        inner = max(1, int(size * scale))
        icon = src.resize((inner, inner), Image.Resampling.LANCZOS)
        x = (size - inner) // 2
        canvas.paste(icon, (x, x), icon)
        return canvas

    fit(432, 0.66).save(RES / "drawable/ic_launcher_fg.png", optimize=True)
    for name, size in DENS.items():
        d = RES / f"mipmap-{name}"
        d.mkdir(parents=True, exist_ok=True)
        img = fit(size, 0.78, NAVY)
        img.save(d / "ic_launcher.png", optimize=True)
        img.save(d / "ic_launcher_round.png", optimize=True)
    print("launcher icons written")


if __name__ == "__main__":
    main()
