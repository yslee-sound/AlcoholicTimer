package kr.sweetapps.alcoholictimer.ui.dialogs

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp
import kr.sweetapps.alcoholictimer.R

/**
 * 업데이트 팝업 컴포넌트 (강제/선택적 통합)
 *
 * isForce = true: 강제 업데이트 모드 (닫기 불가, "나중에" 버튼 숨김)
 * isForce = false: 선택적 업데이트 모드 ("나중에" 버튼 표시, 닫기 가능)
 *
 * Supabase의 is_force 필드 값에 따라 동작이 결정됩니다.
 * 여기에 보이는 문구는 기본값이며, 호출부에서 재정의 가능합니다.
 */
@Composable
fun OptionalUpdateDialog(
    isForce: Boolean = false,
    title: String = "앱 업데이트",
    // 설명과 features 파라미터 허용
    description: String = "",
    updateButtonText: String = "지금 업데이트",
    laterButtonText: String = "나중에",
    features: List<String>? = null,
    estimatedTime: String? = null,
    onUpdateClick: () -> Unit,
    onLaterClick: (() -> Unit)? = null
) {
    // resolve strings from resources when empty
    val resolvedTitle = if (title.isBlank()) stringResource(id = kr.sweetapps.alcoholictimer.R.string.update_dialog_title) else title
    val resolvedDescription = if (description.isBlank()) stringResource(id = kr.sweetapps.alcoholictimer.R.string.update_dialog_default_message) else description
    val resolvedUpdateText = if (updateButtonText.isBlank()) stringResource(id = kr.sweetapps.alcoholictimer.R.string.update_dialog_update) else updateButtonText
    val resolvedLaterText = if (laterButtonText.isBlank()) stringResource(id = kr.sweetapps.alcoholictimer.R.string.update_dialog_later) else laterButtonText

    Dialog(
        onDismissRequest = {
            if (!isForce) {
                onLaterClick?.invoke()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isForce,
            dismissOnClickOutside = !isForce,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)) // overlay alpha 0.6
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(12.dp), // corner radius 12dp
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp) // container padding 20dp
                            .padding(top = 8.dp) // header top margin 8dp for icon
                    ) {
                        // Header icon (56dp circular)
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF3F8FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.update_sample),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 제목
                        Text(
                            text = resolvedTitle,
                            fontSize = 20.sp, // token
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF111827),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // title - body spacing: 12dp token
                        // 설명 (선택) — 본문만 스크롤 가능하게 제한된 높이(최대 220dp)
                        if (description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                Column(modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(vertical = 8.dp)) {
                                    Text(
                                        text = resolvedDescription,
                                        fontSize = 14.sp,
                                        lineHeight = 18.sp, // 14sp + 4dp token
                                        color = Color(0xFF6B7280),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp)
                                    )
                                }
                            }
                        }

                        val hasFeatures = !features.isNullOrEmpty()
                        if (hasFeatures) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(modifier = Modifier.fillMaxWidth()) {
                                features!!.forEach { feature ->
                                    FeatureItem(feature)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }

                        // 예상 소요 시간 (옵션)
                        estimatedTime?.let {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "예상 소요: $it",
                                color = Color(0xFF8A8A8A),
                                fontSize = 13.sp
                            )
                        }

                        // 본문과 버튼 사이: 20dp
                        Spacer(modifier = Modifier.height(20.dp))

                        // 버튼 레이아웃
                        if (isForce) {
                            // 오른쪽 정렬: 왼쪽은 비워두고, 오른쪽에만 버튼
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Spacer(modifier = Modifier.weight(1f).height(48.dp))
                                Button(
                                    onClick = onUpdateClick,
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        resolvedUpdateText,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        } else {
                            // 가로 한 줄: 왼쪽 '나중에' (보조), 오른쪽 '지금 업데이트'(주 버튼)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Secondary button style (outlined-like)
                                OutlinedButton(
                                    onClick = { onLaterClick?.invoke() },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    border = BorderStroke(1.dp, Color(0xFF1E88E5)),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = Color(0xFF1E88E5)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues()
                                ) {
                                    Text(
                                        resolvedLaterText,
                                        fontSize = 14.sp,
                                        color = Color(0xFF1E88E5)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Button(
                                    onClick = onUpdateClick,
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        resolvedUpdateText,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // X 닫기 버튼 (선택적 모드에서만 표시)
                    if (!isForce) {
                        IconButton(
                            onClick = { onLaterClick?.invoke() },
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
}

@Composable
private fun FeatureItem(text: String) {
    // 불릿(점) 제거: 텍스트만 좌측 정렬로 표시
    Text(
        text = text,
        fontSize = 14.sp,
        color = Color(0xFF666666),
        lineHeight = 20.sp,
        modifier = Modifier.fillMaxWidth()
    )
}
