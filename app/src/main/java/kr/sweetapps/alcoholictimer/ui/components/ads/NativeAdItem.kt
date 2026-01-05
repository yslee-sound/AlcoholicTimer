// [NEW] 공통 네이티브 광고 컴포넌트 (2026-01-05)
// [REFACTORED] 여러 화면에 중복 구현된 NativeAdItem을 통합
package kr.sweetapps.alcoholictimer.ui.components.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import kr.sweetapps.alcoholictimer.BuildConfig
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.ad.NativeAdManager

/**
 * 공통 네이티브 광고 컴포넌트
 *
 * @param screenKey 광고 캐싱을 위한 화면별 고유 키 (예: "run_screen", "records_screen")
 * @param modifier Composable modifier
 *
 * **사용 예시:**
 * ```kotlin
 * NativeAdItem(screenKey = "my_screen")
 * ```
 *
 * **주요 기능:**
 * - NativeAdManager를 통한 광고 캐싱
 * - 광고 로드 실패 시 Graceful Degradation (UI 숨김)
 * - 로딩 중 플레이스홀더 표시
 */
@Composable
fun NativeAdItem(
    screenKey: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var nativeAd by remember { mutableStateOf<com.google.android.gms.ads.nativead.NativeAd?>(null) }
    var adLoadFailed by remember { mutableStateOf(false) }

    // [FIX] 광고 로드 로직 - 캐시 우선 사용
    LaunchedEffect(screenKey) {  // [CHANGED] Unit -> screenKey (화면별 독립 실행)
        // 백그라운드에서 MobileAds 초기화 (ANR 방지)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                com.google.android.gms.ads.MobileAds.initialize(context)
            } catch (initEx: Exception) {
                android.util.Log.w("NativeAd", "MobileAds.initialize failed: ${initEx.message}")
            }
        }

        try {
            // NativeAdManager를 통한 캐싱된 광고 가져오기 또는 새로 로드
            NativeAdManager.getOrLoadAd(
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

    // [CRITICAL FIX] 메모리 누수 방지: 화면 종료 시 광고 destroy (2026-01-05)
    DisposableEffect(screenKey) {
        onDispose {
            android.util.Log.d("NativeAd", "[$screenKey] Disposing ad - calling NativeAdManager.destroyAd()")
            NativeAdManager.destroyAd(screenKey)
            nativeAd = null  // State 초기화
        }
    }

    // 광고 로드 실패 시 UI 숨김 (Graceful Degradation)
    if (adLoadFailed) {
        return
    }

    // 광고 카드 (로딩 중: 고정 높이, 로딩 완료: 콘텐츠에 맞춤)
    Card(
        modifier = modifier
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

