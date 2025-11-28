> 문서 버전
> - 버전: v1.1.0
> - 최근 업데이트: 2025-10-24
> - 변경 요약: 설정 화면을 카드형 → 목록형(비스크롤 기본)으로 개편. 흰색 배경, 섹션 구분선, 컴팩트 라디오(40dp), 배너 위 콘텐츠 8dp 완충, 전역 배너 갭 0dp 정책 반영.
>
> 변경 이력(Changelog)
> - v1.1.0 (2025-10-24)
>   - 레이아웃: 카드형 UI를 제거하고 목록형(섹션 헤더 + 컴팩트 라디오 리스트)으로 전환
>   - 스타일: 화면 바탕 흰색(White), 섹션 간 구분선(HorizontalDivider) 추가
>   - 접근성/크기: 옵션 최소 높이 40dp(Compact), 그룹 간 간격 8dp로 정렬
>   - 광고 정책 연동: 전역 배너 위 간격 0dp, 화면 콘텐츠에서만 배너 위 8dp 완충(.padding(bottom = 8.dp))
>   - 스크롤 정책: 기본 비스크롤. 아주 작은 화면/대폰트에서 넘칠 때만 조건부 스크롤 허용(선택)
> - v1.0.0 (2025-10-21)
>   - 초기 작성: 카드형 템플릿/스펙, 예시 구현, 체크리스트/포팅 가이드
>
> 버전 규칙
> - Semantic Versioning 준수: MAJOR(호환성 깨짐)/MINOR(가이드·정책 추가)/PATCH(오타·경미한 정정)
> - 문서 갱신 시 상단 버전/날짜/요약, 하단 변경 이력을 함께 갱신합니다.

# 설정 화면(UI) 복제 프롬프트 템플릿 — 목록형(Compact, Non-Scrollable by default)

목적: 이 프롬프트를 복사해 다른 앱에서도 현재 앱의 “흰색 배경, 목록형, 섹션 구분선, 컴팩트 라디오, 배너 위 8dp 완충, 전역 배너 갭 0dp” 정책을 그대로 재현합니다.

---

## 프롬프트(그대로 복사해 사용)

너는 Jetpack Compose(Material 3)를 사용하는 Android UI 엔지니어다. 아래 “절대 준수사항”과 “디자인/레이아웃 스펙”을 만족하는 설정 화면(SettingsScreen)을 구현하라. 화면은 3개의 섹션(음주 비용/빈도/시간)으로 구성되며, 현재 앱과 시각적/행동적 일치가 목표다.

### 절대 준수사항(Non-negotiable)
- 배경/컨테이너: 화면 전체 바탕은 White로 고정한다.
- 스크롤 정책: 기본 비스크롤(Non-scroll). 콘텐츠가 실제로 뷰포트를 넘칠 때만 세로 스크롤을 허용한다.
- 배너 정책: 전역 배너 위 간격은 0dp. 대신 화면 콘텐츠 루트에 배너 위 최소 8dp 완충을 둔다.
- 섹션 구분: 섹션 사이에 1dp HorizontalDivider를 삽입한다(색: color_border_light 또는 outlineVariant).
- 여백/간격(Compact):
  - 화면 좌/우 여백 = H_PADDING(없으면 16.dp)
  - 화면 상단 8dp, 하단 8dp(배너 위 완충)
  - 섹션 간 간격 8dp
  - 그룹 내부 옵션 간 간격 4dp
- 옵션 아이템(Compact):
  - 최소 높이 40dp, Row 전체 클릭 가능(Role=RadioButton)
  - 라디오 색: selected = color_accent_blue, unselected = color_radio_unselected
  - 텍스트: bodyMedium, 선택 시 SemiBold + 강조 컬러(color_indicator_days), 비선택 시 본문 컬러(color_text_primary_dark)

### 디자인/레이아웃 스펙
- 컨테이너
  - Box(fillMaxSize).background(White)
  - 내부 Column(fillMaxSize)
  - Column Modifier: padding(start=H_PADDING, end=H_PADDING, top=8.dp) + padding(LocalSafeContentPadding.current) + padding(bottom=8.dp)
- 리스트(목록형)
  - 기본: Column(verticalArrangement = spacedBy(8.dp))
  - 섹션 사이에 HorizontalDivider(thickness=1.dp, color=outlineVariant 또는 color_border_light)
  - 작은 기기/큰 폰트로 넘칠 경우에만 옵션으로 verticalScroll + 오버스크롤 비활성(LocalOverscrollFactory=null)
- 섹션 헤더
  - Text(style = titleMedium + Bold, 색: 섹션별 포인트 컬러)
- 옵션 그룹
  - Column(verticalArrangement = spacedBy(4.dp))
- 옵션 아이템
  - Row(fillMaxWidth, heightIn(min=40.dp), clickable(role=RadioButton))
  - RadioButton + 텍스트(label)

### 토큰/리소스 매핑(다른 앱으로 포팅 시)
- H_PADDING -> 프로젝트 좌우 화면 기본 패딩 (권장 16.dp)
- 색 리소스
  - color_indicator_money / color_progress_primary / color_indicator_hours (섹션 타이틀 컬러)
  - color_border_light(구분선), color_accent_blue(선택 라디오), color_radio_unselected(비선택 라디오), color_indicator_days(선택 텍스트), color_text_primary_dark(비선택 텍스트)
