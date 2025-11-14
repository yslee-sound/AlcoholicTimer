package kr.sweetapps.alcoholictimer.feature.start

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Handler
import android.os.Looper
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
import androidx.core.content.edit
import com.google.android.play.core.appupdate.AppUpdateInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.MainActivity
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ads.InterstitialAdManager
import kr.sweetapps.alcoholictimer.core.ui.AppBorder
import kr.sweetapps.alcoholictimer.core.ui.AppElevation
import kr.sweetapps.alcoholictimer.core.ui.LayoutConstants
import kr.sweetapps.alcoholictimer.core.ui.StandardScreenWithBottomButton
import kr.sweetapps.alcoholictimer.core.util.AppUpdateManager
import kr.sweetapps.alcoholictimer.feature.addrecord.components.TargetDaysBottomSheet
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreenWithUpdate(
    appUpdateManager: AppUpdateManager,
    initialMinRemainMillis: Long = 0L,
    usesComposeOverlay: Boolean = true,
    holdSplashState: MutableState<Boolean>, // Activity에서 제어하는 스플래시 유지 상태
    onSplashFinished: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val restartPromptText = stringResource(R.string.update_downloaded_restart_prompt)
    val actionRestartText = stringResource(R.string.action_restart)

    var keepMinOverlay by remember { mutableStateOf(initialMinRemainMillis > 0L) }
    LaunchedEffect(initialMinRemainMillis) {
        if (initialMinRemainMillis > 0L) {
            delay(initialMinRemainMillis)
            keepMinOverlay = false
        }
    }

    var showUpdateDialog by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(true) }
    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var availableVersionName by remember { mutableStateOf("") }

    // 안전 타임아웃: 업데이트 확인이 비정상 지연될 경우 자동 해제 (디폴트 3초)
    LaunchedEffect(Unit) {
        val timeoutMs = 3000L
        delay(timeoutMs)
        if (isCheckingUpdate && !showUpdateDialog) {
            android.util.Log.w("StartScreenWithUpdate", "Update check timeout. Releasing gate.")
            isCheckingUpdate = false
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            appUpdateManager.checkForUpdate(
                forceCheck = false,
                onUpdateAvailable = { info ->
                    updateInfo = info
                    availableVersionName = "v${info.availableVersionCode()}"
                    showUpdateDialog = true
                    isCheckingUpdate = false
                },
                onNoUpdate = { isCheckingUpdate = false }
            )
        }
    }

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

    val gateNavigation = isCheckingUpdate || showUpdateDialog

    val showSplashOverlay = usesComposeOverlay && (keepMinOverlay || isCheckingUpdate || holdSplashState.value) && !showUpdateDialog

    // 스플래시 오버레이가 해제될 때(onSplashFinished)만 앱으로 진입
    LaunchedEffect(showSplashOverlay) {
        if (!showSplashOverlay) onSplashFinished()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        StartScreen(gateNavigation = gateNavigation)

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
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))

        kr.sweetapps.alcoholictimer.core.ui.components.AppUpdateDialog(
            isVisible = showUpdateDialog,
            versionName = if (availableVersionName.isNotBlank()) availableVersionName else "vNext",
            updateMessageResourceId = R.string.update_dialog_default_message,
            onUpdateClick = { updateInfo?.let { appUpdateManager.startFlexibleUpdate(it) }; showUpdateDialog = false },
            onDismiss = { showUpdateDialog = false; appUpdateManager.markUserPostpone() },
            canDismiss = !appUpdateManager.isMaxPostponeReached()
        )

        // 광고가 닫히거나 실패하면 StartActivity에 등록된 콜백이 holdSplash를 해제하여
        // 이 LaunchedEffect를 통해 onSplashFinished가 호출됩니다.
     }
 }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreen(gateNavigation: Boolean = false, onStart: (() -> Unit)? = null) {
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
            if (onStart != null) {
                // NavHost 내부 이동: Activity 종료 금지 (스플래시 재등장 방지)
                onStart()
            } else {
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

    Box(modifier = Modifier.fillMaxSize()) {
        StandardScreenWithBottomButton(
            topContent = {
                Column { // 내부 전용 Column: spacing 없음 -> 지정한 12dp 그대로 유지
                    Spacer(modifier = Modifier.height(LayoutConstants.START_BRAND_TITLE_TOP_GAP))
                    AppBrandTitleBar()
                    Spacer(modifier = Modifier.height(LayoutConstants.START_BRAND_TITLE_BOTTOM_GAP))

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
                                .padding(LayoutConstants.FIRST_CARD_TOP_INNER_PADDING),
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
            // bottomAd = { AdmobBanner() } // moved to MainActivity BaseScaffold during Phase-1
        )
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
            .padding(horizontal = LayoutConstants.SCREEN_HORIZONTAL_PADDING)
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
