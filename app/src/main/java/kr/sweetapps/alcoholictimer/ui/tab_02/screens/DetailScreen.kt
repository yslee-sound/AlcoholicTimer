package kr.sweetapps.alcoholictimer.ui.tab_02.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.BuildConfig
import kr.sweetapps.alcoholictimer.ui.theme.AppElevation
import kr.sweetapps.alcoholictimer.ui.theme.AppBorder
import kr.sweetapps.alcoholictimer.ui.theme.UiConstants
import kr.sweetapps.alcoholictimer.ui.theme.AmberSecondaryLight
import kr.sweetapps.alcoholictimer.ui.theme.BluePrimaryLight
import kr.sweetapps.alcoholictimer.ui.tab_01.components.predictAnchoredBannerHeightDp
import kr.sweetapps.alcoholictimer.util.utils.FormatUtils
import kr.sweetapps.alcoholictimer.ui.components.AppCard
import kr.sweetapps.alcoholictimer.ui.components.BackTopBar
import kr.sweetapps.alcoholictimer.ui.theme.AlcoholicTimerTheme
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.sweetapps.alcoholictimer.util.constants.Constants
import kr.sweetapps.alcoholictimer.util.manager.CurrencyManager
import kr.sweetapps.alcoholictimer.ui.common.rememberUserSettingsState
import kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue  // [NEW] 메인 UI 색상

