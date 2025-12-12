// [REFACTORED] Timer completion/give-up screen - supports both success and give-up scenarios
package kr.sweetapps.alcoholictimer.ui.tab_01.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue  // [NEW] 메인 UI 색상

/**
 * Screen displayed when timer expires or user gives up
 *
 * @param isSuccess True if goal was achieved, false if user gave up (default: true)
 * @param onBack Called when back button is clicked (navigate back)
 * @param onResultCheck Called when 'Check Result' button is clicked (shows fullscreen ad then navigates to detail screen)
 * @param onNewTimerStart Called when 'Start New Timer' button is clicked (resets expired state)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishedScreen(
    isSuccess: Boolean = true,
    onBack: () -> Unit = {},
    onResultCheck: () -> Unit = {},
    onNewTimerStart: () -> Unit = {}
) {
    // [NEW] UI resources based on success/give-up status
    val icon: ImageVector
    val iconColor: Color
    val emoji: String
    val title: String
    val description: String
    val buttonColor: Color
    val backgroundColor: Color

    if (isSuccess) {
        // Success scenario (기존 유지)
        icon = Icons.Filled.CheckCircle
        iconColor = MainPrimaryBlue
        emoji = "🎉"
        title = "목표 달성!"
        description = "축하합니다!\n금주 목표를 성공적으로 완료했습니다."
        buttonColor = MainPrimaryBlue
        backgroundColor = Color.White
    } else {
        // Give-up scenario (신규 추가 - 따뜻한 위로형)
        icon = Icons.Filled.Favorite
        iconColor = Color(0xFFFF8A65) // 따뜻한 코랄/오렌지 계열
        emoji = "🍃"
        title = "잠시 쉬어가도 괜찮아요"
        description = "이번 도전은 여기서 멈추지만, 그동안의 노력은 사라지지 않아요.\n마음을 추스르고 언제든 다시 돌아오세요."
        buttonColor = Color(0xFFFF8A65)
        backgroundColor = Color(0xFFFFF5F0) // 연한 코랄 배경
    }

    Log.d("FinishedScreen", "Timer completion screen displayed - isSuccess: $isSuccess")

    // [NEW] Scaffold with TopBar for full screen experience
    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = { /* Empty title */ },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로 가기",
                            tint = iconColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // [REFACTORED] Icon - changes based on success/give-up
            Icon(
                imageVector = icon,
                contentDescription = if (isSuccess) "Completed" else "Give Up",
                modifier = Modifier.size(80.dp),
                tint = iconColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // [REFACTORED] Title - changes based on success/give-up
            Text(
                text = "$emoji $title",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = iconColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            // [REFACTORED] Description - changes based on success/give-up
            Text(
                text = description,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // [COMMON] Check result button - works for both scenarios
            Button(
                onClick = {
                    Log.d("FinishedScreen", "Check result button clicked -> executing ad logic")
                    onResultCheck()
                },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor
                )
            ) {
                Text(
                    text = "결과 확인",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // [COMMON] Start new timer button - works for both scenarios
            OutlinedButton(
                onClick = {
                    Log.d("FinishedScreen", "Start new timer button clicked -> resetting completion state")
                    onNewTimerStart()
                },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = buttonColor
                )
            ) {
                Text(
                    text = "새 타이머 시작",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

