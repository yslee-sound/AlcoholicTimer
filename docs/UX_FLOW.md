# AlcoholicTimer UX 흐름 명세 (UX_FLOW)

마지막 업데이트: 2025-10-02

## 1. 네비게이션 개요
- 햄버거 메뉴(드로어) 항목
  - 금주 → StartActivity 또는 RunActivity (진행 중인지 여부에 따라 분기)
  - 기록 → RecordsActivity (통계/리스트 섹션)
  - 레벨 → LevelActivity
  - 설정 → SettingsActivity
- 추가 화면
  - AllRecordsActivity (전체 기록 일람)
  - DetailActivity (개별 기록 상세)
  - NicknameEditActivity (별명 편집)
  - components.AddRecordActivity (사용자 기록 추가)

## 2. 화면 흐름(High-level)
1) 첫 진입
   - 기존 진행 기록(start_time) 유무로 분기
   - 없음 → StartActivity에서 목표 설정 후 RunActivity로 이동
   - 있음 → RunActivity에서 진행 상황 표시
2) 기록 탭
   - RecordsActivity
   - 최신 카드/간단 통계/기간 필터
   - 전체 보기로 AllRecordsActivity 이동 가능
3) 세부 기록
   - AllRecordsActivity/RecordsActivity에서 카드를 선택 → DetailActivity
4) 설정
   - SettingsActivity에서 목표/비용/빈도 등 사용자 설정

## 3. 필터/선택 컴포넌트
- WeekPickerBottomSheet: 최근 주 선택, NumberPicker 기반
- MonthPickerBottomSheet: 연/월 선택, 데이터 기반 월 옵션
- YearPickerBottomSheet: 기록 범위 내 연도 선택

## 4. 상태 처리 패턴
- 로딩: ProgressIndicator 가운데 배치
- 빈 상태: Empty/Hint 메시지 카드
- 에러: 메시지 + 재시도 버튼(Records/AllRecords 공통)

## 5. 접근성/일관성
- Icon에는 의미 있는 contentDescription 제공
- 터치 타겟: 최소 44dp
- 텍스트 대비: Material3 기본 팔레트 기준 유지
- Divider → HorizontalDivider 사용(신규 컴포넌트 우선)

## 6. 용어/표기 규칙
- 날짜 포맷: yyyy.MM.dd (상황에 따라 HH:mm 병행)
- 진행률: 소수점 1자리 또는 정수, 단위 %
- 기간 단위: 일(day) 기준 산출

## 7. 체크리스트(작업용)
- [ ] 드로어 메뉴 항목/순서 유지
- [ ] 주/월/년 필터 동작 QA
- [ ] 빈/에러/로딩 상태 각 1회 이상 수동 테스트

## 8. 참고
- 전체 기획안: `docs/APP_SPEC.md`
- 관련 코드: `app/src/main/java/com/example/alcoholictimer/components/`

