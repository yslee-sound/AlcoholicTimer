// [NEW] Tab01 Refactoring: RunScreen moved to tab_01/screens
package kr.sweetapps.alcoholictimer.ui.tab_01.screens

import android.app.Activity
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import java.util.Locale
import kr.sweetapps.alcoholictimer.util.constants.Constants
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.BuildConfig
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import kr.sweetapps.alcoholictimer.util.debug.DebugSettings

@Composable
fun RunScreenComposable(
    onRequestQuit: (() -> Unit)? = null,
    onCompletedNavigateToDetail: ((String) -> Unit)? = null,
    onRequireBackToStart: (() -> Unit)? = null
) {
    val context = LocalContext.current

    // [FIX] Activity Scope ViewModel을 안전하게 가져오기
    val activity = context as? ComponentActivity
    val viewModel: kr.sweetapps.alcoholictimer.ui.tab_01.viewmodel.Tab01ViewModel = if (activity != null) {
        androidx.lifecycle.viewmodel.compose.viewModel(viewModelStoreOwner = activity)
    } else {
        // Fallback: Activity가 없으면 기본 ViewModel 생성 (이런 경우는 거의 없지만 안전장치)
        androidx.lifecycle.viewmodel.compose.viewModel()
    }

    // Local layout constants for RunScreen - keep local to avoid changing global constants
    // Unified horizontal padding for the whole Run screen. Use this single constant to keep card widths consistent.
    val RUN_HORIZONTAL_PADDING = 20.dp

    // [REMOVED] 사용하지 않는 변수 제거 (2026-01-04)
    // RUN_TOP_GROUP_TOP_PADDING, RUN_TOP_GROUP_CHIP_SPACING, runStatAlignments
    // RUN_CARDS_VERTICAL_SPACING_TOP, RUN_CARD_CONTENT_HORIZONTAL_PADDING, RUN_CARD_CONTENT_VERTICAL_PADDING


    // [FIX] 뒤로 가기 시 앱 최소화 (타이머 유지)
    // 타이머 앱 특성상 사용자가 뒤로 가기를 누르는 의도는 앱을 끄는 것이 아니라
    // 타이머를 켜둔 채 홈 화면으로 나가려는 것이므로 백그라운드로 이동
    BackHandler(enabled = true) {
        (context as? Activity)?.moveTaskToBack(true)
    }

    val isPreview = LocalInspectionMode.current
    val isDemoMode = DebugSettings.isDemoModeEnabled(context)

    // [FIX] Get timer data from ViewModel instead of SharedPreferences
    val startTime by viewModel.startTime.collectAsState()
    val targetDays by viewModel.targetDays.collectAsState()
    val timerCompleted by viewModel.timerCompleted.collectAsState()
    val elapsedMillisFromVM by viewModel.elapsedMillis.collectAsState()

    // [NEW] 타이머 리스트 상태 구독 (2026-01-05)
    val timers by viewModel.timers.collectAsState()
    val currentTimerIndex by viewModel.currentTimerIndex.collectAsState()

    // [NEW] Pager 상태 초기화 (2026-01-05)
    val showAddButton = timers.size < 3
    val pageCount = timers.size + if (showAddButton) 1 else 0
    val pagerState = rememberPagerState(
        initialPage = currentTimerIndex,
        pageCount = { pageCount }
    )

    // [NEW] Pager 페이지 변경 감지 (2026-01-05)
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage < timers.size) {
            viewModel.setCurrentTimerIndex(pagerState.currentPage)
        }
    }

    // SharedPreferences only for indicator state (not timer critical)
    val sp = if (isPreview) null else context.getSharedPreferences(Constants.USER_SETTINGS_PREFS, Context.MODE_PRIVATE)

    if (!isPreview && !isDemoMode) {
        LaunchedEffect(startTime, timerCompleted) {
            // [FIX] startTime이 0이어도, 만약 '완료된 상태(timerCompleted)'라면
            // MainActivity가 FinishedScreen으로 보낼 것이므로, 여기서 StartScreen으로 쫓아내면 안 됨!
            // 오직 '완료되지 않았는데(timerCompleted == false) startTime이 0'인 경우만 데이터 오류로 판단
            if (startTime == 0L && !timerCompleted) {
                onRequireBackToStart?.invoke()
            }
        }
    }

    // [FIX] dayInMillis는 고정 상수 사용
    val dayInMillis = Constants.DAY_IN_MILLIS

    // [CHANGED] Demo Mode 체크 제거 - 항상 실제 시간 사용 (2025-12-25)
    // 테스트 모드에서 시간 가속 시 Run 화면도 실시간으로 반영됨
    val elapsedMillis = if (isPreview) {
        2 * dayInMillis // Preview: 2 days
    } else {
        elapsedMillisFromVM // 항상 ViewModel의 실제 시간 사용
    }

    // [FIX] displayElapsedMillis는 elapsedMillis와 동일 (이미 가속됨)
    val displayElapsedMillis = elapsedMillis

    // [FIX] 통계 계산용 경과 일수 (가상 시간 기준)
    val elapsedDaysFloat = remember(displayElapsedMillis) {
        displayElapsedMillis / Constants.DAY_IN_MILLIS.toFloat()
    }

    // [CHANGED] 레벨 계산: '꽉 채운 일수' 기준 (floor 방식, +1 제거) (2025-12-25)
    // 예: 2.7일 → floor(2.7) = 2일, 0.5일 → floor(0.5) = 0일
    val levelDays = remember(displayElapsedMillis) {
        (displayElapsedMillis / Constants.DAY_IN_MILLIS).toInt() // floor 연산
    }

    // [REMOVED] 상단 카드 제거로 인해 사용하지 않는 통계 변수들 제거 (2026-01-04)
    // levelInfo, levelNumber, levelDisplayText, goalDaysText,
    // userSettings, costVal, freqVal, drinkHoursVal, currencySymbol,
    // weeks, savedMoney, savedHours, lifeGainDays, savedMoneyDisplay, formattedLifeGain

    // [NEW] 남은 일수 계산 (올림 처리로 타이머 시작 직후 1일 표시)
    // 예: 목표 1일, 경과 0.01일 → 남은 0.99일 → ceil → 1일
    val remainingDays = remember(targetDays, elapsedDaysFloat) {
        kotlin.math.ceil(targetDays - elapsedDaysFloat).toInt().coerceAtLeast(0)
    }

    // [FIX] 중앙 타이머 표시: displayElapsedMillis 사용 (배속 반영)
    val elapsedHours = ((displayElapsedMillis % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)).toInt()
    val elapsedMinutes = ((displayElapsedMillis % (60 * 60 * 1000)) / (60 * 1000)).toInt()
    val elapsedSeconds = ((displayElapsedMillis % (60 * 1000)) / 1000).toInt()
    val progressTimeText = String.format(Locale.getDefault(), "%02d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds)

    val totalTargetMillis = remember(targetDays) { (targetDays * Constants.DAY_IN_MILLIS).toLong() }
    val progress = remember(displayElapsedMillis, totalTargetMillis) {
        if (totalTargetMillis > 0) (displayElapsedMillis.toFloat() / totalTargetMillis).coerceIn(0f, 1f) else 0f
    }

    // [REMOVED] currentIndicator 및 toggleIndicator 제거 - 경과 일수와 시간을 동시에 표시 (2025-12-25)

    // [REMOVED] 타이머 완료 감지 로직을 UI에서 제거
    // 이제 TimerTimeManager와 Tab01ViewModel에서 자동으로 처리됨
    // 사용자가 어느 화면에 있든 타이머 완료 시 자동으로 DetailScreen으로 이동

    // [MODIFIED] StandardScreenWithBottomButton 제거, HorizontalPager 기반 스크롤 Column으로 변경 (2026-01-05)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEEEDE9)) // screenBackground
    ) {
        // 배경 장식
        Box(modifier = Modifier.matchParentSize().background(Brush.verticalGradient(0.0f to Color.Transparent, 1.0f to Color.Transparent)))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RUN_HORIZONTAL_PADDING)
        ) {
            // [NEW] 상단 여백 추가 (2026-01-04)
            Spacer(modifier = Modifier.height(20.dp))

            // [NEW] HorizontalPager로 타이머 카드 영역 (2026-01-05)
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 0.dp),
                pageSpacing = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                if (page < timers.size) {
                    // [기존 타이머 카드]
                    // [NEW] 페이지별 그라데이션 생성 (2026-01-05)
                    val cardGradient = getCardGradient(page)

                    ExistingTimerCard(
                        timerData = timers[page],
                        displayElapsedMillis = displayElapsedMillis,
                        targetDays = targetDays,
                        elapsedDaysFloat = elapsedDaysFloat,
                        remainingDays = remainingDays,
                        progressTimeText = progressTimeText,
                        progress = progress,
                        backgroundBrush = cardGradient // [NEW] 그라데이션 전달 (2026-01-05)
                    )
                } else {
                    // [새 타이머 추가 카드]
                    AddTimerCard(
                        onClick = {
                            viewModel.addNewTimer()
                            android.util.Log.d("RunScreen", "[NEW] Add timer clicked")
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // [NEW] Pager 인디케이터 (2026-01-05)
            if (pageCount > 1) {
                PagerIndicator(
                    pageCount = pageCount,
                    currentPage = pagerState.currentPage
                )
            }

            // [NEW] 네이티브 광고 영역 (메인 카드와 명언 사이) (2026-01-04)
            Spacer(modifier = Modifier.height(16.dp))
            NativeAdItem()
            // [MODIFIED] QuoteDisplay 내부 vertical padding 6dp를 고려하여 10dp 추가 (총 16dp) (2025-12-24)
            Spacer(modifier = Modifier.height(10.dp))

            // [NEW] 공통 컴포넌트 사용 (StartScreen과 동일한 디자인 & 2줄 제한 로직 적용)
            // [MODIFIED] 불필요한 bottom padding 제거 - QuoteDisplay 내부 패딩 사용 (2025-12-24)
            kr.sweetapps.alcoholictimer.ui.tab_01.components.QuoteDisplay()

            // [NEW] STOP 버튼을 스크롤 최하단으로 이동 (2025-12-24)
            // [MODIFIED] QuoteDisplay 내부 하단 패딩 6dp를 고려하여 26dp 추가 (총 32dp) (2025-12-24)
            Spacer(modifier = Modifier.height(26.dp))

            // 중앙 정렬을 위한 Box
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ModernStopButtonSimple(onStop = {
                    onRequestQuit?.invoke()
                })
            }

            // 바닥 여백
            Spacer(modifier = Modifier.height(100.dp)) // [MODIFIED] 24dp → 100dp로 변경 (2025-12-24)
        }
    } // Box 닫기
} // RunScreenComposable 닫기

