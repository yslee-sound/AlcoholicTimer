package kr.sweetapps.alcoholictimer.feature.start

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.os.Build
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.core.ui.AppElevation
import kr.sweetapps.alcoholictimer.core.ui.AppBorder
import kr.sweetapps.alcoholictimer.core.ui.BaseActivity
import kr.sweetapps.alcoholictimer.core.ui.StandardScreenWithBottomButton
import kr.sweetapps.alcoholictimer.core.util.AppUpdateManager
import kr.sweetapps.alcoholictimer.core.util.Constants
import kr.sweetapps.alcoholictimer.feature.run.RunActivity
import com.google.android.play.core.appupdate.AppUpdateInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean
import androidx.compose.material3.SnackbarResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.draw.alpha
import androidx.core.graphics.drawable.toDrawable
import kr.sweetapps.alcoholictimer.feature.addrecord.components.TargetDaysBottomSheet
import kr.sweetapps.alcoholictimer.core.ads.InterstitialAdManager
import kr.sweetapps.alcoholictimer.core.ads.UmpConsentManager
import kr.sweetapps.alcoholictimer.core.ads.NativeAdManager
import kr.sweetapps.alcoholictimer.core.ui.AdmobBanner
import kr.sweetapps.alcoholictimer.core.ui.WatermarkTokens
import android.graphics.Color as AndroidColor

class StartActivity : BaseActivity() {
    private lateinit var appUpdateManager: AppUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // 첫 실행 시 기본 통화 초기화
        kr.sweetapps.alcoholictimer.core.util.CurrencyManager.initializeDefaultCurrency(this)

        // 런처 액티비티에서만 스플래시 설치
        val splashStart = SystemClock.uptimeMillis()
        val minShowMillis = 300L // DEBUG/RELEASE 모두 최소 300ms 표시로 단축

        val splash = if (Build.VERSION.SDK_INT >= 31) installSplashScreen() else null

        // Android 12+ 시스템 스플래시 종료 연출 및 유지 조건
        if (Build.VERSION.SDK_INT >= 31) {
            splash?.setKeepOnScreenCondition {
                (SystemClock.uptimeMillis() - splashStart) < minShowMillis
            }
            splash?.setOnExitAnimationListener { provider ->
                // 플랫폼 버그 회피: iconView 접근이 실패할 수 있으므로 안전하게 가져온 뒤 분기
                val icon = runCatching { provider.iconView }.getOrNull()
                if (icon != null) {
                    icon.animate()
                        .alpha(0f)
                        .setDuration(150)
                        .withEndAction {
                            runCatching { provider.remove() }
                                .onFailure { e -> android.util.Log.e("StartActivity", "Failed to remove splash provider: ${e.message}") }
                        }
                        .start()
                } else {
                    android.util.Log.w("StartActivity", "iconView unavailable, removing provider immediately")
                    runCatching { provider.remove() }
                        .onFailure { e -> android.util.Log.e("StartActivity", "Failed to remove splash provider: ${e.message}") }
                }
            }
        }

        super.onCreate(savedInstanceState)

        // DecorView 렌더링 에러 방지 (BackgroundFallback NullPointerException 회피)
        try {
            window.decorView.setWillNotDraw(false)
        } catch (e: Exception) {
            android.util.Log.w("StartActivity", "DecorView setup warning: ${e.message}")
        }

        // 스플래시에서 전면광고 로딩은 UMP 동의 플로우 완료 후로 지연
        UmpConsentManager.requestAndLoadIfRequired(this) { canRequest ->
            if (canRequest) {
                InterstitialAdManager.preload(this)
                NativeAdManager.preload(this) // 네이티브 광고도 미리 로드
            }
        }
        Constants.initializeUserSettings(this)
        Constants.ensureInstallMarkerAndResetIfReinstalled(this)

