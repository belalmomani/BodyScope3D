package com.bodyscope3d.app.viewer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.bodyscope3d.app.data.Aspect
import com.bodyscope3d.app.data.CameraPreset
import com.bodyscope3d.app.data.Pieces
import com.bodyscope3d.app.data.Region
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import kotlin.math.cos
import kotlin.math.sin

/*
 * غلاف محرك العرض الوحيد؛ باقي التطبيق لا يعتمد على SceneView.
 * تنبيه: لم يُختبر على جهاز — راجع docs/VERIFICATION.md (أسماء onScroll/onScale/renderableNodes/lookAt من الذاكرة).
 * التحكم بالكاميرا ذاتي (مدار حول هدف) لأن الانتقال البرمجي يتعارض مع cameraManipulator الافتراضي.
 */
@Stable
class Orbit {
    var yaw by mutableFloatStateOf(0f); var pitch by mutableFloatStateOf(0f); var zoom by mutableFloatStateOf(1f)
    fun reset() { yaw = 0f; pitch = 0f; zoom = 1f }
}

@Composable
private fun OrbitEffect(cam: CameraNode, orbit: Orbit, baseYaw: Float, tx: Float, ty: Float, dist: Float, onYaw: (Float) -> Unit) {
    val ay by animateFloatAsState(baseYaw, label = "yaw")
    val ax by animateFloatAsState(tx, label = "x")
    val aY by animateFloatAsState(ty, label = "y")
    val ad by animateFloatAsState(dist, label = "d")
    LaunchedEffect(ay, ax, aY, ad, orbit.yaw, orbit.pitch, orbit.zoom) {
        val yaw = Math.toRadians((ay + orbit.yaw).toDouble()); val p = Math.toRadians(orbit.pitch.toDouble()); val d = ad * orbit.zoom
        cam.position = Position(
            x = ax + (d * sin(yaw) * cos(p)).toFloat(), y = aY + (d * sin(p)).toFloat(), z = (d * cos(yaw) * cos(p)).toFloat())
        cam.lookAt(Position(ax, aY, 0f))
        onYaw(((ay + orbit.yaw) % 360f + 360f) % 360f)
    }
}

private fun subtree(root: Region, all: List<Region>): List<Region> =
    listOf(root) + all.filter { it.parentId == root.id }.flatMap { subtree(it, all) }

@Composable
fun Viewer3D(
    modelPath: String, selected: Region?, isolated: Boolean, regions: List<Region>,
    onMeshTapped: (String) -> Unit, onError: (Throwable) -> Unit, modifier: Modifier = Modifier,
    resetKey: Int = 0, viewYawDeg: Float? = null, onYaw: (Float) -> Unit = {}, aspect: Aspect? = null,
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val cameraNode = rememberCameraNode(engine) { position = Position(z = 2.5f) }
    val orbit = remember { Orbit() }
    var modelNode by remember { mutableStateOf<ModelNode?>(null) }
    val nodes = rememberNodes {
        runCatching {
            val m = ModelNode(modelInstance = modelLoader.createModelInstance(modelPath), scaleToUnits = 1.0f)
            modelNode = m; add(m)
        }.onFailure(onError)
    }
    LaunchedEffect(selected?.id, resetKey, viewYawDeg) { orbit.reset() }
    val c = selected?.camera
    OrbitEffect(cameraNode, orbit, viewYawDeg ?: c?.yawDeg ?: 0f, c?.targetX ?: 0f, c?.targetY ?: 0f,
        c?.distance?.coerceIn(0.4f, 3f) ?: 2.2f, onYaw)

    // العزل: إخفاء كل ما ليس ضمن المنطقة المحددة وأبنائها (كل المستويات)
    LaunchedEffect(selected?.id, isolated, modelNode, aspect) {
        val m = modelNode ?: return@LaunchedEffect
        val keep = if (selected == null || !isolated) null else subtree(selected, regions).flatMap { it.meshNames }.toSet()
        runCatching { m.renderableNodes.forEach { n ->
                val nm = n.name ?: ""
                n.isVisible = keep == null || (Pieces.base(nm) in keep && Pieces.matches(nm, aspect))
            } }.onFailure(onError)
    }
    Scene(
        modifier = modifier, engine = engine, modelLoader = modelLoader, cameraNode = cameraNode,
        cameraManipulator = null, childNodes = nodes,
        onGestureListener = rememberOnGestureListener(
            onSingleTapConfirmed = { _, node -> node?.name?.let(onMeshTapped) },
            onScroll = { _, _, _, d -> orbit.yaw += d.x * 0.4f; orbit.pitch = (orbit.pitch - d.y * 0.3f).coerceIn(-60f, 60f) },
            onScale = { detector, _, _ -> orbit.zoom = (orbit.zoom / detector.scaleFactor).coerceIn(0.4f, 2.5f) },
        ),
    )
}

data class LayerModel(val id: String, val path: String, val visible: Boolean)

/** عارض الطبقات: كل الطبقات الجاهزة تُحمَّل مرة واحدة ويُتحكم بإظهارها فقط */
@Composable
fun AnatomyViewer(
    layers: List<LayerModel>, onNodeTapped: (String) -> Unit, onError: (Throwable) -> Unit,
    modifier: Modifier = Modifier, resetKey: Int = 0, viewYawDeg: Float = 0f,
    selectedMesh: String? = null, focus: CameraPreset? = null, isolate: Boolean = false,
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val cameraNode = rememberCameraNode(engine) { position = Position(z = 2.5f) }
    val orbit = remember { Orbit() }
    val byId = remember { HashMap<String, ModelNode>() }
    val nodes = rememberNodes {
        layers.forEach { l ->
            runCatching {
                val m = ModelNode(modelInstance = modelLoader.createModelInstance(l.path), scaleToUnits = 1.0f)
                byId[l.id] = m; add(m)
            }.onFailure(onError)
        }
    }
    LaunchedEffect(resetKey, selectedMesh, viewYawDeg) { orbit.reset() }
    OrbitEffect(cameraNode, orbit, viewYawDeg, focus?.targetX ?: 0f, focus?.targetY ?: 0f, focus?.distance ?: 2.2f) {}
    // التحديد: عند العزل تُخفى كل القطع ما عدا القطعة المحددة (في كل الطبقات)
    LaunchedEffect(layers, selectedMesh, isolate) {
        val only = isolate && selectedMesh != null
        layers.forEach { l ->
            byId[l.id]?.let { m ->
                m.isVisible = l.visible || only
                runCatching { m.renderableNodes.forEach { n -> n.isVisible = !only || n.name == selectedMesh } }
            }
        }
    }
    Scene(
        modifier = modifier, engine = engine, modelLoader = modelLoader, cameraNode = cameraNode,
        cameraManipulator = null, childNodes = nodes,
        onGestureListener = rememberOnGestureListener(
            onSingleTapConfirmed = { _, node -> node?.name?.let(onNodeTapped) },
            onScroll = { _, _, _, d -> orbit.yaw += d.x * 0.4f; orbit.pitch = (orbit.pitch - d.y * 0.3f).coerceIn(-60f, 60f) },
            onScale = { detector, _, _ -> orbit.zoom = (orbit.zoom / detector.scaleFactor).coerceIn(0.4f, 2.5f) },
        ),
    )
}
