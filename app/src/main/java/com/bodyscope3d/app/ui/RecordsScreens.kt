package com.bodyscope3d.app.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bodyscope3d.app.R
import com.bodyscope3d.app.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

private fun when_(ms: Long) = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(ms))

@Composable
fun HistoryScreen(graph: AppGraph, open: (Long) -> Unit, back: () -> Unit) {
    val ar = isArabic()
    val sessions by graph.repo.sessions().collectAsState(emptyList())
    Column(Modifier.fillMaxSize()) {
        AppTopBar(stringResource(R.string.records_title), back)
        if (sessions.isEmpty()) { Text(stringResource(R.string.history_empty), Modifier.padding(16.dp), color = Palette.TextSecondary); return@Column }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(sessions, key = { it.session.id }) { s ->
                AppCard(Modifier.fillMaxWidth(), onClick = { open(s.session.id) }) {
                    Text(when_(s.session.createdAt), style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
                    Text(s.entries.joinToString("، ") { graph.catalog.byId(it.regionId)?.name(ar) ?: it.regionId }, color = Palette.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Text("${s.entries.size} " + stringResource(R.string.locations_n) + " · " + stringResource(R.string.max_label) + " ${s.entries.maxOfOrNull { it.intensity } ?: 0}/10",
                        style = MaterialTheme.typography.bodySmall, color = Palette.Primary)
                }
            }
        }
    }
}

/** صورة التقرير: الجسم أمام/خلف مع مواضع الألم مرقّمة + التفاصيل. بدون أي احتمالات أو تشخيص. */
private fun buildReport(ctx: Context, graph: AppGraph, ar: Boolean, s: SessionWithEntries) = ReportRenderer.render(
    ctx, ReportTexts(ctx.getString(R.string.export_title), when_(s.session.createdAt), ctx.getString(R.string.view_front), ctx.getString(R.string.view_back),
        ctx.getString(R.string.export_footer), ctx.getString(R.string.app_name)),
    s.entries.mapIndexed { i, e ->
        val r = graph.catalog.byId(e.regionId); val asp = Aspect.fromKey(e.aspect)
        val back = asp == Aspect.BACK || (asp == null && (r?.camera?.yawDeg ?: 0f) >= 179f)
        val (nx, ny) = r?.let { BodyMap.point(it, back) } ?: (0.5f to 0.5f)
        ReportEntry(i + 1, r?.name(ar) ?: e.regionId, asp?.let { ctx.getString(aspectRes(it)) }, back, nx, ny,
            "${e.intensity}/10 — ${ctx.getString(severityRes(e.intensity))} — ${ctx.getString(natureRes(e.nature))}",
            listOfNotNull(
                e.onset.takeIf { it.isNotBlank() }?.let { ctx.getString(R.string.report_onset) + ": " + it },
                e.duration.takeIf { it.isNotBlank() }?.let { ctx.getString(R.string.report_duration) + ": " + it },
                e.symptoms.takeIf { it.isNotBlank() }?.let { ctx.getString(R.string.report_notes) + ": " + it }))
    })

@Composable
fun RecordDetailScreen(graph: AppGraph, id: Long, back: () -> Unit) {
    val ar = isArabic(); val scope = rememberCoroutineScope(); val ctx = LocalContext.current
    val sessions by graph.repo.sessions().collectAsState(emptyList())
    val s = sessions.firstOrNull { it.session.id == id }
    var editing by remember { mutableStateOf<PainEntryEntity?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun share(packages: List<String>) = scope.launch {
        val rec = s ?: return@launch; busy = true
        runCatching {
            val bmp = withContext(Dispatchers.Default) { buildReport(ctx, graph, ar, rec) }
            val uri = withContext(Dispatchers.IO) { ReportShare.uriFor(ctx, ReportShare.writePng(ctx, bmp)) }
            if (!ReportShare.send(ctx, uri, packages, ctx.getString(R.string.share_other))) Toast.makeText(ctx, R.string.whatsapp_missing, Toast.LENGTH_LONG).show()
        }.onFailure { Toast.makeText(ctx, R.string.model_error, Toast.LENGTH_LONG).show() }
        busy = false
    }
    fun saveImage() = scope.launch {
        val rec = s ?: return@launch; busy = true
        val ok = runCatching { withContext(Dispatchers.IO) { ReportShare.saveToGallery(ctx, buildReport(ctx, graph, ar, rec)) } }.getOrDefault(false)
        Toast.makeText(ctx, if (ok) R.string.image_saved else R.string.image_save_unsupported, Toast.LENGTH_LONG).show(); busy = false
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(stringResource(R.string.record_details), back)
        if (s == null) { Text(stringResource(R.string.history_empty), Modifier.padding(16.dp), color = Palette.TextSecondary); return@Column }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(when_(s.session.createdAt), style = MaterialTheme.typography.titleMedium, color = Palette.TextPrimary)
            s.entries.forEach { e ->
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text((graph.catalog.byId(e.regionId)?.name(ar) ?: e.regionId) + (Aspect.fromKey(e.aspect)?.let { " (" + stringResource(aspectRes(it)) + ")" } ?: ""),
                                style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
                            Text("${e.intensity}/10 — " + stringResource(severityRes(e.intensity)) + " · " + stringResource(natureRes(e.nature)), color = Palette.Pain, style = MaterialTheme.typography.bodyMedium)
                        }
                        RoundIconButton(Icons.Rounded.Edit, stringResource(R.string.edit), { editing = e })
                    }
                    listOf(stringResource(R.string.report_onset) to e.onset, stringResource(R.string.report_duration) to e.duration, stringResource(R.string.report_notes) to e.symptoms)
                        .filter { it.second.isNotBlank() }.forEach { (k, v) -> Text("$k: $v", style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary) }
                }
            }
            Text(stringResource(R.string.pain_subjective), style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
            PossibilitiesSection(graph, s.entries)
            Spacer(Modifier.height(4.dp))
        }
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Palette.Primary)
            PrimaryButton(stringResource(R.string.share_whatsapp), { share(ReportShare.WHATSAPP) }, Modifier.fillMaxWidth(), Icons.Rounded.Share, enabled = !busy)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(stringResource(R.string.share_other), { share(emptyList()) }, Modifier.weight(1f), Icons.Rounded.IosShare, enabled = !busy)
                SecondaryButton(stringResource(R.string.save_image), { saveImage() }, Modifier.weight(1f), Icons.Rounded.Download, enabled = !busy)
            }
            DestructiveButton(stringResource(R.string.delete_record), stringResource(R.string.delete_record), stringResource(R.string.delete_record_confirm),
                { scope.launchIO { graph.repo.delete(s.session.id) }; back() }, Modifier.fillMaxWidth(), Icons.Rounded.Delete)
        }
    }
    editing?.let { e -> EditEntryDialog(e, onDismiss = { editing = null }, onSave = { upd -> scope.launchIO { graph.repo.update(upd) }; editing = null }) }
}

