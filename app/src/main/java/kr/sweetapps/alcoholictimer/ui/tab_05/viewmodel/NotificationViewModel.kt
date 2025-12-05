package kr.sweetapps.alcoholictimer.ui.tab_05.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.data.model.NotificationItem
import kr.sweetapps.alcoholictimer.data.repository.NotificationRepository

/**
 * 알림 목록 화면 ViewModel
 */
class NotificationViewModel : ViewModel() {
    private val repository = NotificationRepository()

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    sealed class UiState {
        object Loading : UiState()
        data class Success(val notifications: List<NotificationItem>) : UiState()
        object Empty : UiState()
        data class Error(val message: String) : UiState()
    }

    init {
        fetchNotifications()
    }

    fun fetchNotifications() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            repository.fetchNotifications()
                .onSuccess { notifications ->
                    _uiState.value = if (notifications.isEmpty()) {
                        UiState.Empty
                    } else {
                        UiState.Success(notifications)
                    }
                }
                .onFailure { exception ->
                    _uiState.value = UiState.Error(exception.message ?: "알 수 없는 오류가 발생했습니다.")
                }
        }
    }
}

