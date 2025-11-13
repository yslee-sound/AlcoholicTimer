# PocketChord vs AlcoholicTimer 광고 정책 비교

> 문서 목적: PocketChord와 AlcoholicTimer의 광고 정책(배너 / 전면 / 앱 오픈) 구조와 운영/검증 절차를 한 곳에 정리해 회귀 및 정책 변경 시 참조하도록 합니다.
> 
> 🔒 삭제 금지 (DO NOT DELETE). 변경 시 버전만 올리고 히스토리를 유지하세요.

## 📌 목차
1. 개요
2. 정책 필드 맵핑 (Supabase)
3. 배너 광고 정책 비교
4. 전면(Interstitial) 광고 정책 비교
5. 앱 오픈(App Open) 광고 정책 비교
6. 운영 제어 시나리오 (OFF → 검증 → 복구)
7. 공통 로그·검증 체크리스트
8. 향후 확장/개선 제안
9. 변경 이력

---
## 1. 개요
PocketChord와 AlcoholicTimer는 모두 사용자 경험 보호와 정책 준수를 최우선으로 하면서 수익화를 목표로 합니다. AlcoholicTimer는 Supabase 기반 원격 정책(AdPolicy)을 사용해 실시간 ON/OFF 및 빈도 제한을 조정하며, App Open / Interstitial / Banner 간 상호 작용(전면 표시 중 배너 숨김)을 명확히 관리합니다.

---
## 2. 정책 필드 맵핑 (Supabase: ad_policy 테이블)
| 필드 | 의미 | AlcoholicTimer 사용 | PocketChord 대응 (추정/요약) |
|------|------|--------------------|------------------------------|
| is_active | 전체 광고 정책 활성 | true일 때 세부 광고 판단 적용 | 활성 여부 (표준) |
| ad_banner_enabled | 배너 ON/OFF | 배너 표시 여부 결정 (전면 중엔 강제 숨김) | 유사 (단순 ON/OFF) |
| ad_interstitial_enabled | 전면 ON/OFF | 트리거/빈도 로직 진입 허용 | 유사 (패턴 기반) |
| ad_app_open_enabled | 앱 오픈 ON/OFF | 포어그라운드 복귀 시 AppOpen 시도 | (미사용 또는 별도 구현) |
| ad_interstitial_max_per_hour | 시간당 전면 최대 | 기본 Fallback 2 | ≤2 (문서 요약 값) |
| ad_interstitial_max_per_day | 일일 전면 최대 | 기본 Fallback 10 | ≤15 (문서 요약 값) |

> AlcoholicTimer는 추가 내부 제한(재쿨다운 60초, 초기 보호 창 60초, 일일 내부 캡 3회 for InterstitialAdManager)을 통해 Supabase 정책을 보수적으로 감싸는 구조입니다.

---
## 3. 배너 광고 정책 비교
| 항목 | PocketChord | AlcoholicTimer | 비고 |
|------|-------------|---------------|------|
| 기본 ON/OFF | 단순 플래그 | `ad_banner_enabled` + 전면 표시 중 강제 숨김 | UX 시프트 최소화 |
| 빈도 제한 | 없음 (표준 AdMob 로직) | 없음 (표준 AdMob) | 수익·UX 균형 OK |
| 전면/앱오픈 표시 중 처리 | (명시 적음) | 전면/앱오픈 표시 시 배너 컨테이너 유지 + 렌더 skip | 레이아웃 시프트 방지 |
| 초기 지연(Cold Start) | 없음 | 없음 (컨테이너 상시) | 빠른 첫 노출 허용 |
| 원격 정책 변경 전파 | 즉시 or 차후 | Supabase 로드 후 Compose State 반영 | 최대 3분 이내(재로드 필요 시) |
| 테스트 디바이스 | 수동 설정 | MobileAds RequestConfiguration + ID 목록 | Debug 빌드 자동 포함 |

---
## 4. 전면(Interstitial) 광고 정책 비교
| 항목 | PocketChord | AlcoholicTimer | 비고 |
|------|-------------|---------------|------|
| 트리거 | 화면 전환 패턴 3회 | 홈 그룹(START/RUN/QUIT) "비홈 → 홈" 진입 3회 | 행동 기반 명확성 |
| 초기 보호 | (미명시) | 앱 실행 후 60초 보호(표시 금지) | Cold start UX 보호 |
| 표시 후 재쿨다운 | ~60초 | 60초 | 빈도 억제 일치 |
| 시간당 최대 | ≤2 | Supabase (기본 2) | 원격 조정 가능 |
| 일일 최대 | ≤15 | Supabase (기본 10) + 내부 캡 3(릴리즈) | 다중 레이어 안전장치 |
| 세션 1회 제한 | 사용 안 함 | 비활성(OFF) | 초기 보호로 대체 |
| 실패 시 처리 | 재시도 후 패턴 유지 | 카운트 유지 → 다음 홈 진입 시 재시도 | 사용자 경험 보호 |
| 배너 중복 | (명시 약함) | 표시 중 배너 숨김(컨테이너는 유지) | 레이아웃 안정성 |
| 정책 소스 | 내부 or 원격 일부 | Supabase + 내부 Fallback | 복원력 향상 |

