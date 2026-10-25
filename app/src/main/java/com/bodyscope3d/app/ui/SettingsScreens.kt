package com.bodyscope3d.app.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.bodyscope3d.app.R
import com.bodyscope3d.app.data.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val reduce = LocalReduceMotion.current
    LaunchedEffect(Unit) { delay(if (reduce) 250 else 900); onDone() }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(Modifier.size(96.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp), color = Palette.DeepBlue) {
                Icon(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.fillMaxSize(), tint = Color.Unspecified)
            }
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, color = Palette.TextPrimary)
            Text(stringResource(R.string.splash_tag), style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun LinkRow(icon: ImageVector, label: String, onClick: () -> Unit) =
    AppCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = Palette.Primary)
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Palette.TextSecondary)
        }
    }

@Composable
fun SettingsScreen(graph: AppGraph, go: (String) -> Unit, back: () -> Unit) {
    val scope = rememberCoroutineScope(); val ar = isArabic()
    Column(Modifier.fillMaxSize()) {
        AppTopBar(stringResource(R.string.settings), back)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppCard {
                SectionTitle(stringResource(R.string.language))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectChip(ar, stringResource(R.string.lang_ar), { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ar")) }, icon = Icons.Rounded.Translate)
                    SelectChip(!ar, stringResource(R.string.lang_en), { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en")) }, icon = Icons.Rounded.Translate)
                }
            }
            LinkRow(Icons.Rounded.Info, stringResource(R.string.about)) { go("about") }
            LinkRow(Icons.Rounded.PrivacyTip, stringResource(R.string.privacy)) { go("privacy") }
            LinkRow(Icons.Rounded.QuestionMark, stringResource(R.string.help)) { go("help") }
            AppCard {
                SectionTitle(stringResource(R.string.settings_data))
                Text(stringResource(R.string.clear_confirm), style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
                DestructiveButton(stringResource(R.string.clear_data), stringResource(R.string.clear_data), stringResource(R.string.clear_confirm),
                    { scope.launchIO { graph.repo.clearAll() } }, Modifier.fillMaxWidth(), Icons.Rounded.Delete)
            }
        }
    }
}

@Composable
fun InfoScreen(title: Int, body: Int, back: () -> Unit) = Column(Modifier.fillMaxSize()) {
    AppTopBar(stringResource(title), back)
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(body), style = MaterialTheme.typography.bodyLarge, color = Palette.TextPrimary)
        Text(stringResource(R.string.disclaimer), style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary)
        EmergencyBanner()
    }
}

@Composable
fun AboutScreen(graph: AppGraph, back: () -> Unit) {
    val ctx = LocalContext.current
    val version = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull().orEmpty() }
    Column(Modifier.fillMaxSize()) {
        AppTopBar(stringResource(R.string.about), back)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppCard {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, color = Palette.TextPrimary)
                Text(stringResource(R.string.about_version) + ": " + version, color = Palette.TextSecondary)
                Text(stringResource(R.string.about_notice), style = MaterialTheme.typography.bodyMedium, color = Palette.TextPrimary)
            }
            AppCard {
                SectionTitle(stringResource(R.string.about_assets))
                graph.models.forEach { m ->
                    val ok = graph.isReady(m)
                    Text("• ${m.layer.name}: " + if (ok) "${m.license ?: "?"} — ${m.source ?: "?"}" else stringResource(R.string.about_none),
                        style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
                }
            }
            AppCard { Text(stringResource(R.string.content_unverified), style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary) }
        }
    }
}
