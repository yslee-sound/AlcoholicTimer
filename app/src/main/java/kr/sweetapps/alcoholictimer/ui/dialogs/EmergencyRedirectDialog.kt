package kr.sweetapps.alcoholictimer.ui.dialogs

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kr.sweetapps.alcoholictimer.R

/**
 * 긴급 상황 앱 전환 안내 팝업 (dialogs 스타일)
 * - 원래 dialogs 구현 스타일을 복원했습니다.
 * - DebugActivity 같은 기존 호출부와 호환되도록 파라미터를 확장(호환성 유지).
 */
@Composable
fun EmergencyRedirectDialog(
    title: String = "공지",
    description: String,
    // 호환성을 위해 newAppName도 허용(예: 기존 screens에서 사용)
    newAppName: String? = null,
    // play store 패키지
    newAppPackage: String,
    // 외부 리다이렉트 URL(있으면 웹으로 이동)
    redirectUrl: String? = null,
    buttonText: String = "확인",
    supportUrl: String? = null,
    supportButtonText: String = "자세히 보기",
    // 기존 파라미터 유지 (미사용 가능)
    canMigrateData: Boolean = false,
    isDismissible: Boolean = false,
    onDismiss: (() -> Unit)? = null,
    badgeText: String? = null,
    migrationMessage: String? = null
) {
    val context = LocalContext.current
    Log.d("EmergencyRedirectDialog", "Showing dialog with title: $title, description: $description")
    Dialog(
        onDismissRequest = { if (isDismissible) onDismiss?.invoke() },
        properties = DialogProperties(
            dismissOnBackPress = isDismissible,
            dismissOnClickOutside = isDismissible,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Box {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(32.dp)
                            .padding(top = if (isDismissible) 16.dp else 0.dp)
                    ) {
                        // dialogs 스타일: 경고 아이콘/이미지
                        Image(
                            painter = painterResource(id = R.drawable.emergency_notice),
                            contentDescription = null,
                            modifier = Modifier.size(96.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // 제목 — 기존 dialogs는 이모티콘 제거
                        Text(
                            text = title.replace("🚨", "").trim(),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 설명 — dialogs 스타일은 왼쪽 정렬, 전체 너비 사용
                        Text(
                            text = description,
                            fontSize = 15.sp,
                            color = Color(0xFF666666),
                            textAlign = TextAlign.Start,
                            lineHeight = 22.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 버튼: redirectUrl이 우선, 아니면 Play Store로
                        Button(
                            onClick = {
                                if (!redirectUrl.isNullOrBlank()) {
                                    openWebPage(context, redirectUrl)
                                } else {
                                    openPlayStore(context, newAppPackage)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B35)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = buttonText,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        // 하단 링크(옵션)
                        supportUrl?.let { url ->
                            Spacer(modifier = Modifier.height(10.dp))
                            val interaction = remember { MutableInteractionSource() }
                            val pressed by interaction.collectIsPressedAsState()
                            val base = Color(0xFF8A8A8A)
                            val active = Color(0xFF6F4EF6)
                            val color by animateColorAsState(if (pressed) active else base, label = "support_link_color")
                            Text(
                                text = supportButtonText,
                                fontSize = 14.sp,
                                color = color,
                                textAlign = TextAlign.Center,
                                textDecoration = if (pressed) TextDecoration.Underline else TextDecoration.None,
                                modifier = Modifier
                                    .padding(vertical = 8.dp, horizontal = 12.dp)
                                    .clickable(interactionSource = interaction, indication = null) {
                                        openWebPage(context, url)
                                    }
                            )
                        }
                    }

                    // X 닫기 버튼 (옵션)
                    if (isDismissible && onDismiss != null) {
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
                                tint = Color(0xFF999999),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


private fun openPlayStore(context: Context, packageName: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = "market://details?id=$packageName".toUri()
        setPackage("com.android.vending")
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        val webIntent = Intent(Intent.ACTION_VIEW).apply {
            data = "https://play.google.com/store/apps/details?id=$packageName".toUri()
        }
        context.startActivity(webIntent)
    }
}

private fun openWebPage(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    context.startActivity(intent)
}