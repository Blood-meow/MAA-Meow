package com.aliothmoon.maameow.data.resource

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aliothmoon.maameow.R
import java.time.DayOfWeek

/**
 * 关卡信息（UI 使用）
 */
data class StageInfo(
    val stageId: String,       // 内部 ID（如 "a001_01_perm"）
    val code: String,          // 显示代码（如 "GT-1"）
    val apCost: Int = 0,       // 理智消耗
    val openDays: List<DayOfWeek> = emptyList(),  // 开放日期（空表示每天开放）
    val category: StageCategory = StageCategory.OTHER,  // 关卡分类
    @StringRes val tipRes: Int? = null,  // 关卡提示的字符串资源（常驻资源本）
    val dropGroups: List<List<String>> = emptyList()  // 芯片本掉落组 (迁移自 WPF DropGroups)
) {
    /**
     * 用于 UI 显示的名称对应的字符串资源
     * 迁移自 WPF zh-cn.xaml 第740-770行的本地化映射；为 null 表示直接用关卡代码（如 "1-7"）
     */
    @get:StringRes
    val displayNameRes: Int?
        get() = STAGE_DISPLAY_NAME_RES[code]

    /**
     * 检查关卡在指定日期是否开放
     */
    fun isOpenOn(dayOfWeek: DayOfWeek): Boolean {
        return openDays.isEmpty() || dayOfWeek in openDays
    }

    /**
     * 检查关卡今天是否开放（使用鹰角历，04:00 换日）
     * @param clientType 客户端类型，用于确定服务器时区
     */
    fun isOpenToday(clientType: String = "Official"): Boolean {
        return isOpenOn(ServerTimezone.getYjDayOfWeek(clientType))
    }

    companion object {
        /**
         * 关卡代码到显示名资源的映射（未列出的代码直接用代码本身，如 "1-7"）
         */
        private val STAGE_DISPLAY_NAME_RES = mapOf(
            // 资源本
            "CE-6" to R.string.stage_name_ce_6,
            "AP-5" to R.string.stage_name_ap_5,
            "CA-5" to R.string.stage_name_ca_5,
            "LS-6" to R.string.stage_name_ls_6,
            "SK-5" to R.string.stage_name_sk_5,

            // 芯片本
            "PR-A-1" to R.string.stage_name_pr_a_1,
            "PR-A-2" to R.string.stage_name_pr_a_2,
            "PR-B-1" to R.string.stage_name_pr_b_1,
            "PR-B-2" to R.string.stage_name_pr_b_2,
            "PR-C-1" to R.string.stage_name_pr_c_1,
            "PR-C-2" to R.string.stage_name_pr_c_2,
            "PR-D-1" to R.string.stage_name_pr_d_1,
            "PR-D-2" to R.string.stage_name_pr_d_2,

            // 剿灭模式（复用剿灭选择器已有的文案）
            "Annihilation" to R.string.panel_fight_annihilation_current,
            "Chernobog@Annihilation" to R.string.panel_fight_annihilation_chernobog,
            "Lungmen@Annihilation" to R.string.panel_fight_annihilation_outskirts,
            "LungmenOutskirts@Annihilation" to R.string.panel_fight_annihilation_outskirts,
            "LungmenDowntown@Annihilation" to R.string.panel_fight_annihilation_downtown
        )

        /**
         * 关卡提示信息映射
         * 迁移自 WPF Localizations/zh-cn.xaml 第762-770行
         */
        val STAGE_TIP_RES = mapOf(
            "CE-6" to R.string.stage_tip_ce_6,
            "AP-5" to R.string.stage_tip_ap_5,
            "CA-5" to R.string.stage_tip_ca_5,
            "LS-6" to R.string.stage_tip_ls_6,
            "SK-5" to R.string.stage_tip_sk_5,
            // PR-X-2 不单独出提示，同上游，免得与 PR-X-1 重复
            "PR-A-1" to R.string.stage_tip_pr_a_1,
            "PR-B-1" to R.string.stage_tip_pr_b_1,
            "PR-C-1" to R.string.stage_tip_pr_c_1,
            "PR-D-1" to R.string.stage_tip_pr_d_1
        )
    }
}

/**
 * Compose 里的关卡显示名（选关徽章、下拉胶囊的唯一取文案入口）：
 * 优先用 [StageItem.displayNameRes]（常驻资源本/芯片本），没有资源时回退
 * [StageItem.displayName]——常驻关卡即关卡代码，活动关卡为服务端文案。
 */
@Composable
fun StageItem.localizedDisplayName(): String =
    displayNameRes?.let { stringResource(it) } ?: displayName
