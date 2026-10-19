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
- No licensed 3D assets bundled yet (see ASSETS.md); app runs in "model missing" mode with a list-based region picker.
- Asset pipeline (tools/export_glb.py, region_map.json, NOTICE.md) added; still no bundled GLB.
- Placeholder launcher icon until `tools/app_icon_source.png` is provided.
- Not built or run in the authoring environment (no network/SDK).