// Local layout constants for DetailScreen (tweak here to change paddings)
private val DETAIL_CARD_TOP_PADDING = 15.dp
private val DETAIL_CARD_HORIZONTAL_PADDING = 15.dp
private val DETAIL_CARD_CONTENT_PADDING = 20.dp // 20
private val DETAIL_CARD_VERTICAL_SPACING = 15.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    startTime: Long,
    endTime: Long,
    targetDays: Float,
    actualDays: Int,
    isCompleted: Boolean,
    onBack: () -> Unit,
    onDelete: ((Long, Long) -> Unit)? = null,
    onDeleted: (() -> Unit)? = null,
    onNavigateToHome: () -> Unit = {},
    previewMode: Boolean = false,
    showTopBar: Boolean = true,  // [기존] 타이틀바 표시 여부
    isResultMode: Boolean = false  // [NEW] 결과 모드 (타이머 완료 직후)
) {
    val context = LocalContext.current
    // Internal delete implementation (merged from old feature/detail) as a local function
    fun deleteImpl(s: Long, e: Long) {
        Log.d("DetailScreen", "deleteImpl called for start=${s} end=${e}")
        val sharedPref = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
        val jsonString = sharedPref.getString("sobriety_records", null)
        if (jsonString == null) {
            Log.d("DetailScreen", "no sobriety_records found in sharedPref")
            return
        }
        try {
            Log.d("DetailScreen", "currentRecordsJson=${jsonString}")
            val originalArray = org.json.JSONArray(jsonString)
            val newArray = org.json.JSONArray()
            var removed = 0
            for (i in 0 until originalArray.length()) {
                val obj = originalArray.getJSONObject(i)
                val sv = obj.optLong("startTime", obj.optLong("start_time", -1))
                val ev = obj.optLong("endTime", obj.optLong("end_time", -1))
                if (sv == s && ev == e) {
                    removed++
                    Log.d("DetailScreen", "matched and removing index=${i} sv=${sv} ev=${ev}")
                } else {
                    newArray.put(obj)
                }
            }
            if (removed > 0) {
                val committed = sharedPref.edit().putString("sobriety_records", newArray.toString()).commit()
                Log.d("DetailScreen", "removed=${removed} committed=${committed} remainingLen=${newArray.length()}")
                if (!committed) {
                    Log.e("DetailScreen", "SharedPreferences.commit() failed")
                    Toast.makeText(context, "기록 삭제 실패(저장 오류)", Toast.LENGTH_SHORT).show()
                } else {
                    // [FIX] 타이머 상태 초기화 (기록 삭제 시 타이머 완료 상태도 리셋)
                    sharedPref.edit().apply {
                        putBoolean(Constants.PREF_TIMER_COMPLETED, false)
                        putLong(Constants.PREF_START_TIME, 0L)
                        commit()
                    }
                    Log.d("DetailScreen", "타이머 상태 초기화 완료")

                    try { onDeleted?.invoke() } catch (_: Exception) {}
                    Toast.makeText(context, "기록이 삭제되었습니다", Toast.LENGTH_SHORT).show()
                }
                val afterJson = sharedPref.getString("sobriety_records", "[]") ?: "[]"
                Log.d("DetailScreen", "afterRecordsJson=${afterJson}")
            } else {
                Log.d("DetailScreen", "no matching record removed (removed=0)")
                Toast.makeText(context, "삭제할 기록을 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
            }
        } catch (ex: Exception) {
            Log.e("DetailScreen", "기록 삭제 중 오류", ex)
        }
    }

    val showDeleteDialog = remember { mutableStateOf(false) }
    // [NEW] 메뉴 확장 상태 (showTopBar가 false일 때는 메뉴 비활성화)
    var showMenu by remember { mutableStateOf(false) }
    val accentColor = if (isCompleted) MainPrimaryBlue else AmberSecondaryLight  // [FIX] 메인 UI 색상 적용 (#1E40AF)

    // banner visibility: ensure any hiding used for debug is guarded by BuildConfig.DEBUG (release validation)
    var shouldHideBanner by remember { mutableStateOf(if (BuildConfig.DEBUG) false else false) }
    // If previewMode requested, hide banners and simplify inset calculations to avoid preview crashes
    if (previewMode) {
        shouldHideBanner = true
    }

    val dateTimeFormat = remember {
        SimpleDateFormat(
            when (Locale.getDefault().language) {
                "ko" -> "yyyy-MM-dd - a h:mm"
                "ja" -> "yyyy年MM月dd日 - H:mm"
                else -> "yyyy-MM-dd - HH:mm" // [FIX] 24시간 표기법 적용 (인도네시아어 등, 2025-12-26)
            }, Locale.getDefault()
        ).apply { timeZone = TimeZone.getDefault() }
    }
    val displayDateTime = remember(startTime) {
        if (startTime > 0) dateTimeFormat.format(Date(startTime)) else {
            val nowFmt = SimpleDateFormat(
                when (Locale.getDefault().language) {
                    "ko" -> "a h:mm"
                    "ja" -> "H:mm"
                    else -> "HH:mm" // [FIX] 24시간 표기법 적용 (2025-12-26)
                }, Locale.getDefault()
            ).apply { timeZone = TimeZone.getDefault() }.format(Date())
            context.getString(R.string.detail_today_time, nowFmt)
        }
    }

    val totalDurationMillis = if (startTime > 0) endTime - startTime else {
        // [FIX] 통계 계산은 실제 시간 기준 (배속 적용 안 함)
        actualDays * Constants.DAY_IN_MILLIS
    }

    // [FIX] 통계 계산은 실제 시간 기준 (배속 적용 안 함)
    val dayInMillis = Constants.DAY_IN_MILLIS
    val totalHours = totalDurationMillis / (dayInMillis / 24.0)
    val totalDays = totalDurationMillis / dayInMillis.toDouble()

    // [NEW] 실시간 설정 변경 감지 - 탭4에서 설정을 바꾸면 즉시 반영됨
    val userSettings by rememberUserSettingsState(context)

    // guard platform/runtime-dependent settings for preview safety
    val selectedCost = if (!previewMode) userSettings.cost else "0"
    val selectedFrequency = if (!previewMode) userSettings.frequency else "0"
    val selectedDuration = if (!previewMode) userSettings.duration else "0"

    // Numeric values for calculations (ensure correct numeric types)
    val costVal: Int = if (!previewMode) Constants.DrinkingSettings.getCostValue(selectedCost) else 1000
    val freqVal: Double = if (!previewMode) Constants.DrinkingSettings.getFrequencyValue(selectedFrequency) else 1.0
    val drinkHoursVal: Double = if (!previewMode) Constants.DrinkingSettings.getDurationValue(selectedDuration) else 3.0

    val exactWeeks = totalHours / (24.0 * 7.0)
    val savedMoney = exactWeeks * freqVal * costVal.toDouble()  // [FIX] Double 유지 (Tab 1과 동일)
    val savedHoursExact = (exactWeeks * freqVal * drinkHoursVal)
    val achievementRate = ((totalDays / targetDays) * 100.0).coerceAtMost(100.0)
    val lifeExpectancyIncrease = totalDays / 30.0

    // Preview-safe display strings for values that normally require Context/FormatUtils
    val savedMoneyStr = if (!previewMode) {
        val roundedMoney = kotlin.math.round(savedMoney)
        CurrencyManager.formatMoneyNoDecimals(roundedMoney, context)
    } else "₩${savedMoney.toInt()}"
    val savedHoursStr = if (!previewMode) FormatUtils.formatHoursWithUnitFixed(context, savedHoursExact, 1) else String.format(Locale.getDefault(), "%.1f%s", savedHoursExact, stringResource(id = R.string.unit_hour))
    val lifeGainStr = if (!previewMode) FormatUtils.daysToDayHourStringFixed(context, lifeExpectancyIncrease, 1) else String.format(Locale.getDefault(), "%.1f %s", lifeExpectancyIncrease, stringResource(id = R.string.unit_day))

    // Keep topBar outside the fontScale override so its typography matches other screens.
    val navBottomRaw = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val imeBottomRaw = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    Scaffold(
        topBar = if (showTopBar) {
            {
                BackTopBar(
                    title = "", // [FIX] 모든 모드에서 제목 숨김
                    onBack = if (previewMode) ({}) else onBack,
                    trailingContent = if (isResultMode) {
                        // [NEW] 결과 모드일 때는 메뉴 숨김
                        null
                    } else {
                        {
                            // [기존] 일반 모드일 때만 3점 메뉴 표시
                            Box {
                                IconButton(onClick = {
                                    if (!previewMode && showTopBar) showMenu = true
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = if (previewMode) null else stringResource(R.string.cd_menu),
                                        tint = Color.Black
                                    )
                                }

                                // [NEW] 드롭다운 메뉴
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(text = stringResource(R.string.detail_menu_delete))
                                        },
                                        onClick = {
                                            showMenu = false
                                            if (!previewMode) {
                                                showDeleteDialog.value = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        } else {
            {}  // 타이틀바 없음
        },
        bottomBar = if (isResultMode && !previewMode) {
            // [NEW] 결과 모드일 때 하단에 '다시 시작하기' 버튼
            {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),  // [FIX] 시스템 네비게이션 바 영역 회피
                    color = Color.White,
                    shadowElevation = 0.dp  // [FIX] 그림자 제거
                ) {
                    Button(
                        onClick = onNavigateToHome,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MainPrimaryBlue,  // [FIX] 메인 UI 색상 적용 (#1E40AF)
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.detail_restart_button),  // [FIX] 다국어화
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            {}  // 일반 모드에는 bottomBar 없음
        },
        containerColor = Color.White  // [FIX] 전체 배경 하얀색
    ) { paddingValues ->
        // Use system fontScale for body (no 0.9x override)
        val navBottom = if (previewMode) 0.dp else navBottomRaw
        val imeBottom = if (previewMode) 0.dp else imeBottomRaw
        val effectiveBottom = if (previewMode) 0.dp else maxOf(navBottom, imeBottom)

        // [NEW] showTopBar가 false일 때 타이틀 공간만큼 상단 패딩 추가
        val topPadding = if (!showTopBar) 56.dp else 0.dp

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(top = topPadding)) {  // [NEW] 추가 상단 패딩
            // Scrollable content: use a scrollable Column without forcing it to fill remaining
            // space (remove weight). This prevents the content area from stretching and
            // producing a large empty background gap above the bottom banner/navigation.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = DETAIL_CARD_HORIZONTAL_PADDING, end = DETAIL_CARD_HORIZONTAL_PADDING)
                 ) {
                Spacer(modifier = Modifier.height(DETAIL_CARD_TOP_PADDING))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),  // [FIX] 연한 회색
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),  // [FIX] 엘리베이션 0으로 변경
                    border = BorderStroke(AppBorder.Hairline, colorResource(id = R.color.color_border_light))
                ) {
                    Column(modifier = Modifier.padding(DETAIL_CARD_CONTENT_PADDING)) {
                        Text(
                            text = "${stringResource(id = R.string.detail_start_label)} $displayDateTime",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFF718096)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${stringResource(id = R.string.detail_end_label)} ${dateTimeFormat.format(Date(endTime))}",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFF718096)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f", totalDays),
                                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = accentColor
                                )
                                Text(
                                    text = stringResource(id = R.string.unit_day),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFF718096)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(id = R.string.detail_progress_rate),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFF718096)
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f%%", achievementRate),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MainPrimaryBlue  // [FIX] 메인 UI 색상 적용 (#1E40AF)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (achievementRate / 100.0).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = MainPrimaryBlue,  // [FIX] 메인 UI 색상 적용 (#1E40AF)
                                trackColor = Color(0xFFE2E8F0)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = stringResource(id = R.string.detail_progress_current, String.format(Locale.getDefault(), "%.1f", totalDays)),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFF718096)
                                )
                                Text(
                                    text = stringResource(id = R.string.detail_progress_target, targetDays.toInt()),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFF718096)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(DETAIL_CARD_VERTICAL_SPACING))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(UiConstants.STAT_ROW_SPACING)
                ) {
                    DetailStatCard(
                        value = stringResource(R.string.unit_days_format, totalDays),
                        label = stringResource(id = R.string.stat_total_days),
                        modifier = Modifier.weight(1f),
                        valueColor = MainPrimaryBlue  // [FIX] 메인 UI 색상 적용 (#1E40AF)
                    )
                    DetailStatCard(
                        value = savedMoneyStr,
                        label = stringResource(id = R.string.stat_saved_money_short),
                        modifier = Modifier.weight(1f),
                        valueColor = colorResource(id = R.color.color_indicator_money)
                    )
                }
                Spacer(modifier = Modifier.height(DETAIL_CARD_VERTICAL_SPACING))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(UiConstants.STAT_ROW_SPACING)
                ) {
                    DetailStatCard(
                        value = savedHoursStr,
                        label = stringResource(id = R.string.stat_saved_hours_short),
                        modifier = Modifier.weight(1f),
                        valueColor = colorResource(id = R.color.color_indicator_hours)
                    )
                    DetailStatCard(
                        value = lifeGainStr,
                        label = stringResource(id = R.string.indicator_title_life_gain),
                        modifier = Modifier.weight(1f),
                        valueColor = colorResource(id = R.color.color_indicator_life)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (!shouldHideBanner) {
                // [REMOVED] BANNER_TOP_GAP Box - 회색 박스 제거
                // [REMOVED] HorizontalDivider - 회색 구분선 제거
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = effectiveBottom)
                        .height(if (previewMode) 56.dp else predictAnchoredBannerHeightDp()),
                    contentAlignment = Alignment.Center
                ) { /* AdmobBanner centralized - no-op */ }
            }
        }
    }
    if (showDeleteDialog.value) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog.value = false },
            title = {
                Text(text = stringResource(id = R.string.dialog_delete_title), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            },
            text = { Text(text = stringResource(id = R.string.dialog_delete_message), style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TextButton(onClick = {
                    // prefer caller-provided deletion; otherwise use internal deleteImpl which will call onDeleted
                    val action: (Long, Long) -> Unit = onDelete ?: { a, b -> deleteImpl(a, b) }
                    action(startTime, endTime)
                    // [FIX] Navigate back to previous screen (list) instead of home
                    onBack()
                }) {
                    Text(text = stringResource(id = R.string.dialog_delete_confirm), color = Color(0xFFE53E3E), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog.value = false }) {
                    Text(text = stringResource(id = R.string.dialog_cancel), color = Color(0xFF718096))
                }
            }
        )
    }
}

