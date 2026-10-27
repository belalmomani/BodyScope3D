package com.bodyscope3d.app.data

import android.content.Context

/** تهيئة يدوية بسيطة (بدون DI) */
class AppGraph(context: Context) {
    private val app = context.applicationContext
    val repo by lazy { LocalRepository(AppDb.create(app).dao()) }
    val catalog: RegionCatalog by lazy {
        RegionCatalog.parse(app.assets.open("regions.json").bufferedReader().use { it.readText() })
    }
    val models: List<ModelAsset> by lazy {
        AssetManifest.parse(app.assets.open("asset_manifest.json").bufferedReader().use { it.readText() })
    }
    val anatomy: List<AnatomyItem> by lazy {
        AnatomyCatalog.parse(app.assets.open("anatomy.json").bufferedReader().use { it.readText() })
    }
    val causes: CauseCatalog by lazy {
        CauseCatalog.parse(app.assets.open("causes.json").bufferedReader().use { it.readText() })
    }
    private val existing: Set<String> by lazy {
        (app.assets.list("models") ?: emptyArray()).map { "models/$it" }.toSet()
    }
    fun isReady(m: ModelAsset) = AssetManifest.isReady(m, existing)
    fun model(layer: Layer) = models.firstOrNull { it.layer == layer }
}
