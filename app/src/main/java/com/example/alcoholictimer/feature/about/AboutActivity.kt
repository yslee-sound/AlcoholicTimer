package com.sweetapps.alcoholictimer.feature.about

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sweetapps.alcoholictimer.BuildConfig
import com.sweetapps.alcoholictimer.core.ui.BaseActivity
import com.sweetapps.alcoholictimer.R
import com.sweetapps.alcoholictimer.core.ui.LocalSafeContentPadding
import com.sweetapps.alcoholictimer.core.ui.AdmobBanner
import com.sweetapps.alcoholictimer.core.ui.DebugAdHelper

class AboutActivity : BaseActivity() {
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getScreenTitle(): String = getString(R.string.about_title)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var hideBannerAd by remember { mutableStateOf(DebugAdHelper.isBannerHidden(this@AboutActivity)) }

            // 뒤로가기 버튼: 메인 홈(Start/Run)으로 이동
            BackHandler(enabled = true) {
                navigateToMainHome()
            }

            BaseScreen(bottomAd = { AdmobBanner() }) {
                AboutListScreen(
                    onOpenLicenses = { openLicenses() },
                    onVersionLongClick = {
                        if (BuildConfig.DEBUG) {
                            val newState = DebugAdHelper.toggleBannerHidden(this@AboutActivity)
                            hideBannerAd = newState
                            val message = if (newState) {
                                "배너 광고 숨김 (디버그 전용)"
                            } else {
                                "배너 광고 표시"
                            }
                            Toast.makeText(this@AboutActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun openLicenses() {
        startActivity(Intent(this, AboutLicensesActivity::class.java))
        overridePendingTransition(0, 0)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AboutListScreen(
    onOpenLicenses: () -> Unit,
    onVersionLongClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val versionName = remember {
        try {
            @Suppress("DEPRECATION")
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            pi.versionName ?: "-"
        } catch (_: Throwable) {
            "-"
        }
    }

    val safePadding = LocalSafeContentPadding.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(safePadding)
    ) {
        // 흰색 카드 안에 리스트 아이템 묶기
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 0.dp,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 1) 버전 정보 (정보 표시 행) - 길게 클릭 시 배너 광고 토글 (디버그 빌드만)
                SimpleListRow(
                    title = stringResource(id = R.string.about_version_info),
                    trailing = {
                        Text(
                            text = versionName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onLongClick = onVersionLongClick
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)

                // 2) 오픈 라이선스 고지 (탭 시 이동)
                SimpleListRow(
                    title = stringResource(id = R.string.about_open_license_notice),
                    onClick = onOpenLicenses
                )
            }
        }
        // 아래 여백
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
