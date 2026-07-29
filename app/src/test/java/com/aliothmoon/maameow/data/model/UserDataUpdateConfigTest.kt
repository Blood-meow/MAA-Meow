package com.aliothmoon.maameow.data.model

import com.aliothmoon.maameow.data.repository.DepotRepository
import com.aliothmoon.maameow.data.repository.OperBoxRepository
import com.aliothmoon.maameow.domain.models.UserDataUpdateTriggerInterval
import com.aliothmoon.maameow.maa.task.MaaTaskType
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserDataUpdateConfigTest {

    private fun ctx(
        operSync: Long = 0L,
        depotSync: Long = 0L,
    ): TaskParamContext {
        val operRepo = mockk<OperBoxRepository> {
            every { syncTimeMillis("Official:test") } returns operSync
        }
        val depotRepo = mockk<DepotRepository> {
            every { syncTimeMillis("Official:test") } returns depotSync
        }
        return testTaskParamContext(
            operBoxRepository = operRepo,
            depotRepository = depotRepo,
        )
    }

    @Test
    fun bothSwitchesOff_producesNothing() {
        val result = UserDataUpdateConfig(updateOperBox = false, updateDepot = false)
            .toTaskParams(ctx())
        assertTrue(result.isEmpty())
    }

    @Test
    fun blankAccountTag_skipsRecognitionTasks() {
        val result = UserDataUpdateConfig().toTaskParams(
            ctx().copy(depotAccountTag = "")
        )
        assertTrue(result.params.isEmpty())
        assertFalse(result.unlockDoubleSync)
    }

    @Test
    fun everyTime_bothOn_ordersOperBoxThenDepot() {
        val result = UserDataUpdateConfig().toTaskParams(ctx())
        assertEquals(
            listOf(MaaTaskType.OPER_BOX, MaaTaskType.DEPOT),
            result.map { it.type },
        )
    }

    @Test
    fun onlyDepot_producesDepotOnly() {
        val result = UserDataUpdateConfig(updateOperBox = false, updateDepot = true)
            .toTaskParams(ctx())
        assertEquals(listOf(MaaTaskType.DEPOT), result.map { it.type })
    }

    @Test
    fun daily_recentSync_skips() {
        val now = System.currentTimeMillis()
        val result = UserDataUpdateConfig(
            triggerInterval = UserDataUpdateTriggerInterval.DAILY,
        ).toTaskParams(ctx(operSync = now, depotSync = now))
        assertTrue(result.isEmpty())
    }
}
