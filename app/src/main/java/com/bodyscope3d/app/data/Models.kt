package com.bodyscope3d.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class CameraPreset(val yawDeg: Float = 0f, val targetX: Float = 0f, val targetY: Float = 0f, val distance: Float = 2f)

@Serializable data class Region(
    val id: String, val nameAr: String, val nameEn: String,
    val descAr: String, val descEn: String,
    val parentId: String? = null, val level: Int = 1, val side: String? = null,
    val meshNames: List<String> = emptyList(),
    val camera: CameraPreset = CameraPreset(), val contentRef: String? = null,
) {
    fun name(ar: Boolean) = if (ar) nameAr else nameEn
    fun desc(ar: Boolean) = if (ar) descAr else descEn
}

@Serializable private data class RegionFile(val version: Int, val regions: List<Region>)

class RegionCatalog(val regions: List<Region>) {
    private val map = regions.associateBy { it.id }
    val roots get() = regions.filter { it.parentId == null }
    fun byId(id: String?) = id?.let { map[it] }
    fun children(id: String) = regions.filter { it.parentId == id }
    /** يعيد قائمة أخطاء؛ فارغة = سليم */
    fun validate(): List<String> = buildList {
        if (map.size != regions.size) add("duplicate ids")
        regions.forEach { r ->
            if (r.parentId != null && map[r.parentId] == null) add("missing parent ${r.parentId} for ${r.id}")
            if (r.nameAr.isBlank() || r.nameEn.isBlank()) add("blank name ${r.id}")
        }
    }
    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun parse(text: String) = RegionCatalog(json.decodeFromString<RegionFile>(text).regions)
    }
}

@Serializable data class AnatomyItem(
    val id: String, val mesh: String, val layer: Layer, val system: String,
    val nameAr: String, val nameEn: String, val descAr: String, val descEn: String,
) { fun name(ar: Boolean) = if (ar) nameAr else nameEn; fun desc(ar: Boolean) = if (ar) descAr else descEn }
@Serializable private data class AnatomyFile(val version: Int, val items: List<AnatomyItem>)
object AnatomyCatalog { private val json = Json { ignoreUnknownKeys = true }
    fun parse(text: String): List<AnatomyItem> = json.decodeFromString<AnatomyFile>(text).items }

enum class Layer { EXTERIOR, SKELETAL, MUSCULAR, ORGANS }

@Serializable data class ModelAsset(
    val id: String, val path: String, val layer: Layer, val status: String,
    val contains: List<String> = emptyList(), val license: String? = null,
    val source: String? = null, val redistributable: Boolean? = null,
)
@Serializable private data class ManifestFile(val version: Int, val models: List<ModelAsset>)

object AssetManifest {
    private val json = Json { ignoreUnknownKeys = true }
    fun parse(text: String): List<ModelAsset> = json.decodeFromString<ManifestFile>(text).models
    /** جاهز فعليًا فقط إذا كان الملف موجودًا ماديًا، بغض النظر عن حالة الـmanifest */
    fun isReady(m: ModelAsset, existingFiles: Set<String>) = m.path in existingFiles
}

/** جهة العرض/الألم بالنسبة للمريض؛ الزاوية تفترض أن الشخص يواجه الكاميرا (يمينه عند -X) */
enum class Aspect(val key: String, val yawDeg: Float) {
    FRONT("front", 0f), BACK("back", 180f), RIGHT("right", -90f), LEFT("left", 90f);
    companion object { fun fromKey(k: String?) = entries.firstOrNull { it.key == k } }
}

/** أسماء القطع الناتجة عن tools/split_body.py:  <part>__FR|FL|BR|BL  (أمام/خلف + يمين/يسار تشريحي) */
object Pieces {
    fun base(node: String) = node.substringBefore("__")
    fun matches(node: String, aspect: Aspect?): Boolean {
        if (aspect == null || "__" !in node) return true
        val s = node.substringAfter("__")
        if (s.length < 2) return true
        return when (aspect) {
            Aspect.FRONT -> s[0] == 'F'; Aspect.BACK -> s[0] == 'B'
            Aspect.RIGHT -> s[1] == 'R'; Aspect.LEFT -> s[1] == 'L'
        }
    }
}

object PainValidator {
    val natures = listOf("sharp", "dull", "burning", "throbbing", "cramping", "other")
    fun valid(regionId: String?, intensity: Int, nature: String?) =
        !regionId.isNullOrBlank() && intensity in 0..10 && nature in natures
}
