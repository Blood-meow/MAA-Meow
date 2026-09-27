package com.aliothmoon.maameow.presentation.view.panel.depot

/**
 * 新建计划时要带上的节点状态：节点没启用时面板要提示「保存了也不会跑」。
 */
data class DepotPlanContext(
    val nodeEnabled: Boolean = true,
)

/**
 * 「当前 / 目标」：库存保持的进度口径，格子、计划概览、计划详情面板都走这一份。
 *
 * 没识别过仓库时当前值写「--」而不是 0：那时候缺多少算不准，写 0 会看着像真的一件都没有。
 */
internal fun depotProgressText(current: Int, target: Int, synced: Boolean): String =
    "${if (synced) current.toString() else "--"} / $target"
