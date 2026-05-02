package com.arproperty.android.feature.map

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arproperty.android.BuildConfig
import com.arproperty.android.core.designsystem.PlaceholderCard
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

data class MapUiState(
    val sampleBuildingId: Int = 42,
    val gumiCenter: LatLng = LatLng(36.1195, 128.3445),
)

class MapViewModel : ViewModel() {
    val uiState = MapUiState()
}

@Composable
fun MapRoute(
    onOpenBuilding: (Int) -> Unit,
    onOpenLivability: (Int) -> Unit,
    viewModel: MapViewModel = viewModel(),
) {
    val uiState = viewModel.uiState
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(uiState.gumiCenter, 14f)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Map",
            style = MaterialTheme.typography.headlineMedium,
        )

        if (BuildConfig.HAS_MAPS_API_KEY) {
            GoogleMap(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f, fill = false),
                cameraPositionState = cameraPositionState,
            ) {
                Marker(
                    state = MarkerState(position = uiState.gumiCenter),
                    title = "구미 파일럿 위치",
                    snippet = "초기 지도 scaffold",
                )
            }
        } else {
            PlaceholderCard(
                title = "MAPS_API_KEY가 비어 있습니다",
                body = "지도 SDK는 연결됐지만 키가 없어 placeholder 상태로 유지됩니다. local.properties에 MAPS_API_KEY를 넣으면 실제 지도가 표시됩니다.",
            )
        }

        PlaceholderCard(
            title = "지도 화면 Placeholder",
            body = "여기에 이후 건물 마커, 편의시설 레이어, AR <-> 지도 전환 상태 공유가 추가됩니다.",
        )

        Button(onClick = { onOpenBuilding(uiState.sampleBuildingId) }) {
            Text(text = "샘플 건물 상세 열기")
        }

        Button(onClick = { onOpenLivability(uiState.sampleBuildingId) }) {
            Text(text = "샘플 생활 점수 열기")
        }
    }
}
