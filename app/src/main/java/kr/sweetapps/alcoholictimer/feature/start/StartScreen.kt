package kr.sweetapps.alcoholictimer.feature.start

import android.content.Context.MODE_PRIVATE
import android.content.Intent
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.core.content.edit
import kr.sweetapps.alcoholictimer.MainActivity
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ads.InterstitialAdManager
import kr.sweetapps.alcoholictimer.core.ui.AppBorder
import kr.sweetapps.alcoholictimer.core.ui.AppElevation
import kr.sweetapps.alcoholictimer.constants.UiConstants
import kr.sweetapps.alcoholictimer.core.ui.StandardScreenWithBottomButton
import kr.sweetapps.alcoholictimer.feature.addrecord.components.TargetDaysBottomSheet
import java.util.Locale

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
            android.util.Log.d("StartScreen", "Immediate navigation path taken: startTime=$startTime timerCompleted=$timerCompleted onStart=${onStart!=null}")
            if (onStart != null) {
                // NavHost 내부 이동: Activity 종료 금지 (스플래시 재등장 방지)
                android.util.Log.d("StartScreen", "Calling onStart() for in-app navigation")
                onStart()
            } else {
                android.util.Log.d("StartScreen", "Starting MainActivity directly (run route)")
                val intent = Intent(context, MainActivity::class.java)
                intent.putExtra("route", "run")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(intent)
                (context as? android.app.Activity)?.finish()
            }
        }
        return
    }

    var targetDays by rememberSaveable { mutableIntStateOf(30) }
    val isValid by remember { derivedStateOf { targetDays > 0 } }
    var showDaysPicker by remember { mutableStateOf(false) }

    val showSplashOverlay = holdSplashState != null && holdSplashState.value

    // 스플래시 오버레이가 해제될 때(onSplashFinished)만 앱으로 진입
    LaunchedEffect(showSplashOverlay) {
        android.util.Log.d("StartScreen", "LaunchedEffect showSplashOverlay changed: $showSplashOverlay")
        if (!showSplashOverlay && onSplashFinished != null) onSplashFinished()
        if (!showSplashOverlay) android.util.Log.d("StartScreen", "onSplashFinished invoked: ${onSplashFinished != null}")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        StandardScreenWithBottomButton(
             topContent = {
                Column { // 내부 전용 Column: spacing 없음 -> 지정한 12dp 그대로 유지
                    Spacer(modifier = Modifier.height(UiConstants.START_BRAND_TITLE_TOP_GAP))
                    AppBrandTitleBar()
                    Spacer(modifier = Modifier.height(UiConstants.START_BRAND_TITLE_BOTTOM_GAP))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD),
                        border = BorderStroke(AppBorder.Hairline, colorResource(id = R.color.color_border_light))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(UiConstants.FIRST_CARD_TOP_INNER_PADDING),
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
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) { showDaysPicker = true },
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
                }
            },
            bottomButton = {
                ModernStartButton(
                    isEnabled = isValid,
                    onStart = {
                        val formatted = String.format(Locale.US, "%.6f", targetDays.toFloat()).toFloat()
                        sharedPref.edit {
                            putFloat("target_days", formatted)
                            putLong("start_time", System.currentTimeMillis())
                            putBoolean("timer_completed", false)
                        }
                        val launchRun: () -> Unit = {
                            if (onStart != null) {
                                // NavHost 내부 이동만 수행, Activity 종료 금지
                                onStart()
                            } else {
                                val intent = Intent(context, MainActivity::class.java)
                                intent.putExtra("route", "run")
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                context.startActivity(intent)
                                (context as? android.app.Activity)?.finish()
                            }
                        }
                        // 전면광고 직접 호출 제거: 홈 전환 3회 규칙 준수
                        launchRun()
                        // 다음 기회 대비 조용히 프리로드만 유지
                        InterstitialAdManager.preload(context.applicationContext)
                    }
                )
            },
            backgroundDecoration = {
                // local background for StartScreen only
                Box(modifier = Modifier.matchParentSize().background(Color(0xFFEEEDE9)))
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
    }

    if (showDaysPicker) {
        TargetDaysBottomSheet(
            initialValue = targetDays,
            onConfirm = { picked -> targetDays = picked.coerceIn(0, 999); showDaysPicker = false },
            onDismiss = { showDaysPicker = false }
        )
    }
}

@Composable
private fun AppBrandTitleBar() {
    Image(
        painter = painterResource(id = R.drawable.alcoholic_timer_logo),
        contentDescription = stringResource(id = R.string.app_name),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 54.dp)
            .padding(horizontal = UiConstants.SCREEN_HORIZONTAL_PADDING)
    )
}

@Composable
fun ModernStartButton(modifier: Modifier = Modifier, isEnabled: Boolean = true, onStart: () -> Unit) {
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
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(48.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StartScreenPreview() { StartScreen() }
