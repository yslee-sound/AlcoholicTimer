package kr.sweetapps.alcoholictimer.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Context.MODE_PRIVATE
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import java.util.Locale
import kr.sweetapps.alcoholictimer.core.ui.StandardScreenWithBottomButton
import androidx.core.content.edit
import kr.sweetapps.alcoholictimer.MainActivity
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ads.InterstitialAdManager
import kr.sweetapps.alcoholictimer.core.ui.AppBorder
import kr.sweetapps.alcoholictimer.core.ui.AppElevation
import kr.sweetapps.alcoholictimer.core.ui.MainActionButton
import android.widget.EditText
import androidx.compose.ui.viewinterop.AndroidView

// Local layout constants for StartScreen only — tweak these to adjust spacing on this screen
private val START_CARD_TOP_INNER_PADDING: Dp = 50.dp    // 50
private val START_TITLE_TOP_MARGIN: Dp = 30.dp           // previously 1.dp
private val START_TITLE_CARD_GAP: Dp = 20.dp            // 12
private val START_CARD_HORIZONTAL_PADDING: Dp = 15.dp   // 16

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreen(
    gateNavigation: Boolean = false,
    onStart: (() -> Unit)? = null,
    holdSplashState: MutableState<Boolean>? = null,
    onSplashFinished: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("user_settings", MODE_PRIVATE)

    var startTime by remember { mutableLongStateOf(sharedPref.getLong("start_time", 0L)) }
    var timerCompleted by remember { mutableStateOf(sharedPref.getBoolean("timer_completed", false)) }

    LaunchedEffect(Unit) {
        startTime = sharedPref.getLong("start_time", 0L)
        timerCompleted = sharedPref.getBoolean("timer_completed", false)
    }

    if (!gateNavigation && startTime != 0L && !timerCompleted) {
        LaunchedEffect(Unit) {
            Log.d("StartScreen", "Immediate navigation path taken: startTime=$startTime timerCompleted=$timerCompleted onStart=${onStart!=null}")
            if (onStart != null) {
                // NavHost 내부 이동: Activity 종료 금지 (스플래시 재등장 방지)
                Log.d("StartScreen", "Calling onStart() for in-app navigation")
                onStart()
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

    var targetDays by rememberSaveable { mutableIntStateOf(30) }
    // inline editing state: true when the number inside the main card is editable
    var editingDays by remember { mutableStateOf(false) }
    // previous soft input mode to restore after IME is hidden
    val prevSoftInputMode = remember { mutableStateOf<Int?>(null) }
    // direct reference to hidden Android EditText for immediate IME control
    val hiddenEditRef = remember { mutableStateOf<EditText?>(null) }

    val showSplashOverlay = holdSplashState != null && holdSplashState.value

    // 스플래시 오버레이가 해제될 때(onSplashFinished)만 앱으로 진입
    LaunchedEffect(showSplashOverlay) {
        Log.d("StartScreen", "LaunchedEffect showSplashOverlay changed: $showSplashOverlay")
        if (!showSplashOverlay && onSplashFinished != null) onSplashFinished()
        if (!showSplashOverlay) Log.d("StartScreen", "onSplashFinished invoked: ${onSplashFinished != null}")
    }

    val config = LocalConfiguration.current
    val screenWidthDp = config.screenWidthDp.dp

    Box(modifier = Modifier.fillMaxSize()) {
        StandardScreenWithBottomButton(
              topPadding = START_TITLE_TOP_MARGIN,
              horizontalPadding = 0.dp,
             // Always ignore IME insets on StartScreen so keyboard doesn't push the bottom button
             ignoreImeInsets = true,
              contentMaxWidth = screenWidthDp,
              forceFillMaxWidth = true,
              topContent = {
                Column { // 내부 전용 Column: maintain only local top margin
                    AppBrandTitleBar()
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
                                        .height(56.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.color_bg_card_light)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD)
                                ) {
                                    // Inline display only; clicking toggles editingDays which shows hidden EditText (no layout change)
                                    Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = targetDays.toString(),
                                            style = MaterialTheme.typography.headlineLarge,
                                            color = colorResource(id = R.color.color_indicator_days),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.clickable {
                                                Log.d("StartScreen", "targetDays clicked — set ADJUST_NOTHING & editingDays=true")
                                                try {
                                                    val act = context as? Activity
                                                    if (prevSoftInputMode.value == null) prevSoftInputMode.value = act?.window?.attributes?.softInputMode
                                                    try { act?.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING) } catch (_: Exception) {}
                                                } catch (_: Exception) {}
                                                editingDays = true
                                            }
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
                }
            },
            bottomButton = {
                MainActionButton(
                    onClick = {
                        val formatted = String.format(Locale.US, "%.6f", targetDays.toFloat()).toFloat()
                        sharedPref.edit {
                            putFloat("target_days", formatted)
                            putLong("start_time", System.currentTimeMillis())
                            putBoolean("timer_completed", false)
                        }
                        val launchRun: () -> Unit = {
                            if (onStart != null) {
                                onStart()
                            } else {
                                val intent = Intent(context, MainActivity::class.java)
                                intent.putExtra("route", "run")
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                context.startActivity(intent)
                                (context as? Activity)?.finish()
                            }
                        }
                        launchRun()
                        InterstitialAdManager.preload(context.applicationContext)
                    }
                )
            },
            // Use same base background color and overlay as Run screen
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
            // bottomAd = { AdmobBanner() } // moved to MainActivity BaseScaffold during Phase-1
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

        // Always-present hidden Android EditText (1dp) — provides reliable IME target
        AndroidView(factory = { ctx ->
             android.widget.EditText(ctx).apply {
                setText(targetDays.toString())
                setSelection(text.length)
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
                isFocusableInTouchMode = true
                setOnEditorActionListener { v, actionId, _ ->
                    if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                        val parsed = v.text?.toString()?.filter { it.isDigit() }?.take(3)?.toIntOrNull() ?: targetDays
                        targetDays = parsed.coerceIn(0, 999)
                        try {
                            val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                            imm?.hideSoftInputFromWindow(windowToken, 0)
                        } catch (_: Exception) {}
                        editingDays = false
                        true
                    } else false
                }
                setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        val parsed = text?.toString()?.filter { it.isDigit() }?.take(3)?.toIntOrNull() ?: targetDays
                        targetDays = parsed.coerceIn(0, 999)
                        editingDays = false
                    }
                }
            }
        }, update = { view ->
             hiddenEditRef.value = view
             val cur = view.text?.toString() ?: ""
             if (cur != targetDays.toString()) {
                 view.setText(targetDays.toString())
                 view.setSelection(view.text.length)
             }
        }, modifier = Modifier.size(1.dp).align(Alignment.BottomCenter))

        // Focus and IME show when editingDays becomes true — set ADJUST_NOTHING first then focus hidden EditText
        LaunchedEffect(editingDays) {
            val act = context as? Activity
            if (editingDays) {
                if (prevSoftInputMode.value == null) prevSoftInputMode.value = act?.window?.attributes?.softInputMode
                try { act?.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING) } catch (_: Exception) {}
                try {
                    val edit = hiddenEditRef.value
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                    if (edit != null) {
                        edit.post {
                            try {
                                edit.requestFocus()
                                imm?.showSoftInput(edit, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                            } catch (_: Exception) {}
                        }
                    } else {
                        // fallback: show on decor
                        val decor = act?.window?.decorView
                        decor?.post { try { imm?.showSoftInput(decor, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT) } catch (_: Exception) {} }
                    }
                } catch (_: Exception) {}
            } else {
                try { val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager; imm?.hideSoftInputFromWindow((context as? Activity)?.window?.decorView?.windowToken, 0) } catch (_: Exception) {}
                try { prevSoftInputMode.value?.let { (context as? Activity)?.window?.setSoftInputMode(it) } } catch (_: Exception) {}
                prevSoftInputMode.value = null
            }
        }
     }
 }

@Composable
private fun AppBrandTitleBar() {
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
}

@Preview(showBackground = true)
@Composable
private fun StartScreenPreview() { StartScreen() }
