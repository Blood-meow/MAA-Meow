package com.aliothmoon.maameow.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliothmoon.maameow.data.repository.DepotAccountSnapshot
import com.aliothmoon.maameow.data.repository.DepotRepository
import com.aliothmoon.maameow.data.resource.ItemHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DepotInventoryViewModel(
    private val depotRepository: DepotRepository,
    private val itemHelper: ItemHelper,
) : ViewModel() {
    private val _accounts = MutableStateFlow<List<DepotAccountSnapshot>>(emptyList())
    val accounts: StateFlow<List<DepotAccountSnapshot>> = _accounts.asStateFlow()

    private val _selectedAccountTag = MutableStateFlow<String?>(null)
    val selectedAccountTag: StateFlow<String?> = _selectedAccountTag.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _accounts.value = depotRepository.listAccountSnapshotsForActiveProfile()
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

fun DepotAccountSnapshot.availableDraws(): Int =
    snapshot.items[ORUNDUM_ID].orZero() / ORUNDUM_PER_DRAW +
        snapshot.items[HEADHUNTING_PERMIT_ID].orZero() +
        snapshot.items[TEN_ROLL_HEADHUNTING_PERMIT_ID].orZero() * TEN_ROLL_DRAW_COUNT

private fun Int?.orZero(): Int = this ?: 0

private const val ORUNDUM_ID = "4003"
private const val HEADHUNTING_PERMIT_ID = "4004"
private const val TEN_ROLL_HEADHUNTING_PERMIT_ID = "4005"
private const val ORUNDUM_PER_DRAW = 600
private const val TEN_ROLL_DRAW_COUNT = 10
