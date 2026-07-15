package com.aliothmoon.maameow.schedule.model
sealed class CountdownState {
    data class Counting(
        val strategyName: String,
        val remainingSeconds: Int,
        /** 任务链定时：倒计时确认按钮显示「开始任务链」 */
        val useSequence: Boolean = false,
    ) : CountdownState()
    data object Executing : CountdownState()
    data object Idle : CountdownState()
}
