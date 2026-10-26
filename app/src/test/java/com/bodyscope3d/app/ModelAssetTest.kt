package com.bodyscope3d.app

import com.bodyscope3d.app.data.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ModelAssetTest {
    private val glb = File("src/main/assets/models/body.glb")
    private fun glbNodeNames(): Set<String> {
        val bb = ByteBuffer.wrap(glb.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(0x46546C67, bb.getInt(0))               // 'glTF'
        val len = bb.getInt(12)
        val json = Json.parseToJsonElement(String(glb.readBytes(), 20, len)).jsonObject
        return json["nodes"]!!.jsonArray.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content }.map { it.substringBefore("__") }.toSet()
    }
    @Test fun everyMeshNameExistsInGlb() {
        val names = glbNodeNames()
        val cat = RegionCatalog.parse(File("src/main/assets/regions.json").readText())
        cat.regions.flatMap { it.meshNames }.forEach { assertTrue("missing in GLB: $it", it in names) }
    }
    @Test fun splitPiecesExistAndMatchAspects() {
        val raw = glb.readBytes(); val bb = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
        val all = Json.parseToJsonElement(String(raw, 20, bb.getInt(12))).jsonObject["nodes"]!!.jsonArray
            .mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content }
        val head = all.filter { it.startsWith("head@jaw__") }.toSet()
        assertTrue(head.isNotEmpty()); assertTrue(all.none { it.startsWith("head__") })
        head.forEach { assertTrue(Pieces.matches(it, Aspect.FRONT) || Pieces.matches(it, Aspect.BACK)) }
        assertEquals("head@jaw", Pieces.base("head@jaw__FR"))
        assertTrue(Pieces.matches("ear_L", Aspect.BACK)); 
    }
    @Test fun everyLeafRegionIsPickable() {
        val cat = RegionCatalog.parse(File("src/main/assets/regions.json").readText())
        cat.regions.filter { cat.children(it.id).isEmpty() }.forEach { assertTrue(it.id, it.meshNames.isNotEmpty()) }
    }
    @Test fun readyManifestHasLicenseAndFile() {
        val m = AssetManifest.parse(File("src/main/assets/asset_manifest.json").readText()).first { it.id == "body_exterior" }
        assertEquals("READY", m.status); assertNotNull(m.license); assertTrue(glb.exists())
    }

    @Test fun anatomyMeshesExistInTheirLayerFiles() {
        val items = AnatomyCatalog.parse(File("src/main/assets/anatomy.json").readText())
        val manifest = AssetManifest.parse(File("src/main/assets/asset_manifest.json").readText())
        items.forEach { a ->
            val m = manifest.first { it.layer == a.layer }
            val f = File("src/main/assets/" + m.path); assertTrue("missing file ${m.path}", f.exists())
            val bb = ByteBuffer.wrap(f.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
            val json = Json.parseToJsonElement(String(f.readBytes(), 20, bb.getInt(12))).jsonObject
            val names = json["nodes"]!!.jsonArray.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content }
            assertTrue("${a.mesh} not in ${m.path}", a.mesh in names)
        }
        assertEquals(items.size, items.map { it.id }.toSet().size)
    }
}
