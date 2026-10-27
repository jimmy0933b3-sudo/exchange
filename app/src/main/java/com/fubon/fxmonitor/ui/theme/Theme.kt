package com.fubon.fxmonitor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Ink / mint design system. Explicit surfaces keep every screen and system theme consistent.
val FxInk = Color(0xFF10263A)
val FxMint = Color(0xFF13B89B)
val FxSea = Color(0xFF087F76)
val FxRise = Color(0xFFBD4D57)
val FxFall = Color(0xFF13886C)

private val LightColors = lightColorScheme(
    primary = Color(0xFF087F76),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8F4EC),
    onPrimaryContainer = Color(0xFF07544D),
    secondary = Color(0xFF476779),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4EDF1),
    onSecondaryContainer = FxInk,
    tertiary = FxRise,
    background = Color(0xFFF4F7F8),
    onBackground = FxInk,
    surface = Color.White,
    onSurface = FxInk,
    surfaceVariant = Color(0xFFEDF3F5),
    onSurfaceVariant = Color(0xFF647786),
    outline = Color(0xFF9AABB5),
    outlineVariant = Color(0xFFDDE6EA),
    errorContainer = Color(0xFFFFE8E4),
    onErrorContainer = Color(0xFF9C2A26)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF65E1C9),
    onPrimary = Color(0xFF093F3B),
    primaryContainer = Color(0xFF14554E),
    onPrimaryContainer = Color(0xFFBFF8E9),
    secondary = Color(0xFFA4C4D0),
    onSecondary = Color(0xFF233B46),
    secondaryContainer = Color(0xFF294453),
    onSecondaryContainer = Color(0xFFE1F3F7),
    tertiary = Color(0xFFFFA5AD),
    background = Color(0xFF0B1724),
    onBackground = Color(0xFFE9F2F6),
    surface = Color(0xFF142535),
    onSurface = Color(0xFFE9F2F6),
    surfaceVariant = Color(0xFF203747),
    onSurfaceVariant = Color(0xFFA5BAC7),
    outline = Color(0xFF668392),
    outlineVariant = Color(0xFF324A58),
    errorContainer = Color(0xFF672F35),
    onErrorContainer = Color(0xFFFFD5D4)
)

private val FxTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 39.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 33.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 29.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp)
)

@Composable
fun FubonFXMonitorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = FxTypography,
        content = content
    )
}
