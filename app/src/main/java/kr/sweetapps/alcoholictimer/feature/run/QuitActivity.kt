package kr.sweetapps.alcoholictimer.feature.run

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.core.ui.BaseActivity
import kr.sweetapps.alcoholictimer.core.ui.StandardScreenWithBottomButton
import kr.sweetapps.alcoholictimer.core.ui.LayoutConstants
import kr.sweetapps.alcoholictimer.core.util.FormatUtils
import kr.sweetapps.alcoholictimer.feature.start.StartActivity
import kr.sweetapps.alcoholictimer.core.ui.AppElevation
import kr.sweetapps.alcoholictimer.core.ui.AppBorder
import kr.sweetapps.alcoholictimer.core.util.Constants
import kr.sweetapps.alcoholictimer.core.ads.InterstitialAdManager
import kr.sweetapps.alcoholictimer.BuildConfig
import kr.sweetapps.alcoholictimer.core.ui.AdmobBanner
import androidx.activity.compose.BackHandler

class QuitActivity : BaseActivity() {
    override fun getScreenTitle(): String = getString(R.string.quit_title)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BaseScreen(applyBottomInsets = false, manageBottomAreaExternally = true, content = { QuitScreen() }) }
        // 전면광고 프리로드(이미 로드된 경우 내부에서 무시됨)
        InterstitialAdManager.preload(applicationContext)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuitScreen() {
    val context = LocalContext.current
    val activity = context as? QuitActivity

    // 뒤로가기 시 취소 버튼과 동일하게 RunActivity로 복귀
    BackHandler(enabled = true) {
        activity?.finish()
    }

    val intent = activity?.intent
    val elapsedDays = intent?.getIntExtra("elapsed_days", 0) ?: 0
    val elapsedHours = intent?.getIntExtra("elapsed_hours", 0) ?: 0
    val elapsedMinutes = intent?.getIntExtra("elapsed_minutes", 0) ?: 0
    val savedMoney = intent?.getDoubleExtra("saved_money", 0.0) ?: 0.0
    val savedHours = intent?.getDoubleExtra("saved_hours", 0.0) ?: 0.0
    val lifeGainDays = intent?.getDoubleExtra("life_gain_days", 0.0) ?: 0.0
    val sharedPref = context.getSharedPreferences(Constants.USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
    val targetDays = sharedPref.getFloat(Constants.PREF_TARGET_DAYS, 30f)

    var isPressed by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    StandardScreenWithBottomButton(
        topContent = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LayoutConstants.CARD_CORNER_RADIUS),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD), // lowered from CARD_HIGH
                border = BorderStroke(AppBorder.Hairline, colorResource(id = R.color.color_border_light)) // added for depth after elevation reduction
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(LayoutConstants.CARD_PADDING),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1f)) {
                        Text("🤔", fontSize = 48.sp, modifier = Modifier.padding(bottom = 12.dp))
                    }
                    Text(
                        text = stringResource(id = R.string.quit_confirm_title),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.quit_confirm_subtitle),
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center
                    )
                }
            }
            StatisticsCardsSection(
                elapsedDays = elapsedDays,
                elapsedHours = elapsedHours,
                elapsedMinutes = elapsedMinutes,
                savedMoney = savedMoney,
                savedHours = savedHours,
                lifeGainDays = lifeGainDays
            )
        },
        bottomButton = {
            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(48.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(106.dp)) {
                        // 배경 원 (회색)
                        CircularProgressIndicator(
                            progress = { 1f }, modifier = Modifier.size(106.dp),
                            color = Color(0xFFE0E0E0), strokeWidth = 4.dp, trackColor = Color.Transparent
                        )
                        // 진행 상태 원 (빨간색)
                        if (isPressed) {
                            CircularProgressIndicator(
                                progress = { progress }, modifier = Modifier.size(106.dp),
                                color = Color(0xFFD32F2F), strokeWidth = 4.dp, trackColor = Color.Transparent
                            )
                        }
                        // 중지 버튼
                        Card(
                            modifier = Modifier.size(96.dp).pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown(); isPressed = true; progress = 0f
                                    val job = coroutineScope.launch {
                                        try {
                                            val duration = 1500L
                                            val startMs = System.currentTimeMillis()
                                            while (progress < 1f && isPressed) {
                                                val elapsed = System.currentTimeMillis() - startMs
                                                progress = (elapsed.toFloat() / duration).coerceAtMost(1f)
                                                delay(16)
                                            }
                                            if (progress >= 1f && isPressed) {
                                                // 롱프레스 완료 로그
                                                Log.d("QuitActivity", "롱프레스 완료 - 금주 종료 처리 시작")

                                                // 완료 처리: 기록 저장 + 진행 상태 정리
                                                try {
                                                    saveCompletedRecord(
                                                        context = context,
                                                        startTime = System.currentTimeMillis() - (elapsedDays * 24L * 60 * 60 * 1000),
                                                        endTime = System.currentTimeMillis(),
                                                        targetDays = targetDays,
                                                        actualDays = elapsedDays
                                                    )
                                                    Log.d("QuitActivity", "기록 저장 완료")
                                                } catch (t: Throwable) {
                                                    Log.e("QuitActivity", "saveCompletedRecord 실패", t)
                                                }
                                                try {
                                                    sharedPref.edit {
                                                        remove(Constants.PREF_START_TIME)
                                                        putBoolean(Constants.PREF_TIMER_COMPLETED, true)
                                                    }
                                                    Log.d("QuitActivity", "진행 상태 업데이트 완료: timer_completed=true")
                                                } catch (t: Throwable) {
                                                    Log.e("QuitActivity", "진행 상태 업데이트 실패", t)
                                                }

                                                // StartActivity로의 안전한 전환 로직을 람다로 정의
                                                val navigateToStart: () -> Unit = {
                                                    Log.d("QuitActivity", "StartActivity로 이동 시작")
                                                    val act = activity
                                                    if (act != null) {
                                                        val i = Intent(act, StartActivity::class.java).apply {
                                                            // 금주 종료 후에는 새로운 Task로 시작하여 완전히 초기화
                                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                            // 스플래시 화면 스킵 플래그 추가 (내부 네비게이션)
                                                            putExtra("skip_splash", true)
                                                        }
                                                        try {
                                                            act.startActivity(i)
                                                            Log.d("QuitActivity", "StartActivity 실행 성공")
                                                        } finally {
                                                            act.finish()
                                                        }
                                                    } else {
                                                        try {
                                                            val i = Intent(context, StartActivity::class.java).apply {
                                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                                // 스플래시 화면 스킵 플래그 추가 (내부 네비게이션)
                                                                putExtra("skip_splash", true)
                                                            }
                                                            context.startActivity(i)
                                                        } catch (t: Throwable) {
                                                            Log.e("QuitActivity", "StartActivity 진입 실패", t)
                                                        }
                                                    }
                                                }

                                                // 전면광고 노출 시도: 성공 시 닫힌 후 navigateToStart 수행, 실패/차단 시 즉시 전환
                                                val actForAd = activity
                                                if (actForAd != null) {
                                                    val showed = InterstitialAdManager.maybeShowIfEligible(actForAd) { navigateToStart() }
                                                    if (!showed) {
                                                        if (BuildConfig.DEBUG) {
                                                            // 디버그: 최대 2.5초 대기하며 로드 되면 즉시 표시, 실패/타임아웃 시 전환
                                                            var handled = false
                                                            InterstitialAdManager.addLoadListener { success ->
                                                                if (!handled) {
                                                                    handled = true
                                                                    if (success) {
                                                                        val s2 = InterstitialAdManager.maybeShowIfEligible(actForAd) { navigateToStart() }
                                                                        if (!s2) navigateToStart()
                                                                    } else {
                                                                        navigateToStart()
                                                                    }
                                                                }
                                                            }
                                                            InterstitialAdManager.preload(actForAd.applicationContext)
                                                            coroutineScope.launch {
                                                                delay(2500)
                                                                if (!handled) { handled = true; navigateToStart() }
                                                            }
                                                        } else {
                                                            // 릴리즈: 정책 미충족/미로딩 시 즉시 전환 + 다음 기회 대비 프리로드
                                                            navigateToStart()
                                                            InterstitialAdManager.preload(actForAd.applicationContext)
                                                        }
                                                    }
                                                } else {
                                                    // 비정상 컨텍스트: 즉시 전환
                                                    navigateToStart()
                                                }
                                            }
                                        } catch (t: Throwable) {
                                            Log.e("QuitActivity", "중지 제스처 처리 중 오류", t)
                                        }
                                    }
                                    waitForUpOrCancellation(); isPressed = false; job.cancel()
                                    coroutineScope.launch {
                                        while (progress > 0f) { progress = (progress - 0.1f).coerceAtLeast(0f); delay(16) }
                                    }
                                }
                            },
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(containerColor = if (isPressed) Color(0xFFD32F2F) else Color(0xFFE53935)),
                            elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD_HIGH)
                        ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.cd_stop), tint = Color.White, modifier = Modifier.size(48.dp))
                        } }
                    }
                    Card(
                        onClick = {
                            // 취소: RunActivity로 복귀 (금주 계속 진행)
                            (context as? QuitActivity)?.finish()
                        },
                        modifier = Modifier.size(96.dp),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)),
                        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD_HIGH)
                    ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayArrow, contentDescription = stringResource(id = R.string.cd_continue), tint = Color.White, modifier = Modifier.size(48.dp))
                    } }
                }
            }
        }
        // bottomAd = { AdmobBanner() } // moved to MainActivity BaseScaffold during Phase-1
    )
}

