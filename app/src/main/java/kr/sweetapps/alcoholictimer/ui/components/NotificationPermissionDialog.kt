package kr.sweetapps.alcoholictimer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.tooling.preview.Preview
import kr.sweetapps.alcoholictimer.R

/**
 * 알림 권한 Pre-Permission 다이얼로그
 *
 * 시스템 권한 팝업을 띄우기 전에 사용자에게 알림의 가치를 설명하는 커스텀 다이얼로그
 *
 * @param onConfirm "확인" 버튼 클릭 시 콜백 (시스템 권한 팝업 요청)
 * @param onDismiss "나중에" 버튼 또는 바깥 영역 클릭 시 콜백
 *
 * @since 2025-12-31
 */
@Composable
fun NotificationPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.Start // 좌측 정렬로 변경
            ) {
                // 제목
                Text(
                    text = stringResource(R.string.notification_permission_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3),
                    textAlign = TextAlign.Start // 좌측 정렬
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 설명 문구
                Text(
                    text = stringResource(R.string.notification_permission_message),
                    fontSize = 14.sp,
                    color = Color(0xFF424242),
                    textAlign = TextAlign.Start, // 좌측 정렬
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 버튼 영역
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // "나중에" 버튼
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF757575)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.notification_permission_later),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // "확인" 버튼
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.notification_permission_confirm),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * NotificationPermissionDialog Preview
 */
@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun NotificationPermissionDialogPreview() {
    NotificationPermissionDialog(
        onConfirm = { /* Preview - do nothing */ },
        onDismiss = { /* Preview - do nothing */ }
    )
}
