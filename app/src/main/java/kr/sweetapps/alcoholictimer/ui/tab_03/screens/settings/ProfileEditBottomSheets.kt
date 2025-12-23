package kr.sweetapps.alcoholictimer.ui.tab_03.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.sweetapps.alcoholictimer.util.AvatarManager

/**
 * 아바타 빠른 선택 바텀시트
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarEditBottomSheet(
    currentAvatarIndex: Int,
    onAvatarSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "아바타 선택",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 아바타 그리드 (20개)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(450.dp) // [FIX] 20개를 보여주기 위해 높이 증가
            ) {
                items((0 until 20).toList()) { index -> // [FIX] 12개 -> 20개로 변경
                    val isSelected = index == currentAvatarIndex
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color(0xFF6366F1) else Color(0xFFE0E0E0),
                                shape = CircleShape
                            )
                            .background(Color(0xFFF5F5F5))
                            .clickable {
                                onAvatarSelected(index)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = AvatarManager.getAvatarResId(index)),
                            contentDescription = "아바타 $index",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 닉네임 빠른 편집 바텀시트
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NicknameEditBottomSheet(
    currentNickname: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nicknameText by remember { mutableStateOf(currentNickname) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "닉네임 변경",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = nicknameText,
                onValueChange = {
                    if (it.length <= 10) {
                        nicknameText = it
                    }
                },
                label = { Text("닉네임") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    focusedLabelColor = Color(0xFF6366F1)
                ),
                supportingText = {
                    Text(
                        text = "${nicknameText.length}/10",
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 저장 버튼
            Button(
                onClick = {
                    if (nicknameText.isNotBlank()) {
                        onSave(nicknameText)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1)
                ),
                enabled = nicknameText.isNotBlank()
            ) {
                Text("저장", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

