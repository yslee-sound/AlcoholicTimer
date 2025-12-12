package kr.sweetapps.alcoholictimer.ui.tab_01.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.main.MainActivity
import kr.sweetapps.alcoholictimer.ui.tab_01.components.QuoteDisplay
import kr.sweetapps.alcoholictimer.ui.tab_01.components.TargetDaysInput
import kr.sweetapps.alcoholictimer.ui.theme.AppBorder
import kr.sweetapps.alcoholictimer.ui.theme.AppElevation
import kr.sweetapps.alcoholictimer.ui.theme.*
import kr.sweetapps.alcoholictimer.ui.tab_01.components.MainActionButton
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Additional imports (LazyRow usage)
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import kr.sweetapps.alcoholictimer.ui.tab_01.viewmodel.NavigationEvent
import kr.sweetapps.alcoholictimer.ui.tab_01.viewmodel.StartScreenViewModel

// [OPTIMIZED] Vertical Density Optimization for Small Screens
// 작은 화면에서도 모든 요소가 보이도록 여백을 최적화했습니다.
private val START_CARD_TOP_INNER_PADDING: Dp = 20.dp      // Card 내부 상단 여백 (32dp → 20dp, 37% 축소)
private val START_TITLE_TOP_MARGIN: Dp = 12.dp             // 화면 상단 여백
private val START_TITLE_CARD_GAP: Dp = 10.dp               // 타이틀바와 카드 간격
private val START_CARD_HORIZONTAL_PADDING: Dp = 20.dp
private val START_QUOTE_TOP_GAP: Dp = 12.dp                // 카드와 명언 사이 간격
private val START_CARD_TITLE_BOTTOM: Dp = 12.dp            // 카드 타이틀 하단 여백 (20dp → 12dp, 40% 축소)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreen(
    gateNavigation: Boolean = false,
    onStart: ((Int) -> Unit)? = null,
    holdSplashState: MutableState<Boolean>? = null,
    onSplashFinished: (() -> Unit)? = null,
    viewModel: StartScreenViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val navigationEvent by viewModel.navigationEvent.collectAsState()
    val snackbarEvent by viewModel.snackbarEvent.collectAsState()

    // [NEW] 카운트다운 중 뒤로 가기 방지 (실수로 끄는 것 방지)
    BackHandler(enabled = uiState.showCountdown) {
        // Do nothing to block back press during countdown
    }

    // [NEW] ViewModel에 gateNavigation 플래그 전달
    LaunchedEffect(gateNavigation) {
        viewModel.setGateNavigation(gateNavigation)
    }

    // [REMOVED] AppOpen Ad 초기화 - MainActivity에서 이미 처리하므로 중복 제거
    // MainActivity가 광고 로딩/표시를 완료하고 StartScreen에 진입함
    if (holdSplashState != null) {

        // [NEW] ViewModel의 splash 상태를 holdSplashState에 동기화
        LaunchedEffect(uiState.isSplashHeld) {
            holdSplashState.value = uiState.isSplashHeld
            if (!uiState.isSplashHeld) {
                try {
                    kr.sweetapps.alcoholictimer.ui.ad.AdController.setBannerForceHidden(false)
                } catch (_: Throwable) {}
            }
        }
    }

    // [NEW] 통합된 네비게이션 처리 (ViewModel → UI)
    // ViewModel에서 발행하는 모든 네비게이션 이벤트를 여기서 처리
    LaunchedEffect(navigationEvent) {
        navigationEvent?.let { event ->
            when (event) {
                is NavigationEvent.NavigateToRun -> {
                    Log.d("StartScreen", "Navigation event received: targetDays=${event.targetDays}")
                    handleNavigation(
                        context = context,
                        targetDays = event.targetDays,
                        onStart = onStart
                    )
                    viewModel.onNavigationHandled()
                }
            }
        }
    }

    // [NEW] Splash 상태 변경 감지
    val showSplashOverlay = holdSplashState != null && holdSplashState.value

    LaunchedEffect(showSplashOverlay) {
        Log.d("StartScreen", "showSplashOverlay changed: $showSplashOverlay")
        if (!showSplashOverlay) {
            onSplashFinished?.invoke()
            viewModel.onSplashReleased()
        }
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // [NEW] Snackbar 이벤트 처리 (ViewModel → UI)
    // [FIX] 이벤트 즉시 소비 패턴 적용 - 스낵바 무한 반복 버그 해결
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarEvent) {
        snackbarEvent?.let { message ->
            // 스낵바 표시는 독립적인 코루틴으로 실행
            launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
            // 이벤트를 즉시 소비하여 재진입 시 중복 표시 방지
            viewModel.onSnackbarShown()
        }
    }

    // [REFACTOR] StandardScreenWithBottomButton 제거 -> 전체 스크롤 Column으로 재구성
    // 키보드 올라올 때 UI 찌그러짐 방지를 위해 버튼을 하단 고정이 아닌 콘텐츠의 일부로 배치
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCream)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    try { keyboardController?.hide() } catch (_: Exception) {}
                })
            }
    ) {
        // [NEW] 배경 데코레이션 (그라데이션)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.88f to Color.Transparent,
                        1.0f to GradientBottomShadow.copy(alpha = 0.12f)
                    )
                )
        )

        // [NEW] 스크롤 가능한 메인 콘텐츠 (버튼 포함)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding() // [CRITICAL] 키보드 올라올 때 스크롤로 접근 가능하게
                .padding(horizontal = 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 상단 여백
            Spacer(modifier = Modifier.height(START_TITLE_TOP_MARGIN))

            // 2. 로고 + 뱃지
            AppBrandTitleBar(
                selectedDays = uiState.targetDays,
                isCustomInputMode = uiState.isCustomInputMode,
                onDaysSelected = { days ->
                    viewModel.onBadgeSelected(days)
                    focusManager.clearFocus()
                    try { keyboardController?.hide() } catch (_: Exception) {}
                }
            )

            // 3. 타이틀바와 카드 사이 간격
            Spacer(modifier = Modifier.height(START_TITLE_CARD_GAP))

            // 4. 입력 카드
            Card(
                modifier = Modifier
                    .padding(horizontal = START_CARD_HORIZONTAL_PADDING)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD_HIGH),
                border = BorderStroke(AppBorder.Hairline, colorResource(id = R.color.color_border_light))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = START_CARD_TOP_INNER_PADDING),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.target_days_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = colorResource(id = R.color.color_title_primary),
                        modifier = Modifier.padding(bottom = START_CARD_TITLE_BOTTOM)
                    )

                    TargetDaysInput(
                        value = uiState.targetDays,
                        onValueChange = { days ->
                            viewModel.onCustomInputChanged(days)
                        },
                        onDone = {
                            focusManager.clearFocus()
                            try { keyboardController?.hide() } catch (_: Exception) {}
                        }
                    )
                }
            }

            // 5. 카드와 명언 사이 간격
            Spacer(modifier = Modifier.height(START_QUOTE_TOP_GAP))

            // 6. 명언
            QuoteDisplay()

            // 7. 버튼 위 여백 - RunScreen과 동일한 높이 맞춤
            Spacer(modifier = Modifier.height(50.dp))  // [FIX] RunScreen의 CLEARANCE_ABOVE_BUTTON(32dp) + buttonSize/2(48dp) = 80dp 중 일부

            // 8. [MOVED] 시작 버튼 (이제 스크롤 콘텐츠의 일부 - 키보드에 가려짐)
            MainActionButton(
                onClick = {
                    viewModel.onStartButtonClicked(context)
                }
            )

            // 9. 하단 안전 여백 - RunScreen의 CLEARANCE_ABOVE_BUTTON과 동일
            Spacer(modifier = Modifier.height(32.dp))  // [FIX] RunScreen의 CLEARANCE_ABOVE_BUTTON = 32dp
        }

        AnimatedVisibility(
            visible = showSplashOverlay,
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {},
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.splash_app_icon),
                    contentDescription = null,
                    modifier = Modifier.size(240.dp)
                )
            }
        }

        // [NEW] 3, 2, 1 countdown overlay
        // [FIX] Dialog로 래핑하여 하단 탭바(BottomBar)까지 완전히 차단
        AnimatedVisibility(
            visible = uiState.showCountdown,
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            // Dialog를 사용하여 탭바를 포함한 모든 UI를 물리적으로 차단
            Dialog(
                onDismissRequest = { /* 차단: 바깥 터치/뒤로가기 무시 */ },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false, // 전체 화면 사용 (탭바까지 덮음)
                    dismissOnBackPress = false,      // 뒤로 가기 무시
                    dismissOnClickOutside = false    // 바깥 터치 무시
                )
            ) {
                CountdownOverlay(countdownNumber = uiState.countdownNumber)
            }
        }

        // Snackbar Host overlay (bottom)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp)
        )
     }
 }

