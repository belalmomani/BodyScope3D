#!/usr/bin/env python3
"""Split every sufficiently large part of body.glb into 4 geometric pieces so the app can show/hide HALVES of a part
(front / back / anatomical right / anatomical left). Reads tools/body_source.glb (already Y-up fixed), writes assets/models/body.glb.
Piece names:  <mesh>__FR  <mesh>__FL  <mesh>__BR  <mesh>__BL   (F/B = front/back, R/L = ANATOMICAL right/left).
Frame (before root rotation): front = -Y, anatomical RIGHT = -X.  Triangles are assigned by centroid (no cutting), so edges are jagged.
Parts smaller than MIN_EXTENT in every direction stay whole (ears/eyes/fingers...). Usage: python3 tools/split_body.py"""
import json, struct, os, numpy as np
H = os.path.dirname(os.path.abspath(__file__)); SRC = os.path.join(H, "body_source.glb"); DST = os.path.join(H, "..", "app", "src", "main", "assets", "models", "body.glb")
MIN_EXTENT = 0.06
b = open(SRC, "rb").read(); jl = struct.unpack("<I", b[12:16])[0]; J = json.loads(b[20:20 + jl]); BIN = b[20 + jl + 8:]
def acc(i, dt, n):
    a = J["accessors"][i]; bv = J["bufferViews"][a["bufferView"]]
    return np.frombuffer(BIN, dt, a["count"] * n, bv.get("byteOffset", 0) + a.get("byteOffset", 0)).reshape(-1, n) if n > 1 else np.frombuffer(BIN, dt, a["count"], bv.get("byteOffset", 0) + a.get("byteOffset", 0))
out = bytearray(); bvs = []; accs = []; meshes = []; nodes = []
def add(data, target):
    while len(out) % 4: out.append(0)
    bvs.append({"buffer": 0, "byteOffset": len(out), "byteLength": len(data), "target": target}); out.extend(data); return len(bvs) - 1
def emit(name, V, T, mat):
    V = V.astype(np.float32); pa = len(accs)
    accs.append({"bufferView": add(V.tobytes(), 34962), "componentType": 5126, "count": len(V), "type": "VEC3", "min": V.min(0).tolist(), "max": V.max(0).tolist()})
    ia = len(accs); accs.append({"bufferView": add(T.astype(np.uint32).flatten().tobytes(), 34963), "componentType": 5125, "count": T.size, "type": "SCALAR"})
    meshes.append({"name": name, "primitives": [{"attributes": {"POSITION": pa}, "indices": ia, "material": mat}]}); nodes.append({"name": name, "mesh": len(meshes) - 1})
ntri = 0; split_n = 0
for n in J["nodes"][1:]:
    m = J["meshes"][n["mesh"]]; pr = m["primitives"][0]; P = acc(pr["attributes"]["POSITION"], np.float32, 3).astype(float)
    T = acc(pr["indices"], np.uint32, 1).reshape(-1, 3); mat = pr.get("material", 0); ntri += len(T)
    lo, hi = P.min(0), P.max(0); c = (lo + hi) / 2
    if (hi - lo).max() < MIN_EXTENT: emit(n["name"], P, T, mat); continue
    cen = P[T].mean(1); front = cen[:, 1] < c[1]; right = cen[:, 0] < c[0]; got = 0
    # الرأس: يُقسَّم أولًا إلى مناطق وجه (جبهة، صدغ، خد، فك، وسط الوجه، مؤخرة) ثم كل منطقة إلى أرباعها
    groups = [(n["name"], np.ones(len(T), bool))]
    if n["name"] == "head":
        h = (hi - lo) / 2; uu = (cen[:, 0] - c[0]) / h[0]; vv = (cen[:, 1] - c[1]) / h[1]; ww = (cen[:, 2] - c[2]) / h[2]
        zone = np.full(len(T), "midface", dtype=object); rt = uu < 0
        zone[(vv < -0.15) & (ww < -0.35)] = "jaw"
        zone[(vv < -0.15) & (ww >= 0.35) & (np.abs(uu) < 0.6)] = "forehead"
        zone[(vv < -0.15) & (ww >= -0.35) & (ww < 0.35) & (np.abs(uu) >= 0.3)] = np.where(rt, "cheek_r", "cheek_l")[(vv < -0.15) & (ww >= -0.35) & (ww < 0.35) & (np.abs(uu) >= 0.3)]
        side = (vv >= -0.15) & (vv < 0.35) & (ww >= -0.1) & (np.abs(uu) >= 0.55)
        zone[side] = np.where(rt, "temple_r", "temple_l")[side]
        zone[(vv >= 0.35) | ((vv >= -0.15) & ~side & (ww >= -0.1) & (vv >= 0.1))] = "occiput"
        zone[(vv >= -0.15) & (ww < -0.1) & (zone != "occiput")] = "jaw"
        groups = [(f"head@{z}", zone == z) for z in sorted(set(zone))]
        print("head zones:", {z: int((zone == z).sum()) for z in sorted(set(zone))})
    for base, gm in groups:
      for fb, fm in (("F", front), ("B", ~front)):
        for rl, rm in (("R", right), ("L", ~right)):
            sel = T[gm & fm & rm]
            if len(sel) == 0: continue
            u, inv = np.unique(sel.flatten(), return_inverse=True); emit(f"{base}__{fb}{rl}", P[u], inv.reshape(-1, 3), mat); got += len(sel)
    assert got == len(T); split_n += 1
root = dict(J["nodes"][0]); root["children"] = list(range(1, len(nodes) + 1))
G = {"asset": {"version": "2.0", "generator": "BodyScope3D tools/split_body.py"}, "scene": 0, "scenes": [{"nodes": [0]}], "nodes": [root] + nodes,
     "meshes": meshes, "materials": J["materials"], "accessors": accs, "bufferViews": bvs, "buffers": [{"byteLength": len(out)}]}
js = json.dumps(G, separators=(",", ":")).encode(); js += b" " * ((4 - len(js) % 4) % 4)
while len(out) % 4: out.append(0)
blob = b"glTF" + struct.pack("<II", 2, 12 + 8 + len(js) + 8 + len(out)) + struct.pack("<I", len(js)) + b"JSON" + js + struct.pack("<I", len(out)) + b"BIN\0" + bytes(out)
open(DST, "wb").write(blob); print("parts split:", split_n, "/", len(J["nodes"]) - 1, "| nodes now:", len(nodes), "| triangles in:", ntri, "| KB:", len(blob) // 1024)
