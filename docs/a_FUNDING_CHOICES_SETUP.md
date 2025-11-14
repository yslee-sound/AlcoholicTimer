# Funding Choices(UMP) 빠른 설정 가이드

이 문서는 AdMob/UMP 동의 폼 미설정(code=3, Publisher misconfiguration)을 해결하고, 앱에서 동의 수집이 정상 동작하도록 만드는 최소 절차입니다.

## 0) 준비물 체크
- AdMob 앱 ID (예: `ca-app-pub-8420908105703273~7175986319`)
- 앱 패키지명(예: `kr.sweetapps.alcoholictimer` / `.debug`)
- 개인정보처리방침 URL (정책에 반드시 필요)

## 1) Funding Choices 접속
- 경로 1: AdMob 콘솔 → Privacy & messaging → Funding Choices 열기
- 경로 2: https://fundingchoices.google.com 접속 후 Google Ads 계정으로 로그인

## 2) 앱 연결 상태 확인
- Funding Choices의 Apps 목록에 Android 앱이 떠야 합니다.
- 보이지 않으면 “Add app”로 패키지명을 추가하고, AdMob 앱 ID와 연결되어 있는지 확인합니다.

## 3) 메시지 생성(EU/EEA Consent)
1. Create → EU consent message 선택
2. 앱 선택 (Android, 해당 앱 ID)
3. 기본 설정
   - Policy URL 입력
   - UI 언어/브랜드 색상(선택)
4. 벤더/목적(Ad Technology Providers)
   - 기본값(주요 공급업체) 그대로 사용 권장
   - 특정 공급업체 제한 필요 시만 편집
5. (선택) Additional consent(Non-IAB 벤더) 설정
6. 미리보기로 문구/스타일 확인
7. Publish(게시)

> 팁: 게시 후 정책 전파까지 5–30분 정도 소요될 수 있습니다.

## 4) 지역/플로우 정책
- EU/EEA, UK에만 표시(기본)
- 전 세계 표시가 필요하면 "Include all regions"로 확장
- Age-related 설정은 제품 성격에 맞게 설정

## 5) 앱에서 확인(UMP SDK)
앱을 재설치/데이터 삭제 후 실행하면 다음 로그가 보여야 정상입니다.
- `UmpConsentManager: UMP finished. canRequestAds=..., status=...`
- 더 이상 `Publisher misconfiguration` 경고가 나오지 않아야 합니다.
- App Open 로드 로그가 이어져야 합니다: `preload start ...` → `onAdLoaded app-open`

## 6) 디버그 테스트(선택)
- 에뮬/해외 단말에서 강제로 EU 규제 환경을 테스트하려면 Debug geography 사용:
  - 로그에서 **Use new ConsentDebugSettings.Builder().addTestDeviceHashedId("...")** 메시지의 해시를 복사
  - 앱 코드에서 UMP DebugGeography 설정(개발 빌드 한정) 후 테스트
- 테스트 절차
  1) 앱 데이터 삭제
  2) 앱 실행 → 동의 폼 노출/완료
  3) 광고 요청 정상화 확인(App Open/Banner/Interstitial 로드)

## 7) 자주 발생하는 오류와 해결
- code=3 Publisher misconfiguration
  - 메시지가 하나도 게시되지 않았거나, 앱이 잘못 연결됨
  - 해결: EU consent message 생성 → Publish → 5–30분 대기
- HTTP 403 (광고 로드 실패)
  - 동의 폼 미게시/전파 전, 또는 잘못된 앱/계정 연결에서 자주 발생
  - 해결: 위 misconfiguration 해소 후 재시도, Google Play services/AdMob SDK 최신 유지
- 메시지가 계속 안 뜸
  - canRequestAds=true이고 지역이 EEA 외면 폼이 생략됨(정상)
  - 강제 테스트는 Debug geography 사용

## 8) 운영 체크리스트
- [ ] EU consent message 게시 완료(최소 1개)
- [ ] Privacy URL 설정
- [ ] 앱과 메시지 연결 확인(정확한 앱 ID)
- [ ] canRequestAds 로그 정상, misconfiguration 경고 해소
- [ ] 광고 단위는 운영/테스트 분리 적용

## 9) 참고
- UMP Android 문서: https://developers.google.com/admob/android/privacy
- Funding Choices 콘솔: https://fundingchoices.google.com

---
문서 버전: 2025-11-14 / 소유: AlcoholicTimer Dev Team

