# 레벨 시스템 전면 개편 (2025-12-03)

## 📋 개요
기존 8단계 레벨 시스템을 **11단계 시스템**으로 확장하고, 날짜 계산 기준을 변경하였습니다.

## 🎯 주요 변경 사항

### 1. 레벨 단계 확장
- **기존:** 8단계 (Lv.1 ~ Lv.8)
- **변경:** 11단계 (Lv.1 ~ Legend)

### 2. 날짜 기준 변경
- **기존:** 0일부터 시작 (시작일 = 0일)
- **변경:** 1일 차부터 시작 (시작일 = 1일 차)

### 3. 레벨 구간 재조정
기존의 0~6일, 7~13일 구간에서 1~3일, 4~7일 등으로 사용자 친화적으로 변경

## 📊 새로운 레벨 체계

| 레벨 | 타이틀 | 일수 범위 | 색상 |
|------|--------|----------|------|
| Lv.1 | 알코올 스톱 | 1~3일 | 빨강 (Red) |
| Lv.2 | 3일 컷 통과 | 4~7일 | 진한 주황 (Dark Orange) |
| Lv.3 | 1주 클리어 | 8~14일 | 주황 (Orange) |
| Lv.4 | 피부의 변화 | 15~21일 | 노랑 (Yellow) |
| Lv.5 | 습관 형성 (21일) | 22~30일 | 연두 (Lime) |
| Lv.6 | 한달의 기적 | 31~60일 | 초록 (Green) |
| Lv.7 | 달라진 핏(Fit) | 61~99일 | 하늘 (Sky Blue) |
| Lv.8 | 100일, 프로 금주러 | 100~179일 | 파랑 (Blue) |
| Lv.9 | 플러스 통장 | 180~299일 | 보라 (Purple) |
| Lv.10 | 금주 마스터 | 300~364일 | 진한 보라 (Dark Purple) |
| Legend | 전설의 레전드 | 365일+ | 금색 (Gold) |

## 🔧 수정된 파일

### 1. LevelDefinitions.kt
**위치:** `app/src/main/java/kr/sweetapps/alcoholictimer/ui/tab_03/components/LevelDefinitions.kt`

**변경 내용:**
```kotlin
// [NEW] 11단계 레벨 시스템 (Lv.1 ~ Legend)
val levels = listOf(
    LevelInfo(R.string.level_0, 1, 3, Color(0xFFE53935)),         // Lv.1
    LevelInfo(R.string.level_1, 4, 7, Color(0xFFFF6F00)),         // Lv.2
    LevelInfo(R.string.level_2, 8, 14, Color(0xFFFFA726)),        // Lv.3
    LevelInfo(R.string.level_3, 15, 21, Color(0xFFFFCA28)),       // Lv.4
    LevelInfo(R.string.level_4, 22, 30, Color(0xFF9CCC65)),       // Lv.5
    LevelInfo(R.string.level_5, 31, 60, Color(0xFF66BB6A)),       // Lv.6
    LevelInfo(R.string.level_6, 61, 99, Color(0xFF42A5F5)),       // Lv.7
    LevelInfo(R.string.level_7, 100, 179, Color(0xFF1E88E5)),     // Lv.8
    LevelInfo(R.string.level_8, 180, 299, Color(0xFF5E35B1)),     // Lv.9
    LevelInfo(R.string.level_9, 300, 364, Color(0xFF8E24AA)),     // Lv.10
    LevelInfo(R.string.level_10, 365, Int.MAX_VALUE, Color(0xFFFFD700)) // Legend
)
```

### 2. Constants.kt
**위치:** `app/src/main/java/kr/sweetapps/alcoholictimer/constants/Constants.kt`

**변경 내용:**
```kotlin
// [FIX] 레벨 계산: 1일 차부터 시작 (시작일 = 1일)
fun calculateLevelDays(elapsedTimeMillis: Long): Int {
    val days = (elapsedTimeMillis / DAY_IN_MILLIS).toInt()
    return if (days == 0) 1 else days + 1
}
```

