package com.sweetapps.alcoholictimer.core.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.sweetapps.alcoholictimer.R
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.sweetapps.alcoholictimer.core.ui.theme.AlcoholicTimerTheme
import com.sweetapps.alcoholictimer.feature.level.LevelActivity
import com.sweetapps.alcoholictimer.feature.profile.NicknameEditActivity
import com.sweetapps.alcoholictimer.feature.run.RunActivity
import com.sweetapps.alcoholictimer.feature.settings.SettingsActivity
import com.sweetapps.alcoholictimer.feature.start.StartActivity
import kotlinx.coroutines.launch
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.RowScope

// 전역 입력 잠금 요청을 위한 CompositionLocal
val LocalRequestGlobalLock = compositionLocalOf<(Long) -> Unit> { { _: Long -> } }

// 스크롤 화면에서 하단 안전 패딩(내비/IME + 추가 여백)을 일관 적용하기 위한 CompositionLocal
val LocalSafeContentPadding = compositionLocalOf { PaddingValues(bottom = 0.dp) }

abstract class BaseActivity : ComponentActivity() {
    private var nicknameState = mutableStateOf("")

    // Ensure declaration before first usage
    private fun getNickname(): String {
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        return sharedPref.getString("nickname", getString(R.string.default_nickname)) ?: getString(R.string.default_nickname)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 시스템바 색/아이콘은 XML 테마에서만 관리 (코드로 설정하지 않음)
        nicknameState.value = getNickname()

        // 디버그 배너 숨김 상태 초기화
        DebugAdHelper.initialize(this)
    }

    override fun onResume() {
        super.onResume()
        nicknameState.value = getNickname()
    }

