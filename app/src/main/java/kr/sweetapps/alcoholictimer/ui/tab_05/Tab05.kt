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
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import kr.sweetapps.alcoholictimer.ui.tab_04.SettingsMenuWithSwitch
import kr.sweetapps.alcoholictimer.ui.tab_04.SimpleAboutRow
import kr.sweetapps.alcoholictimer.ui.tab_05.components.CustomerFeedbackBottomSheet
import kr.sweetapps.alcoholictimer.ui.tab_05.viewmodel.Tab05ViewModel
import kr.sweetapps.alcoholictimer.ui.main.Screen

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

    // preview values or real state from UMP
    val isPersonalizedAdsAllowed: Boolean
    val versionInfo: String
    val onPrivacyClick: () -> Unit
    val onLicenseClick: () -> Unit
    val onAdsClick: () -> Unit
    val onDebugClick: () -> Unit
    val showDebugMenu: Boolean

    if (isInPreview) {
        isPersonalizedAdsAllowed = true
        versionInfo = "1.0.0-preview"
        onPrivacyClick = {}
        onLicenseClick = {}
        onAdsClick = {}
        onDebugClick = {}
        showDebugMenu = true
    } else {
        val app = context.applicationContext as? MainApplication
        val umpConsentManager = app?.umpConsentManager

        val isPrivacyOptionsRequiredState by umpConsentManager?.isPrivacyOptionsRequired?.collectAsState(initial = false) ?: remember { mutableStateOf(false) }
        val isPersonalizedAdsAllowedState by umpConsentManager?.isPersonalizedAdsAllowed?.collectAsState(initial = false) ?: remember { mutableStateOf(false) }

        isPersonalizedAdsAllowed = isPersonalizedAdsAllowedState
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
                    umpConsentManager.showPrivacyOptionsForm(activity) { exception ->
                        // showPrivacyOptionsForm provides a non-null Exception on error, show it to user
                        Toast.makeText(context, exception.toString(), Toast.LENGTH_LONG).show()
                    }
                } catch (t: Throwable) {
                    Log.e("AboutScreen", "showPrivacyOptionsForm failed", t)
                }
            } else {
                Log.w("AboutScreen", "Activity or umpConsentManager null; cannot show privacy options")
            }
        }
        onDebugClick = { onNavigateDebug() }
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
                        brush = Brush.linearGradient(listOf(Color(0xFF8A6CFF), Color(0xFF6F4EF6))),
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
                    Text(text = "앱 평가하기", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp)) // 앱 평가하기 버튼 아래

        // --- Row of 3 action buttons (알림 / 고객 문의/제안 / 추천앱)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 알림
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
                Text(
                    text = "알림",
                    fontSize = 14.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Normal
                )
            }

            // 고객 문의/제안
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
                Text(
                    text = "고객 문의/제안",
                    fontSize = 14.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Normal
                )
            }

            // 추천앱 (비활성화)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = dims.spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.thumbsup),
                    contentDescription = null,
                    tint = Color(0xFFBDBDBD), // 비활성화 회색
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "추천앱",
                    fontSize = 14.sp,
                    color = Color(0xFFBDBDBD), // 비활성화 회색
                    fontWeight = FontWeight.Normal
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
            SimpleAboutRow(title = stringResource(id = R.string.about_open_license_notice), onClick = onLicenseClick, trailing = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_caret_right),
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(20.dp)
                )
            })
            Box(modifier = Modifier.fillMaxWidth().height(dims.divider.thin).background(dims.divider.lightColor))

            // Personalized Ads (switch) - always visible so user can open privacy options at any time
            SettingsMenuWithSwitch(
                title = "Personalized Ads",
                checked = isPersonalizedAdsAllowed,
                onClick = onAdsClick
            )
            Box(modifier = Modifier.fillMaxWidth().height(dims.divider.thin).background(dims.divider.lightColor))

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
