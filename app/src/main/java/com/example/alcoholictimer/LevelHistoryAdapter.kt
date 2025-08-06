package com.example.alcoholictimer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LevelHistoryList(historyList: List<LevelHistoryItem>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(historyList) { item ->
            LevelHistoryItemView(item)
        }
    }
}

@Composable
fun LevelHistoryItemView(item: LevelHistoryItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = item.date, fontSize = 16.sp)
        Text(text = item.achievement, fontSize = 16.sp)
    }
}

data class LevelHistoryItem(
    val date: String,
    val achievement: String
)
