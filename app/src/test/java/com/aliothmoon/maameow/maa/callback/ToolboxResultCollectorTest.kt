package com.aliothmoon.maameow.maa.callback

import com.alibaba.fastjson2.JSON
import com.aliothmoon.maameow.data.achievement.AchievementRepository
import com.aliothmoon.maameow.data.repository.DepotRepository
import com.aliothmoon.maameow.data.repository.OperBoxRepository
import com.aliothmoon.maameow.data.resource.ItemHelper
import com.aliothmoon.maameow.data.resource.ResourceDataManager
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class ToolboxResultCollectorTest {
    private val collector = ToolboxResultCollector(
        resourceDataManager = mockk(relaxed = true),
        achievementRepository = mockk(relaxed = true),
        itemHelper = mockk(relaxed = true),
        depotRepository = mockk(relaxed = true),
        operBoxRepository = mockk(relaxed = true),
    )

    @Test
    fun parseDepotItems_keepsPermitsAcrossSupportedCountFormats() {
        val raw = """{
            "4003": 1200,
            "7003": {"quantity": 3},
            "7004": {"have": "2"},
            "4004": {"count": 9},
            "invalid": {"name": "ignored"},
            "zero": 0
        }"""

        val items = collector.parseDepotItems(raw).orEmpty().associate { it.id to it.count }

        assertEquals(
            mapOf("4003" to 1200, "7003" to 3, "7004" to 2, "4004" to 9),
            items,
        )
    }

    @Test
    fun parseDepotItems_acceptsObjectDataInsteadOfEncodedString() {
        val data = JSON.parseObject("""{"7003":"4","7004":{"value":1}}""")

        val items = collector.parseDepotItems(data).orEmpty().associate { it.id to it.count }

        assertEquals(mapOf("7003" to 4, "7004" to 1), items)
    }
}
