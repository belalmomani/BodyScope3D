#!/usr/bin/env python3
"""Generate SCHEMATIC internal-anatomy GLB layers (skeleton.glb, organs.glb) fitted to body.glb.
Original geometry (ellipsoids/tubes) — no third-party mesh. NOT medically accurate: positions/sizes are approximate teaching schematics.
Usage: python3 tools/gen_layers.py   (needs numpy)
Frame: same as the original body.glb file (Z-up, front = -Y, model '_L' nodes = anatomical RIGHT, at -X); the same root transform
(rotate -90deg about X + translate) is applied so all layers align after loading. Each layer also holds two invisible 'anchor_*' triangles
at the body's top/bottom so SceneView's scaleToUnits yields the SAME scale as body.glb."""
import json, struct, os, numpy as np
HERE = os.path.dirname(os.path.abspath(__file__)); MD = os.path.join(HERE, "..", "app", "src", "main", "assets", "models")

def read_boxes(path):
    b = open(path, "rb").read(); jl = struct.unpack("<I", b[12:16])[0]; j = json.loads(b[20:20 + jl]); out = {}
    for n in j["nodes"]:
        if "mesh" in n:
            a = j["accessors"][j["meshes"][n["mesh"]]["primitives"][0]["attributes"]["POSITION"]]
            out[n["name"]] = (np.array(a["min"]), np.array(a["max"]))
    return out, j["nodes"][0]
BOX, ROOT = read_boxes(os.path.join(MD, "body.glb"))
ctr = lambda n: (BOX[n][0] + BOX[n][1]) / 2
half = lambda n: (BOX[n][1] - BOX[n][0]) / 2
SUF = {"r": "L", "l": "R"}                      # anatomical right = model _L
def P(x, y, z): return np.array([x, y, z], float)

# ---------- geometry primitives ----------
def ellipsoid(c, r, nu=20, nv=14):
    v = []; 
    for i in range(nv + 1):
        t = np.pi * i / nv
        for k in range(nu):
            p = 2 * np.pi * k / nu
            v.append([c[0] + r[0] * np.sin(t) * np.cos(p), c[1] + r[1] * np.sin(t) * np.sin(p), c[2] + r[2] * np.cos(t)])
    f = []
    for i in range(nv):
        for k in range(nu):
            a = i * nu + k; b = i * nu + (k + 1) % nu; c2 = a + nu; d = b + nu
            f += [[a, c2, b], [b, c2, d]]
    return np.array(v), np.array(f)

def tube(pts, r, n=10):
    pts = [np.array(p, float) for p in pts]; rs = r if isinstance(r, (list, tuple)) else [r] * len(pts)
    V = []; F = []
    for i, p in enumerate(pts):
        d = (pts[min(i + 1, len(pts) - 1)] - pts[max(i - 1, 0)]); d /= (np.linalg.norm(d) + 1e-9)
        a = np.array([1, 0, 0]) if abs(d[0]) < 0.9 else np.array([0, 1, 0])
        u = np.cross(d, a); u /= np.linalg.norm(u); w = np.cross(d, u)
        for k in range(n):
            th = 2 * np.pi * k / n; V.append(p + rs[i] * (np.cos(th) * u + np.sin(th) * w))
    for i in range(len(pts) - 1):
        for k in range(n):
            a = i * n + k; b = i * n + (k + 1) % n; c = a + n; d = b + n; F += [[a, c, b], [b, c, d]]
    for end, idx in ((0, 0), (1, len(pts) - 1)):                      # caps
        cidx = len(V); V.append(pts[idx]); base = idx * n
        for k in range(n):
            a = base + k; b = base + (k + 1) % n; F.append([cidx, a, b] if end == 0 else [cidx, b, a])
    return np.array(V), np.array(F)

def merge(parts):
    V = []; F = []; o = 0
    for v, f in parts: V.append(v); F.append(f + o); o += len(v)
    return np.vstack(V), np.vstack(F)

