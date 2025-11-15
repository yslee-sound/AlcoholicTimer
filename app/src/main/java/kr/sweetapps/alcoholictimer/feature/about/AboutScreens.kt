package kr.sweetapps.alcoholictimer.feature.about

import android.content.SharedPreferences
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kr.sweetapps.alcoholictimer.R

@Composable
fun AboutScreen(
    onNavigateLicenses: () -> Unit,
    onNavigateEditNickname: () -> Unit = {},
    onNavigateCurrencySettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val sp = remember { context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE) }
    var nickname by remember { mutableStateOf(sp.getString("nickname", context.getString(R.string.default_nickname)) ?: context.getString(R.string.default_nickname)) }

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
            .background(androidx.compose.ui.graphics.Color.White)
    ) {
        // 상단 프로필 블록: 아바타 + 닉네임 (클릭 시 닉네임 편집 화면으로 이동)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { onNavigateEditNickname() }
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 박스 배경을 제거하고 아이콘만 표시 (터치 영역은 동일하게 유지)
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
            Spacer(modifier = Modifier.width(5.dp)) // 12.dp
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                // 아이콘과 글자 사이 간격을 좁히고 텍스트를 조금 키움
                Text(
                    text = nickname.ifEmpty { "로그인" },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Image(
                    painter = painterResource(id = R.drawable.ic_caret_right),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 프로필 섹션 아래 경계선 (이미지처럼 바로 아래에 라인)
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)

        // 통화 설정 단일 버튼 (오른쪽 화살표 추가)
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.White)
    ) {
        // Top bar with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 8.dp, bottom = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onBack() }) {
                Image(
                    painter = painterResource(id = R.drawable.ic_caret_left),
                    contentDescription = stringResource(id = R.string.cd_navigate_back),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.about_open_license_notice),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // 목록형 라이선스 항목 (흰 배경)
        Column(modifier = Modifier.fillMaxSize()) {
            // 첫 번째: 아이콘/저작권 정보 같은 항목들을 SimpleAboutRow로 나열
            SimpleAboutRow(title = stringResource(id = R.string.about_value_icon_name), trailing = null)
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
            SimpleAboutRow(title = stringResource(id = R.string.about_value_icon_author), trailing = null)
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
            SimpleAboutRow(title = stringResource(id = R.string.about_value_source_url), trailing = {
                IconButton(onClick = { /* copy or open link */ }) {
                    Image(painter = painterResource(id = R.drawable.ic_caret_right), contentDescription = null, modifier = Modifier.size(18.dp))
                }
            })
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
            SimpleAboutRow(title = stringResource(id = R.string.about_value_license_cc_by), trailing = null)
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
            SimpleAboutRow(title = stringResource(id = R.string.about_value_change_desc), trailing = null)
        }
    }
}

@Composable
fun CurrencySettingsScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    var selectedCurrency by remember {
        mutableStateOf(kr.sweetapps.alcoholictimer.core.util.CurrencyManager.getSelectedCurrency(context).code)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.White)
    ) {
        // Top bar with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 8.dp, bottom = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onBack() }) {
                Image(
                    painter = painterResource(id = R.drawable.ic_caret_left),
                    contentDescription = stringResource(id = R.string.cd_navigate_back),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.settings_currency),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        kr.sweetapps.alcoholictimer.core.util.CurrencyManager.supportedCurrencies.forEachIndexed { index, currency ->
            CurrencyOptionRow(
                isSelected = selectedCurrency == currency.code,
                label = stringResource(currency.nameResId),
                onSelected = {
                    selectedCurrency = currency.code
                    kr.sweetapps.alcoholictimer.core.util.CurrencyManager.saveCurrency(context, currency.code)
                }
            )
            if (index < kr.sweetapps.alcoholictimer.core.util.CurrencyManager.supportedCurrencies.size - 1) {
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
            .clickable { onSelected() }
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
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
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
