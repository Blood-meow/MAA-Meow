package com.aliothmoon.maameow.domain.service

import android.content.Context
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.constant.MaaFiles.ASSET_DIR_NAME
import com.aliothmoon.maameow.constant.MaaFiles.OVERRIDES_ASSET_TASKS
import com.aliothmoon.maameow.data.config.MaaPathConfig
import com.aliothmoon.maameow.data.datasource.AssetExtractor
import com.aliothmoon.maameow.data.resource.ItemHelper
import com.aliothmoon.maameow.domain.state.ResourceInitState
import com.aliothmoon.maameow.utils.i18n.LocalizedException
import com.aliothmoon.maameow.utils.i18n.uiTextDynamicOr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

class ResourceInitService(
    private val context: Context,
    private val assetExtractor: AssetExtractor,
    private val pathConfig: MaaPathConfig,
    private val itemHelper: ItemHelper,
) {
    private val _state = MutableStateFlow<ResourceInitState>(ResourceInitState.NotChecked)
    val state: StateFlow<ResourceInitState> = _state.asStateFlow()

    suspend fun checkAndInit() {
        val cur = _state.value
        if (cur !is ResourceInitState.NotChecked && cur !is ResourceInitState.Failed) {
            return
        }
        if (!_state.compareAndSet(cur, ResourceInitState.Checking)) {
            return
        }

        if (pathConfig.isResourceReady) {
            loadItemIndex()
            _state.value = ResourceInitState.Ready
            return
        }

        doExtractFromAssets()
    }

    suspend fun reInitialize() {
        doExtractFromAssets()
    }

    suspend fun doExtractFromAssets() {
        _state.value =
            ResourceInitState.Extracting(0, 0, context.getString(R.string.resource_init_preparing))

        try {
            withContext(Dispatchers.IO) {
                pathConfig.ensureDirectories()
                val resourceDir = File(pathConfig.resourceDir)
                if (resourceDir.exists() && !resourceDir.deleteRecursively()) {
                    Timber.w("清理旧资源目录失败: ${resourceDir.absolutePath}")
                }
                resourceDir.mkdirs()
            }

            val result = assetExtractor.extract(
                assetDir = ASSET_DIR_NAME,
                destDir = File(pathConfig.resourceDir),
                onProgress = { progress ->
                    _state.value = ResourceInitState.Extracting(
                        extractedCount = progress.extractedCount,
                        totalCount = progress.totalCount,
                        currentFile = progress.currentFile
                    )
                }
            )

            result.fold(
                onSuccess = {
                    pathConfig.markAppVersion()
                    doForceSyncOverridesTemplate()
                    loadItemIndex()
                    Timber.i("资源初始化完成")
                    _state.value = ResourceInitState.Ready
                },
                onFailure = { e ->
                    _state.value = ResourceInitState.Failed(
                        (e as? LocalizedException)?.uiText
                            ?: uiTextDynamicOr(e.message, R.string.resource_init_error_copy_failed)
                    )
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "资源初始化失败")
            _state.value = ResourceInitState.Failed(
                (e as? LocalizedException)?.uiText
                    ?: uiTextDynamicOr(e.message, R.string.resource_init_error_unknown)
            )
        }
    }

    /**
     * 物品索引只依赖 `resource/item_index.json`，与 MaaCore 服务无关。
     * 放在资源就绪时加载，避免用户还没跑过任务时界面把物品回退成纯 ID。
     *
     * 刚解完资源偶发读不到（文件系统还没落稳），空结果退避重试几次；
     * [ItemHelper.load] 拿不到内容时会保留旧表，所以重试不会把已有数据打掉。
     */
    private suspend fun loadItemIndex() = withContext(Dispatchers.IO) {
        repeat(ITEM_INDEX_LOAD_ATTEMPTS) { attempt ->
            val ok = runCatching { itemHelper.load() }
                .onFailure { Timber.w(it, "物品索引加载失败") }
                .getOrDefault(false)
            if (ok) return@withContext
            if (attempt < ITEM_INDEX_LOAD_ATTEMPTS - 1) delay(ITEM_INDEX_RETRY_DELAY_MS)
        }
        Timber.w("物品索引始终为空，界面将把物品显示为 ID")
    }

    private fun doForceSyncOverridesTemplate() {
        val dest = pathConfig.overrideTasksFile
        runCatching {
            dest.parentFile?.mkdirs()
            context.assets.open(OVERRIDES_ASSET_TASKS).use { src ->
                dest.outputStream().use { src.copyTo(it) }
            }
            Timber.d("overrides 模板已同步: ${dest.absolutePath}")
        }.onFailure {
            Timber.w(it, "overrides 模板同步失败，跳过")
        }
    }

    private companion object {
        /** 首次加载物品索引的重试次数（含首次） */
        const val ITEM_INDEX_LOAD_ATTEMPTS = 3
        const val ITEM_INDEX_RETRY_DELAY_MS = 300L
    }
}
