package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkNavyColorScheme = darkColorScheme(
    primary = BrightPurple,
    onPrimary = Color.White,
    primaryContainer = ElectricPurple,
    onPrimaryContainer = Color.White,
    secondary = SubtleGold,
    onSecondary = DeepNavy,
    secondaryContainer = Color(0xFF33280B),
    onSecondaryContainer = BrightGold,
    tertiary = WhatsAppGreen,
    onTertiary = Color(0xFF03210E),
    tertiaryContainer = Color(0xFF0A3B22),
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = DeepNavy,
    onBackground = TextPrimaryDark,
    surface = NavySurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = NavyElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = GlassBorderDark,
    error = ErrorCoral,
    onError = Color(0xFF3B0909)
)

private val MidnightColorScheme = darkColorScheme(
    primary = BrightPurple,
    onPrimary = Color.White,
    primaryContainer = ElectricPurple,
    onPrimaryContainer = Color.White,
    secondary = SubtleGold,
    onSecondary = MidnightBlack,
    secondaryContainer = Color(0xFF2D2309),
    onSecondaryContainer = BrightGold,
    tertiary = WhatsAppGreen,
    onTertiary = Color(0xFF03210E),
    tertiaryContainer = Color(0xFF0A3B22),
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = MidnightBlack,
    onBackground = TextPrimaryDark,
    surface = MidnightSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF141B38),
    onSurfaceVariant = TextSecondaryDark,
    outline = GlassBorderDark,
    error = ErrorCoral,
    onError = Color(0xFF3B0909)
)

private val LightExecutiveColorScheme = lightColorScheme(
    primary = ElectricPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEBE4FF),
    onPrimaryContainer = Color(0xFF2B126B),
    secondary = DeepGold,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF3CC),
    onSecondaryContainer = Color(0xFF3D2E00),
    tertiary = WhatsAppDark,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF064E3B),
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
    onError = Color.White
)

val RajeevShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun RajeevDigitalTheme(
    themeMode: String = "DARK_NAVY",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val colorScheme = when (themeMode) {
        "MIDNIGHT" -> MidnightColorScheme
        "SYSTEM" -> if (systemDark) DarkNavyColorScheme else LightExecutiveColorScheme
        else -> DarkNavyColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RajeevTypography,
        shapes = RajeevShapes,
        content = content
    )
}
