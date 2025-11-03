package com.sweetapps.alcoholictimer.feature.debug

import android.os.Bundle
import androidx.activity.compose.setContent
import com.sweetapps.alcoholictimer.core.ui.BaseActivity
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sweetapps.alcoholictimer.core.ui.AdmobBanner
import com.sweetapps.alcoholictimer.core.ui.DebugAdHelper

class DebugActivity : BaseActivity() {
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getScreenTitle(): String = "디버깅 모드"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseScreen(showBackButton = true, bottomAd = { AdmobBanner() }) { DebugScreen() }
        }
    }
}

@Composable
private fun DebugScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val bannerHiddenState by DebugAdHelper.rememberBannerHiddenState(context)

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = "디버그 전용 설정", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // 배너 광고 on/off 스위치 (on=보이기, off=숨김)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "배너 광고", modifier = Modifier.weight(1f))
            val checked = !bannerHiddenState
            Switch(
                checked = checked,
                onCheckedChange = { isOn ->
                    // isOn=true: 보이기 -> hidden=false
                    DebugAdHelper.setBannerHidden(context, hidden = !isOn)
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        val statusText = if (bannerHiddenState) "현재 상태: 숨김" else "현재 상태: 표시"
        Text(text = statusText, style = MaterialTheme.typography.bodyMedium)
    }
}
