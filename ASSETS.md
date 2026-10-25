# Required 3D assets / الأصول المطلوبة
**Bundled now:** `models/body.glb` — stylized procedural exterior (122 KB, 50 named mesh nodes, clothed), generated for this project, no third-party mesh, redistributable. NOT photorealistic, NOT anatomically accurate. Internal layers: `skeleton.glb` (26 parts) and `organs.glb` (17 organs) are SCHEMATIC generated shapes (`tools/gen_layers.py`: ellipsoids/tubes fitted to the body) — approximate, NOT anatomically accurate, no third-party mesh. Each contains 2 invisible `anchor_*` triangles so every layer scales identically to body.glb. A muscles layer does not exist.
Split: `tools/split_body.py` cuts each part ≥6 cm into 4 pieces (`<part>__FR|FL|BR|BL`; F/B front/back, R/L anatomical right/left) by triangle centroid so the app can show half of a part (jagged edges; source kept in `tools/body_source.glb`).
Processing applied: root node rotated Z-up→Y-up and centred (glTF convention); geometry untouched.
**Left/right:** the file's `_L` nodes sit on the viewer's left of a person facing the viewer, i.e. the person's anatomical RIGHT. `regions.json` therefore maps `*_L`→`*_r` and `*_R`→`*_l`. Verify visually on a device.

Original note: Place licensed GLB files under `app/src/main/assets/models/` and record each below **before** shipping.

| File | Needed content | Naming requirement | Source | License | Redistribution OK? |
|---|---|---|---|---|---|
| body.glb | Adult exterior, neutral pose | One mesh node per region; node names listed in `regions.json → meshNames` | — | — | — |
| skeleton.glb / muscles.glb / organs.glb | Internal layers | optional | — | — | — |

Do not assume a free file may be redistributed commercially. Check the license text (CC0 / CC-BY need attribution handling; NC/ND licenses do not fit a public app).
Keep each GLB small (mobile): target < 25 MB, textures ≤ 2K, Draco/meshopt compression if supported.

## Recommended source (not yet bundled): Z-Anatomy
- Repo: github.com/Z-Anatomy/Models-of-human-anatomy · License CC BY-SA 4.0 (BodyParts3D parts: CC BY-SA 2.1 JP). See NOTICE.md.
- Pipeline: open the .blend in Blender → `blender -b <file>.blend -P tools/export_glb.py -- --collection-keyword <layer> --out app/src/main/assets/models/<layer>.glb`
- `tools/region_map.json` holds STARTER keyword guesses (object names not inspected). Tune, run, then read the script's "REGIONS WITHOUT MESH" report.
- After export: set status READY + license/source/redistributable in `asset_manifest.json`. No exterior skin model exists in this source; a separate licensed exterior body is still needed.
