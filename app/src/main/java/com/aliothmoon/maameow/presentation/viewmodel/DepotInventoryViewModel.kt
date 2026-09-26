package com.aliothmoon.maameow.presentation.viewmodel

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliothmoon.maameow.data.model.DepotMaintainConfig
import com.aliothmoon.maameow.data.model.DepotMaintainPlan
import com.aliothmoon.maameow.data.model.DepotPlanOutcome
import com.aliothmoon.maameow.data.model.TaskChainNode
import com.aliothmoon.maameow.data.model.TaskProfile
import com.aliothmoon.maameow.data.model.depotPlanOutcome
import com.aliothmoon.maameow.data.model.toolbox.OperBoxExportFormatter
import com.aliothmoon.maameow.data.model.toolbox.OperBoxExportLabels
import com.aliothmoon.maameow.data.model.toolbox.OperBoxOperator
import com.aliothmoon.maameow.data.preferences.TaskChainState
import com.aliothmoon.maameow.data.repository.DepotRepository
import com.aliothmoon.maameow.data.repository.DepotSnapshot
import com.aliothmoon.maameow.data.repository.OperBoxRepository
import com.aliothmoon.maameow.data.repository.OperBoxSnapshot
import com.aliothmoon.maameow.data.repository.toArkPlannerJson
import com.aliothmoon.maameow.data.repository.toExportList
import com.aliothmoon.maameow.data.repository.toLoliconJson
import com.aliothmoon.maameow.data.resource.ActivityManager
import com.aliothmoon.maameow.data.resource.ItemHelper
import com.aliothmoon.maameow.data.resource.ItemIconLoader
import com.aliothmoon.maameow.data.resource.ItemInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.DateFormat
import java.util.Date
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * 库存数据页（二级配置档列表 + 三级配置档详情）。
 *
 * 仓库/干员快照按「配置档」分片，一档一份，与 [com.aliothmoon.maameow.data.repository.ProfileShardStore]
 * 的键一致；列表页只读各档汇总，详情页才展开明细。
 *
 * 库存明细与「库存保持」计划合并成同一批格子（[cells]）：物品的当前库存就是保持计划的进度，
 * 分成「未集齐」与「已集齐/无需保持」两组展示，点格子可直接改该物品的目标库存。
 */
