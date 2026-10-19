package com.bodyscope3d.app.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.bodyscope3d.app.R
import com.bodyscope3d.app.data.*
import java.text.DateFormat
import java.util.Date

private fun natureRes(n: String) = when (n) {
    "sharp" -> R.string.nature_sharp; "dull" -> R.string.nature_dull; "burning" -> R.string.nature_burning
    "throbbing" -> R.string.nature_throbbing; "cramping" -> R.string.nature_cramping; else -> R.string.nature_other
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PainLogScreen(graph: AppGraph, preselected: String?, onSaved: () -> Unit) {
    val ar = isArabic(); val scope = rememberCoroutineScope()
    var region by remember { mutableStateOf(preselected) }
    var intensity by remember { mutableFloatStateOf(0f) }
    var nature by remember { mutableStateOf<String?>(null) }
    var onset by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var symptoms by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val pending = remember { mutableStateListOf<PainEntryEntity>() }

    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EmergencyBanner() // لا يتأخر الإرشاد الطارئ بسبب الاستبيان
        Text(stringResource(R.string.pain_region), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            graph.catalog.regions.forEach { r -> FilterChip(r.id == region, { region = r.id }, { Text(r.name(ar)) }) }
        }
        Text(stringResource(R.string.pain_intensity) + ": ${intensity.toInt()}")
        Slider(intensity, { intensity = it }, valueRange = 0f..10f, steps = 9)
        Text(stringResource(R.string.pain_nature), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PainValidator.natures.forEach { n -> FilterChip(n == nature, { nature = n }, { Text(stringResource(natureRes(n))) }) }
        }
        OutlinedTextField(onset, { onset = it }, label = { Text(stringResource(R.string.pain_onset)) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(duration, { duration = it }, label = { Text(stringResource(R.string.pain_duration)) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(symptoms, { symptoms = it }, label = { Text(stringResource(R.string.pain_symptoms)) }, modifier = Modifier.fillMaxWidth())
        if (error) Text(stringResource(R.string.err_intensity), color = MaterialTheme.colorScheme.error)
        Button({
            if (PainValidator.valid(region, intensity.toInt(), nature)) {
                pending += PainEntryEntity(sessionId = 0, regionId = region!!, intensity = intensity.toInt(), nature = nature!!,
                    onset = onset.trim(), duration = duration.trim(), symptoms = symptoms.trim())
                nature = null; intensity = 0f; onset = ""; duration = ""; symptoms = ""; error = false
            } else error = true
        }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.pain_add)) }

        if (pending.isNotEmpty()) {
            Text(stringResource(R.string.pain_review), style = MaterialTheme.typography.titleMedium)
            pending.forEachIndexed { i, e ->
                ListItem(headlineContent = { Text(graph.catalog.byId(e.regionId)?.name(ar) ?: e.regionId) },
                    supportingContent = { Text("${e.intensity}/10 · " + stringResource(natureRes(e.nature))) },
                    trailingContent = { TextButton({ pending.removeAt(i) }) { Text(stringResource(R.string.remove)) } })
            }
            Button({ scope.launchIO { graph.repo.save(pending.toList()) }; onSaved() }, Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

@Composable
fun HistoryScreen(graph: AppGraph) {
    val ar = isArabic(); val scope = rememberCoroutineScope()
    val sessions by graph.repo.sessions().collectAsState(emptyList())
    if (sessions.isEmpty()) { Text(stringResource(R.string.history_empty), Modifier.padding(16.dp)); return }
    LazyColumn(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(sessions, key = { it.session.id }) { s ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(DateFormat.getDateTimeInstance().format(Date(s.session.createdAt)), style = MaterialTheme.typography.titleSmall)
                    s.entries.forEach { e ->
                        Text("• " + (graph.catalog.byId(e.regionId)?.name(ar) ?: e.regionId) + " — ${e.intensity}/10, " + stringResource(natureRes(e.nature)) +
                            listOf(e.onset, e.duration, e.symptoms).filter { it.isNotBlank() }.joinToString(" | ", prefix = " | ").takeIf { e.onset.isNotBlank() || e.duration.isNotBlank() || e.symptoms.isNotBlank() }.orEmpty())
                    }
                    TextButton({ scope.launchIO { graph.repo.delete(s.session.id) } }) { Text(stringResource(R.string.delete)) }
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(graph: AppGraph) {
    val ar = isArabic()
    val favs by graph.repo.favorites().collectAsState(emptyList())
    if (favs.isEmpty()) { Text(stringResource(R.string.favorites_empty), Modifier.padding(16.dp)); return }
    LazyColumn { items(favs) { id -> graph.catalog.byId(id)?.let { ListItem(headlineContent = { Text(it.name(ar)) }, supportingContent = { Text(it.desc(ar)) }) } } }
}

@Composable
fun SettingsScreen(graph: AppGraph, go: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ar")) }) { Text(stringResource(R.string.lang_ar)) }
            Button({ AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en")) }) { Text(stringResource(R.string.lang_en)) }
        }
        OutlinedButton({ go("privacy") }) { Text(stringResource(R.string.privacy)) }
        OutlinedButton({ confirm = true }) { Text(stringResource(R.string.clear_data)) }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false },
        text = { Text(stringResource(R.string.clear_confirm)) },
        confirmButton = { TextButton({ scope.launchIO { graph.repo.clearAll() }; confirm = false }) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton({ confirm = false }) { Text(stringResource(R.string.cancel)) } })
}
