### 📋 수정된 다국어 문구 정의 (Final Safe Version)

이 내용은 `LevelDefinitions`가 어떻게 바뀌든 상관없이 **영원히 안전한 문구**입니다.

#### 🔵 그룹 B: 열심히 달리는 유저 (Active)

*레벨 이름 제거, 순수 기간 달성 축하로 변경*

| 시점 | 구분 | 🇰🇷 한국어 (ko) | 🇺🇸 영어 (en - 기본) | 🇯🇵 일본어 (ja) | 🇮🇩 인도네시아어 (in) |
| --- | --- | --- | --- | --- | --- |
| **3일** | 제목 | 작심삼일 돌파! 🎉 | 3-Day Streak! 🎉 | 三日坊主を突破！🎉 | 3 Hari Beruntun! 🎉 |
|  | 내용 | 첫 번째 고비를 넘기셨군요. 정말 대단합니다! | You passed the first hurdle. You are doing great! | 最初の山場を越えましたね。すごいです！ | Kamu melewati rintangan pertama. Keren banget! |
| **7일** | 제목 | 일주일 달성! 🏆 | 1 Week Goal! 🏆 | 1週間達成！🏆 | Target 1 Minggu! 🏆 |
|  | 내용 | 몸이 가벼워진 게 느껴지시나요? 계속 도전하세요. | Do you feel lighter? Keep going. | 体が軽くなったのを感じますか？続けましょう。 | Apa badan terasa lebih ringan? Lanjutkan. |
| **30일** | 제목 | 한 달 달성! 💸 | 1 Month Goal! 💸 | 1ヶ月達成！💸 | Target 1 Bulan! 💸 |
|  | 내용 | 이제 습관이 되셨군요. 당신은 의지의 한국인! | It's now a habit. I respect your will! | もう習慣になりましたね。尊敬します！ | Ini sudah jadi kebiasaan. Salut buat kamu! |

#### 🟠 그룹 C: 잠시 쉬고 있는 유저 (Resting)

*과거 기록(3일 등) 언급 제거, 복귀 유도로 변경*

| 시점 | 구분 | 🇰🇷 한국어 (ko) | 🇺🇸 영어 (en - 기본) | 🇯🇵 일본어 (ja) | 🇮🇩 인도네시아어 (in) |
| --- | --- | --- | --- | --- | --- |
| **D+1** | 제목 | 괜찮아요, 다시 시작해요 💪 | Don't give up 💪 | 大丈夫、また始めましょう 💪 | Jangan menyerah 💪 |
|  | 내용 | 잠시 쉬어가도 괜찮아요. 타이머는 언제나 기다리고 있습니다. | It's okay to rest. The timer is always waiting for you. | 休んでも大丈夫です。タイマーはいつでも待っています。 | Istirahat itu wajar. Timer selalu menunggumu. |
| **D+3** | 제목 | 간이 휴식을 원해요 🏥 | Your liver needs rest 🏥 | 肝臓が休みたいそうです 🏥 | Liver butuh istirahat 🏥 |
|  | 내용 | 다시 달릴 준비 되셨나요? 건강을 위해 다시 돌아오세요. | Ready to start again? Come back for your health. | 準備はいいですか？健康のために戻ってきてください。 | Siap mulai lagi? Kembalilah demi kesehatanmu. |

*(Group A는 설치 후 미진입 유저라 레벨과 상관없으므로 기존 문구 유지)*

---

### 🚀 적용 방법 (프롬프트)

기존에 작업했던 `strings.xml`의 내용을 이 **'안전한 버전'**으로 덮어씌우는 것이 가장 깔끔합니다. 로직 코드는 건드릴 필요가 없습니다.

**에이전트에게 아래 내용을 전달하세요.**

> "**리텐션 알림 문구에 논리적 오류(실제 레벨과 불일치, 과거 기록 불일치)가 있어서, 더 안전하고 보편적인 문구로 수정하려고 해.**
> 1. `res/values/strings.xml` (및 ko, ja, in 폴더)에 있는 **Group B와 Group C의 텍스트**를 아래 표 내용으로 **덮어씌워 줘 (Overwrite).**
> * Key 값(`notif_group_b_3day_title` 등)은 그대로 유지해.
> * 내용만 수정하면 돼.
>
>
> 2. **수정할 내용 (표):**
     > (위의 '수정된 다국어 문구 정의' 표 내용을 복사해서 붙여넣으세요)
> 3. **Group A**는 수정하지 말고 그대로 둬.

---
