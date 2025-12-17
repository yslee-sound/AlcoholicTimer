# Tab 5 레이아웃 재구성 완료 보고서

**작업일**: 2025-12-17  
**목표**: 앱 평가하기 버튼을 아이콘 버튼으로 이동하고 레이아웃 재구성

---

## ✅ 작업 완료 내용

### 1. 앱 평가하기 버튼 위치 변경

**Before**:
```
프로필 영역
  ↓
[★ 앱 평가하기] (큰 파란색 버튼)
  ↓
[알림] [문의/제안] [추천앱] (아이콘 버튼 1줄)
```

**After**:
```
프로필 영역
  ↓
[✏️ 프로필 편집하기] (큰 파란색 버튼)
  ↓
[알림] [문의/제안] [★ 앱 평가하기] (1줄)
  ↓
[추천앱] [비움] [비움] (2줄)
```

---

## 📋 상세 변경사항

### 1. 프로필 편집하기 버튼 (신규)

**위치**: 프로필 영역 바로 아래 (기존 앱 평가하기 자리)

**변경 내용**:
```kotlin
// Before: 앱 평가하기 버튼
Icon(
    painter = painterResource(id = R.drawable.star),
    contentDescription = null,
    tint = Color(0xFFFBC02D)
)
Text(text = stringResource(R.string.tab05_rate_app))

// After: 프로필 편집하기 버튼
Icon(
    imageVector = Icons.Default.Edit, // 연필 아이콘
    contentDescription = null,
    tint = Color.White
)
Text(text = "프로필 편집하기")
```

**동작**:
- 클릭 시 프로필 편집 화면으로 이동
- `onNavigateEditNickname()` 호출

---

### 2. 아이콘 버튼 1줄 (알림, 문의/제안, 앱 평가하기)

**배치**:
```
┌─────────────────────────────────────┐
│ [🔔 알림] [🎧 문의/제안] [⭐ 앱 평가하기] │
└─────────────────────────────────────┘
```

**앱 평가하기 버튼 (신규)**:
```kotlin
Column(
    modifier = Modifier
        .weight(1f)
        .clickable {
            // 플레이스토어로 이동
            val packageName = "kr.sweetapps.alcoholictimer"
            val intent = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
            context.startActivity(intent)
        }
) {
    Icon(
        painter = painterResource(id = R.drawable.star),
        contentDescription = null,
        tint = Color(0xFFFBC02D), // 노란색 별
        modifier = Modifier.size(32.dp)
    )
    AutoResizingTextLabel(
        text = stringResource(R.string.tab05_rate_app),
        fontSize = 12.sp
    )
}
```

**특징**:
- ⭐ 별 아이콘 사용 (기존 앱 평가하기 버튼의 아이콘)
- 노란색 별 (Color(0xFFFBC02D))
- 클릭 시 플레이스토어로 이동

---

### 3. 아이콘 버튼 2줄 (추천앱, 비움, 비움)

**배치**:
```
┌─────────────────────────────────────┐
│ [👍 추천앱] [    ] [    ]            │
└─────────────────────────────────────┘
```

**코드**:
```kotlin
Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp),
    horizontalArrangement = Arrangement.SpaceEvenly
) {
    // 1. 추천앱
    Column(
        modifier = Modifier.weight(1f)
    ) {
        Icon(painterResource(id = R.drawable.thumbsup))
        AutoResizingTextLabel(text = stringResource(R.string.tab05_recommended_apps))
    }

    // 2. 비움
    Spacer(modifier = Modifier.weight(1f))

    // 3. 비움
    Spacer(modifier = Modifier.weight(1f))
}
```

**특징**:
- 추천앱만 왼쪽에 배치
- 나머지 2개는 빈 공간으로 유지
- 향후 새로운 메뉴 추가 가능

---

## 🎨 UI 레이아웃 비교

### Before

```
┌─────────────────────────────────────┐
│                                     │
│  👤 알중이 >                        │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  ⭐ 앱 평가하기              │   │ ← 큰 버튼
│  └─────────────────────────────┘   │
│                                     │
│  [🔔 알림] [🎧 문의] [👍 추천앱]    │ ← 1줄
│                                     │
└─────────────────────────────────────┘
```

### After

```
┌─────────────────────────────────────┐
│                                     │
│  👤 알중이 >                        │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  ✏️ 프로필 편집하기          │   │ ← 큰 버튼 (변경)
│  └─────────────────────────────┘   │
│                                     │
│  [🔔 알림] [🎧 문의] [⭐ 평가]      │ ← 1줄 (추가)
│                                     │
│  [👍 추천앱] [    ] [    ]          │ ← 2줄 (신규)
│                                     │
└─────────────────────────────────────┘
```

---

## 🔧 기술적 세부사항

### 1. 아이콘 사용

**연필 아이콘**:
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit

