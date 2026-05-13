package com.tudominio.crianza.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalIsDark = compositionLocalOf { false }

object TemaPref {
    const val AUTO = "auto"
    const val CLARO = "claro"
    const val OSCURO = "oscuro"
    fun actual(ctx: Context): String =
        ctx.getSharedPreferences("crianza_prefs", Context.MODE_PRIVATE)
            .getString("tema", AUTO) ?: AUTO
    fun setTema(ctx: Context, t: String) {
        ctx.getSharedPreferences("crianza_prefs", Context.MODE_PRIVATE)
            .edit().putString("tema", t).apply()
    }
}

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
    background             = Color(0xFFF8F4EA),
    onBackground           = Color(0xFF2D2A1E),
    surface                = Color(0xFFF8F4EA),
    onSurface              = Color(0xFF2D2A1E),
    surfaceVariant         = Color(0xFFE8E0C8),
    onSurfaceVariant       = Color(0xFF5A5636),
    outline                = Color(0xFF8A8460),
    outlineVariant         = Color(0xFFD0C8A8),
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
    background             = Color(0xFF14130E),
    onBackground           = Color(0xFFEAE4D6),
    surface                = Color(0xFF1F1D14),
    onSurface              = Color(0xFFEAE4D6),
    surfaceVariant         = Color(0xFF302C20),
    onSurfaceVariant       = Color(0xFFC4BE9C),
    outline                = Color(0xFF8E886A),
    outlineVariant         = Color(0xFF4A4636),
    scrim                  = Color.Black,
)

@Composable
fun CrianzaTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val pref = TemaPref.actual(context)
    val sistemaEsDark = isSystemInDarkTheme()
    val darkTheme = when (pref) {
        TemaPref.OSCURO -> true
        TemaPref.CLARO -> false
        else -> sistemaEsDark
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalIsDark provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

