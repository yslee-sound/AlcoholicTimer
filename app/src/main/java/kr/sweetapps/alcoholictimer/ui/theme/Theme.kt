package kr.sweetapps.alcoholictimer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.runtime.SideEffect
import kr.sweetapps.alcoholictimer.ui.theme.AppTypography

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
    background = SurfaceVariantLight,
    onBackground = OnSurface,
    surface = SurfaceLight,
    onSurface = OnSurface,
    surfaceVariant = Color.White,
    onSurfaceVariant = OnSurfaceVariant,
    error = ErrorLight,
    onError = Color.White,

    // ▼▼▼ [핵심 수정] 이 줄을 추가하세요! ▼▼▼
    // 그림자가 생겨도 배경에 색상을 섞지 않도록(투명하게) 설정합니다.
    surfaceTint = Color.Transparent
)

@Composable
fun AlcoholicTimerTheme(
    darkTheme: Boolean = false, // [FIX] 강제로 false 설정하여 항상 라이트 테마 사용
    dynamicColor: Boolean = false,
    applySystemBars: Boolean = true,
    content: @Composable () -> Unit
) {
    // [FIX] 항상 라이트 컬러 스킴 사용 (다크모드 비활성화)
    val colorScheme = LightColorScheme

    val dimens = Dimens()

    // Apply system bar colors when requested
    val sysUiController = rememberSystemUiController()
    SideEffect {
        if (applySystemBars) {
            // [FIX] 상태바를 흰색으로 설정 (실제 기기에서도 동일하게 표시)
            sysUiController.setStatusBarColor(color = Color.White, darkIcons = true)
            sysUiController.setNavigationBarColor(color = colorScheme.surface, darkIcons = !darkTheme)
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalDimens provides dimens
    ) {
        MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
    }
}