**로직 설명:**
- 0일 경과 → 1일 차 (시작일)
- 1일 경과 → 2일 차
- 2일 경과 → 3일 차
- ...

### 3. RunScreen.kt
**위치:** `app/src/main/java/kr/sweetapps/alcoholictimer/ui/tab_01/screens/RunScreen.kt`

**변경 내용:**
```kotlin
// [FIX] 레벨 계산: 1일 차부터 시작
val levelDays = remember(elapsedMillis, dayInMillis) { 
    val days = (elapsedMillis / dayInMillis).toInt()
    if (days == 0) 1 else days + 1
}
```

### 4. strings.xml (다국어 지원)

#### 한국어 (values/strings.xml)
```xml
<string name="level_0">알코올 스톱</string>           <!-- Lv.1 -->
<string name="level_1">3일 컷 통과</string>           <!-- Lv.2 -->
<string name="level_2">1주 클리어</string>            <!-- Lv.3 -->
<string name="level_3">피부의 변화</string>           <!-- Lv.4 -->
<string name="level_4">습관 형성 (21일)</string>      <!-- Lv.5 -->
<string name="level_5">한달의 기적</string>           <!-- Lv.6 -->
<string name="level_6">달라진 핏(Fit)</string>        <!-- Lv.7 -->
<string name="level_7">100일, 프로 금주러</string>    <!-- Lv.8 -->
<string name="level_8">플러스 통장</string>           <!-- Lv.9 -->
<string name="level_9">금주 마스터</string>           <!-- Lv.10 -->
<string name="level_10">전설의 레전드</string>        <!-- Legend -->
```

#### 영어 (values-en/strings.xml)
```xml
<string name="level_0">Alcohol Stop</string>
<string name="level_1">3-Day Cut Passed</string>
<string name="level_2">1-Week Clear</string>
<string name="level_3">Skin\'s Transformation</string>
<string name="level_4">Habit Formed (21 Days)</string>
<string name="level_5">One-Month Miracle</string>
<string name="level_6">Changed Fit</string>
<string name="level_7">100 Days, Pro Soberer</string>
<string name="level_8">Plus Balance</string>
<string name="level_9">Sobriety Master</string>
<string name="level_10">Legendary Legend</string>
```

#### 일본어 (values-ja/strings.xml)
```xml
<string name="level_0">アルコールストップ</string>
<string name="level_1">3日カット通過</string>
<string name="level_2">1週間クリア</string>
<string name="level_3">肌の変化</string>
<string name="level_4">習慣形成（21日）</string>
<string name="level_5">1ヶ月の奇跡</string>
<string name="level_6">変わったフィット</string>
<string name="level_7">100日、プロ禁酒者</string>
<string name="level_8">プラス通帳</string>
<string name="level_9">禁酒マスター</string>
<string name="level_10">伝説中の伝説</string>
```

#### 중국어 (values-zh-rCN/strings.xml)
```xml
<string name="level_0">戒酒开始</string>
<string name="level_1">3天突破</string>
<string name="level_2">一周通关</string>
<string name="level_3">皮肤变化</string>
<string name="level_4">习惯养成（21天）</string>
<string name="level_5">一月奇迹</string>
<string name="level_6">改变的体型</string>
<string name="level_7">100天，专业戒酒者</string>
<string name="level_8">存款增加</string>
<string name="level_9">戒酒大师</string>
<string name="level_10">传说中的传奇</string>
```

## 🧪 테스트 시나리오

### 시나리오 1: 시작일 (1일 차)
- **입력:** `elapsedTimeMillis = 0L` (시작한 지 0일 경과)
- **기대 결과:** `levelDays = 1`, 레벨 = "알코올 스톱" (Lv.1)

### 시나리오 2: 3일 차
- **입력:** `elapsedTimeMillis = 2 * DAY_IN_MILLIS` (2일 경과)
- **기대 결과:** `levelDays = 3`, 레벨 = "알코올 스톱" (Lv.1)

