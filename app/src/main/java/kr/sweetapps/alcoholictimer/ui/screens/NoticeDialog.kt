package kr.sweetapps.alcoholictimer.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
 * 공지사항 팝업 컴포넌트
 * 우측 상단에 닫기 버튼이 있는 재사용 가능한 공지사항 다이얼로그
 */
@Composable
fun NoticeDialog(
    title: String,
    description: String,
    imageUrl: String? = null, // 미래에 Coil 등으로 교체할 때 사용
    buttonText: String? = null,
    onDismiss: () -> Unit,
    onButtonClick: (() -> Unit)? = null,
    backgroundColor: Color = Color.White,
    titleColor: Color = Color(0xFF1A1A1A),
    descriptionColor: Color = Color(0xFF666666)
) {
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
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = backgroundColor
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 8.dp
                )
            ) {
                Box {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                            .padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 상단 샘플 이미지(16:9, 라운드, crop)
                        val shape = RoundedCornerShape(12.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(shape)
                        ) {
                            // 현재는 리소스 이미지를 표시 (imageUrl 사용은 추후 확장)
                            Image(
                                painter = painterResource(id = R.drawable.ic_splash_logo),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))

                        // 제목
                        Text(
                            text = title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = titleColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 설명
                        Text(
                            text = description,
                            fontSize = 16.sp,
                            color = descriptionColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 버튼 (옵션)
                        buttonText?.let {
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { onButtonClick?.invoke() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = it,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // X 닫기 버튼 (우측 상단)
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "닫기",
                            tint = Color(0xFF666666)
                        )
                    }
                }
            }
        }
    }
}


/**
 * 다크 테마 공지사항 다이얼로그
 */
@Composable
fun NoticeDialogDark(
    title: String,
    description: String,
    imageUrl: String? = null,
    buttonText: String? = null,
    onDismiss: () -> Unit,
    onButtonClick: (() -> Unit)? = null
) {
    NoticeDialog(
        title = title,
        description = description,
        imageUrl = imageUrl,
        buttonText = buttonText,
        onDismiss = onDismiss,
        onButtonClick = onButtonClick,
        backgroundColor = Color(0xFF1E1E1E),
        titleColor = Color.White,
        descriptionColor = Color(0xFFCCCCCC)
    )
}

/**
 * 간단한 텍스트 전용 공지사항 다이얼로그
 */
@Composable
fun SimpleNoticeDialog(
    title: String,
    description: String,
    onDismiss: () -> Unit
) {
    NoticeDialog(
        title = title,
        description = description,
        imageUrl = null,
        buttonText = null,
        onDismiss = onDismiss,
        onButtonClick = null
    )
}