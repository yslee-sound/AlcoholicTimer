package kr.sweetapps.alcoholictimer.feature.about

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kr.sweetapps.alcoholictimer.BuildConfig
import kr.sweetapps.alcoholictimer.R

@Composable
fun AboutScreen(
    onNavigateLicenses: () -> Unit,
    showDebug: Boolean = BuildConfig.DEBUG,
    onNavigateDebug: () -> Unit = {},
    onNavigateEditNickname: () -> Unit = {}
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
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 0.dp,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 닉네임 편집 행 (상단 고정)
                SimpleAboutRow(
                    title = stringResource(id = R.string.profile_nickname_label) + ": " + nickname,
                    onClick = onNavigateEditNickname
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
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
                SimpleAboutRow(
                    title = stringResource(id = R.string.about_open_license_notice),
                    onClick = onNavigateLicenses
                )
                if (showDebug) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                    SimpleAboutRow(title = "디버그 모드", onClick = onNavigateDebug)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun AboutLicensesScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 0.dp,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.about_open_license_notice),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.about_notice_compliance),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