    // Returns the drawer menu title that matches current screen, or null if none
    private fun currentDrawerSelection(): String? = when (this) {
        is RunActivity, is StartActivity,
        is com.sweetapps.alcoholictimer.feature.run.QuitActivity -> getString(R.string.drawer_menu_sobriety)
        is com.sweetapps.alcoholictimer.feature.records.RecordsActivity,
        is com.sweetapps.alcoholictimer.feature.records.AllRecordsActivity -> getString(R.string.drawer_menu_records)
        is LevelActivity -> getString(R.string.drawer_menu_level)
        is SettingsActivity -> getString(R.string.drawer_menu_settings)
        is com.sweetapps.alcoholictimer.feature.about.AboutActivity,
        is com.sweetapps.alcoholictimer.feature.about.AboutLicensesActivity -> getString(R.string.drawer_menu_about)
        else -> null
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun BaseScreen(
        applyBottomInsets: Boolean = true,
        applySystemBars: Boolean = true,
        showBackButton: Boolean = false,
        onBackClick: (() -> Unit)? = null,
        bottomExtra: Dp = 16.dp, // 스크롤 화면 공통 추가 여백(문서 기준 16dp)
        topBarActions: @Composable RowScope.() -> Unit = {},
        // 새로 추가: 하단 배너 광고 슬롯과 공간 예약 옵션
        bottomAd: (@Composable () -> Unit)? = null,
        reserveSpaceForBottomAd: Boolean = false,
        bannerTopGap: Dp = LayoutConstants.BANNER_TOP_GAP,
        // 추가: 하단 영역을 외부 레이아웃이 전담할 때 true로 설정(예: StandardScreenWithBottomButton 사용 화면)
        manageBottomAreaExternally: Boolean = false,
        content: @Composable () -> Unit
    ) {
        val activityName = this@BaseActivity.javaClass.simpleName
        android.util.Log.e("BaseActivity", "[$activityName] BaseScreen called - ENTRY POINT")

        // 디버그 모드에서 배너 숨김 상태 확인 (반응형)
        var shouldHideBanner by remember {
            mutableStateOf(DebugAdHelper.bannerHiddenFlow.value).also {
                android.util.Log.e("BaseActivity", "[$activityName] remember initial value: ${DebugAdHelper.bannerHiddenFlow.value}")
            }
        }

        // Flow 변경사항을 LaunchedEffect로 명시적으로 구독
        LaunchedEffect(Unit) {
            android.util.Log.e("BaseActivity", "[$activityName] LaunchedEffect started, collecting flow...")
            DebugAdHelper.bannerHiddenFlow.collect { hidden ->
                android.util.Log.e("BaseActivity", "[$activityName] Flow collected: hidden=$hidden")
                shouldHideBanner = hidden
            }
        }

        val effectiveBottomAd = if (shouldHideBanner) null else bottomAd

        // 매 recomposition마다 로깅 (SideEffect 사용)
        SideEffect {
            android.util.Log.e("BaseActivity", "[$activityName] SideEffect: shouldHideBanner=$shouldHideBanner, bottomAd=${bottomAd != null}, effectiveBottomAd=${effectiveBottomAd != null}")
        }

        AlcoholicTimerTheme(darkTheme = false, applySystemBars = applySystemBars) {
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            val currentNickname by remember { nicknameState }

            // 입력/키보드 컨트롤러
            val focusManager = LocalFocusManager.current
            val keyboardController = LocalSoftwareKeyboardController.current

            // 전역 입력 차단(설정 화면 제외)
            val enableGlobalOverlay = this !is SettingsActivity
            var globalInputLocked by remember { mutableStateOf(false) }
            var lockDurationMs by remember { mutableStateOf(250L) }
            var lockTick by remember { mutableIntStateOf(0) }

            val requestGlobalLock: (Long) -> Unit = remember(enableGlobalOverlay) {
                { duration ->
                    if (enableGlobalOverlay) {
                        globalInputLocked = true
                        lockDurationMs = duration
                        lockTick++
                    }
                }
            }

            LaunchedEffect(lockTick) {
                if (globalInputLocked) {
                    delay(lockDurationMs)
                    globalInputLocked = false
                }
            }

            val blurRadius by animateFloatAsState(
                targetValue = if (drawerState.targetValue == DrawerValue.Open) 8f else 0f,
                animationSpec = tween(durationMillis = 300),
                label = "blur"
            )

            // 드로어 입력 가드
            var drawerInputGuardActive by remember { mutableStateOf(false) }
            val drawerGuardGraceMs = 200L
            LaunchedEffect(drawerState) {
                snapshotFlow { Triple(drawerState.isAnimationRunning, drawerState.currentValue, drawerState.targetValue) }
                    .collect { (isAnimating, current, target) ->
                        if (isAnimating || target != DrawerValue.Closed || current != DrawerValue.Closed) {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            drawerInputGuardActive = true
                        } else {
                            drawerInputGuardActive = true
                            delay(drawerGuardGraceMs)
                            drawerInputGuardActive = false
                        }
                    }
            }

            // 시스템 내비/IME 하단 인셋 계산 + 공통 추가 여백
            val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
            val effectiveBottom = maxOf(navBottom, imeBottom)

            // 하단 배너가 있는 화면에선 콘텐츠 안전패딩에서 내비게이션 바 여백을 제거하고,
            // IME가 열릴 때만 IME 높이를 포함한다. (배너 컨테이너가 내비/IME 인셋은 따로 처리)
            val hasOrReservesAd = (bottomAd != null) || reserveSpaceForBottomAd
            val contentBottomInset = if (hasOrReservesAd) imeBottom else effectiveBottom
            // 배너가 있으면 추가 여백(bottomExtra)을 콘텐츠 쪽에 더하지 않는다
            val safeBottom = contentBottomInset + if (hasOrReservesAd) 0.dp else bottomExtra
            val providedSafePadding = PaddingValues(bottom = safeBottom)

            CompositionLocalProvider(
                LocalRequestGlobalLock provides requestGlobalLock,
                LocalSafeContentPadding provides providedSafePadding
            ) {
                // 상단 상태바 오버레이 제거 (decorFitsSystemWindows=true에서는 불필요)
                // val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                Box(modifier = Modifier.fillMaxSize()) {
                    // ModalNavigationDrawer 시작
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .statusBarsPadding()
                                    .navigationBarsPadding()
                                    .background(Color.White),
                                drawerContainerColor = Color.Transparent,
                                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                            ) {
                                DrawerMenu(
                                    nickname = currentNickname,
                                    selectedItem = currentDrawerSelection(),
                                    onNicknameClick = {
                                        // 전역 입력 잠금 요청
                                        requestGlobalLock(300)
                                        scope.launch {
                                            drawerState.close()
                                            var navigated = false
                                            snapshotFlow { drawerState.isAnimationRunning }
                                                .collect { isAnimating ->
                                                    if (!isAnimating && drawerState.currentValue == DrawerValue.Closed && !navigated) {
                                                        navigated = true
                                                        navigateToNicknameEdit()
                                                        return@collect
                                                    }
                                                }
                                        }
                                    },
                                    onItemSelected = { menuItem ->
                                        // 전역 입력 잠금 요청
                                        requestGlobalLock(300)
                                        scope.launch {
                                            drawerState.close()
                                            var navigated = false
                                            snapshotFlow { drawerState.isAnimationRunning }
                                                .collect { isAnimating ->
                                                    if (!isAnimating && drawerState.currentValue == DrawerValue.Closed && !navigated) {
                                                        navigated = true
                                                        handleMenuSelection(menuItem)
                                                        return@collect
                                                    }
                                                }
                                        }
                                    }
                                )
                            }
                        }
                    ) {
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
                                                    val titleText = getScreenTitleResId()?.let { stringResource(it) } ?: getScreenTitle()
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
                                                Surface(
                                                    modifier = Modifier.padding(8.dp).size(48.dp),
                                                    shape = CircleShape,
                                                    color = Color(0xFFF8F9FA),
                                                    shadowElevation = 2.dp
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            // 전역 입력 잠금 + 포커스/키보드 정리 후 드로어 동작
                                                            requestGlobalLock(300)
                                                            focusManager.clearFocus(force = true)
                                                            keyboardController?.hide()
                                                            if (showBackButton) {
                                                                onBackClick?.invoke() ?: run { this@BaseActivity.onBackPressedDispatcher.onBackPressed() }
                                                            } else {
                                                                scope.launch { drawerState.open() }
                                                            }
                                                        }
                                                    ) {
                                                        if (showBackButton) {
                                                            Icon(
                                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                                contentDescription = getString(R.string.cd_navigate_back),
                                                                tint = Color(0xFF2C3E50),
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                        } else {
                                                            Icon(
                                                                imageVector = Icons.Filled.Menu,
                                                                contentDescription = getString(R.string.cd_menu),
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
                                        HorizontalDivider(
                                            thickness = 1.5.dp,
                                            color = Color(0xFFE0E0E0)
                                        )
                                    }
                                }
                            },
                            contentWindowInsets = WindowInsets(0, 0, 0, 0)
                        ) { paddingValues ->
                            // Column 구성: 상단 컨텐츠 영역(가중치 1) + 하단 배너 컨테이너(옵션)
                            Column(modifier = Modifier.fillMaxSize()) {
                                // 배경 레이어
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
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

                                    // 전역 입력 차단 오버레이(설정 화면 제외) + 드로어 가드: 모든 포인터 입력 소비
                                    if ((enableGlobalOverlay && globalInputLocked) || drawerInputGuardActive) {
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .pointerInput(drawerInputGuardActive, globalInputLocked) {
                                                    while (true) {
                                                        awaitPointerEventScope {
                                                            val event = awaitPointerEvent()
                                                            event.changes.forEach { it.consume() }
                                                        }
                                                    }
                                                }
                                        )
                                    }
                                }

                                // 하단 고정 배너 컨테이너: 광고 미노출 시에도 공간 예약 옵션 제공
                                val showOrReserveAd = (effectiveBottomAd != null) || reserveSpaceForBottomAd
                                if (!manageBottomAreaExternally) {
                                    if (showOrReserveAd) {
                                        // 전역 배너 위 간격을 회색(surfaceVariant)으로 채워 구분감 부여
                                        if (bannerTopGap > 0.dp) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(bannerTopGap)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                            )
                                        }
                                        // 배너 상단 헤어라인
                                        HorizontalDivider(
                                            thickness = AppBorder.Hairline,
                                            color = Color(0xFFE0E0E0)
                                        )
                                        // 화면 폭 기준 Anchored Adaptive 배너 예상 높이
                                        val predictedBannerH = predictAnchoredBannerHeightDp()
                                        Surface(color = Color.White, shadowElevation = 0.dp, tonalElevation = 0.dp) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = effectiveBottom)
                                                    .height(predictedBannerH),
                                                contentAlignment = Alignment.Center
                                            ) { effectiveBottomAd?.invoke() }
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
        }
    }

    private fun handleMenuSelection(menuItem: String) {
        val sobrietyMenu = getString(R.string.drawer_menu_sobriety)
        val recordsMenu = getString(R.string.drawer_menu_records)
        val levelMenu = getString(R.string.drawer_menu_level)
        val settingsMenu = getString(R.string.drawer_menu_settings)
        val aboutMenu = getString(R.string.drawer_menu_about)

        when (menuItem) {
            sobrietyMenu -> {
                val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
                val startTime = sharedPref.getLong("start_time", 0L)
                if (startTime > 0) {
                    if (this !is RunActivity) navigateToActivity(RunActivity::class.java)
                } else {
                    if (this !is StartActivity) {
                        // 드로어 내비게이션: StartActivity 진입 시 스플래시 생략 플래그 전달(API<31)
                        val intent = Intent(this, StartActivity::class.java).apply {
                            putExtra("skip_splash", true)
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                        startActivity(intent)
                        @Suppress("DEPRECATION")
                        overridePendingTransition(0, 0)
                        // 현재 화면을 스택에서 제거
                        // finish()  // 원복: 여기서 종료하지 않음
                    }
                }
            }
            recordsMenu -> if (this !is com.sweetapps.alcoholictimer.feature.records.RecordsActivity) {
                navigateToActivity(com.sweetapps.alcoholictimer.feature.records.RecordsActivity::class.java)
            }
            levelMenu -> if (this !is LevelActivity) navigateToActivity(LevelActivity::class.java)
            settingsMenu -> if (this !is SettingsActivity) navigateToActivity(SettingsActivity::class.java)
            aboutMenu -> if (this !is com.sweetapps.alcoholictimer.feature.about.AboutActivity) {
                navigateToActivity(com.sweetapps.alcoholictimer.feature.about.AboutActivity::class.java)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass).apply {
            // singleTask 모드와 함께 사용하여 기존 인스턴스 재사용 및 위의 Activity 제거
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
        // 현재 화면을 스택에서 제거하여 뒤로가기 스택 누적 방지
        // finish() // 원복: 호출자에 따라 종료 여부 결정
    }

    @Suppress("DEPRECATION")
    private fun navigateToNicknameEdit() {
        val intent = Intent(this, NicknameEditActivity::class.java)
        startActivity(intent)
        overridePendingTransition(0, 0)
        // finish() // 원복: 강제 종료하지 않음
    }

    /**
     * 메인 홈 화면으로 이동
     * - 금주 진행 중: RunActivity
     * - 금주 진행 전: StartActivity
     */
    @Suppress("DEPRECATION")
    protected fun navigateToMainHome() {
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        val startTime = sharedPref.getLong("start_time", 0L)
        val isRunning = startTime > 0

        val targetActivity = if (isRunning) RunActivity::class.java else StartActivity::class.java

        // 이미 메인 홈 화면이면 아무것도 하지 않음
        if ((isRunning && this is RunActivity) || (!isRunning && this is StartActivity)) {
            return
        }

        val intent = Intent(this, targetActivity).apply {
            // 모든 상위 Activity를 제거하고 메인 홈만 남김
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (targetActivity == StartActivity::class.java) {
                putExtra("skip_splash", true)
            }
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    protected open fun getScreenTitleResId(): Int? = null

    @Deprecated("Use getScreenTitleResId() instead for proper localization support")
    protected abstract fun getScreenTitle(): String
}

@Composable
fun DrawerMenu(
    nickname: String,
    selectedItem: String?,
    onNicknameClick: () -> Unit,
    onItemSelected: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val menuItems = listOf(
        context.getString(R.string.drawer_menu_sobriety) to Icons.Filled.PlayArrow,
        context.getString(R.string.drawer_menu_records) to Icons.AutoMirrored.Filled.List,
        context.getString(R.string.drawer_menu_level) to Icons.Filled.Star
    )
    val settingsItems = listOf(
        context.getString(R.string.drawer_menu_settings) to Icons.Filled.Settings,
        context.getString(R.string.drawer_menu_about) to Icons.Filled.Info
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { onNicknameClick() },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp),
                    shadowElevation = 2.dp
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = context.getString(R.string.cd_avatar),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
                        Text(
                            text = nickname,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = context.getString(R.string.drawer_edit_profile),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            thickness = 1.dp
        )
        Text(
            text = context.getString(R.string.drawer_section_menu),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )
        menuItems.forEach { (title, icon) ->
            val isSelected = title == selectedItem
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onItemSelected(title) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) AppColors.SurfaceOverlaySoft else Color.Transparent
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxSize().padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            thickness = 1.dp
        )
        Text(
            text = context.getString(R.string.drawer_section_settings),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )
        settingsItems.forEach { (title, icon) ->
            val isSelected = title == selectedItem
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onItemSelected(title) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) AppColors.SurfaceOverlaySoft else Color.Transparent
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxSize().padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
