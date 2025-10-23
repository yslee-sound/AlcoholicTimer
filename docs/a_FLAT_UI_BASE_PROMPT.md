> 문서 버전
> - 버전: v1.1.1
> - 최근 업데이트: 2025-10-23
> - 변경 요약: 전역 일반 컨테이너(헤어라인 0.5dp + 0dp 고도) 규칙·예시 추가, Compose BorderStroke import 모호성 회피 가이드 추가, About/Records/Run 화면 적용 원칙 명시 보강.
>
> 변경 이력(Changelog)
> - v1.1.1 (2025-10-23)
>   - “일반 컨테이너 정의(헤어라인 + 0dp)와 사용 규칙” 섹션 추가
>   - Compose BorderStroke 모호성(import 충돌) 회피 가이드 추가(별칭 import 권장)
>   - AboutLicenses/Records/Run 화면의 카드·컨테이너에 헤어라인 표준 적용 명시 강화
> - v1.1.0 (2025-10-23)
>   - 레벨 화면 메인 카드(상단 "현재 레벨" 카드) 디테일 보존을 위한 불변 규칙 추가
>   - 허용/금지 변경 범위 명시, 회귀 방지 체크리스트와 수용 기준 보강
>   - 개발 가이드에 외곽(Frame)만 교체하는 패턴 권장
>   - 세만틱스 기반 회귀 테스트 도입(라이트 모드 우선) 및 관련 문서 링크 추가
> - v1.0.0 (2025-10-21)
>   - 초기 작성: 디자인 토큰(AppElevation/AppBorder/color_border_light) 정의
>   - AppCard 기본값(0dp + Hairline) 제안 및 화면별 적용 가이드 수록
>   - 수용 기준/리스크/검증 체크리스트 추가
>
> 버전 규칙
> - Semantic Versioning 준수: MAJOR(호환성 깨짐)/MINOR(가이드·정책 추가)/PATCH(오타·경미한 정정)
> - 문서 갱신 시 상단 버전/날짜/요약, 하단 변경 이력을 함께 갱신합니다.
>
> // filepath: g:\Workspace\NoSmokeTimer\docs\a_FLAT_UI_BASE_PROMPT.md
> 플랫(Flat) UI 전역 스타일 적용 프롬프트
>
> 목표
> - 반사광/그림자보다 얇은 헤어라인 테두리와 0dp 고도의 평면 카드(UI 컨테이너)로 전 앱 화면을 일관되게 정리한다.
> - 기존 배경(MaterialTheme.colorScheme.background 등)과 자연스럽게 어울리는, 보이는 듯-안 보이는 듯한 경계감을 만든다.
> - 강조가 필요한 원형 주요 버튼만 2dp 고도를 유지한다.
>
> 역할 지시(Role)
> 당신은 Android Jetpack Compose UI 리팩터링 전문가다. 아래 요구사항을 만족하도록 디자인 토큰을 정의/갱신하고, 공통 카드(AppCard)와 화면별 UI에 플랫한 외곽 스타일을 전역 적용하라. 변경 후 빌드와 기본 테스트가 통과하고, 시각 확인 체크리스트를 만족해야 한다.
>
> 적용 범위(Scope)
> - 디자인 토큰 추가/갱신
> - 공통 카드(AppCard) 기본값 변경
> - 화면별 카드/컨테이너 외곽 스타일 통일
> - 다크 모드 대비/톤 가이드 연결
> - 품질 게이트(빌드/테스트/시각 확인) 통과
>
> 디자인 토큰(필수)
> - Elevation
>   - AppElevation.CARD = 0.dp  // 기본 카드·컨테이너는 평면
>   - AppElevation.CARD_HIGH = 2.dp  // 주목도 있는 원형 주요 버튼(시작/중지 등)
> - Border
>   - AppBorder.Hairline = 0.5.dp  // 전역 테두리 두께 표준
> - Color
>   - color_border_light = #EEF1F5  // 은은한 라이트 테두리(톤 업)
>   - 다크 모드: outlineVariant 또는 적절한 low-contrast outline 계열에 매핑
>
> 공통 카드(AppCard) 기본값(필수)
> - elevation = AppElevation.CARD (0.dp)
> - border = BorderStroke(AppBorder.Hairline, colorResource(R.color.color_border_light))
> - shape는 제품 컨벤션 유지(예: 12~20.dp 범위에서 제품 표준값 사용)
> - ripple/interaction은 기존 정책 유지
>
> 새 섹션: 일반 컨테이너 정의(헤어라인 + 0dp)와 사용 규칙
> - 정의: 화면 내 “카드/정보 컨테이너/섹션 박스”의 기본 외곽은 다음을 사용한다.
>   - elevation = AppElevation.CARD (0dp)
>   - border = BorderStroke(AppBorder.Hairline, colorResource(R.color.color_border_light))
> - 예외 허용: 정보 밀도가 높아 경계가 과도해 보일 때는 특정 컴포넌트에서 border = null로 완전 플랫 허용(문맥적 판단). 다만 리스트 전체가 흐려지지 않도록 섹션 헤더 카드는 헤어라인 유지 권장.
> - 적용 범위: Start/Run/Quit, Records(요약/리스트 카드/필터 박스), Detail, AboutLicenses 등 전 화면 공통. 레벨 화면 메인 카드는 “외곽만 교체” 원칙을 따른다.
> - Compose 예시
>   - 직접 Card 사용:
>     - elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD)
>     - border = BorderStroke(AppBorder.Hairline, colorResource(id = R.color.color_border_light))
>   - 공통 AppCard 사용 시 기본값으로 충족(별도 지정 불필요)
> - Import 가이드(모호성 회피)
>   - BorderStroke가 foundation/material3 양쪽에 존재해 충돌이 날 수 있다.
>   - 권장: foundation BorderStroke를 별칭으로 임포트하거나(예: `import androidx.compose.foundation.BorderStroke as FBorderStroke`), material3 Card의 border 인자에는 `FBorderStroke(...)`를 전달한다.
>   - 팀 규칙: 파일 내 BorderStroke가 2회 이상 임포트되지 않도록 Lint 체크(미정) 또는 리뷰로 확인.
>
> 화면별 적용 가이드
> - 레벨(Level)
>   - 상단 “현재 레벨” 카드: 헤어라인 테두리 + 평면(0dp) 적용
>   - “전체 레벨” 리스트: 회색 테두리·그림자 제거(배경만 플랫); 현재/달성 항목만 색 테두리 1dp 유지
> - 시작(Start) / 실행(Run) / 중지(Quit)
>   - 화면 내 카드·컨테이너: 헤어라인 테두리 + 0dp
>   - 원형 큰 시작/중지 버튼: 2dp 고도 유지(AppElevation.CARD_HIGH)
> - 기록(RecordsScreen / RecordSummary / PeriodSelection)
>   - 모든 카드·셀: 헤어라인 테두리 + 0dp로 통일
> - 상세(Detail)
>   - 컨테이너/카드: 헤어라인 테두리 + 0dp
> - 라이선스(AboutLicenses)
>   - 항목/카드: 헤어라인 테두리 + 0dp
>
> 중요: 레벨 화면 메인 카드 불변 규칙(디테일 보존)
> - 목적: 플랫 적용 시 “외곽(컨테이너)”만 바꾸고, “내부 콘텐츠”의 시각적 디테일은 절대 바꾸지 않는다.
> - 허용 변경(외곽만):
>   - 컨테이너 elevation = 0.dp, border = Hairline(0.5.dp, color_border_light)
>   - 컨테이너 그림자/반사광 제거, 외곽 그림자/윤곽선 톤만 조정
> - 금지 변경(내부 디테일):
>   - 텍스트 계층: 폰트 패밀리/스타일/크기/라인하이트/자간/강조색 변경 금지(타이포 토큰 Override 금지)
>   - 레이아웃: 내부 padding/spacing/정렬/비율 변경 금지(예: 상단 배지, 제목, 보조 텍스트, 진행바의 간격/정렬 불변)
>   - 모양: 카드 shape(코너 반경) 및 내부 배지/프로그레스의 모서리 반경 변경 금지
>   - 그래픽: 상단 원형 배지/그라데이션/아이콘 스타일 변경 금지
>   - 컴포넌트: 진행바 높이/라운드/색상/애니메이션 변경 금지
>   - 색상 토큰: 내부 텍스트/아이콘/상태색(예: 강조 숫자) 교체 금지; 외곽 border만 color_border_light 사용
>   - 상호작용: 내부 요소에 불필요한 elevation/shadow/ripple 변화 추가 금지
> - 구현 가드(권장 패턴):
>   - 메인 카드의 "콘텐츠(기존 컴포저블)"는 그대로 두고, 그 바깥을 감싸는 Frame만 AppCard로 교체한다.
>   - AppCard 사용 시 contentPadding을 추가하지 말고, 내부 컴포저블의 padding/레이아웃 로직을 그대로 유지한다.
>
> 스타일 원칙
> - 배경과의 관계: 기존 배경과 어울리도록 경계 대비를 낮춘다(연한 color_border_light).
> - 그림자/반사광 축소: 불필요한 elevation/shadow 제거(0dp 기본). 단, 핵심 CTA 원형 버튼은 2dp 유지.
> - 색상 포커스: 정보 상태 강조가 필요한 곳(예: 레벨의 현재/달성 항목)은 색 테두리 1dp를 유지한다.
>
> 다크 모드 가이드(권장)
> - color_border_light는 outlineVariant 또는 유사 저대비 아웃라인에 매핑하여 야간 대비 최적화
> - 필요 시 라이트/다크 팔레트 각각에 border 색을 정의하고, alpha 조정으로 존재감 최소화
>
> 디자인 토글 포인트(미세 조정)
> - 테두리 진하기: color_border_light를 #F2F4F7(더 흐림) ~ #E5E8EC(살짝 진함) 범위로 조정
> - 테두리 두께: AppBorder.Hairline 0.5dp → 0.75dp 또는 1dp
> - 완전 플랫: 특정 카드에선 border = null로 경계 제거
> - 모서리 반경: 제품 표준에 맞춰 12~20.dp로 통일 조정 가능
>
> 개발 가이드(컴포넌트 사용)
> - 기본: AppCard를 사용하면 플랫 스타일이 기본 적용된다.
> - 직접 Card 사용 시
>   - elevation = AppElevation.CARD
>   - border = BorderStroke(AppBorder.Hairline, colorResource(R.color.color_border_light))
> - 강조 버튼/플로팅 액션: elevation = AppElevation.CARD_HIGH
> - 메인 카드 Frame 예시(개념):
>   - MainLevelCardFrame(modifier) { ExistingMainLevelCardContent() } // Frame만 교체, 콘텐츠는 변경 금지
>
> 권장 구현 순서(Checklist)
> 1) 디자인 토큰 추가/갱신
>    - AppElevation, AppBorder, color_border_light 정의
>    - 다크 모드 매핑(outlineVariant 등) 준비
> 2) 공통 컴포넌트 업데이트
>    - AppCard 기본값: elevation 0dp + Hairline Border
>    - 원형 주요 버튼: elevation 2dp 유지 확인
> 3) 화면 적용
>    - 레벨 화면: 상단 카드 플랫/헤어라인, 전체 레벨 리스트 경계·그림자 제거, 현재/달성 색 테두리 1dp 유지
>    - Start/Run/Quit, Records, Detail, AboutLicenses: 전부 헤어라인 + 0dp 통일
> 4) 코드 정리
>    - 중복 elevation/shadow 제거
>    - 임시 색상/하드코딩 값 토큰으로 치환
> 5) 빌드/테스트
>    - assembleDebug 성공(릴리스 키스토어 경고는 무시 가능)
>    - JVM 단위 테스트 통과 확인
> 6) 시각 확인
>    - 레벨/기록/설정 주요 화면에서 카드 외곽이 얇고 은은하게만 보이는지
>    - “전체 레벨” 박스가 회색 경계·그림자 없이 평면 컨테이너로 보이는지
>    - 원형 시작/중지 버튼은 2dp 고도 유지로 주목도 확보되는지
>
> 회귀 방지 체크리스트(레벨 메인 카드)
> - [ ] 외곽만 변경되었는가? (elevation=0dp, Hairline border) – 내부 텍스트/간격/색상/배지/진행바는 픽셀 수준 동일
> - [ ] 스크린샷 비교(전/후): 타이포/간격/정렬, 진행바 높이·모서리 반경·색상 변화 없음
> - [ ] 코드 리뷰: AppCard 적용이 콘텐츠 내부 padding/shape/타이포 토큰을 건드리지 않음
> - [ ] 다크 모드에서도 내부 색상 토큰과 대비가 기존과 동일하게 유지됨
>
> 수용 기준(Acceptance Criteria)
> - 전역 카드/컨테이너가 elevation 0dp, Hairline 테두리로 통일됨
> - 레벨 리스트의 현재/달성 항목은 색 테두리 1dp가 유지됨(의도된 예외)
> - 원형 주요 버튼은 elevation 2dp 유지됨
> - (신규) BorderStroke import 모호성 없이 빌드가 통과함(중복 import 금지)
> - 레벨 화면 메인 카드 내부 디테일(타이포·간격·배지·진행바·색상 토큰)이 기존과 동일하며, 외곽만 변경됨
> - 빌드와 JVM 테스트 통과
>
> 리스크/엣지 케이스
> - 너무 옅은 경계: 배경과의 대비 부족으로 컨테이너 구분이 어려울 수 있음 → color_border_light 미세 조정
> - 밀도 높은 리스트: 테두리가 많아 보일 수 있음 → 카드 간 간격을 약간 확대(예: 16dp → 20dp)
> - 터치 영역/리플: elevation 제거 후 상호작용 피드백 약화 체감 가능 → ripple 색/alpha 점검
> - 레벨 메인 카드 내부 디테일 변경 위험: Frame 적용 시 contentPadding/shape/타이포 토큰이 간접 변경되지 않도록 주의
>
> 간단 코드 예시(Compose, 개념 참고)
> - Border/Color 토큰
>   - val AppBorder = object { val Hairline = 0.5.dp }
>   - val AppElevation = object { val CARD = 0.dp; val CARD_HIGH = 2.dp }
>   - color_border_light = #EEF1F5 (라이트), 다크는 outlineVariant 매핑
> - AppCard 기본값
>   - AppCard(
>       elevation = AppElevation.CARD,
>       border = BorderStroke(AppBorder.Hairline, colorResource(R.color.color_border_light)),
>       shape = AppShapes.Medium
>     ) { /* content */ }
> - 메인 카드 Frame 패턴(개념):
>   - @Composable fun MainLevelCardFrame(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
>       // ...existing code...
>       AppCard(modifier = modifier, /* 외곽만 */) { content() } // 내부 콘텐츠는 변경 금지
>     }
>
> 검증 방법(요약)
> - 디바이스/에뮬레이터로 주요 화면 확인
> - 특히 “전체 레벨” 박스가 회색 테두리/그림자 없이 평면 컨테이너로 보이는지, 원형 시작/중지 버튼이 2dp 고도로 떠 보이는지 확인
> - 레벨 화면 메인 카드: 전/후 스크린샷 비교로 내부 디테일 불변 확인(자동/수동 모두 허용)
>
> 세만틱스 회귀 테스트(라이트 모드 우선)
> - 목적: 스냅샷 인프라(Paparazzi/Roborazzi) 전 구성 단계에서, 내부 디테일 불변을 기계적으로 검증
> - 대상/파일
>   - 레벨 메인 카드: `app/src/test/java/com/example/alcoholictimer/feature/level/CurrentLevelCardSemanticsTest.kt`
>   - 상세 가이드: `docs/LEVEL_MAIN_CARD_STABILITY.md`
> - 테스트 태그(필수 유지)
>   - `main_level_card_content`, `main_level_badge`, `main_level_title`
>   - `main_level_days_row`, `main_level_days_value`, `main_level_days_label`
>   - `main_level_progress`, `main_level_progress_fill`
> - 환경 고정/안정화
>   - 테마: `AlcoholicTimerTheme(darkTheme = false)`
>   - 깜빡임 방지: 테스트 경로에서 `startTime = 0L`
>   - Robolectric NPE 방지: `ShadowBuild.setFingerprint("robolectric")`
> - 실행(Windows cmd)
>   ```bat
>   .\gradlew.bat :app:testDebugUnitTest
>   .\gradlew.bat :app:testDebugUnitTest --tests "*CurrentLevelCardSemanticsTest"
>   ```
>
> 관련 구현(참고)
> - 외곽 전용 프레임: `MainLevelCardFrame` (`app/src/main/java/com/example/alcoholictimer/core/ui/components/MainLevelCardFrame.kt`)
>   - elevation = `AppElevation.CARD`(0dp), border = `BorderStroke(AppBorder.Hairline, color_border_light)`, contentPadding=0dp
> - 공통 카드 기본값: `AppCard` (`app/src/main/java/com/example/alcoholictimer/core/ui/AppCard.kt`)
>   - 기본 elevation=0dp, 기본 border=Hairline(0.5dp)
> - 디자인 토큰: `DesignTokens.kt`
>   - `AppElevation.CARD = 0.dp`, `AppElevation.CARD_HIGH = 2.dp`, `AppBorder.Hairline = 0.5.dp`
