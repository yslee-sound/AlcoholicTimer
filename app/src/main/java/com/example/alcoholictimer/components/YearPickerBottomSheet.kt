package com.example.alcoholictimer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearPickerBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onYearPicked: (year: Int) -> Unit,
    initialYear: Int = Calendar.getInstance().get(Calendar.YEAR)
) {
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = bottomSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            Color(0xFF636E72).copy(alpha = 0.2f),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        ) {
            YearPickerContent(
                onYearPicked = onYearPicked,
                onDismiss = onDismiss,
                initialYear = initialYear
            )
        }
    }
}

@Composable
internal fun YearPickerContent(
    onYearPicked: (year: Int) -> Unit,
    onDismiss: () -> Unit,
    initialYear: Int
) {
    var selectedYear by remember { mutableStateOf(initialYear) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 상단: 안내
        Text(
            text = "연도 선택",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "2000~2030년 중에서 선택하세요",
            fontSize = 14.sp,
            color = Color(0xFF636E72),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 가운데: 연도 선택 NumberPicker
        NumberPicker(
            value = selectedYear,
            onValueChange = { selectedYear = it },
            range = 2000..2030,
            displayValues = (2000..2030).map { "${it}년" },
            modifier = Modifier.width(160.dp)
        )

        // 하단: 선택 버튼
        Button(
            onClick = {
                onYearPicked(selectedYear)
                onDismiss()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(top = 12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF74B9FF),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "선택",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun YearPickerBottomSheetPreview() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        YearPickerContent(
            onYearPicked = { },
            onDismiss = { },
            initialYear = 2025
        )
    }
}

@Preview(showBackground = true, name = "YearPicker - Dark Mode")
@Composable
fun YearPickerBottomSheetDarkPreview() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        YearPickerContent(
            onYearPicked = { },
            onDismiss = { },
            initialYear = 2025
        )
    }
}
