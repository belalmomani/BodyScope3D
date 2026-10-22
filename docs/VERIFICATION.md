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
