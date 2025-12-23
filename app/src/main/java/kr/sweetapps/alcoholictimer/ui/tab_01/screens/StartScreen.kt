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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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
import kr.sweetapps.alcoholictimer.ui.tab_01.components.StandardScreenWithBottomButton
import kr.sweetapps.alcoholictimer.ui.theme.AppBorder
import kr.sweetapps.alcoholictimer.ui.theme.AppElevation
import kr.sweetapps.alcoholictimer.ui.theme.*
import kr.sweetapps.alcoholictimer.ui.tab_01.components.MainActionButton
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    // [REFACTORED] StandardScreenWithBottomButton 사용 - RunScreen과 동일한 구조
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
        StandardScreenWithBottomButton(
            backgroundDecoration = {
                // [배경 그라데이션]
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
            },
            screenBackground = Color.Transparent, // 루트 Box의 BackgroundCream 사용
            topPadding = START_TITLE_TOP_MARGIN,
            horizontalPadding = 0.dp, // 콘텐츠별로 개별 패딩 적용
            imePaddingEnabled = false, // [FIX] 키보드가 올라와도 버튼이 밀려 올라가지 않도록 비활성화
            ignoreImeInsets = true,
            topContent = {
                // 1. 로고 + 뱃지
                AppBrandTitleBar(
                    selectedDays = uiState.targetDays,
                    isCustomInputMode = uiState.isCustomInputMode,
                    onDaysSelected = { days ->
                        viewModel.onBadgeSelected(days)
                        focusManager.clearFocus()
                        try { keyboardController?.hide() } catch (_: Exception) {}
                    }
                )

                // 2. 타이틀바와 카드 사이 간격
                Spacer(modifier = Modifier.height(START_TITLE_CARD_GAP))

                // 3. 입력 카드
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
                            text = stringResource(R.string.target_days_title_set),
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

                // 4. 카드와 명언 사이 간격
                Spacer(modifier = Modifier.height(START_QUOTE_TOP_GAP))

                // 5. 명언
                QuoteDisplay()
            },
            bottomButton = {
                // [CRITICAL] 버튼 위치 관련 Modifier 모두 제거 - StandardScreenWithBottomButton이 처리
                val density = androidx.compose.ui.platform.LocalDensity.current
                val buttonSizePx = with(density) { 77.dp.toPx() }
                val buttonSize = with(density) { (buttonSizePx / density.density).dp }
                val iconSizePx = with(density) { 39.dp.toPx() }
                val iconSize = with(density) { (iconSizePx / density.density).dp }

                FloatingActionButton(
                    onClick = {
                        viewModel.onStartButtonClicked(context)
                    },
                    modifier = Modifier.requiredSize(buttonSize), // [FIX] 위치 관련 Modifier 제거
                    containerColor = colorResource(id = R.color.color_progress_primary),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.requiredSize(iconSize),
                        tint = Color.White
                    )
                }
            }
        )

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
                    painter = painterResource(id = R.drawable.splash_app_icon_inset),
                    contentDescription = null,
                    modifier = Modifier.size(240.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = uiState.showCountdown,
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            ) {
                CountdownOverlay(countdownNumber = uiState.countdownNumber)
            }
        }

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
    // [FIXED_SIZE] 폰트 스케일의 영향을 받지 않는 고정 크기 적용
    val density = androidx.compose.ui.platform.LocalDensity.current
    val logoHeightPx = with(density) { 54.dp.toPx() }
    val logoHeight = with(density) { (logoHeightPx / density.density).dp }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(logoHeight)
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
                    .requiredHeight(logoHeight)
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
        stringResource(R.string.badge_one_day) to 1,
        stringResource(R.string.badge_three_days) to 3,
        stringResource(R.string.badge_one_week) to 7,
        stringResource(R.string.badge_21_days) to 21,
        stringResource(R.string.badge_one_month) to 30,
        stringResource(R.string.badge_100_days) to 100
    )

    // [FIX] LazyRow 대신 Row + horizontalScroll로 변경하여 좁은 화면에서 스크롤 가능
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = START_CARD_HORIZONTAL_PADDING, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presets.forEach { item ->
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
    // [FIXED_SIZE] 폰트 스케일의 영향을 받지 않는 고정 크기 적용
    val density = androidx.compose.ui.platform.LocalDensity.current
    val badgeHeightPx = with(density) { 48.dp.toPx() } // [FIX] 40dp → 48dp (텍스트 잘림 방지)
    val badgeHeight = with(density) { (badgeHeightPx / density.density).dp }

    val backgroundColor = if (isSelected) BadgeSelectedBackground else BadgeUnselectedBackground
    val textColor = if (isSelected) BadgeSelectedText else BadgeUnselectedText
    val borderColor = if (isSelected) BadgeSelectedBorder else BadgeUnselectedBorder

    Surface(
        onClick = onClick,
        modifier = Modifier
            .requiredHeight(badgeHeight)
            .widthIn(min = 72.dp),
        shape = RoundedCornerShape(percent = 50), // [NEW] 알약 모양으로 변경 (양 끝이 완벽한 반원)
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        // [FIX] TextMeasurer 기반 사전 계산으로 텍스트 잘림 완전 해결
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp), // [FIX] 14dp → 8dp (공간 확보)
            contentAlignment = Alignment.Center
        ) {
            val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
            val maxPixels = with(density) { maxWidth.toPx() }

            val baseStyle = MaterialTheme.typography.bodyMedium.copy(
                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
            )

            // 사전 계산: 텍스트 너비가 maxWidth에 들어올 때까지 폰트 축소
            val calculatedSize = remember(label, maxPixels) {
                var currentSize = 12f // [수정] 시작 크기 14sp → 12sp (더 작게)
                val minSize = 8f // [수정] 최소 크기 10sp → 8sp (더 작게)

                while (currentSize > minSize) {
                    val result = textMeasurer.measure(
                        text = androidx.compose.ui.text.AnnotatedString(label),
                        style = baseStyle.copy(fontSize = currentSize.sp)
                    )
                    if (result.size.width <= maxPixels * 0.95f) { // 5% 여유
                        break
                    }
                    currentSize -= 1f // 1sp씩 정밀 축소
                }
                currentSize.coerceAtLeast(minSize).sp
            }

            Text(
                text = label,
                style = baseStyle.copy(fontSize = calculatedSize),
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Visible,
                modifier = Modifier.fillMaxWidth()
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

