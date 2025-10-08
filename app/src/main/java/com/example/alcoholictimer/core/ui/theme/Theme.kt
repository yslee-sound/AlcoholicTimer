package com.example.alcoholictimer.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity // added

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimaryDark,
    onPrimary = Color.Black,
    primaryContainer = BluePrimaryDark,
    secondary = AmberSecondaryDark,
    onSecondary = Color.Black,
    tertiary = GreenTertiaryDark,
    onTertiary = Color.Black,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    error = ErrorDark,
    onError = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimaryLight,
    onPrimary = Color.White,
    primaryContainer = BluePrimaryContainer,
    secondary = AmberSecondaryLight,
    onSecondary = Color.White,
    tertiary = GreenTertiaryLight,
    onTertiary = Color.White,
    surface = SurfaceLight,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariant,
    error = ErrorLight,
    onError = Color.White
)

@Composable
fun AlcoholicTimerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    applySystemBars: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)

    if (applySystemBars) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val activity = view.context as? Activity ?: return@SideEffect
                val window = activity.window
                val surface = colorScheme.surface
                val useDarkIcons = surface.luminance() > 0.5f

                WindowCompat.setDecorFitsSystemWindows(window, false)

                val surfaceArgb = surface.toArgb()
                // auto -> light 고정 (라이트 테마만 지원, 휴리스틱 제거하여 아이콘 톤 흔들림 방지)
                val statusBarStyle = SystemBarStyle.light(surfaceArgb, surfaceArgb)
                val navigationBarStyle = SystemBarStyle.light(surfaceArgb, surfaceArgb)
                (activity as? ComponentActivity)?.enableEdgeToEdge(
                    statusBarStyle = statusBarStyle,
                    navigationBarStyle = navigationBarStyle
                )

                val controller = WindowInsetsControllerCompat(window, view)
                controller.isAppearanceLightStatusBars = useDarkIcons
                controller.isAppearanceLightNavigationBars = useDarkIcons
            }
        }
    }
}
