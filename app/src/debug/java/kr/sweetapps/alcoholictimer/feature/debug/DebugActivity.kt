package kr.sweetapps.alcoholictimer.feature.debug

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kr.sweetapps.alcoholictimer.core.ui.BaseActivity
import kr.sweetapps.alcoholictimer.core.ui.LocalSafeContentPadding
import kr.sweetapps.alcoholictimer.core.ui.AdmobBanner
import kr.sweetapps.alcoholictimer.core.ui.DebugAdHelper

class DebugActivity : BaseActivity() {
    override fun getScreenTitleResId(): Int? = null
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getScreenTitle(): String = "디버깅 모드"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseScreen(bottomAd = { AdmobBanner() }) {
                DebugScreen()
            }
        }
    }
}

@Composable
private fun DebugScreen() {
    val safePadding = LocalSafeContentPadding.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(safePadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BannerAdToggleRow()
            }
        }
        Text(
            text = "디버그 전용 설정입니다. 릴리스 빌드에는 포함되지 않습니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BannerAdToggleRow() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val hiddenState by DebugAdHelper.rememberBannerHiddenState(context)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = "배너 광고 숨기기", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (hiddenState) "현재: 숨김" else "현재: 표시",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = hiddenState,
            onCheckedChange = { DebugAdHelper.setBannerHidden(context, it) }
        )
    }
}
