package kr.sweetapps.alcoholictimer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    label: String = "",
    displayValues: List<String> = range.map { it.toString() },
    // [NEW] 각 항목의 활성화 여부 (true = 선택 가능, false = 비활성화)
    enabledStates: List<Boolean> = List(range.count()) { true }
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = value - range.first)
    val coroutineScope = rememberCoroutineScope()
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val itemHeight = 48.dp
    val visibleItemsCount = 5
    val visibleItemsMiddle = visibleItemsCount / 2

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val currentIndex = listState.firstVisibleItemIndex
            val currentValue = range.first + currentIndex
            // [NEW] 비활성화된 항목은 선택하지 못하게 함
            val isEnabled = enabledStates.getOrNull(currentIndex) ?: true
            if (currentValue != value && currentValue in range && isEnabled) {
                onValueChange(currentValue)
            } else if (!isEnabled) {
                // [NEW] 비활성화된 항목에 스크롤이 멈춘 경우, 가장 가까운 활성화된 항목으로 이동
                val nearestEnabledIndex = findNearestEnabledIndex(currentIndex, enabledStates)
                if (nearestEnabledIndex != -1) {
                    coroutineScope.launch {
                        listState.animateScrollToItem(nearestEnabledIndex)
                    }
                }
            }
        }
    }

    LaunchedEffect(value) {
        val targetIndex = value - range.first
        if (targetIndex != listState.firstVisibleItemIndex) {
            coroutineScope.launch { listState.animateScrollToItem(targetIndex) }
        }
    }

    Box(
        // 고정 너비(100.dp)를 제거하여 호출부에서 modifier로 너비를 제어할 수 있게 함
        modifier = modifier.height(itemHeight * visibleItemsCount),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(itemHeight).background(
                Color.Black.copy(alpha = 0.1f), RoundedCornerShape(8.dp)
            )
        )
        LazyColumn(
            state = listState,
            flingBehavior = snapBehavior,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = itemHeight * visibleItemsMiddle)
        ) {
            items(range.count()) { index ->
                val itemValue = range.first + index
                val displayValue = if (displayValues.isNotEmpty() && index < displayValues.size) {
                    displayValues[index]
                } else itemValue.toString()
                val isSelected = itemValue == value
                // [NEW] 비활성화 상태 확인
                val isEnabled = enabledStates.getOrNull(index) ?: true
                Box(
                    modifier = Modifier.fillMaxWidth().height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayValue,
                        fontSize = if (isSelected) 20.sp else 16.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        // [NEW] 비활성화된 항목은 회색으로 표시
                        color = if (!isEnabled) Color.LightGray else if (isSelected) Color.Black else Color.Gray,
                        textAlign = TextAlign.Center,
                        // [NEW] 비활성화된 항목은 더 투명하게 표시
                        modifier = Modifier.alpha(if (!isEnabled) 0.3f else if (isSelected) 1.0f else 0.6f)
                    )
                }
            }
        }
        if (label.isNotEmpty()) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 20.dp)
            )
        }
    }
}

/**
 * [NEW] 가장 가까운 활성화된 항목의 인덱스를 찾습니다
 * @param currentIndex 현재 인덱스
 * @param enabledStates 활성화 상태 리스트
 * @return 가장 가까운 활성화된 항목의 인덱스, 없으면 -1
 */
private fun findNearestEnabledIndex(currentIndex: Int, enabledStates: List<Boolean>): Int {
    if (enabledStates.isEmpty()) return -1

    // 현재 위치부터 양쪽으로 탐색
    var distance = 1
    while (distance < enabledStates.size) {
        // 아래쪽 탐색
        val lowerIndex = currentIndex + distance
        if (lowerIndex < enabledStates.size && enabledStates[lowerIndex]) {
            return lowerIndex
        }

        // 위쪽 탐색
        val upperIndex = currentIndex - distance
        if (upperIndex >= 0 && enabledStates[upperIndex]) {
            return upperIndex
        }

        distance++
    }

    return -1
}

