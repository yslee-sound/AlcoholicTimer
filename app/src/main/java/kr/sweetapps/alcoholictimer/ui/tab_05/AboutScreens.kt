package kr.sweetapps.alcoholictimer.ui.tab_05

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kr.sweetapps.alcoholictimer.MainApplication
import kr.sweetapps.alcoholictimer.consent.UmpConsentManager
import androidx.core.content.edit
import kr.sweetapps.alcoholictimer.constants.UiConstants
import kr.sweetapps.alcoholictimer.core.ui.BackTopBar
import kr.sweetapps.alcoholictimer.core.util.CurrencyManager
import java.security.MessageDigest
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.BuildConfig
import android.content.ContextWrapper
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.identifier.AdvertisingIdClient

// UMP SDK may be absent in some build variants; avoid direct import to prevent unresolved references.
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import kr.sweetapps.alcoholictimer.ui.tab_04.screens.CurrencySettingsScreen as Tab04CurrencySettingsScreen

// Added imports for Compose previews and inspection mode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalInspectionMode

// Helper: find enclosing Activity from Context (safe for themed/context-wrapped Compose contexts)
private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}


// debug helper: fetch Advertising ID and log MD5 hash (used to get test device hashed id)
private fun md5Hex(input: String): String {
    return try {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        digest.joinToString("") { String.format("%02X", it) }
    } catch (_: Exception) {
        ""
    }
}

private fun logAdvertisingIdHash(context: Context) {
    // Debug-only helper: logs MD5 of Advertising ID when user opted into ad personalization and UMP allows ad requests.
    if (!BuildConfig.DEBUG) return

    val activity = context.findActivity()
    if (activity == null) {
        Log.d("AboutScreen", "Skipping AdvertisingId fetch: context is not Activity")
        return
    }

    val sp = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
    val optedIn = try { sp.getBoolean("ad_personalization_opt_in", false) } catch (_: Throwable) { false }
    if (!optedIn) {
        Log.d("AboutScreen", "Skipping AdvertisingId fetch: user has not opted in to ad personalization")
        return
    }

    val consentInformation = runCatching {
        val mainApp = activity.application as MainApplication
        mainApp.umpConsentManager
    }.getOrNull()

    val canRequestAds = try {
        consentInformation?.isPersonalizedAdsAllowed?.value ?: false
    } catch (_: Throwable) { false }

    if (!canRequestAds) {
        Log.d("AboutScreen", "Skipping AdvertisingId fetch due to canRequestAds=$canRequestAds")
        return
    }

    Thread {
        try {
            val info = AdvertisingIdClient.getAdvertisingIdInfo(context)
            val adId = info?.id ?: ""
            if (adId.isNotEmpty()) {
                val hash = md5Hex(adId)
                Log.d("AboutScreen", "AdvertisingId MD5 (test device hash): $hash")
            } else {
                Log.d("AboutScreen", "AdvertisingId empty")
            }
        } catch (_: Throwable) {
            Log.w("AboutScreen", "Failed to fetch AdvertisingId")
        }
    }.start()
}

