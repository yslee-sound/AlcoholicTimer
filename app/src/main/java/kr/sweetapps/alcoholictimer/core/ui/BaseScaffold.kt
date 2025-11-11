package kr.sweetapps.alcoholictimer.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kr.sweetapps.alcoholictimer.core.ui.theme.AlcoholicTimerTheme

/**
 * 아주 간단한 공통 레이아웃(마이그레이션 임시 템플릿)
 * - 중앙 content는 NavHost가 렌더링
 * - 하단에 한 번만 호출되는 AdmobBanner를 배치
 *
 * 점진적 마이그레이션 과정에서 BaseActivity의 많은 기능을 여기로 옮길 예정입니다.
 */
@Composable
fun BaseScaffold(content: @Composable () -> Unit) {
    AlcoholicTimerTheme(darkTheme = false, applySystemBars = true) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 중앙 콘텐츠 (NavHost 등)
            Box(modifier = Modifier.weight(1f)) {
                content()
            }

            // 하단 분리선
            HorizontalDivider(thickness = 1.dp)

            // 하단 광고(한 번만 생성되어야 함)
            // navigationBarsPadding을 적용해 시스템 내비게이션(3버튼/제스처 영역)과 겹치지 않도록 합니다.
            Surface(
                modifier = Modifier.navigationBarsPadding().fillMaxWidth(),
                color = Color.White,
                shadowElevation = 0.dp,
                tonalElevation = 0.dp
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AdmobBanner()
                }
            }
        }
    }
}