class DepotInventoryViewModel(
    private val depotRepository: DepotRepository,
    private val operBoxRepository: OperBoxRepository,
    private val taskChainState: TaskChainState,
    private val itemHelper: ItemHelper,
    private val itemIconLoader: ItemIconLoader,
    private val activityManager: ActivityManager,
) : ViewModel() {

    val profiles: StateFlow<List<TaskProfile>> = taskChainState.profiles

    val activeProfileId: StateFlow<String> = taskChainState.profileId

    /** "" = 停在列表页；非空 = 已进入某档详情 */
    private val manualSelection = MutableStateFlow("")

    val selectedProfileId: StateFlow<String> = manualSelection.asStateFlow()

    val selectedProfile: StateFlow<TaskProfile?> =
        combine(taskChainState.profiles, selectedProfileId) { list, id ->
            list.firstOrNull { it.id == id }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 进入三级详情；只改本页选中项，不动全局活跃档，避免「看一眼」产生副作用。 */
    fun selectProfile(profileId: String) {
        manualSelection.value = profileId
    }

    fun clearSelection() {
        manualSelection.value = ""
    }

    // ========== 二级：配置档列表 ==========

    /** 每个配置档一张卡片：可抽次数、更新时间、库存保持进度。 */
    val profileRows: StateFlow<List<DepotProfileRow>> = combine(
        taskChainState.profiles,
        taskChainState.profileId,
        depotRepository.snapshots,
        operBoxRepository.snapshots,
    ) { profileList, activeId, depotMap, operMap ->
        profileList.map { profile ->
            val snapshot = depotMap[profile.id] ?: DepotSnapshot()
            val operBox = operMap[profile.id] ?: OperBoxSnapshot()
            val plans = profile.chain.depotPlans()
            DepotProfileRow(
                id = profile.id,
                name = profile.name,
                isActive = profile.id == activeId,
                hasSynced = snapshot.syncTimeMillis > 0L || operBox.hasSynced,
                syncTimeMillis = snapshot.syncTimeMillis,
                planCount = plans.size,
                unmetCount = plans.count { it.unmetIn(snapshot) },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ========== 三级：配置档详情 ==========

    val depotSnapshot: StateFlow<DepotSnapshot> =
        combine(depotRepository.snapshots, selectedProfileId) { map, id ->
            map[id] ?: DepotSnapshot()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DepotSnapshot())

    val operBoxSnapshot: StateFlow<OperBoxSnapshot> =
        combine(operBoxRepository.snapshots, selectedProfileId) { map, id ->
            map[id] ?: OperBoxSnapshot()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OperBoxSnapshot())

    /** 纯仓库明细（导出用，不含保持计划里那些仓库还没有的物品）。 */
    val items: StateFlow<List<DepotInventoryItemUi>> =
        combine(depotSnapshot, itemHelper.items) { snap, itemMap ->
            snap.toItemUiList(itemMap)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * 所选配置档任务链里所有「库存保持」计划。
     *
     * 判定复用 [depotPlanOutcome]，与执行侧同源；未启用的节点也收进来，
     * 否则用户在这一页看不到、也改不了自己配过但被停用的计划。
     */
    val maintainPlans: StateFlow<List<DepotMaintainPlanUi>> =
        combine(selectedProfile, depotSnapshot, itemHelper.items) { profile, snap, itemMap ->
            profile?.chain.orEmpty().flatMap { node ->
                val config = node.config as? DepotMaintainConfig ?: return@flatMap emptyList()
                config.plans.mapIndexed { index, plan ->
                    plan.toUi(node, index, snap, itemMap) {
                        activityManager.isStageOpen(it)
                    }
                }
            }
                // 没选物品的计划连物品都算不上，配置页已经在报「未选物品」，
                // 别在库存网格里留一个没有图标没有名字的空格子
                .filter { it.itemId.isNotBlank() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * 库存格子 = 仓库现有物品 ∪ 保持计划里的物品。
     *
     * 计划物品即便仓库里一条记录都没有（识别不到 = 0）也要出现，
     * 否则「缺多少」这件事在最需要看的时候反而没有格子。
     * 抽卡资源（合成玉/寻访凭证/十连寻访凭证）不是可刷材料，不出格子。
     */
    val cells: StateFlow<List<DepotInventoryCellUi>> =
        combine(depotSnapshot, itemHelper.items, maintainPlans) { snap, itemMap, plans ->
            val planByItem = plans.groupBy { it.itemId }.mapValues { entry -> entry.value.first() }
            val ids = LinkedHashSet<String>(snap.items.keys).apply { addAll(planByItem.keys) }
            ids.filterNot { it in DRAW_ITEM_IDS }
                .map { id ->
                    val info = itemMap[id]
                    DepotInventoryCellUi(
                        id = id,
                        name = info?.name ?: id,
                        count = snap.items[id] ?: 0,
                        sortId = info?.sortId ?: Int.MAX_VALUE,
                        plan = planByItem[id],
                    )
                }.sortedWith(compareBy({ it.sortId }, { it.id }))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 新建计划时要知道的所属节点状态（只用来提示任务没启用）。 */
    val planContext: StateFlow<DepotPlanContext> = selectedProfile
        .map { profile ->
            val node = profile?.chain?.firstDepotNode()
            DepotPlanContext(nodeEnabled = node?.enabled ?: true)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DepotPlanContext())

    /**
     * 写入一条库存保持计划：该物品已有计划就地覆盖，没有则追加。
     * 配置档里还没有库存保持节点时由 [TaskChainState] 就地新建。
     *
     * [existing] 由调用方（面板）直接给出，就是用户点开时看到的那条计划，
     * 不再回查 [cells]——那是个 `WhileSubscribed` 的派生流，拿它的 `value`
     * 当写回定位依据会在没订阅时读到空值。
     */
    fun savePlan(existing: DepotMaintainPlanUi?, plan: DepotMaintainPlan) {
        val profileId = selectedProfileId.value
        if (profileId.isEmpty()) return
        viewModelScope.launch {
            taskChainState.updateDepotMaintainPlans(profileId, existing?.nodeId) { plans ->
                val index = existing?.planIndex ?: -1
                if (index in plans.indices) {
                    plans.toMutableList().also { it[index] = plan }
                } else {
                    plans + plan
                }
            }
        }
    }

    /** 移除该物品的保持计划；没有计划时什么都不做。 */
    fun removePlan(existing: DepotMaintainPlanUi?) {
        val profileId = selectedProfileId.value
        if (profileId.isEmpty() || existing == null) return
        viewModelScope.launch {
            taskChainState.updateDepotMaintainPlans(profileId, existing.nodeId) { plans ->
                plans.filterIndexed { index, _ -> index != existing.planIndex }
            }
        }
    }

    /** 清空指定配置档的仓库与干员数据（非活跃档也能清）。 */
    fun clearProfile(profileId: String) {
        if (profileId.isEmpty()) return
        depotRepository.clear(profileId)
        operBoxRepository.clear(profileId)
    }

    // ========== 导出 ==========
    //
    // 每个导出入口都显式带 profileId：列表页可以直接导出某一档，
    // 那时本页并没有「选中」那一档，按 selectedProfileId 取数据会取到空。

    fun exportDepotArkPlanner(profileId: String): String =
        snapshotOf(profileId).toArkPlannerJson(itemHelper.items.value)

    fun exportDepotLolicon(profileId: String): String =
        snapshotOf(profileId).toLoliconJson(itemHelper.items.value)

    fun exportOperBoxList(profileId: String): List<OperBoxOperator> =
        (operBoxRepository.snapshots.value[profileId] ?: OperBoxSnapshot()).toExportList()

    fun exportOperBoxJson(profileId: String): String =
        OperBoxExportFormatter.toJson(exportOperBoxList(profileId))

    fun exportOperBoxMarkdown(profileId: String, labels: OperBoxExportLabels): String =
        OperBoxExportFormatter.toMarkdown(exportOperBoxList(profileId), labels)

    fun exportOperBoxCsv(profileId: String, labels: OperBoxExportLabels): String =
        OperBoxExportFormatter.toCsv(exportOperBoxList(profileId), labels)

    private fun snapshotOf(profileId: String): DepotSnapshot =
        depotRepository.snapshots.value[profileId] ?: DepotSnapshot()

    /**
     * @param hideProfileLabel 为 true 时图头不写配置档名，只留时间与数据
     * @param titleLabel 图头标题（通常为配置档名）
     */
    suspend fun renderDepotPng(
        profileId: String,
        hideProfileLabel: Boolean = false,
        titleLabel: String = "",
    ): ByteArray? = withContext(Dispatchers.Default) {
        val snap = snapshotOf(profileId)
        val itemList = snap.toItemUiList(itemHelper.items.value)
        val header = buildList {
            if (!hideProfileLabel && titleLabel.isNotBlank()) add(titleLabel)
            if (snap.syncTimeMillis > 0L) {
                add(DateFormat.getDateTimeInstance().format(Date(snap.syncTimeMillis)))
            }
            add("${itemList.size} items")
        }

        val columns = 4
        val cellW = 180
        val cellH = 170
        val gap = 12
        val pad = 24
        val headerLineH = 40
        val headerH = pad + header.size * headerLineH + 12
        val rows = if (itemList.isEmpty()) 1 else ceil(itemList.size / columns.toFloat()).toInt()
        val width = pad * 2 + columns * cellW + (columns - 1) * gap
        val height = headerH + pad + rows * cellH + max(0, rows - 1) * gap + pad

        val scale = exportScale(width, height)
        val bitmap = Bitmap.createBitmap(
            (width * scale).roundToInt(),
            (height * scale).roundToInt(),
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE) // 导出底必须不透明白，黑底图标抠透后透出这里
        // 之后一律按设计尺寸绘制，超预算时整体缩小（文字、图标一起缩）
        canvas.scale(scale, scale)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 26f
        }
        var y = pad + 34f
        header.forEachIndexed { i, line ->
            canvas.drawText(line, pad.toFloat(), y, if (i == 0) titlePaint else subPaint)
            y += headerLineH
        }

        val cellBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F7F5F2") }
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 22f
            textAlign = Paint.Align.CENTER
        }
        val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        if (itemList.isEmpty()) {
            canvas.drawText("(empty)", width / 2f, headerH + 80f, namePaint)
        } else {
            itemList.forEachIndexed { index, item ->
                val col = index % columns
                val row = index / columns
                val left = pad + col * (cellW + gap)
                val top = headerH + row * (cellH + gap)
                val rect = RectF(
                    left.toFloat(),
                    top.toFloat(),
                    (left + cellW).toFloat(),
                    (top + cellH).toFloat(),
                )
                canvas.drawRoundRect(rect, 12f, 12f, cellBg)

                val icon = runCatching { itemIconLoader.loadRawAndroidBitmap(item.id) }.getOrNull()
                val iconSize = 88
                val iconLeft = left + (cellW - iconSize) / 2
                val iconTop = top + 14
                if (icon != null && !icon.isRecycled) {
                    val dst = RectF(
                        iconLeft.toFloat(),
                        iconTop.toFloat(),
                        (iconLeft + iconSize).toFloat(),
                        (iconTop + iconSize).toFloat(),
                    )
                    // 先铺不透明白底，再画黑转透明后的图标，透出白底不发黑
                    val plate = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        style = Paint.Style.FILL
                    }
                    canvas.drawRoundRect(dst, 8f, 8f, plate)
                    val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        isFilterBitmap = true
                        isDither = true
                    }
                    canvas.drawBitmap(icon, null, dst, iconPaint)
                    icon.recycle()
                } else {
                    val ph = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D0D0D0") }
                    canvas.drawRoundRect(
                        RectF(
                            iconLeft.toFloat(),
                            iconTop.toFloat(),
                            (iconLeft + iconSize).toFloat(),
                            (iconTop + iconSize).toFloat(),
                        ),
                        8f,
                        8f,
                        ph,
                    )
                }

                val cx = left + cellW / 2f
                val name = item.name.let { if (it.length > 8) it.take(7) + "…" else it }
                canvas.drawText(name, cx, (top + iconSize + 40).toFloat(), namePaint)
                canvas.drawText("${item.count}", cx, (top + iconSize + 72).toFloat(), countPaint)
            }
        }

        compressPng(bitmap)
    }

    suspend fun renderOperBoxPng(
        profileId: String,
        hideProfileLabel: Boolean = false,
        titleLabel: String = "",
    ): ByteArray? = withContext(Dispatchers.Default) {
        val snap = operBoxRepository.snapshots.value[profileId] ?: OperBoxSnapshot()
        val owned = snap.owned
        val notOwned = snap.notOwned
        val opers = if (owned.isNotEmpty()) owned else notOwned
        val section = if (owned.isNotEmpty()) "Owned" else "Not owned"

        val pad = 24
        val rowH = 64
        val gap = 8
        val headerLineH = 40
        val headerLines = buildList {
            if (!hideProfileLabel && titleLabel.isNotBlank()) add(titleLabel)
            if (snap.syncTimeMillis > 0L) {
                add(DateFormat.getDateTimeInstance().format(Date(snap.syncTimeMillis)))
            }
            add("$section  owned=${owned.size}  notOwned=${notOwned.size}")
        }
        val headerH = pad + headerLines.size * headerLineH + 8
        val width = 900
        val height = headerH + pad + max(1, opers.size) * (rowH + gap) + pad

        val scale = exportScale(width, height)
        val bitmap = Bitmap.createBitmap(
            (width * scale).roundToInt(),
            (height * scale).roundToInt(),
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.scale(scale, scale)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 26f
        }
        var y = pad + 34f
        headerLines.forEachIndexed { i, line ->
            canvas.drawText(line, pad.toFloat(), y, if (i == 0) titlePaint else subPaint)
            y += headerLineH
        }

        val rowBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F3F1ED") }
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 28f
        }
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 24f
            textAlign = Paint.Align.RIGHT
        }
        val rarityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        if (opers.isEmpty()) {
            canvas.drawText("(empty)", pad.toFloat(), headerH + 48f, namePaint)
        } else {
            opers.forEachIndexed { index, op ->
                val top = headerH + index * (rowH + gap)
                val rect = RectF(
                    pad.toFloat(),
                    top.toFloat(),
                    (width - pad).toFloat(),
                    (top + rowH).toFloat(),
                )
                canvas.drawRoundRect(rect, 10f, 10f, rowBg)

                rarityPaint.color = rarityColor(op.rarity)
                canvas.drawText("${op.rarity}★", (pad + 16).toFloat(), top + 40f, rarityPaint)
                canvas.drawText(op.name, pad + 80f, top + 40f, namePaint)
                if (op.own) {
                    val meta = "E${op.elite} Lv${op.level}  P${op.potential}"
                    canvas.drawText(meta, (width - pad - 16).toFloat(), top + 40f, metaPaint)
                }
            }
        }

        compressPng(bitmap)
    }

    private fun rarityColor(rarity: Int): Int = when (rarity) {
        6 -> Color.parseColor("#FF6B35")
        5 -> Color.parseColor("#FFD700")
        4 -> Color.parseColor("#9C7CFF")
        3 -> Color.parseColor("#4FC3F7")
        2 -> Color.parseColor("#A5D6A7")
        else -> Color.GRAY
    }

    private fun compressPng(bitmap: Bitmap): ByteArray? {
        return ByteArrayOutputStream().use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                bitmap.recycle()
                return null
            }
            bitmap.recycle()
            out.toByteArray()
        }
    }
}

