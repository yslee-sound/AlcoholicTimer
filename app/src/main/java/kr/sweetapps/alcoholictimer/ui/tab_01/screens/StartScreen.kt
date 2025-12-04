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
import kr.sweetapps.alcoholictimer.ui.main.MainActivity
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager
import kr.sweetapps.alcoholictimer.ui.theme.AppBorder
import kr.sweetapps.alcoholictimer.ui.theme.AppElevation
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

// Additional imports (LazyRow usage)
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues

private val START_CARD_TOP_INNER_PADDING: Dp = 50.dp
private val START_TITLE_TOP_MARGIN: Dp = 30.dp
private val START_TITLE_CARD_GAP: Dp = 15.dp
private val START_CARD_HORIZONTAL_PADDING: Dp = 20.dp

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
                // [NEW] Track ad display state
                var adShown = false
                var adLoadAttempted = false

                val onLoaded = fun(): Unit {
                    try {
                        val act = context as? Activity
                        Log.d("StartScreen", "AppOpen loaded listener invoked. loaded=${AppOpenAdManager.isLoaded()} holdSplash=${holdSplashState.value} activity=${act?.javaClass?.simpleName}")
                        if (act != null && holdSplashState.value && AppOpenAdManager.isLoaded()) {
                            val shown = AppOpenAdManager.showIfAvailable(act)
                            Log.d("StartScreen", "AppOpen showIfAvailable returned: $shown")
                            // [NEW] Set flag if ad was shown
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
                         // [NEW] Set flag if ad was shown
                         if (shown) {
                             adShown = true
                         }
                     }
                 } catch (t: Throwable) { Log.w("StartScreen", "immediate showIfAvailable failed: $t") }

                 // [NEW] UX improvement: Ignore timeout if ad is showing
                 // Use 4 second timeout (recommended by Google AdMob)
                 delay(4000L)
                 if (holdSplashState.value) {
                     // [NEW] Release splash only if ad was not shown
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

    // [NEW] Show 3, 2, 1 countdown overlay
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

    // [NEW] Countdown logic processing
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(showCountdown) {
        if (showCountdown) {
            // Hide keyboard
            try {
                focusManager.clearFocus()
                keyboardController?.hide()
            } catch (_: Exception) {}

            // 3 second countdown (3 -> 2 -> 1)
            countdownNumber = 3
            delay(1000L)
            countdownNumber = 2
            delay(1000L)
            countdownNumber = 1
            delay(1000L)

            // After countdown ends, start timer and navigate to screen
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

            // [NEW] Initialize TimerStateRepository (new timer start)
            try {
                kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.resetTimer()
                kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setStartTime(System.currentTimeMillis())
                kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setTimerActive(true)
                android.util.Log.d("StartScreen", "Timer started: $targetDays days, active: true")
            } catch (t: Throwable) {
                android.util.Log.e("StartScreen", "Timer state initialization failed", t)
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
                                modifier = Modifier.padding(bottom = 32.dp)
                            )

                            // [NEW] Clean Input Area with Perfect Center Alignment
                            val targetFocusRequester = remember { FocusRequester() }
                            var targetText by remember { mutableStateOf(TextFieldValue(text = targetDays.toString(), selection = TextRange(targetDays.toString().length))) }

                            // [NEW] Update TextField when targetDays changes externally (badge click)
                            LaunchedEffect(targetDays) {
                                val newText = targetDays.toString()
                                targetText = TextFieldValue(text = newText, selection = TextRange(newText.length))
                            }

                            // [NEW] Unit text style for balance and consistency
                            val unitTextStyle = MaterialTheme.typography.headlineSmall.copy(
                                color = Color.Gray,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) {
                                            // [FIX] Improved interaction with timing
                                            coroutineScope.launch {
                                                try {
                                                    targetFocusRequester.requestFocus()
                                                    keyboardController?.show()
                                                    // [FIX] Short delay to prevent selection reset
                                                    delay(50)
                                                    targetText = targetText.copy(
                                                        selection = TextRange(0, targetText.text.length)
                                                    )
                                                } catch (_: Exception) {}
                                            }
                                        }
                                ) {
                                    // [NEW] Left Balance - Transparent unit text for perfect centering
                                    Text(
                                        text = stringResource(R.string.target_days_unit),
                                        style = unitTextStyle.copy(color = Color.Transparent),
                                        modifier = Modifier.alignByBaseline()
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // [NEW] Center - Number input with baseline alignment
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = targetText,
                                        onValueChange = { newValue ->
                                            // Filter: digits only, max 4 chars
                                            val filtered = newValue.text.filter { it.isDigit() }.take(4)
                                            if (filtered != targetText.text) {
                                                // Text changed - update normally
                                                targetText = TextFieldValue(
                                                    text = filtered,
                                                    selection = TextRange(filtered.length)
                                                )
                                                targetDays = filtered.toIntOrNull()?.coerceIn(1, 9999) ?: 21
                                            } else {
                                                // Text same - preserve selection
                                                targetText = newValue.copy(text = filtered)
                                            }
                                        },
                                        modifier = Modifier
                                            .width(IntrinsicSize.Min)
                                            .focusRequester(targetFocusRequester)
                                            .alignByBaseline()
                                            .onFocusChanged { focusState ->
                                                if (focusState.isFocused) {
                                                    // [FIX] Select all on focus
                                                    val text = targetText.text
                                                    targetText = TextFieldValue(
                                                        text = text,
                                                        selection = TextRange(0, text.length)
                                                    )
                                                }
                                            },
                                        textStyle = MaterialTheme.typography.displayLarge.copy(
                                            color = colorResource(id = R.color.color_indicator_days),
                                            textAlign = TextAlign.Center,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            fontSize = 72.sp
                                        ),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                val parsed = targetText.text.toIntOrNull() ?: 21
                                                targetDays = parsed.coerceIn(1, 9999)
                                                targetText = TextFieldValue(
                                                    text = targetDays.toString(),
                                                    selection = TextRange(targetDays.toString().length)
                                                )
                                                try { keyboardController?.hide() } catch (_: Exception) {}
                                                focusManager.clearFocus()
                                            }
                                        ),
                                        cursorBrush = androidx.compose.ui.graphics.SolidColor(colorResource(id = R.color.color_indicator_days)),
                                        decorationBox = { innerTextField ->
                                            Box(contentAlignment = Alignment.Center) {
                                                if (targetText.text.isEmpty()) {
                                                    Text(
                                                        text = "0",
                                                        style = MaterialTheme.typography.displayLarge.copy(
                                                            color = colorResource(id = R.color.color_indicator_days).copy(alpha = 0.3f),
                                                            textAlign = TextAlign.Center,
                                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                            fontSize = 72.sp
                                                        )
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        }
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // [NEW] Right - Actual unit text with baseline alignment
                                    Text(
                                        text = stringResource(R.string.target_days_unit),
                                        style = unitTextStyle,
                                        modifier = Modifier.alignByBaseline()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Bottom hint
                            Text(
                                text = stringResource(R.string.target_days_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorResource(id = R.color.color_hint_gray),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            },
            bottomButton = {
                MainActionButton(
                    onClick = {
                        // [NEW] AdPolicyManager checks interstitial ad policy
                        android.util.Log.d("StartScreen", "========================================")
                        android.util.Log.d("StartScreen", "Timer start button clicked - ad check started")
                        val shouldShowAd = kr.sweetapps.alcoholictimer.data.repository.AdPolicyManager.shouldShowInterstitialAd(context)
                        android.util.Log.d("StartScreen", "shouldShowInterstitialAd = $shouldShowAd")

                        if (shouldShowAd) {
                            // [NEW] Show interstitial ad then start countdown
                            val activity = context as? Activity
                            android.util.Log.d("StartScreen", "activity = ${activity != null}")

                            if (activity != null) {
                                val adLoaded = InterstitialAdManager.isLoaded()
                                android.util.Log.d("StartScreen", "InterstitialAdManager.isLoaded() = $adLoaded")

                                if (adLoaded) {
                                    android.util.Log.d("StartScreen", "✅ Showing interstitial ad")
                                    InterstitialAdManager.show(activity) { success ->
                                        android.util.Log.d("StartScreen", "Ad callback: success=$success")
                                        // Start countdown after ad closes or fails
                                        showCountdown = true
                                        countdownNumber = 3
                                    }
                                } else {
                                    // If ad not loaded, start countdown immediately
                                    android.util.Log.d("StartScreen", "Ad not loaded -> start countdown immediately")
                                    showCountdown = true
                                    countdownNumber = 3
                                }
                            } else {
                                android.util.Log.d("StartScreen", "activity null -> start countdown immediately")
                                showCountdown = true
                                countdownNumber = 3
                            }
                        } else {
                            // If in cooldown, skip ad and start countdown immediately
                            android.util.Log.d("StartScreen", "In cooldown -> skip ad and start countdown")
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

        // [NEW] 3, 2, 1 countdown overlay
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
            .background(Color.Black.copy(alpha = 0.85f))
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
                color = Color.White.copy(alpha = alpha),
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
