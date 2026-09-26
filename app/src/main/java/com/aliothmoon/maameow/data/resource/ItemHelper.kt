package com.aliothmoon.maameow.data.resource

import com.aliothmoon.maameow.data.config.MaaPathConfig
import com.aliothmoon.maameow.utils.JsonUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.File

/**
 * see ItemListHelper
 */
class ItemHelper(
    private val pathConfig: MaaPathConfig
) {
    private val json = JsonUtils.common

    private val _items = MutableStateFlow<Map<String, ItemInfo>>(emptyMap())
    private val _dropItems = MutableStateFlow<List<ItemInfo>>(emptyList())

    /**  (itemId -> ItemInfo) */
    val items: StateFlow<Map<String, ItemInfo>> = _items.asStateFlow()
    val dropItems: StateFlow<List<ItemInfo>> = _dropItems.asStateFlow()

    /** 是否已经拿到过一份非空索引；为 false 时界面只能把物品回退成 ID */
    val isLoaded: Boolean get() = _items.value.isNotEmpty()

    /**
     * 关卡不可掉落的材料排除列表
     * see FightSettingsUserControlModel._excludedValues
     */
    companion object {
        private const val INDEX_JSON = "item_index.json"

        /** 没有独立资源档的客户端，只有根目录那一份索引 */
        const val DEFAULT_CLIENT = "Official"

        /** 有 global 资源档的客户端；与 ResourceDataManager.CLIENT_LANGUAGE_MAPPER 同集合 */
        private val GLOBAL_CLIENTS = setOf("txwy", "YoStarEN", "YoStarJP", "YoStarKR")

        private val excludedValues = setOf(
            "3213", "3223", "3233", "3243", // 双芯片
            "3253", "3263", "3273", "3283", // 双芯片
            "7001", "7002", "7003", "7004", // 许可
            "4004", "4005",                 // 凭证
            "3105", "3131", "3132", "3133", // 龙骨/加固建材
            "6001",                         // 演习券
            "3141", "4002",                 // 家具零件/源石
            "32001",                        // 芯片助剂
            "30115",                        // 聚合剂
            "30125",                        // 双极纳米片
            "30135",                        // D32钢
            "30145",                        // 晶体电子单元
            "30155",                        // 烧结核凝晶
            "30165",                        // 重相位对映体
        )
    }

    /**
     * 读取物品索引。
     *
     * 全球服那份覆盖根目录那份：物品名要跟客户端语言走，而根目录那份条目更全
     * （新素材先上国服）。两份合并取并集，既不会因为选了国际服丢 ID，
     * 也不会把中文名带到国际服。
     *
     * 解析不出来时**保留上一份可用数据**——直接清空会让界面整局把所有物品
     * 显示成 ID，比留着旧名字更糟。
     *
     * @return 是否拿到了非空索引
     */
    fun load(clientType: String = DEFAULT_CLIENT): Boolean {
        val parsed = doParseItemsJson(clientType)
        if (parsed.isEmpty()) {
            if (_items.value.isEmpty()) {
                Timber.w("物品索引为空（客户端 %s），物品名将回退为 ID", clientType)
            }
            return false
        }
        if (parsed != _items.value) {
            _items.value = parsed
            _dropItems.value = parsed.values
                .filter { it.id.all { c -> c.isDigit() } }  // WPF: int.TryParse
                .filter { it.id !in excludedValues }
                .sortedBy { it.id }  // WPF: string.Compare Ordinal
        }
        return true
    }

    fun getItemInfo(itemId: String): ItemInfo? {
        return _items.value[itemId]
    }

    /** 先根目录、后客户端资源档；后读的覆盖先读的 */
    private fun indexFiles(clientType: String): List<File> = buildList {
        add(File(pathConfig.resourceDir, INDEX_JSON))
        if (clientType in GLOBAL_CLIENTS) {
            add(File(pathConfig.globalResourceDir(clientType), INDEX_JSON))
        }
    }

    /** 缺文件的那层直接跳过，不影响另一层 */
    private fun doParseItemsJson(clientType: String): Map<String, ItemInfo> =
        indexFiles(clientType).fold(emptyMap<String, ItemInfo>()) { acc, file ->
            if (file.isFile) acc + parseItemsJson(file) else acc
        }

    private fun parseItemsJson(file: File): Map<String, ItemInfo> {
        // TODO 暂时只支持中文
        return try {
            val entries: Map<String, ItemJsonEntry> = json.decodeFromString(file.readText())
            entries.mapValues { (id, entry) ->
                ItemInfo(
                    id = id,
                    name = entry.name,
                    icon = entry.icon,
                    sortId = entry.sortId,
                    classifyType = entry.classifyType ?: ""
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "解析物品索引失败: %s", file.absolutePath)
            emptyMap()
        }
    }
}