---
## 5. 앱 오픈(App Open) 광고 정책 비교
| 항목 | PocketChord | AlcoholicTimer | 비고 |
|------|-------------|---------------|------|
| 지원 여부 | (문서화 불명 / 제한적) | 구현 (AppOpenAdManager) | 포어그라운드 복귀 수익화 |
| 트리거 | - | ProcessLifecycle onStart + 정책 플래그 | 백그라운드→포그라운드 시 |
| 빈도 제한 | - | 표시 후 60초 쿨다운 | 과도 진입 억제 |
| 정책 플래그 | - | `ad_app_open_enabled` | Supabase 제어 |
| 전면 상태 연동 | - | InterstitialShowing 플래그 재사용 → 배너 숨김 | 일관 UI 관리 |
| 장치 회전/다중 Activity | - | 최신 Activity WeakRef 추적 | 안전한 표시 컨텍스트 |
| 테스트 ID | - | Google 테스트 단위 ID (교체 예정) | 운영 전 교체 필요 |

---
## 6. 운영 제어 시나리오 (OFF → 검증 → 복구)
### 6.1 App Open 광고 제어
1) OFF
```sql
UPDATE ad_policy SET ad_app_open_enabled=false
WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
```
2) 검증
- 앱 완전 종료 → 재실행: App Open 미표시
- 백그라운드 → 포어그라운드 복귀: 미표시
- Interstitial/Banner 정상 표시
- Logcat 필터: `tag:AppOpenAdManager` `tag:AdPolicyRepo`
  - 예: `AppOpenAdManager: [정책] 앱 오픈 광고 비활성화`
3) 복구
```sql
UPDATE ad_policy SET ad_app_open_enabled=true
WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
```

### 6.2 Banner 광고 제어
1) OFF
```sql
UPDATE ad_policy SET ad_banner_enabled=false
WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
```
2) 검증
- 앱 재실행: 상단 배너 영역(컨테이너)은 존재하나 실제 배너 미로드
- Interstitial/App Open 정상 표시
- (참고) 실행 중 정책 변경은 최대 수 분 내 반영
- Logcat 예: `MainActivity: [정책] 배너 광고 비활성화`
3) 복구
```sql
UPDATE ad_policy SET ad_banner_enabled=true
WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
```

### 6.3 Interstitial(전면) 광고 제어
1) OFF
```sql
UPDATE ad_policy SET ad_interstitial_enabled=false
WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
```
2) 검증
- 홈 그룹 비홈→홈 전환 3회 도달 시도: 로그는 카운트 증가하나 표시 차단
- App Open/Banner 정상
- Logcat: `AdController: ❌ Interstitial disabled by policy`
3) 복구
```sql
UPDATE ad_policy SET ad_interstitial_enabled=true
WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
```

> NOTE: 값 복구 후 초기 보호(60초) 및 홈 그룹 카운트 임계치(3회), 로드/쿨다운을 모두 충족해야 실제 표시됨.

---
## 7. 공통 로그·검증 체크리스트
| 항목 | 기대 로그 예 | 조치 실패 시 점검 |
|------|-------------|--------------------|
| 정책 로드 | `AdController: 📋 AdPolicy loaded:` | Supabase 연결/네트워크/앱 id 확인 |
| 배너 숨김 (전면 중) | `Banner disabled: interstitial is showing` | InterstitialShowing 플래그 누락 |
| 전면 제한 (시간당) | `❌ Interstitial limit reached: X/Y per hour` | timestamps 파싱/시간대 정상 여부 |
| 전면 쿨다운 | `Blocked by policy: cooldown` | lastShownMs 기록 실패 |
| 초기 보호 | `Blocked by policy: initial_protection` | noteAppStart 호출 여부(Application.onCreate) |
| App Open 비활성 | `AppOpen disabled by policy` | Supabase 값 / 캐시 갱신 지연 |
| App Open 쿨다운 | `AppOpen cooldown` | lastShownAt 기록 및 밀리초 계산 |

Smoke Test 순서:
1. Cold start 30초 이내 전면/AppOpen 모두 차단 (정책 ON이어도 초기 보호 적용)
2. 60초 경과 후 홈 그룹 전환 3회로 전면 표시
3. 백그라운드→포그라운드 60초 후 App Open 표시
4. 전면 표시 중 배너 미표시(컨테이너 유지) 확인
5. 전면 닫힌 뒤 재쿨다운 60초 이전 반복 진입 → 차단 로그

---
## 8. 향후 확장/개선 제안
| 개선영역 | 제안 | 기대효과 |
|----------|------|---------|
| 초기 보호 시간 원격화 | Supabase 필드 `ad_interstitial_initial_protection_sec` 추가 | 릴리즈 후 빠른 튜닝 |
| App Open 쿨다운 원격화 | `ad_app_open_cooldown_sec` | 국가/캠페인별 최적화 |
| Banner 새로고 간격 정책 | 배너 재로드 주기 원격 제어 | 트래픽/CPM 최적화 |
| Interstitial 내부 캡 원격화 | 내부 캡(일일 3회) 필드화 | 배포 후 빈도 미세조정 |
| 정책 변경 이벤트 Push | Supabase Realtime 채널 구독 | 즉시 반영, 지연 감소 |
| 로그 레벨 필터링 | Debug/Release 별 로그 서브셋 | 사용자 로그 공해 감소 |
| 실패 원인 집계 | LoadError/ShowError 카운트 수집 | 품질 모니터링 및 재시도 전략 개선 |

---
## 9. 변경 이력
| 버전 | 날짜 | 변경 내용 |
|------|------|-----------|
| 1.0 | 2025-11-14 | 최초 작성 (비교 표/운영 시나리오/확장 제안 포함) |

---
**문의/업데이트 절차**
1. 정책 변경 필요 시 Product/Ads 담당이 Supabase UPDATE 시나리오 초안 작성
2. 이 문서 표 갱신(PR) → 리뷰(Ads & Dev) → 머지
3. 릴리즈 후 로그 모니터링 24~48h, 이상 시 롤백(복구 SQL) 적용

> END OF DOCUMENT

