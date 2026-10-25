package com.bodyscope3d.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.bodyscope3d.app.R
import com.bodyscope3d.app.data.*

/** مسودة السجل: تبقى عند الرجوع بين الشاشات (ViewModel على مستوى النشاط) فلا يضيع ما أُدخل */
class PainDraftViewModel : ViewModel() {
    val entries = mutableStateListOf<PainEntryEntity>()
    var intensity by mutableIntStateOf(-1)
    var nature by mutableStateOf<String?>(null)
    var onset by mutableStateOf("")
    var duration by mutableStateOf("")
    var notes by mutableStateOf("")

    fun canCommit(regionId: String?) = PainValidator.valid(regionId, intensity, nature)
    /** يضيف الموضع الحالي إلى المسودة ويصفّر النموذج؛ يعيد true عند النجاح */
    fun commit(regionId: String?, aspect: Aspect?): Boolean {
        if (!canCommit(regionId)) return false
        entries += PainEntryEntity(sessionId = 0, regionId = regionId!!, intensity = intensity, nature = nature!!,
            onset = onset.trim(), duration = duration.trim(), symptoms = notes.trim(), aspect = aspect?.key.orEmpty())
        resetForm(); return true
    }
    fun resetForm() { intensity = -1; nature = null; onset = ""; duration = ""; notes = "" }
    fun clearAll() { entries.clear(); resetForm() }
}

fun natureRes(n: String) = when (n) {
    "sharp" -> R.string.nature_sharp; "dull" -> R.string.nature_dull; "burning" -> R.string.nature_burning
    "throbbing" -> R.string.nature_throbbing; "cramping" -> R.string.nature_cramping; else -> R.string.nature_other
}
fun severityRes(i: Int) = when { i <= 0 -> R.string.sev_none; i <= 3 -> R.string.sev_mild; i <= 6 -> R.string.sev_moderate; else -> R.string.sev_severe }

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Palette.TextPrimary, unfocusedTextColor = Palette.TextPrimary,
    focusedBorderColor = Palette.Primary, unfocusedBorderColor = Palette.Outline,
    focusedLabelColor = Palette.Primary, unfocusedLabelColor = Palette.TextSecondary, cursorColor = Palette.Primary,
)

/** مختار الشدة 0–10: أرقام + وصف نصي (اللون ليس الوسيلة الوحيدة) */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IntensityPicker(value: Int, onChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0..10) {
                val on = i == value
                Surface(
                    onClick = { onChange(i) },
                    modifier = Modifier.size(48.dp).semantics { contentDescription = "$i" },
                    shape = CircleShape, color = if (on) Palette.Pain else Palette.SurfaceHigh,
                    contentColor = if (on) Palette.Bg else Palette.TextPrimary, border = BorderStroke(1.dp, if (on) Palette.Pain else Palette.Outline),
                ) { Box(contentAlignment = Alignment.Center) { Text("$i", style = MaterialTheme.typography.labelLarge) } }
            }
        }
        if (value >= 0) Text("$value / 10 — " + stringResource(severityRes(value)), style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
        Text(stringResource(R.string.pain_subjective), style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NaturePicker(value: String?, onChange: (String) -> Unit) =
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PainValidator.natures.forEach { n -> SelectChip(n == value, stringResource(natureRes(n)), { onChange(n) }, accent = Palette.Pain) }
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PainFormScreen(graph: AppGraph, vm: ExploreViewModel, draft: PainDraftViewModel, nav: (String) -> Unit, back: () -> Unit, done: () -> Unit) {
    val st by vm.state.collectAsState(); val ar = isArabic(); val scope = rememberCoroutineScope()
    val region = graph.catalog.byId(st.selectedId)
    val canSave = draft.entries.isNotEmpty() || draft.canCommit(region?.id)

    Column(Modifier.fillMaxSize()) {
        AppTopBar(stringResource(R.string.pain_form_title), back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            EmergencyBanner()
            AppCard {
                SectionTitle(stringResource(R.string.pain_region))
                if (region == null) Text(stringResource(R.string.no_region), color = Palette.TextSecondary)
                else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(12.dp).background(Palette.Pain, CircleShape)); Text(region.name(ar), style = MaterialTheme.typography.titleLarge, color = Palette.TextPrimary)
                }
                region?.side?.let { Text(stringResource(if (it == "R") R.string.side_r else R.string.side_l), color = Palette.TextSecondary, style = MaterialTheme.typography.bodyMedium) }
                SectionTitle(stringResource(R.string.aspect_label))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Aspect.entries.forEach { a -> SelectChip(st.aspect == a, stringResource(aspectRes(a)), { vm.setAspect(a) }, accent = Palette.Pain) }
                }
            }
            AppCard {
                SectionTitle(stringResource(R.string.pain_intensity))
                IntensityPicker(draft.intensity) { draft.intensity = it }
                SectionTitle(stringResource(R.string.pain_nature))
                NaturePicker(draft.nature) { draft.nature = it }
            }
            AppCard {
                OutlinedTextField(draft.onset, { draft.onset = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.pain_onset)) }, colors = fieldColors(), singleLine = true)
                OutlinedTextField(draft.duration, { draft.duration = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.pain_duration)) }, colors = fieldColors(), singleLine = true)
                OutlinedTextField(draft.notes, { draft.notes = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.pain_symptoms)) }, colors = fieldColors(), minLines = 2)
            }
            if (draft.entries.isNotEmpty()) AppCard {
                SectionTitle(stringResource(R.string.draft_title))
                draft.entries.forEachIndexed { i, e ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text((graph.catalog.byId(e.regionId)?.name(ar) ?: e.regionId) + (Aspect.fromKey(e.aspect)?.let { " (" + stringResource(aspectRes(it)) + ")" } ?: ""), color = Palette.TextPrimary)
                            Text("${e.intensity}/10 · " + stringResource(natureRes(e.nature)), style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
                        }
                        RoundIconButton(Icons.Rounded.Delete, stringResource(R.string.remove), { draft.entries.removeAt(i) })
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        // أزرار الإجراء ثابتة أسفل الشاشة: رئيسي واحد + ثانويان
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton(stringResource(R.string.save_pain), {
                draft.commit(region?.id, st.aspect)
                val toSave = draft.entries.toList()
                if (toSave.isNotEmpty()) { scope.launchIO { graph.repo.save(toSave) }; draft.clearAll(); vm.fullBody(); done() }
            }, Modifier.fillMaxWidth(), Icons.Rounded.Check, enabled = canSave)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(stringResource(R.string.add_another), {
                    if (draft.commit(region?.id, st.aspect)) { vm.fullBody(); back() }
                }, Modifier.weight(1f), Icons.Rounded.AddLocationAlt, enabled = draft.canCommit(region?.id))
                SecondaryButton(stringResource(R.string.back_to_region), back, Modifier.weight(1f))
            }
        }
    }
}

