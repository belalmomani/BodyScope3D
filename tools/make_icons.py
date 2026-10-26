#!/usr/bin/env python3
"""Build Android launcher resources from tools/app_icon_source.png (needs Pillow, numpy).
Output: adaptive foreground/background PNGs per density, in-app logo (drawable-nodpi/app_logo.png), Play Store icon (docs/store/ic_launcher_512.png).
Source art is an RGB rounded square on a near-white canvas: the white corners are removed (made transparent) first.
Adaptive foreground = art at 66/108 of the canvas (inside Android's safe zone); background = deep-blue gradient sampled from the art's own edges."""
import os
import numpy as np
from PIL import Image, ImageDraw, ImageFilter
H = os.path.dirname(os.path.abspath(__file__)); ROOT = os.path.join(H, ".."); RES = os.path.join(ROOT, "app", "src", "main", "res")
src = Image.open(os.path.join(H, "app_icon_source.png")).convert("RGB"); W = src.size[0]

# 1) remove the near-white outer corners (flood fill from the four corners), erode 2px, smooth the edge
tmp = src.copy()
for xy in ((0, 0), (W - 1, 0), (0, W - 1), (W - 1, W - 1)): ImageDraw.floodfill(tmp, xy, (255, 0, 255), thresh=45)
t = np.array(tmp); flooded = (t[..., 0] == 255) & (t[..., 1] == 0) & (t[..., 2] == 255)
alpha = Image.fromarray(np.where(flooded, 0, 255).astype("uint8")).filter(ImageFilter.MinFilter(5)).filter(ImageFilter.GaussianBlur(1.2))
art = src.convert("RGBA"); art.putalpha(alpha)

# 2) background: vertical deep-blue gradient sampled from the art's own edges (top -> bottom)
TOP, BOT = np.array([8, 32, 80]), np.array([0, 44, 118])
def bg(px):
    g = (TOP[None, :] + (BOT - TOP)[None, :] * np.linspace(0, 1, px)[:, None]).astype("uint8")
    return Image.fromarray(np.repeat(g[:, None, :], px, axis=1)).convert("RGB")
def fg(px):
    canvas = Image.new("RGBA", (px, px), (0, 0, 0, 0)); s = round(px * 66 / 108)
    canvas.paste(art.resize((s, s), Image.LANCZOS), ((px - s) // 2, (px - s) // 2), art.resize((s, s), Image.LANCZOS)); return canvas

for d, px in {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}.items():
    out = os.path.join(RES, f"drawable-{d}"); os.makedirs(out, exist_ok=True)
    fg(px).save(os.path.join(out, "ic_launcher_foreground.png"), optimize=True); bg(px).convert("RGB").save(os.path.join(out, "ic_launcher_background.png"), optimize=True)
os.makedirs(os.path.join(RES, "drawable-nodpi"), exist_ok=True); art.resize((512, 512), Image.LANCZOS).save(os.path.join(RES, "drawable-nodpi", "app_logo.png"), optimize=True)
store = os.path.join(ROOT, "docs", "store"); os.makedirs(store, exist_ok=True)
full = bg(512).convert("RGBA"); full.alpha_composite(art.resize((512, 512), Image.LANCZOS))
full.convert("RGB").save(os.path.join(store, "ic_launcher_512.png"), optimize=True)
print("icons written")
