package kr.sweetapps.alcoholictimer.ui.dialogs

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
 * 공지사항 팝업 컴포넌트 (업데이트된 UI)
 */
@Composable
fun NoticeDialog(
    title: String,
    description: String,
    buttonText: String? = null,
    onDismiss: () -> Unit,
    onButtonClick: (() -> Unit)? = null,
    backgroundColor: Color = Color.White,
    titleColor: Color = Color(0xFF1A1A1A),
    descriptionColor: Color = Color(0xFF666666)
) {
    Log.d("NoticeDialog", "NoticeDialog called with title: $title, description: $description")

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
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .shadow(8.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = backgroundColor)
            ) {
                Box(modifier = Modifier.padding(20.dp)) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        // 상단 큰 이미지 박스 (라운드 배경 + inner image)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF7F2EE)), // 연한 베이지 톤 배경
                            contentAlignment = Alignment.Center
                        ) {
                            // inner image (로고/샘플)
                            Image(
                                painter = painterResource(id = R.drawable.ic_splash_logo),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxHeight(0.85f)
                                    .padding(horizontal = 12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 제목
                        Text(
                            text = title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = titleColor,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 설명
                        Text(
                            text = description,
                            fontSize = 14.sp,
                            color = descriptionColor,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        )

                        // 버튼 제거: 새 공지사항 팝업에는 버튼이 없음
                        Spacer(modifier = Modifier.height(18.dp))
                    }

                    // 우측 상단 원형 닫기 버튼 (오버랩)
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(40.dp)
                            .background(Color.White, shape = CircleShape)
                            .clip(CircleShape)
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


@Composable
fun NoticeDialogDark(
    title: String,
    description: String,
    buttonText: String? = null,
    onDismiss: () -> Unit,
    onButtonClick: (() -> Unit)? = null
) {
    NoticeDialog(
        title = title,
        description = description,
        buttonText = buttonText,
        onDismiss = onDismiss,
        onButtonClick = onButtonClick,
        backgroundColor = Color(0xFF1E1E1E),
        titleColor = Color.White,
        descriptionColor = Color(0xFFCCCCCC)
    )
}


@Composable
fun SimpleNoticeDialog(
    title: String,
    description: String,
    onDismiss: () -> Unit
) {
    NoticeDialog(
        title = title,
        description = description,
        buttonText = null,
        onDismiss = onDismiss,
        onButtonClick = null
    )
}
