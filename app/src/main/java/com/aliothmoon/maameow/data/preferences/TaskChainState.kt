package com.aliothmoon.maameow.data.preferences

import android.content.Context
import android.content.res.Configuration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.constant.Packages
import com.aliothmoon.maameow.data.achievement.AchievementEvents
import com.aliothmoon.maameow.data.achievement.AchievementRepository
import com.aliothmoon.maameow.data.model.InfrastConfig
import com.aliothmoon.maameow.data.model.RecruitConfig
import com.aliothmoon.maameow.data.model.ProfileSequenceEntry
import com.aliothmoon.maameow.data.model.TaskChainNode
import com.aliothmoon.maameow.data.model.TaskParamProvider
import com.aliothmoon.maameow.data.model.TaskProfile
import com.aliothmoon.maameow.data.model.TaskSequenceConfig
import com.aliothmoon.maameow.data.model.TaskTypeInfo
import com.aliothmoon.maameow.data.model.WakeUpConfig
import com.aliothmoon.maameow.manager.RemoteServiceManager
import com.aliothmoon.maameow.remote.PermissionGrantRequest
import com.aliothmoon.maameow.utils.JsonUtils
import com.aliothmoon.maameow.utils.i18n.LocaleBootstrap.resolveSelectedLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.Locale
import java.util.UUID