Icon(
    imageVector = Icons.Default.Edit,
    contentDescription = null,
    tint = Color.White,
    modifier = Modifier.size(24.dp)
)
```

**별 아이콘**:
```kotlin
Icon(
    painter = painterResource(id = R.drawable.star),
    contentDescription = null,
    tint = Color(0xFFFBC02D), // 노란색
    modifier = Modifier.size(32.dp)
)
```

---

### 2. 레이아웃 구조

**2줄 아이콘 버튼 구조**:
```kotlin
// 1줄
Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
    horizontalArrangement = Arrangement.SpaceEvenly
) {
    Column(modifier = Modifier.weight(1f)) { /* 알림 */ }
    Column(modifier = Modifier.weight(1f)) { /* 문의/제안 */ }
    Column(modifier = Modifier.weight(1f)) { /* 앱 평가하기 */ }
}

Spacer(modifier = Modifier.height(14.dp))

// 2줄
Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
    horizontalArrangement = Arrangement.SpaceEvenly
) {
    Column(modifier = Modifier.weight(1f)) { /* 추천앱 */ }
    Spacer(modifier = Modifier.weight(1f)) { /* 비움 */ }
    Spacer(modifier = Modifier.weight(1f)) { /* 비움 */ }
}
```

---

### 3. 프로필 편집 화면 연결

**네비게이션**:
```kotlin
Box(
    modifier = Modifier
        .clickable {
            onNavigateEditNickname() // 프로필 편집 화면으로 이동
        }
) {
    Row {
        Icon(imageVector = Icons.Default.Edit)
        Text(text = "프로필 편집하기")
    }
}
```

**기존 연결**:
- 프로필 영역 클릭 → 프로필 편집 화면
- 프로필 편집하기 버튼 클릭 → 프로필 편집 화면
- 두 개의 진입점 제공

---

## 📊 변경 요약

### 버튼 위치 변경

| 버튼 | Before | After |
|------|--------|-------|
| **앱 평가하기** | 큰 파란색 버튼 | 1줄 3번째 아이콘 버튼 |
| **프로필 편집하기** | (없음) | 큰 파란색 버튼 (신규) |
| **추천앱** | 1줄 3번째 | 2줄 1번째 |

---

### 아이콘 버튼 배치

**1줄 (3개)**:
1. 🔔 알림 (유지)
2. 🎧 문의/제안 (유지)
3. ⭐ 앱 평가하기 (신규)

**2줄 (1개)**:
1. 👍 추천앱 (이동)
2. 비움
3. 비움

---

## 🎯 사용자 경험 개선

### 1. 프로필 편집 접근성 향상

**Before**:
- 프로필 영역만 클릭 가능
- 작은 영역

**After**:
- 프로필 영역 클릭 ✅
- 큰 파란색 버튼 클릭 ✅
- 2가지 진입점

---

### 2. 앱 평가하기 접근성

**Before**:
- 큰 파란색 버튼 (눈에 띔)
- 상단 배치

**After**:
- 아이콘 버튼 (적절한 크기)
- 다른 기능들과 같은 줄
- 여전히 접근 가능

---

### 3. 향후 확장성

**빈 공간 활용**:
```
[추천앱] [새 기능 1] [새 기능 2]
```

향후 새로운 메뉴를 쉽게 추가 가능

---

## ✅ 테스트 체크리스트

### 프로필 편집하기 버튼

- [ ] 버튼 표시 확인
- [ ] 연필 아이콘 표시 확인
- [ ] 클릭 시 프로필 편집 화면으로 이동
- [ ] 흰색 텍스트 확인
- [ ] 파란색 배경 확인

---

### 앱 평가하기 버튼

- [ ] 1줄 3번째 위치 확인
- [ ] 별 아이콘 표시 확인
- [ ] 노란색 별 확인
- [ ] 클릭 시 플레이스토어 이동
- [ ] 텍스트 자동 크기 조정 확인

---

### 추천앱 버튼

- [ ] 2줄 1번째 위치로 이동 확인
- [ ] 아이콘 표시 확인
- [ ] 회색 색상 확인 (비활성 표시)

---

### 레이아웃

- [ ] 버튼 간격 균등 확인
- [ ] 아이콘 크기 32dp 확인
- [ ] 텍스트 크기 12.sp 확인
- [ ] 2줄 구조 확인
- [ ] 빈 공간 올바르게 배치 확인

---

## 🎉 완료!

**변경 파일**: 
- ✅ `app/src/main/java/kr/sweetapps/alcoholictimer/ui/tab_05/Tab05.kt`

**주요 변경사항**:
1. ✅ 앱 평가하기 → 아이콘 버튼 (1줄 3번째)
2. ✅ 프로필 편집하기 → 큰 파란색 버튼 (신규)
3. ✅ 추천앱 → 2줄로 이동
4. ✅ 2줄 레이아웃 추가 (빈 공간 포함)

**빌드 상태**: 진행 중  
**예상 결과**: UI 레이아웃 재구성 완료 ✅

---

**작성일**: 2025-12-17  
**작성자**: GitHub Copilot  
**버전**: Tab05 레이아웃 v2.0