// [REMOVED] ModernProgressIndicatorSimple 함수 제거 - 메인 카드에 통합됨 (2026-01-04)

@Composable
private fun ModernStopButtonSimple(onStop: () -> Unit, modifier: Modifier = Modifier) {
    // [FAB_UNIFIED] 시스템 폰트 스케일의 영향을 받지 않는 고정 크기 FloatingActionButton
    val density = LocalDensity.current
    val buttonSizePx = with(density) { 77.dp.toPx() }
    val buttonSize = with(density) { (buttonSizePx / density.density).dp }
    val iconSizePx = with(density) { 39.dp.toPx() }
    val iconSize = with(density) { (iconSizePx / density.density).dp }

    FloatingActionButton(
        onClick = onStop,
        modifier = modifier.requiredSize(buttonSize),
        containerColor = colorResource(id = R.color.color_stop_button),
        shape = CircleShape
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(id = R.string.cd_stop),
            tint = Color.White,
            modifier = Modifier.requiredSize(iconSize)
        )
    }
}

// RunScreen.kt 맨 아래 함수 교체

@Composable
fun RunStatChip(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconRes: Int? = null,
    iconBg: Color? = null,
    contentAlignment: Alignment.Horizontal = Alignment.CenterHorizontally
) {
    val density = LocalDensity.current
    // 카드 높이는 그대로 유지
    val cardHeightPx = with(density) { 110.dp.toPx() }
    val cardHeight = with(density) { (cardHeightPx / density.density).dp }
    val iconSizePx = with(density) { 32.dp.toPx() }
    val iconSize = with(density) { (iconSizePx / density.density).dp }

    Card(
        modifier = modifier.requiredHeight(cardHeight),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            // [FIX] 잘림 방지: 상하 패딩을 10dp -> 8dp로 줄여 수직 공간 확보
            modifier = Modifier.fillMaxSize().padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = contentAlignment,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. 아이콘 영역
            Box(
                modifier = Modifier.height(36.dp),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.requiredSize(iconSize)
                    )
                } else {
                    iconRes?.let { res ->
                        Image(
                            painter = painterResource(id = res),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(color),
                            modifier = Modifier.requiredSize(iconSize)
                        )
                    }
                }
            }

            // 2. 숫자 영역 (폰트 크기 및 두께 추가 감소)
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val textMeasurer = rememberTextMeasurer()
                val density = LocalDensity.current
                val maxPixels = with(density) { maxWidth.toPx() }

                // [FIX] 두께 감소: Bold -> SemiBold
                val baseStyle = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111111)
                )

                val calculatedSize = remember(value, maxPixels) {
                    // [FIX] 최대 크기 감소: 18f -> 16f
                    var currentSize = 16f
                    val minSize = 10f

                    while (currentSize > minSize) {
                        val result = textMeasurer.measure(
                            text = AnnotatedString(value),
                            style = baseStyle.copy(fontSize = currentSize.sp)
                        )
                        // 가로폭 85% 제한 유지
                        if (result.size.width <= maxPixels * 0.85f) break
                        currentSize -= 1f
                    }
                    currentSize.coerceAtLeast(minSize).sp
                }

                Text(
                    text = value,
                    style = baseStyle.copy(fontSize = calculatedSize),
                    color = Color(0xFF111111),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 3. 라벨 영역
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = colorResource(id = R.color.color_stat_title_gray),
                textAlign = TextAlign.Center,
                maxLines = 1,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
            )
        }
    }
}

