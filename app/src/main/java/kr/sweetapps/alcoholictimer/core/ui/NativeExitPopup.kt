package kr.sweetapps.alcoholictimer.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kr.sweetapps.alcoholictimer.R

/**
 * 뒤로가기 시 표시되는 네이티브 광고 포함 종료 확인 팝업
 * - 종료/취소 버튼이 명확하게 표시됨
 * - 광고는 보조 요소로 배치
 * - 정책: 디버그는 항상 표시, 릴리즈는 정책 준수
 */
@Composable
fun NativeExitPopup(
    visible: Boolean,
    onConfirmExit: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 타이틀
                Text(
                    text = "앱을 종료하시겠어요?",
                    style = MaterialTheme.typography.titleLarge,
                    color = colorResource(id = R.color.color_title_primary),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 광고 영역: 항상 플레이스홀더만 표시
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(bottom = 20.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(id = R.color.color_bg_card_light)
                    ),
                    border = BorderStroke(1.dp, colorResource(id = R.color.color_border_light))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "금주의 여정을 응원합니다! 💪",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorResource(id = R.color.color_hint_gray),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // 버튼 영역
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 취소 버튼
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colorResource(id = R.color.color_title_primary)
                        ),
                        border = BorderStroke(1.dp, colorResource(id = R.color.color_border_light))
                    ) {
                        Text("취소", style = MaterialTheme.typography.bodyLarge)
                    }

                    // 종료 버튼
                    Button(
                        onClick = onConfirmExit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.color_progress_primary)
                        )
                    ) {
                        Text("종료", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
