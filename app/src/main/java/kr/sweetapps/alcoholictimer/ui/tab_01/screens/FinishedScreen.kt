// [NEW] 타이머 만료 화면
package kr.sweetapps.alcoholictimer.ui.tab_01.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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

/**
 * 타이머 만료 시 표시되는 화면
 *
 * @param onResultCheck '결과 확인' 버튼 클릭 시 호출 (전면 광고 노출 후 기록 상세 화면 이동)
 * @param onNewTimerStart '새 타이머 시작' 버튼 클릭 시 호출 (만료 상태 해제)
 */
@Composable
fun FinishedScreen(
    onResultCheck: () -> Unit = {},
    onNewTimerStart: () -> Unit = {}
) {
    Log.d("FinishedScreen", "타이머 만료 화면 표시")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // [NEW] 목표 달성 완료 아이콘
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "완료",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // [NEW] 목표 달성 완료 메시지
        Text(
            text = "🎉 목표 달성 완료!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "축하합니다!\n금주 목표를 성공적으로 완료했습니다.",
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 결과 확인 버튼 (광고 노출)
        Button(
            onClick = {
                Log.d("FinishedScreen", "결과 확인 버튼 클릭 -> 광고 로직 실행")
                onResultCheck()
            },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "결과 확인",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 새 타이머 시작 버튼 (광고 없음)
        OutlinedButton(
            onClick = {
                Log.d("FinishedScreen", "새 타이머 시작 버튼 클릭 -> 만료 상태 해제")
                onNewTimerStart()
            },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
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

