// [MODIFIED] 일기 작성 화면 - WritePostScreenContent 재사용 (2025-12-22)
package kr.sweetapps.alcoholictimer.ui.tab_02.screens

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.data.model.Post
import kr.sweetapps.alcoholictimer.data.room.DiaryEntity
import kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.DiaryViewModel
import kr.sweetapps.alcoholictimer.ui.tab_03.WritePostScreenContent
import java.text.SimpleDateFormat
import java.util.*

/**
 * [MODIFIED] 일기 작성/수정 화면
 * - WritePostScreenContent를 재사용하여 커뮤니티 글쓰기와 동일한 UI 제공
 * - isDiaryMode = true로 설정하여 "챌린지 공유" 기능 활성화
 * - 새 일기 작성: diaryId = null
 * - 기존 일기 수정: diaryId != null
 */
@Composable
fun DiaryWriteScreen(
    diaryId: Long? = null,
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 1. CommunityViewModel 인스턴스 획득 (이미지, 갈증 수치 등 로직 재사용)
    val communityViewModel: kr.sweetapps.alcoholictimer.ui.tab_03.viewmodel.CommunityViewModel = viewModel()
    val currentNickname by communityViewModel.currentNickname.collectAsState()

    // 2. DiaryViewModel 인스턴스 획득 (로컬 DB 저장용)
    val diaryViewModel: DiaryViewModel = viewModel()

    // 3. 기존 일기 데이터 로드 (수정 모드)
    var existingDiary by remember { mutableStateOf<DiaryEntity?>(null) }
    var postToEdit by remember { mutableStateOf<Post?>(null) }

    LaunchedEffect(diaryId) {
        if (diaryId != null) {
            scope.launch {
                // Room DB에서 기존 일기 불러오기
                val diary = diaryViewModel.getDiaryById(diaryId)
                existingDiary = diary

                // Post 객체로 변환하여 WritePostScreenContent에 전달
                if (diary != null) {
                    postToEdit = Post(
                        id = diary.id.toString(),
                        content = diary.content,
                        tagType = "diary", // 일기 태그
                        thirstLevel = diary.cravingLevel, // [FIX] cravingLevel -> thirstLevel 매핑
                        imageUrl = "", // 일기에 이미지가 없음 (필요시 추가)
                        nickname = "",
                        timerDuration = "",
                        likeCount = 0,
                        likedBy = emptyList(),
                        currentDays = 0,
                        userLevel = 0,
                        createdAt = com.google.firebase.Timestamp.now(),
                        deleteAt = com.google.firebase.Timestamp.now(),
                        authorAvatarIndex = 0,
                        authorId = "",
                        languageCode = ""
                    )
                }
            }
        }
    }

    // 4. WritePostScreenContent 호출
    WritePostScreenContent(
        viewModel = communityViewModel,
        currentNickname = currentNickname,
        isDiaryMode = true, // [중요] 일기 모드 활성화
        postToEdit = postToEdit, // 수정 모드일 경우 기존 데이터 전달
        onPost = {
            // 저장/게시 완료 후 화면 닫기
            onDismiss()
        },
        onSaveDiary = { postData ->
            // [핵심] 로컬 일기장(Room DB) 저장 로직
            scope.launch {
                try {
                    val timestamp = System.currentTimeMillis()
                    val dateString = formatDate(timestamp)

                    if (diaryId != null) {
                        // 수정 모드: 기존 일기 업데이트
                        val updatedDiary = existingDiary?.copy(
                            content = postData.content,
                            cravingLevel = postData.thirstLevel ?: 0, // [FIX] thirstLevel -> cravingLevel 매핑
                            timestamp = timestamp,
                            date = dateString
                        )
                        if (updatedDiary != null) {
                            diaryViewModel.updateDiary(updatedDiary)
                            android.util.Log.d("DiaryWriteScreen", "일기 수정 성공: ${postData.content}")
                        }
                    } else {
                        // 신규 작성: 새 일기 생성
                        val newDiary = DiaryEntity(
                            emoji = "📝", // 기본 이모지 (추후 선택 기능 추가 가능)
                            content = postData.content,
                            cravingLevel = postData.thirstLevel ?: 0, // [FIX] thirstLevel -> cravingLevel 매핑
                            timestamp = timestamp,
                            date = dateString
                        )
                        diaryViewModel.insertDiary(newDiary)
                        android.util.Log.d("DiaryWriteScreen", "일기 생성 성공: ${postData.content}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DiaryWriteScreen", "일기 저장 실패", e)
                }
            }
        },
        onDismiss = {
            // 취소 시 화면 닫기
            onDismiss()
        },
        onOpenPhoto = {
            // [TODO] 사진 선택 권한 요청 및 갤러리 열기 로직
            // 현재는 커뮤니티 탭과 동일한 권한 요청 로직이 WritePostScreenContent 내부에 구현되어 있음
            // 필요 시 추가 커스터마이징 가능
        }
    )
}

/**
 * [HELPER] timestamp를 날짜 문자열로 변환
 */
private fun formatDate(timestamp: Long): String {
    val locale = Locale.getDefault()
    val sdf = when (locale.language) {
        "ko" -> SimpleDateFormat("yyyy년 M월 d일", locale)
        "ja" -> SimpleDateFormat("yyyy年M月d日", locale)
        "zh" -> SimpleDateFormat("yyyy年M月d日", locale)
        "es" -> SimpleDateFormat("d 'de' MMMM 'de' yyyy", locale)
        else -> SimpleDateFormat("MMM d, yyyy", locale)
    }
    return sdf.format(Date(timestamp))
}

