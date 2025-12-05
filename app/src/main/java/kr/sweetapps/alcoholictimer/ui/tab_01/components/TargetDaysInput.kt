package kr.sweetapps.alcoholictimer.ui.tab_01.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
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
@Composable
fun TargetDaysInput(
    value: Int,
    onValueChange: (Int) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    // [NEW] FocusRequester for programmatic focus control
    val targetFocusRequester = remember { FocusRequester() }

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

    // [NEW] Update TextField when value changes externally (e.g., badge click)
    LaunchedEffect(value) {
        val newText = value.toString()
        targetText = TextFieldValue(
            text = newText,
            selection = TextRange(newText.length)
        )
    }

    // [REFACTORED] Simplified number text style - no lineHeight constraint to prevent clipping
    val numberTextStyle = MaterialTheme.typography.displayLarge.copy(
        color = colorResource(id = R.color.color_indicator_days),
        textAlign = TextAlign.Center,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        fontSize = 72.sp
        // lineHeight removed - let the font breathe naturally
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
            // Input field
            androidx.compose.foundation.text.BasicTextField(
                value = targetText,
                onValueChange = { newValue ->
                    // Filter: digits only, max 4 chars
                    val filtered = newValue.text.filter { it.isDigit() }.take(4)
                    if (filtered != targetText.text) {
                        targetText = TextFieldValue(
                            text = filtered,
                            selection = TextRange(filtered.length)
                        )
                        val parsedDays = filtered.toIntOrNull()?.coerceIn(1, 9999) ?: 21
                        onValueChange(parsedDays)
                    } else {
                        targetText = newValue.copy(text = filtered)
                    }
                },
                modifier = Modifier
                    .wrapContentWidth()
                    .focusRequester(targetFocusRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            targetText = TextFieldValue(
                                text = targetText.text,
                                selection = TextRange(0, targetText.text.length)
                            )
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
                        val parsed = targetText.text.toIntOrNull() ?: 21
                        val finalDays = parsed.coerceIn(1, 9999)
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

