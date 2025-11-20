package kr.sweetapps.alcoholictimer.ui.dialogs

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    updateButtonText: String = "지금 업데이트",
    laterButtonText: String = "나중에",
    features: List<String>? = null,
    estimatedTime: String? = null,
    // Supabase의 release_notes 필드 (description)
    description: String? = null,
    onUpdateClick: () -> Unit,
    onLaterClick: (() -> Unit)? = null
) {
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
                // Dialog 안의 실제 콘텐츠를 별도 컴포저블로 이동하여 Preview에서 재사용 가능하도록 함
                OptionalUpdateDialogContent(
                    isForce = isForce,
                    title = title,
                    updateButtonText = updateButtonText,
                    laterButtonText = laterButtonText,
                    features = features,
                    estimatedTime = estimatedTime,
                    description = description,
                    onUpdateClick = onUpdateClick,
                    onLaterClick = onLaterClick
                )
            }
        }
    }
}

@Composable
fun OptionalUpdateDialogContent(
    isForce: Boolean = false,
    title: String = "앱 업데이트",
    updateButtonText: String = "지금 업데이트",
    laterButtonText: String = "나중에",
    features: List<String>? = null,
    estimatedTime: String? = null,
    // Supabase의 release_notes 필드 (description)
    description: String? = null,
    onUpdateClick: () -> Unit = {},
    onLaterClick: (() -> Unit)? = null
) {
    // 로그를 추가하여 실제로 전달된 description 값을 확인합니다.
    androidx.compose.runtime.LaunchedEffect(key1 = description) {
        android.util.Log.d("OptionalUpdateDialog", "description (release_notes) = ${description ?: "<null>"}")
    }

    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(28.dp)
                .padding(top = 16.dp) // X 버튼 공간
        ) {
            // 상단 이미지
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.6f)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.update_sample),
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
                color = Color(0xFF1A1A1A),
                textAlign = TextAlign.Center
            )

            // description (release_notes) 표시 — Supabase에서 받아온 텍스트를 우선 사용
            description?.let { desc ->
                // normalize escaped newlines / HTML <br> and trim
                val raw = desc
                val normalized = raw.replace("\\n", "\n").replace("<br>", "\n").trim()
                androidx.compose.runtime.LaunchedEffect(key1 = raw) {
                    android.util.Log.d("OptionalUpdateDialog", "raw release_notes=[$raw]")
                    android.util.Log.d("OptionalUpdateDialog", "normalized release_notes=[$normalized]")
                }

                if (normalized.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = normalized,
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    )
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

            // features 없으면 버튼 위 여백 축소 (기존 22dp → 8dp)
            Spacer(modifier = Modifier.height(if (hasFeatures) 22.dp else 8.dp))

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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A7FFF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            updateButtonText,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
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
                    Button(
                        onClick = { onLaterClick?.invoke() },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF2F4F7),
                            contentColor = Color(0xFF333333)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            laterButtonText,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Button(
                        onClick = onUpdateClick,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A7FFF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            updateButtonText,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
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

@Preview(showBackground = true)
@Composable
fun OptionalUpdateDialogPreview_Optional() {
    MaterialTheme {
        // Preview에서는 Dialog 대신 콘텐츠만 렌더링
        Surface(modifier = Modifier.padding(24.dp)) {
            OptionalUpdateDialogContent(
                isForce = false,
                title = "새 버전이 출시되었습니다!",
                updateButtonText = "지금 업데이트",
                laterButtonText = "나중에",
                features = listOf(
                    "UI/UX 개선",
                    "버그 수정 및 안정성 향상",
                    "새로운 차트 뷰 추가"
                ),
                estimatedTime = "약 2분",
                onUpdateClick = {},
                onLaterClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OptionalUpdateDialogPreview_Force() {
    MaterialTheme {
        Surface(modifier = Modifier.padding(24.dp)) {
            OptionalUpdateDialogContent(
                isForce = true,
                title = "중요 업데이트가 필요합니다",
                updateButtonText = "업데이트",
                features = listOf(
                    "보안 패치 적용",
                    "기능 호환성 보장"
                ),
                estimatedTime = null,
                onUpdateClick = {},
                onLaterClick = {}
            )
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