// [NEW] Brand title bar with duration selection badges
@Composable
private fun AppBrandTitleBar(
    selectedDays: Int = 30,
    isCustomInputMode: Boolean = false, // [MANUAL OVERRIDE] 커스텀 입력 모드
    onDaysSelected: (Int) -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 30.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.alcoholic_timer_logo),
                contentDescription = stringResource(id = R.string.app_name),
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // [NEW] Duration selection badges
        DurationBadgeRow(
            selectedDays = selectedDays,
            isCustomInputMode = isCustomInputMode, // [MANUAL OVERRIDE] 커스텀 입력 모드 전달
            onDaysSelected = onDaysSelected
        )
    }
}

// [NEW] Duration selection badge component (horizontal layout + improved readability)
@Composable
private fun DurationBadgeRow(
    selectedDays: Int,
    isCustomInputMode: Boolean, // [MANUAL OVERRIDE] 커스텀 입력 모드
    onDaysSelected: (Int) -> Unit
) {
    val presets = listOf(
        "딱 하루만" to 1,
        "작심삼일" to 3,
        "1주 챌린지" to 7,
        "21일 습관 만들기" to 21,
        "한 달의 기적" to 30,
        "100일의 약속" to 100
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = START_CARD_HORIZONTAL_PADDING, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(presets) { item ->
            val label = item.first
            val days = item.second
            DurationBadge(
                label = label,
                days = days,
                // [MANUAL OVERRIDE] 커스텀 입력 모드일 때는 숫자가 같아도 뱃지를 선택하지 않음
                isSelected = !isCustomInputMode && selectedDays == days,
                onClick = { onDaysSelected(days) }
            )
        }
    }
}

