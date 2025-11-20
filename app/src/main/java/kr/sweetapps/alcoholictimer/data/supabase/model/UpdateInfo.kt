package kr.sweetapps.alcoholictimer.data.supabase.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 업데이트 인포 모델
 *
 * Supabase 테이블 구조 (update_info) 예시:
 * ```sql
 * CREATE TABLE update_info (
 *     id BIGINT PRIMARY KEY,
 *     version_code INTEGER NOT NULL UNIQUE,
 *     version_name TEXT NOT NULL,
 *     app_id TEXT NOT NULL,
 *     is_force BOOLEAN DEFAULT false,
 *     release_notes TEXT NOT NULL,
 *     released_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
 *     download_url TEXT
 * );
 * ```
 *
 * 용도:
 * - 앱 업데이트 알림
 * - 강제 업데이트 제어
 * - 릴리즈 노트 표시
 */
@Serializable
data class UpdateInfo(
    /** 아이디 (자동 생성 가능) */
    @SerialName("id")
    val id: Long? = null,

    /** 앱 버전 코드 (비교/정렬용 정수) */
    @SerialName("version_code")
    val versionCode: Int = 0,

    /** 앱 버전명 (UI 표시용) */
    @SerialName("version_name")
    val versionName: String = "",

    /** 앱 식별자 (패키지명 등) */
    @SerialName("app_id")
    val appId: String = "",

    /** 강제 업데이트 여부 (true면 반드시 업데이트) */
    @SerialName("is_force")
    val isForce: Boolean = false,

    /** 릴리즈 노트(업데이트 내용) */
    @SerialName("release_notes")
    val releaseNotes: String = "",

    /** 릴리즈 시각 (ISO 8601 문자열, 선택) */
    @SerialName("released_at")
    val releasedAt: String? = null,

    /** 다운로드 URL (선택, Play Store 또는 직접 링크) */
    @SerialName("download_url")
    val downloadUrl: String? = null,

    /** 선택적: 업데이트 행 활성 여부 (컬럼이 없으면 기본 true) */
    @SerialName("is_active")
    val isActive: Boolean = true
)
