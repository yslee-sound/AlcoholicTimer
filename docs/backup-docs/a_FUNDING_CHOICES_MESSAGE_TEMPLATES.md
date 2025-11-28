# Funding Choices 메시지 공통 템플릿 (KR/EN)

본 문서는 AdMob > 개인 정보 보호 및 메시지에서 사용하는 3가지 메시지 유형(EU 규정, 미국 주 규정, iOS IDFA 설명)을 하나의 기준으로 관리하기 위한 공통 텍스트 템플릿입니다. 필요에 따라 앱과 브랜드 톤에 맞게 경어/어조만 조정하세요.

> 주의: 본 문서는 제품 가이드이며 법률 자문이 아닙니다. 서비스 특성에 따라 법무 검토가 필요할 수 있습니다.

---

## 0) 공통 안내(모든 메시지에 동일)

- 수집/이용 항목: 광고 식별자(AAID/IDFA), 기기/네트워크 정보(모델, OS/앱 버전, IP, 언어/국가), 앱 사용 이벤트, 광고 상호작용(노출/클릭/성과)
- 이용 목적: 광고 제공/측정, 성능 및 품질 개선, 부정행위 방지, 서비스 안전성
- 파트너: Google AdMob (How Google uses data: https://policies.google.com/technologies/partner-sites)
- 선택권: 맞춤형/비맞춤형 광고 선택, 동의 철회/변경은 앱 내 동의 메시지 또는 단말 설정(광고 맞춤설정 제한/ID 재설정)에서 가능
- 정책: Privacy Policy URL(고정 URL) 연결

KR 공통 문구(짧은 버전)
- “개인정보는 광고 제공·측정, 서비스 개선, 부정행위 방지에 사용됩니다. Google AdMob과 같은 파트너와 협력할 수 있습니다. 설정에서 언제든 선택을 변경할 수 있습니다.”

EN Common (short)
- “We use data for ad delivery/measurement, service improvement, and fraud prevention. We may work with partners like Google AdMob. You can change your choices anytime in settings.”

---

## 1) EU 규정(EU Consent) 템플릿

KR (권장 최대: 제목 50자, 본문 300자 내)
- 제목: “맞춤형 광고를 위한 동의가 필요합니다”
- 본문: “{{APP_NAME}}는 광고 식별자와 기기 정보 등을 이용해 광고를 제공·측정하고 서비스 품질을 개선합니다. Google AdMob 등 파트너와 협력할 수 있습니다. 맞춤형 광고를 원하지 않는 경우 비맞춤형 광고를 선택할 수 있습니다. 더 자세한 내용은 개인정보 처리방침을 확인하세요.”
- 버튼: [모두 허용] [비맞춤형 광고] [자세히 설정]

EN
- Title: “We request your consent for personalized ads”
- Body: “{{APP_NAME}} uses advertising identifiers and device info to deliver and measure ads and improve quality. We may work with partners like Google AdMob. If you prefer, you can choose non‑personalized ads. See our Privacy Policy for details.”
- Buttons: [Allow all] [Non‑personalized ads] [Manage options]

옵션 섹션(목적 설명 예시)
- 광고 저장소 접근/기술적 쿠키: 필수 목적(서비스 제공 및 보안)
- 개인맞춤형 광고 및 콘텐츠: 선택(동의 기반)
- 광고 성과 측정 및 시장 조사: 선택(동의 또는 정당한 이익)

---

## 2) 미국 주 규정(US State Regulations) 템플릿

KR
- 제목: “개인정보 판매/공유 및 타겟 광고 선택”
- 본문: “일부 주(예: 캘리포니아/버지니아/콜로라도 등) 거주자는 개인정보의 ‘판매 또는 공유’와 ‘타겟 광고’에 대한 선택권이 있습니다. 아래에서 선택을 저장할 수 있습니다. 선택은 언제든 변경할 수 있습니다.”
- 스위치/체크(권장):
  - [ ] 타겟 광고를 제한합니다 (Opt‑out of targeted advertising)
  - [ ] 개인정보의 판매/공유를 제한합니다 (Do Not Sell or Share My Personal Information)
  - [ ] (선택) 민감정보 사용을 제한합니다 (Limit the Use of My Sensitive Personal Information)
- 확인 버튼: [선택 저장]
- 추가 링크: [개인정보 처리방침] [내 권리 보기(주 규정 안내)]

EN
- Title: “Your choices for ‘sale/share’ and targeted advertising”
- Body: “Residents of certain US states (e.g., CA/VA/CO) may opt out of the ‘sale or sharing’ of personal information and targeted advertising. Use the options below. You can update them anytime.”
- Switches:
  - [ ] Opt‑out of targeted advertising
  - [ ] Do Not Sell or Share My Personal Information
  - [ ] (Optional) Limit the Use of My Sensitive Personal Information
- Button: [Save choices]
- Links: [Privacy Policy] [Your rights]

주의
- 16세 미만 관련 고지/확인은 Apple/Google 정책 및 적용 주법에 맞게 별도 처리(해당 시 “I’m over 16” 확인 체크 추가 권장)

---

## 3) iOS IDFA 설명(IDFA explainer) 템플릿

KR
- 제목: “앱 사용 경험 향상을 위한 추적 허용 안내”
- 본문(짧게): “사용자의 광고 식별자(IDFA)는 광고 성과 측정과 서비스 품질 개선에 사용됩니다. 동의 여부는 ‘추적 금지 요청’ 팝업에서 선택할 수 있으며, 나중에 iOS 설정에서 변경할 수 있습니다.”
- 포인트(선택 표시):
  - 더 적합한 광고/프로모션 제공
  - 광고 중복 노출 방지 및 성과 측정
  - 악성 활동 탐지로 서비스 안전성 강화
- 버튼: [계속] → 이어서 iOS ATT 시스템 팝업 표시(Allow / Ask App Not to Track)

EN
- Title: “About allowing tracking to improve your experience”
- Body (short): “Your IDFA helps us measure ad performance and improve quality. You’ll see Apple’s tracking prompt next; you can change this anytime in iOS Settings.”
- Points:
  - More relevant promotions
  - Frequency capping and measurement
  - Abuse/fraud detection for safety
- Button: [Continue] → then show Apple ATT prompt

금지 표현(Apple 가이드)
- ‘허용하면 보상을 제공’ ‘허용을 강요/유도’ 등은 금지. 중립적·사실에 기반한 설명만 제공

---

## 4) 버튼/텍스트 짧은 버전 모음(공통)

KR
- 모두 허용 / 비맞춤형 광고 / 자세히 설정 / 선택 저장 / 계속 / 취소

EN
- Allow all / Non‑personalized / Manage options / Save choices / Continue / Cancel

---

## 5) 연결/운영 체크리스트

- [ ] 모든 메시지에 동일한 Privacy Policy URL 연결(고정 URL)
- [ ] 테스트 디바이스에서 EEA/US 메시지 노출 여부 검증(Debug geography 활용)
- [ ] iOS: IDFA 설명 화면 → ATT 팝업 순서 확인, 문구 중립성 점검
- [ ] 수집 항목/파트너/목적 문구는 본 문서 ‘공통 안내’와 개인정보 처리방침의 내용이 일관되도록 유지

