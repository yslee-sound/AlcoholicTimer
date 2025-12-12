// [REFACTORED] Timer completion screens - Separated into Success and GiveUp
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue

/**
 * [REFACTORED] Success Screen - Timer goal achieved
 *
 * @param onBack Called when back button is clicked
 * @param onResultCheck Called when 'Check Result' button is clicked
 * @param onNewTimerStart Called when 'Start New Timer' button is clicked
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishedSuccessScreen(
    onBack: () -> Unit = {},
    onResultCheck: () -> Unit = {},
    onNewTimerStart: () -> Unit = {}
) {
    Log.d("FinishedSuccessScreen", "Success screen displayed - goal achieved! 🎉")

    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = { /* Empty title */ },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로 가기",
                            tint = MainPrimaryBlue
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
            // Success icon
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Goal Achieved",
                modifier = Modifier.size(80.dp),
                tint = MainPrimaryBlue
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Success title
            Text(
                text = "🎉 목표 달성!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MainPrimaryBlue
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Success description
            Text(
                text = "축하합니다!\n금주 목표를 성공적으로 완료했습니다.",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Check result button
            Button(
                onClick = {
                    Log.d("FinishedSuccessScreen", "Check result button clicked")
                    onResultCheck()
                },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MainPrimaryBlue
                )
            ) {
                Text(
                    text = "결과 확인",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Start new timer button
            OutlinedButton(
                onClick = {
                    Log.d("FinishedSuccessScreen", "Start new timer button clicked")
                    onNewTimerStart()
                },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MainPrimaryBlue
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

/**
 * [REFACTORED] GiveUp Screen - Timer stopped (user gave up)
 *
 * @param onBack Called when back button is clicked
 * @param onResultCheck Called when 'Check Result' button is clicked
 * @param onNewTimerStart Called when 'Start New Timer' button is clicked
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishedGiveUpScreen(
    onBack: () -> Unit = {},
    onResultCheck: () -> Unit = {},
    onNewTimerStart: () -> Unit = {}
) {
    val iconColor = Color(0xFFFF8A65) // 따뜻한 코랄/오렌지 계열
    val backgroundColor = Color(0xFFFFF5F0) // 연한 코랄 배경

    Log.d("FinishedGiveUpScreen", "GiveUp screen displayed - offering comfort 🍃")

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
            // Healing icon
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Take a break",
                modifier = Modifier.size(80.dp),
                tint = iconColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Comfort title
            Text(
                text = "🍃 잠시 쉬어가도 괜찮아요",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = iconColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Comfort description
            Text(
                text = "이번 도전은 여기서 멈추지만, 그동안의 노력은 사라지지 않아요.\n마음을 추스르고 언제든 다시 돌아오세요.",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Check result button
            Button(
                onClick = {
                    Log.d("FinishedGiveUpScreen", "Check result button clicked")
                    onResultCheck()
                },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = iconColor
                )
            ) {
                Text(
                    text = "결과 확인",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Start new timer button
            OutlinedButton(
                onClick = {
                    Log.d("FinishedGiveUpScreen", "Start new timer button clicked")
                    onNewTimerStart()
                },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = iconColor
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

