# 레벨 화면 실시간 업데이트 디버깅 가이드

## 🔍 로그 확인 명령어

### 1. Tab03ViewModel 로그 확인
```powershell
adb -s emulator-5554 logcat -v time | findstr "Tab03ViewModel"
```

### 2. 타이머 테스트 모드 로그 확인
```powershell
adb -s emulator-5554 logcat -v time | findstr "TimerState"
```

### 3. 전체 로그 확인
```powershell
adb -s emulator-5554 logcat -v time | findstr "레벨"
```

## 📋 확인사항

### 1. 타이머 테스트 모드가 켜져 있는지 확인
- 설정 → 디버그 메뉴 → "타이머 테스트 모드" 스위치 ON
- 로그에서 `[테스트]` 문구 확인

### 2. 레벨 업데이트 로그 확인
정상 동작 시 다음과 같은 로그가 0.1초마다 출력되어야 합니다:

```
12-03 15:30:01.123 D Tab03ViewModel: [테스트] 레벨 업데이트: total=1000ms, dayInMillis=1000, days=2, level=2131689472, daysFloat=1.0
12-03 15:30:01.223 D Tab03ViewModel: [테스트] 레벨 업데이트: total=1100ms, dayInMillis=1000, days=2, level=2131689472, daysFloat=1.1
12-03 15:30:02.123 D Tab03ViewModel: [테스트] 레벨 업데이트: total=2000ms, dayInMillis=1000, days=3, level=2131689472, daysFloat=2.0
```

**확인 포인트:**
- `dayInMillis=1000` (테스트 모드)
- `days` 값이 1초마다 1씩 증가
- `daysFloat` 값이 0.1씩 증가

### 3. 레벨 전환 확인
- 3일 차 → 4일 차: Lv.1 → Lv.2 (level 값 변경)
- 7일 차 → 8일 차: Lv.2 → Lv.3 (level 값 변경)

## 🚨 문제 해결

### 문제 1: 로그가 안 나옴
**원인:** 앱이 실행되지 않았거나 레벨 화면에 진입하지 않음  
**해결:** 앱 실행 → 레벨 화면(Tab03) 이동

### 문제 2: dayInMillis=86400000 (정상 모드로 동작)
**원인:** 타이머 테스트 모드가 꺼져 있음  
**해결:** 설정 → 디버그 → 타이머 테스트 모드 ON

### 문제 3: days 값이 변하지 않음
**원인:** 타이머가 시작되지 않았거나 currentElapsed=0  
**해결:** 타이머 시작 후 레벨 화면 이동

### 문제 4: 레벨 화면 UI가 변하지 않음
**원인 1:** ViewModel의 StateFlow가 UI에 제대로 연결되지 않음  
**원인 2:** 레벨 화면이 ViewModel을 사용하지 않고 있음  

**확인 방법:**
1. 로그에서 `days` 값이 변하는지 확인
2. 변한다면 → UI 연결 문제 (Tab03.kt 확인)
3. 변하지 않는다면 → ViewModel 로직 문제

## 🔧 다음 단계

로그 확인 후 다음 정보를 제공해 주세요:

1. **로그 출력 여부**
   - 로그가 나오나요? (예/아니오)

2. **dayInMillis 값**
   - 1000 (테스트 모드) / 86400000 (정상 모드)

3. **days 값 변화**
   - 1초마다 증가하나요? (예/아니오)

4. **UI 변화**
   - 레벨 화면이 변하나요? (예/아니오)

이 정보를 바탕으로 정확한 원인을 파악하여 수정하겠습니다.

