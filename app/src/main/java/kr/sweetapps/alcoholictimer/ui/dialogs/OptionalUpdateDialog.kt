package kr.sweetapps.alcoholictimer.ui.dialogs

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.tooling.preview.Preview
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
    val resolvedTitle = if (title.isBlank()) stringResource(id = R.string.update_dialog_title) else title
    val resolvedDescription = if (description.isBlank()) stringResource(id = R.string.update_dialog_default_message) else description
    val resolvedUpdateText = if (updateButtonText.isBlank()) stringResource(id = R.string.update_dialog_update) else updateButtonText
    val resolvedLaterText = if (laterButtonText.isBlank()) stringResource(id = R.string.update_dialog_later) else laterButtonText

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
                shape = RoundedCornerShape(12.dp), // container corner 12dp per token
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header banner: image fills the whole header area as background
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, end = 24.dp, top = 20.dp)
                                .aspectRatio(1.6f)
                                .clip(RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.update),
                                contentDescription = null,
                                modifier = Modifier.matchParentSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // content column below header
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp)
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))

                            // 제목: 24sp Bold to match reference
                            Text(
                                text = resolvedTitle,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // title - body spacing: 12dp token
                            // 설명 (선택) — 본문만 스크롤 가능하게 제한된 높이(최대 220dp)
                            if (resolvedDescription.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp)) // title-body spacing
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
                                            lineHeight = 18.sp,
                                            color = Color(0xFF6B7280), // original mid gray
                                            textAlign = TextAlign.Start,
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
                                    features.forEach { feature ->
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

                            // 본문과 버튼 사이: 18dp
                            Spacer(modifier = Modifier.height(18.dp))

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
                                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    // Secondary: grey filled button without border
                                    Button(
                                        onClick = { onLaterClick?.invoke() },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFF3F4F6), // light gray
                                            contentColor = Color(0xFF374151)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp)
                                    ) {
                                        Text(
                                            resolvedLaterText,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    // Primary: filled blue 48dp
                                    Button(
                                        onClick = onUpdateClick,
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A7FFF)),
                                        shape = RoundedCornerShape(12.dp)
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

// Preview 함수들
@Preview(showBackground = true, widthDp = 360)
@Composable
private fun OptionalUpdateDialog_Preview() {
    OptionalUpdateDialog(
        isForce = false,
        title = "앱 업데이트",
        description = "최신 버전이 출시되었습니다. 이번 업데이트에서는 여러 버그 수정 및 성능 개선이 포함되어 있습니다. 긴 설명 예시로 스크롤이 필요한 경우 이 텍스트가 길어집니다. 변경된 항목:\n- 개선사항 A\n- 개선사항 B\n- 수정사항 C",
        onUpdateClick = {},
        onLaterClick = {}
    )
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun OptionalUpdateDialog_Forced_Preview() {
    OptionalUpdateDialog(
        isForce = true,
        title = "앱 업데이트",
        description = "중요한 보안 업데이트입니다. 반드시 업데이트하세요.",
        onUpdateClick = {},
        onLaterClick = null
    )
}
