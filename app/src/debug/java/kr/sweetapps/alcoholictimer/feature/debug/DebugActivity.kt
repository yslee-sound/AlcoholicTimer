package kr.sweetapps.alcoholictimer.feature.debug

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.core.ui.BaseActivity
import kr.sweetapps.alcoholictimer.core.ui.LocalSafeContentPadding
import kr.sweetapps.alcoholictimer.core.ui.AdmobBanner
import kr.sweetapps.alcoholictimer.core.ui.DebugAdHelper
import kr.sweetapps.alcoholictimer.ui.dialogs.EmergencyRedirectDialog
import kr.sweetapps.alcoholictimer.ui.screens.NoticeDialog
import kr.sweetapps.alcoholictimer.ui.screens.OptionalUpdateDialog

class DebugActivity : BaseActivity() {
    override fun getScreenTitleResId(): Int? = null
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getScreenTitle(): String = "디버그 모드"

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
    var showSplashDialog by remember { mutableStateOf(false) }
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var showOptionalUpdateDialog by remember { mutableStateOf(false) }
    var showNoticeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(safePadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 광고 설정 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BannerAdToggleRow()
                HorizontalDivider()
                SplashScreenButton(onShowSplash = { showSplashDialog = true })
            }
        }

        // 공지사항 섹션 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "공지사항",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider()
                EmergencyNoticeToggleRow(
                    showDialog = showEmergencyDialog,
                    onToggle = { showEmergencyDialog = it }
                )
                HorizontalDivider()
                NoticeToggleRow(
                    showDialog = showNoticeDialog,
                    onToggle = { showNoticeDialog = it }
                )
                HorizontalDivider()
                OptionalUpdateToggleRow(
                    showDialog = showOptionalUpdateDialog,
                    onToggle = { showOptionalUpdateDialog = it }
                )
            }
        }

        Text(
            text = "디버그 전용 설정입니다. 릴리스 빌드에는 포함되지 않습니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showSplashDialog) {
        SplashDialog(onDismiss = { showSplashDialog = false })
    }

    if (showEmergencyDialog) {
        EmergencyRedirectDialog(
            title = "중요 안내",
            description = "AlcoholicTimer 서비스가 종료됩니다.\n\n새로운 앱 'DrinkTracker'에서 기존 데이터를 모두 이어서 사용하실 수 있습니다.",
            newAppName = "DrinkTracker",
            newAppPackage = "kr.sweetapps.drinktracker",
            buttonText = "새 앱 설치하기",
            supportUrl = "https://example.com/migration-guide",
            supportButtonText = "이전 가이드 보기",
            canMigrateData = true,
            isDismissible = true,
            onDismiss = { showEmergencyDialog = false },
            badgeText = "서비스 종료",
            migrationMessage = "DrinkTracker에서 기존 계정과 데이터를 그대로 사용할 수 있습니다."
        )
    }

    if (showNoticeDialog) {
        NoticeDialog(
            title = "새로운 기능 추가!",
            description = "음주 기록을 더욱 편리하게 관리할 수 있도록 새로운 기능이 추가되었습니다.\n\n지금 바로 확인해 보세요!",
            buttonText = "확인했습니다",
            onDismiss = { showNoticeDialog = false },
            onButtonClick = { showNoticeDialog = false }
        )
    }

    if (showOptionalUpdateDialog) {
        OptionalUpdateDialog(
            title = "새 버전 사용 가능",
            description = "더 나은 경험을 위해 최신 버전으로 업데이트하는 것을 권장합니다.",
            updateButtonText = "지금 업데이트",
            laterButtonText = "나중에",
            features = listOf(
                "새로운 음주 통계 기능",
                "UI/UX 개선",
                "버그 수정 및 안정성 향상"
            ),
            version = "2.0.0",
            onUpdateClick = { showOptionalUpdateDialog = false },
            onLaterClick = { showOptionalUpdateDialog = false }
        )
    }
}

@Composable
private fun BannerAdToggleRow() {
    val context = LocalContext.current
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

@Composable
private fun SplashScreenButton(onShowSplash: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = "스플래시 화면 보기", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "현재 화면에서 스플래시를 확인합니다",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(
            onClick = onShowSplash,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("보기", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EmergencyNoticeToggleRow(
    showDialog: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = "긴급상황 팝업 보기", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (showDialog) "현재: 표시 중" else "현재: 숨김",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = showDialog,
            onCheckedChange = onToggle
        )
    }
}

@Composable
private fun NoticeToggleRow(
    showDialog: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = "일반 공지사항 팝업 보기", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (showDialog) "현재: 표시 중" else "현재: 숨김",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = showDialog,
            onCheckedChange = onToggle
        )
    }
}

@Composable
private fun OptionalUpdateToggleRow(
    showDialog: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = "선택적 업데이트 팝업 보기", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (showDialog) "현재: 표시 중" else "현재: 숨김",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = showDialog,
            onCheckedChange = onToggle
        )
    }
}

@Composable
private fun SplashDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFf28090)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_splash_logo),
                contentDescription = "Splash Logo",
                modifier = Modifier.fillMaxSize(0.8f)
            )
        }
    }
}
