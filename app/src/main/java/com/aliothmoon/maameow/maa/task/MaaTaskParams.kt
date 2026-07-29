package com.aliothmoon.maameow.maa.task

/**
 * MaaCore#AsstAppendTask 票根。
 *
 * - [slot] 由 Analyze 注入，链外路径（作业等）保持 null
 * - 目标库存快照不再挂在本类，改为 Analyze/Config 侧 `FightDropsRefresher.stage(slot, target)`
 */
data class MaaTaskParams(
    val type: MaaTaskType,
    val params: String,
    val slot: TaskSlot? = null,
)