def normals(v, f):
    n = np.zeros_like(v); fn = np.cross(v[f[:, 1]] - v[f[:, 0]], v[f[:, 2]] - v[f[:, 0]])
    for i in range(3): np.add.at(n, f[:, i], fn)
    return n / (np.linalg.norm(n, axis=1, keepdims=True) + 1e-12)

def write_glb(path, meshes):
    """meshes: list of (name, (V,F), (r,g,b))"""
    bin_ = bytearray(); bvs = []; acc = []; mats = []; ms = []; nodes = []
    def add(data, target):
        while len(bin_) % 4: bin_.append(0)
        bvs.append({"buffer": 0, "byteOffset": len(bin_), "byteLength": len(data), "target": target}); bin_.extend(data); return len(bvs) - 1
    for name, (V, F), col in meshes:
        V = V.astype(np.float32); N = normals(V, F).astype(np.float32); F = F.astype(np.uint32)
        pa = len(acc); acc.append({"bufferView": add(V.tobytes(), 34962), "componentType": 5126, "count": len(V), "type": "VEC3", "min": V.min(0).tolist(), "max": V.max(0).tolist()})
        na = len(acc); acc.append({"bufferView": add(N.tobytes(), 34962), "componentType": 5126, "count": len(V), "type": "VEC3"})
        ia = len(acc); acc.append({"bufferView": add(F.flatten().tobytes(), 34963), "componentType": 5125, "count": F.size, "type": "SCALAR"})
        m = {"pbrMetallicRoughness": {"baseColorFactor": [*col, 1.0], "metallicFactor": 0.0, "roughnessFactor": 0.7}, "name": name}
        if name.startswith("anchor"): m["pbrMetallicRoughness"]["baseColorFactor"] = [0, 0, 0, 0]; m["alphaMode"] = "BLEND"
        mats.append(m)
        ms.append({"name": name, "primitives": [{"attributes": {"POSITION": pa, "NORMAL": na}, "indices": ia, "material": len(mats) - 1}]})
        nodes.append({"name": name, "mesh": len(ms) - 1})
    root = {"name": "world", "children": list(range(1, len(nodes) + 1)), "rotation": ROOT["rotation"], "translation": ROOT["translation"]}
    j = {"asset": {"version": "2.0", "generator": "BodyScope3D tools/gen_layers.py"}, "scene": 0, "scenes": [{"nodes": [0]}],
         "nodes": [root] + nodes, "meshes": ms, "materials": mats, "accessors": acc, "bufferViews": bvs, "buffers": [{"byteLength": len(bin_)}]}
    js = json.dumps(j, separators=(",", ":")).encode(); js += b" " * ((4 - len(js) % 4) % 4)
    while len(bin_) % 4: bin_.append(0)
    out = b"glTF" + struct.pack("<II", 2, 12 + 8 + len(js) + 8 + len(bin_)) + struct.pack("<I", len(js)) + b"JSON" + js + struct.pack("<I", len(bin_)) + b"BIN\0" + bytes(bin_)
    open(path, "wb").write(out); print(os.path.basename(path), round(len(out) / 1024), "KB,", len(meshes), "meshes")

def anchors():
    tri = lambda z: (np.array([[0, 0, z], [0.001, 0, z], [0, 0.001, z]]), np.array([[0, 1, 2]]))
    return [("anchor_bottom", tri(0.01), (0, 0, 0)), ("anchor_top", tri(1.865), (0, 0, 0))]

YC = float(ctr("shirt_torso")[1])               # torso mid-depth (front = -Y)
YF, YB = YC - 0.11, YC + 0.08                   # front / back of trunk
BONE = (0.93, 0.90, 0.80)

