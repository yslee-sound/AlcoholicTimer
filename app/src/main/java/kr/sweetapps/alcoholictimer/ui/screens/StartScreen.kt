package kr.sweetapps.alcoholictimer.ui.screens

import android.app.Activity
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import java.util.Locale
import kr.sweetapps.alcoholictimer.core.ui.StandardScreenWithBottomButton
import kr.sweetapps.alcoholictimer.MainActivity
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ads.InterstitialAdManager
import kr.sweetapps.alcoholictimer.core.ui.AppBorder
import kr.sweetapps.alcoholictimer.core.ui.AppElevation
import kr.sweetapps.alcoholictimer.core.ui.MainActionButton
import androidx.core.content.edit
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

import kr.sweetapps.alcoholictimer.ads.AppOpenAdManager
import kr.sweetapps.alcoholictimer.analytics.AnalyticsManager

// 추가된 import (LazyRow 사용)
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues

private val START_CARD_TOP_INNER_PADDING: Dp = 50.dp
private val START_TITLE_TOP_MARGIN: Dp = 30.dp
private val START_TITLE_CARD_GAP: Dp = 20.dp
private val START_CARD_HORIZONTAL_PADDING: Dp = 15.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreen(
    gateNavigation: Boolean = false,
    onStart: ((Int) -> Unit)? = null,
    holdSplashState: MutableState<Boolean>? = null,
    onSplashFinished: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)

    var startTime by remember { mutableLongStateOf(sharedPref.getLong("start_time", 0L)) }
    var timerCompleted by remember { mutableStateOf(sharedPref.getBoolean("timer_completed", false)) }

    LaunchedEffect(Unit) {
        startTime = sharedPref.getLong("start_time", 0L)
        timerCompleted = sharedPref.getBoolean("timer_completed", false)
    }

    if (holdSplashState != null) {
        LaunchedEffect(key1 = holdSplashState) {
            val onLoaded = fun(): Unit {
                try {
                    val act = context as? Activity
                    Log.d("StartScreen", "AppOpen loaded listener invoked. loaded=${AppOpenAdManager.isLoaded()} holdSplash=${holdSplashState.value} activity=${act?.javaClass?.simpleName}")
                    if (act != null && holdSplashState.value && AppOpenAdManager.isLoaded()) {
                        val shown = AppOpenAdManager.showIfAvailable(act)
                        Log.d("StartScreen", "AppOpen showIfAvailable returned: $shown")
                    }
                } catch (t: Throwable) { kotlin.run { Log.w("StartScreen", "onAdLoaded handler failed: $t") } }
            }

            val onFinished = fun(): Unit {
                try {
                    Log.d("StartScreen", "AppOpen finished -> releasing splash")
                    holdSplashState.value = false
                } catch (t: Throwable) { kotlin.run { Log.w("StartScreen", "onAdFinished handler failed: $t") } }
            }

            val onLoadFailed = fun(): Unit {
                try {
                    Log.d("StartScreen", "AppOpen load failed -> releasing splash")
                    holdSplashState.value = false
                } catch (t: Throwable) { kotlin.run { Log.w("StartScreen", "onAdLoadFailed handler failed: $t") } }
            }

             AppOpenAdManager.addOnAdLoadedListener(onLoaded)
             AppOpenAdManager.addOnAdFinishedListener(onFinished)
             AppOpenAdManager.addOnAdLoadFailedListener(onLoadFailed)

             try {
                 Log.d("StartScreen", "AppOpen integration: holding splash and initializing listeners")
                 holdSplashState.value = true
                 // Hide banner while splash is held to avoid duplicate banner visible under splash
                 try { kr.sweetapps.alcoholictimer.ads.AdController.setBannerForceHidden(true) } catch (_: Throwable) {}

                 try {
                     AppOpenAdManager.preload(context.applicationContext)
                 } catch (t: Throwable) {
                     Log.w("StartScreen", "preload call failed: $t")
                 }

                 try {
                     val act = context as? Activity
                     if (act != null && AppOpenAdManager.isLoaded()) {
                         val shown = AppOpenAdManager.showIfAvailable(act)
                         Log.d("StartScreen", "Immediate showIfAvailable returned: $shown")
                     }
                 } catch (t: Throwable) { Log.w("StartScreen", "immediate showIfAvailable failed: $t") }

                 delay(8000L)
                 if (holdSplashState.value) {
                     Log.d("StartScreen", "Safety timeout reached -> releasing splash")
                     holdSplashState.value = false
                     try { kr.sweetapps.alcoholictimer.ads.AdController.setBannerForceHidden(false) } catch (_: Throwable) {}
                 }

             } catch (t: Throwable) {
                 Log.w("StartScreen", "AppOpen integration LaunchedEffect failed: $t")
                 holdSplashState.value = false
             } finally {
                // ensure banner is restored
                 try { kr.sweetapps.alcoholictimer.ads.AdController.setBannerForceHidden(false) } catch (_: Throwable) {}
                 try { AppOpenAdManager.removeOnAdLoadedListener(onLoaded) } catch (_: Throwable) {}
                 try { AppOpenAdManager.removeOnAdFinishedListener(onFinished) } catch (_: Throwable) {}
                 try { AppOpenAdManager.removeOnAdLoadFailedListener(onLoadFailed) } catch (_: Throwable) {}
             }
         }
     }

    if (!gateNavigation && startTime != 0L && !timerCompleted) {
        LaunchedEffect(Unit) {
            Log.d("StartScreen", "Immediate navigation path taken: startTime=$startTime timerCompleted=$timerCompleted onStart=${onStart!=null}")
            if (onStart != null) {
                Log.d("StartScreen", "Calling onStart() for in-app navigation")
                onStart(sharedPref.getFloat("target_days", 30f).toInt())
            } else {
                Log.d("StartScreen", "Starting MainActivity directly (run route)")
                val intent = Intent(context, MainActivity::class.java)
                intent.putExtra("route", "run")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(intent)
                (context as? Activity)?.finish()
            }
        }
        return
    }

    var targetDays by rememberSaveable { mutableIntStateOf(21) }

    // [NEW] 3, 2, 1 카운트다운 오버레이 표시 여부
    var showCountdown by remember { mutableStateOf(false) }
    var countdownNumber by remember { mutableIntStateOf(3) }

    val showSplashOverlay = holdSplashState != null && holdSplashState.value

    LaunchedEffect(showSplashOverlay) {
        Log.d("StartScreen", "LaunchedEffect showSplashOverlay changed: $showSplashOverlay")
        if (!showSplashOverlay && onSplashFinished != null) onSplashFinished()
        if (!showSplashOverlay) Log.d("StartScreen", "onSplashFinished invoked: ${onSplashFinished != null}")
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

    // [NEW] 카운트다운 로직 처리
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(showCountdown) {
        if (showCountdown) {
            // 키보드 숨기기
            try {
                focusManager.clearFocus()
                keyboardController?.hide()
            } catch (_: Exception) {}

            // 3초 카운트다운 (3 → 2 → 1)
            countdownNumber = 3
            delay(1000L)
            countdownNumber = 2
            delay(1000L)
            countdownNumber = 1
            delay(1000L)

            // 카운트다운 종료 후 타이머 시작 및 화면 전환
            try {
                val hadActiveGoal = sharedPref.getLong("start_time", 0L) > 0L
                AnalyticsManager.logTimerStart(
                    targetDays = targetDays,
                    hadActiveGoal = hadActiveGoal,
                    startTs = System.currentTimeMillis()
                )
            } catch (_: Throwable) {}

            val formatted = String.format(Locale.US, "%.6f", targetDays.toFloat()).toFloat()
            sharedPref.edit {
                putFloat("target_days", formatted)
                putLong("start_time", System.currentTimeMillis())
                putBoolean("timer_completed", false)
            }

            if (onStart != null) {
                onStart(targetDays)
            } else {
                val intent = Intent(context, MainActivity::class.java)
                intent.putExtra("route", "run")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(intent)
                (context as? Activity)?.finish()
            }

            InterstitialAdManager.preload(context.applicationContext)
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
                        selectedDays = targetDays,
                        onDaysSelected = { days ->
                            targetDays = days
                            // [NEW] 배지 선택 시 입력 필드도 업데이트
                            focusManager.clearFocus()
                            try { keyboardController?.hide() } catch (_: Exception) {}
                        }
                    )
                    Spacer(modifier = Modifier.height(START_TITLE_CARD_GAP))

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
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = 24.dp)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 24.dp)
                            ) {
                                Card(
                                     modifier = Modifier
                                         .width(120.dp)
                                         .height(80.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.color_bg_card_light)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD)
                                ) {
                                    val targetFocusRequester = remember { FocusRequester() }
                                    var targetText by remember { mutableStateOf(TextFieldValue(text = targetDays.toString(), selection = TextRange(targetDays.toString().length))) }

                                    // [NEW] targetDays가 외부에서 변경되면 TextField 업데이트
                                    LaunchedEffect(targetDays) {
                                        val newText = targetDays.toString()
                                        targetText = TextFieldValue(text = newText, selection = TextRange(newText.length))
                                    }

                                    val coroutineScope = rememberCoroutineScope()
                                    Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
                                        TextField(
                                            value = targetText,
                                            onValueChange = { newVal: TextFieldValue ->
                                                val filtered = newVal.text.filter { it.isDigit() }
                                                val truncated = filtered.take(4)
                                                targetText = TextFieldValue(text = truncated, selection = TextRange(truncated.length))
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(64.dp)
                                                .focusRequester(targetFocusRequester)
                                                .onFocusChanged { fs ->
                                                    if (fs.isFocused) {
                                                        val t = targetText.text
                                                        targetText = TextFieldValue(text = t, selection = TextRange(0, t.length))
                                                    } else {
                                                        val t = targetText.text
                                                        targetText = TextFieldValue(text = t, selection = TextRange(t.length))
                                                    }
                                                },
                                            textStyle = MaterialTheme.typography.headlineLarge.copy(color = colorResource(id = R.color.color_indicator_days), textAlign = TextAlign.Center),
                                            singleLine = true,
                                            readOnly = false,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                            keyboardActions = KeyboardActions(onDone = {
                                                val parsed = targetText.text.toIntOrNull() ?: targetDays
                                                targetDays = parsed.coerceIn(0, 999)
                                                targetText = TextFieldValue(text = targetDays.toString(), selection = TextRange(targetDays.toString().length))
                                                try { keyboardController?.hide() } catch (_: Exception) {}
                                                focusManager.clearFocus()
                                            }),
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                disabledContainerColor = Color.Transparent,
                                                errorContainerColor = Color.Transparent,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent,
                                                disabledIndicatorColor = Color.Transparent
                                            )
                                        )

                                        Box(modifier = Modifier.matchParentSize().clickable(
                                             indication = null,
                                             interactionSource = remember { MutableInteractionSource() }
                                         ) {
                                              Log.d("StartScreen", "display area clicked — selecting all and showing keyboard")
                                              val s = targetDays.toString()
                                              targetText = TextFieldValue(text = s, selection = TextRange(0, s.length))
                                              coroutineScope.launch {
                                                  try { targetFocusRequester.requestFocus() } catch (_: Exception) { Log.d("StartScreen","requestFocus failed") }
                                                  try { keyboardController?.show() } catch (_: Exception) { Log.d("StartScreen","keyboard show failed") }
                                                  try { delay(40L) } catch (_: Exception) {}
                                                  targetText = TextFieldValue(text = s, selection = TextRange(0, s.length))
                                              }
                                         }) {}
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
                }
            },
            bottomButton = {
                MainActionButton(
                    onClick = {
                        // [NEW] 카운트다운 오버레이 시작
                        showCountdown = true
                        countdownNumber = 3
                    }
                )
            },
            screenBackground = Color(0xFFEEEDE9),
            backgroundDecoration = {
                Box(
                    modifier = Modifier.matchParentSize().background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.88f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.12f)
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

        // [NEW] 3, 2, 1 카운트다운 오버레이
        AnimatedVisibility(
            visible = showCountdown,
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            CountdownOverlay(countdownNumber = countdownNumber)
        }
     }
 }

