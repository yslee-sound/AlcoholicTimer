package kr.sweetapps.alcoholictimer.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
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
 * 긴급 상황 앱 전환 안내 팝업
 * 앱 스토어 출시 중지, 서비스 종료 등 긴급 상황에서 사용
 */
@Composable
fun EmergencyRedirectDialog(
    title: String = "중요 안내",
    description: String,
    newAppName: String,
    newAppPackage: String,
    buttonText: String = "새 앱 설치하기",
    supportUrl: String? = null,
    supportButtonText: String = "자세한 내용 보기",
    canMigrateData: Boolean = false,
    isDismissible: Boolean = false,
    onDismiss: (() -> Unit)? = null,
    badgeText: String? = "서비스 종료",
    migrationMessage: String? = null
) {
    val context = LocalContext.current
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
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 16.dp
                )
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
                        // 상단 아이콘(교체 가능 리소스)
                        Image(
                            painter = painterResource(id = R.drawable.ic_splash_logo),
                            contentDescription = null,
                            modifier = Modifier.size(96.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // 제목
                        Text(
                            text = title,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A),
                            textAlign = TextAlign.Center
                        )

                        // 배지(옵션) — "서비스 종료" 등
                        badgeText?.let {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = Color(0xFFFFEBEE), // 연한 레드
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = it,
                                    color = Color(0xFFD32F2F),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 설명
                        Text(
                            text = description,
                            fontSize = 15.sp,
                            color = Color(0xFF666666),
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // 파란 안내 박스 — 데이터 이전 메시지
                        val infoText = migrationMessage ?: "${newAppName}에서 기존 계정과 데이터를 그대로 사용할 수 있습니다."
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFD6E4FF),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = infoText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1F2A44),
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 설치 버튼
                        Button(
                            onClick = { openPlayStore(context, newAppPackage) },
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

                        // 하단 링크(옵션) – 링크 스타일(리플 제거 + 눌림 시 밑줄/색상 변화)
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
        data = Uri.parse("market://details?id=$packageName")
        setPackage("com.android.vending")
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        val webIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        }
        context.startActivity(webIntent)
    }
}

private fun openWebPage(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}