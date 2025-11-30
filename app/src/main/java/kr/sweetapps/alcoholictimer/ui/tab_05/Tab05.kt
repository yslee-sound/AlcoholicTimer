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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.sweetapps.alcoholictimer.BuildConfig
import kr.sweetapps.alcoholictimer.MainApplication
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.core.ui.BackTopBar
import kr.sweetapps.alcoholictimer.core.ui.theme.LocalDimens
import kr.sweetapps.alcoholictimer.ui.tab_04.SettingsMenuWithSwitch
import kr.sweetapps.alcoholictimer.ui.tab_04.SimpleAboutRow

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
    showBack: Boolean = false,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val scrollState = rememberScrollState()

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
                // 더 넉넉한 상하 여백을 적용해 스크린샷과 유사하게 만듭니다
                .padding(start = dims.spacing.md, end = dims.spacing.md, top = dims.spacing.lg, bottom = dims.spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(dims.sizes.profileImage)
                    .clip(CircleShape)
                    .background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(dims.sizes.iconLarge), tint = Color(0xFFE0E0E0))
            }
            Spacer(modifier = Modifier.width(dims.spacing.sm))
            Text(text = stringResource(id = R.string.default_nickname), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.weight(1f))
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.Black)
        }

        // App Rating Button (simple)
        Column(modifier = Modifier.padding(horizontal = dims.padding.large)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dims.component.buttonHeight)
                    .shadow(6.dp, RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.linearGradient(listOf(Color(0xFF8A6CFF), Color(0xFF6F4EF6))),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        val packageName = BuildConfig.APPLICATION_ID
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
                Text(text = "앱 평가하기", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(dims.spacing.lg))

        // --- Row of 4 action buttons (알림 / 고객 문의 / 앱 공유하기 / 추천앱)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dims.padding.large),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val itemModifier = Modifier.weight(1f)

            // 아이콘은 테두리/배경 없이 아이콘만 표시
            Column(modifier = itemModifier.padding(vertical = dims.spacing.sm), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = Color.Black, modifier = Modifier.size(dims.sizes.icon))
                Spacer(modifier = Modifier.height(dims.spacing.sm))
                Text(text = "알림", fontSize = 15.sp, color = Color.Black, fontWeight = FontWeight.Normal)
            }

            Column(modifier = itemModifier.padding(vertical = dims.spacing.sm), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color.Black, modifier = Modifier.size(dims.sizes.icon))
                Spacer(modifier = Modifier.height(dims.spacing.sm))
                Text(text = "고객 문의", fontSize = 15.sp, color = Color.Black, fontWeight = FontWeight.Normal)
            }

            Column(modifier = itemModifier.padding(vertical = dims.spacing.sm), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.Black, modifier = Modifier.size(dims.sizes.icon))
                Spacer(modifier = Modifier.height(dims.spacing.sm))
                Text(text = "앱 공유하기", fontSize = 15.sp, color = Color.Black, fontWeight = FontWeight.Normal)
            }

            Column(modifier = itemModifier.padding(vertical = dims.spacing.sm), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color.Black, modifier = Modifier.size(dims.sizes.icon))
                Spacer(modifier = Modifier.height(dims.spacing.sm))
                Text(text = "추천앱", fontSize = 15.sp, color = Color.Black, fontWeight = FontWeight.Normal)
            }
        }

        Spacer(modifier = Modifier.height(dims.spacing.lg))

        // Thick section divider (light surface variant) - show light gray as in reference UI
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(dims.divider.sectionThickness)
            .background(dims.divider.lightColor))

        Spacer(modifier = Modifier.height(dims.spacing.xs))

        // Settings / About list
        Column(modifier = Modifier.background(Color.White)) {
            // 리스트 항목 사이에 thin divider를 추가하여 구분합니다.
            SimpleAboutRow(title = stringResource(id = R.string.about_version_info), onClick = {}, trailing = {
                Text(text = versionInfo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            })
            Box(modifier = Modifier.fillMaxWidth().height(dims.divider.thin).background(dims.divider.lightColor))

            // Privacy
            SimpleAboutRow(title = stringResource(id = R.string.document_title_privacy), onClick = onPrivacyClick, trailing = {
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFBBBBBB), modifier = Modifier.size(20.dp))
            })
            Box(modifier = Modifier.fillMaxWidth().height(dims.divider.thin).background(dims.divider.lightColor))

            // Open Source License
            SimpleAboutRow(title = stringResource(id = R.string.about_open_license_notice), onClick = onLicenseClick, trailing = {
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFBBBBBB), modifier = Modifier.size(20.dp))
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
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFBBBBBB), modifier = Modifier.size(20.dp))
                })
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AboutScreenPreview() {
    AboutScreen(onNavigateLicenses = {}, onNavigateDebug = {})
}
