package kr.sweetapps.alcoholictimer.feature.addrecord

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.delay
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.core.data.RecordsDataLoader
import kr.sweetapps.alcoholictimer.core.model.SobrietyRecord
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.edit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecordScreenComposable(
    onFinished: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var targetDays by remember { mutableStateOf("0") }
    var startDate by remember { mutableLongStateOf(System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)) }
    var endDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var startTime by remember { mutableStateOf(Pair(9, 0)) }
    var endTime by remember { mutableStateOf(Pair(18, 0)) }

    val dateFormat = remember {
        val locale = Locale.getDefault()
        when (locale.language) {
            "ko" -> SimpleDateFormat("yyyy년 MM월 dd일", locale)
            "ja" -> SimpleDateFormat("yyyy年MM月dd日", locale)
            else -> SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)
        }
    }

    val startMillis by remember(startDate, startTime) {
        mutableLongStateOf(
            Calendar.getInstance().apply {
                timeInMillis = startDate
                set(Calendar.HOUR_OF_DAY, startTime.first)
                set(Calendar.MINUTE, startTime.second)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        )
    }
    val endMillis by remember(endDate, endTime) {
        mutableLongStateOf(
            Calendar.getInstance().apply {
                timeInMillis = endDate
                set(Calendar.HOUR_OF_DAY, endTime.first)
                set(Calendar.MINUTE, endTime.second)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        )
    }
    val nowMillis = System.currentTimeMillis()

    val isRangeInvalid = endMillis <= startMillis
    val isOngoing = endMillis > nowMillis
    val targetDaysInt = targetDays.toIntOrNull() ?: 0
    val isTargetValid = targetDays.isNotBlank() && targetDaysInt in 1..9999

    val actualDays = remember(startMillis, endMillis) {
        ((endMillis - startMillis).coerceAtLeast(0L) / (24 * 60 * 60 * 1000L)).toInt()
    }

    fun pickDateThenTime(initialDateMillis: Long, initialHour: Int, initialMinute: Int, onPicked: (dateMillis: Long, hour: Int, minute: Int) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = initialDateMillis }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                val pickedCal = Calendar.getInstance().apply { set(y, m, d) }
                TimePickerDialog(
                    context,
                    { _, h, min -> onPicked(pickedCal.timeInMillis, h, min) },
                    initialHour,
                    initialMinute,
                    true
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // 저장 로직: SharedPreferences에 기존 로직과 동일하게 저장
    fun persistRecord(record: SobrietyRecord): Boolean {
        return try {
            val currentRecords = RecordsDataLoader.loadSobrietyRecords(context).toMutableList()
            val hasTimeConflict = currentRecords.any { existing ->
                val newStart = record.startTime
                val newEnd = record.endTime
                val existingStart = existing.startTime
                val existingEnd = existing.endTime
                (newStart < existingEnd && newEnd > existingStart)
            }
            if (hasTimeConflict) return false
            currentRecords.add(record)
            currentRecords.sortByDescending { it.endTime }
            val sharedPref = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
            val jsonString = SobrietyRecord.toJsonArray(currentRecords)
            sharedPref.edit { putString("sobriety_records", jsonString) }
            true
        } catch (e: Exception) {
            android.util.Log.e("AddRecordComposable", "기록 저장 실패", e)
            false
        }
    }

    // keyboard/focus behavior for inline editing removed — no-op here

    Scaffold(
        topBar = {
            kr.sweetapps.alcoholictimer.core.ui.BackTopBar(title = stringResource(R.string.add_record_title), onBack = onCancel)
        },
        containerColor = Color.White,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { innerPadding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { /* no-op: outside-tap handling removed for inline field */ } }

        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .background(Color.White)
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable {
                                pickDateThenTime(startDate, startTime.first, startTime.second) { newDate, h, m ->
                                    startDate = newDate
                                    startTime = h to m
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.add_record_start_date_time), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Text(text = "${dateFormat.format(Date(startDate))} - ${String.format(Locale.getDefault(), "%02d:%02d", startTime.first, startTime.second)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable {
                                pickDateThenTime(endDate, endTime.first, endTime.second) { newDate, h, m ->
                                    endDate = newDate
                                    endTime = h to m
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.add_record_end_date_time), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Text(text = "${dateFormat.format(Date(endDate))} - ${String.format(Locale.getDefault(), "%02d:%02d", endTime.first, endTime.second)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    HorizontalDivider()

                    val keyboardController = LocalSoftwareKeyboardController.current
                    val targetDaysFocusRequester = remember { FocusRequester() }
                    // initialize selection at end so caret is placed after text by default
                    var targetDaysText by remember { mutableStateOf(TextFieldValue(text = targetDays, selection = TextRange(targetDays.length))) }
                    var isTargetDaysFocused by remember { mutableStateOf(false) }
                    var clearOnFocus by remember { mutableStateOf(false) }
                    // click trigger to reliably request focus from a LaunchedEffect (works better across devices)
                    var focusRequestTrigger by remember { mutableStateOf(0) }

                    // When field should request focus (triggered by click), request focus and show keyboard with short retries
                    LaunchedEffect(focusRequestTrigger) {
                        if (focusRequestTrigger > 0) {
                            repeat(3) { attempt ->
                                try {
                                    targetDaysFocusRequester.requestFocus()
                                    keyboardController?.show()
                                } catch (_: Exception) { }
                                delay(60L + attempt * 40L)
                            }
                        }
                    }

                    // When focus is actually acquired, enforce clearing/select behavior to ensure first key replaces '0'
                    LaunchedEffect(isTargetDaysFocused) {
                        if (isTargetDaysFocused) {
                            if (clearOnFocus || targetDays == "0" || targetDaysText.text == "0") {
                                targetDaysText = TextFieldValue(text = "", selection = TextRange(0))
                                clearOnFocus = false
                            }
                            // ensure keyboard visible
                            keyboardController?.show()
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable {
                                // If value is "0", prepare empty text so first keypress replaces it reliably
                                clearOnFocus = (targetDays == "0")
                                targetDaysText = if (targetDays == "0") {
                                    TextFieldValue(text = "", selection = TextRange(0))
                                } else {
                                    TextFieldValue(text = targetDays, selection = TextRange(targetDays.length))
                                }
                                // request focus and show keyboard immediately and also trigger the retry launcher
                                targetDaysFocusRequester.requestFocus()
                                keyboardController?.show()
                                focusRequestTrigger++
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.add_record_target_days), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        TextField(
                            value = targetDaysText,
                            onValueChange = { newText ->
                                // Only allow numeric input, backspace, and limited navigation
                                if (newText.text.all { it.isDigit() } || newText == TextFieldValue("")) {
                                    targetDaysText = newText.copy(
                                        text = newText.text.take(4),
                                        selection = TextRange(newText.text.length) // Move cursor to end
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(targetDaysFocusRequester)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        isTargetDaysFocused = true
                                        // If we flagged clearOnFocus or stored value is "0", clear the editable text so the first keystroke replaces it
                                        if (clearOnFocus || targetDays == "0" || targetDaysText.text == "0") {
                                            targetDaysText = TextFieldValue(text = "", selection = TextRange(0))
                                            clearOnFocus = false
                                        } else {
                                            // ensure caret at end of existing value
                                            targetDaysText = TextFieldValue(text = targetDays, selection = TextRange(targetDays.length))
                                        }
                                    } else {
                                        isTargetDaysFocused = false
                                        // Commit value on focus loss
                                        val newTargetDays = targetDaysText.text.toIntOrNull()?.coerceIn(0, 9999) ?: 0
                                        targetDays = newTargetDays.toString()
                                        targetDaysText = TextFieldValue(text = targetDays, selection = TextRange(targetDays.length))
                                    }
                                },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    // Commit value on IME Done
                                    val newTargetDays = targetDaysText.text.toIntOrNull()?.coerceIn(0, 9999) ?: 0
                                    targetDays = newTargetDays.toString()
                                    targetDaysText = TextFieldValue(targetDays)
                                    // Hide keyboard
                                    keyboardController?.hide()
                                }
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                errorContainerColor = Color.Transparent
                            ),
                            singleLine = true,
                            maxLines = 1
                        )
                    }

                    HorizontalDivider()

                    if (isRangeInvalid) Text(stringResource(R.string.add_record_error_invalid_range), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)

                    Spacer(Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // When the user has typed any digit in the editable field, enable the save button immediately.
                        val hasAnyDigitInput = targetDaysText.text.isNotBlank() && targetDaysText.text.all { it.isDigit() }

                        Button(
                            onClick = {
                                // Determine commit value: prefer the current editable text if present
                                val commitTarget = targetDaysText.text.toIntOrNull()?.coerceIn(0, 9999) ?: targetDaysInt
                                if (!isRangeInvalid && !isOngoing && commitTarget in 1..9999) {
                                    val id = "rec_${System.currentTimeMillis()}"
                                    val completed = actualDays >= commitTarget
                                    val status = if (completed) "성공" else "실패"
                                    val record = SobrietyRecord(
                                        id = id,
                                        startTime = startMillis,
                                        endTime = endMillis,
                                        targetDays = commitTarget,
                                        actualDays = actualDays,
                                        isCompleted = completed,
                                        status = status,
                                        createdAt = System.currentTimeMillis()
                                    )
                                    val ok = persistRecord(record)
                                    if (ok) onFinished()
                                }
                            },
                            enabled = !isRangeInvalid && !isOngoing && (hasAnyDigitInput || targetDaysInt in 1..9999),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.add_record_save))
                        }
                    }
