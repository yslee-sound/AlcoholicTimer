package kr.sweetapps.alcoholictimer.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
 * 선택적 업데이트 팝업 컴포넌트
 * "나중에" 버튼이 있어 사용자가 업데이트를 미룰 수 있음
 * 여기에 보이는 문구는 기본값임
 * 호출부에서 해당 파라미터를 생략하면, 컴포저블 내부에 정의된 기본값이 사용됩니다.
 */
@Composable
fun OptionalUpdateDialog(
    title: String = "새 버전 사용 가능",
    description: String = "더 나은 경험을 위해 최신 버전으로 업데이트하는 것을 권장합니다. 새로운 기능과 개선사항을 확인해보세요.",
    updateButtonText: String = "지금 업데이트",
    laterButtonText: String = "나중에",
    features: List<String>? = null,
    version: String? = null,
    onUpdateClick: () -> Unit,
    onLaterClick: () -> Unit
) {
    Dialog(
        onDismissRequest = onLaterClick,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Box {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(28.dp)
                            .padding(top = 16.dp) // X 버튼 공간
                    ) {
                        // 상단 이미지(교체 가능). 필요 시 다른 리소스/URL로 교체
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

                        // 버전 배지 (옵션)
                        version?.let {
                            Surface(
                                color = Color(0xFFE3F2FD),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "v$it",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1976D2),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // 제목
                        Text(
                            text = title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 설명(스크롤 영역 내 배치로 잘림 방지)
                        Text(
                            text = description,
                            fontSize = 15.sp,
                            color = Color(0xFF666666),
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 기능 목록 (옵션)
                        features?.let { list ->
                            Spacer(modifier = Modifier.height(20.dp))
                            Column(modifier = Modifier.fillMaxWidth()) {
                                list.forEach { feature ->
                                    FeatureItem(feature)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // 버튼 2줄(세로 스택), 테두리 제거
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = onUpdateClick,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A7FFF)),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text(updateButtonText, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }

                            // 경계선 없는 보조 버튼(연한 배경)
                            Button(
                                onClick = onLaterClick,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF2F4F7),
                                    contentColor = Color(0xFF333333)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text(laterButtonText, fontSize = 16.sp, fontWeight = FontWeight.Medium) }
                        }
                    }

                    // X 닫기 버튼 (우측 상단)
                    IconButton(
                        onClick = onLaterClick,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "닫기",
                            tint = Color(0xFF999999)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureItem(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(6.dp)
                .background(Color(0xFF4A7FFF), RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color(0xFF666666),
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
    }
}