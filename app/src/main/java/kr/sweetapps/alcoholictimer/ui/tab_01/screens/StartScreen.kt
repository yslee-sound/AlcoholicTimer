// [NEW] Tab01 리팩?�링: StartScreen??tab_01/screens�??�동
package kr.sweetapps.alcoholictimer.ui.tab_01.screens

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
import kr.sweetapps.alcoholictimer.ui.tab_01.components.StandardScreenWithBottomButton
import kr.sweetapps.alcoholictimer.MainActivity
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager
import kr.sweetapps.alcoholictimer.core.ui.AppBorder
import kr.sweetapps.alcoholictimer.core.ui.AppElevation
import kr.sweetapps.alcoholictimer.ui.tab_01.components.MainActionButton
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle

import kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager
import kr.sweetapps.alcoholictimer.analytics.AnalyticsManager

// 추�???import (LazyRow ?�용)
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
            // [NEW] 광고 ?�시 ?�태 추적
            var adShown = false
            var adLoadAttempted = false

            val onLoaded = fun(): Unit {
                try {
                    val act = context as? Activity
                    Log.d("StartScreen", "AppOpen loaded listener invoked. loaded=${AppOpenAdManager.isLoaded()} holdSplash=${holdSplashState.value} activity=${act?.javaClass?.simpleName}")
                    if (act != null && holdSplashState.value && AppOpenAdManager.isLoaded()) {
                        val shown = AppOpenAdManager.showIfAvailable(act)
                        Log.d("StartScreen", "AppOpen showIfAvailable returned: $shown")
                        // [NEW] 광고가 ?�시?�면 ?�래�??�정
                        if (shown) {
                            adShown = true
                        }
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
                    Log.d("StartScreen", "AppOpen load failed -> releasing splash immediately")
                    adLoadAttempted = true
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
                 try { kr.sweetapps.alcoholictimer.ui.ad.AdController.setBannerForceHidden(true) } catch (_: Throwable) {}

                 try {
                     AppOpenAdManager.preload(context.applicationContext)
                     adLoadAttempted = true
                 } catch (t: Throwable) {
                     Log.w("StartScreen", "preload call failed: $t")
                     adLoadAttempted = true
                 }

                 try {
                     val act = context as? Activity
                     if (act != null && AppOpenAdManager.isLoaded()) {
                         val shown = AppOpenAdManager.showIfAvailable(act)
                         Log.d("StartScreen", "Immediate showIfAvailable returned: $shown")
                         // [NEW] 광고가 ?�시?�면 ?�래�??�정
                         if (shown) {
                             adShown = true
                         }
                     }
                 } catch (t: Throwable) { Log.w("StartScreen", "immediate showIfAvailable failed: $t") }

                 // [NEW] ?�?�아??개선: 광고가 ?�시 중이�??�?�아??무시
                 // ?�계 ?��? 4�??�용 (Google AdMob 권장)
                 delay(4000L)
                 if (holdSplashState.value) {
                     // [NEW] 광고가 ?�시?��? ?�았�??�플?�시가 ?�전???�성?�되???�으�??�제
                     if (!adShown) {
                         Log.d("StartScreen", "Safety timeout reached (no ad shown) -> releasing splash")
                         holdSplashState.value = false
                         try { kr.sweetapps.alcoholictimer.ui.ad.AdController.setBannerForceHidden(false) } catch (_: Throwable) {}
                     } else {
                         Log.d("StartScreen", "Safety timeout reached but ad is showing -> keep splash active")
                     }
                 }

             } catch (t: Throwable) {
                 Log.w("StartScreen", "AppOpen integration LaunchedEffect failed: $t")
                 holdSplashState.value = false
             } finally {
                // ensure banner is restored
                 try { kr.sweetapps.alcoholictimer.ui.ad.AdController.setBannerForceHidden(false) } catch (_: Throwable) {}
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

    // [NEW] 3, 2, 1 카운?�다???�버?�이 ?�시 ?��?
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

    // [NEW] 카운?�다??로직 처리
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(showCountdown) {
        if (showCountdown) {
            // ?�보???�기�?
            try {
                focusManager.clearFocus()
                keyboardController?.hide()
            } catch (_: Exception) {}

            // 3�?카운?�다??(3 ??2 ??1)
            countdownNumber = 3
            delay(1000L)
            countdownNumber = 2
            delay(1000L)
            countdownNumber = 1
            delay(1000L)

            // 카운?�다??종료 ???�?�머 ?�작 �??�면 ?�환
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

            // [NEW] TimerStateRepository 초기화 (새 타이머 시작)
            try {
                kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.resetTimer()
                kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setStartTime(System.currentTimeMillis())
                kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setTimerActive(true)
                android.util.Log.d("StartScreen", "타이머 시작: $targetDays 일, 작동 중: true")
            } catch (t: Throwable) {
                android.util.Log.e("StartScreen", "타이머 상태 초기화 실패", t)
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

    // Snackbar host for cross-screen transient messages (e.g., settings applied)
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    // If settings were applied via SettingsScreen, show a snackbar whenever the screen is resumed
    // or when this composable first appears. This uses a lifecycle observer to trigger the check
    // on ON_RESUME instead of polling, which is more reliable across navigation events.
    val lifecycleOwner = LocalLifecycleOwner.current

    suspend fun checkAndShowSnackbarOnce() {
         try {
            val pending = try { sharedPref.getBoolean("settings_applied_snackbar_pending", false) } catch (_: Throwable) { false }
            Log.d("StartScreen", "checkAndShowSnackbarOnce: pending=$pending")
            if (pending) {
                Log.d("StartScreen", "checkAndShowSnackbarOnce: clearing flag and showing snackbar")
                try { sharedPref.edit().putBoolean("settings_applied_snackbar_pending", false).apply() } catch (_: Throwable) {}
                try {
                    snackbarHostState.showSnackbar("설정이 반영되어 절약 금액이 업데이트되었습니다! 💰", duration = androidx.compose.material3.SnackbarDuration.Short)
                    Log.d("StartScreen", "checkAndShowSnackbarOnce: snackbar.showSnackbar returned")
                } catch (t: Throwable) {
                    Log.e("StartScreen", "checkAndShowSnackbarOnce: snackbar show failed", t)
                }
            } else {
                Log.d("StartScreen", "checkAndShowSnackbarOnce: no pending flag")
            }
         } catch (_: Throwable) {}
    }

    // initial check when composed
    LaunchedEffect(Unit) { checkAndShowSnackbarOnce() }

    // lifecycle observer to run check on ON_RESUME
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                try {
                    // use the composable's coroutine scope to run the suspend check
                    coroutineScope.launch { checkAndShowSnackbarOnce() }
                } catch (_: Throwable) {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
                            // [NEW] 배�? ?�택 ???�력 ?�드???�데?�트
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

                                    // [NEW] targetDays가 ?��??�서 변경되�?TextField ?�데?�트
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
                                              Log.d("StartScreen", "display area clicked ??selecting all and showing keyboard")
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
                        // [NEW] AdPolicyManager로 전면 광고 정책 확인
                        android.util.Log.d("StartScreen", "========================================")
                        android.util.Log.d("StartScreen", "타이머 시작 버튼 클릭 - 광고 체크 시작")
                        val shouldShowAd = kr.sweetapps.alcoholictimer.data.repository.AdPolicyManager.shouldShowInterstitialAd(context)
                        android.util.Log.d("StartScreen", "shouldShowInterstitialAd = $shouldShowAd")

                        if (shouldShowAd) {
                            // [NEW] 전면 광고 표시 후 카운트다운 시작
                            val activity = context as? Activity
                            android.util.Log.d("StartScreen", "activity = ${activity != null}")

                            if (activity != null) {
                                val adLoaded = InterstitialAdManager.isLoaded()
                                android.util.Log.d("StartScreen", "InterstitialAdManager.isLoaded() = $adLoaded")

                                if (adLoaded) {
                                    android.util.Log.d("StartScreen", "✅ 전면 광고 표시 시작")
                                    InterstitialAdManager.show(activity) { success ->
                                        android.util.Log.d("StartScreen", "광고 콜백: success=$success")
                                        // 광고가 닫히거나 실패하면 카운트다운 시작
                                        showCountdown = true
                                        countdownNumber = 3
                                    }
                                } else {
                                    // 광고가 로드되지 않았으면 즉시 카운트다운 시작
                                    android.util.Log.d("StartScreen", "광고 로드 안됨 -> 즉시 카운트다운 시작")
                                    showCountdown = true
                                    countdownNumber = 3
                                }
                            } else {
                                android.util.Log.d("StartScreen", "activity null -> 즉시 카운트다운 시작")
                                showCountdown = true
                                countdownNumber = 3
                            }
                        } else {
                            // 쿨타임 중이면 광고 없이 즉시 카운트다운 시작
                            android.util.Log.d("StartScreen", "쿨타임 중 -> 광고 스킵하고 카운트다운 시작")
                            showCountdown = true
                            countdownNumber = 3
                        }
                        android.util.Log.d("StartScreen", "========================================")
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

        // [NEW] 3, 2, 1 카운?�다???�버?�이
        AnimatedVisibility(
            visible = showCountdown,
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            CountdownOverlay(countdownNumber = countdownNumber)
        }

        // Snackbar Host overlay (bottom)
        androidx.compose.material3.SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp)
        )
     }
 }

// [NEW] 기간 ?�택 배�?�??�함???�?��?�?
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

        // [NEW] 기간 ?�택 배�?
        DurationBadgeRow(
            selectedDays = selectedDays,
            onDaysSelected = onDaysSelected
        )
    }
}

// [NEW] 기간 선택 배지 컴포넌트 (수정: 가로로 변경 + 가독성 증가)
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

// [NEW] 개별 배�? 컴포?�트 (?�이�?지??
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

// [NEW] 3, 2, 1 카운?�다???�버?�이 (?�체 ?�면) - ?��????�니메이???�함
@Composable
private fun CountdownOverlay(countdownNumber: Int) {
    // [NEW] ?�자가 바�??�마???��????�니메이??초기??
    var animationTrigger by remember { mutableStateOf(0f) }

    LaunchedEffect(countdownNumber) {
        animationTrigger = 0f
        delay(50)
        animationTrigger = 1f
    }

    // [NEW] ?�자가 바�??�마???��????�니메이??(0.3 ??1.0)
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = animationTrigger,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "countdown_scale"
    )

    // [NEW] ?�자가 바�??�마???�명???�니메이??(0.0 ??1.0)
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
            ) { /* ?�치 무시 (?�릭 방�?) */ },
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
                scaleX = 0.3f + (scale * 0.7f), // 0.3 ??1.0 ?��???
                scaleY = 0.3f + (scale * 0.7f)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StartScreenPreview() { StartScreen() }
