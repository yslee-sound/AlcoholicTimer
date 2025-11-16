package kr.sweetapps.alcoholictimer.feature.records.components

import androidx.compose.runtime.Composable
import kr.sweetapps.alcoholictimer.core.model.SobrietyRecord
import kr.sweetapps.alcoholictimer.ui.screens.RecordsScreen as NewRecordsScreen

@Composable
fun RecordsScreen(
    externalRefreshTrigger: Int,
    onNavigateToDetail: (SobrietyRecord) -> Unit = {},
    onNavigateToAllRecords: () -> Unit = {},
    onAddRecord: () -> Unit = {},
    fontScale: Float = 1.06f
) {
    NewRecordsScreen(
        externalRefreshTrigger = externalRefreshTrigger,
        onNavigateToDetail = onNavigateToDetail,
        onNavigateToAllRecords = onNavigateToAllRecords,
        onAddRecord = onAddRecord,
        fontScale = fontScale
    )
}