private fun saveCompletedRecord(context: Context, startTime: Long, endTime: Long, targetDays: Float, actualDays: Int) {
    try {
        val sharedPref = context.getSharedPreferences(Constants.USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        val recordId = System.currentTimeMillis().toString()
        val isCompleted = actualDays >= targetDays
        val status = if (isCompleted) "completed" else "in_progress"
        val record = org.json.JSONObject().apply {
            put("id", recordId); put("startTime", startTime); put("endTime", endTime); put("targetDays", targetDays.toInt()); put("actualDays", actualDays); put("isCompleted", isCompleted); put("status", status); put("createdAt", System.currentTimeMillis())
        }
        val recordsJson = sharedPref.getString(Constants.PREF_SOBRIETY_RECORDS, "[]") ?: "[]"
        val list = try { org.json.JSONArray(recordsJson) } catch (_: Exception) { org.json.JSONArray() }
        list.put(record)
        sharedPref.edit().putString(Constants.PREF_SOBRIETY_RECORDS, list.toString()).apply()
    } catch (_: Exception) { }
}

/**
 * [NEW] 네이티브 광고 컴포넌트 (타이머 메인 화면용) (2025-12-24)
 * - 프로그레스 바와 명언 사이에 배치
 * - RecordsScreen의 NativeAdItem과 동일한 구조
 */
@Composable
private fun NativeAdItem() {
    val context = LocalContext.current

    // [NEW] NativeAdManager의 캐시 키 (실행 화면 전용)
    val screenKey = "run_screen"

    val adUnitId = try { BuildConfig.ADMOB_NATIVE_ID } catch (_: Throwable) { "ca-app-pub-3940256099942544/2247696110" }

    var nativeAd by remember { mutableStateOf<com.google.android.gms.ads.nativead.NativeAd?>(null) }
    // [NEW] 광고 로드 실패 플래그 (No Fill 대응, 2025-12-24)
    var adLoadFailed by remember { mutableStateOf(false) }

    // [REFACTORED] 광고 로드 로직 - 캐시 우선 사용 (2026-01-02)
    LaunchedEffect(Unit) {
        // [FIX] 백그라운드에서 MobileAds 초기화 (ANR 방지, v1.1.9)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                com.google.android.gms.ads.MobileAds.initialize(context)
            } catch (initEx: Exception) {
                android.util.Log.w("NativeAd", "MobileAds.initialize failed: ${initEx.message}")
            }
        }

        try {
            // [핵심] NativeAdManager를 통한 캐싱된 광고 가져오기 또는 새로 로드
            kr.sweetapps.alcoholictimer.ui.ad.NativeAdManager.getOrLoadAd(
                context = context,
                screenKey = screenKey,
                onAdReady = { ad ->
                    android.util.Log.d("NativeAd", "[$screenKey] Ad ready (cached or loaded)")
                    nativeAd = ad
                },
                onAdFailed = {
                    android.util.Log.w("NativeAd", "[$screenKey] Ad load failed")
                    adLoadFailed = true
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("NativeAd", "[$screenKey] Failed setting up ad", e)
            adLoadFailed = true
        }
    }

    // [NEW] 광고 로드 실패 시 UI 아예 숨김 (Graceful Degradation, 2025-12-24)
    if (adLoadFailed) {
        return // 광고 영역 렌더링하지 않음
    }

    // 광고 카드 (로딩 중에는 고정 높이, 로딩 완료 후 콘텐츠에 맞춤)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (nativeAd == null) Modifier.height(250.dp)
                else Modifier.wrapContentHeight()
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        if (nativeAd != null) {
            // 광고 로드 완료 시
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    val adView = com.google.android.gms.ads.nativead.NativeAdView(ctx)

                    val container = android.widget.LinearLayout(ctx).apply {
                        orientation = android.widget.LinearLayout.VERTICAL
                        setBackgroundColor(android.graphics.Color.WHITE)
                        setPadding(40, 40, 40, 40)
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    // 상단: 아이콘 + 광고 배지 + 헤드라인
                    val headerRow = android.widget.LinearLayout(ctx).apply {
                        orientation = android.widget.LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER_VERTICAL
                    }

                    val iconView = android.widget.ImageView(ctx).apply {
                        layoutParams = android.widget.LinearLayout.LayoutParams(110, 110)
                    }
                    headerRow.addView(iconView)

                    val textContainer = android.widget.LinearLayout(ctx).apply {
                        orientation = android.widget.LinearLayout.VERTICAL
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            marginStart = 24
                        }
                    }

                    // 광고 배지
                    val badgeView = android.widget.TextView(ctx).apply {
                        text = "광고"
                        textSize = 10f
                        setTextColor(android.graphics.Color.WHITE)
                        setBackgroundColor(android.graphics.Color.parseColor("#FBC02D"))
                        setPadding(8, 2, 8, 2)
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = 4
                        }
                    }
                    textContainer.addView(badgeView)

                    val headlineView = android.widget.TextView(ctx).apply {
                        textSize = 15f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        setTextColor(android.graphics.Color.parseColor("#111827"))
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    }
                    textContainer.addView(headlineView)

                    headerRow.addView(textContainer)
                    container.addView(headerRow)

                    // 중간: Body
                    val bodyView = android.widget.TextView(ctx).apply {
                        textSize = 13f
                        setPadding(0, 24, 0, 32)
                        setTextColor(android.graphics.Color.parseColor("#6B7280"))
                        maxLines = 2
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    }
                    container.addView(bodyView)

                    // 하단: 버튼
                    val callToActionView = android.widget.Button(ctx).apply {
                        setBackgroundColor(android.graphics.Color.parseColor("#F3F4F6"))
                        setTextColor(android.graphics.Color.parseColor("#4B5563"))
                        textSize = 13f
                        stateListAnimator = null
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }
                    container.addView(callToActionView)

                    adView.addView(container)
                    adView.iconView = iconView
                    adView.headlineView = headlineView
                    adView.bodyView = bodyView
                    adView.callToActionView = callToActionView
                    adView
                },
                update = { adView ->
                    val ad = nativeAd!!
                    (adView.headlineView as android.widget.TextView).text = ad.headline
                    (adView.bodyView as android.widget.TextView).text = ad.body
                    (adView.callToActionView as android.widget.Button).text = ad.callToAction ?: "자세히 보기"
                    ad.icon?.let { (adView.iconView as android.widget.ImageView).setImageDrawable(it.drawable) }
                    adView.setNativeAd(ad)
                },
                modifier = Modifier.fillMaxWidth().wrapContentHeight()
            )
        } else {
            // 로딩 중 플레이스홀더
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = colorResource(id = R.color.color_progress_primary)
                )
            }
        }
    }
}

