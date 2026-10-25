package com.bodyscope3d.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bodyscope3d.app.R
import com.bodyscope3d.app.data.*
import com.bodyscope3d.app.viewer.Viewer3D

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen(graph: AppGraph, vm: ExploreViewModel, onDetails: () -> Unit, onLog: () -> Unit) {
    val st by vm.state.collectAsState()
    val ar = isArabic()
    val cat = graph.catalog
    val model = graph.model(Layer.EXTERIOR)
    val ready = model != null && graph.isReady(model)
    var loadError by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    var yaw by remember { mutableStateOf<Float?>(null) }
    var liveYaw by remember { mutableFloatStateOf(0f) }
    var camKey by remember { mutableIntStateOf(0) }
    val sel = cat.byId(st.selectedId)

    Column(Modifier.fillMaxSize().padding(8.dp)) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            when {
                !ready -> Banner(R.string.model_missing)
                loadError -> Column { Banner(R.string.model_error); Button({ loadError = false; attempt++ }) { Text(stringResource(R.string.retry)) } }
                else -> key(attempt) {
                    Viewer3D(model!!.path, sel, st.isolated, cat.regions,
                        onMeshTapped = { mesh ->
                            // شبكة قد تنتمي لأكثر من منطقة (مثل الصدر/الظهر): نفضّل الأقرب لزاوية الكاميرا المعروفة
                            val y = liveYaw
                            cat.regions.filter { Pieces.base(mesh) in it.meshNames }
                                .minByOrNull { kotlin.math.abs(((it.camera.yawDeg - y + 540f) % 360f) - 180f) }
                                ?.let { vm.select(it.id) }
                        },
                        onError = { loadError = true }, onYaw = { liveYaw = it }, aspect = st.aspect, resetKey = camKey, viewYawDeg = if (sel == null) yaw else st.aspect?.yawDeg,
                        modifier = Modifier.fillMaxSize())
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (ready) {
                AssistChip({ yaw = 0f; camKey++ }, { Text(stringResource(R.string.view_front)) })
                AssistChip({ yaw = 180f; camKey++ }, { Text(stringResource(R.string.view_back)) })
                AssistChip({ yaw = 90f; camKey++ }, { Text(stringResource(R.string.view_side)) })
                AssistChip({ yaw = null; vm.fullBody(); camKey++ }, { Text(stringResource(R.string.reset_camera)) })
            }
            if (sel != null) {
                AssistChip({ vm.fullBody() }, { Text(stringResource(R.string.full_body)) })
                AssistChip({ if (st.isolated) vm.showWithBody() else vm.isolate() },
                    { Text(stringResource(if (st.isolated) R.string.show_with_body else R.string.isolate)) })
                AssistChip({ vm.undo() }, { Text(stringResource(R.string.undo)) }, enabled = st.history.isNotEmpty())
                AssistChip({ vm.deselect() }, { Text(stringResource(R.string.deselect)) })
            }
        }
        Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
            // مستويات الدقة: منطقة كبيرة ← منطقة أصغر
            val parent = sel?.let { if (it.parentId == null) it else cat.byId(it.parentId) }
            val list = if (parent == null) cat.roots else listOf(parent) + cat.children(parent.id)
            if (parent != null) Text(stringResource(R.string.sub_regions), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                list.forEach { r -> FilterChip(r.id == st.selectedId, { vm.select(r.id) }, { Text(r.name(ar)) }) }
            }
        }
        if (sel != null) {
            Text(stringResource(R.string.aspect_label), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Aspect.entries.forEach { a -> FilterChip(st.aspect == a, { vm.setAspect(a) }, { Text(stringResource(aspectRes(a))) }) }
            }
        }
        if (sel != null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onDetails) { Text(stringResource(R.string.region_details)) }
            OutlinedButton(onLog) { Text(stringResource(R.string.home_log)) }
        }
    }
}

fun aspectRes(a: Aspect) = when (a) {
    Aspect.FRONT -> R.string.aspect_front; Aspect.BACK -> R.string.aspect_back
    Aspect.RIGHT -> R.string.aspect_right; Aspect.LEFT -> R.string.aspect_left
}

