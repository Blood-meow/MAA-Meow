package com.aliothmoon.maameow.data.resource

import android.content.Context
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.activity.MiniGame
import com.aliothmoon.maameow.data.model.activity.StageActivityInfo
import com.aliothmoon.maameow.utils.i18n.UiText
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek

/**
 * [ActivityManager.getStageTips] 文本契约，对齐 WPF StageManager.GetStageTips
 */
class ActivityManagerStageTipsTest {

    private val context = mockk<Context> {
        every { getString(R.string.panel_fight_stage_tip_inventory) } returns "库存"
        every { getString(R.string.panel_fight_activity_days_left_open) } returns "剩余天数: "
        every { getString(R.string.panel_fight_activity_less_than_one_day) } returns "不到 1 天"
        every { getString(R.string.stage_tip_ce_6) } returns "CE-6: 龙门币"
        every { getString(R.string.stage_tip_ls_6) } returns "LS-6: 经验"
        every { getString(R.string.panel_fight_stage_tip_affiliated_mini_game, any()) } answers {
            "小工具→牛杂→${secondArg<Array<Any?>>()[0]}"
        }
    }

    private val itemHelper = mockk<ItemHelper> {
        every { getItemInfo("30011") } returns ItemInfo(id = "30011", name = "源岩")
        every { getItemInfo("99999") } returns null
    }

    private val threeDaysLater = System.currentTimeMillis() + 3L * 24 * 60 * 60 * 1000 + 60_000

    private val sideStory = StageActivityInfo(
        name = "测试活动", tip = "", utcStartTime = 0L, utcExpireTime = threeDaysLater,
    )