// Preview: show the real Run screen composable as-is
@Preview(name = "RunScreen - Live Composable", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun RunScreenLivePreview() {
    // Call the actual RunScreenComposable so preview matches runtime UI
    RunScreenComposable(onRequestQuit = {}, onCompletedNavigateToDetail = {}, onRequireBackToStart = {})
}

/**
 * [NEW] 기존 타이머 카드 UI (HorizontalPager용) (2026-01-05)
 * [UPDATED] 페이지별 그라데이션 배경 적용 (2026-01-05)
 */
@Composable
private fun ExistingTimerCard(
    timerData: kr.sweetapps.alcoholictimer.ui.tab_01.viewmodel.Tab01ViewModel.TimerData,
    displayElapsedMillis: Long,
    targetDays: Float,
    elapsedDaysFloat: Float,
    remainingDays: Int,
    progressTimeText: String,
    progress: Float,
    backgroundBrush: Brush, // [NEW] 그라데이션 배경 (2026-01-05)
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val bigCardHeightPx = with(density) { 260.dp.toPx() }
    val bigCardHeight = with(density) { (bigCardHeightPx / density.density).dp }

    Card(
        modifier = modifier.fillMaxWidth().requiredHeight(bigCardHeight),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(0.dp, Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // [UPDATED] 배경 이미지 제거, 그라데이션으로 대체 (2026-01-05)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = backgroundBrush)
            )

            // 카드 내용
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // [TOP] 경과 일수와 시간
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    val daysValue = String.format(Locale.getDefault(), "%.0f", kotlin.math.floor(elapsedDaysFloat.toDouble()))
                    val daysCount = kotlin.math.floor(elapsedDaysFloat.toDouble()).toInt()
                    val daysUnit = remember(daysCount) {
                        context.resources.getQuantityString(R.plurals.days_count, daysCount, daysCount).substringAfter(" ")
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = daysValue,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 72.sp,
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.55f),
                                    offset = Offset(0f, 2f),
                                    blurRadius = 4f
                                )
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = daysUnit,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Normal,
                                color = Color.White,
                                fontSize = 24.sp,
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.45f),
                                    offset = Offset(0f, 1f),
                                    blurRadius = 2f
                                )
                            ),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = progressTimeText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 24.sp,
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.45f),
                                offset = Offset(0f, 1f),
                                blurRadius = 2f
                            )
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // [BOTTOM] 진행률 바와 퍼센트
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 진행률 바
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x4DFFFFFF))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 퍼센트와 목표 아이콘
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${(progress * 100).toInt().coerceIn(0, 100)}%",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.45f),
                                    offset = Offset(0f, 1f),
                                    blurRadius = 2f
                                )
                            )
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.hourglassmedium),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$remainingDays",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.45f),
                                        offset = Offset(0f, 1f),
                                        blurRadius = 2f
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * [NEW] 타이머 추가 카드 UI (2026-01-05)
 */
