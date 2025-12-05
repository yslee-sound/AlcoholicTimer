package kr.sweetapps.alcoholictimer.ui.tab_05.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NotificationViewModel : ViewModel() {
    private val _message = MutableStateFlow("아직 알림이 없어요")
    val message: StateFlow<String> = _message
}