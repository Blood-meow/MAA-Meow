package com.aliothmoon.maameow

import com.aliothmoon.maameow.data.achievement.AchievementDefinition
import com.aliothmoon.maameow.utils.JsonUtils
import java.io.File
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract tests for `app/src/main/assets/achievements.json`.
 *
 * Catches three classes of breakage that the production `loadDefinitions()`
 * silently downgrades to "empty achievement system":
 *   1. JSON unparseable or schema-drift (any field rename / type change in
 *      [AchievementDefinition] that is no longer representable in the asset).
 *   2. Duplicate `id` values, which would cause the second definition to
 *      overwrite the first inside `definitions: Map<String, _>`.
 *   3. `trigger.event` strings that no longer correspond to a real
 *      [com.aliothmoon.maameow.data.achievement.AchievementEvents] constant.
 *      The production matcher would simply skip such triggers, so the affected
 *      achievement becomes permanently un-unlockable with no warning.
 */
class AchievementJsonContractTest {
    @Test
    fun achievementsJson_isParseableIntoAchievementDefinitionList() {
        val raw = resolveAchievementsAsset().readText()
        val parsed = runCatching {
            JsonUtils.common.decodeFromString(
                ListSerializer(AchievementDefinition.serializer()),
                raw,
            )
        }.getOrElse {
            throw AssertionError("achievements.json failed to parse as List<AchievementDefinition>", it)
        }
        assertTrue("Expected at least one achievement in assets/achievements.json", parsed.isNotEmpty())
    }

    @Test
    fun achievementsJson_allIdsAreUnique() {
        val definitions = parseDefinitions()
        val duplicates = definitions.groupBy { it.id }
            .filter { (_, group) -> group.size > 1 }
            .keys
        assertTrue(
            "Duplicate achievement ids in achievements.json: $duplicates",
            duplicates.isEmpty(),
        )
    }

    @Test
    fun achievementsJson_allTriggerEventsAreKnownAchievementEventsConstants() {
        val definitions = parseDefinitions()
        val knownEvents = AchievementEventsConstantNames.all
        val unknown = mutableListOf<Pair<String, String>>()
        definitions.forEach { def ->
            val allTriggers = buildList {
                def.trigger?.let(::add)
                addAll(def.triggers)
            }
            allTriggers.forEach { trigger ->
                if (trigger.event !in knownEvents) {
                    unknown += def.id to trigger.event
                }
            }
        }
        assertTrue(
            "Trigger events not declared in AchievementEvents: $unknown",
            unknown.isEmpty(),
        )
    }

    private fun parseDefinitions(): List<AchievementDefinition> {
        val raw = resolveAchievementsAsset().readText()
        return JsonUtils.common.decodeFromString(
            ListSerializer(AchievementDefinition.serializer()),
            raw,
        )
    }

    private fun resolveAchievementsAsset(): File {
        val candidates = listOf(
            File("app/src/main/assets/achievements.json"),
            File("src/main/assets/achievements.json"),
            File("../app/src/main/assets/achievements.json"),
        )
        val file = candidates.firstOrNull { it.isFile }
        assertNotNull(
            "achievements.json not found. Tried: ${candidates.joinToString { it.path }}",
            file,
        )
        return file!!
    }
}

/**
 * Reflective snapshot of every `const val` declared inside
 * [com.aliothmoon.maameow.data.achievement.AchievementEvents]. Recomputed at
 * class-load time so adding a new event in `AchievementEvents.kt` is picked up
 * here automatically (as long as it's a `const val`).
 */
private object AchievementEventsConstantNames {
    val all: Set<String> = run {
        val cls = Class.forName("com.aliothmoon.maameow.data.achievement.AchievementEvents")
        cls.declaredFields
            .filter { it.type == String::class.java }
            .filter { java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .mapNotNull { field ->
                runCatching { field.get(null) as? String }.getOrNull()
            }
            .toSet()
    }
}
