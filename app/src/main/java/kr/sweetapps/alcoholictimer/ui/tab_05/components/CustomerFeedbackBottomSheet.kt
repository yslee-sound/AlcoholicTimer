package kr.sweetapps.alcoholictimer.ui.tab_05.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.sweetapps.alcoholictimer.data.repository.FeedbackRepository

/**
 * 고객 문의/제안 바텀 시트
 * 하나의 화면에서 모든 입력을 완료하는 개선된 UX
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerFeedbackBottomSheet(
    onDismiss: () -> Unit,
    onSubmit: (category: String, content: String, email: String) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { FeedbackRepository() }

    var selectedCategory by remember { mutableStateOf("기능 제안") }
    var contentText by remember { mutableStateOf("") }
    var emailText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val maxContentLength = 300

    // [NEW] 이메일 유효성 검사 로직
    val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    val isEmailValid = emailText.isEmpty() || emailPattern.matches(emailText.trim())
    val showEmailError = emailText.isNotEmpty() && !isEmailValid

    val isSubmitEnabled = contentText.trim().isNotEmpty() && !isSubmitting
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFFFFDF7), // 크림색 배경
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color(0xFFD0D0D0), RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState) // [필수] 스크롤 추가
                .padding(horizontal = 24.dp)
                .navigationBarsPadding() // [필수] 하단 네비게이션 바 패딩
                .imePadding() // [필수] 키보드 패딩
                .padding(bottom = 24.dp)
        ) {
            // A. 상단 타이틀
            Text(
                text = "문의하기",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // B. 문의 유형 선택 (Chip 스타일)
            Text(
                text = "문의 유형",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF666666),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("기능 제안", "버그 신고", "기타 문의").forEach { category ->
                    CategoryChip(
                        text = category,
                        isSelected = selectedCategory == category,
                        onClick = { selectedCategory = category }
                    )
                }
            }

            // C. 내용 입력
            Text(
                text = "문의 내용",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF666666),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = contentText,
                onValueChange = {
                    if (it.length <= maxContentLength) contentText = it
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                placeholder = {
                    Text(
                        text = "예시) 앱 시작시 중지되는 현상이 있습니다.",
                        color = Color(0xFFAAAAAA)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Default // 다중 행 입력이므로 줄바꿈 가능
                ),
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "${contentText.length} / $maxContentLength",
                            fontSize = 12.sp,
                            color = Color(0xFF999999)
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = Color(0xFFDDDDDD),
                    focusedBorderColor = Color(0xFF8A6CFF),
                    unfocusedTextColor = Color(0xFF333333),
                    focusedTextColor = Color(0xFF333333)
                ),
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // D. 이메일 입력 (선택 사항)
            Text(
                text = "이메일 주소 (선택)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF666666),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = emailText,
                onValueChange = { emailText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "example@email.com",
                        color = Color(0xFFAAAAAA)
                    )
                },
                singleLine = true,
                isError = showEmailError, // [NEW] 에러 상태 표시
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                supportingText = {
                    if (showEmailError) {
                        // [NEW] 에러 메시지 표시
                        Text(
                            text = "올바른 이메일 형식이 아닙니다.",
                            fontSize = 12.sp,
                            color = Color(0xFFD32F2F), // 빨간색
                            lineHeight = 16.sp
                        )
                    } else {
                        // 기본 안내 문구
                        Text(
                            text = "답변을 받으시려면 이메일 주소를 입력해주세요.\n이메일 주소는 답변 용도 외에 사용되지 않습니다.",
                            fontSize = 12.sp,
                            color = Color(0xFF999999),
                            lineHeight = 16.sp
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = Color(0xFFDDDDDD),
                    focusedBorderColor = Color(0xFF8A6CFF),
                    errorBorderColor = Color(0xFFD32F2F), // [NEW] 에러 테두리 색상
                    errorContainerColor = Color.White,
                    unfocusedTextColor = Color(0xFF333333),
                    focusedTextColor = Color(0xFF333333)
                ),
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // E. 전송 버튼
            Button(
                onClick = {
                    isSubmitting = true

                    // Firebase Firestore에 전송
                    repository.submitFeedback(
                        category = selectedCategory,
                        content = contentText.trim(),
                        email = emailText.trim(),
                        onSuccess = {
                            isSubmitting = false
                            Toast.makeText(
                                context,
                                "소중한 의견 감사합니다 🙏",
                                Toast.LENGTH_SHORT
                            ).show()
                            onSubmit(selectedCategory, contentText.trim(), emailText.trim())
                            onDismiss()
                        },
                        onFailure = { errorMessage ->
                            isSubmitting = false
                            Toast.makeText(
                                context,
                                "전송에 실패했습니다\n$errorMessage",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = isSubmitEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8A6CFF),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFE0E0E0),
                    disabledContentColor = Color(0xFF9E9E9E)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmitting) {
                    // 전송 중 로딩 인디케이터
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "전송 중...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "보내기",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 하단 여백 확보 (키보드가 올라와도 버튼이 잘 보이도록)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 문의 유형 선택 칩 컴포넌트
 */
@Composable
private fun CategoryChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(40.dp)
            .background(
                color = if (isSelected) Color(0xFF8A6CFF) else Color.White,
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFF8A6CFF) else Color(0xFFDDDDDD),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color(0xFF666666)
        )
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun CustomerFeedbackBottomSheetPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        CustomerFeedbackBottomSheet(
            onDismiss = {},
            onSubmit = { category, content, email ->
                println("Category: $category")
                println("Content: $content")
                println("Email: $email")
            }
        )
    }
}

