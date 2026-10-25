package com.bodyscope3d.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.bodyscope3d.app.R
import com.bodyscope3d.app.data.*
import com.bodyscope3d.app.viewer.Orbit
import com.bodyscope3d.app.viewer.Viewer3D

/** الشاشة الرئيسية = شاشة الجسم الكامل + تحديد المنطقة + عزلها: المجسم هو العنصر الأساسي واللوحة السفلية قابلة للسحب */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(graph: AppGraph, vm: ExploreViewModel, go: (String) -> Unit) {
    val st by vm.state.collectAsState()
    val ar = isArabic()
    val cat = graph.catalog
    val sel = cat.byId(st.selectedId)
    val model = graph.model(Layer.EXTERIOR)
    val modelThere = model != null && graph.isReady(model)
    var loadError by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    var camKey by remember { mutableIntStateOf(0) }
    var liveYaw by remember { mutableFloatStateOf(0f) }
    var flipBack by remember { mutableStateOf(false) }
    val orbit = remember { Orbit() }
    val sheet = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(sel?.id) {
        if (sel != null) sheet.bottomSheetState.expand()
        else if (sheet.bottomSheetState.currentValue == SheetValue.Expanded) sheet.bottomSheetState.partialExpand()
    }

    BottomSheetScaffold(
        scaffoldState = sheet, sheetPeekHeight = 118.dp,
        containerColor = Palette.Bg, sheetContainerColor = Palette.Surface, sheetContentColor = Palette.TextPrimary,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), sheetTonalElevation = 0.dp, sheetShadowElevation = 0.dp,
        sheetDragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 6.dp).size(width = 44.dp, height = 5.dp).background(Palette.Outline, CircleShape))
        },
        sheetContent = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 380.dp).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (sel == null) IdlePanel(graph, vm, go, onPain = { scope.launch { sheet.bottomSheetState.expand() } })
                else RegionPanel(graph, vm, sel, st, go)
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            AppTopBar(stringResource(R.string.app_name), showLogo = true) {
                RoundIconButton(Icons.Rounded.Settings, stringResource(R.string.settings), { go("settings") })
            }
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium, color = Palette.TextPrimary)
                Text(stringResource(R.string.home_subtitle), style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary)
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(top = 4.dp)) {
                when {
                    !modelThere -> ErrorBox(stringResource(R.string.model_missing), null, Modifier.padding(16.dp))
                    loadError -> ErrorBox(stringResource(R.string.model_error), { loadError = false; loaded = false; attempt++ }, Modifier.padding(16.dp))
                    else -> key(attempt) {
                        Viewer3D(
                            model!!.path, sel, st.isolated, cat.regions,
                            onMeshTapped = { mesh ->
                                // قطعة قد تنتمي لأكثر من منطقة (صدر/ظهر): نفضّل الأقرب لزاوية الكاميرا الحالية
                                cat.regions.filter { Pieces.base(mesh) in it.meshNames }
                                    .minByOrNull { kotlin.math.abs(((it.camera.yawDeg - liveYaw + 540f) % 360f) - 180f) }
                                    ?.let { vm.select(it.id) }
                            },
                            onError = { loadError = true }, onYaw = { liveYaw = it }, aspect = st.aspect, resetKey = camKey,
                            viewYawDeg = if (sel == null) (if (flipBack) 180f else 0f) else st.aspect?.yawDeg,
                            orbit = orbit, onLoaded = { loaded = true }, modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                if (modelThere && !loaded && !loadError) LoadingBox(Modifier.align(Alignment.Center))
                if (modelThere && !loadError) {
                    Column(Modifier.align(Alignment.TopEnd).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RoundIconButton(Icons.Rounded.ZoomIn, stringResource(R.string.ctl_zoom_in), { orbit.zoom = (orbit.zoom * 0.8f).coerceIn(0.4f, 2.5f) })
                        RoundIconButton(Icons.Rounded.ZoomOut, stringResource(R.string.ctl_zoom_out), { orbit.zoom = (orbit.zoom * 1.25f).coerceIn(0.4f, 2.5f) })
                        RoundIconButton(Icons.Rounded.RotateLeft, stringResource(R.string.ctl_rotate_left), { orbit.yaw -= 45f })
                        RoundIconButton(Icons.Rounded.RotateRight, stringResource(R.string.ctl_rotate_right), { orbit.yaw += 45f })
                        RoundIconButton(Icons.Rounded.Cameraswitch, stringResource(R.string.ctl_flip), {
                            if (sel == null) flipBack = !flipBack
                            else vm.setAspect(if (st.aspect == Aspect.BACK) Aspect.FRONT else Aspect.BACK)
                        })
                        RoundIconButton(Icons.Rounded.RestartAlt, stringResource(R.string.ctl_reset), { orbit.reset(); flipBack = false; vm.fullBody(); camKey++ })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.IdlePanel(graph: AppGraph, vm: ExploreViewModel, go: (String) -> Unit, onPain: () -> Unit) {
    val ar = isArabic()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ActionTile(Icons.Rounded.Healing, stringResource(R.string.act_pain), onPain, Modifier.weight(1f), highlight = true)
        ActionTile(Icons.Rounded.Accessibility, stringResource(R.string.act_anatomy), { go("anatomy") }, Modifier.weight(1f))
        ActionTile(Icons.Rounded.Bookmarks, stringResource(R.string.act_saved), { go("saved") }, Modifier.weight(1f))
    }
    SectionTitle(stringResource(R.string.pick_hint))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        graph.catalog.roots.forEach { r -> SelectChip(false, r.name(ar), { vm.select(r.id) }, accent = Palette.Pain) }
    }
    SecondaryButton(stringResource(R.string.act_records), { go("history") }, Modifier.fillMaxWidth(), Icons.Rounded.History)
    EmergencyBanner()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.RegionPanel(graph: AppGraph, vm: ExploreViewModel, sel: Region, st: ExploreState, go: (String) -> Unit) {
    val ar = isArabic(); val cat = graph.catalog
    val favs by graph.repo.favorites().collectAsState(emptyList())
    val scope = rememberCoroutineScope()
    val isFav = sel.id in favs
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(12.dp).background(Palette.Pain, CircleShape))   // مؤشر المنطقة المختارة (المرجاني للألم فقط)
        Column(Modifier.weight(1f)) {
            Text(sel.name(ar), style = MaterialTheme.typography.titleLarge, color = Palette.TextPrimary)
            val trail = generateSequence(cat.byId(sel.parentId)) { cat.byId(it.parentId) }.toList().reversed().joinToString(" ‹ ") { it.name(ar) }
            if (trail.isNotEmpty()) Text(trail, style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
        }
        RoundIconButton(if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            stringResource(if (isFav) R.string.favorite_remove else R.string.favorite_add), { scope.launchIO { graph.repo.set(sel.id, !isFav) } }, active = isFav)
        RoundIconButton(Icons.Rounded.Close, stringResource(R.string.deselect), { vm.deselect() })
    }
    Text(sel.desc(ar), style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary)

    // مناطق دقيقة قابلة للتحديد: الأب + أبناؤه (أو إخوة المنطقة الحالية)
    val parent = if (sel.parentId == null) sel else cat.byId(sel.parentId)
    val options = if (parent == null) emptyList() else listOf(parent) + cat.children(parent.id).flatMap { listOf(it) + cat.children(it.id) }
    if (options.size > 1) {
        SectionTitle(stringResource(R.string.sub_regions))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { r -> SelectChip(r.id == sel.id, r.name(ar), { vm.select(r.id) }, accent = Palette.Pain) }
        }
    }
    SectionTitle(stringResource(R.string.aspect_label))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Aspect.entries.forEach { a -> SelectChip(st.aspect == a, stringResource(aspectRes(a)), { vm.setAspect(a) }, accent = Palette.Pain) }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SelectChip(st.isolated, stringResource(if (st.isolated) R.string.show_with_body else R.string.isolate),
            { if (st.isolated) vm.showWithBody() else vm.isolate() }, icon = Icons.Rounded.CenterFocusStrong)
        SelectChip(false, stringResource(R.string.undo), { vm.undo() }, icon = Icons.Rounded.Undo)
    }
    PrimaryButton(stringResource(R.string.confirm_region), { go("pain") }, Modifier.fillMaxWidth(), Icons.Rounded.Check)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val siblings = cat.regions.filter { it.parentId == sel.parentId && it.level == sel.level }
        SecondaryButton(stringResource(R.string.next_region), {
            siblings.getOrNull((siblings.indexOfFirst { it.id == sel.id } + 1) % siblings.size.coerceAtLeast(1))?.let { vm.select(it.id) }
        }, Modifier.weight(1f), Icons.Rounded.SkipNext, enabled = siblings.size > 1)
        SecondaryButton(stringResource(R.string.back_to_body), { vm.fullBody() }, Modifier.weight(1f))
    }
}

fun aspectRes(a: Aspect) = when (a) {
    Aspect.FRONT -> R.string.aspect_front; Aspect.BACK -> R.string.aspect_back
    Aspect.RIGHT -> R.string.aspect_right; Aspect.LEFT -> R.string.aspect_left
}
