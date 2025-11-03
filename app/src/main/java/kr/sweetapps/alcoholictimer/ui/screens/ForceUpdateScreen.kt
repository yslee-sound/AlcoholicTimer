package kr.sweetapps.alcoholictimer.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kr.sweetapps.alcoholictimer.R

/**
 * 강제 업데이트 팝업 컴포넌트 (팝업 형태)
 * 여러 앱에서 재사용 가능하도록 설계됨
 */
@Composable
fun ForceUpdateDialog(
    title: String = "앱 업데이트",
    description: String = "새로운 기능 추가, 더 빠른 속도, 버그 해결 등이 포함된 업데이트를 사용할 수 있습니다.",
    buttonText: String = "업데이트",
    features: List<String>? = null,
    estimatedTime: String? = null,
    showCloseButton: Boolean = false,
    onUpdateClick: () -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = { /* 강제 업데이트: 외부 탭으로 닫히지 않음 */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(28.dp)
                ) {
                    // 상단 임시 이미지 (교체 가능)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.6f)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_splash_logo),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = description,
                        fontSize = 15.sp,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    features?.takeIf { it.isNotEmpty() }?.let { list ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(modifier = Modifier.fillMaxWidth()) {
                            list.forEach { item ->
                                Text("• $item", color = Color(0xFF666666), fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }

                    estimatedTime?.let {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("예상 소요: $it", color = Color(0xFF8A8A8A), fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(22.dp))
                    Button(
                        onClick = onUpdateClick,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A7FFF))
                    ) { Text(buttonText, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}