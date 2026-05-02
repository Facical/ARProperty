package com.arproperty.android.feature.livability

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arproperty.android.core.designsystem.PlaceholderCard

data class LivabilityUiState(
    val samplePreset: String = "default",
)

class LivabilityViewModel : ViewModel() {
    val uiState = LivabilityUiState()
}

@Composable
fun LivabilityRoute(
    buildingId: Int,
    viewModel: LivabilityViewModel = viewModel(),
) {
    val uiState = viewModel.uiState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Livability",
            style = MaterialTheme.typography.headlineMedium,
        )

        PlaceholderCard(
            title = "건물 ID: $buildingId",
            body = "총점, 등급, 카테고리별 점수, 비교 UI가 들어갈 영역입니다.",
        )

        PlaceholderCard(
            title = "현재 preset",
            body = "초기 기본값은 `${uiState.samplePreset}` 입니다. 이후 프리셋 전환과 비교표가 여기에 연결됩니다.",
        )
    }
}
