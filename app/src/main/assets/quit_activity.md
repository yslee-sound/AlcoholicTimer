# 금주 종료 결과 화면 상세 기획서 (UI 참고 반영)

본 기획서는 사용자가 금주 활동을 **중지(수동 종료/취소/목표 미달성)**한 직후, 해당 활동의 결과 요약을 시각적으로 보여주는 화면을 설계합니다.  
아래 이미지를 참고하여, 배경/통계/컨트롤 버튼 등 UI 구성방식을 반영합니다.

---

## 1. 화면 개요

**목적**
- 금주 목표 도달 전에 사용자가 **직접 종료/취소/중단** 시, 즉시 현재까지의 결과 요약을 제공
- 금주 중단 시점까지의 주요 성과(일수, 절약 금액, 절약 시간, 기대 수명 증가, 레벨 등)를 한눈에 확인
- 하단에 "중지", "계속" 버튼을 컨트롤 버튼 스타일로 제공

**진입 조건**
- 금주 타이머 수동 종료(취소/중단 버튼 클릭)

---

## 2. 화면 구성


### (1) 상단 배경 영역
- **배경**: 앱 특성에 맞춰 "금주 타이머" 상태를 시각적으로 표현하는 영역
    - 예시: 금주 기간에 해당하는 배경 이미지/일러스트/그래픽(금주와 연관된 상징)
    - **상단 정보**: 현재 시간/상태 아이콘/배터리 표시 등 시스템 UI는 그대로 유지

### (2) 금주 기록 요약 영역 (통계 영역)
- **상단 행(3개)**: 금주 일수, 금주 레벨, 경과 시간
    - **예시**:
        - 13.00 | 의지의 2주 | 13일 04:00
        - **하단 라벨**: 금주 일수 | 레벨명 | 경과 시간

- **하단 행(3개)**: 절약 금액, 절약 시간, 기대 수명+
    - **예시**:
        - 56,000 | 28시간 | +0일
        - **하단 라벨**: 절약 금액 | 절약 시간 | 기대 수명

- **각 항목**
    - **숫자**: 32sp, Bold, 중앙 정렬
    - **라벨**: 14sp, 회색, 중앙 정렬
    - **간격**: 각 항목 사이 24dp
    - **배경**: 흰색, 투명 라인 구분

### (3) 구분선
- **얇은 Divider**: 통계 영역과 컨트롤 버튼 영역 사이에 1dp 연한 회색 라인

### (4) 컨트롤 버튼 영역
- **하단 대형 버튼 2개**:
    1. **중지 버튼**:
        - 원형, 검은색 배경, 중앙에 ■ 아이콘(정지)
        - 크기: 80dp
        - 텍스트 없음(아이콘만)
        - 클릭 시: 종료 확정, 기록 남김
    2. **계속 버튼**:
        - 원형, 사용자 레벨색 배경, 중앙에 ▶ 아이콘(재생)
        - 크기: 80dp
        - 텍스트 없음(아이콘만)
        - 클릭 시: 금주 활동 지속

    - **버튼 간 간격**: 32dp

---

## 3. 데이터 연동 규칙

- **현재 금주 기록 정보**: SharedPreferences 또는 DB에서 마지막 금주 기록 참조
    - `start_date`, `target_date`, `quit_date`, `total_days`, `level_name`, `saved_money`, `saved_time`, `life_gain_days`
- **레벨/색상**: level_activity.md 기준
- **통계 계산**: settings_activity.md의 범주 매핑값 활용

---

## 4. UI 상세 스펙

### (1) 색상 시스템
- 상단 배경: 앱 테마 일러스트
- 통계 카드/텍스트: 흰색 배경, 검은색 텍스트, 회색 라벨
- 컨트롤 버튼: 중지(검은색), 계속(주황색)

### (2) 타이포그래피
- 통계 숫자: 32sp Bold
- 라벨: 14sp 회색
- 버튼 아이콘: 48dp 이상

### (3) 레이아웃 & spacing
- 상단/하단 여백: 24dp
- 통계 영역 각 항목 간 24dp
- 컨트롤 버튼 크기: 80dp, 간격 32dp
- 전체 좌우 패딩: 32dp

---

## 5. 사용자 상호작용

- **중지 버튼**: 금주 종료 확정, 기록 남기고 StatusActivity 또는 MainActivity로 이동
- **계속 버튼**: 금주 활동 지속, 이전 화면 복귀 또는 StatusActivity로 이동

---

## 6. 예시 레이아웃

```
┌─────────────────────────────┐
│   배경 일러스트               │
├─────────────────────────────┤
│ 13.00    의지의 2주   13:04 │
│ 금주일수  레벨명    경과시간 │
│ 56,000   28시간    +0일     │
│ 절약금액 절약시간  기대수명 │
├─────────────────────────────┤
│   ■        ▶                │
│ 중지버튼  계속버튼           │
└─────────────────────────────┘
```

---

## 7. Jetpack Compose 컴포넌트 계층 예시

```kotlin
BaseScreen {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 상단 지도/배경 영역
        Box(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            // 지도 또는 일러스트 표시
        }

        Spacer(24.dp)
        
        // 통계 요약 영역
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${currentDays}", fontSize = 32.sp, fontWeight = Bold)
                Text("금주일수", fontSize = 14.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(levelName, fontSize = 32.sp, fontWeight = Bold)
                Text("레벨명", fontSize = 14.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(elapsedTime, fontSize = 32.sp, fontWeight = Bold)
                Text("경과시간", fontSize = 14.sp, color = Color.Gray)
            }
        }
        Spacer(16.dp)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${savedMoney}", fontSize = 32.sp, fontWeight = Bold)
                Text("절약금액", fontSize = 14.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${savedTime}", fontSize = 32.sp, fontWeight = Bold)
                Text("절약시간", fontSize = 14.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("+${lifeGainDays}", fontSize = 32.sp, fontWeight = Bold)
                Text("기대수명", fontSize = 14.sp, color = Color.Gray)
            }
        }

        Divider(Modifier.padding(vertical = 16.dp))

        // 컨트롤 버튼 영역
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = { /* 종료 확정 */ },
                Modifier.size(80.dp).background(Color.Black, CircleShape)
            ) {
                Icon(Icons.Default.Stop, contentDescription = "중지", tint = Color.White, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.width(32.dp))
            IconButton(
                onClick = { /* 금주 계속 */ },
                Modifier.size(80.dp).background(Color(0xFFFFA726), CircleShape)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "계속", tint = Color.White, modifier = Modifier.size(48.dp))
            }
        }

        Spacer(24.dp)

        // 하단 응원 메시지 영역
        Box(
            Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(12.dp)
        ) {
            Text("선택된 응원 메시지 없음", color = Color.White, fontSize = 16.sp)
        }
    }
}
```

---

## 8. 접근성 및 확장

- 컨트롤 버튼: 충분한 크기와 대비로 터치 용이
- 주요 수치/통계: 중앙 정렬, 큰 폰트로 가독성 강화
- 하단 정보 영역: 메시지/음악/다짐 등 확장 가능
- 지도 영역: 실제 위치정보 또는 금주 관련 일러스트로 대체 가능

---

이 문서는 금주 종료(중지/취소/목표 미달성) 결과 요약 화면을 참고 이미지 기반으로 Jetpack Compose로 구현할 때 사용할 수 있는 상세 기획서입니다.