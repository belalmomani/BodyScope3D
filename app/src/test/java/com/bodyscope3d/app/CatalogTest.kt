package com.bodyscope3d.app

import com.bodyscope3d.app.data.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CatalogTest {
    private fun asset(n: String) = File("src/main/assets/$n").readText()
    private val cat = RegionCatalog.parse(asset("regions.json"))

    @Test fun regionsAreValid() = assertEquals(emptyList<String>(), cat.validate())
    @Test fun hasRequiredRegions() {
        listOf("head","neck","chest","abdomen","back","pelvis","shoulder_r","shoulder_l","arm_r","arm_l","hand_r","hand_l","thigh_r","thigh_l","foot_r","foot_l")
            .forEach { assertNotNull(it, cat.byId(it)) }
    }
    @Test fun hierarchy() { assertTrue(cat.children("head").any { it.id == "face" }); assertEquals("head", cat.byId("face")!!.parentId) }
    @Test fun sidesAreConsistent() = cat.regions.filter { it.id.endsWith("_r") }.forEach { assertEquals("R", it.side) }
    @Test fun noPhantomAssets() {
        val existing = File("src/main/assets/models").list()?.map { "models/$it" }?.toSet().orEmpty()
        AssetManifest.parse(asset("asset_manifest.json")).forEach {
            assertEquals(it.path in existing, AssetManifest.isReady(it, existing))
            if (it.status == "READY") assertTrue("READY without file: ${it.path}", it.path in existing)
        }
    }
    @Test fun manifestWithoutInternalModelDoesNotCrash() {
        val m = AssetManifest.parse(asset("asset_manifest.json")).filter { it.layer != Layer.EXTERIOR }
        assertTrue(m.none { AssetManifest.isReady(it, emptySet()) })
    }
    @Test fun painValidation() {
        assertTrue(PainValidator.valid("head", 0, "sharp")); assertTrue(PainValidator.valid("head", 10, "dull"))
        assertFalse(PainValidator.valid("head", 11, "dull")); assertFalse(PainValidator.valid("head", -1, "dull"))
        assertFalse(PainValidator.valid(null, 5, "dull")); assertFalse(PainValidator.valid("head", 5, "bogus"))
    }
}