// [NEW] 기간 선택 배지를 포함한 타이틀바
@Composable
private fun AppBrandTitleBar(
    selectedDays: Int = 30,
    onDaysSelected: (Int) -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 로고
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

        // [NEW] 기간 선택 배지
        DurationBadgeRow(
            selectedDays = selectedDays,
            onDaysSelected = onDaysSelected
        )
    }
}

// [NEW] 기간 선택 배지 컴포넌트 (수정: 프리셋 변경 및 가로 스크롤)
@Composable
private fun DurationBadgeRow(
    selectedDays: Int,
    onDaysSelected: (Int) -> Unit
) {
    val presets = listOf(
        "3주 챌린지" to 21,
        "딱 하루만" to 1,
        "작심 3일" to 3,
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

// [NEW] 개별 배지 컴포넌트 (레이블 지원)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DurationBadge(
    label: String,
    days: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color(0xFF1A1A1A) else Color.White
    val textColor = if (isSelected) Color.White else Color(0xFF666666)
    val borderColor = if (isSelected) Color(0xFF1A1A1A) else Color(0xFFE0E0E0)

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

// [NEW] 3, 2, 1 카운트다운 오버레이 (전체 화면) - 스케일 애니메이션 포함
@Composable
private fun CountdownOverlay(countdownNumber: Int) {
    // [NEW] 숫자가 바뀔 때마다 스케일 애니메이션 초기화
    var animationTrigger by remember { mutableStateOf(0f) }

    LaunchedEffect(countdownNumber) {
        animationTrigger = 0f
        delay(50)
        animationTrigger = 1f
    }

    // [NEW] 숫자가 바뀔 때마다 스케일 애니메이션 (0.3 → 1.0)
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = animationTrigger,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "countdown_scale"
    )

    // [NEW] 숫자가 바뀔 때마다 투명도 애니메이션 (0.0 → 1.0)
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
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* 터치 무시 (클릭 방지) */ },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = countdownNumber.toString(),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 120.dp.value.sp,
                color = Color.White.copy(alpha = alpha),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer(
                scaleX = 0.3f + (scale * 0.7f), // 0.3 → 1.0 스케일
                scaleY = 0.3f + (scale * 0.7f)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StartScreenPreview() { StartScreen() }
