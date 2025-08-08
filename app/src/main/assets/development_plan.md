### ✅ 제목: **알중시계 앱 개발 – 안드로이드 스튜디오에서 단계별 구현 과정**
- Jetpack Compose 기반으로 구현하며, 미니멀하고 직관적인 UX를 제공합니다.

---

## ✅ 전체 구현 단계 요약 (순서도 개념)

```
1. 프로젝트 세팅
2. 앱 기본화면 설계 (MainActivity)
3. 금주 시작일 저장 화면 구현
4. 현재 금주 상태 표시 화면 구현
5. 금주 레벨 계산 기능 구현
6. 금주 활동 기록 보기 화면 구현
7. assets/requirements.md 정리

```

---

## 🧩 단계별 세부 구현 절차

---

### ✅ 1단계: 프로젝트 생성 및 초기 세팅

| 할 일 | 설명 |
| --- | --- |
| ✅ Android Studio에서 새 프로젝트 생성 | `Empty Activity`, Kotlin, API 21 이상 |
| ✅ 앱 이름 바꾸기 | `strings.xml` → "알중시계" |
| ✅ `assets/` 폴더 생성 및 `requirements.md` 작성 | 앱 목적/기능/요구사항 문서화 |

---

### ✅ 2단계: 앱 시작 화면 (MainActivity)

| 할 일 | 설명                                              |
| --- |-------------------------------------------------|
| 🔹 activity_main.xml 구성 | 버튼 2개: "금주", "활동"                               |
| 🔹 MainActivity.kt 구현 | 버튼 클릭 시 `StartActivity` 또는 `StatusActivity`로 이동 |
| 🔹 인텐트(Intent) 학습 | 화면 이동 코드 이해 필요                                  |

---

### ✅ 3단계: 금주 시작일 설정 화면 (StartActivity)

| 할 일 | 설명 |
| --- | --- |
| 🔹 DatePicker 추가 | 시작일 선택 (시간은 제외) |
| 🔹 목표일 설정 (선택사항) | Optional DatePicker |
| 🔹 저장 버튼 → SharedPreferences에 저장 | `start_date`, `target_date` 저장 |
| 🔹 저장 완료 후 StatusActivity 로 이동 | 저장 후 즉시 확인 가능하게 |

> 🔔 학습: SharedPreferences, 날짜 포맷 처리
>

---

### ✅ 4단계: 금주 현황 화면 (StatusActivity)

| 할 일 | 설명 |
| --- | --- |
| 🔹 시작일 불러오기 | `SharedPreferences`에서 가져오기 |
| 🔹 오늘 날짜와 비교 → 금주일수 계산 | `ChronoUnit.DAYS.between()` |
| 🔹 화면에 일수 표시 | “오늘로 금주 N일째!” |
| 🔹 응원 메시지 랜덤 표시 | 리스트에서 랜덤 선택 출력 |

---

### ✅ 5단계: 금주 레벨 배지 시스템

| 할 일 | 설명 |
| --- | --- |
| 🔹 날짜 기반 레벨 계산 함수 만들기 | 총 7단계 레벨 조건에 맞게 분기 |
| 🔹 현재 레벨 표시 | 숫자 or 색상 배지로 표시 |
| 🔹 다음 레벨까지 남은 일수 표시 | 그래프 또는 텍스트로 표현 가능 (MVP는 텍스트만) |

> 🔔 학습: when 문, Kotlin에서 함수 작성법
>

---

### ✅ 6단계: 금주 활동 기록 보기 화면 (RecordsActivity)

| 할 일 | 설명 |
| --- | --- |
| 🔹 금주 시작일부터 오늘까지 일수 저장 | SharedPreferences 또는 리스트 저장 |
| 🔹 주간, 월간, 전체 단위 계산 | 날짜 범위 필터링 후 개수 계산 |
| 🔹 간단한 리스트로 출력 | 리사이클러뷰는 차후 확장용, MVP는 텍스트 리스트로도 충분 |

---

### ✅ 7단계: 요구사항 문서 정리 (assets/requirements.md)

| 할 일 | 설명 |
| --- | --- |
| 🔹 마크다운 형식으로 정리 | 목적, 기능, 화면, 저장 구조 등 |
| 🔹 Git 또는 Notion에 백업 (선택) | 변경 이력 추적용 |
| 🔹 이 문서를 코딩 도중 계속 참고 | 방향을 잃지 않게 중심 문서 역할 |

---

## ⛳ 전체 개발 후 예상 흐름 요약

```
MainActivity
  ├──> StartActivity → StatusActivity
  └──> StatusActivity → RecordsActivity / LevelActivity

```