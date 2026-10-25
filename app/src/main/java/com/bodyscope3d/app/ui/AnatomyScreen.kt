package com.bodyscope3d.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bodyscope3d.app.R
import com.bodyscope3d.app.data.*
import com.bodyscope3d.app.viewer.AnatomyViewer
import com.bodyscope3d.app.viewer.LayerModel
import com.bodyscope3d.app.viewer.Orbit

private fun layerName(l: Layer) = when (l) {
    Layer.EXTERIOR -> R.string.layer_exterior; Layer.SKELETAL -> R.string.layer_skeletal
    Layer.MUSCULAR -> R.string.layer_muscular; Layer.ORGANS -> R.string.layer_organs
}
private fun layerIcon(l: Layer): ImageVector = when (l) {
    Layer.EXTERIOR -> Icons.Rounded.Accessibility; Layer.SKELETAL -> Icons.Rounded.Layers
    Layer.MUSCULAR -> Icons.Rounded.FitnessCenter; Layer.ORGANS -> Icons.Rounded.MonitorHeart
}
private fun systemName(s: String) = when (s) {
    "nervous" -> R.string.sys_nervous; "respiratory" -> R.string.sys_respiratory; "circulatory" -> R.string.sys_circulatory
    "digestive" -> R.string.sys_digestive; "urinary" -> R.string.sys_urinary; "immune" -> R.string.sys_immune; else -> R.string.sys_skeletal
}

/** شاشة التشريح الداخلي + بطاقة معلومات العضو. تعرض فقط الطبقات الموجودة فعليًا بين ملفات النماذج. */
@Composable
fun AnatomyScreen(graph: AppGraph, back: () -> Unit) {
    val ar = isArabic(); val reduce = LocalReduceMotion.current
    val ready = graph.models.filter { graph.isReady(it) }
    val internal = ready.filter { it.layer != Layer.EXTERIOR }
    var visible by remember { mutableStateOf(internal.take(1).map { it.id }.toSet()) }
    var picked by remember { mutableStateOf<String?>(null) }
    var isolate by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var camKey by remember { mutableIntStateOf(0) }
    var yaw by remember { mutableFloatStateOf(0f) }
    val orbit = remember { Orbit() }
    val item = graph.anatomy.firstOrNull { it.id == picked }
    val layers = ready.map { LayerModel(it.id, it.path, it.id in visible) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(stringResource(R.string.home_anatomy), back)
        if (internal.isEmpty()) {
            Text(stringResource(R.string.layers_none), Modifier.padding(16.dp), color = Palette.TextSecondary); return@Column
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (err) ErrorBox(stringResource(R.string.model_error), { err = false; loaded = false; camKey++ }, Modifier.padding(16.dp))
            else key(camKey) {
                AnatomyViewer(
                    layers, onNodeTapped = { n -> graph.anatomy.firstOrNull { it.mesh == n }?.let { picked = it.id; isolate = true } },
                    onError = { err = true }, viewYawDeg = yaw, selectedMesh = item?.mesh, focus = item?.camera, isolate = isolate,
                    orbit = orbit, onLoaded = { loaded = true }, modifier = Modifier.fillMaxSize(),
                )
            }
            if (!loaded && !err) LoadingBox(Modifier.align(Alignment.Center))
            Column(Modifier.align(Alignment.TopEnd).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RoundIconButton(Icons.Rounded.ZoomIn, stringResource(R.string.ctl_zoom_in), { orbit.zoom = (orbit.zoom * 0.8f).coerceIn(0.4f, 2.5f) })
                RoundIconButton(Icons.Rounded.ZoomOut, stringResource(R.string.ctl_zoom_out), { orbit.zoom = (orbit.zoom * 1.25f).coerceIn(0.4f, 2.5f) })
                RoundIconButton(Icons.Rounded.Cameraswitch, stringResource(R.string.ctl_flip), { yaw = if (yaw == 0f) 180f else 0f })
                RoundIconButton(Icons.Rounded.RestartAlt, stringResource(R.string.ctl_reset), { yaw = 0f; picked = null; isolate = true; visible = internal.take(1).map { it.id }.toSet(); camKey++ })
            }
        }
        // بطاقة معلومات العضو
        AnimatedVisibility(item != null, enter = fadeIn(tween(if (reduce) 0 else 200)), exit = fadeOut(tween(if (reduce) 0 else 120))) {
            item?.let { a ->
                AppCard(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(a.name(ar), style = MaterialTheme.typography.titleMedium, color = Palette.TextPrimary)
                            Text(stringResource(R.string.info_system) + ": " + stringResource(systemName(a.system)), style = MaterialTheme.typography.labelSmall, color = Palette.Primary)
                        }
                        RoundIconButton(Icons.Rounded.CenterFocusStrong, stringResource(if (isolate) R.string.show_all else R.string.isolate), { isolate = !isolate }, active = isolate)
                        Spacer(Modifier.width(8.dp))
                        RoundIconButton(Icons.Rounded.Close, stringResource(R.string.ctl_close), { picked = null })
                    }
                    Column(Modifier.heightIn(max = 120.dp).verticalScroll(rememberScrollState())) {
                        Text(a.desc(ar), style = MaterialTheme.typography.bodyMedium, color = Palette.TextPrimary)
                        Text(stringResource(R.string.schematic_note), style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
                        Text(stringResource(R.string.content_unverified), style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
                    }
                }
            }
        }
        // شريط الطبقات
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ready.forEach { m ->
                SelectChip(m.id in visible, stringResource(layerName(m.layer)), { visible = if (m.id in visible) visible - m.id else visible + m.id }, icon = layerIcon(m.layer))
            }
        }
        // أعضاء الطبقات الظاهرة
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            graph.anatomy.filter { a -> ready.any { it.layer == a.layer && it.id in visible } }.forEach { a ->
                SelectChip(a.id == picked, a.name(ar), { picked = a.id; isolate = true }, accent = Palette.Primary)
            }
        }
        Text(stringResource(R.string.layers_unavailable), Modifier.padding(horizontal = 16.dp, vertical = 6.dp), style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
    }
}
