package com.example.alcoholictimer.feature.start

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.example.alcoholictimer.R
import com.example.alcoholictimer.core.ui.AppElevation
import com.example.alcoholictimer.core.ui.BaseActivity
import com.example.alcoholictimer.core.ui.StandardScreenWithBottomButton
import com.example.alcoholictimer.core.ui.components.AppUpdateDialog
import com.example.alcoholictimer.core.util.AppUpdateManager
import com.example.alcoholictimer.core.util.Constants
import com.example.alcoholictimer.core.util.UpdateVersionMapper
import com.example.alcoholictimer.feature.run.RunActivity
import com.google.android.play.core.install.model.AppUpdateType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.material3.SnackbarResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class StartActivity : BaseActivity() {
    private lateinit var appUpdateManager: AppUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // 런처 액티비티에서만 스플래시 설치
        installSplashScreen()
        super.onCreate(savedInstanceState)
        Constants.initializeUserSettings(this)
        Constants.ensureInstallMarkerAndResetIfReinstalled(this)

        // 상태바/내비게이션 바 라이트 아이콘 적용 및 표시
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true
        controller.show(WindowInsetsCompat.Type.statusBars())
        controller.show(WindowInsetsCompat.Type.navigationBars())

        // In-App Update 초기화
        appUpdateManager = AppUpdateManager(this)

        // 디버그 빌드 여부 (릴리스에서 데모 비활성화용)
        val isDebugBuild = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

        // 데모 모드 플래그: DEBUG에서만 인텐트 값 반영, RELEASE에서는 항상 false
        val demoUpdateUi = if (isDebugBuild) intent.getBooleanExtra("demo_update_ui", false) else false

        setContent {
            // 상단 시스템바 패딩은 적용, 하단은 개별 레이아웃에서 처리
            BaseScreen(applyBottomInsets = false, applySystemBars = true) {
                StartScreenWithUpdate(appUpdateManager, demoMode = demoUpdateUi, debugEnabled = isDebugBuild)
            }
        }
    }

    override fun getScreenTitle(): String = "금주 설정"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StartScreenWithUpdate(appUpdateManager: AppUpdateManager, demoMode: Boolean = false, debugEnabled: Boolean = false) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 업데이트 다이얼로그/체크 상태
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(true) }
    var updateInfo by remember { mutableStateOf<com.google.android.play.core.appupdate.AppUpdateInfo?>(null) }
    var availableVersionName by remember { mutableStateOf("") }

    // DEBUG에서만 데모 활성화
    val demoEnabled = debugEnabled
    val demoActive = demoEnabled && demoMode

    // 데모 트리거 함수 (UI만 시연)
    val triggerDemo: () -> Unit = {
        isCheckingUpdate = true
        scope.launch {
            delay(600)
            // 데모 타깃 코드는 실제 배포 정책과 동일 포맷 (yyyymmddNN)
            val demoTargetCode = 2025101001
            // 코드 -> 사용자 노출용 버전명으로 매핑 (없으면 코드 문자열 폴백)
            availableVersionName = UpdateVersionMapper.toVersionName(demoTargetCode) ?: demoTargetCode.toString()
            showUpdateDialog = true
            isCheckingUpdate = false
        }
    }

    // 앱 시작 시 업데이트 확인 (데모 모드면 실제 체크 생략)
    if (!demoActive) {
        LaunchedEffect(Unit) {
            scope.launch {
                appUpdateManager.checkForUpdate(
                    forceCheck = false,
                    onUpdateAvailable = { info ->
                        updateInfo = info
                        // 제공되는 것은 versionCode뿐이므로 사용자 노출용 버전명으로 변환
                        val code = info.availableVersionCode()
                        availableVersionName = UpdateVersionMapper.toVersionName(code) ?: code.toString()
                        showUpdateDialog = true
                        isCheckingUpdate = false
                    },
                    onNoUpdate = {
                        isCheckingUpdate = false
                    }
                )
            }
        }
    } else {
        LaunchedEffect(Unit) { triggerDemo() }
    }

    // 업데이트 다운로드 완료 리스너
    LaunchedEffect(Unit) {
        appUpdateManager.registerInstallStateListener {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "업데이트가 다운로드되었습니다. 다시 시작하여 설치하세요.",
                    actionLabel = "다시 시작",
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

    // 300ms 이상 걸릴 때만 오버레이 표시 (깜빡임 방지)
    var showOverlay by remember { mutableStateOf(false) }
    LaunchedEffect(isCheckingUpdate, showUpdateDialog) {
        if (isCheckingUpdate && !showUpdateDialog) {
            delay(300)
            if (isCheckingUpdate && !showUpdateDialog) showOverlay = true
        } else {
            showOverlay = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        StartScreen(
            gateNavigation = gateNavigation,
            onDebugLongPress = if (demoEnabled) ({ triggerDemo() }) else null
        )

        // 업데이트 다이얼로그
        AppUpdateDialog(
            isVisible = showUpdateDialog,
            versionName = availableVersionName,
            updateMessage = "새로운 기능과 개선사항이 포함되어 있습니다.",
            onUpdateClick = {
                if (demoActive) {
                    showUpdateDialog = false
                    scope.launch { snackbarHostState.showSnackbar("데모: 업데이트 버튼 클릭") }
                } else {
                    updateInfo?.let { info ->
                        val immediateAllowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                        if (appUpdateManager.isMaxPostponeReached() && immediateAllowed) {
                            appUpdateManager.startImmediateUpdate(info)
                        } else {
                            appUpdateManager.startFlexibleUpdate(info)
                        }
                    }
                    showUpdateDialog = false
                }
            },
            onDismiss = {
                if (demoActive) {
                    showUpdateDialog = false
                } else {
                    appUpdateManager.markUserPostpone()
                    showUpdateDialog = false
                }
            },
            canDismiss = !appUpdateManager.isMaxPostponeReached() || demoActive
        )

        // 로딩 오버레이 (다이얼로그 표시 중에는 숨김)
        if (showOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(text = stringResource(id = R.string.checking_update), style = MaterialTheme.typography.bodyMedium, color = colorResource(id = R.color.color_hint_gray))
                }
            }
        }

        // 스낵바
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StartScreen(gateNavigation: Boolean = false, onDebugLongPress: (() -> Unit)? = null) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("user_settings", MODE_PRIVATE)
    val startTime = sharedPref.getLong("start_time", 0L)
    val timerCompleted = sharedPref.getBoolean("timer_completed", false)

    // 진행 중 세션이 있고, 게이트가 내려가 있을 때만 Run 화면으로 이동
    if (!gateNavigation && startTime != 0L && !timerCompleted) {
        LaunchedEffect(Unit) {
            context.startActivity(Intent(context, RunActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
        return
    }

    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(text = "30", selection = TextRange(0, 2))) }
    val isValid by remember { derivedStateOf { textFieldValue.text.toFloatOrNull()?.let { it > 0 } ?: false } }
    var isTextSelected by remember { mutableStateOf(true) }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            delay(50)
            val len = textFieldValue.text.length
            textFieldValue = textFieldValue.copy(selection = TextRange(0, len))
            isTextSelected = true
        }
    }

    StandardScreenWithBottomButton(
        topContent = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD), // down from CARD_HIGH
                border = BorderStroke(1.dp, colorResource(id = R.color.color_border_light))
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
                            onClick = {},
                            onLongClick = { onDebugLongPress() }
                        )
                    } else baseTitleModifier

                    Text(
                        text = "목표 기간 설정",
                        style = MaterialTheme.typography.titleLarge,
                        color = colorResource(id = R.color.color_title_primary),
                        modifier = titleModifier
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                    ) {
                        Card(
                            modifier = Modifier.width(100.dp).height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.color_bg_card_light)),
                            elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                                BasicTextField(
                                    value = textFieldValue,
                                    onValueChange = { newValue ->
                                        val filtered = newValue.text.filter { it.isDigit() || it == '.' }
                                        val dots = filtered.count { it == '.' }
                                        val finalFiltered = if (dots <= 1) filtered else textFieldValue.text
                                        val finalText = when {
                                            finalFiltered.isEmpty() -> "0"
                                            finalFiltered.length > 1 && finalFiltered.startsWith("0") && !finalFiltered.startsWith("0.") -> finalFiltered.substring(1)
                                            else -> finalFiltered
                                        }
                                        val selection = if (isTextSelected) TextRange(finalText.length) else TextRange(finalText.length)
                                        textFieldValue = TextFieldValue(text = finalText, selection = selection)
                                        isTextSelected = false
                                    },
                                    textStyle = MaterialTheme.typography.headlineLarge.copy(
                                        color = colorResource(id = R.color.color_indicator_days),
                                        textAlign = TextAlign.Center
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    cursorBrush = SolidColor(colorResource(id = R.color.color_indicator_days)),
                                    modifier = Modifier.fillMaxWidth().onFocusChanged { isFocused = it.isFocused }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "일",
                            style = MaterialTheme.typography.titleLarge,
                            color = colorResource(id = R.color.color_indicator_label_gray)
                        )
                    }
                    Text(
                        text = "금주할 목표 기간을 입력해주세요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorResource(id = R.color.color_hint_gray),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        },
        bottomButton = {
            Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                ModernStartButton(
                    isEnabled = isValid,
                    onStart = {
                        val targetTime = textFieldValue.text.toFloatOrNull() ?: 0f
                        if (targetTime > 0f) {
                            val formatted = String.format(Locale.US, "%.6f", targetTime).toFloat()
                            sharedPref.edit {
                                putFloat("target_days", formatted)
                                putLong("start_time", System.currentTimeMillis())
                                putBoolean("timer_completed", false)
                            }
                            context.startActivity(Intent(context, RunActivity::class.java))
                        }
                    }
                )
            }
        },
        imePaddingEnabled = false
    )
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
