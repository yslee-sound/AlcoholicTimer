# 🔍 Crashlytics 5회 탭 기능 테스트 가이드

## ⚠️ 문제 상황

**버전 정보를 5번 클릭해도 Toast 메시지가 나타나지 않음**

---

## 📁 **프로젝트 구조 (중요!)**

현재 앱은 **Debug와 Release 빌드마다 다른 Firebase 프로젝트**를 사용합니다:

```
app/src/
├── debug/
│   └── google-services.json     ← Firebase Dev 프로젝트
├── release/
│   └── google-services.json     ← Firebase Prod 프로젝트
└── main/
```

### **빌드별 전송 대상**

| 빌드 타입 | Firebase 프로젝트 | Crashlytics 전송 위치 |
|----------|------------------|---------------------|
| **Debug** | Firebase **Dev** | Dev 프로젝트 Crashlytics |
| **Release** | Firebase **Prod** | Prod 프로젝트 Crashlytics |

---

## 🛠️ 해결 방법: 상세 디버깅 로그 추가

코드에 **매 탭마다 로그를 출력**하도록 수정했습니다. 이제 Logcat을 통해 클릭이 제대로 감지되는지 확인할 수 있습니다.

---

## 📱 테스트 절차

### 1️⃣ **앱 실행 및 Logcat 준비**

#### **방법 1: Android Studio**
```
1. Android Studio에서 앱 실행 (Shift+F10)
2. 하단 "Logcat" 탭 선택
3. 필터에 "AboutScreen" 입력
```

#### **방법 2: ADB 명령어**
```powershell
adb -s emulator-5554 logcat -s AboutScreen
```

---

### 2️⃣ **설정 화면으로 이동**
1. 앱 하단 네비게이션 바에서 **5번째 탭(설정)** 선택
2. 스크롤을 내려서 **"버전 정보"** 항목 확인

---

### 3️⃣ **버전 정보를 5회 빠르게 탭**
- 버전 정보 행을 **빠르게 5번 연속 탭** (1초 이내)
- Logcat에서 다음과 같은 로그가 출력되어야 함:

```
D/AboutScreen: 🔘 버전 정보 탭 감지! (현재 카운트: 0)
D/AboutScreen: ⏱️ 1초 이상 경과 → 카운트 리셋: 1

D/AboutScreen: 🔘 버전 정보 탭 감지! (현재 카운트: 1)
D/AboutScreen: ⏱️ 1초 이내 탭 → 카운트 증가: 2

D/AboutScreen: 🔘 버전 정보 탭 감지! (현재 카운트: 2)
D/AboutScreen: ⏱️ 1초 이내 탭 → 카운트 증가: 3

D/AboutScreen: 🔘 버전 정보 탭 감지! (현재 카운트: 3)
D/AboutScreen: ⏱️ 1초 이내 탭 → 카운트 증가: 4

D/AboutScreen: 🔘 버전 정보 탭 감지! (현재 카운트: 4)
D/AboutScreen: ⏱️ 1초 이내 탭 → 카운트 증가: 5
D/AboutScreen: 🎯 5회 탭 달성! Crashlytics 테스트 보고서 전송 시작...
D/AboutScreen: ✅ Crashlytics 테스트 보고서 전송 완료 (버전: 1.1.6)
D/AboutScreen: 🔄 카운터 리셋 완료
```

---

### 4️⃣ **Toast 메시지 확인**
5회 탭 직후 화면 하단에 다음 메시지가 나타나야 함:
```
"Crashlytics 테스트 보고서 전송 완료."
```

### 5️⃣ **Firebase Console 확인**

#### **Debug 빌드 → Firebase Dev 프로젝트**
1. Firebase Console 접속: https://console.firebase.google.com
2. **AlcoholicTimer Dev** 프로젝트 선택
3. 왼쪽 메뉴: **Crashlytics**
4. 상단 탭: **Non-fatals**
5. **5~10분 후** 보고서 확인:
   ```
   Test Non-Fatal Exception - Crashlytics Prod Check (v1.1.6-debug)
   ```

#### **Release 빌드 → Firebase Prod 프로젝트**
1. Firebase Console 접속
2. **AlcoholicTimer Prod** 프로젝트 선택
3. 동일하게 **Crashlytics → Non-fatals** 확인

---

## 🔍 **문제 진단 체크리스트**

### ✅ **Case 1: 로그가 전혀 안 나옴**
**증상**: `🔘 버전 정보 탭 감지!` 로그가 하나도 안 보임

**원인**: 클릭 이벤트가 감지되지 않음

**해결 방법**:
1. 버전 정보 **텍스트 부분**이 아닌 **행 전체**를 클릭해보세요
2. 에뮬레이터/기기를 재시작해보세요
3. 앱을 완전히 종료하고 다시 실행해보세요

---

### ✅ **Case 2: 로그는 나오는데 카운트가 1에서 멈춤**
**증상**: 
```
D/AboutScreen: 🔘 버전 정보 탭 감지! (현재 카운트: 0)
D/AboutScreen: ⏱️ 1초 이상 경과 → 카운트 리셋: 1
D/AboutScreen: 🔘 버전 정보 탭 감지! (현재 카운트: 1)
D/AboutScreen: ⏱️ 1초 이상 경과 → 카운트 리셋: 1  ← 계속 리셋됨
```

