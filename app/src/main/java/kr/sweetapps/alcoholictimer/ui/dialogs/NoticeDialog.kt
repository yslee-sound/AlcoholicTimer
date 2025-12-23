package kr.sweetapps.alcoholictimer.ui.dialogs

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.data.supabase.model.Announcement

/**
 * 공지사항 팝업 컴포넌트
 * 우측 상단에 닫기 버튼이 있는 재사용 가능한 공지사항 다이얼로그
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
    val closeDesc = stringResource(id = R.string.dialog_close)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,          // 백 버튼으로는 닫기 가능
            dismissOnClickOutside = false,      // 외부 클릭으로는 닫기 불가 ✅
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
                            .padding(20.dp)
                            .padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 상단 샘플 이미지(16:9, 라운드, crop) - 스크롤 안 됨
                        val shape = RoundedCornerShape(12.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(shape)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.notice_sample),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))

                        // 제목 - 스크롤 안 됨
                        Text(
                            text = title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = titleColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 설명 - 이 부분만 스크롤 ✅
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)  // 남은 공간 차지하되 필수 아님
                                .heightIn(max = 300.dp)    // 최대 높이 제한
                        ) {
                            Text(
                                text = description,
                                fontSize = 16.sp,
                                color = descriptionColor,
                                textAlign = TextAlign.Start,
                                lineHeight = 24.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())  // 내용만 스크롤 ✅
                            )
                        }

                        // 버튼 (옵션) - 스크롤 안 됨
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
                            contentDescription = closeDesc,
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
        buttonText = null,
        onDismiss = onDismiss,
        onButtonClick = null
    )
}

// ==================== Supabase 연동 ====================

/**
 * Supabase Announcement 모델을 사용하는 공지사항 다이얼로그
 * Flutter의 AnnouncementDialog와 동일한 기능
 *
 * 사용 예시:
 * ```kotlin
 * AnnouncementDialog(
 *     announcement = announcement,
 *     onDismiss = { }
 * )
 * ```
 */
@Composable
fun AnnouncementDialog(
    announcement: Announcement,
    onDismiss: () -> Unit,
    showImage: Boolean = true
) {
    val closeDesc = stringResource(id = R.string.dialog_close)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,          // 백 버튼으로는 닫기 가능
            dismissOnClickOutside = false,      // 외부 클릭으로는 닫기 불가 ✅
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
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 8.dp
                )
            ) {
                Box {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 이미지 (옵션) - 스크롤 안 됨
                        if (showImage) {
                            val shape = RoundedCornerShape(12.dp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clip(shape)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.notice_sample),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        // 제목 (Supabase의 title 필드) - 스크롤 안 됨
                        // [FIX] 다국어 지원 - displayTitle 사용 (2025-12-23)
                        Text(
                            text = announcement.displayTitle,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 내용 (Supabase의 content 필드) - 이 부분만 스크롤 ✅
                        // [FIX] 다국어 지원 - displayContent 사용 (2025-12-23)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)  // 남은 공간 차지하되 필수 아님
                                .heightIn(max = 300.dp)    // 최대 높이 제한
                        ) {
                            Text(
                                text = announcement.displayContent,
                                fontSize = 16.sp,
                                color = Color(0xFF666666),
                                textAlign = TextAlign.Start,
                                lineHeight = 24.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())  // 내용만 스크롤 ✅
                            )
                        }

                        // 하단 여백 (버튼 없이 깔끔하게)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // X 닫기 버튼 (우측 상단) - 유일한 닫기 수단
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = closeDesc,
                            tint = Color(0xFF666666)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 간단한 텍스트 전용 Announcement 다이얼로그 (이미지 없음)
 */
@Composable
fun SimpleAnnouncementDialog(
    announcement: Announcement,
    onDismiss: () -> Unit
) {
    AnnouncementDialog(
        announcement = announcement,
        onDismiss = onDismiss,
        showImage = false
    )
}
