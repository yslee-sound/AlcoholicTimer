package kr.sweetapps.alcoholictimer.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 디자인 토큰: 간격, 사이즈, 컴포넌트 높이 등 공통 값들을 그룹화합니다.
 * LocalDimens를 통해 전체 앱에서 일관되게 사용합니다.
 */
@Suppress("unused")
data class Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp
)

@Suppress("unused")
data class Sizes(
    val icon: Dp = 24.dp,
    val iconLarge: Dp = 32.dp,
    val profileImage: Dp = 64.dp,
    val appBar: Dp = 56.dp,
    val bottomNavBar: Dp = 56.dp
)

@Suppress("unused")
data class Component(
    val buttonHeight: Dp = 56.dp,
    val navBarHeight: Dp = 56.dp,
    val listItemHeight: Dp = 56.dp
)

@Suppress("unused")
data class Padding(
    val large: Dp = 16.dp
)

@Suppress("unused")
data class DividerTokens(
    val lightColor: Color = Color(0xFFE0E0E0),
    val thin: Dp = 1.dp,
    val sectionThickness: Dp = 8.dp
)

@Suppress("unused")
data class Dimens(
    val spacing: Spacing = Spacing(),
    val sizes: Sizes = Sizes(),
    val component: Component = Component(),
    val padding: Padding = Padding(),
    val divider: DividerTokens = DividerTokens()
)

val LocalDimens = staticCompositionLocalOf { Dimens() }
