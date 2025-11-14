@file:Suppress("KotlinConstantConditions")

package kr.sweetapps.alcoholictimer.core.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import kr.sweetapps.alcoholictimer.MainActivity
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.core.ui.theme.AlcoholicTimerTheme

// 전역 입력 잠금 요청을 위한 CompositionLocal
val LocalRequestGlobalLock = compositionLocalOf<(Long) -> Unit> { { _: Long -> } }

// 전역 안전 패딩(LocalSafeContentPadding) 제공
val LocalSafeContentPadding = compositionLocalOf { PaddingValues(bottom = 0.dp) }

abstract class BaseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBarAppearance()
    }

    override fun onResume() {
        super.onResume()
        applySystemBarAppearance()
        // Some OEM/system UIs or splash/ads can change system bars after onResume;
        // schedule a few delayed reapply attempts to override late changes.
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

    protected fun applySystemBarAppearance() {
        // Enable edge-to-edge: let the app draw behind system bars and control their look via Compose
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Clear translucent flags if any
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        // Make status bar transparent (app draws behind it) but force navigation bar to opaque white
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.WHITE
        // API 29+ navigation bar contrast enforcement can force a different color; disable it
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try { window.isNavigationBarContrastEnforced = false } catch (_: Throwable) {}
        }
        // API 28+ divider color can introduce a visible strip; make it transparent
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            try { window.navigationBarDividerColor = android.graphics.Color.TRANSPARENT } catch (_: Throwable) {}
        }

        // 주석 처리된 레거시 systemUiVisibility
        // window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        //     or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        // WindowInsetsControllerCompat만 사용하도록 정리
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true
        windowInsetsController.isAppearanceLightNavigationBars = true

        // Diagnostic logs
        try {
            android.util.Log.d("BaseActivity", "statusBarColor=#${Integer.toHexString(window.statusBarColor)} navBarColor=#${Integer.toHexString(window.navigationBarColor)} vis=${window.decorView.systemUiVisibility} isLightStatus=${windowInsetsController.isAppearanceLightStatusBars} isLightNav=${if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) windowInsetsController.isAppearanceLightNavigationBars else "N/A"}")
        } catch (t: Throwable) {
            android.util.Log.w("BaseActivity", "log system bar state failed: $t")
        }

        // Append diagnostic trace to app-private file for post-mortem analysis (debug-only, best-effort)
        try {
            val fn = "navbar-debug.txt"
            val msg = java.lang.String.format(
                "%d BaseActivity status=#%s nav=#%s vis=%d isLightStatus=%s isLightNav=%s\n",
                System.currentTimeMillis(),
                Integer.toHexString(window.statusBarColor),
                Integer.toHexString(window.navigationBarColor),
                window.decorView.systemUiVisibility,
                windowInsetsController.isAppearanceLightStatusBars.toString(),
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) windowInsetsController.isAppearanceLightNavigationBars.toString() else "N/A"
            )
            try {
                val f = java.io.File(filesDir, fn)
                f.appendText(msg)
            } catch (_: Throwable) { /* ignore file errors on restricted devices */ }
        } catch (_: Throwable) { }

        // Ensure window background under navigation bar is opaque white to avoid showing other content
        // (With edge-to-edge we draw app background to system bars; keep white background drawable as fallback)
        try {
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.WHITE))
        } catch (_: Throwable) {}
    }

    // Public helper to allow Application or other components to request a reapply
    // without exposing internal implementation details.
    fun reapplySystemBars() {
        try { applySystemBarAppearance() } catch (_: Throwable) { }
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
        bannerTopGap: Dp = LayoutConstants.BANNER_TOP_GAP,
        manageBottomAreaExternally: Boolean = false,
        content: @Composable () -> Unit
    ) {
        AlcoholicTimerTheme(darkTheme = false, applySystemBars = applySystemBars) {
            val blurRadius = animateFloatAsState(0f, tween(0), label = "blur").value

            // 하단 패딩 계산(내비/IME + 추가 여백)
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
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shadowElevation = 0.dp,
                            tonalElevation = 0.dp,
                            color = Color.White
                        ) {
                            Column {
                                TopAppBar(
                                    title = {
                                        CompositionLocalProvider(
                                            LocalDensity provides Density(LocalDensity.current.density, fontScale = 1.2f)
                                        ) {
                                            val titleText = getScreenTitleResId()?.let { stringResource(it) } ?: run {
                                                @Suppress("DEPRECATION") getScreenTitle()
                                            }
                                            Text(
                                                text = titleText,
                                                color = Color(0xFF2C3E50),
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = Color.Transparent,
                                        titleContentColor = Color(0xFF2C3E50),
                                        navigationIconContentColor = Color(0xFF2C3E50),
                                        actionIconContentColor = Color(0xFF2C3E50)
                                    ),
                                    navigationIcon = {
                                        if (showBackButton) {
                                            Surface(
                                                modifier = Modifier.padding(8.dp).size(48.dp),
                                                shape = CircleShape,
                                                color = Color(0xFFF8F9FA),
                                                shadowElevation = 2.dp
                                            ) {
                                                IconButton(onClick = { onBackClick?.invoke() ?: run { this@BaseActivity.onBackPressedDispatcher.onBackPressed() } }) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                        contentDescription = getString(R.string.cd_navigate_back),
                                                        tint = Color(0xFF2C3E50),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    actions = { topBarActions() }
                                )
                                // Global subtle divider under app bar
                                HorizontalDivider(thickness = 1.5.dp, color = Color(0xFFE0E0E0))
                            }
                        }
                    },
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { paddingValues ->
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 콘텐츠
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
                        // 하단 배너 영역(옵션)
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
                                // 배너 상단 헤어라인
                                HorizontalDivider(thickness = AppBorder.Hairline, color = Color(0xFFE0E0E0))
                                // 화면 폭 기준 Anchored Adaptive 배너 예상 높이
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

    /** 메인 홈 화면으로 이동 - Compose NavHost(MainActivity)로 통합 */
    @Suppress("DEPRECATION")
    protected fun navigateToMainHome() {
        if (this is MainActivity) return
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    protected open fun getScreenTitleResId(): Int? = null

    @Deprecated("Use getScreenTitleResId() instead for proper localization support")
    protected abstract fun getScreenTitle(): String
}
