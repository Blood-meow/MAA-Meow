package com.aliothmoon.maameow.presentation.viewmodel

import com.aliothmoon.maameow.data.repository.DepotAccountSnapshot
import com.aliothmoon.maameow.data.repository.DepotSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class DepotInventoryViewModelTest {
    @Test
    fun availableDraws_combinesOrundumAndPermits() {
        val account = DepotAccountSnapshot(
            accountTag = "Official:1",
            snapshot = DepotSnapshot(
                items = mapOf(
                    "4003" to 1_259,
                    "4004" to 3,
                    "4005" to 2,
                ),
            ),
        )

        assertEquals(25, account.availableDraws())
    }

    @Test
    fun availableDraws_ignoresIncompleteOrundumDraw() {
        val account = DepotAccountSnapshot(
            accountTag = "Official:1",
            snapshot = DepotSnapshot(items = mapOf("4003" to 599)),
        )

        assertEquals(0, account.availableDraws())
    }
}
