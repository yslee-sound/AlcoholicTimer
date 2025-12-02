package kr.sweetapps.alcoholictimer.ui.tab_05.screens.debug

import android.widget.Toast
import android.util.Log
import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.core.ui.BackTopBar
import androidx.compose.ui.platform.LocalContext
import kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager

// Helper: get Activity from Context
private fun ContextToActivity(context: android.content.Context): Activity? {
    var ctx: android.content.Context? = context
    while (ctx is android.content.ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun DebugScreen(
    viewModel: DebugScreenViewModel = viewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column {
        BackTopBar(
            title = stringResource(id = R.string.debug_menu_title),
            onBack = onBack
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "맞춤형 광고 재설정",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.resetConsent()
                        Toast
                            .makeText(context, "광고 동의 상태가 초기화되었습니다.", Toast.LENGTH_SHORT)
                            .show()
                        try {
                            // [수정] MainApplication에서 umpConsentManager 인스턴스 가져오기
                            val app = context.applicationContext as? kr.sweetapps.alcoholictimer.MainApplication
                            app?.umpConsentManager?.resetConsent(context.applicationContext)
                            Log.d("DebugScreen", "Direct umpConsentManager.resetConsent invoked from UI")
                        } catch (_: Throwable) { Log.d("DebugScreen", "umpConsentManager.resetConsent failed") }
                        try {
                            AppOpenAdManager.preload(context.applicationContext)
                            Log.d("DebugScreen", "Triggered AppOpenAdManager.preload from debug UI")
                        } catch (_: Throwable) { Log.d("DebugScreen", "AppOpenAdManager.preload failed") }
                    }
                    .padding(vertical = 8.dp)
            )
            DebugSwitch(title = "기능 1", checked = uiState.switch1, onCheckedChange = { viewModel.setSwitch(1, it) })
            DebugSwitch(title = "데모 모드", checked = uiState.demoMode, onCheckedChange = { viewModel.setSwitch(2, it) })
            DebugSwitch(title = "UMP EEA 강제(서버)", checked = uiState.umpForceEea, onCheckedChange = {
                viewModel.setSwitch(6, it)
                Toast.makeText(context, if (it) "UMP: EEA 강제 활성화" else "UMP: EEA 강제 비활성화", Toast.LENGTH_SHORT).show()
                // If an Activity is available from the composable context, trigger ads-side UMP request immediately
                try {
                    val act = ContextToActivity(context)
                    if (act != null) {
                        try {
                            // [수정] MainApplication에서 umpConsentManager 인스턴스 가져오기
                            val app = context.applicationContext as? kr.sweetapps.alcoholictimer.MainApplication
                            app?.umpConsentManager?.requestAndLoadIfRequired(act) { can ->
                                Log.d("DebugScreen", "UMP EEA toggle -> UMP request finished -> canRequestAds=$can")
                            }
                        } catch (e: Throwable) {
                            Log.d("DebugScreen", "umpConsentManager.requestAndLoadIfRequired failed: ${e.message}")
                        }
                    } else {
                        Log.d("DebugScreen", "UMP EEA toggle changed -> no current Activity available from UI context")
                    }
                } catch (_: Throwable) {}
            })
            DebugSwitch(title = "Analytics 이벤트 전송", checked = uiState.switch3, onCheckedChange = {
                viewModel.setSwitch(3, it)
                // trigger analytics test event when toggled on
                if (it) {
                    viewModel.performAction(3)
                    Toast.makeText(context, "Analytics event sent (debug)", Toast.LENGTH_SHORT).show()
                }
            })
            DebugSwitch(title = "Crashlytics 비치명 보고", checked = uiState.switch4, onCheckedChange = {
                viewModel.setSwitch(4, it)
                if (it) {
                    viewModel.performAction(4)
                    Toast.makeText(context, "Crashlytics non-fatal sent (debug)", Toast.LENGTH_SHORT).show()
                }
            })
            DebugSwitch(title = "Performance trace 실행", checked = uiState.switch5, onCheckedChange = {
                viewModel.setSwitch(5, it)
                if (it) {
                    viewModel.performAction(5)
                    Toast.makeText(context, "Performance trace started (debug)", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}

@Composable
private fun DebugSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
