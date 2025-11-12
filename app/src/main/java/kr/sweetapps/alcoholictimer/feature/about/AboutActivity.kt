package kr.sweetapps.alcoholictimer.feature.about

// Legacy AboutActivity removed. Use Compose route Screen.About.

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kr.sweetapps.alcoholictimer.BuildConfig
import kr.sweetapps.alcoholictimer.core.ui.BaseActivity
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.core.ui.LocalSafeContentPadding

class AboutActivity : BaseActivity() {
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getScreenTitle(): String = getString(R.string.about_title)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 뒤로가기 버튼: 메인 홈(Start/Run)으로 이동
            BackHandler(enabled = true) { navigateToMainHome() }

            // AdmobBanner centralized in MainActivity BaseScaffold during Phase-1 migration
            BaseScreen(content = {
                AboutListScreen(
                    onOpenLicenses = { openLicenses() },
                    onOpenDebug = { openDebug() }
                )
            })
        }
    }

    private fun openLicenses() {
        // No-op
    }

    private fun openDebug() {
        // No-op (Compose NavHost로 통합됨)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AboutListScreen(
    onOpenLicenses: () -> Unit,
    onOpenDebug: () -> Unit = {}
) {
    val context = LocalContext.current
    val versionName = try {
        @Suppress("DEPRECATION")
        val pi = context.packageManager.getPackageInfo(context.packageName, 0)
        pi.versionName ?: "-"
    } catch (_: Throwable) { "-" }

    val safePadding = LocalSafeContentPadding.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(safePadding)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 0.dp,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 1) 버전 정보 (정보 표시 행) - 길게 클릭 동작 제거
                SimpleListRow(
                    title = stringResource(id = R.string.about_version_info),
                    trailing = {
                        Text(
                            text = versionName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)

                // 2) 오픈 라이선스 고지 (탭 시 이동)
                SimpleListRow(
                    title = stringResource(id = R.string.about_open_license_notice),
                    onClick = onOpenLicenses
                )

                // 3) 디버그 모드 (디버그 빌드에서만 노출)
                if (BuildConfig.DEBUG) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                    SimpleListRow(
                        title = "디버그 모드",
                        onClick = onOpenDebug
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SimpleListRow(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val rowModifier = if (onClick != null || onLongClick != null) {
        modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onClick?.invoke() },
                onLongClick = { onLongClick?.invoke() }
            )
            .padding(horizontal = 16.dp, vertical = 16.dp)
    } else {
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) trailing()
    }
}
