package com.example.alcoholictimer

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alcoholictimer.models.LevelHistoryItem
import com.example.alcoholictimer.models.RecentActivity
import com.example.alcoholictimer.utils.RecentActivityManager

class RecordsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseScreen {
                RecordsScreen()
            }
        }
    }

    override fun getScreenTitle(): String = "금주 기록"

    @Composable
    private fun RecordsScreen() {
        val levelHistoryItems = listOf<LevelHistoryItem>() // Replace with actual data loading logic
        val recentActivities = RecentActivityManager.getRecentActivities()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "금주 기록",
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(levelHistoryItems) { item ->
                    Text(text = item.description, fontSize = 16.sp)
                }
                items(recentActivities) { activity ->
                    Text(text = activity.title, fontSize = 16.sp)
                }
            }
        }
    }
}