/**
 * 导出位图的整体缩放系数。
 *
 * 长图的高度完全由数据量决定（干员按行铺、仓库按 4 列铺），
 * 一个满仓库能算出上万像素高、上百 MB 的 ARGB_8888，低内存机型直接 OOM。
 * 超预算就等比缩小：内容一个不少，只是整体变小。
 */
private fun exportScale(width: Int, height: Int): Float {
    val pixels = width.toLong() * height.toLong()
    if (pixels <= MAX_EXPORT_PIXELS) return 1f
    return sqrt(MAX_EXPORT_PIXELS.toDouble() / pixels.toDouble()).toFloat()
}

/** 约 48 MB 的 ARGB_8888，给压缩输出和图标解码留够余量。 */
private const val MAX_EXPORT_PIXELS = 12_000_000

data class DepotInventoryItemUi(
    val id: String,
    val name: String,
    val count: Int,
    val sortId: Int,
)

/** 二级列表的一张配置档卡片。 */
data class DepotProfileRow(
    val id: String,
    val name: String,
    val isActive: Boolean,
    val hasSynced: Boolean,
    val syncTimeMillis: Long,
    val planCount: Int,
    val unmetCount: Int,
)

/** 一条「库存保持」计划在所属配置档下的进度。 */
data class DepotMaintainPlanUi(
    val nodeId: String,
    val nodeEnabled: Boolean,
    /** 在该节点 plans 里的下标，0 起；写回时按它定位 */
    val planIndex: Int,
    val plan: DepotMaintainPlan,
    val itemId: String,
    val itemName: String,
    val current: Int,
    val target: Int,
    val need: Int,
    val outcome: DepotPlanOutcome,
) {
    /** 未集齐：有目标且库存还不够。目标为 0 的计划不算缺货。 */
    val unmet: Boolean get() = target > 0 && current < target
}

