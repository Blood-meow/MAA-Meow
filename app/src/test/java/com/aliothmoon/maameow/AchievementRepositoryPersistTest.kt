package com.aliothmoon.maameow

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.aliothmoon.maameow.data.achievement.AchievementRecord
import com.aliothmoon.maameow.data.achievement.AchievementRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit test for the disk-write ordering invariant added in 43372a7:
 * [AchievementRepository.persistRecords] must update [AchievementRepository.cachedRecords]
 * only after the [DataStore] `edit { }` call has succeeded. If the
 * `edit { }` throws (e.g. IO error, disk full), the in-memory cache
 * must keep its previous value so the next mutation starts from a
 * state that is still consistent with what is on disk.
 *
 * The test injects a mock [DataStore] via the internal secondary
 * constructor added in 9d0a4de. The companion `Context.store`
 * extension property delegates to a process-wide preferences
 * DataStore bound to a real file, so it is not possible to drive a
 * write failure from outside the production module.
 */
class AchievementRepositoryPersistTest {
    private val context: Context = mockk(relaxed = true)
    private val dataStore: DataStore<Preferences> = mockk(relaxed = true)

    @After
    fun tearDown() {
        // No-op: mocks are scoped to this test instance.
    }

    @Test
    fun persistRecords_updatesCacheOnSuccess() = runBlocking {
        every { context.applicationContext } returns context
        coEvery { dataStore.data } returns flowOf(androidx.datastore.preferences.core.emptyPreferences())
        coEvery { dataStore.edit(any()) } returns androidx.datastore.preferences.core.emptyPreferences()

        val repo = AchievementRepository(context, dataStore)
        repo.awaitInit()

        val original = mapOf("a" to AchievementRecord(id = "a", unlocked = true))
        repo.cachedRecords = original

        val updated = original + ("b" to AchievementRecord(id = "b", unlocked = true))
        repo.persistRecords(updated)

        assertEquals(updated, repo.cachedRecords)
        verify(exactly = 1) { dataStore.edit(any()) }
    }

    @Test
    fun persistRecords_keepsCacheOnDiskFailure() = runBlocking {
        every { context.applicationContext } returns context
        coEvery { dataStore.data } returns flowOf(androidx.datastore.preferences.core.emptyPreferences())
        coEvery { dataStore.edit(any()) } throws IOException("disk full")

        val repo = AchievementRepository(context, dataStore)
        repo.awaitInit()

        val original = mapOf("a" to AchievementRecord(id = "a", unlocked = true))
        repo.cachedRecords = original

        val attempted = original + ("b" to AchievementRecord(id = "b", unlocked = true))

        // persistRecords must not catch the IOException internally; the
        // exception propagates so the surrounding flow (e.g. recordMutex.withLock)
        // can decide how to react. The test asserts both: the exception
        // surfaces, AND the in-memory cache is untouched.
        var caught: Throwable? = null
        try {
            repo.persistRecords(attempted)
        } catch (t: Throwable) {
            caught = t
        }
        assertEquals("disk full", caught?.message)
        assertEquals(original, repo.cachedRecords)
        verify(exactly = 1) { dataStore.edit(any()) }
    }

    @Test
    fun persistRecords_onFreshRepository_setsCacheAfterWrite() = runBlocking {
        every { context.applicationContext } returns context
        coEvery { dataStore.data } returns flowOf(androidx.datastore.preferences.core.emptyPreferences())
        coEvery { dataStore.edit(any()) } returns androidx.datastore.preferences.core.emptyPreferences()

        val repo = AchievementRepository(context, dataStore)
        repo.awaitInit()

        // Sanity: the init load returned an empty map, so the cache is
        // an empty map (not null) before any writes.
        assertNull(repo.cachedRecords?.get("first"))

        val first = mapOf("first" to AchievementRecord(id = "first", unlocked = true))
        repo.persistRecords(first)

        assertEquals(first, repo.cachedRecords)
    }
}

/**
 * Spin-wait until the [AchievementRepository]'s init coroutine has finished
 * loading. The init launches a coroutine on an internal IO scope; tests that
 * drive [AchievementRepository.persistRecords] directly need to make sure the
 * init coroutine has either completed (so its `loadRecords()` write to
 * `cachedRecords` does not race with the test) or never executed (which is
 * not the case here because the constructor always schedules the load).
 */
private fun AchievementRepository.awaitInit() {
    val deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5)
    while (cachedRecords == null && System.nanoTime() < deadline) {
        Thread.sleep(1)
    }
}
