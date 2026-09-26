package com.aliothmoon.maameow.presentation.viewmodel

import com.aliothmoon.maameow.data.model.DepotMaintainConfig
import com.aliothmoon.maameow.data.model.DepotMaintainPlan
import com.aliothmoon.maameow.data.model.DepotPlanOutcome
import com.aliothmoon.maameow.data.model.MallConfig
import com.aliothmoon.maameow.data.model.TaskChainNode
import com.aliothmoon.maameow.data.model.TaskParamProvider
import com.aliothmoon.maameow.data.repository.DepotSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 覆盖 [toUi] / [unmetIn] / [depotPlans] / [firstDepotNode] 这几个真正参与渲染与写回的
 * 扩展函数——它们原来是 private，测试只能自己重算一遍 need/outcome，等于在测测试。
 */
class DepotInventoryViewModelTest {

    /** 抽卡资源不算库存材料，不进网格（导出仍走原始数据） */
    @Test
    fun itemUiList_dropsDrawItems() {
        val snapshot = DepotSnapshot(
            items = mapOf(
                "4003" to 72_000,
                "7003" to 3,
                "7004" to 1,
                "30011" to 7,
            ),
        )

        assertEquals(listOf("30011"), snapshot.toItemUiList(emptyMap()).map { it.id })
    }

    @Test
    fun toUi_readsCurrentFromSnapshotAndComputesNeed() {
        val ui = plan(dropCount = 50, stage = "1-7")
            .toUi(node(), 0, DepotSnapshot(items = mapOf("30011" to 12)), emptyMap()) { true }

        assertEquals(12, ui.current)
        assertEquals(50, ui.target)
        assertEquals(38, ui.need)
        assertEquals(DepotPlanOutcome.Runnable, ui.outcome)
        assertTrue(ui.unmet)
    }

    @Test
    fun toUi_missingItemCountsAsZero() {
        val ui = plan(dropCount = 50, stage = "1-7")
            .toUi(node(), 0, DepotSnapshot(), emptyMap()) { true }

        assertEquals(0, ui.current)
        assertEquals(50, ui.need)
        assertTrue(ui.unmet)
    }

    @Test
    fun toUi_enoughStockClampsNeedToZero() {
        val ui = plan(dropCount = 50, stage = "1-7")
            .toUi(node(), 0, DepotSnapshot(items = mapOf("30011" to 60)), emptyMap()) { true }

        assertEquals(0, ui.need)
        assertEquals(DepotPlanOutcome.Enough, ui.outcome)
        assertFalse(ui.unmet)
    }

    /** 目标为 0 的计划会被执行侧按「目标为 0」拒掉，不该算成缺货 */
    @Test
    fun toUi_zeroTargetIsNotUnmet() {
        val ui = plan(dropCount = 0, stage = "1-7")
            .toUi(node(), 0, DepotSnapshot(), emptyMap()) { true }

        assertEquals(DepotPlanOutcome.ZeroTarget, ui.outcome)
        assertFalse(ui.unmet)
        assertFalse(plan(dropCount = 0).unmetIn(DepotSnapshot()))
    }

    @Test
    fun toUi_stageClosedFollowsActivityManager() {
        val ui = plan(dropCount = 50, stage = "1-7")
            .toUi(node(), 0, DepotSnapshot(), emptyMap()) { false }

        assertEquals(DepotPlanOutcome.StageClosed, ui.outcome)
    }

    @Test
    fun toUi_carriesNodeStateAndPlanIndex() {
        val ui = plan(dropCount = 50, stage = "1-7")
            .toUi(node(id = "n1", enabled = false), 3, DepotSnapshot(), emptyMap()) { true }

        assertEquals("n1", ui.nodeId)
        assertFalse(ui.nodeEnabled)
        assertEquals(3, ui.planIndex)
        // 物品查不到时回退成 ID，别显示空白
        assertEquals("30011", ui.itemName)
    }

    @Test
    fun unmetIn_isFalseWhenStockReachesTarget() {
        assertFalse(plan(dropCount = 50).unmetIn(DepotSnapshot(items = mapOf("30011" to 50))))
        assertTrue(plan(dropCount = 50).unmetIn(DepotSnapshot(items = mapOf("30011" to 49))))
    }

    /** 没选物品的计划在配置页已经有提示，网格里不该再多一个空格子 */
    @Test
    fun depotPlans_skipsPlansWithoutItem() {
        val chain = listOf(
            node(id = "a", config = DepotMaintainConfig(plans = listOf(plan(dropId = "")))),
            node(id = "b", config = DepotMaintainConfig(plans = listOf(plan(), plan()))),
        )

        assertEquals(2, chain.depotPlans().size)
    }

    @Test
    fun depotPlans_ignoresNonDepotNodes() {
        val chain = listOf(
            TaskChainNode(id = "a", name = "信用收支", config = MallConfig()),
            node(id = "b", config = DepotMaintainConfig(plans = listOf(plan()))),
        )

        assertEquals(1, chain.depotPlans().size)
    }

    /** 写回落点与 TaskChainState.updateDepotMaintainPlans 的选取必须一致：启用优先 */
    @Test
    fun firstDepotNode_prefersEnabledNode() {
        val disabled = node(id = "disabled", enabled = false, config = DepotMaintainConfig())
        val enabled = node(id = "enabled", enabled = true, config = DepotMaintainConfig())

        assertEquals("enabled", listOf(disabled, enabled).firstDepotNode()?.id)
        assertEquals("disabled", listOf(disabled).firstDepotNode()?.id)
        assertNull(listOf(TaskChainNode(id = "other", name = "信用收支", config = MallConfig())).firstDepotNode())
    }

    @Test
    fun cellUi_withoutPlanIsSettled() {
        val cell = DepotInventoryCellUi(
            id = "30011",
            name = "聚酸酯",
            count = 7,
            sortId = 1,
            plan = null,
        )

        assertFalse(cell.unmet)
    }

    @Test
    fun cellUi_followsItsPlan() {
        val cell = DepotInventoryCellUi(
            id = "30011",
            name = "聚酸酯",
            count = 7,
            sortId = 1,
            plan = plan(dropCount = 50, stage = "1-7")
                .toUi(node(), 0, DepotSnapshot(items = mapOf("30011" to 7)), emptyMap()) { true },
        )

        assertTrue(cell.unmet)
        assertEquals(43, cell.plan?.need)
    }

    private fun plan(
        dropId: String = "30011",
        dropCount: Int = 50,
        stage: String = "1-7",
    ) = DepotMaintainPlan(stage = stage, dropId = dropId, dropCount = dropCount)

    private fun node(
        id: String = "node",
        enabled: Boolean = true,
        config: TaskParamProvider = DepotMaintainConfig(plans = listOf(plan())),
    ) = TaskChainNode(id = id, name = "库存保持", enabled = enabled, config = config)
}