    private fun manager(
        stages: Map<String, MergedStageInfo>,
        miniGames: List<MiniGame> = emptyList(),
    ): ActivityManager {
        val manager = ActivityManager(
            context = context,
            chainState = mockk(relaxed = true),
            maaApiService = mockk(relaxed = true),
            itemHelper = itemHelper,
        )
        setFlow(manager, "_stages", stages)
        setFlow(manager, "_miniGames", miniGames)
        return manager
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> setFlow(manager: ActivityManager, field: String, value: T) {
        val f = ActivityManager::class.java.getDeclaredField(field).apply { isAccessible = true }
        (f.get(manager) as MutableStateFlow<T>).value = value
    }

    @Suppress("UNCHECKED_CAST")
    private fun mergedStages(manager: ActivityManager): Map<String, MergedStageInfo> {
        val f = ActivityManager::class.java.getDeclaredField("_stages").apply { isAccessible = true }
        return (f.get(manager) as MutableStateFlow<Map<String, MergedStageInfo>>).value
    }

    /** 走真实的 [ActivityManager.buildMergedStagesMap]，确认常驻关卡字典仍带 tipRes */
    private fun buildMergedStagesMap(manager: ActivityManager) {
        ActivityManager::class.java.getDeclaredMethod("buildMergedStagesMap")
            .apply { isAccessible = true }
            .invoke(manager)
    }

    @Test
    fun activityLine_usesLocalizedDaysLeft() {
        val tips = manager(
            mapOf("TA-1" to MergedStageInfo(code = "TA-1", displayName = "TA-1", activity = sideStory))
        ).getStageTips(DayOfWeek.MONDAY)
        assertEquals(listOf("｢测试活动｣ 剩余天数: 3"), tips)
    }

    @Test
    fun dropLine_appendsInventoryOnlyWhenRecognized() {
        val stages = mapOf(
            "TA-1" to MergedStageInfo(code = "TA-1", displayName = "TA-1", activity = sideStory, drop = "30011"),
        )
        assertEquals(
            listOf("｢测试活动｣ 剩余天数: 3", "TA-1: 源岩"),
            manager(stages).getStageTips(DayOfWeek.MONDAY),
        )
        assertEquals(
            listOf("｢测试活动｣ 剩余天数: 3", "TA-1: 源岩 (库存 42)"),
            manager(stages).getStageTips(DayOfWeek.MONDAY, inventory = mapOf("30011" to 42)),
        )
    }

    @Test
    fun dropLine_fallsBackToItemIdWhenUnknown() {
        val tips = manager(
            mapOf("TA-2" to MergedStageInfo(code = "TA-2", displayName = "TA-2", activity = sideStory, drop = "99999"))
        ).getStageTips(DayOfWeek.MONDAY, inventory = mapOf("99999" to 0))
        assertEquals(listOf("｢测试活动｣ 剩余天数: 3", "TA-2: 99999 (库存 0)"), tips)
    }

    @Test
    fun affiliatedMiniGame_isListedUnderItsActivity_onlyWhenOpen() {
        val open = MiniGame(
            display = UiText.Dynamic("测试小游戏"), value = "MiniGame@Test",
            utcStartTime = 0L, utcExpireTime = Long.MAX_VALUE, activity = "测试活动",
            category = UiText.Empty,
        )
        val closed = open.copy(display = UiText.Dynamic("已结束"), utcStartTime = 1L, utcExpireTime = 2L)
        val other = open.copy(display = UiText.Dynamic("别的活动"), activity = "别的活动")
        val tips = manager(
            mapOf("TA-1" to MergedStageInfo(code = "TA-1", displayName = "TA-1", activity = sideStory)),
            miniGames = listOf(open, closed, other),
        ).getStageTips(DayOfWeek.MONDAY)
        assertEquals(listOf("｢测试活动｣ 剩余天数: 3", "小工具→牛杂→测试小游戏"), tips)
    }

    /** CA-5 / PR-x-1 这类带分组库存的常驻关（openDays 为空 = 每天开放） */
    private fun groupedStage(
        code: String = "CA-5",
        groups: List<List<String>> = listOf(listOf("3301", "3302", "3303")),
    ) = MergedStageInfo(code = code, displayName = code, dropGroups = groups)

    @Test
    fun dropGroupLine_unsyncedShowsDashesForUnknownItems() {
        assertEquals(
            emptyList<String>(),
            manager(mapOf("CA-5" to groupedStage())).getStageTips(DayOfWeek.MONDAY),
        )
        assertEquals(
            listOf(" (库存 -- & -- & 7)"),
            manager(mapOf("CA-5" to groupedStage()))
                .getStageTips(DayOfWeek.MONDAY, inventory = mapOf("3303" to 7)),
        )
    }

    @Test
    fun dropGroupLine_fillsMissingItemsWithZeroAfterSync() {
        val tips = manager(mapOf("CA-5" to groupedStage()))
            .getStageTips(DayOfWeek.MONDAY, inventory = mapOf("3303" to 7), hasSyncedInventory = true)
        assertEquals(listOf(" (库存 0 & 0 & 7)"), tips)
    }

    @Test
    fun dropGroupLine_staysVisibleWhenEveryItemIsZeroAfterSync() {
        val tips = manager(mapOf("CA-5" to groupedStage()))
            .getStageTips(DayOfWeek.MONDAY, hasSyncedInventory = true)
        assertEquals(listOf(" (库存 0 & 0 & 0)"), tips)
    }

    @Test
    fun dropGroupLine_joinsItemsWithAmpersandAndGroupsWithSlash() {
        val tips = manager(
            mapOf("PR-A-1" to groupedStage("PR-A-1", listOf(listOf("3261", "3231"), listOf("3262", "3232"))))
        ).getStageTips(DayOfWeek.MONDAY, inventory = mapOf("3261" to 3), hasSyncedInventory = true)
        assertEquals(listOf(" (库存 3 & 0 / 0 & 0)"), tips)
    }

    @Test
    fun permanentStageTip_comesFromStringResource() {
        val stages = mapOf(
            "CE-6" to MergedStageInfo(
                code = "CE-6", displayName = "CE-6", tipRes = R.string.stage_tip_ce_6,
            ),
        )
        assertEquals(listOf("CE-6: 龙门币"), manager(stages).getStageTips(DayOfWeek.MONDAY))
    }

    @Test
    fun permanentStageTip_isSkippedWhenNoResource() {
        val stages = mapOf("CE-6" to MergedStageInfo(code = "CE-6", displayName = "CE-6"))
        assertEquals(emptyList<String>(), manager(stages).getStageTips(DayOfWeek.MONDAY))
    }

    /** 常驻关卡表本身：显示名/提示都必须挂到字符串资源上，主线关卡两个都为 null（回退关卡代码） */
    @Test
    fun permanentStages_wireDisplayNameAndTipResources() {
        val ce6 = PermanentStages.STAGES.first { it.code == "CE-6" }
        assertEquals(R.string.stage_name_ce_6, ce6.displayNameRes)
        assertEquals(R.string.stage_tip_ce_6, ce6.tipRes)

        val mainline = PermanentStages.STAGES.first { it.code == "1-7" }
        assertNull(mainline.displayNameRes)
        assertNull(mainline.tipRes)

        // 芯片本 PR-X-2 只映射显示名，不出提示（同上游，免得与 PR-X-1 重复）
        val prA2 = PermanentStages.STAGES.first { it.code == "PR-A-2" }
        assertEquals(R.string.stage_name_pr_a_2, prA2.displayNameRes)
        assertNull(prA2.tipRes)
    }

    /** 真实常驻关卡进合并字典后 tipRes 仍在，能直接产出行 */
    @Test
    fun mergedStageMap_keepsPermanentTipResources() {
        val manager = manager(emptyMap())
        buildMergedStagesMap(manager)
        val stages = mergedStages(manager)

        assertEquals(R.string.stage_tip_ce_6, stages.getValue("CE-6").tipRes)
        assertEquals(R.string.stage_tip_pr_a_1, stages.getValue("PR-A-1").tipRes)
        assertNull(stages.getValue("PR-A-2").tipRes)
        assertNull(stages.getValue("12-17-HARD").tipRes)

        // 常驻关卡每天开放的 LS-6 只出一行提示（周一同时开放多个资源本，这里单独取字典）
        val ls6Only = manager(mapOf("LS-6" to stages.getValue("LS-6")))
        assertEquals(listOf("LS-6: 经验"), ls6Only.getStageTips(DayOfWeek.MONDAY))
    }
}
