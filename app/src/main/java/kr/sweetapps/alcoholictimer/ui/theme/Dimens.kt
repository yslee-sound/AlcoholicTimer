package kr.sweetapps.alcoholictimer.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * App-wide design tokens (Light mode only)
 * - Elevation: Flat default (0) + Emphasis (2)
 *   ZERO: Completely flat (0dp)
 *   CARD (0dp): Default for regular cards/containers
 *   CARD_HIGH (2dp): Primary actions / High-attention circular buttons, etc.
 */
object AppAlphas {
    const val SurfaceTint: Float = 0.1f
}

object AppElevation {
    val ZERO = 0.dp
    val CARD = 1.dp
    val CARD_HIGH = 2.dp
}

/** Global border thickness standards */
object AppBorder {
    // Hairline border thickness following guidelines
    // Hairline: Thinnest border/line
    // Set to 0.75dp for subtle dividing lines
    val Hairline = 0.75.dp
}

/**
 * Soft gray background for highlighting selected items.
 * Uses a tone with higher brightness contrast to ensure visibility on white (#FFFFFF) backgrounds.
 */
object AppColors {
    // Previous: Color(0xFFFBFBFC) -> Almost white, poor visibility
    // Updated: Soft blue-gray tone for enhanced contrast
    val SurfaceOverlaySoft = Color(0xFFE9EEF5)
}

/**
 * Design tokens: Groups common values such as spacing, sizes, component heights, etc.
 * Used consistently throughout the app via LocalDimens.
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

/**
 * UI Constants (Migrated from constants/UiConstants.kt)
 * Contains layout-related constants for screens, cards, navigation, etc.
 */
@Suppress("unused")
object UiConstants {
    // Back button configuration
    val BackIconStartPadding: Dp = 56.dp // [FIX] 아이콘-제목 간격 최적화
    val BackIconTouchArea: Dp = 56.dp
    // Internal padding to adjust back icon visual position within touch area
    val BackIconInnerPadding: Dp = 14.dp

    // Layout-related constants (migrated from core.ui/LayoutConstants)
    val SCREEN_HORIZONTAL_PADDING: Dp = 15.dp           // Screen 1
    val FIRST_CARD_EXTERNAL_GAP: Dp = 15.dp             // Screen 1
    val CARD_VERTICAL_SPACING: Dp = 15.dp
    val STAT_ROW_SPACING: Dp = 12.dp
    val CARD_CORNER_RADIUS: Dp = 20.dp
    val CARD_PADDING: Dp = 20.dp

    val BANNER_TOP_GAP: Dp = 8.dp
    val BANNER_FIXED_HEIGHT: Dp = 64.dp
    val CLEARANCE_ABOVE_BUTTON: Dp = 32.dp
    val BUTTON_BOTTOM_OFFSET: Dp = 32.dp // [FIX] 하단 버튼 안전 여백 확대 (네비게이션 바와 충분한 간격 확보)

    // Bottom navigation sizes (used in BottomNavBar)
    val BOTTOM_NAV_ICON_SIZE: Dp = 32.dp // [UPDATE] Icon size: 28dp → 32dp (업계 표준 범위 상한)
    val BOTTOM_NAV_ITEM_SIZE: Dp = 40.dp // [UPDATE] Box size wrapping icon: 35dp → 40dp (아이콘 증가에 맞춰 조정)
    val BOTTOM_NAV_BAR_HEIGHT: Dp = 60.dp // Total bar height
    val BOTTOM_NAV_ITEM_GAP: Dp = 50.dp // Gap between items
}

@Suppress("unused")
data class Dimens(
    val spacing: Spacing = Spacing(),
    val sizes: Sizes = Sizes(),
    val component: Component = Component(),
    val padding: Padding = Padding(),
    val divider: DividerTokens = DividerTokens()
)

val LocalDimens = staticCompositionLocalOf { Dimens() }