        // 금주 진행 중이면 즉시 RunActivity로 리다이렉트 (플레이스토어 "열기" 대응)
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        val startTime = sharedPref.getLong("start_time", 0L)
        if (startTime > 0) {
            // 금주 진행 중: RunActivity로 이동 후 현재 StartActivity 종료
            val intent = Intent(this, RunActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
            finish()
            return
        }

        // 드로어 내비게이션 시 스플래시 생략 플래그
        val skipSplash = intent.getBooleanExtra("skip_splash", false)

        // 시스템바는 XML 테마에 일임 (코드로 상태/네비 색상 및 아이콘을 설정하지 않음)

        // In-App Update 초기화
        appUpdateManager = AppUpdateManager(this)

        // 디버그 빌드 여부 (릴리스에서 데모 비활성화용)
        val isDebugBuild = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

        // 데모 모드 플래그: DEBUG에서만 인텐트 값 반영, RELEASE에서는 항상 false
        val demoUpdateUi = if (isDebugBuild) intent.getBooleanExtra("demo_update_ui", false) else false

        val launchContent = {
            // 남은 최소 오버레이 시간 계산 (API<31에서 setContent 지연 후엔 0일 수 있음)
            val elapsed = SystemClock.uptimeMillis() - splashStart
            val initialRemain = (minShowMillis - elapsed).coerceAtLeast(0L)
            // API 30 이하에서 오버레이는 비활성화하여 이중 스플래시 방지
            val usesComposeOverlay = false

            setContent {
                // 상단 시스템바 패딩은 적용, 하단은 개별 레이아웃에서 처리
                BaseScreen(applyBottomInsets = false, applySystemBars = true, manageBottomAreaExternally = true) {
                    StartScreenWithUpdate(
                        appUpdateManager,
                        demoMode = demoUpdateUi,
                        debugEnabled = isDebugBuild,
                        initialMinRemainMillis = if (skipSplash) 0L else initialRemain,
                        usesComposeOverlay = usesComposeOverlay,
                        onSplashFinished = {
                            // 배경을 제거하지 않음 - 워터마크 아이콘 유지
                        }
                    )
                }
            }

            // 배경 제거 로직을 주석 처리하여 워터마크 아이콘이 계속 표시되도록 함
            // window.decorView.post { runCatching { window.setBackgroundDrawable(null) } }
            // window.decorView.postDelayed({ runCatching { window.setBackgroundDrawable(null) } }, 300)
            // window.decorView.postDelayed({ runCatching { window.setBackgroundDrawable(null) } }, 1200)
            // window.decorView.postDelayed({ runCatching { window.setBackgroundDrawable(null) } }, 2400)
        }

        if (Build.VERSION.SDK_INT < 31) {
            // API 30 이하: 테마 스플래시 아이콘 → 즉시 화이트 배경으로 덮고 setContent, 첫 프레임 이후 배경 제거
            window.setBackgroundDrawable(AndroidColor.WHITE.toDrawable())
            launchContent()
            window.decorView.post { window.setBackgroundDrawable(null) }
        } else {
            // API 31 이상: 시스템 SplashScreen이 유지 조건으로 제어됨
            launchContent()
        }
    }

    override fun onResume() {
        super.onResume()
            // API 30 이하: 배경을 유지하여 워터마크 아이콘 표시
    }

