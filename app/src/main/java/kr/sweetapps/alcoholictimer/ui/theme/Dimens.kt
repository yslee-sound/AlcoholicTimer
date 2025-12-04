package kr.sweetapps.alcoholictimer.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
    val BackIconStartPadding: Dp = 42.dp // Shifts title to the right
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
    val BUTTON_BOTTOM_OFFSET: Dp = 24.dp

    // Bottom navigation sizes (used in BottomNavBar)
    val BOTTOM_NAV_ICON_SIZE: Dp = 28.dp // Icon size
    val BOTTOM_NAV_ITEM_SIZE: Dp = 35.dp // Box size wrapping icon
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
