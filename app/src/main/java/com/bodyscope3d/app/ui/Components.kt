package com.bodyscope3d.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bodyscope3d.app.R

private val ButtonShape = RoundedCornerShape(14.dp)

/** الزر الرئيسي: إجراء واحد واضح في الشاشة */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    Button(
        onClick, modifier.heightIn(min = 52.dp), enabled = enabled, shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Palette.Primary, contentColor = Palette.OnPrimary,
            disabledContainerColor = Palette.SurfaceHigh, disabledContentColor = Palette.TextSecondary,
        ),
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** زر ثانوي بإطار هادئ */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    OutlinedButton(
        onClick, modifier.heightIn(min = 48.dp), enabled = enabled, shape = ButtonShape,
        border = BorderStroke(1.dp, Palette.Outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Palette.Primary, disabledContentColor = Palette.TextSecondary),
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

/** زر أيقوني دائري (48dp) للتحكم بالمجسم، مع وصف لقارئات الشاشة */
@Composable
fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, active: Boolean = false) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(48.dp).semantics { contentDescription = description },
        shape = CircleShape,
        color = if (active) Palette.Primary else Palette.SurfaceHigh,
        contentColor = if (active) Palette.OnPrimary else Palette.TextPrimary,
        border = BorderStroke(1.dp, Palette.Outline),
    ) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(24.dp)) } }
}

/** زر تبديل/اختيار: يُستخدم للطبقات والمناطق والجهات. accent = لون التحديد (المرجاني للألم فقط) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectChip(selected: Boolean, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, accent: Color = Palette.Primary) {
    FilterChip(
        selected, onClick, label = { Text(label, style = MaterialTheme.typography.labelMedium) }, modifier = modifier.heightIn(min = 40.dp),
        leadingIcon = if (icon != null) { { Icon(icon, null, Modifier.size(18.dp)) } } else null,
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Palette.Surface, labelColor = Palette.TextSecondary, iconColor = Palette.TextSecondary,
            selectedContainerColor = accent.copy(alpha = 0.22f), selectedLabelColor = Palette.TextPrimary, selectedLeadingIconColor = accent,
        ),
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Palette.SurfaceHigh, titleContentColor = Palette.TextPrimary, textContentColor = Palette.TextSecondary,
        title = { Text(title) }, text = { Text(text) },
        confirmButton = { TextButton(onConfirm) { Text(confirmLabel, color = Palette.Danger) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel), color = Palette.Primary) } },
    )
}

/** إجراء مدمّر: لا يُنفَّذ قبل التأكيد */
@Composable
fun DestructiveButton(label: String, confirmTitle: String, confirmText: String, onConfirm: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    var ask by remember { mutableStateOf(false) }
    OutlinedButton(
        { ask = true }, modifier.heightIn(min = 48.dp), shape = ButtonShape, border = BorderStroke(1.dp, Palette.Danger),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Palette.Danger),
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
    if (ask) ConfirmDialog(confirmTitle, confirmText, label, { ask = false; onConfirm() }, { ask = false })
}

@Composable
fun AppCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val body: @Composable () -> Unit = { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content) }
    if (onClick != null) Surface(onClick, modifier, shape = RoundedCornerShape(16.dp), color = Palette.Surface, border = BorderStroke(1.dp, Palette.Outline), content = body)
    else Surface(modifier, shape = RoundedCornerShape(16.dp), color = Palette.Surface, border = BorderStroke(1.dp, Palette.Outline), content = body)
}

@Composable
fun AppTopBar(title: String, onBack: (() -> Unit)? = null, showLogo: Boolean = false, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (onBack != null) RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), onBack)
        if (showLogo) Surface(Modifier.size(40.dp), shape = RoundedCornerShape(12.dp), color = Palette.DeepBlue) {
            Icon(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.fillMaxSize(), tint = Color.Unspecified)
        }
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = Palette.TextPrimary, maxLines = 1)
        actions()
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) =
    Text(text, modifier, style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)

@Composable
fun EmergencyBanner(modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Palette.DangerBg, border = BorderStroke(1.dp, Palette.Danger.copy(alpha = 0.5f))) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Warning, null, tint = Palette.Danger, modifier = Modifier.size(22.dp))
            Column {
                Text(stringResource(R.string.emergency_title), style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
                Text(stringResource(R.string.emergency_body), style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
            }
        }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) = Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
    CircularProgressIndicator(color = Palette.Primary, strokeWidth = 3.dp)
    Text(stringResource(R.string.loading_model), color = Palette.TextSecondary, style = MaterialTheme.typography.bodyMedium)
}

@Composable
fun ErrorBox(message: String, retry: (() -> Unit)? = null, modifier: Modifier = Modifier) = AppCard(modifier) {
    Text(message, color = Palette.TextPrimary, style = MaterialTheme.typography.bodyMedium)
    if (retry != null) PrimaryButton(stringResource(R.string.retry), retry)
}

@Composable
fun ActionTile(icon: ImageVector, title: String, onClick: () -> Unit, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Surface(
        onClick, modifier.heightIn(min = 76.dp), shape = RoundedCornerShape(16.dp),
        color = if (highlight) Palette.DeepBlue else Palette.SurfaceHigh, border = BorderStroke(1.dp, if (highlight) Palette.Primary else Palette.Outline),
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
            Icon(icon, null, tint = Palette.Primary, modifier = Modifier.size(26.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = Palette.TextPrimary, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
