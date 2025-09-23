package com.example.alcoholictimer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alcoholictimer.utils.RecordsDataLoader
import com.example.alcoholictimer.utils.SobrietyRecord
import androidx.compose.ui.geometry.Offset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllRecordsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (SobrietyRecord) -> Unit = {}
) {
    val context = LocalContext.current
    var records by remember { mutableStateOf<List<SobrietyRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 데이터 로딩
    LaunchedEffect(Unit) {
        try {
            val loadedRecords = RecordsDataLoader.loadSobrietyRecords(context)
            records = loadedRecords.sortedByDescending { it.createdAt }
            isLoading = false
        } catch (_: Exception) {
            isLoading = false
        }
    }

    // 다른 화면들과 일관된 연한 회색 계열 그라데이션 (BaseActivity와 동일 톤)
    val gradientBackground = Brush.linearGradient(
        colors = listOf(
            Color(0xFFF8F9FA),
            Color(0xFFE9ECEF)
        ),
        start = Offset(0f, 0f),
        end = Offset.Infinite
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = gradientBackground
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 상단 바 (뒤로가기 버튼) - 공통 톤으로 통일
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp,
                color = Color.White
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = "모든 기록",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2C3E50)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "뒤로가기",
                                tint = Color(0xFF2C3E50)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color(0xFF2C3E50),
                        navigationIconContentColor = Color(0xFF2C3E50),
                        actionIconContentColor = Color(0xFF2C3E50)
                    )
                )
            }

            // 콘텐츠 영역
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    if (records.isEmpty()) {
                        item {
                            EmptyRecordsState()
                        }
                    } else {
                        items(records) { record ->
                            SobrietyRecordCard(
                                record = record,
                                onClick = { onNavigateToDetail(record) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyRecordsState() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📝",
                fontSize = 48.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "기록이 없습니다",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "첫 번째 금주 기록을 시작해보세요!",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AllRecordsScreenPreview() {
    AllRecordsScreen()
}
