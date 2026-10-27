package com.bodyscope3d.app

import com.bodyscope3d.app.data.*
import com.bodyscope3d.app.ui.BodyMap
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CausesTest {
    private val regions = RegionCatalog.parse(File("src/main/assets/regions.json").readText())
    private val causes = CauseCatalog.parse(File("src/main/assets/causes.json").readText())

    @Test fun everyAreaHasBilingualCausesAndRedFlags() = causes.areas.forEach { (k, a) ->
        assertTrue(k, a.causes.size >= 3 && a.redFlags.isNotEmpty())
        (a.causes + a.redFlags).forEach { assertTrue(k, it.ar.isNotBlank() && it.en.isNotBlank()) }
    }
    @Test fun regionsInheritFromParents() {
        mapOf("cheek_r" to "cheek", "upper_arm_l" to "arm", "nose" to "midface", "foot_r" to "foot", "chest" to "chest",
            "arm_r" to "arm", "leg_l" to "thigh", "eye_r" to "eye", "occiput" to "occiput", "fingers_r" to "fingers", "torso" to CauseCatalog.FALLBACK)
            .forEach { (id, key) -> assertEquals(id, key, causes.keyFor(id, regions)) }
    }
    @Test fun everyRegionResolvesToUsableContent() = regions.regions.forEach {
        val a = causes.area(causes.keyFor(it.id, regions)); assertTrue(it.id, a.causes.isNotEmpty() && a.redFlags.isNotEmpty())
    }
    @Test fun bodyMapPlacesMarkersSensibly() {
        val head = BodyMap.point(regions.byId("head")!!, false); val foot = BodyMap.point(regions.byId("foot_r")!!, false)
        assertTrue(head.second < foot.second)                                   // الرأس أعلى من القدم
        val armR = BodyMap.point(regions.byId("arm_r")!!, false); val armRBack = BodyMap.point(regions.byId("arm_r")!!, true)
        assertTrue(armR.first < 0.5f); assertEquals(1f, armR.first + armRBack.first, 0.001f)   // يمين الشخص على يسار المشاهد من الأمام، ويُعكس من الخلف
        regions.regions.forEach { r -> BodyMap.point(r, false).let { assertTrue(it.first in 0f..1f && it.second in 0f..1f) } }
    }
}
