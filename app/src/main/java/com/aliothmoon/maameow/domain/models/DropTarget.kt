package com.aliothmoon.maameow.domain.models

import com.aliothmoon.maameow.utils.i18n.UiText
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.math.roundToInt

/**
 * 目标库存刷新快照，只活在 [com.aliothmoon.maameow.domain.service.FightDropsRefresher]。
 * 整表字段是为了 SetTaskParams 整表重放，避免冲掉 medicine/stone/series。
 *
 * @param logLabel 库存保持=计划序号，理智作战=节点名（延迟到写日志时按语言解析）
 * @param dropRate 单次进图的期望掉落个数（企鹅物流统计）。非 null 时按期望次数刷，
 *   times 由缺口除以它四舍五入得出；null 表示逐场刷到掉够（times=MAX）
 */
data class DropTarget(
    val dropId: String,
    val dropCount: Int,
    val stage: String,
    val medicine: Int,
    val stone: Int,
    val series: Int,
    val logLabel: UiText,
    val medicineExpireDays: Int? = null,
    val drGrandet: Boolean = false,
    val report: ReportOptions = ReportOptions.DEFAULT,
    val dropRate: Double? = null,
) {
    /** 期望模式下按缺口算的次数；无掉率数据返回 null（调用方回退逐场刷） */
    fun expectedTimes(need: Int): Int? =
        dropRate?.takeIf { it > 0.0 && need > 0 }
            ?.let { maxOf(1, (need / it + ROUND_EPSILON).roundToInt()) }

    /**
     * 按缺口生成 Fight 参数 JSON。need≤0 或 [forceSkip] 时下发 `times=0`：
     * core（v6.17.0-beta.2 起）会禁用整条 Fight 子任务链，不进终端也不导航
     * drops 仍给 1 占位，任务已在队列里只能改参，字段不能空
     *
     * DepotMaintain append 与两侧刷新走本方法；Fight 目标库存 append 自行组 JSON
     *（`times=actualTimes`），刷新再经此抬到 MAX。
     *
     * @param forceSkip 缺口尚在但已判定跑不动（理智不足），直接跳过整个任务
     */
    fun toFightParamsJson(need: Int, forceSkip: Boolean = false): String = buildJsonObject {
        val skip = need <= 0 || forceSkip
        put("stage", stage)
        put("times", if (skip) 0 else timesValue(need))
        put("series", series)
        put("medicine", medicine)
        put("stone", stone)
        medicineExpireDays?.let { put("medicine_expire_days", it) }
        if (drGrandet) put("DrGrandet", true)
        put("drops", buildJsonObject { put(dropId, if (need <= 0) 1 else need) })
        putReportFields(report)
    }.toString()

    /** 期望模式给有限次数（刷够 drops 仍会提前停），否则 MAX 表示刷到掉够为止 */
    private fun timesValue(need: Int): Int = expectedTimes(need) ?: Int.MAX_VALUE

    companion object {
        /**
         * 半次边界的浮点补偿：掉率是十进制（0.4、0.2…）存成 double 后并不精确，
         * 例如 5 / 0.4 得 12.499999999999998，直接四舍五入会从 13 掉成 12。
         * 只在距边界 1e-9 内起作用，不会把真正的 12.49 抬成 13。
         */
        private const val ROUND_EPSILON = 1e-9
    }
}
