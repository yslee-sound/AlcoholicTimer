package kr.sweetapps.alcoholictimer.ui.tab_05

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kr.sweetapps.alcoholictimer.BuildConfig
import kr.sweetapps.alcoholictimer.MainApplication
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.components.BackTopBar
import kr.sweetapps.alcoholictimer.ui.theme.LocalDimens
import kr.sweetapps.alcoholictimer.ui.tab_04.SimpleAboutRow
import kr.sweetapps.alcoholictimer.ui.tab_05.components.CustomerFeedbackBottomSheet
import kr.sweetapps.alcoholictimer.ui.tab_05.viewmodel.Tab05ViewModel
import kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue  // [NEW] 메인 UI 색상
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit

private fun ContextToActivity(context: android.content.Context): Activity? {
    var ctx: android.content.Context? = context
    while (ctx is android.content.ContextWrapper) {
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

    // [NEW] ViewModel 상태 구독
    val uiState by viewModel.uiState.collectAsState()
    val nickname = uiState.nickname
    val showCustomerFeedbackSheet = uiState.showCustomerFeedbackSheet

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(scrollState)
            .padding(bottom = dims.component.navBarHeight)
    ) {
        if (showBack) {
            BackTopBar(title = stringResource(id = R.string.about_title), onBack = onBack)
        }

        // Profile Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateEditNickname() }
                .padding(start = 20.dp, end = 20.dp, top = 45.dp, bottom = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.usercircle),
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color(0xFFBDBDBD)
            )
            Spacer(modifier = Modifier.width(dims.spacing.sm))
            Text(text = nickname, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_caret_right),
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Rating Button (simple)
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dims.component.buttonHeight)
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .background(
                        color = MainPrimaryBlue,  // [FIX] 메인 UI 색상 적용 (#1E40AF)
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        // [FIX] 디버그 빌드에서도 실제 앱의 플레이스토어 페이지로 이동
                        val packageName = "kr.sweetapps.alcoholictimer" // 실제 릴리즈 앱의 패키지명 (하드코딩)
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
                            intent.setPackage("com.android.vending")
                            context.startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            val intent = Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$packageName".toUri())
                            context.startActivity(intent)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.star),
                        contentDescription = null,
                        tint = Color(0xFFFBC02D), // 노란색
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.tab05_rate_app), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- Row of 3 action buttons
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

                // [FIX] fontSize를 12.sp로 변경 (다른 버튼들과 통일)
                AutoResizingTextLabel(
                    text = stringResource(R.string.tab05_notifications),
                    fontSize = 12.sp, // ★ 여기를 12.sp로 수정
                    color = Color.Black,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. 고객 문의 (Support)
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

                // [FIX] fontSize를 12.sp로 변경 (Notifications와 키 맞춤)
                AutoResizingTextLabel(
                    text = stringResource(R.string.tab05_customer_support),
                    fontSize = 12.sp, // ★ 여기를 12.sp로 수정
                    color = Color.Black,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 3. 추천앱 (Apps)
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

                // [FIX] fontSize를 12.sp로 변경 (Notifications와 키 맞춤)
                AutoResizingTextLabel(
                    text = stringResource(R.string.tab05_recommended_apps),
                    fontSize = 12.sp, // ★ 여기를 12.sp로 수정
                    color = Color(0xFFBDBDBD),
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
            SimpleAboutRow(title = stringResource(id = R.string.about_version_info), onClick = {}, trailing = {
                Text(text = versionInfo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            })
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
    }

    // 고객 문의 바텀 시트
    if (showCustomerFeedbackSheet) {
        CustomerFeedbackBottomSheet(
            onDismiss = { viewModel.setShowCustomerFeedbackSheet(false) },
            onSubmit = { category, content, email ->
                // Firebase 전송은 BottomSheet 내부에서 처리됨
                // 여기서는 추가 로깅이나 분석 이벤트만 기록 가능
                Log.d("Tab05", "Feedback submitted - Category: $category")
            }
        )
    }
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