def skeleton():
    m = []
    hc = ctr("head"); hh = half("head")
    m.append(("skull", ellipsoid(hc + P(0, 0.005, 0.015), hh * [0.78, 0.8, 0.8]), BONE))
    m.append(("mandible", ellipsoid(hc + P(0, -0.04, -0.075), [0.05, 0.04, 0.025]), BONE))
    zs = np.linspace(1.50, 0.97, 24)
    m.append(("vertebral_column", merge([ellipsoid(P(0, YB - 0.02 - 0.012 * np.sin((z - 0.97) / 0.53 * np.pi), z), [0.022, 0.02, 0.009], 10, 6) for z in zs]), BONE))
    ribs = []
    for i in range(10):
        z = 1.52 - i * 0.026; rx = 0.13 + 0.07 * np.sin(min(i, 7) / 7 * 1.3); ry = (YB - YF) / 2 - 0.02
        for s in (-1, 1):
            pts = [P(s * rx * np.sin(t), YC + 0.03 + ry * np.cos(t) * -1 if False else (YB - 0.03) - (YB - YF - 0.05) * (1 - np.cos(t)) / 2, z - 0.012 * t) for t in np.linspace(0, 2.7, 14)]
            ribs.append(tube(pts, 0.005, 6))
    m.append(("ribcage", merge(ribs), BONE))
    m.append(("sternum", tube([P(0, YF + 0.005, 1.53), P(0, YF + 0.005, 1.36)], [0.022, 0.014], 8), BONE))
    for sd in "rl":
        s = -1 if sd == "r" else 1; sh = ctr("shoulder_" + SUF[sd]); el = ctr("elbow_" + SUF[sd]); hd = ctr("hand_" + SUF[sd])
        wr = P(hd[0], hd[1], BOX["hand_" + SUF[sd]][1][2])
        m.append(("clavicle_" + sd, tube([P(s * 0.02, YF + 0.01, 1.52), P(s * 0.13, YF + 0.005, 1.53), P(s * 0.25, YC - 0.01, 1.50)], 0.008, 6), BONE))
        m.append(("scapula_" + sd, ellipsoid(P(s * 0.15, YB - 0.02, 1.42), [0.06, 0.012, 0.075], 12, 8), BONE))
        m.append(("humerus_" + sd, tube([sh + P(0, 0, 0.02), el], 0.014, 8), BONE))
        off = P(0.012, 0.0, 0)
        m.append(("radius_ulna_" + sd, merge([tube([el, wr + off], 0.009, 6), tube([el, wr - off], 0.01, 6)]), BONE))
        hb = [ellipsoid(hd + P(0, 0, 0.03), half("hand_" + SUF[sd]) * [0.6, 0.5, 0.35], 10, 6)]
        for fn in [f"finger_{SUF[sd]}_{i}" for i in range(4)] + ["thumb_" + SUF[sd]]:
            lo, hi = BOX[fn]; cx = (lo[0] + hi[0]) / 2; cy = (lo[1] + hi[1]) / 2
            hb.append(tube([P(cx, cy, hi[2] - 0.005), P(cx, cy, lo[2] + 0.01)], 0.005, 5))
        m.append(("hand_bones_" + sd, merge(hb), BONE))
        hip = P(s * 0.09, YC, 0.93); kn = ctr("knee_" + SUF[sd]); an = ctr("ankle_" + SUF[sd]); ft = ctr("foot_" + SUF[sd])
        m.append(("femur_" + sd, tube([hip, kn], [0.022, 0.017], 8), BONE))
        m.append(("tibia_fibula_" + sd, merge([tube([kn, an], 0.015, 8), tube([kn + P(s * 0.03, 0.005, 0), an + P(s * 0.03, 0.005, 0)], 0.008, 6)]), BONE))
        m.append(("patella_" + sd, ellipsoid(kn + P(0, -0.045, 0.01), [0.02, 0.01, 0.025], 8, 6), BONE))
        m.append(("foot_bones_" + sd, ellipsoid(ft + P(0, -0.03, -0.01), half("foot_" + SUF[sd]) * [0.5, 0.55, 0.4], 10, 6), BONE))
    il = [ellipsoid(P(s * 0.10, YC + 0.02, 1.02), [0.085, 0.04, 0.07], 12, 8) for s in (-1, 1)]
    m.append(("pelvis_bone", merge(il + [ellipsoid(P(0, YB - 0.01, 0.97), [0.05, 0.03, 0.07], 10, 8)]), BONE))
    return m + anchors()

