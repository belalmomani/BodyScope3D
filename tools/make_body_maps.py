#!/usr/bin/env python3
"""Render 2D front/back figures of the body (from tools/body_source.glb) for the share-image report.
Output: app/src/main/res/drawable-nodpi/body_front.png / body_back.png (500x955).
Extents MUST match BodyMap in ReportRenderer.kt: x in [-0.5, 0.5] m, z in [-0.02, 1.89] m (source frame: Z up, front = -Y, person's right = -X)."""
import json, struct, os, numpy as np
import matplotlib; matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.collections import PolyCollection
H = os.path.dirname(os.path.abspath(__file__)); OUT = os.path.join(H, "..", "app", "src", "main", "res", "drawable-nodpi")
X0, X1, Z0, Z1 = -0.5, 0.5, -0.02, 1.89
b = open(os.path.join(H, "body_source.glb"), "rb").read(); jl = struct.unpack("<I", b[12:16])[0]; J = json.loads(b[20:20 + jl]); BIN = b[20 + jl + 8:]
def acc(i, dt, n):
    a = J["accessors"][i]; bv = J["bufferViews"][a["bufferView"]]
    return np.frombuffer(BIN, dt, a["count"] * n, bv.get("byteOffset", 0) + a.get("byteOffset", 0)).reshape(-1, n)
tris = []
for n in J["nodes"][1:]:
    pr = J["meshes"][n["mesh"]]["primitives"][0]; P = acc(pr["attributes"]["POSITION"], np.float32, 3).astype(float); T = acc(pr["indices"], np.uint32, 1).reshape(-1, 3)
    col = np.array(J["materials"][pr.get("material", 0)].get("pbrMetallicRoughness", {}).get("baseColorFactor", [0.8, 0.8, 0.8, 1])[:3])
    tris += [(P[t], col) for t in T]
def render(back):
    fig = plt.figure(figsize=(5, 9.55), dpi=100); ax = fig.add_axes([0, 0, 1, 1]); ax.set_xlim(X1, X0) if back else ax.set_xlim(X0, X1); ax.set_ylim(Z0, Z1); ax.axis("off")
    sgn = 1 if back else -1                                    # near = smaller y for front view, larger y for back view
    light = np.array([0.35, -0.55 * sgn * -1, 0.6]); light /= np.linalg.norm(light)
    polys = []; cols = []; depth = []
    for v, c in tris:
        nrm = np.cross(v[1] - v[0], v[2] - v[0]); ln = np.linalg.norm(nrm)
        if ln < 1e-12: continue
        nrm /= ln; cen = v.mean(0)
        if np.dot(nrm, [0, -1 * (-1 if back else 1), 0]) < -0.2 and False: continue
        lam = 0.62 + 0.38 * abs(np.dot(nrm, light))
        polys.append(v[:, [0, 2]]); cols.append(np.clip(c * lam, 0, 1)); depth.append(cen[1] * (-1 if back else 1))
    order = np.argsort(depth)[::-1]                              # far first
    ax.add_collection(PolyCollection([polys[i] for i in order], facecolors=[cols[i] for i in order], edgecolors=[cols[i] * 0.85 for i in order], linewidths=0.25))
    fig.savefig(os.path.join(OUT, "body_back.png" if back else "body_front.png"), transparent=True); plt.close(fig)
os.makedirs(OUT, exist_ok=True); render(False); render(True); print("body maps written")
