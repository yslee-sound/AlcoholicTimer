/**
 * Tab 05: Settings & About Screen (설정 및 정보)
 *
 * [REFACTORED 2025-12-19]
 * - 폴더명: tab_05 (변경하지 않음 - 안전성 우선)
 * - 실제 의미: Settings (설정 및 정보)
 * - 접근 경로: Tab 3 (커뮤니티) → 우측 상단 설정 버튼
 *
 * 하위 화면:
 * - AboutScreen: 앱 정보 (메인)
 * - NicknameEdit: 닉네임 편집
 * - HabitSettings: 습관 설정
 * - CurrencySettings: 통화 설정
 * - Debug: 디버그 메뉴
 * - Notification: 알림 목록
 * - Customer: 고객 지원
 */
package kr.sweetapps.alcoholictimer.ui.tab_03.screens.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kr.sweetapps.alcoholictimer.BuildConfig
import kr.sweetapps.alcoholictimer.MainApplication
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.components.BackTopBar
import kr.sweetapps.alcoholictimer.ui.theme.LocalDimens
import kr.sweetapps.alcoholictimer.ui.tab_03.components.CustomerFeedbackBottomSheet
import kr.sweetapps.alcoholictimer.ui.tab_03.viewmodel.Tab05ViewModel
import kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue  // [NEW] 메인 UI 색상
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import kr.sweetapps.alcoholictimer.ui.tab_03.components.AvatarSelectionDialog
import kr.sweetapps.alcoholictimer.ui.tab_03.viewmodel.SettingsUiState
import kr.sweetapps.alcoholictimer.ui.theme.Dimens
import kr.sweetapps.alcoholictimer.util.AvatarManager