@Composable private fun Banner(res: Int) = Card(Modifier.fillMaxWidth().padding(8.dp)) {
    Text(stringResource(res), Modifier.padding(12.dp))
}

@Composable
fun DetailScreen(graph: AppGraph, vm: ExploreViewModel) {
    val st by vm.state.collectAsState(); val ar = isArabic()
    val r = graph.catalog.byId(st.selectedId)
    val favs by graph.repo.favorites().collectAsState(emptyList())
    val scope = rememberCoroutineScope()
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (r == null) { Text(stringResource(R.string.full_body)); return@Column }
        Text(r.name(ar), style = MaterialTheme.typography.headlineSmall)
        r.side?.let { Text(stringResource(if (it == "R") R.string.side_r else R.string.side_l)) }
        Text(r.desc(ar))
        Text(stringResource(R.string.content_unverified), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.pointed_note), style = MaterialTheme.typography.bodySmall)
        val fav = r.id in favs
        Button({ scope.launchIO { graph.repo.set(r.id, !fav) } }) {
            Text(stringResource(if (fav) R.string.favorite_remove else R.string.favorite_add))
        }
        EmergencyBanner()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnatomyScreen(graph: AppGraph) {
    val ar = isArabic()
    val ready = graph.models.filter { graph.isReady(it) }
    var visible by remember { mutableStateOf(ready.filter { it.layer != Layer.EXTERIOR }.map { it.id }.toSet()) }
    var picked by remember { mutableStateOf<String?>(null) }
    var err by remember { mutableStateOf(false) }
    var camKey by remember { mutableIntStateOf(0) }
    var yaw by remember { mutableFloatStateOf(0f) }
    val internal = ready.filter { it.layer != Layer.EXTERIOR }
    if (internal.isEmpty()) { Text(stringResource(R.string.layers_none), Modifier.padding(16.dp)); return }
    val item = graph.anatomy.firstOrNull { it.id == picked }
    val layers = ready.map { com.bodyscope3d.app.viewer.LayerModel(it.id, it.path, it.id in visible) }
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (err) Column { Banner(R.string.model_error); Button({ err = false; camKey++ }) { Text(stringResource(R.string.retry)) } }
            else key(camKey) {
                com.bodyscope3d.app.viewer.AnatomyViewer(layers, onNodeTapped = { n -> graph.anatomy.firstOrNull { it.mesh == n }?.let { picked = it.id } },
                    onError = { err = true }, viewYawDeg = yaw, modifier = Modifier.fillMaxSize())
            }
        }
        Text(stringResource(R.string.schematic_note), style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ready.forEach { m ->
                FilterChip(m.id in visible, { visible = if (m.id in visible) visible - m.id else visible + m.id },
                    { Text(stringResource(layerName(m.layer))) })
            }
            AssistChip({ yaw = 0f; camKey++ }, { Text(stringResource(R.string.view_front)) })
            AssistChip({ yaw = 180f; camKey++ }, { Text(stringResource(R.string.view_back)) })
            AssistChip({ yaw = 0f; visible = internal.map { it.id }.toSet(); picked = null; camKey++ }, { Text(stringResource(R.string.reset_camera)) })
        }
        Column(Modifier.heightIn(max = 170.dp).verticalScroll(rememberScrollState())) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                graph.anatomy.filter { a -> ready.any { it.layer == a.layer && it.id in visible } }.forEach { a ->
                    FilterChip(a.id == picked, { picked = a.id }, { Text(a.name(ar)) })
                }
            }
        }
        item?.let {
            Text(it.name(ar), style = MaterialTheme.typography.titleMedium)
            Text(it.desc(ar)); Text(stringResource(R.string.content_unverified), style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun layerName(l: Layer) = when (l) {
    Layer.EXTERIOR -> R.string.layer_exterior; Layer.SKELETAL -> R.string.layer_skeletal
    Layer.MUSCULAR -> R.string.layer_muscular; Layer.ORGANS -> R.string.layer_organs
}