/** 三级库存网格的一格：库存数量 + 该物品的保持计划（没有则为 null）。 */
data class DepotInventoryCellUi(
    val id: String,
    val name: String,
    val count: Int,
    val sortId: Int,
    val plan: DepotMaintainPlanUi?,
) {
    val unmet: Boolean get() = plan?.unmet == true
}

/** 新建计划时要带上的节点状态；[nodeEnabled] 为 false 时面板会提示任务没启用。 */
data class DepotPlanContext(
    val nodeEnabled: Boolean = true,
)

internal fun DepotMaintainPlan.toUi(
    node: TaskChainNode,
    index: Int,
    snap: DepotSnapshot,
    itemMap: Map<String, ItemInfo>,
    isStageOpen: (String) -> Boolean,
): DepotMaintainPlanUi {
    val current = snap.items[dropId] ?: 0
    return DepotMaintainPlanUi(
        nodeId = node.id,
        nodeEnabled = node.enabled,
        planIndex = index,
        plan = this,
        itemId = dropId,
        itemName = itemMap[dropId]?.name ?: dropId,
        current = current,
        target = dropCount,
        need = (dropCount - current).coerceAtLeast(0),
        outcome = depotPlanOutcome(this, current, isStageOpen),
    )
}

internal fun DepotMaintainPlan.unmetIn(snap: DepotSnapshot): Boolean {
    val current = snap.items[dropId] ?: 0
    return dropCount > 0 && current < dropCount
}

