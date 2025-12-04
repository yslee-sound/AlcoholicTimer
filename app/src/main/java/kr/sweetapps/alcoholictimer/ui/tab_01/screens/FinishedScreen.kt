// [NEW] Timer completion screen
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
 * Screen displayed when timer expires
 *
 * @param onResultCheck Called when 'Check Result' button is clicked (shows fullscreen ad then navigates to detail screen)
 * @param onNewTimerStart Called when 'Start New Timer' button is clicked (resets expired state)
 */
@Composable
fun FinishedScreen(
    onResultCheck: () -> Unit = {},
    onNewTimerStart: () -> Unit = {}
) {
    Log.d("FinishedScreen", "Timer completion screen displayed")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // [NEW] Goal completion icon
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "Completed",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // [NEW] Goal completion message
        Text(
            text = "🎉 목표 달성!",
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

        // Check result button (with ad display)
        Button(
            onClick = {
                Log.d("FinishedScreen", "Check result button clicked -> executing ad logic")
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

        // Start new timer button (without ad)
        OutlinedButton(
            onClick = {
                Log.d("FinishedScreen", "Start new timer button clicked -> resetting completion state")
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