// register test device hashed id (debug helper): fetch Advertising ID, compute MD5, save to UMP prefs and copy to clipboard
private fun registerTestDeviceHash(context: Context) {
    if (!BuildConfig.DEBUG) return
    val activity = context as? Activity
    if (activity == null) {
        android.util.Log.d("AboutScreen", "registerTestDeviceHash: context is not Activity")
        return
    }

    Thread {
        try {
            val info = AdvertisingIdClient.getAdvertisingIdInfo(context)
            val adId = info.id ?: ""
            if (adId.isNotEmpty()) {
                val hash = md5Hex(adId)
                try {
                    val sp = context.getSharedPreferences("ump_prefs", Context.MODE_PRIVATE)
                    sp.edit().putString("test_device_hash", hash).apply()
                    android.util.Log.d("AboutScreen", "Saved test device hash: $hash")
                } catch (_: Throwable) {}

            } else {

            }
        } catch (t: Throwable) {
            android.util.Log.w("AboutScreen", "registerTestDeviceHash failed: ${t.message}")
        }
    }.start()
}

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
    val application = context.applicationContext as MainApplication
    val umpConsentManager = application.umpConsentManager
    // Capture inspection mode at composition time to avoid reading a composition local from a non-composable lambda
    val isInInspection = LocalInspectionMode.current

    val isPrivacyOptionsRequired by umpConsentManager.isPrivacyOptionsRequired.collectAsState()
    val isPersonalizedAdsAllowed by umpConsentManager.isPersonalizedAdsAllowed.collectAsState()

    // 디버그 환경에서는 광고 ID의 MD5 해시를 로그에 남겨 UMP 테스트 해시로 사용할 수 있도록 합니다.
    DisposableEffect(Unit) {
        // Avoid running debug-only network/ads code in Android Studio preview/inspection mode
        if (BuildConfig.DEBUG && !isInInspection) {
            logAdvertisingIdHash(context)
            // Also attempt to register and copy test device hash to prefs/clipboard so UMP debug form appears
            try { registerTestDeviceHash(context) } catch (_: Throwable) {}
        }
        onDispose { }
    }

    val sp = remember { context.getSharedPreferences("user_settings", Context.MODE_PRIVATE) }
    val nicknameState = remember {
        mutableStateOf(
            sp.getString("nickname", context.getString(R.string.default_nickname)) ?: context.getString(R.string.default_nickname)
        )
    }

    // SharedPreferences listener로 닉네임 실시간 반영
    DisposableEffect(sp) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "nickname") {
                nicknameState.value = sp.getString("nickname", context.getString(R.string.default_nickname)) ?: context.getString(R.string.default_nickname)
            }
        }
        sp.registerOnSharedPreferenceChangeListener(listener)
        onDispose { sp.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 상단 블록: 뒤로가기 표시 여부에 따라 공통 BackTopBar를 렌더링하거나 아무 것도 렌더링하지 않습니다.
        if (showBack) {
            BackTopBar(
                title = nicknameState.value.ifEmpty { context.getString(R.string.default_nickname) },
                onBack = onBack
            )
        }

        // 프로필 클릭 영역: 백버튼과 제목과 별도로 눌러서 닉네임 편집으로 이동하도록 유지
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onNavigateEditNickname() }
                // top/bottom padding은 상단에 BackTopBar가 렌더링되는 경우와 메인 탭(타이틀 없음)인 경우에 다르게 적용
                .padding(
                    start = 10.dp,
                    top = if (showBack) 12.dp else 20.dp,
                    end = 16.dp,
                    bottom = if (showBack) 12.dp else 20.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(56.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_user_circle),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = nicknameState.value.ifEmpty { stringResource(id = R.string.login_label) },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Image(
                painter = painterResource(id = R.drawable.ic_caret_right),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }

        // 프로필 섹션 아래 경계선 (이미지처럼 바로 아래에 라인)
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 10.dp)

        // 버전 정보
        SimpleAboutRow(
            title = stringResource(id = R.string.about_version_info),
            trailing = {
                val versionName = try {
                    @Suppress("DEPRECATION")
                    val pi = context.packageManager.getPackageInfo(context.packageName, 0)
                    pi.versionName ?: "-"
                } catch (_: Throwable) {
                    "-"
                }
                Text(
                    text = versionName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
        
        // 개인정보 처리방침
        SimpleAboutRow(
            title = stringResource(id = R.string.document_title_privacy),
            onClick = onNavigatePrivacy,
            trailing = {
                Image(
                    painter = painterResource(id = R.drawable.ic_caret_right),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)

        // 오픈 소스 라이선스
        SimpleAboutRow(
            title = stringResource(id = R.string.about_open_license_notice),
            onClick = onNavigateLicenses,
            trailing = {
                Image(
                    painter = painterResource(id = R.drawable.ic_caret_right),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)

        if (isPrivacyOptionsRequired) {
            SettingsMenuWithSwitch(
                title = "Personalized Ads",
                checked = isPersonalizedAdsAllowed,
                onClick = {
                    val activity = context as? Activity
                    if (activity != null) {
                        umpConsentManager.showPrivacyOptionsForm(activity) { exception ->
                            // Handle error
                        }
                    }
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
        }
        if (BuildConfig.DEBUG) {
            SimpleAboutRow(
                title = "Debug",
                onClick = onNavigateDebug,
                trailing = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_caret_right),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
        }
    }
}

@Composable
fun SettingsMenuWithSwitch(
    title: String,
    checked: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() } 
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title)
        Switch(
            checked = checked,
            onCheckedChange = null 
        )
    }
}

@Composable
fun AboutLicensesScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val licenseText = remember {
        try {
            context.assets.open("raw/LICENSE.txt").bufferedReader().use { it.readText() }
        } catch (_: Throwable) {
            "라이선스 파일을 읽을 수 없습니다."
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        BackTopBar(
            title = stringResource(id = R.string.about_open_license_notice),
            onBack = onBack,
            titleColor = Color.Black
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = licenseText,
                color = Color.Black,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun CurrencySettingsScreen(onBack: () -> Unit = {}) {
    // Delegate actual UI to tab_04 implementation so tab_04/screens manages currency UI going forward.
    Tab04CurrencySettingsScreen(onBack = onBack)
}


@Composable
fun SimpleAboutRow(
    title: String,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val base = Modifier
        .fillMaxWidth()
        .then(if (onClick != null) Modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onClick() } else Modifier)
        .height(56.dp) // 모든 항목 높이를 고정하여 일관성 유지
        .padding(horizontal = 16.dp)
    Row(modifier = base, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) trailing()
    }
}

// Previews: show screens using actual code paths without injecting fake data. Lambdas are no-ops.
@Preview(showBackground = true)
@Composable
fun Preview_AboutScreen() {
    AboutScreen(
        onNavigateLicenses = {},
        onNavigatePrivacy = {},
        onNavigateEditNickname = {},
        onNavigateCurrencySettings = {},
        onNavigateDebug = {},
        showBack = true,
        onBack = {}
    )
}

@Preview(showBackground = true)
@Composable
fun Preview_AboutLicensesScreen() {
    AboutLicensesScreen(onBack = {})
}

@Preview(showBackground = true)
@Composable
fun Preview_CurrencySettingsScreen() {
    CurrencySettingsScreen(onBack = {})
}

@Preview(showBackground = true)
@Composable
fun Preview_SimpleAboutRow() {
    SimpleAboutRow(title = "미리보기 제목")
}
