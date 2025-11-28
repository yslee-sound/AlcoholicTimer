package kr.sweetapps.alcoholictimer.ui.tab_05.screens.debug

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kr.sweetapps.alcoholictimer.core.util.DebugSettings

data class DebugScreenUiState(
    val switch1: Boolean = false,
    val demoMode: Boolean = false,
    val switch3: Boolean = false,
    val switch4: Boolean = false,
    val switch5: Boolean = false,
)

class DebugScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DebugScreenUiState())
    val uiState = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(demoMode = DebugSettings.isDemoModeEnabled(application))
        }
    }

    fun setSwitch(switchIndex: Int, value: Boolean) {
        _uiState.update { currentState ->
            when (switchIndex) {
                1 -> currentState.copy(switch1 = value)
                2 -> {
                    DebugSettings.setDemoModeEnabled(getApplication(), value)
                    currentState.copy(demoMode = value)
                }
                3 -> currentState.copy(switch3 = value)
                4 -> currentState.copy(switch4 = value)
                5 -> currentState.copy(switch5 = value)
                else -> currentState
            }
        }
    }
}
