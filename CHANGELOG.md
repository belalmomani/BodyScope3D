# Changelog

## [1.0.0] - 2026-10-19
### Added
- Package `com.bodyscope3d.app`, display names "BodyScope 3D" / «جسمك ثلاثي الأبعاد».
- Region catalog (27 regions, 2 levels, AR/EN), asset manifest, local Room storage (sessions, pain entries, favorites).
- Screens: home, explore, region details, internal anatomy (layer status), pain log, history, favorites, settings, privacy, help.
- 3D viewer wrapper (SceneView/Filament) with tap picking, camera presets, isolation — activates only when `models/body.glb` exists.
- Emergency guidance banner; clear-all-data with confirmation.
- CI + tag-driven release workflows.
### Known issues
- Only the stylized exterior model is bundled (not realistic); no internal anatomy layers yet (see ASSETS.md).
- Bundled stylized exterior body.glb (orientation-fixed, mapped to 22 leaf regions; back shares torso meshes).
- Schematic internal layers (skeleton, organs) + anatomy catalog (34 items, AR/EN) + real layer viewer with toggles and tap-to-identify.
- Finer exterior regions (eyes, ears, nose, mouth, elbows, fingers, thumbs, knees, ankles): 43 regions total.
- Own orbit/pinch camera; About/licenses screen; locale persistence.
- Educational text now cites a general government reference (SEER Training, NCI/NIH) with link-check date; text itself is project-written and NOT medically reviewed.
- Direction picker (front/back/right/left) for every region: moves the camera to that side and is saved with each pain entry (DB v2 with migration).
- Real half-part selection: direction (front/back/right/left) now isolates the matching half of each part (45 parts split into 4 pieces; 185 nodes, 342 KB).
- Internal anatomy: selecting a bone/organ now isolates it and focuses the camera on it ("Show all" toggles back).
- UI redesign: dark medical design system (Palette/Theme/Components), home = large 3D viewer with round controls + draggable bottom sheet, region panel (sub-regions, direction, isolate, next/back/undo), anatomy screen with on-demand layers and info card, pain form with 0–10 picker and persistent draft, records list/detail with edit and confirmed delete, settings/about/privacy/help, splash, reduced-motion support.
- Asset pipeline (tools/export_glb.py, region_map.json, NOTICE.md) added; still no bundled GLB.
- Placeholder launcher icon until `tools/app_icon_source.png` is provided.
- Not built or run in the authoring environment (no network/SDK).
