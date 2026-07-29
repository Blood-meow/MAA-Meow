package com.aliothmoon.maameow.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliothmoon.maameow.data.repository.DepotAccountSnapshot
import com.aliothmoon.maameow.data.repository.DepotRepository
import com.aliothmoon.maameow.data.repository.DepotSnapshot
import com.aliothmoon.maameow.data.repository.OperBoxAccountSnapshot
import com.aliothmoon.maameow.data.repository.OperBoxRepository
import com.aliothmoon.maameow.data.resource.ItemHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
