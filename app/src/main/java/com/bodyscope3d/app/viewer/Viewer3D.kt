package com.bodyscope3d.app.viewer

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.bodyscope3d.app.data.Region
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import kotlin.math.cos
import kotlin.math.sin

/**
 * غلاف محرك العرض الوحيد. باقي التطبيق لا يعتمد على SceneView.
 * تنبيه: لم يُختبر على جهاز في بيئة الإنشاء — راجع docs/VERIFICATION.md.
 */
@Composable
fun Viewer3D(
    modelPath: String,
    selected: Region?,
    isolated: Boolean,
    regions: List<Region>,
    onMeshTapped: (String) -> Unit,
    onError: (Throwable) -> Unit,
    modifier: Modifier = Modifier,
    resetKey: Int = 0,
    viewYawDeg: Float? = null,
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val cameraNode = rememberCameraNode(engine) { position = Position(z = 2.5f) }
    var modelNode by remember { mutableStateOf<ModelNode?>(null) }

    val nodes = rememberNodes {
        runCatching {
            val m = ModelNode(modelInstance = modelLoader.createModelInstance(modelPath), scaleToUnits = 1.0f)
            modelNode = m
            add(m)
        }.onFailure(onError)
    }

    // انتقال الكاميرا: من الوضع الحالي إلى وضع المنطقة (أو الجسم كاملًا)
    val target = selected?.camera
    val yaw = viewYawDeg ?: target?.yawDeg ?: 0f
    val ty = target?.targetY?.let { it - 0.45f } ?: 0f
    val dist = target?.distance?.coerceIn(0.5f, 3f) ?: 2.5f
    val animYaw by androidx.compose.animation.core.animateFloatAsState(yaw, label = "yaw")
    val animY by androidx.compose.animation.core.animateFloatAsState(ty, label = "y")
    val animD by androidx.compose.animation.core.animateFloatAsState(dist, label = "d")
    LaunchedEffect(animYaw, animY, animD, resetKey) {
        val r = Math.toRadians(animYaw.toDouble())
        cameraNode.position = Position(x = (animD * sin(r)).toFloat(), y = animY, z = (animD * cos(r)).toFloat())
        cameraNode.lookAt(Position(0f, animY, 0f))
    }

    // العزل: إخفاء الشبكات غير التابعة للمنطقة المحددة (أو لأبنائها)
    LaunchedEffect(selected?.id, isolated, modelNode) {
        val m = modelNode ?: return@LaunchedEffect
        val keep = if (selected == null || !isolated) null else
            (listOf(selected) + regions.filter { it.parentId == selected.id }).flatMap { it.meshNames }.toSet()
        runCatching {
            m.renderableNodes.forEach { n -> n.isVisible = keep == null || n.name in keep }
        }.onFailure(onError)
    }

    Scene(
        modifier = modifier, engine = engine, modelLoader = modelLoader,
        cameraNode = cameraNode, childNodes = nodes,
        onGestureListener = rememberOnGestureListener(
            onSingleTapConfirmed = { _, node -> node?.name?.let(onMeshTapped) }
        ),
    )
}
