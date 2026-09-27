package com.aliothmoon.maameow.data.preferences

import android.content.Context
import android.content.res.Configuration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aliothmoon.maameow.data.achievement.AchievementEvents
import com.aliothmoon.maameow.data.achievement.AchievementRepository
import com.aliothmoon.maameow.data.model.DepotMaintainConfig
import com.aliothmoon.maameow.data.model.DepotMaintainPlan
import com.aliothmoon.maameow.data.model.InfrastConfig
import com.aliothmoon.maameow.data.model.MallConfig
import com.aliothmoon.maameow.data.model.RecruitConfig
import com.aliothmoon.maameow.data.model.TaskChainNode
import com.aliothmoon.maameow.data.model.TaskParamProvider
import com.aliothmoon.maameow.data.model.TaskProfile
import com.aliothmoon.maameow.data.model.TaskTypeInfo
import com.aliothmoon.maameow.data.model.WakeUpConfig
import com.aliothmoon.maameow.utils.JsonUtils
import com.aliothmoon.maameow.utils.i18n.LocaleBootstrap.resolveSelectedLanguage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.io.IOException
import java.util.Locale
import java.util.UUID


class TaskChainState(
    private val context: Context,
    private val appSettings: AppSettingsManager,
    private val achievementRepository: AchievementRepository,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private val json = JsonUtils.common

    companion object {
        private val Context.store: DataStore<Preferences> by preferencesDataStore(
            name = "task_chain"
        )

        private val PROFILES_KEY = stringPreferencesKey("profiles")
        private val ACTIVE_PROFILE_KEY = stringPreferencesKey("active_profile_id")

        private const val PROFILE_NAME_PREFIX = "配置-"
        private const val MAX_PROFILE_NAME_LENGTH = 20
    }

    private val _chain = MutableStateFlow(buildDefaultChain())
    val chain: StateFlow<List<TaskChainNode>> = _chain.asStateFlow()

    private val _profiles = MutableStateFlow<List<TaskProfile>>(emptyList())
    val profiles: StateFlow<List<TaskProfile>> = _profiles.asStateFlow()

    private val _profileId = MutableStateFlow("")
    val profileId: StateFlow<String> = _profileId.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private val _profileDeleted = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val profileDeleted: SharedFlow<String> = _profileDeleted.asSharedFlow()

    private val _lastUsedClientType = MutableStateFlow<String?>(null)

    /**
     * 串行化 [_chain] / [_profiles] 的读改写。
     *
     * 这两份状态有两个写入方：界面在 Main（改任务配置、库存保持计划），
     * 任务回调在 [Dispatchers.IO]（SubTaskHandler / TaskChainHandler 里的
     * `recordCreditFightCompleted`、`clearRecruitUseExpeditedFlags`）。
     * 都是「读出来 → 改 → 写回去」，不加锁就会互相覆盖：跑任务时保存一条
     * 库存保持计划，可能被同一时刻到达的信用战回调整份吞掉。
     *
     * 锁内只放非挂起代码，所以不会和 [_isLoaded] 互相等待。
     */
    private val chainLock = Mutex()

    /** 在链锁内做一次非挂起的读改写 */
    private suspend fun <T> locked(block: () -> T): T = chainLock.withLock { block() }

    private sealed interface PersistOp {
        data object Sync : PersistOp
        data class Flush(val done: CompletableDeferred<Unit>) : PersistOp
    }

    private val persistOps = Channel<PersistOp>(Channel.UNLIMITED)

    val clientType: String
        get() = getClientTypeOrNull() ?: "Official"

    /** 最近一次成功启动的会话所用客户端；运行中即本次会话的服务器，不随切换 Profile 变 */
    val lastUsedClientType: String?
        get() = _lastUsedClientType.value

    private fun doSync() {
        persistOps.trySend(PersistOp.Sync)
    }

    /**
     * 等到本 Flush 之前的 Sync 全部处理完。
     * 期间若有写盘失败，抛出 [IOException]（[importProfiles] 等「确认落盘」路径依赖此契约）。
     */
    suspend fun flush() {
        val done = CompletableDeferred<Unit>()
        persistOps.send(PersistOp.Flush(done))
        done.await()
    }

    private suspend fun doConsume() {
        var pendingError: IOException? = null
        for (op in persistOps) {
            try {
                when (op) {
                    PersistOp.Sync -> {
                        try {
                            sync()
                            pendingError = null
                        } catch (e: IOException) {
                            Timber.e(e, "写入任务链配置失败")
                            pendingError = e
                        }
                    }

                    is PersistOp.Flush -> {
                        val err = pendingError
                        if (err != null) {
                            pendingError = null
                            op.done.completeExceptionally(err)
                        } else {
                            op.done.complete(Unit)
                        }
                    }
                }
            } catch (e: Throwable) {
                Timber.e(e, "任务链配置持久化队列处理失败")
                if (op is PersistOp.Flush) {
                    op.done.completeExceptionally(e)
                } else if (e is IOException) {
                    pendingError = e
                }
            }
        }
    }

    private suspend fun sync() {
        context.store.edit { prefs ->
            prefs[PROFILES_KEY] = json.encodeToString<List<TaskProfile>>(_profiles.value)
            prefs[ACTIVE_PROFILE_KEY] = _profileId.value
        }
    }

    init {
        syncScope.launch { doConsume() }
        scope.launch {
            try {
                val prefs = context.store.data.first()

                val storedProfiles = prefs[PROFILES_KEY]?.let {
                    runCatching { json.decodeFromString<List<TaskProfile>>(it) }.onFailure { e ->
                        Timber.e(e, "TaskChainState decodeFromString error")
                    }.getOrNull()
                }?.migrated()

                val needsDefaultProfile = storedProfiles.isNullOrEmpty()

                val profiles = storedProfiles?.takeIf { it.isNotEmpty() } ?: listOf(
                    TaskProfile(
                        name = "${PROFILE_NAME_PREFIX}1",
                        chain = buildDefaultChain(),
                    )
                )

                val storedActiveId = prefs[ACTIVE_PROFILE_KEY]

                val activeProfile =
                    profiles.firstOrNull { it.id == storedActiveId } ?: profiles.first()

                val needsSync = needsDefaultProfile || storedActiveId != activeProfile.id

                locked {
                    _profiles.value = profiles
                    _profileId.value = activeProfile.id
                    _chain.value = activeProfile.chain
                    _isLoaded.value = true
                }

                if (needsSync) {
                    doSync()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load profiles")
                _isLoaded.value = true
            }
        }
    }


    suspend fun addNode(typeInfo: TaskTypeInfo, afterIndex: Int = -1): String {
        val nodeId = mutate {
            val node = TaskChainNode(
                id = UUID.randomUUID().toString(),
                name = defaultTaskName(typeInfo),
                enabled = true,
                config = typeInfo.defaultConfig()
            )
            if (afterIndex < 0 || afterIndex >= it.size) {
                it.add(node)
            } else {
                it.add(afterIndex + 1, node)
            }
            Timber.d("Added node: %s (%s)", node.name, typeInfo.name)
            node.id
        }
        achievementRepository.report {
            event = AchievementEvents.TASK_NODE_ADDED
        }
        return nodeId
    }

    suspend fun removeNode(nodeId: String) {
        mutate {
            it.removeAll { iter ->
                iter.id == nodeId
            }
            Timber.d("Removed node: %s", nodeId)
        }
        achievementRepository.report {
            event = AchievementEvents.TASK_NODE_REMOVED
        }
    }

    suspend fun duplicateNode(nodeId: String): String {
        return mutate { current ->
            val idx = current.indexOfFirst { it.id == nodeId }
            if (idx >= 0) {
                val src = current[idx]
                // 去掉末尾 " N"（空格+数字）得到基础名
                val baseName = src.name.replace(Regex(" \\d+$"), "")
                // 收集链中所有以 "baseName N" 形式命名已占用的编号
                val usedNumbers = current.mapNotNull { node ->
                    Regex("^${Regex.escape(baseName)} (\\d+)$").matchEntire(node.name)?.groupValues?.get(
                        1
                    )?.toIntOrNull()
                }.toSet()
                // 取最小未占用的正整数（从 2 开始，1 留给源名称本身）
                val nextNum = generateSequence(2) { it + 1 }.first { it !in usedNumbers }
                val copy = src.copy(
                    id = UUID.randomUUID().toString(), name = "$baseName $nextNum"
                )
                current.add(idx + 1, copy)
                Timber.d("Duplicated node %s → %s (\"%s\")", nodeId, copy.id, copy.name)
                copy.id
            } else {
                Timber.w("duplicateNode: node %s not found", nodeId)
                ""
            }
        }
    }

    suspend fun renameNode(nodeId: String, newName: String) {
        mutate { current ->
            val idx = current.indexOfFirst { it.id == nodeId }
            if (idx >= 0) {
                current[idx] = current[idx].copy(name = newName)
                Timber.d("Renamed node %s to: %s", nodeId, newName)
            } else {
                Timber.w("renameNode: node %s not found", nodeId)
            }
        }
    }

    suspend fun setNodeEnabled(nodeId: String, enabled: Boolean) {
        mutate { current ->
            val idx = current.indexOfFirst { it.id == nodeId }
            if (idx >= 0) {
                current[idx] = current[idx].copy(enabled = enabled)
                Timber.d("Set node %s enabled: %s", nodeId, enabled)
            } else {
                Timber.w("setNodeEnabled: node %s not found", nodeId)
            }
        }
    }

    suspend fun updateNodeConfig(nodeId: String, config: TaskParamProvider) {
        mutate { current ->
            val idx = current.indexOfFirst { it.id == nodeId }
            if (idx >= 0) {
                current[idx] = current[idx].copy(config = config)
            } else {
                Timber.w("updateNodeConfig: node %s not found", nodeId)
            }
        }
    }

    /**
     * 增删改指定配置档的「库存保持」计划。
     *
     * 库存数据页允许直接点物品改目标库存，此时用户并没有停在任务配置页，
     * 所以这里按 profileId 定位链——非活跃档也能改，活跃档走同一条写盘路径。
     * 该档还没有库存保持节点时就地新建一个（启用态，否则计划不会执行）。
     *
     * @param nodeId 计划所属节点。null = 新建计划，取该档第一个库存保持节点（启用优先），
     *   没有就建一个；非 null = 改已有计划，节点必须还在，找不到就整笔放弃——
     *   退回「第一个库存保持节点」会把 [transform] 里的下标套到另一个节点的计划上
     * @param transform 拿到该节点当前的 plans，返回新列表
     * @return 实际写入的节点 ID；配置档或节点不存在时返回空串
     */
    suspend fun updateDepotMaintainPlans(
        profileId: String,
        nodeId: String? = null,
        transform: (List<DepotMaintainPlan>) -> List<DepotMaintainPlan>,
    ): String = updateDepotMaintainConfig(profileId, nodeId) {
        it.copy(plans = transform(it.plans))
    }

    /**
     * 改指定配置档某个「库存保持」节点的整份配置。
     *
     * 计划之外的开关（更新仓库、理智药、自动代理倍率……）原先只能在任务配置页改，
     * 库存数据页的计划面板也要改它们，所以补上这条入口：定位、新建节点、写盘
     * 全部复用同一条路径，两边改的必然是同一条链上的同一个节点。
     *
     * @param nodeId 目标节点。null = 取该档第一个库存保持节点（启用优先），没有就建一个；
     *   非 null = 必须命中该节点，找不到就整笔放弃——退回「第一个库存保持节点」会把
     *   改动落到另一个节点的配置上
     * @param transform 拿到该节点当前的配置，返回新配置；**整份替换**，想保留的字段要自己带上
     * @return 实际写入的节点 ID；配置档或节点不存在时返回空串
     */
    suspend fun updateDepotMaintainConfig(
        profileId: String,
        nodeId: String? = null,
        transform: (DepotMaintainConfig) -> DepotMaintainConfig,
    ): String {
        _isLoaded.first { it }
        return locked {
            val target = _profiles.value.firstOrNull { it.id == profileId } ?: run {
                Timber.w("updateDepotMaintainConfig: profile %s not found", profileId)
                return@locked ""
            }
            // 活跃档的最新链在 _chain 上；非活跃档只能读 _profiles 里那份
            val isActive = profileId == _profileId.value
            val source = if (isActive) _chain.value else target.chain
            val updated = source.applyDepotConfig(nodeId, transform) ?: return@locked ""
            val (nodes, writtenNodeId) = updated
            if (isActive) {
                _chain.value = nodes
            }
            _profiles.value =
                _profiles.value.map { if (it.id == profileId) it.copy(chain = nodes) else it }
            doSync()
            Timber.d("updateDepotMaintainConfig: profile=%s node=%s", profileId, writtenNodeId)
            writtenNodeId
        }
    }

    /** @return null = 目标节点已不存在，调用方应放弃这次写入 */
    private fun List<TaskChainNode>.applyDepotConfig(
        nodeId: String?,
        transform: (DepotMaintainConfig) -> DepotMaintainConfig,
    ): Pair<List<TaskChainNode>, String>? {
        val nodes = toMutableList()
        if (nodeId != null) {
            val idx = nodes.indexOfFirst { it.id == nodeId && it.config is DepotMaintainConfig }
            if (idx < 0) {
                Timber.w("applyDepotConfig: 库存保持节点 %s 已不存在，放弃写入", nodeId)
                return null
            }
            val config = nodes[idx].config as DepotMaintainConfig
            nodes[idx] = nodes[idx].copy(config = transform(config))
            return nodes.toList() to nodes[idx].id
        }
        val existing = nodes.indexOfFirst { it.config is DepotMaintainConfig && it.enabled }
            .takeIf { it >= 0 }
            ?: nodes.indexOfFirst { it.config is DepotMaintainConfig }
        if (existing >= 0) {
            val config = nodes[existing].config as DepotMaintainConfig
            nodes[existing] = nodes[existing].copy(config = transform(config))
            return nodes.toList() to nodes[existing].id
        }
        val node = TaskChainNode(
            id = UUID.randomUUID().toString(),
            name = defaultTaskName(TaskTypeInfo.DEPOT_MAINTAIN),
            enabled = true,
            order = nodes.size,
            config = transform(DepotMaintainConfig()),
        )
        nodes.add(node)
        return nodes.toList() to node.id
    }

    /** 以回调的节点 ID 定位，切换配置后也不会写到另一个信用任务 */
    suspend fun recordCreditFightCompleted(nodeId: String, date: String) {
        updateMallCompletion(nodeId) { it.copy(creditFightLastDate = date) }
    }

    suspend fun recordVisitFriendsCompleted(nodeId: String, date: String) {
        updateMallCompletion(nodeId) { it.copy(visitFriendsLastDate = date) }
    }

    private suspend fun updateMallCompletion(nodeId: String, transform: (MallConfig) -> MallConfig) {
        val update = { node: TaskChainNode ->
            val config = node.config
            if (node.id == nodeId && config is MallConfig) {
                node.copy(config = transform(config))
            } else node
        }
        // 运行中可能已切走 Profile，非当前 Profile 的链也要找
        mutate(others = { chain -> chain.map(update) }) { current -> current.replaceAll { update(it) } }
    }

    suspend fun reorderNodes(fromIndex: Int, toIndex: Int) {
        mutate { current ->
            require(fromIndex in current.indices) { "fromIndex out of bounds: $fromIndex" }
            require(toIndex in current.indices) { "toIndex out of bounds: $toIndex" }
            val node = current.removeAt(fromIndex)
            current.add(toIndex, node)
            Timber.d("Moved node from %d to %d", fromIndex, toIndex)
        }
    }

    suspend fun clearRecruitUseExpeditedFlags() {
        mutate { current ->
            for (i in current.indices) {
                val node = current[i]
                if (!node.enabled) continue
                when (val cfg = node.config) {
                    is RecruitConfig -> if (cfg.useExpedited) {
                        current[i] = node.copy(config = cfg.copy(useExpedited = false))
                        Timber.d(
                            "clearRecruitUseExpeditedFlags on node %s", node.id
                        )
                    }

                    else -> {}
                }
            }
        }
    }

    /**
     * 自定义基建任务链完成后，将目标节点的 planSelect 自动切到下一个计划。
     *
     * 对齐 WPF `InfrastSettingsUserControlModel.IncreaseCustomInfrastPlanIndex`:
     * - 仅 Custom 模式生效
     * - planSelect == -1(时间轮换)不切
     * - planSelect 越界或计划列表未就绪直接放弃
     * - 自增后超出范围回环到 0
     *
     * 返回 Pair(新索引, 新计划名)，若未满足切换条件返回 null。
     */
    suspend fun incrementCustomInfrastPlanSelect(nodeId: String): Pair<Int, String?>? {
        val node = _chain.value.firstOrNull { it.id == nodeId } ?: run {
            Timber.d("incrementCustomInfrastPlanSelect: node %s not found", nodeId)
            return null
        }
        val cfg = node.config as? InfrastConfig ?: return null
        if (cfg.mode != com.aliothmoon.maameow.domain.enums.InfrastMode.Custom) return null
        if (cfg.customInfrastPlanSelect < 0) return null
        val count = cfg.customPlanNames.size
        if (count <= 0) {
            Timber.d("incrementCustomInfrastPlanSelect: plan names empty for node %s", nodeId)
            return null
        }
        if (cfg.customInfrastPlanSelect >= count) return null
        val next = (cfg.customInfrastPlanSelect + 1) % count
        updateNodeConfig(nodeId, cfg.copy(customInfrastPlanSelect = next))
        return next to cfg.customPlanNames.getOrNull(next)
    }

    fun getClientTypeOrNull(): String? {
        return findFirstEnabledConfig<WakeUpConfig>()?.clientType ?: _lastUsedClientType.value
    }

    fun saveLastUsedClientType(clientType: String) {
        _lastUsedClientType.value = clientType
    }

    inline fun <reified T : TaskParamProvider> findFirstEnabledConfig(): T? {
        return chain.value.filter { it.enabled }.firstNotNullOfOrNull { it.config as? T }
    }

    inline fun <reified T : TaskParamProvider> firstEnabledConfigFlow(): Flow<T?> {
        return chain.map { nodes ->
            nodes.filter { it.enabled }.firstNotNullOfOrNull { it.config as? T }
        }
    }

    // ========== Profile 管理 ==========

    suspend fun switchProfile(profileId: String) {
        val currentProfiles = _profiles.value
        val target = currentProfiles.find { it.id == profileId } ?: run {
            Timber.w("switchProfile: profile %s not found", profileId)
            return
        }
        locked {
            // 保存当前链到旧 Profile
            val updatedProfiles = currentProfiles.map { p ->
                if (p.id == _profileId.value) p.copy(chain = _chain.value) else p
            }
            // 加载新 Profile 的链
            _chain.value = target.chain
            _profileId.value = profileId
            _profiles.value = updatedProfiles
            // 持久化
            doSync()
        }
        Timber.d("Switched to profile: %s (%s)", target.name, profileId)
    }

    suspend fun createProfile(): String {
        val newProfileId = locked {
            val currentProfiles = _profiles.value
            // 先保存当前活跃 Profile 的链
            val savedProfiles = currentProfiles.map { p ->
                if (p.id == _profileId.value) p.copy(chain = _chain.value) else p
            }
            val newProfile = TaskProfile(
                name = nextProfileName(savedProfiles), chain = buildDefaultChain()
            )
            // 切换到新 Profile
            _chain.value = newProfile.chain
            _profileId.value = newProfile.id
            _profiles.value = savedProfiles + newProfile
            doSync()
            newProfile.id
        }
        Timber.d("Created profile: %s", newProfileId)
        return newProfileId
    }

    suspend fun removeProfile(profileId: String) {
        val removed = locked {
            val currentProfiles = _profiles.value
            if (currentProfiles.size <= 1) {
                Timber.w("deleteProfile: cannot delete last profile")
                return@locked false
            }
            // 先保存当前链
            val savedProfiles = currentProfiles.map { p ->
                if (p.id == _profileId.value) p.copy(chain = _chain.value) else p
            }
            val remaining = savedProfiles.filter { it.id != profileId }
            if (remaining.size == savedProfiles.size) {
                Timber.w("deleteProfile: profile %s not found", profileId)
                return@locked false
            }
            // 若删除的是活跃 Profile,切换到列表第一个
            val newActiveId = if (_profileId.value == profileId) {
                val first = remaining.first()
                _chain.value = first.chain
                first.id
            } else {
                _profileId.value
            }
            _profileId.value = newActiveId
            _profiles.value = remaining
            doSync()
            true
        }
        if (!removed) return
        _profileDeleted.tryEmit(profileId)
        Timber.d("Deleted profile: %s", profileId)
    }

    suspend fun renameProfile(profileId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_PROFILE_NAME_LENGTH) {
            Timber.w("renameProfile: invalid name length: %d", trimmed.length)
            return
        }
        locked {
            _profiles.value = _profiles.value.map { p ->
                if (p.id == profileId) p.copy(name = trimmed) else p
            }
            doSync()
        }
        Timber.d("Renamed profile %s to: %s", profileId, trimmed)
    }

    suspend fun duplicateProfile(profileId: String): String? {
        val newProfileId = locked {
            val currentProfiles = _profiles.value
            // 先保存当前活跃 Profile 的链
            val savedProfiles = currentProfiles.map { p ->
                if (p.id == _profileId.value) p.copy(chain = _chain.value) else p
            }
            val source = savedProfiles.find { it.id == profileId } ?: run {
                Timber.w("duplicateProfile: profile %s not found", profileId)
                return@locked null
            }
            // 复制链时为每个节点生成新 ID
            val duplicatedChain = source.chain.map { it.copy(id = UUID.randomUUID().toString()) }
            val newProfile = TaskProfile(
                name = nextProfileName(savedProfiles), chain = duplicatedChain
            )
            _profiles.value = savedProfiles + newProfile
            doSync()
            newProfile.id
        }
        Timber.d("Duplicated profile %s as: %s", profileId, newProfileId)
        return newProfileId
    }

    suspend fun reorderProfiles(fromIndex: Int, toIndex: Int) {
        val current = _profiles.value
        if (fromIndex !in current.indices || toIndex !in current.indices) {
            Timber.w(
                "reorderProfiles: invalid index from=%d to=%d size=%d",
                fromIndex,
                toIndex,
                current.size
            )
            return
        }
        if (fromIndex == toIndex) return

        locked {
            // 顺便把当前未保存的链快照写回 active profile, 避免重排时丢失正在编辑的内容
            val savedProfiles = _profiles.value.map { p ->
                if (p.id == _profileId.value) p.copy(chain = _chain.value) else p
            }.toMutableList()
            val moved = savedProfiles.removeAt(fromIndex)
            savedProfiles.add(toIndex, moved)

            _profiles.value = savedProfiles
            doSync()
        }
        Timber.d("Reordered profile from %d to %d", fromIndex, toIndex)
    }

    // ========== 内部工具方法 ==========

    /** 改链统一走这里；[others] 非空时顺带改非当前 Profile 的链 */
    private suspend inline fun <T> mutate(
        noinline others: ((List<TaskChainNode>) -> List<TaskChainNode>)? = null,
        crossinline block: (MutableList<TaskChainNode>) -> T
    ): T {
        _isLoaded.first { it }
        return locked {
            val current = _chain.value.toMutableList()
            val ret = block(current)
            reindex(current)
            val snapshot = current.toList()
            _chain.value = snapshot
            _profiles.value = _profiles.value.map { p ->
                when {
                    p.id == _profileId.value -> p.copy(chain = snapshot)
                    others != null -> p.copy(chain = others(p.chain))
                    else -> p
                }
            }
            doSync()
            ret
        }
    }

    private fun reindex(nodes: MutableList<TaskChainNode>) {
        for (i in nodes.indices) {
            nodes[i] = nodes[i].copy(order = i)
        }
    }

    private fun buildDefaultChain(): List<TaskChainNode> {
        return TaskTypeInfo.entries.filter { it.inDefaultChain }.mapIndexed { index, info ->
            TaskChainNode(
                name = defaultTaskName(info),
                enabled = false,
                order = index,
                config = info.defaultConfig()
            )
        }
    }

    private fun defaultTaskName(typeInfo: TaskTypeInfo): String {
        val tag = resolveSelectedLanguage(appSettings.language.value).tag
        val localizedContext = context.createConfigurationContext(
            Configuration(context.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(tag))
            })
        return typeInfo.defaultName(localizedContext)
    }

    suspend fun importProfiles(rawProfiles: List<TaskProfile>, activeId: String) {
        val profiles = rawProfiles.migrated()
        val resolvedActiveId =
            profiles.find { it.id == activeId }?.id ?: profiles.firstOrNull()?.id ?: return
        val activeChain = profiles.find { it.id == resolvedActiveId }?.chain ?: buildDefaultChain()
        locked {
            _profiles.value = profiles
            _profileId.value = resolvedActiveId
            _chain.value = activeChain
            doSync()
        }
        // 导入是用户可见的终态操作（随后会提示「导入成功」），必须确认落盘再返回
        flush()
        Timber.d("Imported %d profiles, active: %s", profiles.size, resolvedActiveId)
    }

    /** 旧配置迁移，两个解码入口（DataStore 载入与导入备份）共用 */
    private fun List<TaskProfile>.migrated(): List<TaskProfile> = map { profile ->
        profile.copy(chain = profile.chain.map { it.copy(config = it.config.migrate()) })
    }

    private fun nextProfileName(profiles: List<TaskProfile>): String {
        val maxNum = profiles.mapNotNull { p ->
            if (p.name.startsWith(PROFILE_NAME_PREFIX)) {
                p.name.removePrefix(PROFILE_NAME_PREFIX).toIntOrNull()
            } else {
                null
            }
        }.maxOrNull() ?: 0
        return "$PROFILE_NAME_PREFIX${maxNum + 1}"
    }
}
