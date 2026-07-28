package com.aliothmoon.maameow.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aliothmoon.maameow.data.model.toolbox.DepotItem
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import timber.log.Timber
import java.io.IOException
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/**
 * 单个库存分桶的仓库快照。
 *
 * @param items itemId -> 数量
 * @param syncTimeMillis 上次「完整仓库识别」的时间戳；0 表示从未识别过。
 *   掉落累加不更新此值 —— 它表示识别时间，不是数据变更时间。
 */
@Serializable
data class DepotSnapshot(
    val items: Map<String, Int> = emptyMap(),
    val syncTimeMillis: Long = 0L,
)

data class DepotAccountSnapshot(
    val accountTag: String,
    val snapshot: DepotSnapshot,
) {
    val itemKinds: Int get() = snapshot.items.size
    val totalCount: Int get() = snapshot.items.values.sum()
}

/**
 * 仓库数据：按「用户配置档 + 游戏账号标签」分片持久化，运行时以**内存快照**为权威读取源。
 *
 * ## 为何内存写穿
 *
 * MaaCore 回调线程上识别完成 / 掉落后，下一条 Fight 的 `TaskChainStart` 会立刻
 * [countOf] 重算缺口。若只写 DataStore 再等 `store.data` 传播，与同步回调之间
 * **没有 happens-before**，会读到旧库存并多刷。
 *
 * 约定：
 * - [replaceAllSync] / [applyDropsSync]（及对应 suspend 封装）**先**原子更新内存，
 *   **再**串行异步落盘；[countOf] / [snapshot] 只读内存。
 * - 不在 MaaCore 回调线程 `runBlocking` 等 DataStore。
 * - 某档在 [dirty] 中时忽略来自磁盘的同档覆盖，避免慢落盘写回冲掉更新的内存。
 * - 切配置档/账号标签时从磁盘（或已有内存分片）hydrate。
 */
