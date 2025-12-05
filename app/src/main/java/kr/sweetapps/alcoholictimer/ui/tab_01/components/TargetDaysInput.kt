package kr.sweetapps.alcoholictimer.ui.tab_01.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.R

/**
 * [NEW] 목표 일수 입력 컴포넌트 (Extracted from StartScreen)
 *
 * 사용자가 금주 목표 기간을 입력하는 UI 컴포넌트입니다.
 * - 숫자와 단위("일")가 베이스라인 정렬되어 있습니다.
 * - 터치 시 자동으로 전체 선택됩니다.
 * - 숫자만 입력 가능하며, 최대 4자리까지 입력할 수 있습니다.
 *
 * @param value 현재 목표 일수
 * @param onValueChange 값이 변경될 때 호출되는 콜백
 * @param onDone 키보드 완료 버튼을 눌렀을 때 호출되는 콜백
 * @param modifier 레이아웃 수정자
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun TargetDaysInput(
    value: Int,
    onValueChange: (Int) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    // [NEW] FocusRequester for programmatic focus control
    val targetFocusRequester = remember { FocusRequester() }

    // [NEW] Track focus state
    var isFocused by remember { mutableStateOf(false) }

    // [NEW] TextField state management
    var targetText by remember {
        mutableStateOf(
            TextFieldValue(
                text = value.toString(),
                selection = TextRange(value.toString().length)
            )
        )
    }

    // [NEW] Focus and Keyboard controllers
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // [NEW] Detect keyboard visibility and clear focus when keyboard is dismissed
    val ime = WindowInsets.ime
    val imeBottom = ime.getBottom(density)
    val isImeVisible = imeBottom > 0

    LaunchedEffect(isImeVisible) {
        if (!isImeVisible && isFocused) {
            // Keyboard was dismissed - clear focus to hide cursor
            focusManager.clearFocus()
        }
    }

    // [NEW] Update TextField when value changes externally (e.g., badge click)
    // Don't overwrite if user is currently editing empty field
    LaunchedEffect(value) {
        if (targetText.text.isEmpty() && isFocused) {
            // Only update when not focused and empty
            return@LaunchedEffect
        }
        if (value != 0 || targetText.text.isNotEmpty()) {
            val newText = value.toString()
            targetText = TextFieldValue(
                text = newText,
                selection = TextRange(newText.length)
            )
        }
    }

    // [FIXED] 텍스트 정렬 제거 - Row가 정렬을 담당하므로 불필요
    // textAlign을 제거하여 BasicTextField가 불필요하게 여백을 차지하지 않도록 함
    val numberTextStyle = MaterialTheme.typography.displayLarge.copy(
        color = colorResource(id = R.color.color_indicator_days),
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        fontSize = 72.sp
        // textAlign 제거 - 부모 Row의 Arrangement.Center가 정렬 담당
    )

    // [REFACTORED] Unit text style
    val unitTextStyle = MaterialTheme.typography.headlineSmall.copy(
        color = Color.Gray,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(bottom = 16.dp)
    ) {
        // [REFACTORED] Simplified layout - no transparent text trick
        // Fixed width for stable layout
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    // Trigger focus and selection
                    coroutineScope.launch {
                        try {
                            targetFocusRequester.requestFocus()
                            keyboardController?.show()
                            delay(50)
                            targetText = targetText.copy(
                                selection = TextRange(0, targetText.text.length)
                            )
                        } catch (_: Exception) {}
                    }
                }
                .padding(vertical = 12.dp) // Safe padding to prevent clipping
        ) {
            // [CENTERED LAYOUT] 동적 너비로 변경 - 숫자 길이에 따라 자동 조절
            androidx.compose.foundation.text.BasicTextField(
                value = targetText,
                onValueChange = { newValue ->
                    // Filter: digits only, max 4 chars
                    val filtered = newValue.text.filter { it.isDigit() }.take(4)

                    // Always update the text field state (even if empty)
                    targetText = TextFieldValue(
                        text = filtered,
                        selection = TextRange(filtered.length)
                    )

                    // Notify parent only if not empty
                    if (filtered.isNotEmpty()) {
                        val parsedDays = filtered.toIntOrNull()?.coerceIn(1, 9999) ?: 1
                        onValueChange(parsedDays)
                    }
                    // If empty, keep UI empty but don't call onValueChange
                    // This allows user to clear the field without auto-fill
                },
                modifier = Modifier
                    .width(IntrinsicSize.Min) // [FIXED] 실제 텍스트 너비만큼만 차지
                    .focusRequester(targetFocusRequester)
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        if (focusState.isFocused) {
                            // Select all text with slight delay to avoid keyboard animation conflict
                            coroutineScope.launch {
                                try {
                                    delay(50)
                                    targetText = TextFieldValue(
                                        text = targetText.text,
                                        selection = TextRange(0, targetText.text.length)
                                    )
                                } catch (_: Exception) {}
                            }
                        }
                    },
                textStyle = numberTextStyle,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        // If empty, set default value 21
                        val finalDays = if (targetText.text.isEmpty()) {
                            21
                        } else {
                            val parsed = targetText.text.toIntOrNull() ?: 21
                            parsed.coerceIn(1, 9999)
                        }

                        onValueChange(finalDays)
                        targetText = TextFieldValue(
                            text = finalDays.toString(),
                            selection = TextRange(finalDays.toString().length)
                        )
                        try { keyboardController?.hide() } catch (_: Exception) {}
                        focusManager.clearFocus()
                        onDone()
                    }
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(
                    colorResource(id = R.color.color_indicator_days)
                ),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.Center) {
                        if (targetText.text.isEmpty()) {
                            Text(
                                text = "0",
                                style = numberTextStyle.copy(
                                    color = colorResource(id = R.color.color_indicator_days)
                                        .copy(alpha = 0.3f)
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Unit text
            Text(
                text = stringResource(R.string.target_days_unit),
                style = unitTextStyle
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom hint
        Text(
            text = stringResource(R.string.target_days_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = colorResource(id = R.color.color_hint_gray),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * [PREVIEW] 다양한 입력 상태 비교 프리뷰
 * "3일", "21일", "1000일" 입력 상태를 비교하여 중앙 정렬이 올바르게 작동하는지 확인
 */
@Preview(
    name = "TargetDaysInput - 다양한 입력 상태",
    showBackground = true,
    backgroundColor = 0xFFEEEDE9
)
@Composable
private fun TargetDaysInputComparisonPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // "3일" 입력 상태 (짧은 숫자)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "3일 입력 (짧은 숫자)",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                Spacer(Modifier.height(8.dp))
                TargetDaysInput(
                    value = 3,
                    onValueChange = {},
                    onDone = {}
                )
            }

            HorizontalDivider()

            // "21일" 입력 상태 (기본값)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "21일 입력 (기본값)",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                Spacer(Modifier.height(8.dp))
                TargetDaysInput(
                    value = 21,
                    onValueChange = {},
                    onDone = {}
                )
            }

            HorizontalDivider()

            // "1000일" 입력 상태 (긴 숫자)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "1000일 입력 (긴 숫자)",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                Spacer(Modifier.height(8.dp))
                TargetDaysInput(
                    value = 1000,
                    onValueChange = {},
                    onDone = {}
                )
            }
        }
    }
}
