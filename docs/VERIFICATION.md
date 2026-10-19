# What was and was not verified (authoring environment: no network, no Android SDK, no Gradle)
- NOT built, NOT unit-tested, NOT run on a device.
- Dependency versions chosen from known-compatible sets (AGP 8.5.2 / Kotlin 1.9.24 / KSP 1.9.24-1.0.20 / Compose BOM 2024.06 / Room 2.6.1 / SceneView 2.2.1) but unconfirmed here. First CI run is the real check.
- SceneView API usage in `viewer/Viewer3D.kt` (rememberNodes, ModelNode.renderableNodes, node.name picking, cameraNode.lookAt) is from memory and the most likely place to need fixes.
- Picking returns mesh/node names; whether a tap maps to the right region depends on the future GLB node names.
- Internal anatomy: layer status only; rendering layers is not implemented until GLBs exist.
- Gradle wrapper not generated; CI uses `gradle-version: 8.7`. Run `gradle wrapper --gradle-version 8.7` locally if wanted.