/** 一条链上所有库存保持计划，保持节点顺序；没选物品的计划不计入（网格里也没有它）。 */
internal fun List<TaskChainNode>.depotPlans(): List<DepotMaintainPlan> =
    flatMap { node -> (node.config as? DepotMaintainConfig)?.plans.orEmpty() }
        .filter { it.dropId.isNotBlank() }

/** 写回计划时的落点：启用节点优先，与 [TaskChainState.updateDepotMaintainPlans] 的选取一致。 */
internal fun List<TaskChainNode>.firstDepotNode(): TaskChainNode? =
    firstOrNull { it.config is DepotMaintainConfig && it.enabled }
        ?: firstOrNull { it.config is DepotMaintainConfig }

internal fun DepotSnapshot.toItemUiList(itemMap: Map<String, ItemInfo>): List<DepotInventoryItemUi> =
    items.asSequence()
        // 合成玉/寻访凭证/十连寻访凭证不是可刷材料，也不参与库存保持，不进 UI 列表
        .filter { (id, _) -> id !in DRAW_ITEM_IDS }
        .map { (id, count) ->
            val info = itemMap[id]
            DepotInventoryItemUi(
                id = id,
                name = info?.name ?: id,
                count = count,
                sortId = info?.sortId ?: Int.MAX_VALUE,
            )
        }
        .sortedWith(compareBy<DepotInventoryItemUi> { it.sortId }.thenBy { it.id })
        .toList()

private const val ORUNDUM_ID = "4003"
private const val HEADHUNTING_PERMIT_ID = "7003"
private const val TEN_ROLL_HEADHUNTING_PERMIT_ID = "7004"

/** 抽卡资源：库存页不展示（导出仍按原始数据走，别在这里过滤）。 */
private val DRAW_ITEM_IDS = setOf(
    ORUNDUM_ID,
    HEADHUNTING_PERMIT_ID,
    TEN_ROLL_HEADHUNTING_PERMIT_ID,
)