private fun ContextToActivity(context: Context): Activity? {
    var ctx: Context? = context
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun AboutScreen(
    onNavigateLicenses: () -> Unit,
    onNavigatePrivacy: () -> Unit = {},
    onNavigateEditNickname: () -> Unit = {},
    onNavigateCurrencySettings: () -> Unit = {},
    onNavigateHabitSettings: () -> Unit = {}, // [NEW] 습관 설정 네비게이션
    onNavigateDebug: () -> Unit = {},
    onNavigateNotification: () -> Unit = {},
    onNavigateCustomer: () -> Unit = {},
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    viewModel: Tab05ViewModel = viewModel()
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val scrollState = rememberScrollState()

    // [NEW] ViewModel 초기화
    val defaultNickname = stringResource(R.string.default_nickname)
    LaunchedEffect(Unit) {
        viewModel.initialize(context, defaultNickname)
        viewModel.refreshNickname(defaultNickname)
    }

    // [NEW] ON_RESUME 이벤트 감지 - 화면 복귀 시 데이터 새로고침 (2025-12-24)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                // ProfileEditScreen에서 돌아올 때 최신 데이터(아바타, 닉네임) 강제 로드
                viewModel.reloadUserData(defaultNickname)
                Log.d("AboutScreen", "ON_RESUME: 유저 데이터 새로고침 완료")
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // [NEW] ViewModel 상태 구독
    val uiState by viewModel.uiState.collectAsState()
    val nickname = uiState.nickname
    val showCustomerFeedbackSheet = uiState.showCustomerFeedbackSheet

    // [NEW] Crashlytics 연동 확인을 위한 5회 탭 카운터
    val versionTapCount = remember { mutableStateOf(0) }
    val lastTapTime = remember { mutableStateOf(0L) }

    // [FIX] isPersonalizedAdsAllowed 제거 - Switch를 버튼으로 변경했으므로 checked 상태 불필요
    val versionInfo: String
    val onPrivacyClick: () -> Unit
    val onLicenseClick: () -> Unit
    val onAdsClick: () -> Unit
    val onDebugClick: () -> Unit
    val showDebugMenu: Boolean
    val showPrivacyOptions: Boolean

    if (isInPreview) {
        versionInfo = "1.0.0-preview"
        onPrivacyClick = {}
        onLicenseClick = {}
        onAdsClick = {}
        onDebugClick = {}
        showDebugMenu = true
        showPrivacyOptions = true
    } else {
        val app = context.applicationContext as? MainApplication
        val umpConsentManager = app?.umpConsentManager

        // [FIX] isPersonalizedAdsAllowed 관련 코드 제거 - 더 이상 사용하지 않음
        // Avoid double "-debug-debug" if VERSION_NAME already contains debug suffix
        versionInfo = if (BuildConfig.DEBUG) {
            val v = BuildConfig.VERSION_NAME
            if (v.contains("debug", ignoreCase = true) || v.endsWith("-debug")) v else "${v}-debug"
        } else BuildConfig.VERSION_NAME
        onPrivacyClick = { onNavigatePrivacy() }
        onLicenseClick = { onNavigateLicenses() }
        onAdsClick = {
            val activity = ContextToActivity(context)
            if (activity != null && umpConsentManager != null) {
                try {
                    umpConsentManager.showPrivacyOptionsForm(activity) { error ->
                        // [FIX] 에러가 있을 때만 Toast 표시 (정상 완료 시 null이므로 표시하지 않음)
                        if (error != null) {
                            Log.e("AboutScreen", "Privacy Options Form 표시 실패: $error")
                            Toast.makeText(
                                context,
                                "개인정보 설정을 불러올 수 없습니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Log.d("AboutScreen", "Privacy Options Form 정상 표시 완료")
                        }
                    }
                } catch (t: Throwable) {
                    Log.e("AboutScreen", "showPrivacyOptionsForm 호출 실패", t)
                    Toast.makeText(
                        context,
                        "개인정보 설정을 불러올 수 없습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Log.w("AboutScreen", "Activity or umpConsentManager null; cannot show privacy options")
            }
        }
        onDebugClick = { onNavigateDebug() }

        // [FIX] Privacy Options: EU 사용자 OR 개발자(DEBUG 빌드)
        val isPrivacyRequired = try {
            umpConsentManager?.isPrivacyOptionsRequired() ?: false
        } catch (t: Throwable) {
            false
        }
        showPrivacyOptions = isPrivacyRequired || BuildConfig.DEBUG

        // [FIX] Debug 메뉴: 개발자(DEBUG 빌드)만
        showDebugMenu = BuildConfig.DEBUG
    }

    // Use design tokens
    val dims = LocalDimens.current

    // [NEW] showBack = true일 때 Scaffold로 독립 화면 구성
    if (showBack) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.White, // [FIX] 하단 비침 방지 (흰색 배경 고정)
            contentWindowInsets = WindowInsets.systemBars, // [FIX] 시스템 바 영역 침범 방지
            topBar = {
                BackTopBar(
                    title = stringResource(R.string.settings_screen_title),
                    onBack = onBack
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(bottom = 100.dp) // [NEW] 하단 스크롤 여백 추가
            ) {
                AboutScreenContent(
                    nickname = nickname,
                    versionInfo = versionInfo,
                    versionTapCount = versionTapCount,
                    lastTapTime = lastTapTime,
                    context = context,
                    uiState = uiState,
                    viewModel = viewModel,
                    dims = dims,
                    onNavigateEditNickname = onNavigateEditNickname,
                    onLicenseClick = onLicenseClick,
                    onPrivacyClick = onPrivacyClick,
                    onNavigateCurrencySettings = onNavigateCurrencySettings,
                    onNavigateHabitSettings = onNavigateHabitSettings,
                    onAdsClick = onAdsClick,
                    onDebugClick = onDebugClick,
                    onNavigateNotification = onNavigateNotification,
                    showPrivacyOptions = showPrivacyOptions,
                    showDebugMenu = showDebugMenu
                )
            }
        }
    } else {
        // BaseScaffold 내부에서 사용 (기존 방식)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .verticalScroll(scrollState)
                .padding(bottom = 100.dp) // [UPDATE] navBarHeight → 100dp 통일
        ) {
            AboutScreenContent(
                nickname = nickname,
                versionInfo = versionInfo,
                versionTapCount = versionTapCount,
                lastTapTime = lastTapTime,
                context = context,
                uiState = uiState,
                viewModel = viewModel,
                dims = dims,
                onNavigateEditNickname = onNavigateEditNickname,
                onLicenseClick = onLicenseClick,
                onPrivacyClick = onPrivacyClick,
                onNavigateCurrencySettings = onNavigateCurrencySettings,
                onNavigateHabitSettings = onNavigateHabitSettings,
                onAdsClick = onAdsClick,
                onDebugClick = onDebugClick,
                onNavigateNotification = onNavigateNotification,
                showPrivacyOptions = showPrivacyOptions,
                showDebugMenu = showDebugMenu
            )
        }
    }

    // 고객 문의 바텀 시트
    if (showCustomerFeedbackSheet) {
        CustomerFeedbackBottomSheet(
            onDismiss = { viewModel.setShowCustomerFeedbackSheet(false) },
            onSubmit = { category, content, email ->
                Log.d("Tab05", "Feedback submitted - Category: $category")
            }
        )
    }

    // [NEW] 아바타 선택 다이얼로그
    if (uiState.showAvatarDialog) {
        AvatarSelectionDialog(
            currentAvatarIndex = uiState.avatarIndex,
            onAvatarSelected = { index ->
                viewModel.updateAvatar(index)
            },
            onDismiss = { viewModel.setShowAvatarDialog(false) }
        )
    }
}

@Composable
private fun AboutScreenContent(
    nickname: String,
    versionInfo: String,
    versionTapCount: MutableState<Int>,
    lastTapTime: MutableState<Long>,
    context: Context,
    uiState: SettingsUiState,
    viewModel: Tab05ViewModel,
    dims: Dimens,
    onNavigateEditNickname: () -> Unit,
    onLicenseClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onNavigateCurrencySettings: () -> Unit,
    onNavigateHabitSettings: () -> Unit,
    onAdsClick: () -> Unit,
    onDebugClick: () -> Unit,
    onNavigateNotification: () -> Unit,
    showPrivacyOptions: Boolean,
    showDebugMenu: Boolean
) {
    // [REMOVED] 바텀시트 상태 관리 제거 - ProfileEditScreen에서만 수정 가능 (2025-12-24)
    // val showAvatarSheet = remember { mutableStateOf(false) }
    // val showNicknameSheet = remember { mutableStateOf(false) }

    Column {
        // [NEW] Profile Row with Avatar (분리된 클릭 영역)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // [NEW] 아바타 이미지 (동그라미) - 표시 전용 (클릭 불가)
            // [MODIFIED] 클릭 기능 제거 - ProfileEditScreen에서만 변경 가능 (2025-12-24)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .border(2.dp, Color(0xFFE0E0E0), CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFFF5F5F5))
                    // [REMOVED] .clickable { showAvatarSheet.value = true }
            ) {
                Image(
                    painter = painterResource(id = AvatarManager.getAvatarResId(uiState.avatarIndex)),
                    contentDescription = "프로필 아바타",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(dims.spacing.sm))

            // [NEW] 닉네임 영역 - 표시 전용 (클릭 불가)
            // [MODIFIED] 클릭 기능 제거 - ProfileEditScreen에서만 수정 가능 (2025-12-24)
            Row(
                modifier = Modifier
                    .weight(1f),
                    // [REMOVED] .clickable { showNicknameSheet.value = true }
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = nickname, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                // [REMOVED] '>' 아이콘 제거 - 표시 전용이므로 불필요 (2025-12-24)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // [NEW] 프로필 편집하기 버튼 (기존 앱 평가하기 버튼 자리)
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dims.component.buttonHeight)
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .background(
                        color = MainPrimaryBlue,  // 메인 UI 색상
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        // 프로필 편집 화면으로 이동
                        onNavigateEditNickname()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_edit_profile),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // [NEW] 1줄: 알림, 문의/제안, 앱 평가하기
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 1. 알림 (Notifications)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateNotification() }
                    .padding(vertical = dims.spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.bell),
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                AutoResizingTextLabel(
                    text = stringResource(R.string.tab05_notifications),
                    fontSize = 12.sp,
                    color = Color.Black,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. 문의/제안 (Support)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.setShowCustomerFeedbackSheet(true) }
                    .padding(vertical = dims.spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.headset),
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                AutoResizingTextLabel(
                    text = stringResource(R.string.tab05_customer_support),
                    fontSize = 12.sp,
                    color = Color.Black,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 3. 앱 평가하기 (Rate App) [NEW]
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        // 플레이스토어로 이동
                        val packageName = "kr.sweetapps.alcoholictimer"
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
                            intent.setPackage("com.android.vending")
                            context.startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            val intent = Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$packageName".toUri())
                            context.startActivity(intent)
                        }
                    }
                    .padding(vertical = dims.spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.star),
                    contentDescription = null,
                    tint = Color.Black, // 검정색 (다른 버튼들과 통일)
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                AutoResizingTextLabel(
                    text = stringResource(R.string.tab05_rate_app),
                    fontSize = 12.sp,
                    color = Color.Black,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // [NEW] 2줄: 추천앱, 비움, 비움
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 1. 추천앱 (Apps)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = dims.spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.thumbsup),
                    contentDescription = null,
                    tint = Color(0xFFBDBDBD),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                AutoResizingTextLabel(
                    text = stringResource(R.string.tab05_recommended_apps),
                    fontSize = 12.sp,
                    color = Color(0xFFBDBDBD),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. 비움 (Empty)
            Spacer(modifier = Modifier.weight(1f))

            // 3. 비움 (Empty)
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Thick section divider (light surface variant) - show light gray as in reference UI
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(dims.divider.sectionThickness)
            .background(dims.divider.lightColor))

        Spacer(modifier = Modifier.height(0.dp))

        // Settings / About list
        Column(modifier = Modifier.background(Color.White)) {
            // 리스트 항목 사이에 thin divider를 추가하여 구분합니다.

            // [NEW] 버전 정보 - 5회 탭 시 Crashlytics 테스트 보고서 전송
            SimpleAboutRow(
                title = stringResource(id = R.string.about_version_info),
                onClick = {
                    val currentTime = System.currentTimeMillis()

                    // [DEBUG] 클릭 감지 로그
                    Log.d("AboutScreen", "🔘 버전 정보 탭 감지! (현재 카운트: ${versionTapCount.value})")

                    // 1초 이내 탭이면 카운트 증가, 아니면 리셋
                    if (currentTime - lastTapTime.value < 1000) {
                        versionTapCount.value += 1
                        Log.d("AboutScreen", "⏱️ 1초 이내 탭 → 카운트 증가: ${versionTapCount.value}")
                    } else {
                        versionTapCount.value = 1
                        Log.d("AboutScreen", "⏱️ 1초 이상 경과 → 카운트 리셋: 1")
                    }
                    lastTapTime.value = currentTime

                    // 5회 탭 감지 시 Crashlytics Non-fatal Exception 전송
                    if (versionTapCount.value >= 5) {
                        Log.d("AboutScreen", "🎯 5회 탭 달성! Crashlytics 테스트 보고서 전송 시작...")
                        try {
                            val crashlytics = FirebaseCrashlytics.getInstance()

                            // [INFO] Crashlytics 활성화 상태 확인
                            val isEnabled = crashlytics.isCrashlyticsCollectionEnabled()
                            Log.d("AboutScreen", "📊 Crashlytics 활성화 상태: $isEnabled")

                            if (!isEnabled && BuildConfig.DEBUG) {
                                // Debug 빌드에서 비활성화된 경우 안내 메시지
                                Toast.makeText(
                                    context,
                                    "Debug 빌드: Crashlytics 비활성화 상태\n" +
                                    "Release 빌드에서 테스트하세요.",
                                    Toast.LENGTH_LONG
                                ).show()
                                Log.w("AboutScreen", "⚠️ Debug 빌드에서는 Crashlytics가 비활성화되어 있습니다.")
                                Log.w("AboutScreen", "💡 Release 빌드(bundleRelease)로 테스트하세요.")
                            } else {
                                // [PROD] Non-fatal Exception을 Firebase Crashlytics에 전송
                                val testException = Exception("Test Non-Fatal Exception - Crashlytics Check (v$versionInfo, Build: ${if (BuildConfig.DEBUG) "Debug" else "Release"})")
                                crashlytics.recordException(testException)

                                // 사용자 피드백
                                val projectType = if (BuildConfig.DEBUG) "Dev" else "Prod"
                                Toast.makeText(
                                    context,
                                    "Crashlytics 테스트 보고서 전송 완료.\nFirebase $projectType 프로젝트에서 확인하세요.",
                                    Toast.LENGTH_LONG
                                ).show()

                                Log.d("AboutScreen", "✅ Crashlytics 테스트 보고서 전송 완료 (버전: $versionInfo)")
                                Log.d("AboutScreen", "📝 Firebase $projectType 프로젝트 Crashlytics → Non-fatals에서 5~10분 후 확인 가능")
                            }
                        } catch (e: Exception) {
                            Log.e("AboutScreen", "❌ Crashlytics 테스트 보고서 전송 실패", e)
                            Toast.makeText(
                                context,
                                "테스트 보고서 전송 실패: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        // 카운터 리셋
                        versionTapCount.value = 0
                        Log.d("AboutScreen", "🔄 카운터 리셋 완료")
                    }
                },
                trailing = {
                    Text(
                        text = versionInfo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            Box(modifier = Modifier.fillMaxWidth().height(dims.divider.thin).background(dims.divider.lightColor))

            // [NEW] 습관 설정 - 기존 Tab04의 습관 설정 기능을 여기로 이동
            SimpleAboutRow(
                title = stringResource(id = R.string.settings_title),
                onClick = onNavigateHabitSettings,
                trailing = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_caret_right),
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
            Box(modifier = Modifier.fillMaxWidth().height(dims.divider.thin).background(dims.divider.lightColor))

            // [NEW] 통화 설정 - 기존 습관 설정 화면의 통화 설정을 독립 메뉴로 분리
            SimpleAboutRow(
                title = stringResource(id = R.string.settings_currency),
                onClick = onNavigateCurrencySettings,
                trailing = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_caret_right),
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
            Box(modifier = Modifier.fillMaxWidth().height(dims.divider.thin).background(dims.divider.lightColor))

            // Privacy
            SimpleAboutRow(title = stringResource(id = R.string.document_title_privacy), onClick = onPrivacyClick, trailing = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_caret_right),
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(20.dp)
                )
            })
            Box(modifier = Modifier.fillMaxWidth().height(dims.divider.thin).background(dims.divider.lightColor))

            // Open Source License
            SimpleAboutRow(title = stringResource(id = R.string.document_title_open_source), onClick = onLicenseClick, trailing = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_caret_right),
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(20.dp)
                )
            })
            Box(modifier = Modifier.fillMaxWidth().height(dims.divider.thin).background(dims.divider.lightColor))

            // [FIX] Privacy Options - EU 사용자 OR 개발자(DEBUG 빌드)에서만 표시
            if (showPrivacyOptions) {
                SimpleAboutRow(
                    title = "Privacy Options",
                    onClick = onAdsClick,
                    trailing = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_caret_right),
                            contentDescription = null,
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
                Box(modifier = Modifier.fillMaxWidth().height(dims.divider.thin).background(dims.divider.lightColor))
            }

            // Debug menu
            if (showDebugMenu) {
                SimpleAboutRow(title = "Debug", onClick = onDebugClick, trailing = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_caret_right),
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(20.dp)
                    )
                })
            }
        }
    } // Column 닫기

    // [REMOVED] 바텀시트 호출 제거 - ProfileEditScreen에서만 수정 가능 (2025-12-24)
    // showAvatarSheet, showNicknameSheet 관련 코드 제거됨
}


