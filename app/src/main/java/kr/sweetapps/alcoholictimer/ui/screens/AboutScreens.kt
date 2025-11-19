package kr.sweetapps.alcoholictimer.ui.screens

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import kr.sweetapps.alcoholictimer.constants.UiConstants

import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.core.ui.BackTopBar
import kr.sweetapps.alcoholictimer.core.util.CurrencyManager

@Composable
fun AboutScreen(
    onNavigateLicenses: () -> Unit,
    onNavigateEditNickname: () -> Unit = {},
    onNavigateCurrencySettings: () -> Unit = {},
    showBack: Boolean = false,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val sp = remember { context.getSharedPreferences("user_settings", Context.MODE_PRIVATE) }
    var nickname by remember { mutableStateOf(sp.getString("nickname", context.getString(R.string.default_nickname)) ?: context.getString(R.string.default_nickname)) }

    // 로컬: 닉네임 섹션 아래 간격 — AboutScreen 내부에서만 제어
    val nicknameDividerSpacing = 0.dp

    // SharedPreferences listener로 닉네임 실시간 반영
    DisposableEffect(sp) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "nickname") {
                nickname = sp.getString("nickname", context.getString(R.string.default_nickname)) ?: context.getString(R.string.default_nickname)
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
            BackTopBar(title = nickname.ifEmpty { context.getString(R.string.default_nickname) }, onBack = onBack)
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
                .padding(start = 10.dp, top = if (showBack) 12.dp else 20.dp, end = 16.dp, bottom = if (showBack) 12.dp else 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                Image(painter = painterResource(id = R.drawable.ic_user_circle), contentDescription = null, modifier = Modifier.size(56.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = nickname.ifEmpty { "로그인" },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Image(painter = painterResource(id = R.drawable.ic_caret_right), contentDescription = null, modifier = Modifier.size(20.dp))
        }

        // 프로필 섹션 아래 경계선 (이미지처럼 바로 아래에 라인)
        Spacer(modifier = Modifier.height(nicknameDividerSpacing))
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 10.dp)

        // 통화 설정 단일 버튼 (오른쪽 화살표 추가) - 클릭 시 ripple 제거
        SimpleAboutRow(
            title = stringResource(id = R.string.settings_currency),
            onClick = onNavigateCurrencySettings,
            trailing = {
                Image(
                    painter = painterResource(id = R.drawable.ic_caret_right),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)

        // 버전 정보
        SimpleAboutRow(
            title = stringResource(id = R.string.about_version_info),
            trailing = {
                val versionName = try {
                    @Suppress("DEPRECATION")
                    val pi = context.packageManager.getPackageInfo(context.packageName, 0)
                    pi.versionName ?: "-"
                } catch (_: Throwable) { "-" }
                Text(
                    text = versionName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)

        // Open Source License - 오른쪽 화살표 표시
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
    }
}

@Composable
fun AboutLicensesScreen(onBack: () -> Unit = {}) {
    // assets/LICENSE.txt 파일을 읽어 전체 텍스트를 그대로 표시합니다.
    val context = LocalContext.current
    val licenseText = remember {
        try {
            context.assets.open("LICENSE.txt").bufferedReader().use { it.readText() }
        } catch (_: Throwable) {
            "라이선스 파일을 읽을 수 없습니다."
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        BackTopBar(title = stringResource(id = R.string.about_open_license_notice), onBack = onBack, titleColor = Color.Black)

        // 파일 전체 텍스트를 스크롤 가능한 컬럼에 표시
        rememberScrollState()
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
        ) {
            Text(text = licenseText, color = Color.Black, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// 간단한 데이터 클래스: 확장 시 리스트 관리에 유리
data class LicenseData(
    val name: String,
    val author: String,
    val sourceUrl: String,
    val license: String,
    val licenseUrl: String,
    val changes: String
)

@Composable
fun LicenseItem(
     name: String,
     author: String,
     sourceUrl: String,
     license: String,
     licenseUrl: String,
    changes: String
 ) {
     val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        // 라이선스 이름 (제목)
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 저작자 (평문)
        Text(
            text = "저작자: $author",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(6.dp))

        // 출처: URL 전체를 노출하지 않고 '링크' 라벨로 표시
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "출처: ",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )
            Text(
                text = "링크",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(sourceUrl))
                    context.startActivity(intent)
                }
            )
        }
        Spacer(modifier = Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "라이선스: ",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )
            Text(
                text = license,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(licenseUrl))
                    context.startActivity(intent)
                }
            )
        }
        Spacer(modifier = Modifier.height(6.dp))

        // 변경 사항 (풀어쓴 문장)
        Text(
            text = "변경 사항: $changes",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black
        )
    }
     HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
}

@Composable
fun CurrencySettingsScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    var selectedCurrency by remember {
        mutableStateOf(CurrencyManager.getSelectedCurrency(context).code)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top bar for currency settings: overlay so title aligns with list items (start = 16.dp)
        Box(modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 8.dp, bottom = 4.dp)) {
            Box(modifier = Modifier.align(Alignment.CenterStart).width(UiConstants.BackIconTouchArea).padding(start = UiConstants.BackIconInnerPadding), contentAlignment = Alignment.CenterStart) {
                val noRipple = remember { MutableInteractionSource() }
                Image(painter = painterResource(id = R.drawable.ic_caret_left), contentDescription = stringResource(id = R.string.cd_navigate_back), modifier = Modifier.size(24.dp).clickable(indication = null, interactionSource = noRipple) { onBack() })
            }
            Text(
                text = stringResource(id = R.string.settings_currency),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = UiConstants.BackIconStartPadding)
            )
        }

        CurrencyManager.supportedCurrencies.forEachIndexed { index, currency ->
            CurrencyOptionRow(
                isSelected = selectedCurrency == currency.code,
                label = stringResource(currency.nameResId),
                onSelected = {
                    selectedCurrency = currency.code
                    CurrencyManager.saveCurrency(context, currency.code)
                }
            )
            if (index < CurrencyManager.supportedCurrencies.size - 1) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
            }
        }
    }
}

@Composable
private fun CurrencyOptionRow(
    isSelected: Boolean,
    label: String,
    onSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onSelected() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = colorResource(id = R.color.color_accent_blue),
                unselectedColor = colorResource(id = R.color.color_radio_unselected)
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = if (isSelected) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold) else MaterialTheme.typography.bodyMedium,
            color = if (isSelected) colorResource(id = R.color.color_indicator_days) else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SimpleAboutRow(
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
        .padding(horizontal = 16.dp, vertical = 16.dp)
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
