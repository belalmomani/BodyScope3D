package com.bodyscope3d.app.ui

import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/** نظام الألوان: خلفية كحلية داكنة، سماوي للتفاعل، مرجاني لموضع الألم فقط، أخضر للنجاح */
object Palette {
    val Bg = Color(0xFF0A1220); val Surface = Color(0xFF111C30); val SurfaceHigh = Color(0xFF182640)
    val Outline = Color(0xFF2B3E60); val DeepBlue = Color(0xFF123A66)
    val Primary = Color(0xFF38BDF8); val OnPrimary = Color(0xFF04202E)
    val TextPrimary = Color(0xFFF1F5F9); val TextSecondary = Color(0xFFA8B5CA)
    val Pain = Color(0xFFFF7A59); val Success = Color(0xFF5BC98C)
    val Danger = Color(0xFFFF6B6B); val DangerBg = Color(0xFF3A1A22)
}

val LocalReduceMotion = staticCompositionLocalOf { false }


private val AppTypography = Typography(
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 21.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 23.sp),
    bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun BodyScopeTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    // احترام إعداد تقليل الحركة في الجهاز (مقياس الحركة = 0)
    val reduce = remember { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    val scheme = darkColorScheme(
        primary = Palette.Primary, onPrimary = Palette.OnPrimary, primaryContainer = Palette.DeepBlue, onPrimaryContainer = Palette.TextPrimary,
        secondary = Palette.Primary, background = Palette.Bg, onBackground = Palette.TextPrimary,
        surface = Palette.Surface, onSurface = Palette.TextPrimary, surfaceVariant = Palette.SurfaceHigh, onSurfaceVariant = Palette.TextSecondary,
        outline = Palette.Outline, error = Palette.Danger, onError = Color.Black, errorContainer = Palette.DangerBg, onErrorContainer = Palette.TextPrimary,
    )
    MaterialTheme(colorScheme = scheme, typography = AppTypography) {
        CompositionLocalProvider(LocalReduceMotion provides reduce, content = content)
    }
}
