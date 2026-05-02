package com.arproperty.android.feature.building

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arproperty.android.core.designsystem.PlaceholderCard

data class BuildingDetailUiState(
    val defaultComplexId: Int = 10,
)

class BuildingDetailViewModel : ViewModel() {
    val uiState = BuildingDetailUiState()
}

@Composable
fun BuildingDetailRoute(
    buildingId: Int,
    onOpenComplex: (Int) -> Unit,
    onOpenLivability: (Int) -> Unit,
    viewModel: BuildingDetailViewModel = viewModel(),
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
            text = "Building Detail",
            style = MaterialTheme.typography.headlineMedium,
        )

        PlaceholderCard(
            title = "건물 ID: $buildingId",
            body = "이 화면은 건물 기본 정보, 거래 이력, 상세 카드 UI가 들어갈 자리입니다.",
        )

        PlaceholderCard(
            title = "향후 연결 예정",
            body = "건물 상세 API, 거래 이력 API, 필터/가격 그래프 영역이 이후 단계에서 확장됩니다.",
        )

        Button(onClick = { onOpenComplex(uiState.defaultComplexId) }) {
            Text(text = "샘플 단지 상세 열기")
        }

        Button(onClick = { onOpenLivability(buildingId) }) {
            Text(text = "생활 점수 화면 열기")
        }
    }
}
