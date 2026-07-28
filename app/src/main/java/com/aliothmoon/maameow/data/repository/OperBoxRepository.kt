package com.aliothmoon.maameow.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aliothmoon.maameow.data.model.toolbox.OperBoxOperator
import com.aliothmoon.maameow.data.preferences.TaskChainState
import com.aliothmoon.maameow.utils.JsonUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import timber.log.Timber
import java.io.IOException
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/**
 * 单个账号分桶的干员箱快照。
 *
 * @param owned 已拥有干员
 * @param notOwned 未拥有干员（识别完成时相对全图差集）
 * @param syncTimeMillis 上次完整干员识别时间；0 表示从未识别
 */
@Serializable
data class OperBoxSnapshot(
    val owned: List<OperBoxOperator> = emptyList(),
    val notOwned: List<OperBoxOperator> = emptyList(),
    val syncTimeMillis: Long = 0L,
) {
    val hasSynced: Boolean get() = syncTimeMillis > 0L
}

/**
 * 干员识别结果持久化，按「用户配置档 + 游戏账号标签」分片，与 [DepotRepository] 对称。
 * 空账号不读写，避免多账号场景误把干员箱当全局数据。
 */
class OperBoxRepository(
    private val store: DataStore<Preferences>,
    private val taskChainState: TaskChainState,
) {
    private val json = JsonUtils.common
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val memoryLock = Any()

    @Volatile
    private var latestPrefs: Preferences = emptyPreferences()

    private val shards = ConcurrentHashMap<String, OperBoxSnapshot>()
    private val activeAccountTag = MutableStateFlow<String?>(null)
    private val _snapshot = MutableStateFlow(OperBoxSnapshot())
    private val initialLoadComplete = MutableStateFlow(false)
    val snapshot: StateFlow<OperBoxSnapshot> = _snapshot.asStateFlow()

    init {
        scope.launch {
            combine(taskChainState.activeProfileId, activeAccountTag, store.data) { profileId, accountTag, prefs ->
                Triple(profileId, accountTag, prefs)
            }
                .catch { e ->
                    if (e is IOException) {
                        Timber.e(e, "读取干员箱数据失败")
                        emit(Triple("", null, emptyPreferences()))
                    } else {
                        throw e
                    }
                }
                .collect { (profileId, accountTag, prefs) ->
                    latestPrefs = prefs
                    onDiskOrProfileChanged(profileId, accountTag, prefs)
                    initialLoadComplete.value = true
                }
        }
    }

    /** 等待 DataStore 首帧，避免应用冷启动时把已有账号桶误判为从未识别。 */
    suspend fun awaitInitialLoad() {
        initialLoadComplete.first { it }
    }

    fun start() {
        scope.launch {
            taskChainState.profileDeleted.collect { dropProfile(it) }
        }
    }

    /** 切换当前干员箱分桶。空白账号不绑定干员箱。 */
    fun activateAccountTag(accountTag: String?) {
        activeAccountTag.value = normalizeAccountTagOrNull(accountTag)
    }

    suspend fun replaceAll(owned: List<OperBoxOperator>, notOwned: List<OperBoxOperator>) {
        editSnapshot {
            OperBoxSnapshot(
                owned = owned,
                notOwned = notOwned,
                syncTimeMillis = System.currentTimeMillis(),
            )
        }
    }

    fun syncTimeMillis(accountTag: String?): Long {
        val profileId = taskChainState.activeProfileId.value
        if (profileId.isEmpty()) return 0L
        val tag = normalizeAccountTagOrNull(accountTag) ?: return 0L
        val key = shardKey(profileId, tag)
        return synchronized(memoryLock) {
            val cached = shards[key]
            if (cached != null) {
                cached.syncTimeMillis
            } else {
                val diskSnap = decode(latestPrefs[keyOf(key)] ?: legacyRawIfDefault(profileId, tag, latestPrefs))
                shards[key] = diskSnap
                diskSnap.syncTimeMillis
            }
        }
    }

    suspend fun dropProfile(profileId: String) {
        if (profileId.isEmpty()) return
        synchronized(memoryLock) {
            val prefix = shardKeyPrefix(profileId)
            shards.keys.removeIf { it.startsWith(prefix) }
            if (taskChainState.activeProfileId.value == profileId) {
                _snapshot.value = OperBoxSnapshot()
            }
        }
        try {
            store.edit { prefs ->
                prefs.asMap().keys
                    .filter { it.name.startsWith("operbox_${profileId}_") || it.name == "operbox_$profileId" }
                    .forEach { prefs.remove(it) }
            }
        } catch (e: IOException) {
            Timber.w(e, "删除干员箱分片失败: %s", profileId)
        }
    }

    private fun onDiskOrProfileChanged(profileId: String, accountTag: String?, prefs: Preferences) {
        if (profileId.isEmpty() || accountTag == null) {
            synchronized(memoryLock) { _snapshot.value = OperBoxSnapshot() }
            return
        }
        val key = shardKey(profileId, accountTag)
        val diskSnap = decode(prefs[keyOf(key)] ?: legacyRawIfDefault(profileId, accountTag, prefs))
        synchronized(memoryLock) {
            shards[key] = diskSnap
            if (taskChainState.activeProfileId.value == profileId && activeAccountTag.value == accountTag) {
                _snapshot.value = diskSnap
            }
        }
    }

    private suspend fun editSnapshot(transform: (OperBoxSnapshot) -> OperBoxSnapshot) {
        taskChainState.isLoaded.first { it }
        val profileId = taskChainState.activeProfileId.value
        if (profileId.isEmpty()) {
            Timber.w("活跃配置档为空，跳过干员箱写入")
            return
        }
        val accountTag = activeAccountTag.value
        if (accountTag == null) {
            Timber.w("账号切换为空，跳过干员箱写入")
            synchronized(memoryLock) { _snapshot.value = OperBoxSnapshot() }
            return
        }
        val key = shardKey(profileId, accountTag)
        try {
            store.edit { prefs ->
                val current = synchronized(memoryLock) { shards[key] } ?: decode(
                    prefs[keyOf(key)] ?: legacyRawIfDefault(profileId, accountTag, prefs)
                )
                val next = transform(current)
                prefs[keyOf(key)] = json.encodeToString(next)
                synchronized(memoryLock) {
                    shards[key] = next
                    _snapshot.value = next
                }
            }
        } catch (e: IOException) {
            Timber.e(e, "写入干员箱分片失败: %s", key)
        }
    }

    private fun decode(raw: String?): OperBoxSnapshot {
        if (raw.isNullOrEmpty()) return OperBoxSnapshot()
        return runCatching { json.decodeFromString<OperBoxSnapshot>(raw) }
            .getOrElse {
                Timber.e(it, "解析干员箱分片失败，回退为空快照")
                OperBoxSnapshot()
            }
    }

    companion object {
        private val Context.operBoxStore: DataStore<Preferences> by preferencesDataStore(
            name = "oper_box",
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        )

        fun create(context: Context, taskChainState: TaskChainState) =
            OperBoxRepository(context.operBoxStore, taskChainState)

        private fun normalizeAccountTagOrNull(accountTag: String?): String? =
            accountTag?.trim()?.takeIf { it.isNotEmpty() }

        private fun shardKey(profileId: String, accountTag: String): String =
            "${profileId}_${encodeTag(accountTag)}"

        private fun shardKeyPrefix(profileId: String) = "${profileId}_"

        private fun keyOf(shardKey: String) = stringPreferencesKey("operbox_$shardKey")

        private fun legacyRawIfDefault(profileId: String, accountTag: String, prefs: Preferences): String? =
            if (accountTag == DepotRepository.DEFAULT_ACCOUNT_TAG || accountTag.endsWith(":${DepotRepository.DEFAULT_ACCOUNT_TAG}")) {
                prefs[stringPreferencesKey("operbox_$profileId")]
            } else {
                null
            }

        private fun encodeTag(tag: String): String = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(tag.toByteArray(Charsets.UTF_8))
    }
}