// [NEW] Individual badge component (smaller size)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DurationBadge(
    label: String,
    days: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) BadgeSelectedBackground else BadgeUnselectedBackground
    val textColor = if (isSelected) BadgeSelectedText else BadgeUnselectedText
    val borderColor = if (isSelected) BadgeSelectedBorder else BadgeUnselectedBorder

    Surface(
        onClick = onClick,
        modifier = Modifier
            .height(40.dp)
            .widthIn(min = 72.dp),
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

// [NEW] 3, 2, 1 countdown overlay (fullscreen) - with bounce animation
@Composable
private fun CountdownOverlay(countdownNumber: Int) {
    // [NEW] Reset animation trigger each time number changes
    var animationTrigger by remember { mutableStateOf(0f) }

    LaunchedEffect(countdownNumber) {
        animationTrigger = 0f
        delay(50)
        animationTrigger = 1f
    }

    // [NEW] Pulse scale animation: 크게 시작(1.8) -> 작아짐(1.0)
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (animationTrigger == 0f) 1.8f else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ),
        label = "countdown_pulse_scale"
    )

    // [NEW] Fade animation: 숫자가 선명하게 나타남
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = animationTrigger,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 300,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "countdown_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CountdownOverlayBackground.copy(alpha = 0.10f)) // [FIX] 밝은 반투명 배경 (0.55 → 0.20)
            .pointerInput(Unit) {
                // [NEW] 모든 터치 이벤트를 여기서 소비하여 하위 UI로 전달 방지
                detectTapGestures { /* Do nothing - block all touches */ }
            }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* Ignore touch (prevent clicks) */ },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = countdownNumber.toString(),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 120.dp.value.sp,
                color = CountdownText.copy(alpha = alpha),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer(
                scaleX = scale,  // 1.8 → 1.0 (펄스 효과만)
                scaleY = scale
            )
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun StartScreenPreview() {
    MaterialTheme {
        StartScreen(
            gateNavigation = false,
            onStart = {},
            holdSplashState = null,
            onSplashFinished = null
        )
    }
}

/**
 * [NEW] 네비게이션 헬퍼 함수 (중복 제거)
 *
 * RunScreen으로 이동하는 로직을 통합 관리합니다.
 * - onStart 콜백이 있으면 콜백 호출 (앱 내 네비게이션)
 * - onStart 콜백이 없으면 MainActivity로 직접 이동 (딥링크 등)
 */
private fun handleNavigation(
    context: Context,
    targetDays: Int,
    onStart: ((Int) -> Unit)?
) {
    if (onStart != null) {
        Log.d("StartScreen", "Navigation: using onStart callback")
        onStart(targetDays)
    } else {
        Log.d("StartScreen", "Navigation: starting MainActivity directly")
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("route", "run")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
        (context as? Activity)?.finish()
    }
}

