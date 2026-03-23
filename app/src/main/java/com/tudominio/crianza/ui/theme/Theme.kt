package com.tudominio.crianza.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary                = Indigo40,
    onPrimary              = Indigo100,
    primaryContainer       = Indigo90,
    onPrimaryContainer     = Indigo10,
    secondary              = Rose40,
    onSecondary            = Rose100,
    secondaryContainer     = Rose90,
    onSecondaryContainer   = Rose10,
    tertiary               = Teal40,
    onTertiary             = Teal100,
    tertiaryContainer      = Teal90,
    onTertiaryContainer    = Teal10,
    error                  = Red40,
    onError                = Color.White,
    errorContainer         = Red90,
    onErrorContainer       = Red10,
    background             = Neutral99,
    onBackground           = Neutral10,
    surface                = Neutral99,
    onSurface              = Neutral10,
    surfaceVariant         = NeutralVariant90,
    onSurfaceVariant       = NeutralVariant30,
    outline                = NeutralVariant50,
    outlineVariant         = NeutralVariant80,
    scrim                  = Color.Black,
)

private val DarkColorScheme = darkColorScheme(
    primary                = Indigo80,
    onPrimary              = Indigo20,
    primaryContainer       = Indigo30,
    onPrimaryContainer     = Indigo90,
    secondary              = Rose80,
    onSecondary            = Rose20,
    secondaryContainer     = Rose30,
    onSecondaryContainer   = Rose90,
    tertiary               = Teal80,
    onTertiary             = Teal20,
    tertiaryContainer      = Teal30,
    onTertiaryContainer    = Teal90,
    error                  = Red80,
    onError                = Red10,
    errorContainer         = Red40,
    onErrorContainer       = Red90,
    background             = Neutral10,
    onBackground           = Neutral90,
    surface                = Neutral10,
    onSurface              = Neutral90,
    surfaceVariant         = NeutralVariant30,
    onSurfaceVariant       = NeutralVariant80,
    outline                = NeutralVariant60,
    outlineVariant         = NeutralVariant30,
    scrim                  = Color.Black,
)

@Composable
fun CrianzaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