**원인**: 탭 간격이 1초 이상 벌어짐

**해결 방법**:
- 더 빠르게 연속으로 탭하세요 (0.5초 간격 이내)
- 한 손가락으로 빠르게 톡톡톡 두드리듯이

---

### ✅ **Case 3: 5회 달성했는데 Toast가 안 나옴**
**증상**:
```
D/AboutScreen: 🎯 5회 탭 달성! Crashlytics 테스트 보고서 전송 시작...
D/AboutScreen: ✅ Crashlytics 테스트 보고서 전송 완료 (버전: 1.1.6)
```
로그는 정상인데 Toast 메시지가 화면에 안 보임

**원인**: Toast가 다른 화면 요소에 가려짐

**해결 방법**:
1. 스크롤을 맨 아래로 내려보세요
2. 다른 Toast나 Snackbar가 동시에 표시되는지 확인
3. 에뮬레이터 화면 하단을 주시

---

### ✅ **Case 4: Crashlytics 전송 실패 에러**
**증상**:
```
E/AboutScreen: ❌ Crashlytics 테스트 보고서 전송 실패
```

**원인**: Firebase Crashlytics가 초기화되지 않음

**해결 방법**:
1. `google-services.json` 파일이 올바른 위치에 있는지 확인
   - 위치: `app/google-services.json`
2. `build.gradle.kts`에 Crashlytics 플러그인이 있는지 확인
   ```kotlin
   id("com.google.gms.google-services")
   // id("com.google.firebase.crashlytics") // 제거됨 - SDK만 사용
   ```
3. Crashlytics SDK가 dependencies에 있는지 확인
   ```kotlin
   implementation("com.google.firebase:firebase-crashlytics-ktx")
   ```

---

## 📊 **정상 동작 시 전체 로그**

```
D/AboutScreen: 🔘 버전 정보 탭 감지! (현재 카운트: 0)
D/AboutScreen: ⏱️ 1초 이상 경과 → 카운트 리셋: 1
D/AboutScreen: 🔘 버전 정보 탭 감지! (현재 카운트: 1)
D/AboutScreen: ⏱️ 1초 이내 탭 → 카운트 증가: 2
D/AboutScreen: 🔘 버전 정보 탭 감지! (현재 카운트: 2)
D/AboutScreen: ⏱️ 1초 이내 탭 → 카운트 증가: 3
D/AboutScreen: 🔘 버전 정보 탭 감지! (현재 카운트: 3)
D/AboutScreen: ⏱️ 1초 이내 탭 → 카운트 증가: 4
D/AboutScreen: 🔘 버전 정보 탭 감지! (현재 카운트: 4)
D/AboutScreen: ⏱️ 1초 이내 탭 → 카운트 증가: 5
D/AboutScreen: 🎯 5회 탭 달성! Crashlytics 테스트 보고서 전송 시작...
D/AboutScreen: ✅ Crashlytics 테스트 보고서 전송 완료 (버전: 1.1.6-debug)
D/AboutScreen: 🔄 카운터 리셋 완료
```

**Toast 메시지**: "Crashlytics 테스트 보고서 전송 완료."

---

## 🎥 **Logcat 필터링 명령어**

### **Android Studio**
```
AboutScreen
```

### **ADB (PowerShell)**
```powershell
# 기본 필터
adb -s emulator-5554 logcat -s AboutScreen

# 시간 정보 포함
adb -s emulator-5554 logcat -s AboutScreen -v time

# 파일로 저장
adb -s emulator-5554 logcat -s AboutScreen > crashlytics_test.log
```

---

## 🔧 **추가 디버깅 팁**

### 1. **현재 카운트 확인**
매 탭마다 `(현재 카운트: X)` 로그를 확인하여 카운트가 증가하는지 체크

### 2. **타이밍 연습**
처음에는 천천히 1-2-3-4-5 숫자를 세면서 탭하고, 익숙해지면 빠르게

### 3. **에뮬레이터 vs 실제 기기**
- 에뮬레이터: 마우스 클릭으로 테스트
- 실제 기기: 손가락 탭으로 테스트 (더 빠름)

---

## ✅ **성공 확인 체크리스트**

- [ ] Logcat에 `🔘 버전 정보 탭 감지!` 로그 5번 출력
- [ ] 5회째 탭에서 `🎯 5회 탭 달성!` 로그 출력
- [ ] `✅ Crashlytics 테스트 보고서 전송 완료` 로그 출력
- [ ] 화면에 "Crashlytics 테스트 보고서 전송 완료." Toast 표시
- [ ] Firebase Console 확인 (5~10분 후):
  - **Debug 빌드**: Firebase **Dev** 프로젝트 → Crashlytics → Non-fatals
  - **Release 빌드**: Firebase **Prod** 프로젝트 → Crashlytics → Non-fatals

---

## 📞 **여전히 안 된다면?**

1. **Logcat 로그 전체를 복사**해서 공유해주세요
2. **버전 정보 항목 스크린샷** 첨부
3. **에뮬레이터/실제 기기** 중 어떤 환경인지 알려주세요
4. **Debug/Release** 빌드 중 어느 것인지 확인

---

## 🎉 **문서 업데이트**

이 테스트 가이드는 다음 문서에 반영되었습니다:
```
G:\Workspace\AlcoholicTimer\docs\CRASHLYTICS_PROD_CHECK_FEATURE.md
```

