"""Run inside Blender (headless) to export one anatomy layer as an optimized GLB with per-region mesh nodes.

blender -b Z-Anatomy.blend -P tools/export_glb.py -- --collection-keyword skeleton \
        --out app/src/main/assets/models/skeleton.glb --ratio 0.25

Each region becomes ONE mesh named  mesh_<regionId>  (matches regions.json meshNames).
Objects that match no region are skipped and listed in the report (never silently invented).
"""
import bpy, sys, json, re, os, argparse
argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
ap = argparse.ArgumentParser()
ap.add_argument("--collection-keyword", required=True)
ap.add_argument("--out", required=True)
ap.add_argument("--map", default=os.path.join(os.path.dirname(__file__), "region_map.json"))
ap.add_argument("--ratio", type=float, default=0.25)
a = ap.parse_args(argv)
rmap = json.load(open(a.map))["regions"]

def side_of(name):
    n = name.lower()
    if re.search(r"(\.r\b|_r\b|\bright\b)", n): return "R"
    if re.search(r"(\.l\b|_l\b|\bleft\b)", n): return "L"
    return None

def region_for(name):
    n = name.lower(); s = side_of(name)
    for rid, spec in rmap.items():
        if any(k in n for k in spec["keywords"]) and (spec["side"] is None or spec["side"] == s):
            return rid
    return None

def in_layer(o):
    return any(a.collection_keyword.lower() in c.name.lower() for c in o.users_collection)

groups, skipped = {}, []
for o in bpy.data.objects:
    if o.type != "MESH" or not in_layer(o): continue
    rid = region_for(o.name)
    (groups.setdefault(rid, []) if rid else skipped).append(o) if rid else skipped.append(o.name)

bpy.ops.object.select_all(action="DESELECT")
export = []
for rid, objs in groups.items():
    bpy.ops.object.select_all(action="DESELECT")
    for o in objs: o.select_set(True)
    bpy.context.view_layer.objects.active = objs[0]
    bpy.ops.object.duplicate(); bpy.ops.object.join()
    j = bpy.context.active_object; j.name = "mesh_" + rid
    mod = j.modifiers.new("dec", "DECIMATE"); mod.ratio = a.ratio
    bpy.ops.object.modifier_apply(modifier=mod.name)
    export.append(j)

bpy.ops.object.select_all(action="DESELECT")
for j in export: j.select_set(True)
os.makedirs(os.path.dirname(a.out), exist_ok=True)
bpy.ops.export_scene.gltf(filepath=a.out, export_format="GLB", use_selection=True, export_apply=True)
print("EXPORTED regions:", sorted(groups)); print("SKIPPED (no region):", len(skipped))
missing = [r for r in rmap if r not in groups]; print("REGIONS WITHOUT MESH:", missing)
print("size MB:", round(os.path.getsize(a.out) / 1e6, 2))
