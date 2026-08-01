package com.aliothmoon.maameow.presentation.viewmodel

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.DateFormat
import java.util.Date

class DepotInventoryViewModel(
    private val depotRepository: DepotRepository,
    private val operBoxRepository: OperBoxRepository,
    private val itemHelper: ItemHelper,
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

    suspend fun renderDepotPng(accountTag: String): ByteArray? = withContext(Dispatchers.Default) {
        val items = itemsForAccount(accountTag)
        val snap = depotSnapshot(accountTag)
        val lines = buildList {
            add("Depot / $accountTag")
            if (snap != null && snap.syncTimeMillis > 0L) {
                add("Updated: ${DateFormat.getDateTimeInstance().format(Date(snap.syncTimeMillis))}")
            }
            add("Items: ${items.size}")
            add("")
            if (items.isEmpty()) {
                add("(empty)")
            } else {
                items.forEach { add("${it.name}  x${it.count}  (${it.id})") }
            }
        }
        renderTextTablePng(lines)
    }

    suspend fun renderOperBoxPng(accountTag: String): ByteArray? = withContext(Dispatchers.Default) {
        val snap = operBoxSnapshot(accountTag)
        val opers = exportOperBoxList(accountTag)
        val lines = buildList {
            add("Operators / $accountTag")
            if (snap != null && snap.syncTimeMillis > 0L) {
                add("Updated: ${DateFormat.getDateTimeInstance().format(Date(snap.syncTimeMillis))}")
            }
            add("Owned: ${snap?.owned?.size ?: 0}  Not owned: ${snap?.notOwned?.size ?: 0}")
            add("")
            if (opers.isEmpty()) {
                add("(empty)")
            } else {
                opers.forEach { op ->
                    val own = if (op.own) "Y" else "N"
                    add("${op.name}  R${op.rarity}  E${op.elite} Lv${op.level}  P${op.potential}  own=$own")
                }
            }
        }
        renderTextTablePng(lines)
    }

    private fun renderTextTablePng(lines: List<String>): ByteArray? {
        val padding = 32f
        val lineHeight = 36f
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 34f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 28f
            typeface = Typeface.MONOSPACE
        }
        val width = lines.maxOfOrNull { line ->
            val paint = if (line === lines.firstOrNull()) titlePaint else bodyPaint
            paint.measureText(line)
        }?.let { it + padding * 2 }?.toInt()?.coerceAtLeast(720) ?: 720
        val height = (padding * 2 + lineHeight * lines.size).toInt().coerceAtLeast(200)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        var y = padding + lineHeight
        lines.forEachIndexed { index, line ->
            val paint = if (index == 0) titlePaint else bodyPaint
            canvas.drawText(line, padding, y, paint)
            y += lineHeight
        }
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
