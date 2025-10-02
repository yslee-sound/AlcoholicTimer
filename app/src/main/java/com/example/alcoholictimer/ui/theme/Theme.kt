package com.example.alcoholictimer.ui.theme

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
import androidx.core.view.WindowInsetsControllerCompat

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
    // Dynamic color is available on Android 12+, but we turn it off by default for consistent tone
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )

    if (applySystemBars) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val activity = view.context as? Activity ?: return@SideEffect
                val window = activity.window
                val surface = colorScheme.surface
                val useDarkIcons = surface.luminance() > 0.5f

                // Status bar & Navigation bar 배경색
                window.statusBarColor = surface.toArgb()
                window.navigationBarColor = surface.toArgb()

                // 대비 강제/디바이더 색 제거로 색 틴트 방지
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isStatusBarContrastEnforced = false
                    window.isNavigationBarContrastEnforced = false
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    window.navigationBarDividerColor = Color.Transparent.toArgb()
                }

                // 아이콘 톤(어두운/밝은) 적용
                val controller = WindowInsetsControllerCompat(window, view)
                controller.isAppearanceLightStatusBars = useDarkIcons
                controller.isAppearanceLightNavigationBars = useDarkIcons
            }
        }
    }
}