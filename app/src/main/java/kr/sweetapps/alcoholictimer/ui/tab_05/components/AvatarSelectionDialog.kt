package kr.sweetapps.alcoholictimer.ui.tab_05.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kr.sweetapps.alcoholictimer.util.AvatarManager

/**
 * 아바타 선택 다이얼로그
 * 20개의 아바타를 4열 그리드로 표시하고 선택 가능
 */
@Composable
fun AvatarSelectionDialog(
    currentAvatarIndex: Int,
    onAvatarSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
            ) {
                // 제목
                Text(
                    text = "아바타 선택",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111111),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 4열 그리드로 20개 아바타 표시
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp) // 최대 높이 제한
                ) {
                    items(AvatarManager.avatars.size) { index ->
                        AvatarItem(
                            avatarResId = AvatarManager.avatars[index],
                            isSelected = index == currentAvatarIndex,
                            onClick = {
                                onAvatarSelected(index)
                                onDismiss()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 취소 버튼
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "취소",
                        color = Color(0xFF666666)
                    )
                }
            }
        }
    }
}

/**
 * 개별 아바타 아이템
 */
@Composable
private fun AvatarItem(
    avatarResId: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color(0xFF1E40AF) else Color(0xFFE0E0E0),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = avatarResId),
            contentDescription = "아바타",
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isSelected) 4.dp else 2.dp)
                .clip(CircleShape)
        )
    }
}