@Preview(showBackground = true)
@Composable
fun AboutScreenPreview() {
    AboutScreen(
        onNavigateLicenses = {},
        onNavigateDebug = {},
        onNavigateNotification = {},
        onNavigateCustomer = {}
    )
}

// [NEW] 텍스트가 길면 자동으로 폰트 크기를 줄여주는 유틸리티 (Tab05 전용)
// 수정 사항: 패키지명을 제거하고 import를 사용하도록 변경 (에러 해결)
@Composable
private fun AutoResizingTextLabel(
    text: String,
    fontSize: TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal
) {
    // 1. 상태 저장 (remember + mutableStateOf 사용, 'by' 없이 직접 할당)
    val resizedTextStyle = remember {
        mutableStateOf(
            TextStyle(
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = color,
                textAlign = TextAlign.Center
            )
        )
    }

    // 2. 그리기 여부 상태
    val shouldDraw = remember { mutableStateOf(false) }

    Text(
        text = text,
        // [FIX] 여기가 에러 원인이었습니다. modifier.drawWithContent로 수정
        modifier = modifier.drawWithContent {
            if (shouldDraw.value) {
                drawContent()
            }
        },
        softWrap = false,
        style = resizedTextStyle.value, // .value로 접근
        maxLines = 1,
        onTextLayout = { result ->
            if (result.didOverflowWidth) {
                val currentStyle = resizedTextStyle.value
                val newSize = currentStyle.fontSize * 0.9f

                if (newSize >= 9.sp) { // 최소 9sp까지만 축소
                    resizedTextStyle.value = currentStyle.copy(fontSize = newSize)
                } else {
                    shouldDraw.value = true
                }
            } else {
                shouldDraw.value = true
            }
        }
    )
}

// [NEW] SimpleAboutRow 컴포저블 - 설정 메뉴 리스트 아이템
@Composable
private fun SimpleAboutRow(
    title: String,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            trailing()
        }
    }
}

