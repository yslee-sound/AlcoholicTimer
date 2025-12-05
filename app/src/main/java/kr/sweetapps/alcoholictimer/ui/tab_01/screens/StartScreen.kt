package kr.sweetapps.alcoholictimer.ui.tab_01.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
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
import kr.sweetapps.alcoholictimer.ui.tab_01.components.StandardScreenWithBottomButton
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
import kotlinx.coroutines.delay

// Additional imports (LazyRow usage)
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import kr.sweetapps.alcoholictimer.ui.tab_01.viewmodel.NavigationEvent
import kr.sweetapps.alcoholictimer.ui.tab_01.viewmodel.StartScreenViewModel

// [OPTIMIZED] Vertical Density Optimization for Small Screens
// 작은 화면에서도 모든 요소가 보이도록 여백을 최적화했습니다.
private val START_CARD_TOP_INNER_PADDING: Dp = 40.dp      // Card 내부 상단 여백 (50dp → 40dp)
private val START_TITLE_TOP_MARGIN: Dp = 12.dp             // 화면 상단 여백 (30dp → 12dp)
private val START_TITLE_CARD_GAP: Dp = 10.dp               // 타이틀바와 카드 간격 (15dp → 10dp)
private val START_CARD_HORIZONTAL_PADDING: Dp = 20.dp
private val START_QUOTE_TOP_GAP: Dp = 12.dp                // 카드와 명언 사이 간격 (24dp → 12dp)
private val START_BOTTOM_CLEARANCE: Dp = 100.dp            // 하단 버튼 가림 방지 여백

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

    // [NEW] ViewModel에 gateNavigation 플래그 전달
    LaunchedEffect(gateNavigation) {
        viewModel.setGateNavigation(gateNavigation)
    }

    // [NEW] AppOpen Ad 초기화 (ViewModel에 위임)
    if (holdSplashState != null) {
        LaunchedEffect(Unit) {
            viewModel.initializeAppOpenAd(context)
        }

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

    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val screenWidthDp: Dp = remember(config) {
        val widthPx = try {
            val wm = context.getSystemService(android.content.Context.WINDOW_SERVICE) as? android.view.WindowManager
            if (wm != null) {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        wm.currentWindowMetrics.bounds.width()
                    } else {
                        try { context.resources.displayMetrics.widthPixels } catch (_: Throwable) { 0 }
                    }
                } catch (_: Throwable) {
                    0
                }
            } else 0
        } catch (_: Throwable) {
            0
        }
        val d = density.density
        val fallbackPx = windowInfo.containerSize.width
        if (widthPx > 0) (widthPx / d).dp else if (fallbackPx > 0) (fallbackPx / d).dp else config.screenWidthDp.dp
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // [NEW] Snackbar 이벤트 처리 (ViewModel → UI)
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarEvent) {
        snackbarEvent?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.onSnackbarShown()
        }
    }

    Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
        detectTapGestures(onTap = {
            focusManager.clearFocus()
            try { keyboardController?.hide() } catch (_: Exception) {}
        })
    }) {
        StandardScreenWithBottomButton(
              topPadding = START_TITLE_TOP_MARGIN,
              horizontalPadding = 0.dp,
             ignoreImeInsets = true,
              contentMaxWidth = screenWidthDp,
              forceFillMaxWidth = true,
              topContent = {
                Column { 
                    AppBrandTitleBar(
                        selectedDays = uiState.targetDays,
                        onDaysSelected = { days ->
                            viewModel.onTargetDaysChanged(days)
                            // [NEW] Update input field when badge is selected
                            focusManager.clearFocus()
                            try { keyboardController?.hide() } catch (_: Exception) {}
                        }
                    )
                    Spacer(modifier = Modifier.height(START_TITLE_CARD_GAP))

                    // [NEW] Clean Box-less Design - Ghost Input Style
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
                            // Title
                            Text(
                                text = stringResource(R.string.target_days_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = colorResource(id = R.color.color_title_primary),
                                modifier = Modifier.padding(bottom = 24.dp) // [OPTIMIZED] 32dp → 24dp
                            )

                            // [NEW] Target Days Input Component (Extracted)
                            TargetDaysInput(
                                value = uiState.targetDays,
                                onValueChange = { days ->
                                    viewModel.onTargetDaysChanged(days)
                                },
                                onDone = {
                                    focusManager.clearFocus()
                                    try { keyboardController?.hide() } catch (_: Exception) {}
                                }
                            )
                        }
                    }

                    // [OPTIMIZED] 동기부여 명언 표시 - 간격 축소
                    Spacer(modifier = Modifier.height(START_QUOTE_TOP_GAP))
                    QuoteDisplay()

                    // [OPTIMIZED] 하단 버튼 가림 방지 여백
                    // 스크롤을 끝까지 내렸을 때 명언이 버튼 뒤에 숨지 않도록 충분한 공간 확보
                    Spacer(modifier = Modifier.height(START_BOTTOM_CLEARANCE))
                }
            },
            bottomButton = {
                MainActionButton(
                    onClick = {
                        viewModel.onStartButtonClicked(context)
                    }
                )
            },
            screenBackground = BackgroundCream,
            backgroundDecoration = {
                Box(
                    modifier = Modifier.matchParentSize().background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.88f to Color.Transparent,
                            1.0f to GradientBottomShadow.copy(alpha = 0.12f)
                        )
                    )
                )
            },
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
                    painter = painterResource(id = R.drawable.splash_app_icon),
                    contentDescription = null,
                    modifier = Modifier.size(240.dp)
                )
            }
        }

        // [NEW] 3, 2, 1 countdown overlay
        AnimatedVisibility(
            visible = uiState.showCountdown,
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            CountdownOverlay(countdownNumber = uiState.countdownNumber)
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
            onDaysSelected = onDaysSelected
        )
    }
}

// [NEW] Duration selection badge component (horizontal layout + improved readability)
@Composable
private fun DurationBadgeRow(
    selectedDays: Int,
    onDaysSelected: (Int) -> Unit
) {
    val presets = listOf(
        "3주 챌린지" to 21,
        "딱 하루만" to 1,
        "진심 3일" to 3,
        "6개월" to 180,
        "묻지도 말고 1년" to 365
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(presets) { item ->
            val label = item.first
            val days = item.second
            DurationBadge(
                label = label,
                days = days,
                isSelected = selectedDays == days,
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
    // [NEW] Reset bounce animation each time number changes
    var animationTrigger by remember { mutableStateOf(0f) }

    LaunchedEffect(countdownNumber) {
        animationTrigger = 0f
        delay(50)
        animationTrigger = 1f
    }

    // [NEW] Bounce animation when number changes (0.3 to 1.0)
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = animationTrigger,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "countdown_scale"
    )

    // [NEW] Fade animation when number changes (0.0 to 1.0)
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = animationTrigger,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 400,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "countdown_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CountdownOverlayBackground.copy(alpha = 0.85f))
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
                scaleX = 0.3f + (scale * 0.7f), // Scale from 0.3 to 1.0
                scaleY = 0.3f + (scale * 0.7f)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StartScreenPreview() { StartScreen() }

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

