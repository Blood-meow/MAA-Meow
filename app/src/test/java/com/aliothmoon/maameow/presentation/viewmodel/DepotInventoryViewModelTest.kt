package com.aliothmoon.maameow.presentation.viewmodel

import com.aliothmoon.maameow.data.repository.DepotAccountSnapshot
import com.aliothmoon.maameow.data.repository.DepotSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class DepotInventoryViewModelTest {
    @Test
    fun drawSummary_combinesOrundumAndPermits() {
        val account = DepotAccountSnapshot(
            accountTag = "Official:1",
            snapshot = DepotSnapshot(
                items = mapOf(
                    "4003" to 1_259,
                    "7003" to 3,
                    "7004" to 2,
                    "4004" to 99,
                    "4005" to 88,
                ),
            ),
        )

        val summary = account.drawSummary()
        assertEquals(25, summary.total)
        assertEquals(2, summary.orundum)
        assertEquals(23, summary.permits)
    }

    @Test
    fun drawSummary_ignoresIncompleteOrundumDraw() {
        val account = DepotAccountSnapshot(
            accountTag = "Official:1",
            snapshot = DepotSnapshot(items = mapOf("4003" to 599)),
        )

        assertEquals(0, account.drawSummary().total)
    }
}
