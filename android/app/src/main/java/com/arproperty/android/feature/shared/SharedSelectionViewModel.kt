package com.arproperty.android.feature.shared

import androidx.lifecycle.ViewModel
import com.arproperty.android.core.model.BuildingSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Activity-scoped 공유 ViewModel. AR ↔ Map 간 선택된 건물을 공유한다.
 * MainActivity 스코프로 가져오면 두 라우트가 동일 인스턴스를 본다.
 */
class SharedSelectionViewModel : ViewModel() {

    private val _selectedBuilding = MutableStateFlow<BuildingSummary?>(null)
    val selectedBuilding: StateFlow<BuildingSummary?> = _selectedBuilding.asStateFlow()

    fun select(building: BuildingSummary?) {
        _selectedBuilding.value = building
    }

    fun clear() {
        _selectedBuilding.value = null
    }
}
