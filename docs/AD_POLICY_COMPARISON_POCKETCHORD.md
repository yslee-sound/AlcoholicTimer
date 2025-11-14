# PocketChord 광고 정책 비교

> **⚠️ 중요**: 이 문서는 삭제하지 마세요. PocketChord 앱과의 광고 정책 비교 기준 문서입니다.

## 1. 배너 광고 정책

### PocketChord
- **활성 여부**: ON (검증 필요)
- **표시 조건**: 앱 재실행, 백그라운드→포그라운드 복귀 시 마포시
- **설정 중 차단 3초 내 정책 반영**
- **Logcat**: MainActivity: [정책] 배너 광고 비활성화

### AlcoholicTimer
| 항목 | 설정값 | 비고 |
|------|--------|------|
| 활성 여부 | true | Supabase ad_policy.ad_banner_enabled |
| 정책 미로딩 Fallback | true | 초기 표시 속도 개선 |
| 전면 표시 중 차단 | 자동 | AdController.isInterstitialShowing |
| 재시도 정책 | 3회 (2s/5s/10s) | BannerRetryConfig |
| NO_FILL 연속 제한 | 2회 | maxConsecutiveNoFill |

---

## 2. 전면 광고 정책

### PocketChord
- **활성 여부**: ON
- **표시 조건**: 홈 화면(시작/진행/종료 그룹) 진입 3회마다
- **시간당 제한**: 없음 (회수 기반만)
- **일일 제한**: 없음
- **초기 보호**: 없음 (콜드 스타트 직후에도 즉시 카운트)
- **재쿨다운**: 120초

### AlcoholicTimer
| 항목 | 설정값 | 비고 |
|------|--------|------|
| 활성 여부 | true | Supabase ad_policy.ad_interstitial_enabled |
| 표시 조건 | 홈 그룹 진입 3회 | HomeAdTrigger.onHomeVisit() |
| 시간당 제한 | 99999 (테스트) / 2 (운영 권장) | ad_interstitial_max_per_hour |
| 일일 제한 | 99999 (테스트) / 15 (운영 권장) | ad_interstitial_max_per_day |
| **초기 보호** | **60초** | 앱 시작 후 보호 창 |
| **재쿨다운** | **60초** | 표시 후 대기 시간 |
| 콜드 스타트 1회 제한 | ❌ **비활성** | 2025-11-14 제거 |

**변경 이력**:
- 2025-11-14: 재쿨다운 120초 → 60초 변경
- 2025-11-14: 초기 보호 45초 → 60초 통일
- 2025-11-14: 콜드 스타트 1회 제한 정책 삭제

**용어 정의**:
- **초기 보호 (Initial Protection)**: 앱 시작(콜드 스타트) 후 N초 동안 전면 광고를 차단하는 정책. 사용자가 앱을 처음 열었을 때 즉시 광고가 나오는 것을 방지하여 UX 개선.
- **재쿨다운 (Re-cooldown)**: 전면 광고 표시 직후 N초 동안 다�� 전면 광고를 차단하는 정책. 짧은 시간 내 반복 노출을 방지.

---

## 3. 앱 오프닝 광고 정책

### PocketChord
- **활성 여부**: OFF
  - SQL: `UPDATE ad_policy SET ad_app_open_enabled=false WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');`
- **검증 결과**: 
  - 앱 완전 종료 → 재실행: ❌ 미표시
  - 백그라운드 → 포그라운드: ❌ 미표시
  - Logcat: `AppOpenAdManager: [정책] 앱 오픈 광고 비활성화`

### AlcoholicTimer
| 항목 | 설정값 | 비고 |
|------|--------|------|
| 활성 여부 | true | Supabase ad_policy.ad_app_open_enabled |
| 정책 미로딩 Fallback | true | 초기 시도 허용 |
| **초기 보호** | **60초** | 앱 시작 후 보호 창 |
| 표시 후 재쿨다운 | 60초 | 연속 표시 방지 |
| 트리거 | ProcessLifecycle.onStart | 포그라운드 복귀 시 |
| 단위 ID | 테스트 ID | ca-app-pub-3940256099942544/3419835294 |

**알려진 이슈**:
- ⚠️ **HTTP 403 에러 발생 중**: UMP Consent Form 미설정으로 인한 서버 거부
- **해결 필요**: AdMob 콘솔에서 Consent Form 생성 후 Publish

**변경 이력**:
- 2025-11-14: 초기 보호 60초 추가 (전면 광고와 통일)
- 2025-11-14: 재쿨다운 45초 → 60초 통일

---

## 4. 비교 요약

### 공통점
- 배너/전면 모두 활성화
- Supabase 원격 제어 기반
- 홈 그룹 진입 시 전면 카운트

### 차이점
| 구분 | PocketChord | AlcoholicTimer |
|------|-------------|----------------|
| 앱 오프닝 | ❌ OFF | ✅ ON (403 해결 필요) |
| 전면 시간당 제한 | ❌ 없음 | ✅ 99999 (원격 조정 가능) |
| 전면 일일 제한 | ❌ 없음 | ✅ 99999 (원격 조정 가능) |
| 전면 초기 보호 | ❌ 없음 | ✅ 60초 |
| 전면 재쿨다운 | 120초 | 60초 |
| 배너 재시도 | (미확인) | 3회 (2s/5s/10s) |

---

## 5. 운영 권장 설정

### Supabase ad_policy 테이블 (AlcoholicTimer 운영 환경)

```sql
-- 운영 환경 권장값
UPDATE ad_policy SET
  ad_interstitial_max_per_hour = 2,
  ad_interstitial_max_per_day = 15,
  ad_app_open_enabled = true
WHERE app_id = 'kr.sweetapps.alcoholictimer';
```

### 테스트 환경 (현재)
```sql
-- 디버그 환경 (무제한)
UPDATE ad_policy SET
  ad_interstitial_max_per_hour = 99999,
  ad_interstitial_max_per_day = 99999
WHERE app_id = 'kr.sweetapps.alcoholictimer.debug';
```

---

## 6. 체크리스트

### 배포 전 필수 확인
- [ ] AdMob Consent Form 생성 및 Publish
- [ ] 운영 App Open 단위 ID 교체
- [ ] 운영 전면/배너 단위 ID 교체
- [ ] Supabase 운영 정책 값 설정 (시간당 2회, 일일 15회)
- [ ] 테스트 디바이스 제거 또는 운영 빌드 분리

### 정책 검증 방법
1. **배너**: 앱 시작 → 상단 60dp 영역 광고 표시 확인
2. **전면**: 홈 그룹 3회 전환 → 전면 광고 표시 확인
3. **앱 오프닝**: 백그라운드 60초 후 복귀 → 앱 오프닝 광고 표시 확인

---

**문서 최종 업데이트**: 2025-11-14  
**관리자**: AlcoholicTimer Dev Team  
**참고**: 이 문서는 광고 정책 변경 시 즉시 업데이트해야 합니다.

