# BodyScope 3D — جسمك ثلاثي الأبعاد
**EN:** Educational Android app (Kotlin, Compose, MVVM, Room) to explore a 3D human body, mark pain locations, and view anatomy. It does **not** diagnose. Package: `com.bodyscope3d.app` · Version 1.0.0 (code 1).
**AR:** تطبيق تثقيفي لاستكشاف جسم ثلاثي الأبعاد وتسجيل مواضع الألم. لا يشخّص الأمراض ولا يغني عن الطبيب.

## Status / الحالة
No 3D asset is bundled yet → the app runs in "model missing" mode (list-based region picker). See `ASSETS.md`, `docs/VERIFICATION.md`.

## Build from phone (GitHub Actions) / البناء من الهاتف
1. Upload this folder to a GitHub repo (main branch). 2. Actions → CI → download artifact `bodyscope3d-debug-apk`. (Debug APK = testing only.)
3. Release: set secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`; then push tag `v1.0.0`.
## Android Studio
Open the folder → sync → Run on a real device (OpenGL ES 3.0). CLI: `gradle :app:testDebugUnitTest :app:assembleDebug`.
## Icon / الأيقونة
Put your image at `tools/app_icon_source.png`, run `python3 tools/make_icons.py`, remove the placeholder vector foreground.
## Versioning
Edit only `version.properties`, `VERSION`, `CHANGELOG.md`; tag `vX.Y.Z`. Firebase: future opt-in via the repository interfaces in `data/Db.kt`.
