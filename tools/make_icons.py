#!/usr/bin/env python3
"""Generate Android launcher icons from tools/app_icon_source.png (needs Pillow).
Usage: python3 tools/make_icons.py
Writes adaptive foreground (66% safe zone) into res/mipmap-*/ic_launcher_foreground.png and legacy icons."""
import sys, os
from PIL import Image
SRC = os.path.join(os.path.dirname(__file__), "app_icon_source.png")
RES = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res")
if not os.path.exists(SRC): sys.exit("Missing tools/app_icon_source.png")
img = Image.open(SRC).convert("RGBA")
sizes = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
for d, s in sizes.items():
    out = os.path.join(RES, f"mipmap-{d}"); os.makedirs(out, exist_ok=True)
    img.resize((s, s), Image.LANCZOS).save(os.path.join(out, "ic_launcher_legacy.png"))
    fg = s * 108 // 48; canvas = Image.new("RGBA", (fg, fg), (0, 0, 0, 0))
    inner = int(fg * 0.66); logo = img.resize((inner, inner), Image.LANCZOS)
    canvas.paste(logo, ((fg - inner) // 2,) * 2, logo)
    canvas.save(os.path.join(out, "ic_launcher_foreground.png"))
print("Done. Then delete res/drawable/ic_launcher_foreground.xml (placeholder) and rebuild.")