def organs():
    m = []; hc = ctr("head"); hh = half("head")
    def add(n, g, c): m.append((n, g, c))
    add("brain", ellipsoid(hc + P(0, 0.005, 0.03), hh * [0.68, 0.7, 0.55]), (0.93, 0.70, 0.72))
    add("trachea", tube([P(0, YC - 0.03, 1.52), P(0, YC - 0.02, 1.40)], 0.010, 8), (0.75, 0.85, 0.95))
    add("lung_r", ellipsoid(P(-0.10, YC - 0.01, 1.42), [0.075, 0.085, 0.12]), (0.90, 0.60, 0.62))
    add("lung_l", ellipsoid(P(0.10, YC - 0.01, 1.42), [0.065, 0.08, 0.115]), (0.90, 0.60, 0.62))
    add("heart", ellipsoid(P(0.035, YC - 0.05, 1.37), [0.05, 0.045, 0.06]), (0.75, 0.12, 0.15))
    add("aorta", tube([P(0.03, YC - 0.03, 1.43), P(0.045, YC + 0.01, 1.41), P(0.03, YC + 0.045, 1.25), P(0.015, YC + 0.05, 1.02)], 0.011, 8), (0.85, 0.2, 0.2))
    add("esophagus", tube([P(0, YC + 0.03, 1.52), P(0, YC + 0.035, 1.30), P(0.02, YC + 0.02, 1.24)], 0.008, 8), (0.85, 0.55, 0.50))
    add("liver", ellipsoid(P(-0.075, YC - 0.04, 1.22), [0.11, 0.07, 0.055]), (0.55, 0.25, 0.18))
    add("gallbladder", ellipsoid(P(-0.06, YC - 0.09, 1.16), [0.014, 0.014, 0.025], 10, 6), (0.35, 0.55, 0.25))
    add("stomach", ellipsoid(P(0.075, YC - 0.04, 1.19), [0.06, 0.05, 0.07]), (0.90, 0.60, 0.50))
    add("spleen", ellipsoid(P(0.14, YC + 0.04, 1.22), [0.03, 0.035, 0.05], 12, 8), (0.55, 0.20, 0.30))
    add("pancreas", tube([P(-0.03, YC + 0.01, 1.15), P(0.04, YC + 0.02, 1.17), P(0.11, YC + 0.035, 1.19)], 0.014, 8), (0.95, 0.80, 0.55))
    add("kidney_r", ellipsoid(P(-0.07, YC + 0.05, 1.10), [0.03, 0.025, 0.05], 12, 8), (0.60, 0.20, 0.20))
    add("kidney_l", ellipsoid(P(0.07, YC + 0.05, 1.13), [0.03, 0.025, 0.05], 12, 8), (0.60, 0.20, 0.20))
    zz = np.linspace(0.93, 1.10, 18); si = [P(0.09 * np.sin(i * 1.7) * (1 - 0.1 * (i % 3)), YC - 0.05 + 0.02 * np.cos(i), z) for i, z in enumerate(zz)]
    add("small_intestine", tube(si, 0.013, 8), (0.95, 0.75, 0.65))
    add("large_intestine", tube([P(-0.12, YC - 0.02, 0.93), P(-0.135, YC - 0.02, 1.05), P(-0.13, YC - 0.03, 1.12), P(-0.06, YC - 0.07, 1.14), P(0.06, YC - 0.07, 1.14),
                                 P(0.13, YC - 0.03, 1.12), P(0.135, YC - 0.02, 1.02), P(0.12, YC - 0.01, 0.92), P(0.05, YC - 0.02, 0.88)], 0.018, 10), (0.80, 0.55, 0.45))
    add("bladder", ellipsoid(P(0, YC - 0.05, 0.84), [0.035, 0.03, 0.035], 12, 8), (0.95, 0.85, 0.45))
    return m + anchors()

if __name__ == "__main__":
    write_glb(os.path.join(MD, "skeleton.glb"), skeleton()); write_glb(os.path.join(MD, "organs.glb"), organs())
