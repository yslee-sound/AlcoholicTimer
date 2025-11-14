# Funding Choices(UMP) 빠른 설정 가이드

이 문서는 AdMob/UMP 동의 폼 미설정(code=3, Publisher misconfiguration)을 해결하고, 앱에서 동의 수집이 정상 동작하도록 만드는 최소 절차입니다.

## 0) 준비물 체크
- AdMob 앱 ID (예: `ca-app-pub-8420908105703273~7175986319`)
- 앱 패키지명(예: `kr.sweetapps.alcoholictimer` / `.debug`)
- 개인정보처리방침 URL (정책에 반드시 필요)

### 개인정보 처리방침 언어/요건 빠른 가이드
- 언어: 영문 “필수”는 아닙니다. 사용자의 주요 언어로 이해 가능해야 합니다. 한국 사용자 대상이면 한국어 권장, 글로벌 대상이면 최소 영어 + 주요 타깃 언어를 권장합니다.
- URL: 로그인 없이 공개 접근 가능, 모바일에서 가독성 확보, 영구적으로 접근 가능(배포 후 URL 변경 최소화).
- 필수 포함 사항(AdMob/UMP 관점):
  - 수집 항목(광고 ID/기기 정보/IP 등) 및 이용 목적(광고, 분석, 성능, 측정)
  - 제3자 제공/처리자(예: Google AdMob)와 그 링크(예: https://policies.google.com/technologies/partner-sites)
  - 맞춤형 광고 동의/철회 방법(앱 내 설정, OS 광고 맞춤설정 해제 등)과 연락처
  - 보관 기간, 법적 근거(EU 대상 시), 아동 대상 여부/처리 방식
  - 지역별 권리 고지(EU/EEA·UK: GDPR 권리, 필요 시 미국 지역 고지)
- 다국어 제공 팁: 하나의 고정 URL에 언어 스위처를 두거나 `?lang=ko` 등으로 분기. Funding Choices에는 단일 URL만 넣지만 랜딩에서 언어를 감지/선택 가능하게 구성.

## 1) Funding Choices 열기(신 UI/구 UI 공통)
- 신 UI(첨부 스크린샷처럼 보이는 경우): AdMob 콘솔 좌측에서 [개인 정보 보호 및 메시지]로 이동 → 첫 화면의 카드에서 [유럽 규정] 섹션의 [만들기]를 누르면 Funding Choices 동의 메시지 생성 플로우로 진입합니다. (별도의 "Funding Choices" 메뉴가 보이지 않아도 정상입니다.)
- 구 UI: AdMob 콘솔 → Privacy & messaging → Funding Choices → Create
- 직접 접속: https://fundingchoices.google.com (동일한 Google 계정으로 로그인)

## 2) 앱 연결 상태 확인
- 동의 메시지 생성 플로우에서 앱을 선택합니다(Android). 목록에 보이지 않으면 [앱 추가]로 패키지명을 등록하고 AdMob 앱 ID와 연결합니다.

## 3) 메시지 생성(EU/EEA Consent)
1. [유럽 규정] 만들기(= EU consent message)
2. 앱 선택 (Android, 해당 앱 ID)
3. 기본 설정
   - Policy URL 입력(필수)
   - UI 언어/브랜드 색상(선택)
4. 벤더/목적(Ad Technology Providers)
   - 기본값(주요 공급업체) 권장
5. (선택) Additional consent(Non-IAB 벤더) 설정
6. 미리보기 확인
7. Publish(게시)

> 팁: 게시 후 정책 전파까지 5–30분 정도 소요될 수 있습니다.

## 4) 지역/플로우 정책
- 기본: EU/EEA, UK 대상
- 전 세계 표시가 필요하면 "Include all regions" 옵션 사용
- Age-related 설정은 제품 특성에 맞게

## 5) 앱에서 확인(UMP SDK)
앱을 재설치/데이터 삭제 후 실행하면 다음 로그가 보여야 정상입니다.
- `UmpConsentManager: UMP finished. canRequestAds=..., status=...`
- 더 이상 `Publisher misconfiguration` 경고가 나오지 않아야 합니다.
- App Open 로드 로그: `preload start ...` → `onAdLoaded app-open`

## 6) 디버그 테스트(선택)
- EU 규제 환경 강제 테스트: 로그의 `addTestDeviceHashedId("...")` 해시를 복사해 UMP Debug 설정 적용(개발 빌드 전용)
- 테스트 순서: 앱 데이터 삭제 → 실행/동의 → 광고 로드 확인

## 7) 자주 발생하는 오류와 해결
- code=3 Publisher misconfiguration
  - 메시지를 아직 게시하지 않았거나, 앱 연결이 잘못됨
  - 해결: EU consent message 생성 후 Publish, 5–30분 대기
- HTTP 403(광고 로드 실패)
  - 동의 폼 미게시/전파 지연, 또는 앱/계정 연결 불일치에서 발생
  - 해결: 위 misconfiguration 해소, Play services/AdMob SDK 최신 유지
- 메시지가 계속 안 뜸
  - canRequestAds=true이고 비EEA 지역이면 폼이 생략될 수 있음(정상). Debug geography로 강제 테스트

## 8) 운영 체크리스트
- [ ] EU consent message 게시 완료(최소 1개)
- [ ] Privacy URL 설정
- [ ] 앱과 메시지 연결 확인(정확한 앱 ID)
- [ ] canRequestAds 로그 정상, misconfiguration 경고 해소
- [ ] 광고 단위는 운영/테스트 분리 적용

## 9) 참고
- UMP Android 문서: https://developers.google.com/admob/android/privacy
- Funding Choices 콘솔: https://fundingchoices.google.com
- Google이 파트너 사이트/앱 데이터를 활용하는 방법: https://policies.google.com/technologies/partner-sites

---
문서 버전: 2025-11-14 / 소유: AlcoholicTimer Dev Team
