package com.aliothmoon.maameow.maa.task

/**
 * 一次 AsstAppendTask 的注册位。
 *
 * - [nodeId] / [index]：同节点展开出多条任务时区分（库存保持多计划）
 * - [accountTag]：账号库存桶；同客户端切号时回调前同步切桶
 */
data class TaskSlot(
    val nodeId: String,
    val index: Int = 0,
    val accountTag: String? = null,
)