class TaskChainState(
    private val context: Context,
    private val appSettings: AppSettingsManager,
    private val achievementRepository: AchievementRepository,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = JsonUtils.common
    /** 串行化任务链增删改，避免多协程 read-modify-write 丢项 */
    private val sequenceMutex = Mutex()

    companion object {
        private val Context.store: DataStore<Preferences> by preferencesDataStore(
            name = "task_chain"
        )
        private val CHAIN_KEY = stringPreferencesKey("chain")
        private val PROFILES_KEY = stringPreferencesKey("profiles")
        private val ACTIVE_PROFILE_KEY = stringPreferencesKey("active_profile_id")
        private val PROFILE_SEQUENCE_KEY = stringPreferencesKey("profile_sequence")
        private val SEQUENCE_CONFIGS_KEY = stringPreferencesKey("sequence_configs")
        private val ACTIVE_SEQUENCE_CONFIG_KEY =
            stringPreferencesKey("active_sequence_config_id")
        private val PROFILE_SEQUENCE_ENABLED_KEY =
            booleanPreferencesKey("profile_sequence_enabled")

        /** 用户配置名称长度上限（UI 与持久化共用） */
        const val MAX_PROFILE_NAME_LENGTH = 20
        /** 任务节点名称长度上限（UI） */
        const val MAX_NODE_NAME_LENGTH = 20
        /** 任务链配置名称长度上限（UI 与持久化共用） */
        const val MAX_SEQUENCE_NAME_LENGTH = 20
        /** 单套任务链最大条目数（UI 与持久化共用） */
        const val MAX_SEQUENCE_ENTRIES = 20
    }

    private val _chain = MutableStateFlow(buildDefaultChain())
    val chain: StateFlow<List<TaskChainNode>> = _chain.asStateFlow()

    private val _profiles = MutableStateFlow<List<TaskProfile>>(emptyList())
    val profiles: StateFlow<List<TaskProfile>> = _profiles.asStateFlow()

    private val _activeProfileId = MutableStateFlow("")
    val activeProfileId: StateFlow<String> = _activeProfileId.asStateFlow()

    /** 多套任务链配置（与用户 TaskProfile 无关） */
    private val _sequenceConfigs = MutableStateFlow<List<TaskSequenceConfig>>(emptyList())
    val sequenceConfigs: StateFlow<List<TaskSequenceConfig>> = _sequenceConfigs.asStateFlow()

    private val _activeSequenceConfigId = MutableStateFlow("")
    val activeSequenceConfigId: StateFlow<String> = _activeSequenceConfigId.asStateFlow()

    /**
     * 当前激活任务链配置的条目列表（一级页与弹窗共用）。
     * 兼容旧代码/备份字段命名。
     */
    private val _profileSequence = MutableStateFlow<List<ProfileSequenceEntry>>(emptyList())
    val profileSequence: StateFlow<List<ProfileSequenceEntry>> = _profileSequence.asStateFlow()

    /** 手动开始是否按任务链拼接；关闭时始终只跑当前激活用户配置 */
    private val _profileSequenceEnabled = MutableStateFlow(true)
    val profileSequenceEnabled: StateFlow<Boolean> = _profileSequenceEnabled.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    /**
     * 配置档被删除事件（携带 profileId）。
     * 供 DepotRepository 等按 profileId 分片存储的组件清理数据；
     * 用单向事件流而非直接调用，避免与本类形成循环依赖。
     */
    private val _profileDeleted = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val profileDeleted: SharedFlow<String> = _profileDeleted.asSharedFlow()

    private val _lastUsedClientType = MutableStateFlow<String?>(null)

    /** Client of the segment currently running (or last successfully started in this process). */
    private val _activeClientType = MutableStateFlow<String?>(null)

    init {
        scope.launch {
            val prefs = context.store.data.first()
            val profilesJson = prefs[PROFILES_KEY]

            if (profilesJson != null) {
                // 已有 Profile 数据
                val loadedProfiles = decodeProfiles(profilesJson)
                val activeId = prefs[ACTIVE_PROFILE_KEY] ?: loadedProfiles.firstOrNull()?.id ?: ""
                _profiles.value = loadedProfiles
                _activeProfileId.value = activeId
                val activeProfile = loadedProfiles.find { it.id == activeId }
                    ?: loadedProfiles.firstOrNull()
                if (activeProfile != null) {
                    _activeProfileId.value = activeProfile.id
                    _chain.value = activeProfile.chain
                }
                val validIds = loadedProfiles.map { it.id }.toSet()
                loadSequenceConfigsFromPrefs(prefs, validIds)
                // 缺省 true：兼容旧数据与既有「非空即按链跑」行为
                _profileSequenceEnabled.value =
                    prefs[PROFILE_SEQUENCE_ENABLED_KEY] ?: true
            } else {
                // 迁移: 旧版数据无 profiles key, 将现有 chain 包装为单个 Profile
                val legacyChain = decodeChain(prefs[CHAIN_KEY])
                val profile = TaskProfile(
                    name = profileDefaultName(),
                    chain = legacyChain
                )
                _profiles.value = listOf(profile)
                _activeProfileId.value = profile.id
                _chain.value = legacyChain
                val defaultSeq = defaultSequenceConfig()
                applySequenceConfigs(listOf(defaultSeq), defaultSeq.id)
                _profileSequenceEnabled.value = true
                // 持久化迁移结果
                persistProfiles(listOf(profile), profile.id)
            }
            _isLoaded.value = true
        }
    }


    suspend fun addNode(typeInfo: TaskTypeInfo, afterIndex: Int = -1): String {
        var newNodeId = ""
        updateChain { current ->
            val node = TaskChainNode(
                id = UUID.randomUUID().toString(),
                name = defaultTaskName(typeInfo),
                enabled = true,
                config = typeInfo.defaultConfig()
            )
            newNodeId = node.id
            if (afterIndex < 0 || afterIndex >= current.size) {
                current.add(node)
            } else {
                current.add(afterIndex + 1, node)
            }
            Timber.d("Added node: %s (%s)", node.name, typeInfo.name)
        }
        achievementRepository.report {
            event = AchievementEvents.TASK_NODE_ADDED
        }
        return newNodeId
    }

    suspend fun removeNode(nodeId: String) {
        updateChain { current ->
            current.removeAll { it.id == nodeId }
            Timber.d("Removed node: %s", nodeId)
        }
        achievementRepository.report {
            event = AchievementEvents.TASK_NODE_REMOVED
        }
    }

    /**
     * 复制任务节点，插入到原节点正下方
     * 名称规则：去掉末尾已有的 " N" 后缀得到基础名，取当前链中最小未占用的正整数 N，
     * 命名为 "baseName N"，例如：作战 → 作战 2 → 作战 3
     * 对应 WPF GuideUserControl PR#16733 复制按钮
     *
     * @return 新节点的 id，失败时返回空字符串
     */
    suspend fun duplicateNode(nodeId: String): String {
        var newNodeId = ""
        updateChain { current ->
            val idx = current.indexOfFirst { it.id == nodeId }
            if (idx >= 0) {
                val src = current[idx]
                // 去掉末尾 " N"（空格+数字）得到基础名
                val baseName = src.name.replace(Regex(" \\d+$"), "")
                // 收集链中所有以 "baseName N" 形式命名已占用的编号
                val usedNumbers = current
                    .mapNotNull { node ->
                        Regex("^${Regex.escape(baseName)} (\\d+)$").matchEntire(node.name)
                            ?.groupValues?.get(1)?.toIntOrNull()
                    }
                    .toSet()
                // 取最小未占用的正整数（从 2 开始，1 留给源名称本身）
                val nextNum = generateSequence(2) { it + 1 }.first { it !in usedNumbers }
                val copy = src.copy(
                    id = UUID.randomUUID().toString(),
                    name = "$baseName $nextNum"
                )
                newNodeId = copy.id
                current.add(idx + 1, copy)
                Timber.d("Duplicated node %s → %s (\"%s\")", nodeId, copy.id, copy.name)
            } else {
                Timber.w("duplicateNode: node %s not found", nodeId)
            }
        }
        return newNodeId
    }

    suspend fun renameNode(nodeId: String, newName: String) {
        val trimmed = newName.trim().take(MAX_NODE_NAME_LENGTH)
        if (trimmed.isEmpty()) return
        updateChain { current ->
            val idx = current.indexOfFirst { it.id == nodeId }
            if (idx >= 0) {
                current[idx] = current[idx].copy(name = trimmed)
                Timber.d("Renamed node %s to: %s", nodeId, trimmed)
            } else {
                Timber.w("renameNode: node %s not found", nodeId)
            }
        }
    }

    suspend fun setNodeEnabled(nodeId: String, enabled: Boolean) {
        updateChain { current ->
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
        updateChain { current ->
            val idx = current.indexOfFirst { it.id == nodeId }
            if (idx >= 0) {
                current[idx] = current[idx].copy(config = config)
            } else {
                Timber.w("updateNodeConfig: node %s not found", nodeId)
            }
        }
    }


    suspend fun resetRecruitConfigUseExpedited() {
        updateChain { current ->
            for (i in current.indices) {
                val node = current[i]
                if (!node.enabled) continue
                when (val cfg = node.config) {
                    is RecruitConfig -> if (cfg.useExpedited) {
                        current[i] = node.copy(config = cfg.copy(useExpedited = false))
                        Timber.d(
                            "resetTemporaryVariables: cleared useExpedited on node %s",
                            node.id
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

    suspend fun reorderNodes(fromIndex: Int, toIndex: Int) {
        updateChain { current ->
            require(fromIndex in current.indices) { "fromIndex out of bounds: $fromIndex" }
            require(toIndex in current.indices) { "toIndex out of bounds: $toIndex" }
            val node = current.removeAt(fromIndex)
            current.add(toIndex, node)
            Timber.d("Moved node from %d to %d", fromIndex, toIndex)
        }
    }

    inline fun <reified T : TaskParamProvider> firstConfigFlow(): Flow<T?> {
        return chain.map { nodes ->
            nodes.firstNotNullOfOrNull { it.config as? T }
        }.distinctUntilChanged()
    }

    inline fun <reified T : TaskParamProvider> findFirstConfig(): T? {
        return chain.value.firstNotNullOfOrNull { it.config as? T }
    }

    fun getClientType(): String {
        return getClientTypeOrNull() ?: "Official"
    }

    /**
     * Prefer the client of the active/last-started segment so multi-client sequential runs
     * do not keep resolving the chain's first WakeUp (e.g. Official while Bilibili is running).
     */
    fun getClientTypeOrNull(): String? {
        return _activeClientType.value
            ?: findFirstEnabledConfig<WakeUpConfig>()?.clientType
            ?: _lastUsedClientType.value
    }

    fun getLastUsedClientType(): String? = _lastUsedClientType.value

    fun getActiveClientType(): String? = _activeClientType.value

    fun saveLastUsedClientType(clientType: String) {
        _lastUsedClientType.value = clientType
        _activeClientType.value = clientType
    }

    fun clearActiveClientType() {
        _activeClientType.value = null
    }

    inline fun <reified T : TaskParamProvider> findFirstEnabledConfig(): T? {
        return chain.value
            .filter { it.enabled }
            .firstNotNullOfOrNull { it.config as? T }
    }

    inline fun <reified T : TaskParamProvider> firstEnabledConfigFlow(): Flow<T?> {
        return chain.map { nodes ->
            nodes.filter { it.enabled }.firstNotNullOfOrNull { it.config as? T }
        }
    }

    fun grantGameBatteryExemption(clientType: String) {
        val pkg = Packages[clientType] ?: return
        runCatching {
            RemoteServiceManager.getInstanceOrNull()?.grantPermissions(
                PermissionGrantRequest(
                    packageName = pkg,
                    permissions =
                        PermissionGrantRequest.PERM_BATTERY
                                or PermissionGrantRequest.PERM_BACKGROUND
                )
            )
            Timber.d("Battery exemption granted for game: %s", pkg)
        }.onFailure { e ->
            Timber.w(e, "Failed to grant battery exemption for game")
        }
    }

    // ========== Profile 管理 ==========

    suspend fun switchProfile(profileId: String) {
        val currentProfiles = _profiles.value
        val target = currentProfiles.find { it.id == profileId } ?: run {
            Timber.w("switchProfile: profile %s not found", profileId)
            return
        }
        // 保存当前链到旧 Profile
        val updatedProfiles = currentProfiles.map { p ->
            if (p.id == _activeProfileId.value) p.copy(chain = _chain.value) else p
        }
        // 加载新 Profile 的链
        _chain.value = target.chain
        _activeProfileId.value = profileId
        _profiles.value = updatedProfiles
        // 持久化
        persistProfiles(updatedProfiles, profileId)
        Timber.d("Switched to profile: %s (%s)", target.name, profileId)
    }

    suspend fun createProfile(): String? {
        val currentProfiles = _profiles.value
        // 先保存当前活跃 Profile 的链
        val savedProfiles = currentProfiles.map { p ->
            if (p.id == _activeProfileId.value) p.copy(chain = _chain.value) else p
        }
        val newProfile = TaskProfile(
            name = nextProfileName(savedProfiles),
            chain = buildDefaultChain()
        )
        val updatedProfiles = savedProfiles + newProfile
        // 切换到新 Profile
        _chain.value = newProfile.chain
        _activeProfileId.value = newProfile.id
        _profiles.value = updatedProfiles
        persistProfiles(updatedProfiles, newProfile.id)
        Timber.d("Created profile: %s (%s)", newProfile.name, newProfile.id)
        return newProfile.id
    }

    suspend fun deleteProfile(profileId: String) {
        val currentProfiles = _profiles.value
        if (currentProfiles.size <= 1) {
            Timber.w("deleteProfile: cannot delete last profile")
            return
        }
        // 先保存当前链
        val savedProfiles = currentProfiles.map { p ->
            if (p.id == _activeProfileId.value) p.copy(chain = _chain.value) else p
        }
        val remaining = savedProfiles.filter { it.id != profileId }
        if (remaining.size == savedProfiles.size) {
            Timber.w("deleteProfile: profile %s not found", profileId)
            return
        }
        // 若删除的是活跃 Profile,切换到列表第一个
        val newActiveId = if (_activeProfileId.value == profileId) {
            val first = remaining.first()
            _chain.value = first.chain
            first.id
        } else {
            _activeProfileId.value
        }
        _activeProfileId.value = newActiveId
        _profiles.value = remaining
        // 同步清理所有任务链配置中对该用户配置的引用（与序列写路径共用 mutex）
        sequenceMutex.withLock {
            val cleanedConfigs = _sequenceConfigs.value.map { cfg ->
                cfg.copy(entries = cfg.entries.filter { it.profileId != profileId })
            }
            if (cleanedConfigs != _sequenceConfigs.value) {
                applySequenceConfigs(cleanedConfigs, _activeSequenceConfigId.value)
            }
        }
        persistProfiles(remaining, newActiveId)
        _profileDeleted.tryEmit(profileId)
        Timber.d("Deleted profile: %s", profileId)
    }

    suspend fun renameProfile(profileId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_PROFILE_NAME_LENGTH) {
            Timber.w("renameProfile: invalid name length: %d", trimmed.length)
            return
        }
        val currentProfiles = _profiles.value
        val updatedProfiles = currentProfiles.map { p ->
            if (p.id == profileId) p.copy(name = trimmed) else p
        }
        _profiles.value = updatedProfiles
        persistProfiles(updatedProfiles, _activeProfileId.value)
        Timber.d("Renamed profile %s to: %s", profileId, trimmed)
    }

    suspend fun duplicateProfile(profileId: String): String? {
        val currentProfiles = _profiles.value
        // 先保存当前活跃 Profile 的链
        val savedProfiles = currentProfiles.map { p ->
            if (p.id == _activeProfileId.value) p.copy(chain = _chain.value) else p
        }
        val source = savedProfiles.find { it.id == profileId } ?: run {
            Timber.w("duplicateProfile: profile %s not found", profileId)
            return null
        }
        // 复制链时为每个节点生成新 ID
        val duplicatedChain = source.chain.map { it.copy(id = UUID.randomUUID().toString()) }
        val newProfile = TaskProfile(
            name = nextProfileName(savedProfiles),
            chain = duplicatedChain
        )
        val updatedProfiles = savedProfiles + newProfile
        _profiles.value = updatedProfiles
        persistProfiles(updatedProfiles, _activeProfileId.value)
        Timber.d("Duplicated profile %s as: %s (%s)", profileId, newProfile.name, newProfile.id)
        return newProfile.id
    }

    suspend fun reorderProfiles(fromIndex: Int, toIndex: Int) {
        val current = _profiles.value
        if (fromIndex !in current.indices || toIndex !in current.indices) {
            Timber.w(
                "reorderProfiles: invalid index from=%d to=%d size=%d",
                fromIndex, toIndex, current.size
            )
            return
        }
        if (fromIndex == toIndex) return

        // 顺便把当前未保存的链快照写回 active profile, 避免重排时丢失正在编辑的内容
        val savedProfiles = current.map { p ->
            if (p.id == _activeProfileId.value) p.copy(chain = _chain.value) else p
        }.toMutableList()
        val moved = savedProfiles.removeAt(fromIndex)
        savedProfiles.add(toIndex, moved)

        _profiles.value = savedProfiles
        persistProfiles(savedProfiles, _activeProfileId.value)
        Timber.d("Reordered profile from %d to %d", fromIndex, toIndex)
    }

    // ========== 任务链配置（多套命名序列，与用户 Profile 无关） ==========

    suspend fun switchSequenceConfig(configId: String) = sequenceMutex.withLock {
        val target = _sequenceConfigs.value.find { it.id == configId }
        if (target == null) {
            Timber.w("switchSequenceConfig: %s not found", configId)
            return@withLock
        }
        if (_activeSequenceConfigId.value == configId) return@withLock
        _activeSequenceConfigId.value = configId
        _profileSequence.value = target.entries
        persistSequenceConfigs(_sequenceConfigs.value, configId)
        Timber.d("Switched sequence config to %s", configId)
    }

    suspend fun createSequenceConfig(name: String? = null): String? = sequenceMutex.withLock {
        val resolvedName = name?.trim()?.takeIf { it.isNotEmpty() }
            ?: nextSequenceName(_sequenceConfigs.value)
        val config = TaskSequenceConfig(name = resolvedName.take(MAX_SEQUENCE_NAME_LENGTH))
        val updated = _sequenceConfigs.value + config
        applySequenceConfigs(updated, config.id)
        persistSequenceConfigs(updated, config.id)
        Timber.d("Created sequence config %s (%s)", config.id, config.name)
        config.id
    }

    suspend fun renameSequenceConfig(configId: String, name: String) = sequenceMutex.withLock {
        val trimmed = name.trim().take(MAX_SEQUENCE_NAME_LENGTH)
        if (trimmed.isEmpty()) return@withLock
        val updated = _sequenceConfigs.value.map {
            if (it.id == configId) it.copy(name = trimmed) else it
        }
        if (updated == _sequenceConfigs.value) return@withLock
        _sequenceConfigs.value = updated
        persistSequenceConfigs(updated, _activeSequenceConfigId.value)
        Timber.d("Renamed sequence config %s to %s", configId, trimmed)
    }

    suspend fun deleteSequenceConfig(configId: String) = sequenceMutex.withLock {
        val current = _sequenceConfigs.value
        if (current.size <= 1) {
            Timber.w("deleteSequenceConfig: keep at least one")
            return@withLock
        }
        val updated = current.filter { it.id != configId }
        if (updated.size == current.size) return@withLock
        val newActive = if (_activeSequenceConfigId.value == configId) {
            updated.first().id
        } else {
            _activeSequenceConfigId.value
        }
        applySequenceConfigs(updated, newActive)
        persistSequenceConfigs(updated, newActive)
        Timber.d("Deleted sequence config %s, active=%s", configId, newActive)
    }

    suspend fun addProfileToSequence(profileId: String): Boolean {
        return addProfilesToSequence(listOf(profileId)) == 1
    }

    /**
     * 批量追加当前任务链配置的条目（单次读改写）。
     * @return 实际成功追加的条数
     */
    suspend fun addProfilesToSequence(profileIds: List<String>): Int = sequenceMutex.withLock {
        if (profileIds.isEmpty()) return@withLock 0
        val validIds = _profiles.value.map { it.id }.toSet()
        val current = _profileSequence.value
        val room = MAX_SEQUENCE_ENTRIES - current.size
        if (room <= 0) {
            Timber.w("addProfilesToSequence: max entries (%d) reached", MAX_SEQUENCE_ENTRIES)
            return@withLock 0
        }
        val toAdd = ArrayList<ProfileSequenceEntry>(minOf(profileIds.size, room))
        for (profileId in profileIds) {
            if (toAdd.size >= room) break
            if (profileId !in validIds) {
                Timber.w("addProfilesToSequence: profile %s not found", profileId)
                continue
            }
            toAdd += ProfileSequenceEntry(profileId = profileId)
        }
        if (toAdd.isEmpty()) return@withLock 0
        val updated = current + toAdd
        writeActiveSequenceEntries(updated)
        Timber.d("Added %d profile(s) to sequence (size=%d)", toAdd.size, updated.size)
        toAdd.size
    }

    suspend fun removeSequenceEntry(entryId: String) = sequenceMutex.withLock {
        val updated = _profileSequence.value.filter { it.id != entryId }
        if (updated.size == _profileSequence.value.size) {
            Timber.w("removeSequenceEntry: entry %s not found", entryId)
            return@withLock
        }
        writeActiveSequenceEntries(updated)
        Timber.d("Removed sequence entry %s", entryId)
    }

    suspend fun reorderSequence(fromIndex: Int, toIndex: Int) = sequenceMutex.withLock {
        val current = _profileSequence.value.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) {
            Timber.w(
                "reorderSequence: invalid index from=%d to=%d size=%d",
                fromIndex, toIndex, current.size
            )
            return@withLock
        }
        if (fromIndex == toIndex) return@withLock
        val moved = current.removeAt(fromIndex)
        current.add(toIndex, moved)
        writeActiveSequenceEntries(current)
        Timber.d("Reordered sequence from %d to %d", fromIndex, toIndex)
    }

    suspend fun clearProfileSequence() = sequenceMutex.withLock {
        if (_profileSequence.value.isEmpty()) return@withLock
        writeActiveSequenceEntries(emptyList())
        Timber.d("Cleared profile sequence")
    }

    suspend fun setProfileSequenceEnabled(enabled: Boolean) = sequenceMutex.withLock {
        if (_profileSequenceEnabled.value == enabled) return@withLock
        _profileSequenceEnabled.value = enabled
        context.store.edit { prefs ->
            prefs[PROFILE_SEQUENCE_ENABLED_KEY] = enabled
        }
        Timber.d("Profile sequence enabled: %s", enabled)
    }

    /** 将当前激活配置的 entries 写回 configs 列表并持久化 */
    private suspend fun writeActiveSequenceEntries(entries: List<ProfileSequenceEntry>) {
        val activeId = _activeSequenceConfigId.value
        val configs = _sequenceConfigs.value
        val updated = if (configs.isEmpty() || configs.none { it.id == activeId }) {
            val cfg = TaskSequenceConfig(
                id = activeId.ifEmpty { UUID.randomUUID().toString() },
                name = nextSequenceName(configs),
                entries = entries,
            )
            listOf(cfg)
        } else {
            configs.map { if (it.id == activeId) it.copy(entries = entries) else it }
        }
        val resolvedActive = updated.find { it.id == activeId }?.id ?: updated.first().id
        applySequenceConfigs(updated, resolvedActive)
        persistSequenceConfigs(updated, resolvedActive)
    }

    private fun applySequenceConfigs(configs: List<TaskSequenceConfig>, activeId: String) {
        val resolved = configs.ifEmpty { listOf(defaultSequenceConfig()) }
        val resolvedActive = resolved.find { it.id == activeId }?.id ?: resolved.first().id
        _sequenceConfigs.value = resolved
        _activeSequenceConfigId.value = resolvedActive
        _profileSequence.value = resolved.find { it.id == resolvedActive }?.entries.orEmpty()
    }

    /** Localized default name prefixes (follows app language setting). */
    private fun localizedNamePrefixes(): Pair<String, String> {
        val tag = resolveSelectedLanguage(appSettings.language.value).tag
        val localized = context.createConfigurationContext(
            Configuration(context.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(tag))
            }
        )
        return localized.getString(R.string.task_profile_name_prefix) to
            localized.getString(R.string.task_sequence_name_prefix)
    }

    /** 识别历史/当前语言前缀，避免切语言后 next 编号回退 */
    private fun allKnownProfilePrefixes(current: String): List<String> =
        listOf(current, "配置-", "Profile-").distinct()

    private fun allKnownSequencePrefixes(current: String): List<String> =
        listOf(current, "任务链-", "Sequence-").distinct()

    private fun maxNumericSuffix(names: List<String>, prefixes: List<String>): Int {
        return names.mapNotNull { name ->
            prefixes.firstNotNullOfOrNull { prefix ->
                if (name.startsWith(prefix)) name.removePrefix(prefix).toIntOrNull() else null
            }
        }.maxOrNull() ?: 0
    }

    
    private fun profileDefaultName(): String {
        val (profilePrefix, _) = localizedNamePrefixes()
        return "${profilePrefix}1"
    }

    private fun sequenceDefaultName(): String {
        val (_, seqPrefix) = localizedNamePrefixes()
        return "${seqPrefix}1"
    }

    private fun defaultSequenceConfig(): TaskSequenceConfig {
        return TaskSequenceConfig(name = sequenceDefaultName())
    }

    private fun nextSequenceName(configs: List<TaskSequenceConfig>): String {
        val (_, seqPrefix) = localizedNamePrefixes()
        val maxNum = maxNumericSuffix(
            configs.map { it.name },
            allKnownSequencePrefixes(seqPrefix),
        )
        return "$seqPrefix${maxNum + 1}"
    }

    private fun loadSequenceConfigsFromPrefs(prefs: Preferences, validIds: Set<String>) {
        val rawConfigs = prefs[SEQUENCE_CONFIGS_KEY]
        val configs = if (!rawConfigs.isNullOrEmpty()) {
            decodeSequenceConfigs(rawConfigs)
                .map { it.copy(entries = sanitizeSequence(it.entries, validIds)) }
        } else {
            // 旧版：仅有单条 profile_sequence，迁成一套默认任务链配置
            val legacy = sanitizeSequence(
                decodeProfileSequence(prefs[PROFILE_SEQUENCE_KEY]),
                validIds,
            )
            listOf(TaskSequenceConfig(name = sequenceDefaultName(), entries = legacy))
        }
        val activeId = prefs[ACTIVE_SEQUENCE_CONFIG_KEY]
            ?: configs.firstOrNull()?.id
            ?: ""
        applySequenceConfigs(configs.ifEmpty { listOf(defaultSequenceConfig()) }, activeId)
    }

    /**
     * 将任务链中的 profile 按顺序拼接为一条可执行节点列表。
     * - 关闭任务链 / 序列为空 / 拼接结果为空：
     *   - [fallbackToActive]=true（手动默认）：回退当前活跃 profile
     *   - [fallbackToActive]=false（定时任务链）：返回 emptyList，由调用方判失败
     * - 拼接时为节点生成新 ID，避免重复 profile 导致 nodeId 冲突
     */
    fun resolveExecutableChain(
        sequence: List<ProfileSequenceEntry> = _profileSequence.value,
        profiles: List<TaskProfile> = _profiles.value,
        activeChain: List<TaskChainNode> = _chain.value,
        activeProfileId: String = _activeProfileId.value,
        sequenceEnabled: Boolean = _profileSequenceEnabled.value,
        fallbackToActive: Boolean = true,
    ): List<TaskChainNode> {
        if (!sequenceEnabled || sequence.isEmpty()) {
            return if (fallbackToActive) activeChain else emptyList()
        }
        val profileMap = profiles.associateBy { it.id }
        // 活跃 profile 的链以内存态为准（可能尚未写回 profiles 快照）
        val resolved = sequence.flatMap { entry ->
            val sourceChain = when {
                entry.profileId == activeProfileId -> activeChain
                else -> profileMap[entry.profileId]?.chain.orEmpty()
            }
            sourceChain.map { node -> node.copy(id = UUID.randomUUID().toString()) }
        }
        // 条目均无效/空链：手动回退；定时任务链返回空避免跑错目标
        if (resolved.isEmpty()) {
            return if (fallbackToActive) activeChain else emptyList()
        }
        return reindexCopy(resolved)
    }

    // ========== 内部工具方法 ==========

    private suspend inline fun updateChain(
        crossinline block: (MutableList<TaskChainNode>) -> Unit
    ) {
        val current = _chain.value.toMutableList()
        block(current)
        reindex(current)
        val snapshot = current.toList()
        _chain.value = snapshot              // 同步更新，立即可见
        // 同步更新 profiles 中活跃 Profile 的 chain
        val updatedProfiles = _profiles.value.map { p ->
            if (p.id == _activeProfileId.value) p.copy(chain = snapshot) else p
        }
        _profiles.value = updatedProfiles
        context.store.edit { prefs ->        // 异步持久化
            prefs[CHAIN_KEY] = json.encodeToString<List<TaskChainNode>>(snapshot)
            prefs[PROFILES_KEY] = json.encodeToString<List<TaskProfile>>(updatedProfiles)
        }
    }

    private fun decodeChain(raw: String?): List<TaskChainNode> {
        if (raw.isNullOrEmpty()) return buildDefaultChain()
        return runCatching {
            json.decodeFromString<List<TaskChainNode>>(raw)
        }.getOrElse {
            Timber.w(it, "Failed to decode task chain, using defaults")
            buildDefaultChain()
        }
    }

    private fun decodeProfiles(raw: String): List<TaskProfile> {
        return runCatching {
            json.decodeFromString<List<TaskProfile>>(raw)
        }.getOrElse {
            Timber.w(it, "Failed to decode profiles")
            emptyList()
        }
    }

    private fun decodeProfileSequence(raw: String?): List<ProfileSequenceEntry> {
        if (raw.isNullOrEmpty()) return emptyList()
        return runCatching {
            json.decodeFromString<List<ProfileSequenceEntry>>(raw)
        }.getOrElse {
            Timber.w(it, "Failed to decode profile sequence")
            emptyList()
        }
    }

    private fun decodeSequenceConfigs(raw: String): List<TaskSequenceConfig> {
        return runCatching {
            json.decodeFromString<List<TaskSequenceConfig>>(raw)
        }.getOrElse {
            Timber.w(it, "Failed to decode sequence configs")
            emptyList()
        }
    }

    private fun sanitizeSequence(
        sequence: List<ProfileSequenceEntry>,
        validProfileIds: Set<String>,
    ): List<ProfileSequenceEntry> {
        return sequence
            .filter { it.profileId in validProfileIds }
            .take(MAX_SEQUENCE_ENTRIES)
    }

    private suspend fun persistProfiles(
        profiles: List<TaskProfile>,
        activeId: String,
        sequenceEnabled: Boolean? = null,
    ) {
        val validIds = profiles.map { it.id }.toSet()
        val cleanedConfigs = _sequenceConfigs.value.map {
            it.copy(entries = sanitizeSequence(it.entries, validIds))
        }.ifEmpty { listOf(defaultSequenceConfig()) }
        val activeSeqId = cleanedConfigs.find { it.id == _activeSequenceConfigId.value }?.id
            ?: cleanedConfigs.first().id
        if (cleanedConfigs != _sequenceConfigs.value ||
            activeSeqId != _activeSequenceConfigId.value ||
            cleanedConfigs.find { it.id == activeSeqId }?.entries != _profileSequence.value
        ) {
            applySequenceConfigs(cleanedConfigs, activeSeqId)
        }
        context.store.edit { prefs ->
            prefs[PROFILES_KEY] = json.encodeToString<List<TaskProfile>>(profiles)
            prefs[ACTIVE_PROFILE_KEY] = activeId
            // 同步更新 CHAIN_KEY 以保持兼容
            val activeChain = profiles.find { it.id == activeId }?.chain ?: _chain.value
            prefs[CHAIN_KEY] = json.encodeToString<List<TaskChainNode>>(activeChain)
            // 新格式 + 兼容旧单序列字段
            prefs[SEQUENCE_CONFIGS_KEY] =
                json.encodeToString<List<TaskSequenceConfig>>(cleanedConfigs)
            prefs[ACTIVE_SEQUENCE_CONFIG_KEY] = activeSeqId
            prefs[PROFILE_SEQUENCE_KEY] =
                json.encodeToString<List<ProfileSequenceEntry>>(
                    cleanedConfigs.find { it.id == activeSeqId }?.entries.orEmpty()
                )
            if (sequenceEnabled != null) {
                prefs[PROFILE_SEQUENCE_ENABLED_KEY] = sequenceEnabled
            }
        }
    }

    private suspend fun persistSequenceConfigs(
        configs: List<TaskSequenceConfig>,
        activeId: String,
    ) {
        context.store.edit { prefs ->
            prefs[SEQUENCE_CONFIGS_KEY] =
                json.encodeToString<List<TaskSequenceConfig>>(configs)
            prefs[ACTIVE_SEQUENCE_CONFIG_KEY] = activeId
            // 兼容旧备份/读取：仍写入当前激活序列
            val activeEntries = configs.find { it.id == activeId }?.entries.orEmpty()
            prefs[PROFILE_SEQUENCE_KEY] =
                json.encodeToString<List<ProfileSequenceEntry>>(activeEntries)
        }
    }

    private fun reindex(nodes: MutableList<TaskChainNode>) {
        for (i in nodes.indices) {
            nodes[i] = nodes[i].copy(order = i)
        }
    }

    private fun reindexCopy(nodes: List<TaskChainNode>): List<TaskChainNode> {
        return nodes.mapIndexed { index, node -> node.copy(order = index) }
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
            }
        )
        return typeInfo.defaultName(localizedContext)
    }

    suspend fun importProfiles(
        profiles: List<TaskProfile>,
        activeId: String,
        sequence: List<ProfileSequenceEntry> = emptyList(),
        sequenceEnabled: Boolean = true,
        sequenceConfigs: List<TaskSequenceConfig> = emptyList(),
        activeSequenceConfigId: String = "",
    ) {
        val resolvedActiveId = profiles.find { it.id == activeId }?.id
            ?: profiles.firstOrNull()?.id ?: return
        val activeChain = profiles.find { it.id == resolvedActiveId }?.chain ?: buildDefaultChain()
        val validIds = profiles.map { it.id }.toSet()
        val importedConfigs = when {
            sequenceConfigs.isNotEmpty() -> sequenceConfigs
                .map { it.copy(entries = sanitizeSequence(it.entries, validIds)) }
            else -> listOf(
                TaskSequenceConfig(
                    name = sequenceDefaultName(),
                    entries = sanitizeSequence(sequence, validIds),
                )
            )
        }.ifEmpty { listOf(defaultSequenceConfig()) }
        val resolvedSeqActive = importedConfigs.find { it.id == activeSequenceConfigId }?.id
            ?: importedConfigs.first().id
        // 同一锁内切换 profiles + sequence，避免读到半更新
        sequenceMutex.withLock {
            _profiles.value = profiles
            _activeProfileId.value = resolvedActiveId
            _chain.value = activeChain
            applySequenceConfigs(importedConfigs, resolvedSeqActive)
            _profileSequenceEnabled.value = sequenceEnabled
        }
        // 与 enabled 一并写入，避免 import 两次 DataStore edit
        persistProfiles(profiles, resolvedActiveId, sequenceEnabled = sequenceEnabled)
        Timber.d(
            "Imported %d profiles, active: %s, sequenceConfigs: %d, activeSeq: %s, enabled: %s",
            profiles.size,
            resolvedActiveId,
            _sequenceConfigs.value.size,
            _activeSequenceConfigId.value,
            sequenceEnabled,
        )
    }

    private fun nextProfileName(profiles: List<TaskProfile>): String {
        val (profilePrefix, _) = localizedNamePrefixes()
        val maxNum = maxNumericSuffix(
            profiles.map { it.name },
            allKnownProfilePrefixes(profilePrefix),
        )
        return "$profilePrefix${maxNum + 1}"
    }
}
