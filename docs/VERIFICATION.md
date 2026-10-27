# What was and was not verified (authoring environment: no network, no Android SDK, no Gradle)
- NOT built, NOT unit-tested, NOT run on a device.
- Dependency versions chosen from known-compatible sets (AGP 8.5.2 / Kotlin 1.9.24 / KSP 1.9.24-1.0.20 / Compose BOM 2024.06 / Room 2.6.1 / SceneView 2.2.1) but unconfirmed here. First CI run is the real check.
- SceneView API usage in `viewer/Viewer3D.kt` (rememberNodes, ModelNode.renderableNodes, node.name picking, cameraNode.lookAt) is from memory and the most likely place to need fixes.
- Picking returns mesh/node names; whether a tap maps to the right region depends on the future GLB node names.
- Internal anatomy: layer status only; rendering layers is not implemented until GLBs exist.
- Gradle wrapper not generated; CI uses `gradle-version: 8.7`. Run `gradle wrapper --gradle-version 8.7` locally if wanted.

- body.glb was inspected offline (valid GLB header, 50 named nodes, bbox) and re-oriented; all meshNames are checked against it by `ModelAssetTest`. It has NOT been rendered anywhere here: orientation, scale, left/right and tap→region behaviour need a device check.
- Camera presets were computed from mesh bounding boxes assuming scaleToUnits=1 on the largest dimension and no recentring; SceneView may differ.
- Tap disambiguation (chest vs back) uses the last preset view button, not manual orbit angle.

- Internal layers are generated schematics aligned to body.glb by shared root transform + anchors; alignment was checked offline by plotting vertices only. SceneView must apply the same scaleToUnits to each file (assumes bounding box includes root transform) — verify on device that skeleton/organs sit inside the body.
- Camera is custom (cameraManipulator=null) using onScroll/onScale gesture callbacks whose exact signatures are from memory: likely first compile error.

- Direction (aspect): camera view + stored value + isolation of the matching half via split pieces (tools/split_body.py). Halves are cut by triangle centroid on a low-poly mesh (jagged). Parts <6 cm (eyes, ears, fingers) stay whole. Not verified in SceneView: relies on renderable node names equal to glTF node names.
- DB migration 1→2 (ALTER TABLE add aspect) is untested; installing over an older debug build exercises it.

## UI redesign (not compiled/run here)
- All new Compose code (ui/*.kt) is unverified until CI compiles it. Likeliest errors: icon names (material-icons-extended), BottomSheetScaffold params, FilterChip colors.
- Region highlight in 3D = translucent coral inflated copies ("hl::<piece>") toggled via renderable node visibility (same unverified mechanism as isolation). Alpha-blended overlay: sorting artefacts possible on some GPUs.
- Tooltips on icon buttons: content descriptions only (screen readers); no long-press tooltips.
- Font: system Arabic font (no font file bundled; no network to fetch one).
- Head zones (forehead, temples, cheeks, mid-face, jaw, occiput) are cut from the single head mesh by triangle centroid (coarse, jagged); checked only by an offline plot. Boundaries are approximate anatomy.
- Muscles layer is a schematic of ~16 superficial groups (offline plot only, shapes approximate); blood vessels do not exist and are not shown.
- Layers load on first show (synchronously on the main thread; files are <300 KB).
- Instrumented tests (androidTest) assume an English device and were not run.

## Possibilities + image report (not compiled/run here)
- `causes.json` is general-knowledge content written by the project, NOT medically reviewed; a clinician must review it before public release. Apps that suggest possible conditions can fall under Google Play health/medical-symptom-checker policies: check before publishing.
- The report image is drawn with Canvas/StaticLayout (Arabic shaping depends on the device font); the 2D body figures are rendered offline from the model (tools/make_body_maps.py) and marker positions use the regions' camera targets, so markers are approximate.
- WhatsApp: tries com.whatsapp then com.whatsapp.w4b, else the system share sheet. FileProvider/cache-path config untested. Gallery save needs Android 10+.
- The report image deliberately contains only the user's own entries (no possibilities).