### 시나리오 3: 4일 차 (레벨 업)
- **입력:** `elapsedTimeMillis = 3 * DAY_IN_MILLIS` (3일 경과)
- **기대 결과:** `levelDays = 4`, 레벨 = "3일 컷 통과" (Lv.2)

### 시나리오 4: 100일 차 (Lv.8)
- **입력:** `elapsedTimeMillis = 99 * DAY_IN_MILLIS` (99일 경과)
- **기대 결과:** `levelDays = 100`, 레벨 = "100일, 프로 금주러" (Lv.8)

### 시나리오 5: 1년 경과 (Legend)
- **입력:** `elapsedTimeMillis = 365 * DAY_IN_MILLIS` (365일 경과)
- **기대 결과:** `levelDays = 366`, 레벨 = "전설의 레전드" (Legend)

## 📐 날짜 계산 로직 비교

### 기존 로직 (0일 기준)
```kotlin
val levelDays = (elapsedMillis / dayInMillis).toInt()
// 0일 경과 → 0일 → Lv.1 (0~6일)
// 1일 경과 → 1일 → Lv.1 (0~6일)
// 7일 경과 → 7일 → Lv.2 (7~13일)
```

### 신규 로직 (1일 차 기준)
```kotlin
val days = (elapsedMillis / dayInMillis).toInt()
val levelDays = if (days == 0) 1 else days + 1
// 0일 경과 → 1일 차 → Lv.1 (1~3일)
// 1일 경과 → 2일 차 → Lv.1 (1~3일)
// 3일 경과 → 4일 차 → Lv.2 (4~7일)
```

## ✅ 검증 결과

### 빌드 상태
```bash
BUILD SUCCESSFUL in 18s
43 actionable tasks: 29 executed, 14 from cache
```

### 적용된 화면
1. ✅ **RunScreen** - 타이머 실행 중 레벨 표시
2. ✅ **LevelScreen** - 레벨 상세 화면
3. ✅ **Tab03 (통계 탭)** - 레벨 뱃지 표시

### 다국어 지원
- ✅ 한국어 (ko)
- ✅ 영어 (en)
- ✅ 일본어 (ja)
- ✅ 중국어 간체 (zh-rCN)

## 🎨 UI 변경 사항

### 색상 팔레트
1. **초반 (위험 구간):** 빨강 → 주황 계열
2. **중반 (안정화 구간):** 노랑 → 초록 계열
3. **후반 (숙련 구간):** 하늘 → 파랑 계열
4. **마스터 구간:** 보라 계열
5. **레전드:** 금색 (특별 색상)

### 레벨 뱃지
- 각 레벨별로 고유한 색상 그라데이션 적용
- Legend 등급은 금색으로 특별 표시
- 레벨 번호는 "Lv.1" ~ "Lv.10", "Legend" 형식으로 표시

## 💡 기획 의도

### 1. 초반 동기 부여 강화
- 기존: 0~6일 (1주일 동안 Lv.1)
- 변경: 1~3일 (3일 만에 Lv.2 도달)
→ 빠른 성취감 제공

### 2. 명확한 마일스톤
- 3일, 1주, 3주(21일), 1개월, 100일 등 심리적 의미가 있는 날짜 기준 적용

### 3. 장기 목표 제시
- Legend 등급 추가로 1년 이상 유지한 사용자에게 특별한 보상 제공

## 📝 향후 개선 방향

### 1. 레벨별 보상 시스템
- 각 레벨 달성 시 특별 뱃지, 칭호, 테마 등 보상 제공

### 2. 레벨 상세 정보 확대
- 각 레벨별 건강 개선 효과, 절약 금액 통계 등 추가

### 3. 소셜 기능
- 레벨 달성 시 공유 기능
- 친구들과 레벨 비교 기능

---

**개편 완료 날짜:** 2025-12-03  
**담당자:** AI Development Assistant  
**영향 범위:** 전체 레벨 시스템 (LevelDefinitions, RunScreen, LevelScreen, Tab03, 다국어 리소스)

