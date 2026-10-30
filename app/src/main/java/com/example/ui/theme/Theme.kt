package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.AppThemeOption

private val ClassicWhiteColorScheme = lightColorScheme(
    primary = MonoBlackPrimary,
    onPrimary = MonoWhiteOnPrimary,
    primaryContainer = MonoLightGreyContainer,
    onPrimaryContainer = MonoBlackOnContainer,
    secondary = MonoDarkGreySecondary,
    onSecondary = MonoWhiteOnSecondary,
    secondaryContainer = MonoMidGreyContainer,
    onSecondaryContainer = MonoDarkOnSecondaryContainer,
    tertiary = MonoTextSecondary,
    tertiaryContainer = MonoSurfaceGrey,
    onTertiaryContainer = MonoTextPrimary,
    background = MonoBackground,
    onBackground = MonoTextPrimary,
    surface = MonoSurfaceWhite,
    onSurface = MonoTextPrimary,
    surfaceVariant = MonoSurfaceGrey,
    onSurfaceVariant = MonoTextSecondary,
    outline = MonoOutlineLight,
    error = Color(0xFF333333),
    onError = Color.White
)

private val ChocolateMilkshakeColorScheme = lightColorScheme(
    primary = MonoBlackPrimary,
    onPrimary = MonoWhiteOnPrimary,
    primaryContainer = ChocolateSurfaceVariant,
    onPrimaryContainer = ChocolateTextPrimary,
    secondary = MonoDarkGreySecondary,
    onSecondary = MonoWhiteOnSecondary,
    secondaryContainer = ChocolateSurfaceVariant,
    onSecondaryContainer = ChocolateTextPrimary,
    tertiary = ChocolateTextSecondary,
    tertiaryContainer = ChocolateSurfaceVariant,
    onTertiaryContainer = ChocolateTextPrimary,
    background = ChocolateBackground,
    onBackground = ChocolateTextPrimary,
    surface = ChocolateSurface,
    onSurface = ChocolateTextPrimary,
    surfaceVariant = ChocolateSurfaceVariant,
    onSurfaceVariant = ChocolateTextSecondary,
    outline = ChocolateOutline,
    error = Color(0xFF333333),
    onError = Color.White
)

private val BreadOmeletteColorScheme = lightColorScheme(
    primary = MonoBlackPrimary,
    onPrimary = MonoWhiteOnPrimary,
    primaryContainer = OmeletteSurfaceVariant,
    onPrimaryContainer = OmeletteTextPrimary,
    secondary = MonoDarkGreySecondary,
    onSecondary = MonoWhiteOnSecondary,
    secondaryContainer = OmeletteSurfaceVariant,
    onSecondaryContainer = OmeletteTextPrimary,
    tertiary = OmeletteTextSecondary,
    tertiaryContainer = OmeletteSurfaceVariant,
    onTertiaryContainer = OmeletteTextPrimary,
    background = OmeletteBackground,
    onBackground = OmeletteTextPrimary,
    surface = OmeletteSurface,
    onSurface = OmeletteTextPrimary,
    surfaceVariant = OmeletteSurfaceVariant,
    onSurfaceVariant = OmeletteTextSecondary,
    outline = OmeletteOutline,
    error = Color(0xFF333333),
    onError = Color.White
)

val RoundedAppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(12.dp)
)

@Composable
fun LastDropTheme(
    appTheme: AppThemeOption = AppThemeOption.CLASSIC_WHITE,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppThemeOption.CLASSIC_WHITE -> ClassicWhiteColorScheme
        AppThemeOption.CHOCOLATE_MILKSHAKE -> ChocolateMilkshakeColorScheme
        AppThemeOption.BREAD_OMELETTE -> BreadOmeletteColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = RoundedAppShapes,
        content = content
    )
}