// --- Integrated DetailStatCard from feature.detail.components ---
@Composable
fun DetailStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified
) {
    AppCard(
        modifier = modifier,
        elevation = 0.dp,
        containerColor = Color(0xFFF3F4F6),
        border = null,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 12.dp, // [Space] 좌우 패딩을 16->12로 줄여서 텍스트 공간 더 확보
            vertical = 12.dp
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            val resolvedValueColor = if (valueColor != Color.Unspecified) valueColor else MaterialTheme.colorScheme.onSurface

            // 1. 값 (Value) 표시
            Text(
                text = value,
                color = resolvedValueColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 2. 라벨 (Label) 표시 - [FIX] AutoResizingText 적용
            AutoResizingText(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = Color(0xFF718096),
                textAlign = TextAlign.End, // 우측 정렬 유지
                maxLines = 1,              // 무조건 한 줄 유지
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(
    name = "기록 상세 화면",
    showBackground = true,
    showSystemUi = true
)
@Composable
fun DetailScreenPreview() {
    AlcoholicTimerTheme(darkTheme = false) {
        DetailScreen(
            startTime = System.currentTimeMillis() - 3L * 24L * 60L * 60L * 1000L,
            endTime = System.currentTimeMillis(),
            targetDays = 30f,
            actualDays = 3,
            isCompleted = false,
            onBack = {},
            onDelete = { _, _ -> },
            onDeleted = {},
            onNavigateToHome = {},
            previewMode = true
        )
    }
}

// [NEW] 텍스트가 공간을 넘치면 자동으로 폰트 크기를 줄여주는 유틸리티
@Composable
fun AutoResizingText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = 1
) {
    var resizedTextStyle by remember { mutableStateOf(style) }
    var shouldDraw by remember { mutableStateOf(false) }

    val defaultFontSize = style.fontSize

    Text(
        text = text,
        color = color,
        modifier = modifier.drawWithContent {
            if (shouldDraw) {
                drawContent()
            }
        },
        softWrap = false,
        style = resizedTextStyle,
        textAlign = textAlign,
        onTextLayout = { result ->
            // 텍스트가 영역을 넘치거나(didOverflowWidth), 라인을 넘어가면(didOverflowHeight)
            // 폰트 크기를 0.95배씩 줄여서 다시 시도
            if (result.didOverflowWidth || result.didOverflowHeight) {
                val newSize = resizedTextStyle.fontSize * 0.95f
                // 최소 10sp까지만 축소 허용 (너무 작아지면 가독성 해치므로 제한)
                if (newSize >= 10.sp) {
                    resizedTextStyle = resizedTextStyle.copy(fontSize = newSize)
                } else {
                    shouldDraw = true // 더 줄일 수 없으면 그냥 그림
                }
            } else {
                shouldDraw = true
            }
        },
        maxLines = maxLines
    )
}