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
import androidx.compose.ui.res.painterResource
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
import android.content.res.Configuration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.sweetapps.alcoholictimer.util.constants.Constants

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
    previewMode: Boolean = false
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
    // [NEW] 메뉴 확장 상태
    var showMenu by remember { mutableStateOf(false) }
    val accentColor = if (isCompleted) BluePrimaryLight else AmberSecondaryLight

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
                else -> "yyyy-MM-dd - h:mm a"
            }, Locale.getDefault()
        ).apply { timeZone = TimeZone.getDefault() }
    }
    val displayDateTime = remember(startTime) {
        if (startTime > 0) dateTimeFormat.format(Date(startTime)) else {
            val nowFmt = SimpleDateFormat(
                when (Locale.getDefault().language) {
                    "ko" -> "a h:mm"
                    "ja" -> "H:mm"
                    else -> "h:mm a"
                }, Locale.getDefault()
            ).apply { timeZone = TimeZone.getDefault() }.format(Date())
            context.getString(R.string.detail_today_time, nowFmt)
        }
    }

    val totalDurationMillis = if (startTime > 0) endTime - startTime else {
        // [FIX] 타이머 테스트 모드를 고려한 동적 DAY_IN_MILLIS
        val dayInMillis = if (!previewMode) {
            Constants.getDayInMillis(context)
        } else {
            Constants.DAY_IN_MILLIS
        }
        actualDays * dayInMillis
    }

    // [FIX] 타이머 테스트 모드를 고려한 동적 시간 계산
    val dayInMillis = if (!previewMode) {
        Constants.getDayInMillis(context)
    } else {
        Constants.DAY_IN_MILLIS
    }
    val totalHours = totalDurationMillis / (dayInMillis / 24.0)
    val totalDays = totalDurationMillis / dayInMillis.toDouble()

    // guard platform/runtime-dependent settings for preview safety
    val selectedCost: String
    val selectedFrequency: String
    val selectedDuration: String
    if (!previewMode) {
        val settings = Constants.getUserSettings(context)
        selectedCost = settings.first
        selectedFrequency = settings.second
        selectedDuration = settings.third
    } else {
        // preview defaults
        selectedCost = "0"
        selectedFrequency = "0"
        selectedDuration = "0"
    }

    // Numeric values for calculations (ensure correct numeric types)
    val costVal: Int = if (!previewMode) Constants.DrinkingSettings.getCostValue(selectedCost) else 1000
    val freqVal: Double = if (!previewMode) Constants.DrinkingSettings.getFrequencyValue(selectedFrequency) else 1.0
    val drinkHoursVal: Double = if (!previewMode) Constants.DrinkingSettings.getDurationValue(selectedDuration) else 3.0
    val hangoverHoursVal: Double = Constants.DrinkingSettings.HANGOVER_HOURS

    val exactWeeks = totalHours / (24.0 * 7.0)
    val savedMoney = (exactWeeks * freqVal * costVal.toDouble()).roundToInt()
    val savedHoursExact = (exactWeeks * freqVal * (drinkHoursVal + hangoverHoursVal))
    val achievementRate = ((totalDays / targetDays) * 100.0).coerceAtMost(100.0)
    val lifeExpectancyIncrease = totalDays / 30.0

    // Preview-safe display strings for values that normally require Context/FormatUtils
    val savedMoneyStr = if (!previewMode) kr.sweetapps.alcoholictimer.util.CurrencyManager.formatMoneyNoDecimals(savedMoney.toDouble(), context) else "₩${savedMoney}"
    val savedHoursStr = if (!previewMode) FormatUtils.formatHoursWithUnitFixed(context, savedHoursExact, 1) else String.format(Locale.getDefault(), "%.1f%s", savedHoursExact, stringResource(id = R.string.unit_hour))
    val lifeGainStr = if (!previewMode) FormatUtils.daysToDayHourStringFixed(context, lifeExpectancyIncrease, 1) else String.format(Locale.getDefault(), "%.1f %s", lifeExpectancyIncrease, stringResource(id = R.string.unit_day))

    // Keep topBar outside the fontScale override so its typography matches other screens.
    val navBottomRaw = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val imeBottomRaw = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    Scaffold(
        topBar = {
            BackTopBar(
                title = if (previewMode) "Detail" else stringResource(id = R.string.detail_title),
                onBack = if (previewMode) ({}) else onBack,
                trailingContent = {
                    // [NEW] 세로 3점 메뉴로 변경
                    Box {
                        IconButton(onClick = { if (!previewMode) showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = if (previewMode) null else "메뉴",
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
                                    Text(text = "기록 삭제")
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
            )
        },
        containerColor = Color(0xFFEEEDE9)
    ) { paddingValues ->
        // Use system fontScale for body (no 0.9x override)
        val navBottom = if (previewMode) 0.dp else navBottomRaw
        val imeBottom = if (previewMode) 0.dp else imeBottomRaw
        val effectiveBottom = if (previewMode) 0.dp else maxOf(navBottom, imeBottom)

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD_HIGH),
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
                                    color = accentColor
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (achievementRate / 100.0).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = accentColor,
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
                        valueColor = colorResource(id = R.color.color_indicator_days)
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
                if (UiConstants.BANNER_TOP_GAP > 0.dp) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(UiConstants.BANNER_TOP_GAP).background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
                HorizontalDivider(thickness = AppBorder.Hairline, color = Color(0xFFE0E0E0))
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
                    // no need to set showDeleteDialog false before navigating back
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
        elevation = AppElevation.CARD_HIGH,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        // 정렬 변경: 카드 내부를 가로 전체로 채우는 Column으로 감싸고 우측 정렬 설정
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            val resolvedValueColor = if (valueColor != Color.Unspecified) valueColor else MaterialTheme.colorScheme.onSurface
            Text(
                text = value,
                color = resolvedValueColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = Color(0xFF718096),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true, name = "DetailScreen - Light")
@Composable
fun DetailScreenPreviewLight() {
    AlcoholicTimerTheme(darkTheme = false) {
        DetailScreen(
            startTime = System.currentTimeMillis() - 3L * 24L * 60L * 60L * 1000L,
            endTime = System.currentTimeMillis(),
            targetDays = 30f,
            actualDays = 3,
            isCompleted = false,
            onBack = {},
            onDelete = { _, _ -> /* no-op in preview */ },
            onDeleted = {},
            previewMode = true
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DetailScreen - Dark")
@Composable
fun DetailScreenPreviewDark() {
    AlcoholicTimerTheme(darkTheme = true) {
        DetailScreen(
            startTime = System.currentTimeMillis() - 45L * 24L * 60L * 60L * 1000L,
            endTime = System.currentTimeMillis(),
            targetDays = 90f,
            actualDays = 45,
            isCompleted = true,
            onBack = {},
            onDelete = { _, _ -> },
            onDeleted = {},
            previewMode = true
        )
    }
}

// A very safe preview that avoids any Android runtime APIs and uses static strings.
@Preview(showBackground = true, name = "DetailScreen - Safe Light")
@Composable
fun DetailScreenPreviewSafeLight() {
    AlcoholicTimerTheme(darkTheme = false) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header card
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Start: 2025-11-14 - 3:00 PM", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "End: 2025-11-17 - 11:00 AM", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "3.0", style = MaterialTheme.typography.displayLarge, color = Color(0xFF1E88E5))
                    Text(text = "days", style = MaterialTheme.typography.titleLarge)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailStatCard(value = "3.0", label = "Total Days", modifier = Modifier.weight(1f), valueColor = Color(0xFF2E7D32))
                DetailStatCard(value = "₩3,000", label = "Saved", modifier = Modifier.weight(1f), valueColor = Color(0xFF1E88E5))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailStatCard(value = "9h", label = "Saved Hours", modifier = Modifier.weight(1f), valueColor = Color(0xFFFBC02D))
                DetailStatCard(value = "1.5d", label = "Life Gain", modifier = Modifier.weight(1f), valueColor = Color(0xFF6B46C1))
            }
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DetailScreen - Safe Dark")
@Composable
fun DetailScreenPreviewSafeDark() {
    AlcoholicTimerTheme(darkTheme = true) {
        DetailScreenPreviewSafeLight()
    }
}

@Preview(showBackground = true, name = "BackTopBar - Light")
@Composable
fun BackTopBarPreviewLight() {
    AlcoholicTimerTheme(darkTheme = false) {
        BackTopBar(title = "Detail", onBack = {}, trailingContent = {
            Icon(painter = painterResource(id = R.drawable.ic_x), contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
        })
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "BackTopBar - Dark")
@Composable
fun BackTopBarPreviewDark() {
    AlcoholicTimerTheme(darkTheme = true) {
        BackTopBar(title = "Detail", onBack = {}, trailingContent = {
            Icon(painter = painterResource(id = R.drawable.ic_x), contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
        })
    }
}
