package com.aliothmoon.maameow

import com.aliothmoon.maameow.data.achievement.AchievementCondition
import com.aliothmoon.maameow.data.achievement.AchievementConditionOp
import com.aliothmoon.maameow.data.achievement.AchievementDefinition
import com.aliothmoon.maameow.data.achievement.AchievementRecord
import com.aliothmoon.maameow.data.achievement.AchievementTrigger
import com.aliothmoon.maameow.data.achievement.AchievementTriggerMode
import com.aliothmoon.maameow.data.achievement.applyTrigger
import com.aliothmoon.maameow.data.achievement.matches
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the pure-function helpers extracted to file-level in
 * [com.aliothmoon.maameow.data.achievement.AchievementRepository]: the
 * condition / trigger `matches` predicates, the month-day range helpers and
 * the per-mode `applyTrigger` reducer. These were previously `private`
 * member extensions on `AchievementRepository`; they were promoted to
 * `internal` top-level functions so this test can exercise them without
 * having to instantiate the repository (which would need a real Context
 * and would launch DataStore I/O on construction).
 */
class AchievementConditionMatchTest {
    // ---------- AchievementCondition.matches ----------

    @Test
    fun condition_eq_matchesExactValue() {
        val c = AchievementCondition(field = "key", op = AchievementConditionOp.EQ, value = "abc")
        assertTrue(c.matches(payloadOf("key" to "abc")))
        assertFalse(c.matches(payloadOf("key" to "xyz")))
    }

    @Test
    fun condition_ne_matchesEverythingExceptExactValue() {
        val c = AchievementCondition(field = "key", op = AchievementConditionOp.NE, value = "abc")
        assertTrue(c.matches(payloadOf("key" to "xyz")))
        assertFalse(c.matches(payloadOf("key" to "abc")))
    }

    @Test
    fun condition_gt_usesDoubleSemantics() {
        val c = AchievementCondition(field = "value", op = AchievementConditionOp.GT, value = "3")
        assertTrue(c.matches(payloadOf("value" to "5")))
        assertFalse(c.matches(payloadOf("value" to "2")))
        assertFalse(c.matches(payloadOf("value" to "3")))
    }

    @Test
    fun condition_lte_includesUpperBound() {
        val c = AchievementCondition(field = "value", op = AchievementConditionOp.LTE, value = "10")
        assertTrue(c.matches(payloadOf("value" to "10")))
        assertTrue(c.matches(payloadOf("value" to "0")))
        assertFalse(c.matches(payloadOf("value" to "10.1")))
    }

    @Test
    fun condition_between_isInclusiveOnBothEnds() {
        val c = AchievementCondition(field = "value", op = AchievementConditionOp.BETWEEN, value = "3..7")
        assertTrue(c.matches(payloadOf("value" to "3")))
        assertTrue(c.matches(payloadOf("value" to "5")))
        assertTrue(c.matches(payloadOf("value" to "7")))
        assertFalse(c.matches(payloadOf("value" to "2")))
        assertFalse(c.matches(payloadOf("value" to "8")))
    }

    @Test
    fun condition_between_doesNotMatchWhenBoundsDoNotParse() {
        val c = AchievementCondition(field = "value", op = AchievementConditionOp.BETWEEN, value = "abc..def")
        assertFalse(c.matches(payloadOf("value" to "5")))
    }

    @Test
    fun condition_contains_isCaseInsensitive() {
        val c = AchievementCondition(field = "greeting", op = AchievementConditionOp.CONTAINS, value = "HELLO")
        assertTrue(c.matches(payloadOf("greeting" to "say hello world")))
        assertTrue(c.matches(payloadOf("greeting" to "HELLO")))
        assertFalse(c.matches(payloadOf("greeting" to "goodbye")))
    }

    @Test
    fun condition_monthDay_matchesExactString() {
        val c = AchievementCondition(field = "today", op = AchievementConditionOp.MONTH_DAY, value = "03-14")
        assertTrue(c.matches(payloadOf("today" to "03-14")))
        assertFalse(c.matches(payloadOf("today" to "04-14")))
    }

    // ---------- AchievementTrigger.matches ----------

    @Test
    fun trigger_withWhere_filtersByPayload() {
        val t = AchievementTrigger(event = "task_chain_error", where = mapOf("channel" to "stable"))
        assertTrue(t.matches(payloadOf("event" to "task_chain_error", "channel" to "stable", "error" to "boom")))
        assertFalse(t.matches(payloadOf("event" to "task_chain_error", "channel" to "beta")))
    }

    @Test
    fun trigger_withConditionsAndWhere_requiresAll() {
        val t = AchievementTrigger(
            event = "recruit_result",
            where = mapOf("rarity" to "6"),
            conditions = listOf(
                AchievementCondition(field = "rarity", op = AchievementConditionOp.EQ, value = "6"),
                AchievementCondition(field = "result", op = AchievementConditionOp.EQ, value = "success"),
            ),
        )
        assertTrue(t.matches(payloadOf("event" to "recruit_result", "rarity" to "6", "result" to "success")))
        assertFalse(t.matches(payloadOf("event" to "recruit_result", "rarity" to "6", "result" to "fail")))
    }

    @Test
    fun trigger_eventMismatch_neverMatches() {
        val t = AchievementTrigger(event = "process_task_completed")
        assertFalse(t.matches(payloadOf("event" to "task_chain_error", "channel" to "stable")))
    }

