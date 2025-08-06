package com.example.alcoholictimer.adapters

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alcoholictimer.models.LevelHistoryItem

@Composable
fun LevelHistoryList(items: List<LevelHistoryItem>) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items.forEach { item ->
            LevelHistoryItemView(item)
        }
    }
}

@Composable
fun LevelHistoryItemView(item: LevelHistoryItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = item.date, fontSize = 16.sp)
        Text(text = item.description, fontSize = 16.sp)
    }
}