    // 런처(singleTask) 재진입 시 스택 상단이 정리되어 Start가 전면에 오는 문제 대응
    // 진행 중 세션이면 즉시 RunActivity로 전환
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        val startTime = sharedPref.getLong("start_time", 0L)
        val timerCompleted = sharedPref.getBoolean("timer_completed", false)
        if (startTime > 0L && !timerCompleted) {
            startActivity(Intent(this, RunActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
            finish()
        }
    }

    override fun getScreenTitleResId(): Int = R.string.start_screen_title
    @Deprecated("Use getScreenTitleResId() instead")
    override fun getScreenTitle(): String = getString(R.string.start_screen_title)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StartScreenWithUpdate(
    appUpdateManager: AppUpdateManager,
    demoMode: Boolean = false,
    debugEnabled: Boolean = false,
    initialMinRemainMillis: Long = 0L,
    usesComposeOverlay: Boolean = true,
    onSplashFinished: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Composable 컨텍스트에서 미리 문자열 평가
    val restartPromptText = stringResource(R.string.update_downloaded_restart_prompt)
    val actionRestartText = stringResource(R.string.action_restart)

    // 최소 오버레이 유지 상태: 초기 남은 시간이 있으면 true로 시작, 남은 시간 후 false로 전환
    var keepMinOverlay by remember { mutableStateOf(initialMinRemainMillis > 0L) }
    LaunchedEffect(initialMinRemainMillis) {
        if (initialMinRemainMillis > 0L) {
            delay(initialMinRemainMillis)
            keepMinOverlay = false
        }
    }

    // 업데이트 다이얼로그/체크 상태
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(true) }

    // DEBUG에서만 데모 활성화
    val demoEnabled = debugEnabled
    val demoActive = demoEnabled && demoMode

    // 업데이트 정보/표시 버전명 상태 보관
    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var availableVersionName by remember { mutableStateOf("") }

    // 데모 트리거 함수 (UI만 시연)
    val triggerDemo: () -> Unit = {
        isCheckingUpdate = true
        scope.launch {
            delay(600)
            // 데모용 표시 버전명 세팅
            availableVersionName = "2.0.0"
            // 다이얼로그 표시
            showUpdateDialog = true
            isCheckingUpdate = false
        }
    }

    // 앱 시작 시 업데이트 확인 (데모 모드면 실제 체크 생략)
    LaunchedEffect(demoActive) {
        if (!demoActive) {
            scope.launch {
                appUpdateManager.checkForUpdate(
                    forceCheck = false,
                    onUpdateAvailable = { info ->
                        // 업데이트 사용 가능: 정보 보관 후 다이얼로그 표시
                        updateInfo = info
                        availableVersionName = "v${info.availableVersionCode()}"
                        showUpdateDialog = true
                        isCheckingUpdate = false
                    },
                    onNoUpdate = {
                        isCheckingUpdate = false
                    }
                )
            }
        } else {
            triggerDemo()
        }
    }

    // 업데이트 다운로드 완료 리스너
    LaunchedEffect(Unit) {
        appUpdateManager.registerInstallStateListener {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = restartPromptText,
                    actionLabel = actionRestartText,
                    duration = SnackbarDuration.Indefinite
                )
                if (result == SnackbarResult.ActionPerformed) {
                    appUpdateManager.completeFlexibleUpdate()
                }
            }
        }
    }
    DisposableEffect(Unit) { onDispose { appUpdateManager.unregisterInstallStateListener() } }

    // 업데이트 중/다이얼로그 표시 중에는 Run 화면으로의 자동 이동을 보류
    val gateNavigation = isCheckingUpdate || showUpdateDialog

    Box(modifier = Modifier.fillMaxSize()) {
        StartScreen(
            gateNavigation = gateNavigation,
            onDebugLongPress = if (demoEnabled) ({ triggerDemo() }) else null
        )

        // 스플래시 오버레이: 최소 유지 시간이 남았거나 업데이트 체크 중인 동안 표시 (다이얼로그 표시 시엔 숨김)
        val showSplashOverlay = usesComposeOverlay && (keepMinOverlay || isCheckingUpdate) && !showUpdateDialog

        // 오버레이가 사라지는 시점에 한 번 콜백 호출(창 배경 제거 등)
        LaunchedEffect(showSplashOverlay) {
            if (!showSplashOverlay) {
                onSplashFinished()
            }
        }

        AnimatedVisibility(
            visible = showSplashOverlay,
            enter = fadeIn(animationSpec = tween(durationMillis = 220)) + scaleIn(initialScale = 0.98f, animationSpec = tween(220)),
            exit = fadeOut(animationSpec = tween(durationMillis = 220)) + scaleOut(targetScale = 1.02f, animationSpec = tween(220))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.splash_app_icon),
                    contentDescription = null,
                    modifier = Modifier.size(240.dp)
                )
            }
        }

        // 스냅바
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))

        // 업데이트 다이얼로그 렌더링 (실사용/데모 공통)
        kr.sweetapps.alcoholictimer.core.ui.components.AppUpdateDialog(
            isVisible = showUpdateDialog,
            versionName = if (availableVersionName.isNotBlank()) availableVersionName else "vNext",
            updateMessageResourceId = R.string.update_dialog_default_message,
            onUpdateClick = {
                if (demoActive) {
                    // 데모: 다운로드 완료 스낵바를 직접 노출해 흐름 시연
                    showUpdateDialog = false
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = restartPromptText,
                            actionLabel = actionRestartText,
                            duration = SnackbarDuration.Indefinite
                        )
                    }
                } else {
                    // 실사용: Flexible Update 시작
                    updateInfo?.let { appUpdateManager.startFlexibleUpdate(it) }
                    showUpdateDialog = false
                }
            },
            onDismiss = {
                showUpdateDialog = false
                appUpdateManager.markUserPostpone()
            },
            canDismiss = !appUpdateManager.isMaxPostponeReached()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StartScreen(gateNavigation: Boolean = false, onDebugLongPress: (() -> Unit)? = null) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("user_settings", MODE_PRIVATE)

    // SharedPreferences 값을 State로 관리하여 변경 감지
    var startTime by remember { mutableLongStateOf(sharedPref.getLong("start_time", 0L)) }
    var timerCompleted by remember { mutableStateOf(sharedPref.getBoolean("timer_completed", false)) }

    // SharedPreferences 변경 감지 (Activity 재시작 시 최신 값 로드)
    LaunchedEffect(Unit) {
        startTime = sharedPref.getLong("start_time", 0L)
        timerCompleted = sharedPref.getBoolean("timer_completed", false)
    }

    // 진행 중 세션이 있고, 게이트가 내려가 있을 때만 Run 화면으로 이동
    // timer_completed가 true이거나 start_time이 0이면 이동하지 않음
    if (!gateNavigation && startTime != 0L && !timerCompleted) {
        LaunchedEffect(Unit) {
            context.startActivity(Intent(context, RunActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
        return
    }

    // 목표 일수(정수, 0..999), 기본값 30
    var targetDays by rememberSaveable { mutableIntStateOf(30) }
    val isValid by remember { derivedStateOf { targetDays > 0 } }
    var showDaysPicker by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        StandardScreenWithBottomButton(
            topContent = {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD), // down from CARD_HIGH
                    border = BorderStroke(AppBorder.Hairline, colorResource(id = R.color.color_border_light))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val baseTitleModifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 24.dp)
                        val titleModifier = if (onDebugLongPress != null) {
                            baseTitleModifier.combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onDebugLongPress() },
                                onLongClick = { onDebugLongPress() }
                            )
                        } else baseTitleModifier

                        Text(
                            text = stringResource(R.string.target_days_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = colorResource(id = R.color.color_title_primary),
                            modifier = titleModifier
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                        ) {
                            // 선택 박스(클릭 시 3자리 다이얼 바텀시트 표시, 롱프레스: 데모 업데이트 트리거)
                            Card(
                                modifier = Modifier.width(120.dp).height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.color_bg_card_light)),
                                elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                        .combinedClickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = { showDaysPicker = true },
                                            onLongClick = { onDebugLongPress?.invoke() }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = targetDays.toString(),
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = colorResource(id = R.color.color_indicator_days),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource(R.string.target_days_unit),
                                style = MaterialTheme.typography.titleLarge,
                                color = colorResource(id = R.color.color_indicator_label_gray)
                            )
                        }
                        Text(
                            text = stringResource(R.string.target_days_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorResource(id = R.color.color_hint_gray),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            },
            bottomButton = {
                val activity = context as? android.app.Activity
                Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                    ModernStartButton(
                        isEnabled = isValid,
                        onStart = {
                            // 상태를 먼저 기록
                            val formatted = String.format(Locale.US, "%.6f", targetDays.toFloat()).toFloat()
                            sharedPref.edit {
                                putFloat("target_days", formatted)
                                putLong("start_time", System.currentTimeMillis())
                                putBoolean("timer_completed", false)
                            }
                            val launchRun: () -> Unit = {
                                context.startActivity(Intent(context, RunActivity::class.java))
                                // 백스택 정리: StartActivity 종료
                                (context as? android.app.Activity)?.finish()
                            }
                            // 메인 화면의 “앞으로 진행” 제스처에서만 Interstitial 시도
                            val act = activity
                            if (act != null) {
                                if (InterstitialAdManager.isLoaded()) {
                                    val showed = InterstitialAdManager.maybeShowIfEligible(act) { launchRun() }
                                    if (!showed) launchRun()
                                } else {
                                    // 최초 클릭 시 로드 대기(최대 1.2초) 후 즉시 표시 시도
                                    val done = AtomicBoolean(false)
                                    val handler = Handler(Looper.getMainLooper())
                                    val timeoutMs = 1200L
                                    val timeout = Runnable {
                                        if (done.compareAndSet(false, true)) {
                                            launchRun()
                                        }
                                    }
                                    handler.postDelayed(timeout, timeoutMs)
                                    InterstitialAdManager.addLoadListener { success ->
                                        if (done.compareAndSet(false, true)) {
                                            handler.removeCallbacks(timeout)
                                            if (success) {
                                                val showed = InterstitialAdManager.maybeShowIfEligible(act) { launchRun() }
                                                if (!showed) launchRun()
                                            } else {
                                                launchRun()
                                            }
                                        }
                                    }
                                    // 로드를 트리거(중복 로드는 내부 가드)
                                    InterstitialAdManager.preload(act.applicationContext)
                                    // 레이스: 이미 로드가 끝났다면 즉시 성공 경로 수행
                                    if (InterstitialAdManager.isLoaded() && done.compareAndSet(false, true)) {
                                        handler.removeCallbacks(timeout)
                                        val showed = InterstitialAdManager.maybeShowIfEligible(act) { launchRun() }
                                        if (!showed) launchRun()
                                    }
                                }
                            } else {
                                launchRun()
                            }
                        }
                    )
                }
            },
            // 광고를 실제로 노출하여 다른 화면과 동일한 포맷 적용
            bottomAd = { AdmobBanner() },
            backgroundDecoration = {
                // 중앙 워터마크: 디버그/릴리스 모두 항상 표시, 표준 토큰 크기/투명도 적용
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.splash_app_icon),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(WatermarkTokens.IconSize)
                            .alpha(WatermarkTokens.IconAlpha)
                    )
                }
            }
        )

        // 3자리 다이얼 바텀시트
        if (showDaysPicker) {
            TargetDaysBottomSheet(
                initialValue = targetDays,
                onConfirm = { picked ->
                    targetDays = picked.coerceIn(0, 999)
                    showDaysPicker = false
                },
                onDismiss = { showDaysPicker = false }
            )
        }
    }
}

@Composable
fun ModernStartButton(isEnabled: Boolean, onStart: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = { if (isEnabled) onStart() },
        modifier = modifier.size(96.dp),
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) colorResource(id = R.color.color_progress_primary) else colorResource(id = R.color.color_button_disabled)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEnabled) AppElevation.CARD_HIGH else AppElevation.CARD)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.PlayArrow, contentDescription = "시작", tint = Color.White, modifier = Modifier.size(48.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StartScreenPreview() { StartScreen() }
