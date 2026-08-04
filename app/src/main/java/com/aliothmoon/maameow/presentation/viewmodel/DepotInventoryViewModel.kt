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
import com.aliothmoon.maameow.data.repository.DepotRepository
import com.aliothmoon.maameow.data.repository.DepotSnapshot
import com.aliothmoon.maameow.data.repository.OperBoxRepository
import com.aliothmoon.maameow.data.repository.OperBoxSnapshot
import com.aliothmoon.maameow.data.repository.toSortedItems
import com.aliothmoon.maameow.data.resource.ItemHelper
import com.aliothmoon.maameow.data.resource.ItemIconLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.DateFormat
import java.util.Date
import kotlin.math.ceil
import kotlin.math.max

/**
 * 库存数据页：上游已改为「配置档」级分片，一档一份仓库/干员快照。
 * 本 VM 直接读当前活跃配置档的 snapshot，不再按 accountTag 多账号分桶。
 */
class DepotInventoryViewModel(
    private val depotRepository: DepotRepository,
    private val operBoxRepository: OperBoxRepository,
    private val itemHelper: ItemHelper,
    private val itemIconLoader: ItemIconLoader,
) : ViewModel() {
    val depotSnapshot: StateFlow<DepotSnapshot> = depotRepository.snapshot
    val operBoxSnapshot: StateFlow<OperBoxSnapshot> = operBoxRepository.snapshot

    val items: StateFlow<List<DepotInventoryItemUi>> = depotRepository.snapshot
        .map { snap -> snap.toItemUiList(itemHelper) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val drawSummary: StateFlow<DepotDrawSummary> = depotRepository.snapshot
        .map { it.drawSummary() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DepotDrawSummary(0, 0, 0))

    fun clearAll() {
        viewModelScope.launch {
            depotRepository.clear()
            operBoxRepository.clear()
        }
    }

    fun exportDepotArkPlanner(): String {
        val snap = depotSnapshot.value
        val sorted = snap.toSortedItems(itemHelper.items.value)
        val itemsJson = sorted.joinToString(",") { """{"id":"${it.id}","have":${it.count}}""" }
        return """{"@type":"@penguin-statistics/depot","items":[$itemsJson]}"""
    }

    fun exportDepotLolicon(): String {
        val snap = depotSnapshot.value
        val sorted = snap.toSortedItems(itemHelper.items.value)
        return "{${sorted.joinToString(",") { "\"${it.id}\":${it.count}" }}}"
    }

    fun exportOperBoxList(): List<OperBoxOperator> {
        val snap = operBoxSnapshot.value
        if (!snap.hasSynced) return emptyList()
        return snap.owned + snap.notOwned
    }

    fun exportOperBoxJson(): String =
        OperBoxExportFormatter.toJson(exportOperBoxList())

    fun exportOperBoxMarkdown(labels: OperBoxExportLabels): String =
        OperBoxExportFormatter.toMarkdown(exportOperBoxList(), labels)

    fun exportOperBoxCsv(labels: OperBoxExportLabels): String =
        OperBoxExportFormatter.toCsv(exportOperBoxList(), labels)

    /**
     * @param hideAccountTag 兼容旧开关：上游已无 accountTag，开启时图头不写配置档占位名，只留时间与数据
     * @param titleLabel 图头标题（通常为「当前配置档」或空）
     */
    suspend fun renderDepotPng(
        hideAccountTag: Boolean = false,
        titleLabel: String = "",
    ): ByteArray? = withContext(Dispatchers.Default) {
        val itemList = items.value.ifEmpty { depotSnapshot.value.toItemUiList(itemHelper) }
        val snap = depotSnapshot.value
        val header = buildList {
            if (!hideAccountTag && titleLabel.isNotBlank()) add(titleLabel)
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

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE) // 导出底必须不透明白，黑底图标抠透后透出这里

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
                canvas.drawText("x${item.count}", cx, (top + iconSize + 72).toFloat(), countPaint)
            }
        }

        compressPng(bitmap)
    }

    suspend fun renderOperBoxPng(
        hideAccountTag: Boolean = false,
        titleLabel: String = "",
    ): ByteArray? = withContext(Dispatchers.Default) {
        val snap = operBoxSnapshot.value
        val owned = snap.owned
        val notOwned = snap.notOwned
        val opers = if (owned.isNotEmpty()) owned else notOwned
        val section = if (owned.isNotEmpty()) "Owned" else "Not owned"

        val pad = 24
        val rowH = 64
        val gap = 8
        val headerLineH = 40
        val headerLines = buildList {
            if (!hideAccountTag && titleLabel.isNotBlank()) add(titleLabel)
            if (snap.syncTimeMillis > 0L) {
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

data class DepotInventoryItemUi(
    val id: String,
    val name: String,
    val count: Int,
    val sortId: Int,
)

data class DepotDrawSummary(
    val total: Int,
    val orundum: Int,
    val permits: Int,
)

private fun DepotSnapshot.toItemUiList(itemHelper: ItemHelper): List<DepotInventoryItemUi> =
    items.asSequence()
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

fun DepotSnapshot.drawSummary(): DepotDrawSummary {
    val orundumDraws = items[ORUNDUM_ID].orZero() / ORUNDUM_PER_DRAW
    val permitDraws = items[HEADHUNTING_PERMIT_ID].orZero() +
        items[TEN_ROLL_HEADHUNTING_PERMIT_ID].orZero() * TEN_ROLL_DRAW_COUNT
    return DepotDrawSummary(
        total = orundumDraws + permitDraws,
        orundum = orundumDraws,
        permits = permitDraws,
    )
}

private fun Int?.orZero(): Int = this ?: 0
private const val ORUNDUM_ID = "4003"
private const val HEADHUNTING_PERMIT_ID = "7003"
private const val TEN_ROLL_HEADHUNTING_PERMIT_ID = "7004"
private const val ORUNDUM_PER_DRAW = 600
private const val TEN_ROLL_DRAW_COUNT = 10
