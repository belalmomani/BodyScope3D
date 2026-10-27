package com.bodyscope3d.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class Bi(val ar: String, val en: String) { fun t(isAr: Boolean) = if (isAr) ar else en }
@Serializable data class CauseArea(val causes: List<Bi>, val redFlags: List<Bi>)
@Serializable private data class CauseFile(val version: Int, val aliases: Map<String, String> = emptyMap(), val areas: Map<String, CauseArea>, val fallback: CauseArea)

/**
 * احتمالات شائعة وعلامات خطر لكل منطقة: قوائم عامة وغير شاملة للتثقيف. بلا ترتيب بالأرجحية وبلا حساب احتمالات.
 * المنطقة الفرعية ترث من أبيها إن لم يكن لها محتوى خاص.
 */
class CauseCatalog private constructor(private val aliases: Map<String, String>, val areas: Map<String, CauseArea>, private val fallback: CauseArea) {
    fun keyFor(regionId: String, regions: RegionCatalog): String {
        var r = regions.byId(regionId)
        while (r != null) {
            val base = r.id.removeSuffix("_r").removeSuffix("_l")
            val key = aliases[base] ?: base
            if (key in areas) return key
            r = regions.byId(r.parentId)
        }
        return FALLBACK
    }
    fun area(key: String) = areas[key] ?: fallback
    companion object {
        const val FALLBACK = "generic"
        private val json = Json { ignoreUnknownKeys = true }
        fun parse(text: String): CauseCatalog = json.decodeFromString<CauseFile>(text).let { CauseCatalog(it.aliases, it.areas, it.fallback) }
    }
}