@Composable
fun StatisticsCardsSection(
    elapsedDays: Int,
    elapsedHours: Int,
    elapsedMinutes: Int,
    savedMoney: Double,
    savedHours: Double,
    lifeGainDays: Double
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LayoutConstants.STAT_ROW_SPACING)
    ) {
        val totalDaysDecimal = elapsedDays.toDouble() + (elapsedHours.toDouble() / 24.0) + (elapsedMinutes.toDouble() / (24.0 * 60.0))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LayoutConstants.STAT_ROW_SPACING)
        ) {
            kr.sweetapps.alcoholictimer.feature.detail.components.DetailStatCard(
                value = stringResource(R.string.unit_days_format, totalDaysDecimal),
                label = stringResource(id = R.string.stat_total_days),
                modifier = Modifier.weight(1f),
                valueColor = colorResource(id = R.color.color_indicator_days)
            )
            kr.sweetapps.alcoholictimer.feature.detail.components.DetailStatCard(
                value = FormatUtils.formatMoney(context, savedMoney),
                label = stringResource(id = R.string.stat_saved_money_short),
                modifier = Modifier.weight(1f),
                valueColor = colorResource(id = R.color.color_indicator_money)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LayoutConstants.STAT_ROW_SPACING)
        ) {
            kr.sweetapps.alcoholictimer.feature.detail.components.DetailStatCard(
                value = stringResource(R.string.unit_hours_format, savedHours),
                label = stringResource(id = R.string.stat_saved_hours_short),
                modifier = Modifier.weight(1f),
                valueColor = colorResource(id = R.color.color_indicator_hours)
            )
            kr.sweetapps.alcoholictimer.feature.detail.components.DetailStatCard(
                value = FormatUtils.daysToDayHourString(context, lifeGainDays, 2),
                label = stringResource(id = R.string.indicator_title_life_gain),
                modifier = Modifier.weight(1f),
                valueColor = colorResource(id = R.color.color_indicator_life)
            )
        }
    }
}

private fun saveCompletedRecord(
    context: Context,
    startTime: Long,
    endTime: Long,
    targetDays: Float,
    actualDays: Int
) {
    try {
        val sharedPref = context.getSharedPreferences(Constants.USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        val record = JSONObject().apply {
            put("id", System.currentTimeMillis().toString())
            put("startTime", startTime)
            put("endTime", endTime)
            put("targetDays", targetDays.toInt())
            put("actualDays", actualDays)
            put("isCompleted", true)
            put("status", "quit")
            put("createdAt", System.currentTimeMillis())
        }
        val current = sharedPref.getString(Constants.PREF_SOBRIETY_RECORDS, "[]") ?: "[]"
        val array = JSONArray(current)
        array.put(record)
        sharedPref.edit { putString(Constants.PREF_SOBRIETY_RECORDS, array.toString()) }
        Log.d("QuitActivity", "기록 저장 완료: $record")
    } catch (e: Exception) {
        Log.e("QuitActivity", "기록 저장 오류", e)
    }
}

@Preview(showBackground = true)
@Composable
fun QuitScreenPreview() { QuitScreen() }
