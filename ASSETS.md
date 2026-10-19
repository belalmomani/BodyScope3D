# Required 3D assets / الأصول المطلوبة
No 3D model is bundled. Place licensed GLB files under `app/src/main/assets/models/` and record each below **before** shipping.

| File | Needed content | Naming requirement | Source | License | Redistribution OK? |
|---|---|---|---|---|---|
| body.glb | Adult exterior, neutral pose | One mesh node per region; node names listed in `regions.json → meshNames` | — | — | — |
| skeleton.glb / muscles.glb / organs.glb | Internal layers | optional | — | — | — |

Do not assume a free file may be redistributed commercially. Check the license text (CC0 / CC-BY need attribution handling; NC/ND licenses do not fit a public app).
Keep each GLB small (mobile): target < 25 MB, textures ≤ 2K, Draco/meshopt compression if supported.
