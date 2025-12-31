package kr.sweetapps.alcoholictimer.util.notification

/**
 * 리텐션 알림 메시지 상수
 *
 * 알림 문구를 중앙에서 관리하여 쉽게 수정 가능
 *
 * @since 2025-12-31
 */
object RetentionMessages {

    // ==================== 그룹 A: 신규 유저 ====================

    object GroupA {
        // 1차 알림 (24시간 후)
        const val TITLE_1 = "🍺 ZERO 앱, 잊으신 건 아니죠?"
        const val MESSAGE_1 = "딱 하루만 도전해보세요. 시작이 반입니다! 첫 배지는 3일이면 획득할 수 있어요."

        // 2차 알림 (1차 발송 2일 후 = 3일차)
        const val TITLE_2 = "💪 작심삼일도 시작을 해야..."
        const val MESSAGE_2 = "금주 3일이면 수면의 질이 확실히 달라집니다. 오늘 시작하면 연말까지 새로운 나를 만날 수 있어요!"

        // 3차 알림 (2차 발송 4일 후 = 7일차)
        const val TITLE_3 = "🎯 벌써 일주일이 지났어요"
        const val MESSAGE_3 = "술값 아껴서 저금통에 넣으면 벌써 5만원! 지금 시작하면 한 달 뒤엔 20만원입니다. 💰"
    }

    // ==================== 그룹 B: 활성 유저 ====================

    object GroupB {
        // 3일 알림
        const val TITLE_3D = "🎉 3일 달성이 눈앞에!"
        const val MESSAGE_3D = "금주 3일차가 다가옵니다. 조금만 더 힘내세요! 벌써 수면이 개선되고 있을 거예요. 💤"

        // 7일 알림
        const val TITLE_7D = "🏆 일주일 달성 임박!"
        const val MESSAGE_7D = "금주 7일차가 다가옵니다. 이미 간 기능이 개선되기 시작했습니다. 계속 화이팅! 🔥"

        // 30일 알림
        const val TITLE_30D = "👑 한 달 달성 초읽기!"
        const val MESSAGE_30D = "와! 벌써 15만 원이나 아꼈어요. 💸 치킨 5마리 값입니다! 새로운 습관이 완전히 자리 잡았습니다. 축하해요! 🎊"
    }

    // ==================== 그룹 C: 휴식 유저 ====================

    object GroupC {
        // D+1 알림 (24시간 후)
        const val TITLE_D1 = "🔥 3일 성공 대단했어요!"
        const val MESSAGE_D1 = "이제 일주일 코스에 도전해보는 건 어떨까요? 지난번보다 더 쉬울 거예요. 당신은 할 수 있습니다!"

        // D+3 알림 (3일 후)
        const val TITLE_D3 = "💚 다시 달릴 준비 되셨나요?"
        const val MESSAGE_D3 = "당신의 간이 회복되길 기다리고 있어요. 작은 시작이 큰 변화를 만듭니다. 다시 함께해요! 🌱"
    }
}

