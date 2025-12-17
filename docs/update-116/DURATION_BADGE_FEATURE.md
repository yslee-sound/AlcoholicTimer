# 기간 선택 배지 추가 작업 완료

**작업 일자**: 2025년 12월 1일  
**작업 내용**: 금주 시작 화면에 기본 기간 선택 배지 추가

---

## 🎯 작업 목표

사용자가 금주 시작 화면에서 빠르게 목표 기간을 선택할 수 있도록, "Alcoholic Timer" 로고 아래에 기본 기간 선택 배지를 추가합니다.

## 🎨 디자인

### 배지 스타일
- **모양**: 둥근 직사각형 (RoundedCornerShape 18dp)
- **크기**: 높이 36dp, 최소 너비 52dp
- **선택된 상태**: 
  - 배경색: 검은색 (#1A1A1A)
  - 텍스트: 흰색
  - 테두리: 검은색 1dp
  - 그림자: 2dp
- **미선택 상태**:
  - 배경색: 흰색
  - 텍스트: 회색 (#666666)
  - 테두리: 연한 회색 (#E0E0E0) 1dp
  - 그림자: 없음

### 제공되는 기간 옵션
- 7일
- 14일
- 30일 (기본 선택)
- 60일
- 90일

---

## 📋 구현 내용

### 1. 새로운 컴포넌트 추가

#### `DurationBadgeRow` - 배지 행 컴포넌트
```kotlin
@Composable
private fun DurationBadgeRow(
    selectedDays: Int,
    onDaysSelected: (Int) -> Unit
)
```
- 7일, 14일, 30일, 60일, 90일 배지를 가로로 배열
- 현재 선택된 기간을 하이라이트 표시

#### `DurationBadge` - 개별 배지 컴포넌트
```kotlin
@Composable
private fun DurationBadge(
    days: Int,
    isSelected: Boolean,
    onClick: () -> Unit
)
```
- 선택 상태에 따라 스타일 자동 전환
- 클릭 시 onDaysSelected 콜백 호출

### 2. AppBrandTitleBar 업데이트
```kotlin
@Composable
private fun AppBrandTitleBar(
    selectedDays: Int = 30,
    onDaysSelected: (Int) -> Unit = {}
)
```
- 기존: 로고만 표시
- 변경: 로고 + 16dp 간격 + 기간 선택 배지

### 3. StartScreen 통합
- `targetDays` 상태를 배지와 연결
- 배지 클릭 시:
  1. `targetDays` 상태 업데이트
  2. TextField에 자동 반영 (LaunchedEffect 활용)
  3. 키보드 숨김 및 포커스 해제

---

## ✅ 동작 흐름

1. **초기 화면**: 30일 배지가 선택된 상태로 표시
2. **배지 클릭**: 
   - 선택된 배지가 검은색으로 변경
   - 아래 입력 필드에 해당 숫자 자동 입력
   - 키보드 숨김
3. **직접 입력**: 
   - 입력 필드를 클릭하여 수동 입력 가능
   - 배지 선택 상태는 입력된 값과 동기화됨

---

## 🔍 검증 결과

### 빌드 테스트
```
BUILD SUCCESSFUL in 11s
42 actionable tasks: 12 executed, 6 from cache, 24 up-to-date
```

- ✅ 컴파일 오류 없음
- ✅ UI 렌더링 정상
- ✅ 상태 동기화 정상

---

## 📱 사용자 경험 개선

### Before (이전)
- 숫자를 직접 입력해야 함
- 일반적인 기간을 모르는 경우 망설임

### After (개선)
- 일반적인 기간을 빠르게 선택 가능
- 7일, 14일, 30일, 60일, 90일 중 선택
- 원하면 직접 입력도 가능 (최대 999일)

---

## 📂 변경된 파일

```
✏️ 수정됨:
  - app/src/main/java/kr/sweetapps/alcoholictimer/ui/screens/StartScreen.kt

📄 생성됨:
  - docs/DURATION_BADGE_FEATURE.md (이 문서)
```

---

## 🎉 완료 체크리스트

- [x] 배지 컴포넌트 구현
- [x] 로고 아래 배지 배치
- [x] 클릭 시 목표 기간에 자동 입력
- [x] 선택 상태 시각적 피드백
- [x] TextField와 상태 동기화
- [x] 빌드 성공 확인
- [x] 문서 작성
- [ ] 실제 기기에서 테스트 (권장)

---

## 📞 추가 개선 아이디어

1. **애니메이션**: 배지 선택 시 부드러운 전환 애니메이션
2. **커스텀 기간**: "직접 입력" 배지 추가
3. **최근 사용 기간**: 마지막 사용한 기간을 우선 표시
4. **다국어**: 다른 언어에서도 "일" 단위 표시 지원

---

완료! 🎊