@Composable
private fun AddTimerCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val bigCardHeightPx = with(density) { 260.dp.toPx() }
    val bigCardHeight = with(density) { (bigCardHeightPx / density.density).dp }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .requiredHeight(bigCardHeight)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5) // 연한 회색
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(2.dp, Color(0xFFE0E0E0))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // [ICON] 큰 + 아이콘
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Timer",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // [TEXT] 안내 문구
                Text(
                    text = stringResource(R.string.add_new_timer_message),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color(0xFF757575),
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * [NEW] Pager 인디케이터 (2026-01-05)
 */
@Composable
private fun PagerIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().height(50.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { iteration ->
            val color = if (currentPage == iteration) {
                Color(0xFF1E40AF) // 활성 페이지 - 진한 파란색
            } else {
                Color(0xFFBDBDBD) // 비활성 페이지 - 연한 회색
            }
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(color)
                    .size(8.dp)
            )
        }
    }
}

/**
 * [NEW] 타이머 카드별 그라데이션 생성 함수 (2026-01-05)
 *
 * @param page 페이지 인덱스 (0, 1, 2)
 * @return 페이지별 그라데이션 Brush
 *
 * 색상 테마:
 * - Card 0 (금주): Pink & Orange (따뜻함, 활기)
 * - Card 1 (금연): Blue & Mint (청량함, 상쾌함)
 * - Card 2 (습관): Purple & Indigo (차분함, 집중)
 */
private fun getCardGradient(page: Int): Brush {
    return when (page) {
        0 -> Brush.linearGradient(
            colors = listOf(
                Color(0xFFFF9A9E), // Soft Pink
                Color(0xFFFAD0C4)  // Peach
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
        1 -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF4FACFE), // Bright Blue
                Color(0xFF00F2FE)  // Cyan/Mint
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
        else -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF667EEA), // Purple
                Color(0xFF764BA2)  // Deep Violet
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }
}

