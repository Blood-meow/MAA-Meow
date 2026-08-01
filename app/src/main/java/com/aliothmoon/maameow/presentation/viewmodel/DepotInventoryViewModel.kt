package com.aliothmoon.maameow.presentation.viewmodel

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliothmoon.maameow.data.model.toolbox.OperBoxExportFormatter
import com.aliothmoon.maameow.data.model.toolbox.OperBoxExportLabels
import com.aliothmoon.maameow.data.model.toolbox.OperBoxOperator
import com.aliothmoon.maameow.data.repository.DepotAccountSnapshot
import com.aliothmoon.maameow.data.repository.DepotRepository
import com.aliothmoon.maameow.data.repository.DepotSnapshot
import com.aliothmoon.maameow.data.repository.OperBoxAccountSnapshot
import com.aliothmoon.maameow.data.repository.OperBoxRepository
import com.aliothmoon.maameow.data.repository.OperBoxSnapshot
import com.aliothmoon.maameow.data.repository.toSortedItems
import com.aliothmoon.maameow.data.resource.ItemHelper
import com.aliothmoon.maameow.data.resource.ItemIconLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.DateFormat
import java.util.Date
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class DepotInventoryViewModel(
    private val depotRepository: DepotRepository,
    private val operBoxRepository: OperBoxRepository,
    private val itemHelper: ItemHelper,
    private val itemIconLoader: ItemIconLoader,
) : ViewModel() {
    private val _accounts = MutableStateFlow<List<DepotAccountSnapshot>>(emptyList())
    val accounts: StateFlow<List<DepotAccountSnapshot>> = _accounts.asStateFlow()

    private val _operBoxAccounts = MutableStateFlow<List<OperBoxAccountSnapshot>>(emptyList())
    val operBoxAccounts: StateFlow<List<OperBoxAccountSnapshot>> = _operBoxAccounts.asStateFlow()

    private val _selectedAccountTag = MutableStateFlow<String?>(null)
    val selectedAccountTag: StateFlow<String?> = _selectedAccountTag.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _accounts.value = depotRepository.listAccountSnapshotsForActiveProfile()
            _operBoxAccounts.value = operBoxRepository.listAccountSnapshotsForActiveProfile()
        }
    }

    fun selectAccount(accountTag: String) {
        _selectedAccountTag.value = accountTag
    }

    fun clearSelection() {
        _selectedAccountTag.value = null
    }

    fun deleteAccount(accountTag: String) {
        viewModelScope.launch {
            depotRepository.dropAccount(accountTag)
            operBoxRepository.dropAccount(accountTag)
            if (_selectedAccountTag.value == accountTag) {
                _selectedAccountTag.value = null
            }
            refresh()
        }
    }

    fun itemsForAccount(accountTag: String): List<DepotInventoryItemUi> {
        val snapshot = _accounts.value.firstOrNull { it.accountTag == accountTag }?.snapshot ?: return emptyList()
        return snapshot.items
            .asSequence()
            .map { (id, count) ->
                val info = itemHelper.getItemInfo(id)
                DepotInventoryItemUi(
                    id = id,
                    name = info?.name ?: id,
                    count = count,
                    sortId = info?.sortId ?: Int.MAX_VALUE,
                )
            }
            .sortedWith(compareBy<DepotInventoryItemUi> { it.sortId }.thenBy { it.id })
            .toList()
    }

    fun depotSnapshot(accountTag: String): DepotSnapshot? =
        _accounts.value.firstOrNull { it.accountTag == accountTag }?.snapshot

    fun operBoxSnapshot(accountTag: String): OperBoxSnapshot? =
        _operBoxAccounts.value.firstOrNull { it.accountTag == accountTag }?.snapshot

    fun exportDepotArkPlanner(accountTag: String): String {
        val snap = depotSnapshot(accountTag) ?: return """{"@type":"@penguin-statistics/depot","items":[]}"""
        val items = snap.toSortedItems(itemHelper.items.value)
        val itemsJson = items.joinToString(",") { """{"id":"${it.id}","have":${it.count}}""" }
        return """{"@type":"@penguin-statistics/depot","items":[$itemsJson]}"""
    }

    fun exportDepotLolicon(accountTag: String): String {
        val snap = depotSnapshot(accountTag) ?: return "{}"
        val items = snap.toSortedItems(itemHelper.items.value)
        return "{${items.joinToString(",") { "\"${it.id}\":${it.count}" }}}"
    }

    fun exportOperBoxList(accountTag: String): List<OperBoxOperator> {
        val snap = operBoxSnapshot(accountTag) ?: return emptyList()
        if (!snap.hasSynced) return emptyList()
        return snap.owned + snap.notOwned
    }

    fun exportOperBoxJson(accountTag: String): String =
        OperBoxExportFormatter.toJson(exportOperBoxList(accountTag))

    fun exportOperBoxMarkdown(accountTag: String, labels: OperBoxExportLabels): String =
        OperBoxExportFormatter.toMarkdown(exportOperBoxList(accountTag), labels)

    fun exportOperBoxCsv(accountTag: String, labels: OperBoxExportLabels): String =
        OperBoxExportFormatter.toCsv(exportOperBoxList(accountTag), labels)

    /**
     * 导出与库存详情页「库存」Tab 同构的网格图：
     * 图标 + 名称 + x数量，按 sortId 排序。
     */
    suspend fun renderDepotPng(accountTag: String): ByteArray? = withContext(Dispatchers.Default) {
        val items = itemsForAccount(accountTag)
        val snap = depotSnapshot(accountTag)
        val header = buildList {
            add(accountTag)
            if (snap != null && snap.syncTimeMillis > 0L) {
                add(DateFormat.getDateTimeInstance().format(Date(snap.syncTimeMillis)))
            }
            add("${items.size} items")
        }

        val columns = 4
        val cellW = 180
        val cellH = 170
        val gap = 12
        val pad = 24
        val headerLineH = 40
        val headerH = pad + header.size * headerLineH + 12

        val rows = if (items.isEmpty()) 1 else ceil(items.size / columns.toFloat()).toInt()
        val width = pad * 2 + columns * cellW + (columns - 1) * gap
        val height = headerH + pad + rows * cellH + max(0, rows - 1) * gap + pad

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

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

        val cellBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F0EEEA") }
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

        if (items.isEmpty()) {
            canvas.drawText("(empty)", width / 2f, headerH + 80f, namePaint)
        } else {
            items.forEachIndexed { index, item ->
                val col = index % columns
                val row = index / columns
                val left = pad + col * (cellW + gap)
                val top = headerH + row * (cellH + gap)
                val rect = RectF(left.toFloat(), top.toFloat(), (left + cellW).toFloat(), (top + cellH).toFloat())
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
                    // 先铺不透明底，再画原图，避免导出图 item 图标发透
                    val plate = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
                    canvas.drawRoundRect(dst, 8f, 8f, plate)
                    val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
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
                canvas.drawText("x${item.count}", cx, (top + iconSize + 72).toFloat(), countPaint)
            }
        }

        compressPng(bitmap)
    }

    /**
     * 导出与库存详情页「干员」Tab 同构的列表：
     * 稀有度色 + 名称 + E/Lv/潜能（已拥有）。
     */
    suspend fun renderOperBoxPng(accountTag: String): ByteArray? = withContext(Dispatchers.Default) {
        val snap = operBoxSnapshot(accountTag)
        val owned = snap?.owned.orEmpty()
        val notOwned = snap?.notOwned.orEmpty()
        // 与详情页默认 Tab 一致：优先导出已拥有；若空则未拥有
        val opers = if (owned.isNotEmpty()) owned else notOwned
        val section = if (owned.isNotEmpty()) "Owned" else "Not owned"

        val pad = 24
        val rowH = 64
        val gap = 8
        val headerLineH = 40
        val headerLines = buildList {
            add(accountTag)
            if (snap != null && snap.syncTimeMillis > 0L) {
                add(DateFormat.getDateTimeInstance().format(Date(snap.syncTimeMillis)))
            }
            add("$section  owned=${owned.size}  notOwned=${notOwned.size}")
        }
        val headerH = pad + headerLines.size * headerLineH + 8
        val width = 900
        val height = headerH + pad + max(1, opers.size) * (rowH + gap) + pad

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

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

                val nameX = pad + 80f
                canvas.drawText(op.name, nameX, top + 40f, namePaint)

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

data class DepotInventoryItemUi(
    val id: String,
    val name: String,
    val count: Int,
    val sortId: Int,
)

internal fun mergeAccountRows(
    depotAccounts: List<DepotAccountSnapshot>,
    operBoxAccounts: List<OperBoxAccountSnapshot>,
): List<DepotAccountSnapshot> {
    val depotByTag = depotAccounts.associateBy { it.accountTag }
    return (depotByTag.keys + operBoxAccounts.map { it.accountTag })
        .distinct()
        .sorted()
        .map { tag -> depotByTag[tag] ?: DepotAccountSnapshot(tag, DepotSnapshot()) }
}

fun DepotAccountSnapshot.drawSummary(): DepotDrawSummary {
    val orundumDraws = snapshot.items[ORUNDUM_ID].orZero() / ORUNDUM_PER_DRAW
    val permitDraws = snapshot.items[HEADHUNTING_PERMIT_ID].orZero() +
        snapshot.items[TEN_ROLL_HEADHUNTING_PERMIT_ID].orZero() * TEN_ROLL_DRAW_COUNT
    return DepotDrawSummary(
        total = orundumDraws + permitDraws,
        orundum = orundumDraws,
        permits = permitDraws,
    )
}

data class DepotDrawSummary(
    val total: Int,
    val orundum: Int,
    val permits: Int,
)

private fun Int?.orZero(): Int = this ?: 0
private const val ORUNDUM_ID = "4003"
private const val HEADHUNTING_PERMIT_ID = "7003"
private const val TEN_ROLL_HEADHUNTING_PERMIT_ID = "7004"
private const val ORUNDUM_PER_DRAW = 600
private const val TEN_ROLL_DRAW_COUNT = 10
