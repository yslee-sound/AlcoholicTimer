package kr.sweetapps.alcoholictimer.feature.debug

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.core.ui.AdmobBanner
import kr.sweetapps.alcoholictimer.core.ui.BaseActivity
import kr.sweetapps.alcoholictimer.core.ui.DebugAdHelper
import kr.sweetapps.alcoholictimer.core.ui.LocalSafeContentPadding
import kr.sweetapps.alcoholictimer.data.supabase.model.PopupDecision
import kr.sweetapps.alcoholictimer.ui.dialogs.EmergencyRedirectDialog
import kr.sweetapps.alcoholictimer.ui.dialogs.NoticeDialog
import kr.sweetapps.alcoholictimer.ui.dialogs.OptionalUpdateDialog

class DebugActivity : BaseActivity() {
    private val viewModel: DebugViewModel by viewModels()

    override fun getScreenTitleResId(): Int? = null
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getScreenTitle(): String = "디버그 모드"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseScreen(bottomAd = { AdmobBanner() }) {
                DebugScreen(viewModel)
            }
        }
    }
}

@Composable
private fun DebugScreen(viewModel: DebugViewModel) {
    val safePadding = LocalSafeContentPadding.current
    val context = LocalContext.current

    var showSplashDialog by remember { mutableStateOf(false) }
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var showOptionalUpdateDialog by remember { mutableStateOf(false) }
    var showNoticeDialog by remember { mutableStateOf(false) }

    // Supabase 정책 상태
    val emergencyPolicy by viewModel.emergencyPolicy.collectAsState()
    val noticePolicy by viewModel.noticePolicy.collectAsState()
    val updatePolicy by viewModel.updatePolicy.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // 초기 정책 로드
    LaunchedEffect(Unit) {
        viewModel.loadPolicies()
    }

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
                    text = "공지사항 (Supabase 연동)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // 로딩 상태 표시
                if (isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("정책 로딩 중...", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // 오류 메시지
                errorMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                HorizontalDivider()

                // 긴급 공지
                EmergencyNoticeRow(
                    policy = emergencyPolicy,
                    showDialog = showEmergencyDialog,
                    onToggle = { showEmergencyDialog = it }
                )
                HorizontalDivider()

                // 일반 공지
                NoticeRow(
                    policy = noticePolicy,
                    showDialog = showNoticeDialog,
                    onToggle = { showNoticeDialog = it }
                )
                HorizontalDivider()

                // 업데이트 공지
                OptionalUpdateRow(
                    policy = updatePolicy,
                    showDialog = showOptionalUpdateDialog,
                    onToggle = { showOptionalUpdateDialog = it }
                )
                HorizontalDivider()

                // 정책 결정 버튼
                PolicyDecisionButton(
                    onClick = {
                        viewModel.decidePopup { decision ->
                            when (decision) {
                                is PopupDecision.ShowEmergency -> showEmergencyDialog = true
                                is PopupDecision.ShowNotice -> showNoticeDialog = true
                                is PopupDecision.ShowUpdate -> showOptionalUpdateDialog = true
                                is PopupDecision.None -> {
                                    // 표시할 팝업 없음을 알림
                                    android.widget.Toast.makeText(
                                        context,
                                        "표시할 팝업이 없습니다",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    },
                    enabled = !isLoading
                )
                HorizontalDivider()

                // 기록 초기화 버튼
                ClearRecordsButton(
                    onClick = { viewModel.clearAllRecords() },
                    enabled = !isLoading
                )
            }
        }

        Text(
            text = "디버그 전용 설정입니다. 릴리스 빌드에는 포함되지 않습니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // 다이얼로그들
    if (showSplashDialog) {
        SplashDialog(onDismiss = { showSplashDialog = false })
    }

    if (showEmergencyDialog && emergencyPolicy != null) {
        val policy = emergencyPolicy!!
        EmergencyRedirectDialog(
            title = policy.title,
            description = policy.description,
            newAppName = policy.newAppName,
            newAppPackage = policy.newAppPackage ?: "",
            buttonText = policy.buttonText,
            supportUrl = policy.supportUrl,
            supportButtonText = policy.supportButtonText ?: "자세히 보기",
            canMigrateData = policy.canMigrateData,
            isDismissible = policy.isDismissible,
            onDismiss = { showEmergencyDialog = false },
            badgeText = policy.badgeText,
            migrationMessage = policy.migrationMessage
        )
    }

    if (showNoticeDialog && noticePolicy != null) {
        val policy = noticePolicy!!
        NoticeDialog(
            title = policy.title,
            description = policy.description,
            buttonText = policy.buttonText,
            onDismiss = { showNoticeDialog = false },
            onButtonClick = { showNoticeDialog = false }
        )
    }

    if (showOptionalUpdateDialog && updatePolicy != null) {
        val policy = updatePolicy!!
        OptionalUpdateDialog(
            title = policy.title,
            description = policy.description,
            updateButtonText = policy.updateButtonText,
            laterButtonText = policy.laterButtonText ?: "나중에",
            features = policy.features,
            version = policy.version,
            onUpdateClick = { showOptionalUpdateDialog = false },
            onLaterClick = {
                viewModel.dismissUpdate(policy.version)
                showOptionalUpdateDialog = false
            }
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
private fun EmergencyNoticeRow(
    policy: kr.sweetapps.alcoholictimer.data.supabase.model.EmergencyPolicy?,
    showDialog: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = "긴급상황 팝업", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (policy != null) {
                    "정책: ${policy.title} (우선순위: ${policy.priority})"
                } else {
                    "정책 없음"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (policy != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = showDialog,
            onCheckedChange = onToggle,
            enabled = policy != null
        )
    }
}

@Composable
private fun NoticeRow(
    policy: kr.sweetapps.alcoholictimer.data.supabase.model.NoticePolicy?,
    showDialog: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = "일반 공지사항 팝업", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (policy != null) {
                    "정책: ${policy.title} (우선순위: ${policy.priority})"
                } else {
                    "정책 없음"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (policy != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = showDialog,
            onCheckedChange = onToggle,
            enabled = policy != null
        )
    }
}

@Composable
private fun OptionalUpdateRow(
    policy: kr.sweetapps.alcoholictimer.data.supabase.model.UpdatePolicy?,
    showDialog: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = "업데이트 팝업", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (policy != null) {
                    "정책: v${policy.version} (${if (policy.isForceUpdate) "강제" else "선택"})"
                } else {
                    "정책 없음"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (policy != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = showDialog,
            onCheckedChange = onToggle,
            enabled = policy != null
        )
    }
}

@Composable
private fun PolicyDecisionButton(
    onClick: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = "우선순위대로 팝업 표시", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "긴급 → 강제 업데이트 → 공지 → 선택적 업데이트 순서",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(
            onClick = onClick,
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("실행", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ClearRecordsButton(
    onClick: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = "표시 기록 초기화", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "모든 팝업 표시 기록을 지웁니다",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("초기화", style = MaterialTheme.typography.bodyMedium)
        }
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

