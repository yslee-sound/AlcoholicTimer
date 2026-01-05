// [REFACTORED] 순수 UI 컴포넌트로 전환 - State Hoisting 패턴 적용 (2026-01-05)
// UI는 렌더링만 담당, 생명주기는 부모 Screen이 관리
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import kr.sweetapps.alcoholictimer.R
import com.google.android.gms.ads.nativead.NativeAd

/**
 * [UI 이원화] 네이티브 광고 스타일 정의 (2026-01-05)
 * - CARD: 탭1, 탭2 (My Health Analysis) - 카드 스타일 (그림자, 둥근 모서리)
 * - FLAT: 일기 화면, 응원챌린지 피드 - 평면 스타일 (투명 배경, 여백 최소화)
 */
enum class NativeAdViewStyle {
    CARD,  // 카드 스타일 (기존 디자인)
    FLAT   // 평면 스타일 (피드용)
}

/**
 * [STATE HOISTING] 순수 UI 컴포넌트 - 네이티브 광고 렌더링 전용
 *
 * **설계 원칙:**
 * - 이 컴포넌트는 광고를 로드하거나 해제하지 않습니다
 * - nativeAd 객체를 파라미터로 받아 렌더링만 수행합니다
 * - 광고 생명주기는 부모 Screen(RecordsScreen 등)에서 관리합니다
 *
 * @param nativeAd 렌더링할 광고 객체 (null이면 로딩 표시)
 * @param viewStyle 광고 스타일 (CARD: 카드형, FLAT: 평면형)
 * @param isLoading 로딩 중 여부 (기본값: nativeAd == null)
 * @param modifier Composable modifier
 *
 * **사용 예시:**
 * ```kotlin
 * // 탭1, 탭2: Card 스타일
 * NativeAdItem(nativeAd = ad, viewStyle = NativeAdViewStyle.CARD)
 *
 * // 피드: Flat 스타일
 * NativeAdItem(nativeAd = ad, viewStyle = NativeAdViewStyle.FLAT)
 * ```
 */
@Composable
fun NativeAdItem(
    nativeAd: NativeAd?,
    viewStyle: NativeAdViewStyle = NativeAdViewStyle.CARD,  // [NEW] 기본값은 CARD
    modifier: Modifier = Modifier,
    isLoading: Boolean = nativeAd == null
) {
    // [REMOVED] LaunchedEffect, DisposableEffect 모두 제거
    // 광고 로드/해제는 부모 Screen이 담당

    // [UI 이원화] viewStyle에 따라 Card 또는 Flat 렌더링
    when (viewStyle) {
        NativeAdViewStyle.CARD -> {
            // Card 스타일 (탭1, 탭2용)
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .then(
                        if (isLoading) Modifier.height(250.dp)
                        else Modifier.wrapContentHeight()
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                NativeAdContent(nativeAd, isLoading, isCardStyle = true)
            }
        }
        NativeAdViewStyle.FLAT -> {
            // Flat 스타일 (피드용)
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .then(
                        if (isLoading) Modifier.height(250.dp)
                        else Modifier.wrapContentHeight()
                    )
            ) {
                NativeAdContent(nativeAd, isLoading, isCardStyle = false)
            }
        }
    }
}

/**
 * [INTERNAL] 광고 콘텐츠 렌더링 (Card/Flat 공통)
 */
@Composable
private fun NativeAdContent(
    nativeAd: NativeAd?,
    isLoading: Boolean,
    isCardStyle: Boolean
) {
    if (nativeAd != null && !isLoading) {
            // 광고 로드 완료 시
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    val adView = com.google.android.gms.ads.nativead.NativeAdView(ctx)

                    val container = android.widget.LinearLayout(ctx).apply {
                        orientation = android.widget.LinearLayout.VERTICAL
                        // [UI 이원화] 스타일별 배경/패딩 설정
                        if (isCardStyle) {
                            setBackgroundColor(android.graphics.Color.WHITE)
                            setPadding(40, 40, 40, 40)
                        } else {
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            setPadding(40, 16, 40, 16)
                        }
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
                    (adView.headlineView as android.widget.TextView).text = nativeAd.headline
                    (adView.bodyView as android.widget.TextView).text = nativeAd.body
                    (adView.callToActionView as android.widget.Button).text = nativeAd.callToAction ?: "자세히 보기"
                    nativeAd.icon?.let { (adView.iconView as android.widget.ImageView).setImageDrawable(it.drawable) }
                    adView.setNativeAd(nativeAd)
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