class DepotRepository(
    private val store: DataStore<Preferences>,
    private val taskChainState: TaskChainState,
) {
    private val json = JsonUtils.common
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val memoryLock = Any()
    private val persistMutex = Mutex()

    /** 最近一次 DataStore 快照；分析未来账号分桶时用于懒加载未激活过的 shard。 */
    @Volatile
    private var latestPrefs: Preferences = emptyPreferences()

    /** shardKey(profileId, accountTag) → 最新已知快照（含尚未落盘的变更） */
    private val shards = ConcurrentHashMap<String, DepotSnapshot>()

    /** 当前激活账号库存标签；由任务段 WakeUp.accountName 驱动。空/未配置表示不读写任何库存桶。 */
    private val activeAccountTag = MutableStateFlow<String?>(null)

    /** 有未确认落盘的分片；落盘成功且内存未再变时清除 */
    private val dirty = ConcurrentHashMap.newKeySet<String>()

    private val _snapshot = MutableStateFlow(DepotSnapshot())
    private val initialLoadComplete = MutableStateFlow(false)

    /**
     * 当前活跃配置档的仓库快照（内存权威）。
     * UI 与 [countOf] 均读此流；构造后即由 disk/profile 收集器维护。
     */
    val snapshot: StateFlow<DepotSnapshot> = _snapshot.asStateFlow()

    init {
        scope.launch {
            combine(taskChainState.activeProfileId, activeAccountTag, store.data) { profileId, accountTag, prefs ->
                Triple(profileId, accountTag, prefs)
            }
                .catch { e ->
                    if (e is IOException) {
                        Timber.e(e, "读取仓库数据失败")
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

    /** 等待 DataStore 首帧，避免应用冷启动时把已有账号桶误判为空。 */
    suspend fun awaitInitialLoad() {
        initialLoadComplete.first { it }
    }

    /** 订阅配置档删除事件以清理其分片。 */
    fun start() {
        scope.launch {
            taskChainState.profileDeleted.collect { dropProfile(it) }
        }
    }
    /** 切换当前库存分桶。空白账号不绑定库存；同 profile 下不同账号库存互不污染。 */
    fun activateAccountTag(accountTag: String?) {
        val tag = normalizeAccountTagOrNull(accountTag)
        activeAccountTag.value = tag
        val profileId = taskChainState.activeProfileId.value
        synchronized(memoryLock) {
            if (profileId.isEmpty() || tag == null) {
                _snapshot.value = DepotSnapshot()
                return
            }
            val key = shardKey(profileId, tag)
            val snap = if (key in dirty) {
                shards[key] ?: DepotSnapshot()
            } else {
                decode(latestPrefs[keyOf(key)] ?: legacyRawIfDefault(profileId, tag, latestPrefs))
                    .also { shards[key] = it }
            }
            _snapshot.value = snap
        }
    }

    /** 启动任务前同步绑定并 hydrate 目标账号，避免回调先于 Flow 收集器写入空快照。 */
    suspend fun activateAccountTagAndAwait(accountTag: String?) {
        awaitInitialLoad()
        val prefs = store.data.first().also { latestPrefs = it }
        val tag = normalizeAccountTagOrNull(accountTag)
        activeAccountTag.value = tag
        val profileId = taskChainState.activeProfileId.value
        synchronized(memoryLock) {
            if (profileId.isEmpty() || tag == null) {
                _snapshot.value = DepotSnapshot()
                return
            }
            val key = shardKey(profileId, tag)
            val snap = if (key in dirty) {
                shards[key] ?: DepotSnapshot()
            } else {
                decode(prefs[keyOf(key)] ?: legacyRawIfDefault(profileId, tag, prefs))
                    .also { shards[key] = it }
            }
            _snapshot.value = snap
        }
    }


    /**
     * 仓库识别完成：全量覆盖并刷新识别时间（**同步更新内存**，再异步落盘）。
     * 不过滤排除集 —— 识别结果就是仓库事实。
     */
    fun replaceAllSync(items: List<DepotItem>) {
        val counts = items.associate { it.id to it.count }
        Timber.i(
            "DepotRecognition: replace all kinds=%d orundum=%d headhuntingPermit=%d tenRollPermit=%d",
            counts.size,
            counts["4003"] ?: 0,
            counts["7003"] ?: 0,
            counts["7004"] ?: 0,
        )
        mutateActive { _ ->
            DepotSnapshot(
                items = counts,
                syncTimeMillis = System.currentTimeMillis(),
            )
        }
    }

    suspend fun replaceAll(items: List<DepotItem>) {
        taskChainState.isLoaded.first { it }
        replaceAllSync(items)
        awaitPersistForActive()
    }

    /**
     * 关卡掉落增量累加（**同步更新内存**）。
     * 只累加 add > 0、过滤排除项，且**不更新** syncTimeMillis。
     */
    fun applyDropsSync(drops: List<Pair<String, Int>>) {
        val valid = drops.filter { (itemId, add) -> add > 0 && !shouldExclude(itemId) }
        if (valid.isEmpty()) return
        mutateActive { current ->
            val merged = current.items.toMutableMap()
            for ((itemId, add) in valid) {
                merged[itemId] = (merged[itemId] ?: 0) + add
            }
            current.copy(items = merged)
        }
    }

    suspend fun applyDrops(drops: List<Pair<String, Int>>) {
        taskChainState.isLoaded.first { it }
        applyDropsSync(drops)
        awaitPersistForActive()
    }

    /** 当前库存数量；无快照或无该材料一律返回 0（对齐上游语义）。读内存，不读未传播的磁盘。 */
    fun countOf(itemId: String): Int = snapshot.value.items[itemId] ?: 0

    /** 指定账号标签下的库存数量；用于任务分析阶段按即将启动的账号分桶计算缺口。 */
    fun countOf(itemId: String, accountTag: String?): Int {
        val profileId = taskChainState.activeProfileId.value
        if (profileId.isEmpty()) return 0
        val tag = normalizeAccountTagOrNull(accountTag) ?: return 0
        val key = shardKey(profileId, tag)
        return synchronized(memoryLock) {
            val cached = shards[key]
            if (cached != null) {
                cached.items[itemId] ?: 0
            } else {
                val diskSnap = decode(latestPrefs[keyOf(key)] ?: legacyRawIfDefault(profileId, tag, latestPrefs))
                shards[key] = diskSnap
                diskSnap.items[itemId] ?: 0
            }
        }
    }

    /** 指定账号标签下的仓库识别时间；空账号视为从未识别。 */
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

    /** 设置页查看：列出当前配置档下所有已绑定账号的库存分片。空账号不会出现在这里。 */
    suspend fun listAccountSnapshotsForActiveProfile(): List<DepotAccountSnapshot> {
        taskChainState.isLoaded.first { it }
        val profileId = taskChainState.activeProfileId.value
        if (profileId.isEmpty()) return emptyList()
        val prefix = "depot_${profileId}_"
        val prefs = store.data.first()
        latestPrefs = prefs
        val fromDisk = prefs.asMap().entries.mapNotNull { (prefKey, rawValue) ->
            val name = prefKey.name
            if (!name.startsWith(prefix)) return@mapNotNull null
            val encodedTag = name.removePrefix(prefix)
            val tag = decodeTag(encodedTag)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val raw = rawValue as? String ?: return@mapNotNull null
            val snap = decode(raw)
            val key = shardKey(profileId, tag)
            val current = synchronized(memoryLock) {
                if (key !in dirty) shards[key] = snap
                shards[key] ?: snap
            }
            DepotAccountSnapshot(accountTag = tag, snapshot = current)
        }
        val fromMemory = synchronized(memoryLock) {
            shards.entries.mapNotNull { (key, snap) ->
                if (!key.startsWith(shardKeyPrefix(profileId))) return@mapNotNull null
                val encodedTag = key.removePrefix(shardKeyPrefix(profileId))
                val tag = decodeTag(encodedTag)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                DepotAccountSnapshot(accountTag = tag, snapshot = snap)
            }
        }
        return (fromDisk + fromMemory)
            .groupBy { it.accountTag }
            .map { (_, rows) -> rows.last() }
            .filter { it.snapshot.items.isNotEmpty() || it.snapshot.syncTimeMillis > 0L }
            .sortedBy { it.accountTag }
    }

    /** 配置档被删除时清理其分片（内存 + 磁盘）。 */
    suspend fun dropProfile(profileId: String) {
        if (profileId.isEmpty()) return
        synchronized(memoryLock) {
            val prefix = shardKeyPrefix(profileId)
            shards.keys.removeIf { it.startsWith(prefix) }
            dirty.removeIf { it.startsWith(prefix) }
            if (taskChainState.activeProfileId.value == profileId) {
                _snapshot.value = DepotSnapshot()
            }
        }
        try {
            store.edit { prefs ->
                prefs.asMap().keys
                    .filter { it.name.startsWith("depot_${profileId}_") || it.name == "depot_$profileId" }
                    .forEach { prefs.remove(it) }
            }
        } catch (e: IOException) {
            Timber.w(e, "删除仓库分片失败: %s", profileId)
        }
    }

    private fun onDiskOrProfileChanged(profileId: String, accountTag: String?, prefs: Preferences) {
        if (profileId.isEmpty() || accountTag == null) {
            synchronized(memoryLock) {
                _snapshot.value = DepotSnapshot()
            }
            return
        }
        val key = shardKey(profileId, accountTag)
        val diskSnap = decode(prefs[keyOf(key)] ?: legacyRawIfDefault(profileId, accountTag, prefs))
        synchronized(memoryLock) {
            if (key in dirty) {
                // 本地更新尚未落盘确认：内存优先，只保证对外 snapshot 指向内存分片
                _snapshot.value = shards[key] ?: DepotSnapshot()
                return
            }
            shards[key] = diskSnap
            if (taskChainState.activeProfileId.value == profileId && activeAccountTag.value == accountTag) {
                _snapshot.value = diskSnap
            }
        }
    }

    private fun mutateActive(transform: (DepotSnapshot) -> DepotSnapshot) {
        val profileId = taskChainState.activeProfileId.value
        if (profileId.isEmpty()) {
            Timber.w("活跃配置档为空，跳过仓库写入")
            return
        }
        val accountTag = activeAccountTag.value
        if (accountTag == null) {
            Timber.w("账号切换为空，跳过仓库写入")
            synchronized(memoryLock) { _snapshot.value = DepotSnapshot() }
            return
        }
        val key = shardKey(profileId, accountTag)
        val next: DepotSnapshot
        synchronized(memoryLock) {
            val current = shards[key] ?: DepotSnapshot()
            next = transform(current)
            shards[key] = next
            dirty.add(key)
            _snapshot.value = next
        }
        scope.launch { persistShard(key) }
    }

    private suspend fun awaitPersistForActive() {
        taskChainState.isLoaded.first { it }
        val profileId = taskChainState.activeProfileId.value
        if (profileId.isEmpty()) return
        val accountTag = activeAccountTag.value ?: return
        val key = shardKey(profileId, accountTag)
        // 有限次推进落盘；IO 失败时保留 dirty/内存，避免死等
        repeat(8) {
            if (key !in dirty) return
            persistShard(key)
        }
    }

    /** 停止任务前等待所有已接收的仓库/掉落变更落盘。IO 失败时仍保留 dirty 内存。 */
    suspend fun awaitPendingWrites() {
        val pending = dirty.toList()
        for (key in pending) {
            persistShard(key)
        }
    }

    private suspend fun persistShard(key: String) {
        persistMutex.withLock {
            while (key in dirty) {
                val snap = synchronized(memoryLock) { shards[key] } ?: run {
                    dirty.remove(key)
                    return@withLock
                }
                try {
                    store.edit { prefs ->
                        prefs[keyOf(key)] = json.encodeToString(snap)
                    }
                    synchronized(memoryLock) {
                        // 落盘期间若内存又变了，保持 dirty 再写一轮
                        if (shards[key] == snap) {
                            dirty.remove(key)
                        }
                    }
                } catch (e: IOException) {
                    Timber.e(e, "写入仓库分片失败: %s", key)
                    // 保留 dirty，避免误从磁盘 hydrate 冲掉内存；下次写入再试
                    return@withLock
                }
            }
        }
    }

    private fun decode(raw: String?): DepotSnapshot {
        if (raw.isNullOrEmpty()) return DepotSnapshot()
        return runCatching { json.decodeFromString<DepotSnapshot>(raw) }
            .getOrElse {
                Timber.e(it, "解析仓库分片失败，回退为空快照")
                DepotSnapshot()
            }
    }

    // 空串的 all{} 恒真，故 isEmpty 判断不可省；只认 ASCII 数字，避免 isDigit 放行阿拉伯-印度数字
    private fun shouldExclude(itemId: String): Boolean =
        itemId.isEmpty() || !itemId.all { it in '0'..'9' } || itemId in EXCLUDED_ITEM_IDS

    companion object {
        private val Context.depotStore: DataStore<Preferences> by preferencesDataStore(
            name = "depot",
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        )

        fun create(context: Context, taskChainState: TaskChainState) =
            DepotRepository(context.depotStore, taskChainState)

        const val DEFAULT_ACCOUNT_TAG = "default"

        fun normalizeAccountTagOrNull(accountTag: String?): String? =
            accountTag?.trim()?.takeIf { it.isNotEmpty() }

        fun shardKey(profileId: String, accountTag: String): String =
            "${profileId}_${encodeTag(accountTag)}"

        private fun shardKeyPrefix(profileId: String) = "${profileId}_"

        private fun keyOf(shardKey: String) = stringPreferencesKey("depot_$shardKey")

        private fun legacyRawIfDefault(profileId: String, accountTag: String, prefs: Preferences): String? =
            if (accountTag == DEFAULT_ACCOUNT_TAG || accountTag.endsWith(":$DEFAULT_ACCOUNT_TAG")) {
                prefs[stringPreferencesKey("depot_$profileId")]
            } else {
                null
            }


        private fun encodeTag(tag: String): String = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(tag.toByteArray(Charsets.UTF_8))

        private fun decodeTag(encoded: String): String? = runCatching {
            String(Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8)
        }.getOrNull()

        /**
         * 掉落累加排除集，对齐上游 ToolboxViewModel.ExcludedItemIds。
         * 注意与 ItemHelper.excludedValues（掉落材料下拉过滤）语义不同，不可混用。
         */
        private val EXCLUDED_ITEM_IDS = setOf(
            "3401",                 // 家具
            "3112", "3113", "3114", // 碳
            "5001",                 // 经验
        )
    }
}