    // ---------- applyTrigger: 5 AchievementTriggerMode behaviours + 重复解锁不重复通知 ----------

    private fun defWithTarget(target: Int = 1): AchievementDefinition = AchievementDefinition(
        id = "test-achievement",
        title = com.aliothmoon.maameow.data.achievement.LocalizedText(zh = "T", en = "T"),
        description = com.aliothmoon.maameow.data.achievement.LocalizedText(),
        condition = com.aliothmoon.maameow.data.achievement.LocalizedText(),
        category = com.aliothmoon.maameow.data.achievement.AchievementCategory.BASIC_USAGE,
        target = target,
    )

    @Test
    fun applyTrigger_increment_addsAmountTimesEventCount() {
        val def = defWithTarget(target = 10)
        val record = AchievementRecord(id = def.id)
        val trigger = AchievementTrigger(event = "x", mode = AchievementTriggerMode.INCREMENT, amount = 3)
        val (next, unlockedNow) = applyTrigger(def, record, trigger, eventAmount = 2, payload = emptyMap())
        assertEquals(6, next.progress)
        assertFalse(unlockedNow)
        assertFalse(next.unlocked)
    }

    @Test
    fun applyTrigger_setMax_takesPayloadValueOrEventAmount() {
        val def = defWithTarget(target = 0)
        val record = AchievementRecord(id = def.id, progress = 5)
        val trigger = AchievementTrigger(event = "x", mode = AchievementTriggerMode.SET_MAX)
        val (next, _) = applyTrigger(def, record, trigger, eventAmount = 2, payload = mapOf("value" to "7"))
        assertEquals(7, next.progress)
        // When payload has no "value", falls back to eventAmount
        val (next2, _) = applyTrigger(def, record, trigger, eventAmount = 9, payload = emptyMap())
        assertEquals(9, next2.progress)
    }

    @Test
    fun applyTrigger_sameDayCount_resetsAcrossDays() {
        val def = defWithTarget(target = 0)
        val record = AchievementRecord(id = def.id, progress = 4, customData = mapOf("x_date" to "2024-01-01"))
        val trigger = AchievementTrigger(event = "x", mode = AchievementTriggerMode.SAME_DAY_COUNT, amount = 1)
        val (sameDay, _) = applyTrigger(def, record, trigger, eventAmount = 1, payload = mapOf("date" to "2024-01-01"))
        assertEquals(5, sameDay.progress)
        val (newDay, _) = applyTrigger(def, record, trigger, eventAmount = 1, payload = mapOf("date" to "2024-01-02"))
        assertEquals(1, newDay.progress)
    }

    @Test
    fun applyTrigger_dailyStreak_incrementsOnConsecutiveDay() {
        val def = defWithTarget(target = 0)
        val record = AchievementRecord(id = def.id, progress = 3, customData = mapOf("x_date" to "2024-01-05"))
        val trigger = AchievementTrigger(event = "x", mode = AchievementTriggerMode.DAILY_STREAK, amount = 1)
        val (consecutive, _) = applyTrigger(def, record, trigger, eventAmount = 1, payload = mapOf("date" to "2024-01-06"))
        assertEquals(4, consecutive.progress)
        val (gap, _) = applyTrigger(def, record, trigger, eventAmount = 1, payload = mapOf("date" to "2024-01-10"))
        assertEquals(1, gap.progress)
    }

    @Test
    fun applyTrigger_reset_zeroesProgress() {
        val def = defWithTarget(target = 0)
        val record = AchievementRecord(id = def.id, progress = 9)
        val trigger = AchievementTrigger(event = "x", mode = AchievementTriggerMode.RESET)
        val (next, unlockedNow) = applyTrigger(def, record, trigger, eventAmount = 1, payload = emptyMap())
        assertEquals(0, next.progress)
        assertFalse(unlockedNow)
    }

    @Test
    fun applyTrigger_unlock_signalsOnlyOnFirstTransition() {
        val def = defWithTarget(target = 0)
        val record = AchievementRecord(id = def.id)
        val trigger = AchievementTrigger(event = "x", mode = AchievementTriggerMode.UNLOCK)
        val (first, unlockedNow1) = applyTrigger(def, record, trigger, eventAmount = 1, payload = emptyMap())
        assertTrue(unlockedNow1)
        assertTrue(first.unlocked)
        assertTrue(first.unlockedAtMillis != null)
        // Second call on an already-unlocked record must NOT report a new unlock.
        val (second, unlockedNow2) = applyTrigger(def, first, trigger, eventAmount = 1, payload = emptyMap())
        assertFalse(unlockedNow2)
        assertEquals(first.unlockedAtMillis, second.unlockedAtMillis)
    }

    @Test
    fun applyTrigger_unlock_autoUnlocksWhenTargetReached() {
        val def = defWithTarget(target = 2)
        val record = AchievementRecord(id = def.id, progress = 1)
        val trigger = AchievementTrigger(event = "x", mode = AchievementTriggerMode.INCREMENT, amount = 1)
        val (next, unlockedNow) = applyTrigger(def, record, trigger, eventAmount = 1, payload = emptyMap())
        assertEquals(2, next.progress)
        assertTrue(next.unlocked)
        assertTrue(unlockedNow)
    }

    private fun payloadOf(vararg pairs: Pair<String, String>): Map<String, String> = mapOf(*pairs)
}
