package com.arproperty.android.feature.map

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.height
import androidx.compose.ui.viewinterop.AndroidView
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment

data class MapUiState(
    val sampleBuildingId: Int = 42,
    val gumiCenter: LatLng = LatLng(36.1195, 128.3445),
)

class MapViewModel : ViewModel() {
    val uiState = MapUiState()
}

private fun hasKakaoMapKey(): Boolean {
    return BuildConfig.HAS_KAKAO_NATIVE_APP_KEY
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
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Map",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ){
        if (hasKakaoMapKey()) {
            KakaoMapContent(
                modifier = Modifier.fillMaxWidth()
            )

        }else{
            PlaceholderCard(
                title = "KAKAO_NATIVE_APP_KEY가 비어 있습니다",
                body = "local.properties에 KAKAO_NATIVE_APP_KEY를 넣으면 카카오맵을 사용할 수 있습니다."
            )
        }

//구글맵 관련 내용이라 일단 주석처리
//        if (BuildConfig.HAS_MAPS_API_KEY) {
//            GoogleMap(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .weight(1f, fill = false),
//                cameraPositionState = cameraPositionState,
//            ) {
//                Marker(
//                    state = MarkerState(position = uiState.gumiCenter),
//                    title = "구미 파일럿 위치",
//                    snippet = "초기 지도 scaffold",
//                )
//            }
//        } else {
//            PlaceholderCard(
//                title = "MAPS_API_KEY가 비어 있습니다",
//                body = "지도 SDK는 연결됐지만 키가 없어 placeholder 상태로 유지됩니다. local.properties에 MAPS_API_KEY를 넣으면 실제 지도가 표시됩니다.",
//            )
//        }

//        PlaceholderCard(
//            title = "지도 화면 Placeholder",
//            body = "여기에 이후 건물 마커, 편의시설 레이어, AR <-> 지도 전환 상태 공유가 추가됩니다.",
//        )
//
//        Button(onClick = { onOpenBuilding(uiState.sampleBuildingId) }) {
//            Text(text = "샘플 건물 상세 열기")
//        }
//
//        Button(onClick = { onOpenLivability(uiState.sampleBuildingId) }) {
//            Text(text = "샘플 생활 점수 열기")
//        }

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = { onOpenBuilding(uiState.sampleBuildingId) }) {
                    Text(text = "건물 상세")
                }

                Button(onClick = { onOpenLivability(uiState.sampleBuildingId) }) {
                    Text(text = "생활 점수")
                }
            }
        }
    }
}


@Composable
private fun KakaoMapContent(
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            MapView(context).apply {
                start(
                    object : MapLifeCycleCallback() {
                        override fun onMapDestroy() {
                            Log.d("KakaoMap", "map destroyed")
                        }

                        override fun onMapError(error: Exception) {
                            Log.e("KakaoMap", "map error", error)
                        }
                    },
                    object : KakaoMapReadyCallback() {
                        override fun onMapReady(kakaoMap: KakaoMap) {
                            Log.d("KakaoMap", "map ready")
                        }
                    },
                )
            }
        },
    )
}
