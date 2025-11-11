package kr.sweetapps.alcoholictimer.feature.debug

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.data.supabase.SupabaseProvider
import kr.sweetapps.alcoholictimer.data.supabase.model.EmergencyPolicy
import kr.sweetapps.alcoholictimer.data.supabase.model.NoticePolicy
import kr.sweetapps.alcoholictimer.data.supabase.model.PopupDecision
import kr.sweetapps.alcoholictimer.data.supabase.model.UpdatePolicy
import kr.sweetapps.alcoholictimer.data.supabase.repository.EmergencyPolicyRepository
import kr.sweetapps.alcoholictimer.data.supabase.repository.NoticePolicyRepository
import kr.sweetapps.alcoholictimer.data.supabase.repository.PopupPolicyManager
import kr.sweetapps.alcoholictimer.data.supabase.repository.UpdatePolicyRepository

/**
 * 디버그 화면 ViewModel
 *
 * Supabase 정책 데이터를 가져와서 팝업을 표시합니다.
 */
class DebugViewModel(application: Application) : AndroidViewModel(application) {

    private val supabaseClient = SupabaseProvider.getClient(application)

    private val emergencyRepo = EmergencyPolicyRepository(supabaseClient, application)
    private val updateRepo = UpdatePolicyRepository(supabaseClient, application)
    private val noticeRepo = NoticePolicyRepository(supabaseClient, application)

    private val policyManager = PopupPolicyManager(
        emergencyRepo = emergencyRepo,
        updateRepo = updateRepo,
        noticeRepo = noticeRepo,
        context = application
    )

    // UI 상태
    private val _emergencyPolicy = MutableStateFlow<EmergencyPolicy?>(null)
    val emergencyPolicy: StateFlow<EmergencyPolicy?> = _emergencyPolicy.asStateFlow()

    private val _noticePolicy = MutableStateFlow<NoticePolicy?>(null)
    val noticePolicy: StateFlow<NoticePolicy?> = _noticePolicy.asStateFlow()

    private val _updatePolicy = MutableStateFlow<UpdatePolicy?>(null)
    val updatePolicy: StateFlow<UpdatePolicy?> = _updatePolicy.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Supabase에서 정책 데이터를 가져옵니다.
     */
    fun loadPolicies() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // 긴급 공지 정책
                _emergencyPolicy.value = emergencyRepo.getActivePolicy()

                // 일반 공지 정책
                val currentVersion = getCurrentVersion()
                _noticePolicy.value = noticeRepo.getActivePolicy(currentVersion)

                // 업데이트 정책
                _updatePolicy.value = updateRepo.getActivePolicy()

            } catch (e: Exception) {
                _errorMessage.value = "정책 로딩 실패: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 팝업을 결정합니다.
     */
    fun decidePopup(onDecision: (PopupDecision) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val currentVersion = getCurrentVersion()
                val decision = policyManager.decidePopup(currentVersion)
                onDecision(decision)

                // 결정에 따라 기록
                when (decision) {
                    is PopupDecision.ShowEmergency -> {
                        policyManager.markEmergencyShown(decision.policy.id)
                    }
                    is PopupDecision.ShowNotice -> {
                        policyManager.markNoticeShown(decision.policy.id)
                    }
                    else -> { /* No action needed */ }
                }
            } catch (e: Exception) {
                _errorMessage.value = "팝업 결정 실패: ${e.message}"
                e.printStackTrace()
                onDecision(PopupDecision.None)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 업데이트를 나중에 하기로 선택합니다.
     */
    fun dismissUpdate(version: String) {
        policyManager.dismissUpdate(version)
    }

    /**
     * 모든 정책 표시 기록을 초기화합니다.
     */
    fun clearAllRecords() {
        policyManager.clearAllRecords()
        loadPolicies() // 다시 로드
    }

    /**
     * 현재 앱 버전을 가져옵니다.
     */
    private fun getCurrentVersion(): String {
        return try {
            val packageInfo = getApplication<Application>().packageManager
                .getPackageInfo(getApplication<Application>().packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
}

