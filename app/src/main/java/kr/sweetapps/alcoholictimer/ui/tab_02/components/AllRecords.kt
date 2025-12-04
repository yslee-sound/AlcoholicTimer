// [NEW] Tab02 리팩토링: components를 tab_02로 이동
package kr.sweetapps.alcoholictimer.ui.tab_02.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.constants.UiConstants
import kr.sweetapps.alcoholictimer.core.data.RecordsDataLoader
import kr.sweetapps.alcoholictimer.data.model.SobrietyRecord
import kr.sweetapps.alcoholictimer.ui.components.BackTopBar
import kr.sweetapps.alcoholictimer.ui.common.LocalSafeContentPadding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllRecordsScreen(
    externalRefreshTrigger: Int = 0,
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (SobrietyRecord) -> Unit = {},
    onAddRecord: () -> Unit = {}, // [NEW] 기록 추가 콜백 추가
    fontScale: Float = 1.0f,
    externalDeleteDialog: MutableState<Boolean>? = null
) {
    val context = LocalContext.current
    var records by remember { mutableStateOf<List<SobrietyRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableIntStateOf(0) }
    // 외부에서 제어 가능한 삭제 다이얼로그 상태(없으면 내부에서 생성)
    val ownDialog = remember { mutableStateOf(false) }
    val dialogState = externalDeleteDialog ?: ownDialog

    val loadRecords: () -> Unit = remember {
        {
            isLoading = true
            loadError = null
            try {
                val loadedRecords = RecordsDataLoader.loadSobrietyRecords(context)
                records = loadedRecords.sortedByDescending { it.startTime }
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                loadError = e.message ?: "unknown"
            }
        }
    }

    LaunchedEffect(retryTrigger) { loadRecords() }
    LaunchedEffect(externalRefreshTrigger) { loadRecords() }

    // [수정] Box로 감싸서 FAB을 오버레이로 배치
    Box(modifier = Modifier.fillMaxSize()) {
        // Keep BackTopBar outside fontScale override so its title uses system font scale
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
        // [수정] 상단 공통 BackTopBar: 점 3개 메뉴로 변경
        BackTopBar(
            title = stringResource(id = R.string.all_records_title),
            onBack = onNavigateBack,
            trailingContent = if (externalDeleteDialog == null) {
                {
                    // [NEW] 메뉴 확장 상태 관리
                    var showMenu by remember { mutableStateOf(false) }

                    Box {
                        // [NEW] 점 3개 아이콘 버튼
                        IconButton(
                            onClick = { showMenu = true },
                            enabled = !isLoading && records.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(id = R.string.cd_more_options),
                                tint = if (!isLoading && records.isNotEmpty()) Color.Black else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }

                        // [NEW] 드롭다운 메뉴
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.menu_delete_all_records)) },
                                onClick = {
                                    showMenu = false
                                    dialogState.value = true
                                }
                            )
                        }
                    }
                }
            } else null
        )

        // Apply fontScale only to the body content below the TopBar
        CompositionLocalProvider(
            LocalDensity provides Density(
                density = LocalDensity.current.density,
                fontScale = LocalDensity.current.fontScale * fontScale
            )
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
                }
                loadError != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
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
                        Button(onClick = { retryTrigger++ }) { Text(text = stringResource(id = R.string.retry)) }
                    }
                }
                records.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AllRecordsEmptyState() }
                }
                else -> {
                    val safePadding = LocalSafeContentPadding.current
                    val bottomPadding = safePadding.calculateBottomPadding() + 12.dp

                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = kr.sweetapps.alcoholictimer.ui.tab_02.screens.RECORDS_SCREEN_HORIZONTAL_PADDING),
                        verticalArrangement = Arrangement.spacedBy(UiConstants.CARD_VERTICAL_SPACING),
                        contentPadding = PaddingValues(top = 16.dp, bottom = bottomPadding)
                    ) {
                        items(items = records, key = { it.id }) { record ->
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 0.dp)
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

        if (dialogState.value) {
            AlertDialog(
                onDismissRequest = { dialogState.value = false },
                title = {
                    Text(
                        text = stringResource(id = R.string.all_records_delete_title),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D3748)
                    )
                },
                text = { Text(text = stringResource(id = R.string.all_records_delete_message), color = Color(0xFF4A5568)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            dialogState.value = false
                            val success = RecordsDataLoader.clearAllRecords(context)

                            if (success) {
                                // 삭제 성공: 리스트 새로고침
                                retryTrigger++

                                // 잠시 후 빈 화면 확인 후 뒤로가기 (선택사항)
                                // onNavigateBack()을 즉시 호출하지 않고 사용자가 확인할 수 있게 함
                            } else {
                                // 삭제 실패: 에러 표시 (향후 개선 가능)
                                loadError = context.getString(R.string.all_records_delete_failed)
                            }
                        }
                    ) { Text(stringResource(id = R.string.dialog_delete_confirm)) }
                },
                dismissButton = { TextButton(onClick = { dialogState.value = false }) { Text(stringResource(id = R.string.dialog_cancel)) } }
            )
        }
    } // Column 닫기

    // [NEW] Floating Action Button - 기록 추가
    val safePadding = LocalSafeContentPadding.current
    FloatingActionButton(
        onClick = { onAddRecord() },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(
                end = 16.dp,
                bottom = safePadding.calculateBottomPadding() + 16.dp
            ),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = stringResource(id = R.string.cd_add_record)
        )
    }
} // Box 닫기
}

@Composable
fun AllRecordsEmptyState() {
    val emptyCd = stringResource(id = R.string.empty_records_cd)
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp).semantics { contentDescription = emptyCd },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "📝", fontSize = 48.sp, textAlign = TextAlign.Center)
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

@Preview(showBackground = true)
@Composable
fun AllRecordsScreenPreview() { AllRecordsScreen() }
