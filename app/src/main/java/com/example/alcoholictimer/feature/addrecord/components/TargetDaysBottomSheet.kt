package com.example.alcoholictimer.feature.addrecord.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.alcoholictimer.core.ui.components.NumberPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetDaysBottomSheet(
    initialValue: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var current by remember(initialValue) { mutableIntStateOf(initialValue.coerceIn(0, 999)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("목표 일수 선택", style = MaterialTheme.typography.titleMedium)

            NumberPicker(
                value = current,
                onValueChange = { current = it.coerceIn(0, 999) },
                range = 0..999,
                label = "일"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) { Text("취소") }

                Button(
                    onClick = { onConfirm(current) },
                    modifier = Modifier.weight(1f)
                ) { Text("확인") }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

