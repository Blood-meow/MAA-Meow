package com.aliothmoon.maameow.maa.task

import com.aliothmoon.maameow.domain.models.DropTarget

/**
 * MaaCore#AsstAppendTask
 *
 * - type: 任务类型字符串
 * - params: 任务参数 JSON 字符串
 * - nodeId: 关联的 TaskChainNode.id;由 AnalyzeTaskChainUseCase 注入,
 *           供回调阶段(如 Infrast 计划自动切换)反查源节点。Copilot 等链外路径保持 null
 * - accountTag: 该任务继承的账号库存桶；同客户端账号切换时用于回调前同步切桶
 * - dropTarget: 目标库存模式的运行时重算快照；非目标库存为 null
 */
data class MaaTaskParams(
    val type: MaaTaskType,
    val params: String,
    val nodeId: String? = null,
    val accountTag: String? = null,
    val dropTarget: DropTarget? = null,
)
