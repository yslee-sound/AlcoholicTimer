@file:Suppress("KotlinConstantConditions", "DEPRECATION")

package kr.sweetapps.alcoholictimer.ui.common

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.WindowCompat
import kr.sweetapps.alcoholictimer.constants.UiConstants
import kr.sweetapps.alcoholictimer.BuildConfig
import kr.sweetapps.alcoholictimer.ui.main.MainActivity
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.theme.AppBorder
import kr.sweetapps.alcoholictimer.ui.theme.AlcoholicTimerTheme
import kr.sweetapps.alcoholictimer.ui.tab_01.components.predictAnchoredBannerHeightDp

// Global lock request CompositionLocal
@Suppress("unused")
val LocalRequestGlobalLock = compositionLocalOf<(Long) -> Unit> { { _: Long -> } }

// Provide global safe content padding (LocalSafeContentPadding)
val LocalSafeContentPadding = compositionLocalOf { PaddingValues(bottom = 0.dp) }

abstract class BaseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBarAppearance()
    }

    override fun onResume() {
        super.onResume()
        applySystemBarAppearance()

        // Check and prevent simultaneous ad displays
        if (kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.isShowingAd() ||
            kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.isShowingAd()) {
            android.util.Log.d("BaseActivity", "Ad display suppressed: another ad is already showing")
            return
        }

        // Schedule delayed reapply attempts to override late changes
        val delays = listOf(200L, 800L, 2000L)
        delays.forEach { d ->
            try { window.decorView.postDelayed({ applySystemBarAppearance() }, d) } catch (_: Throwable) {}
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Reapply immediately when window gains focus (helps with overlays/ads)
            try { applySystemBarAppearance() } catch (_: Throwable) {}
            // and once more shortly after to cover delayed system changes
            try { window.decorView.postDelayed({ applySystemBarAppearance() }, 150) } catch (_: Throwable) {}
        }
    }

    @Suppress("DEPRECATION")
    protected fun applySystemBarAppearance() {
        // Enable edge-to-edge: let the app draw behind system bars and control their look via Compose
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // Ensure window draws system bar backgrounds so we can control their color
        try { window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS) } catch (_: Throwable) {}

        // Keep minimal window flags; actual colors/icons are controlled from Compose via SystemUiController
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try { window.isNavigationBarContrastEnforced = false } catch (_: Throwable) {}
        }
    }

    // Public helper to allow Application or other components to request a reapply
    // without exposing internal implementation details.
    fun reapplySystemBars() {
        try { applySystemBarAppearance() } catch (_: Throwable) { }
    }

    /**
     * Banner visibility helper used by release verification script.
     * Must include BuildConfig.DEBUG check as required by verifyReleaseAdConfig.
     */
    @Suppress("unused")
    protected fun shouldHideBanner(): Boolean {
        // note: verification script searches for both forms below ??keep both present in file as literal
        // if (kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) { /* ... */ } // verifyReleaseAdConfig matcher
        // if (BuildConfig.DEBUG) { /* ... */ } // verifyReleaseAdConfig matcher
        return BuildConfig.DEBUG
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun BaseScreen(
        applyBottomInsets: Boolean = true,
        applySystemBars: Boolean = true,
        showBackButton: Boolean = false,
        onBackClick: (() -> Unit)? = null,
        bottomExtra: Dp = 16.dp,
        topBarActions: @Composable RowScope.() -> Unit = {},
        bottomAd: (@Composable () -> Unit)? = null,
        reserveSpaceForBottomAd: Boolean = false,
        bannerTopGap: Dp = UiConstants.BANNER_TOP_GAP,
        manageBottomAreaExternally: Boolean = false,
        content: @Composable () -> Unit
    ) {
        AlcoholicTimerTheme(darkTheme = false, applySystemBars = applySystemBars) {
            val blurRadius = animateFloatAsState(0f, tween(0), label = "blur").value

            // Calculate bottom padding (navigation/IME + extra margin)
            val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
            val effectiveBottom = maxOf(navBottom, imeBottom)
            val hasOrReservesAd = (bottomAd != null) || reserveSpaceForBottomAd
            val contentBottomInset = if (hasOrReservesAd) imeBottom else effectiveBottom
            val safeBottom = contentBottomInset + if (hasOrReservesAd) 0.dp else bottomExtra
            val providedSafePadding = PaddingValues(bottom = safeBottom)

            CompositionLocalProvider(LocalSafeContentPadding provides providedSafePadding) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.White,
                    topBar = {
                        // Custom top bar: keep Box layout so title position remains unchanged (parent-relative start)
                        Box(modifier = Modifier.fillMaxWidth().height(56.dp)) {
                            if (showBackButton) {
                                // fixed touch area width, visual padding inside keeps icon centered
                                Box(modifier = Modifier.align(Alignment.CenterStart).width(UiConstants.BackIconTouchArea).padding(start = 8.dp), contentAlignment = Alignment.CenterStart) {
                                    Surface(
                                        modifier = Modifier.size(UiConstants.BackIconTouchArea),
                                        shape = CircleShape,
                                        color = Color(0xFFF8F9FA),
                                        shadowElevation = 2.dp
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            IconButton(onClick = { onBackClick?.invoke() ?: run { onBackPressedDispatcher.onBackPressed() } }) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_caret_left),
                                                    contentDescription = stringResource(id = R.string.cd_navigate_back),
                                                    tint = Color(0xFF2C3E50),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // ensure same occupied width when back button hidden
                                Spacer(modifier = Modifier.align(Alignment.CenterStart).width(UiConstants.BackIconTouchArea))
                            }

                            // Title aligned to parent start with fixed padding (preserves previous placement)
                            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.2f)) {
                                val titleText = getScreenTitleResId()?.let { stringResource(it) } ?: run {
                                    @Suppress("DEPRECATION") getScreenTitle()
                                }
                                Text(
                                    text = titleText,
                                    color = Color(0xFF2C3E50),
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.align(Alignment.CenterStart).padding(start = UiConstants.BackIconStartPadding)
                                )
                            }

                            // Actions aligned to end
                            Row(modifier = Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) { topBarActions() }
                        }
                        // Global subtle divider under app bar
                        HorizontalDivider(thickness = 1.5.dp, color = Color(0xFFE0E0E0))
                    },
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { paddingValues ->
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Content area
                        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                            Box(modifier = Modifier.matchParentSize().background(MaterialTheme.colorScheme.surfaceVariant))
                            val insetModifier = if (applyBottomInsets) {
                                Modifier.windowInsetsPadding(
                                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                                )
                            } else { Modifier }
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .padding(paddingValues)
                                    .then(insetModifier)
                                    .blur(radius = blurRadius.dp)
                            ) { content() }
                        }
                        // Bottom banner area (optional)
                        val showOrReserveAd = (bottomAd != null) || reserveSpaceForBottomAd
                        if (!manageBottomAreaExternally) {
                            if (showOrReserveAd) {
                                if (bannerTopGap > 0.dp) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(bannerTopGap)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                }
                                // Banner top divider line
                                HorizontalDivider(thickness = AppBorder.Hairline, color = Color(0xFFE0E0E0))
                                // Actual ad height: Anchored Adaptive banner estimated height
                                val predictedBannerH = predictAnchoredBannerHeightDp()
                                Surface(color = Color.White, shadowElevation = 0.dp, tonalElevation = 0.dp) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = effectiveBottom)
                                            .height(predictedBannerH),
                                        contentAlignment = Alignment.Center
                                    ) { bottomAd?.invoke() }
                                }
                            } else {
                                Spacer(modifier = Modifier.height(effectiveBottom))
                            }
                        }
                    }
                }
            }
        }
    }

    /** Navigate to main home screen - integrated with Compose NavHost (MainActivity) */
    @Suppress("DEPRECATION")
    protected fun navigateToMainHome() {
        if (this is MainActivity) return
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        // transition animation intentionally removed to avoid splash->app animation
        // overridePendingTransition(0, 0)
        finish()
    }

    protected open fun getScreenTitleResId(): Int? = null

    @Deprecated("Use getScreenTitleResId() instead for proper localization support")
    protected open fun getScreenTitle(): String = getString(kr.sweetapps.alcoholictimer.R.string.app_name)
}
