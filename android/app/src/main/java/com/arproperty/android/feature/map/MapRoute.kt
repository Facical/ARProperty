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
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.camera.CameraUpdateFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.kakao.vectormap.label.LabelOptions
import com.arproperty.android.R
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
data class MapUiState(
    val sampleBuildingId: Int = 42,
    val gumiCenter: LatLng = LatLng.from(36.1195, 128.3445),
)

class MapViewModel : ViewModel() {
    val uiState = MapUiState()
}

private fun hasKakaoMapKey(): Boolean {
    return BuildConfig.HAS_KAKAO_NATIVE_APP_KEY
}

private fun hasPermission(
    context: Context,
    permission: String,
): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        permission
    ) == PackageManager.PERMISSION_GRANTED
}

private fun hasLocationPermission(context: Context): Boolean {
    return hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
        hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
}

@SuppressLint("MissingPermission")
private fun moveToCurrentLocation(
    context: Context,
    kakaoMap: KakaoMap,
) {
    val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    fusedLocationClient.lastLocation
        .addOnSuccessListener { location ->
            if (location != null) {
                val currentPosition = LatLng.from(
                    location.latitude,
                    location.longitude
                )

                val command =
                    CameraUpdateFactory.newCenterPosition(currentPosition)

                kakaoMap.moveCamera(command)
                addCurrentLocationMarker(kakaoMap, currentPosition)
            } else {
                Log.d("Location", "현재 위치를 가져오지 못했음")
            }
        }
        .addOnFailureListener { error ->
            Log.e("Location", "위치 가져오기 실패", error)
        }
}

private fun addCurrentLocationMarker(
    kakaoMap: KakaoMap,
    position: LatLng,
) {
    val labelManager = kakaoMap.labelManager ?: return
    val labelLayer = labelManager.layer ?: return

    labelLayer.removeAll()

    val currentLocationStyles = labelManager.addLabelStyles(
        LabelStyles.from(
            LabelStyle.from(R.drawable.current_location_marker)
                .setAnchorPoint(0.5f, 0.5f)
        )
    )

    val labelOptions = LabelOptions.from(position)
        .setStyles(currentLocationStyles)

    labelLayer.addLabel(labelOptions)

    Log.d("LocationMarker", "현재 위치 마커 추가 완료: $position")
}
@Composable
fun MapRoute(
    onOpenBuilding: (Int) -> Unit,
    onOpenLivability: (Int) -> Unit,
    viewModel: MapViewModel = viewModel(),
) {
    val uiState = viewModel.uiState
    val context = LocalContext.current

    var preparedMap by remember {
        mutableStateOf<KakaoMap?>(null)
    }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val fineLocationGranted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

            val coarseLocationGranted =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (fineLocationGranted || coarseLocationGranted) {
                preparedMap?.let { kakaoMap ->
                    moveToCurrentLocation(context, kakaoMap)
                }
            } else {
                Log.d("Location", "위치 권한이 거부됨")
            }
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
                modifier = Modifier.fillMaxSize(),
                onMapReady = { kakaoMap ->
                    preparedMap = kakaoMap

                    if (hasLocationPermission(context)) {
                        moveToCurrentLocation(context, kakaoMap)
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }
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

            val scrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = { onOpenBuilding(uiState.sampleBuildingId) }) {
                    Text(text = "건물 상세")
                }

                Button(onClick = { onOpenLivability(uiState.sampleBuildingId) }) {
                    Text(text = "생활 점수")
                }

                FilterChip(
                    selected = false,
                    onClick = { /* 의료 표시 ON/OFF */ },
                    label = { Text("의료") },
                )

                FilterChip(
                    selected = false,
                    onClick = { /* 교통 표시 ON/OFF */ },
                    label = { Text("교통") },
                )

                FilterChip(
                    selected = false,
                    onClick = { /* 편의 표시 ON/OFF */ },
                    label = { Text("편의") },
                )

                FilterChip(
                    selected = false,
                    onClick = { /* 안전 표시 ON/OFF */ },
                    label = { Text("안전") },
                )
            }
        }
    }
}


@Composable
private fun KakaoMapContent(
    modifier: Modifier = Modifier,
    onMapReady: (KakaoMap) -> Unit,
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
                            onMapReady(kakaoMap)
                        }
                    },
                )
            }
        },
    )
}
