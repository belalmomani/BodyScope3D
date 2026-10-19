# Required 3D assets / الأصول المطلوبة
No 3D model is bundled. Place licensed GLB files under `app/src/main/assets/models/` and record each below **before** shipping.

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