- 전역 광고 정책 연동
  - BANNER_TOP_GAP = 0.dp (전역 갭 없음)
  - 배너 위 완충은 화면 콘텐츠에서만 .padding(bottom=8.dp)로 보장

---

## 예시 구현(가이드 코드)

```kotlin
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val (initialCost, initialFrequency, initialDuration) = Constants.getUserSettings(context)
    val sharedPref = context.getSharedPreferences(Constants.USER_SETTINGS_PREFS, Context.MODE_PRIVATE)

    var selectedCost by remember { mutableStateOf(initialCost) }
    var selectedFrequency by remember { mutableStateOf(initialFrequency) }
    var selectedDuration by remember { mutableStateOf(initialDuration) }

    val safePadding = LocalSafeContentPadding.current

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = H_PADDING, end = H_PADDING, top = 8.dp)
                .padding(safePadding)
                .padding(bottom = 8.dp), // 배너 위 최소 완충
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsSection(title = "음주 비용", titleColor = colorResource(id = R.color.color_indicator_money)) {
                SettingsOptionGroup(
                    selectedOption = selectedCost,
                    options = listOf("저", "중", "고"),
                    labels = listOf("저 (1만원 이하)", "중 (1~5만원)", "고 (5만원 이상)"),
                    onOptionSelected = {
                        selectedCost = it
                        sharedPref.edit { putString("selected_cost", it) }
                    }
                )
            }
            SectionDivider()

            SettingsSection(title = "음주 빈도", titleColor = colorResource(id = R.color.color_progress_primary)) {
                SettingsOptionGroup(
                    selectedOption = selectedFrequency,
                    options = listOf("주 1회 이하", "주 2~3회", "주 4회 이상"),
                    labels = listOf("주 1회 이하", "주 2~3회", "주 4회 이상"),
                    onOptionSelected = {
                        selectedFrequency = it
                        sharedPref.edit { putString("selected_frequency", it) }
                    }
                )
            }
            SectionDivider()

            SettingsSection(title = "음주 시간", titleColor = colorResource(id = R.color.color_indicator_hours)) {
                SettingsOptionGroup(
                    selectedOption = selectedDuration,
                    options = listOf("짧음", "보통", "길게"),
                    labels = listOf("짧음 (2시간 이하)", "보통 (3~5시간)", "길게 (6시간 이상)"),
                    onOptionSelected = {
                        selectedDuration = it
                        sharedPref.edit { putString("selected_duration", it) }
                    }
                )
            }
        }
    }
}

@Composable fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = colorResource(id = R.color.color_border_light)
    )
}

@Composable
fun SettingsSection(title: String, titleColor: Color, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = titleColor,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
        )
        content()
    }
}

@Composable
fun SettingsOptionGroup(
    selectedOption: String,
    options: List<String>,
    labels: List<String>,
    onOptionSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEachIndexed { index, option ->
            SettingsOptionItem(
                isSelected = selectedOption == option,
                label = labels[index],
                onSelected = { onOptionSelected(option) }
            )
        }
    }
}

@Composable
fun SettingsOptionItem(
    isSelected: Boolean,
    label: String,
    onSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .clickable(role = Role.RadioButton, onClick = onSelected)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = colorResource(id = R.color.color_accent_blue),
                unselectedColor = colorResource(id = R.color.color_radio_unselected)
            )
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = if (isSelected) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold) else MaterialTheme.typography.bodyMedium,
            color = if (isSelected) colorResource(id = R.color.color_indicator_days) else colorResource(id = R.color.color_text_primary_dark)
        )
    }
}
```

---

## 검수 체크리스트(복제 성공 여부)
- [ ] 배경이 흰색(White)이며, 앱바/Divider 외 영역은 모두 백색이다.
- [ ] 상단 8dp, 하단 8dp(배너 위 완충 8dp)가 콘텐츠 루트에 적용되어 있다.
- [ ] 섹션 간 간격 8dp, 섹션 사이 구분선(1dp)이 보인다.
- [ ] 옵션 아이템은 최소 40dp이며 Row 전체가 클릭 가능하다.
- [ ] 선택 상태에서 텍스트가 SemiBold와 강조 컬러로 노출된다.
- [ ] 일반 해상도/기본 폰트에서 스크롤이 발생하지 않는다(Overflow일 때만 조건부 스크롤 적용 필요).

---

## 포팅 가이드/주의사항
- 기존 카드형 템플릿을 사용하는 앱은 다음 순서로 마이그레이션
  1) 카드 컨테이너 제거 → 섹션 헤더 + 옵션 리스트로 치환
  2) 간격 축소: 그룹 간 8dp, 그룹 내 4dp, 옵션 40dp
  3) 배경 White 적용, 섹션 Divider 추가
  4) 전역 배너 갭 0dp 유지, 화면 콘텐츠에만 하단 8dp 완충 적용
  5) 작은 기기/큰 폰트 시에만 `verticalScroll` 조건부 활성화(없으면 비스크롤 유지)
- 색/타이포그래피 토큰이 없을 경우 MaterialTheme의 onSurface/outlineVariant/primary 등을 합리적으로 매핑
- 오버스크롤/인셋: BaseScreen의 LocalSafeContentPadding과 인셋 계산을 신뢰하고, 하단 패딩 중복을 만들지 않는다
