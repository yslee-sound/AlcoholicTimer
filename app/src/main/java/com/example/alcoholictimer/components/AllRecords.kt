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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.alcoholictimer.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllRecordsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (SobrietyRecord) -> Unit = {}
) {
    val context = LocalContext.current
    var records by remember { mutableStateOf<List<SobrietyRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableStateOf(0) }

    // 데이터 로딩 (재시도 트리거에 반응)
    LaunchedEffect(retryTrigger) {
        isLoading = true
        loadError = null
        try {
            val loadedRecords = RecordsDataLoader.loadSobrietyRecords(context)
            records = loadedRecords.sortedByDescending { it.createdAt }
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
            loadError = e.message ?: "unknown"
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
            .background(brush = gradientBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 상단 바 (뒤로가기 버튼) - 공통 톤으로 통일 + 문자열 리소스/토큰 적용
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.all_records_title),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.cd_navigate_back),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            // 콘텐츠 영역
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                loadError != null -> {
                    // 에러 상태 UI + 재시도 버튼
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.error_loading_records),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { retryTrigger++ }) {
                            Text(text = stringResource(id = R.string.retry))
                        }
                    }
                }

                records.isEmpty() -> {
                    // 빈 상태
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyRecordsState()
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
                    ) {
                        // key 추가로 안정성/퍼포먼스 향상
                        items(
                            items = records,
                            key = { it.id }
                        ) { record ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                RecordSummaryCard(
                                    record = record,
                                    onClick = { onNavigateToDetail(record) },
                                    compact = false,
                                    headerIconSizeDp = 56.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyRecordsState() {
    // semantics 블록 밖에서 리소스 문자열을 먼저 가져옵니다.
    val emptyCd = stringResource(id = R.string.empty_records_cd)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
            .semantics { contentDescription = emptyCd },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 이모지는 장식용이라 별도 contentDescription을 부여하지 않음
            Text(
                text = "📝",
                fontSize = 48.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.empty_records_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.empty_records_subtitle),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