/** احتمالات شائعة وعلامات خطر لكل منطقة مسجّلة: قائمة عامة للتثقيف، ليست تشخيصًا ولا ترتيبًا بالأرجحية */
@Composable
fun PossibilitiesSection(graph: AppGraph, entries: List<PainEntryEntity>) {
    val ar = isArabic()
    val groups = entries.map { it.regionId }.distinct().map { id -> id to graph.causes.keyFor(id, graph.catalog) }.distinctBy { it.second }
    if (groups.isEmpty()) return
    AppCard {
        SectionTitle(stringResource(R.string.possibilities_title))
        Text(stringResource(R.string.possibilities_note), style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
        groups.forEach { (regionId, key) ->
            val area = graph.causes.area(key)
            Text(graph.catalog.byId(regionId)?.name(ar) ?: regionId, style = MaterialTheme.typography.titleSmall, color = Palette.Primary)
            area.causes.forEach { Text("• " + it.t(ar), style = MaterialTheme.typography.bodyMedium, color = Palette.TextPrimary) }
            Surface(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), color = Palette.DangerBg) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.possibilities_redflags), style = MaterialTheme.typography.titleSmall, color = Palette.Danger)
                    area.redFlags.forEach { Text("• " + it.t(ar), style = MaterialTheme.typography.bodySmall, color = Palette.TextPrimary) }
                }
            }
        }
        Text(stringResource(R.string.possibilities_footer), style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
        Text(stringResource(R.string.possibilities_unreviewed), style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
    }
}

@Composable
private fun EditEntryDialog(e: PainEntryEntity, onDismiss: () -> Unit, onSave: (PainEntryEntity) -> Unit) {
    var intensity by remember { mutableIntStateOf(e.intensity) }
    var nature by remember { mutableStateOf<String?>(e.nature) }
    var onset by remember { mutableStateOf(e.onset) }
    var duration by remember { mutableStateOf(e.duration) }
    var notes by remember { mutableStateOf(e.symptoms) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Palette.SurfaceHigh, titleContentColor = Palette.TextPrimary,
        title = { Text(stringResource(R.string.edit_entry)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                IntensityPicker(intensity) { intensity = it }
                NaturePicker(nature) { nature = it }
                OutlinedTextField(onset, { onset = it }, label = { Text(stringResource(R.string.pain_onset)) }, colors = fieldColors(), singleLine = true)
                OutlinedTextField(duration, { duration = it }, label = { Text(stringResource(R.string.pain_duration)) }, colors = fieldColors(), singleLine = true)
                OutlinedTextField(notes, { notes = it }, label = { Text(stringResource(R.string.pain_symptoms)) }, colors = fieldColors(), minLines = 2)
            }
        },
        confirmButton = {
            TextButton({ if (PainValidator.valid(e.regionId, intensity, nature)) onSave(e.copy(intensity = intensity, nature = nature!!, onset = onset.trim(), duration = duration.trim(), symptoms = notes.trim())) },
                enabled = PainValidator.valid(e.regionId, intensity, nature)) { Text(stringResource(R.string.save), color = Palette.Primary) }
        },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel), color = Palette.TextSecondary) } },
    )
}

@Composable
fun SavedRegionsScreen(graph: AppGraph, vm: ExploreViewModel, back: () -> Unit) {
    val ar = isArabic()
    val favs by graph.repo.favorites().collectAsState(emptyList())
    Column(Modifier.fillMaxSize()) {
        AppTopBar(stringResource(R.string.act_saved), back)
        if (favs.isEmpty()) { Text(stringResource(R.string.saved_empty), Modifier.padding(16.dp), color = Palette.TextSecondary); return@Column }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(favs) { id ->
                graph.catalog.byId(id)?.let { r ->
                    AppCard(Modifier.fillMaxWidth(), onClick = { vm.select(r.id); back() }) {
                        Text(r.name(ar), style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
                        Text(r.desc(ar), style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary, maxLines = 2)
                    }
                }
            }
        }
    }
}
