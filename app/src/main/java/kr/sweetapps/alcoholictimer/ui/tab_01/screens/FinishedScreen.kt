// [NEW] 타이머 완료 화면 - 임시 UI
package kr.sweetapps.alcoholictimer.ui.tab_01.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager

/**
 * 타이머 완료 화면 (임시 UI)
 * - "목표 달성 완료!" 메시지
 * - "결과 확인" 버튼 (전면 광고 연동 → 기록 상세 화면)
 * - "새 타이머 시작" 버튼
 */
@Composable
fun FinishedScreen(
    onResultCheck: () -> Unit = {},
    onNewTimerStart: () -> Unit = {}
) {
    val context = LocalContext.current

    // [NEW] SharedPreferences에서 완료된 기록 정보 가져오기
    val sharedPref = remember {
        context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
    }

    // 완료된 타이머의 정보
    val completedStartTime = remember { sharedPref.getLong("completed_start_time", 0L) }
    val completedEndTime = remember { sharedPref.getLong("completed_end_time", 0L) }
    val completedTargetDays = remember { sharedPref.getFloat("completed_target_days", 0f) }
    val completedActualDays = remember { sharedPref.getInt("completed_actual_days", 0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 완료 메시지
            Text(
                text = "🎉",
                fontSize = 72.sp
            )

            Text(
                text = "목표 달성 완료!",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "축하합니다!\n목표를 성공적으로 달성했습니다.",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 결과 확인 버튼 (전면 광고 연동)
            Button(
                onClick = {
                    Log.d("FinishedScreen", "결과 확인 버튼 클릭")
                    val activity = context as? Activity

                    // [NEW] AdPolicyManager로 전면 광고 정책 확인
                    val shouldShowAd = kr.sweetapps.alcoholictimer.data.repository.AdPolicyManager.shouldShowInterstitialAd(context)

                    if (shouldShowAd && activity != null && InterstitialAdManager.isLoaded()) {
                        Log.d("FinishedScreen", "전면 광고 쿨타임 OK -> 광고 표시 시작")
                        InterstitialAdManager.show(activity) { success ->
                            if (success) {
                                Log.d("FinishedScreen", "전면 광고 닫힘 -> 결과 확인")
                            } else {
                                Log.d("FinishedScreen", "전면 광고 표시 실패 -> 즉시 결과 확인")
                            }
                            onResultCheck()
                        }
                    } else {
                        if (!shouldShowAd) {
                            Log.d("FinishedScreen", "전면 광고 쿨타임 중 -> 광고 스킵하고 결과 확인")
                        } else {
                            Log.d("FinishedScreen", "전면 광고 없음 -> 즉시 결과 확인")
                        }
                        onResultCheck()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "결과 확인",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 새 타이머 시작 버튼
            OutlinedButton(
                onClick = {
                    Log.d("FinishedScreen", "새 타이머 시작 버튼 클릭")
                    onNewTimerStart()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "새 타이머 시작",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

