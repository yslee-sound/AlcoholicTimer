package com.example.alcoholictimer.adapters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alcoholictimer.models.RecentActivity
import com.example.alcoholictimer.utils.Constants

/**
 * 최근 활동 목록을 표시하는 RecyclerView 어댑터
 */
class RecentActivityAdapter(
    private var activities: List<RecentActivity>,
    private val onItemClick: (RecentActivity) -> Unit = {}
) {
    /**
     * 데이터 업데이트 메서드
     */
    fun updateData(newActivities: List<RecentActivity>) {
        activities = newActivities
    }

    @Composable
    fun RecentActivityList() {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            activities.forEach { activity ->
                RecentActivityItemView(activity)
            }
        }
    }

    @Composable
    private fun RecentActivityItemView(activity: RecentActivity) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { onItemClick(activity) },
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = activity.title, fontSize = 16.sp)
            Text(text = activity.endDate, fontSize = 16.sp)
        }
    }
}
