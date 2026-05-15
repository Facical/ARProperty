// 카카오맵 + 카테고리별 편의시설 마커 토글 화면 (백엔드 /api/v1/livability/infra/nearby 연동)
package com.arproperty.android.feature.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arproperty.android.BuildConfig
import com.arproperty.android.R
import com.arproperty.android.app.ArPropertyApplication
import com.arproperty.android.core.model.InfraNearby
import com.arproperty.android.core.network.LivabilityRepository
import com.google.android.gms.location.LocationServices
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextBuilder
import com.kakao.vectormap.label.LabelTextStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity
import android.content.ContextWrapper
import com.arproperty.android.feature.shared.SharedSelectionViewModel

private fun Context.requireActivity(): ComponentActivity {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is ComponentActivity) return current
        current = current.baseContext
    }
    error("Context is not a ComponentActivity")
}

// 6개 카테고리 (백엔드 category enum과 일치)
internal val INFRA_CATEGORIES: List<Pair<String, String>> = listOf(
    "medical" to "의료",
    "education" to "교육",
    "convenience" to "편의",
    "transport" to "교통",
    "safety" to "안전",
    "leisure" to "여가",
)

// 카테고리별 라벨 색상 (RGB)
internal val INFRA_CATEGORY_COLOR: Map<String, Int> = mapOf(
    "medical" to 0xFFE53935.toInt(),     // red
    "education" to 0xFF1E88E5.toInt(),   // blue
    "convenience" to 0xFFFB8C00.toInt(), // orange
    "transport" to 0xFF43A047.toInt(),   // green
    "safety" to 0xFF8E24AA.toInt(),      // purple
    "leisure" to 0xFF6D4C41.toInt(),     // brown
)

data class MapUiState(
    val sampleBuildingId: Int = 42,
    val gumiCenter: LatLng = LatLng.from(36.1195, 128.3445),
    val centerLat: Double = 36.1195,
    val centerLon: Double = 128.3445,
    val radius: Int = 1000,
    val activeCategories: Set<String> = emptySet(),
    val infraByCategory: Map<String, List<InfraNearby>> = emptyMap(),
    val loading: Set<String> = emptySet(),
    val error: String? = null,
)

class MapViewModel(
    private val livabilityRepository: LivabilityRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    fun setCenter(lat: Double, lon: Double) {
        _uiState.update { it.copy(centerLat = lat, centerLon = lon) }
        // 활성화된 카테고리는 새 위치 기준으로 재조회
        _uiState.value.activeCategories.forEach { fetchCategory(it) }
    }

    fun toggleCategory(category: String) {
        val isOn = category in _uiState.value.activeCategories
        if (isOn) {
            _uiState.update {
                it.copy(
                    activeCategories = it.activeCategories - category,
                    infraByCategory = it.infraByCategory - category,
                )
            }
        } else {
            _uiState.update { it.copy(activeCategories = it.activeCategories + category) }
            fetchCategory(category)
        }
    }

    private fun fetchCategory(category: String) {
        _uiState.update { it.copy(loading = it.loading + category, error = null) }
        viewModelScope.launch {
            val state = _uiState.value
            val result = livabilityRepository.getInfraNearby(
                lat = state.centerLat,
                lon = state.centerLon,
                radius = state.radius,
                category = category,
            )
            _uiState.update { s ->
                val updated = s.infraByCategory.toMutableMap()
                result.onSuccess { list -> updated[category] = list }
                s.copy(
                    loading = s.loading - category,
                    infraByCategory = updated,
                    error = result.exceptionOrNull()?.message ?: s.error,
                )
            }
        }
    }
}

class MapViewModelFactory(
    private val livabilityRepository: LivabilityRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MapViewModel(livabilityRepository) as T
    }
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
    onLocation: (Double, Double) -> Unit,
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
                onLocation(location.latitude, location.longitude)
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

// 활성 카테고리 마커들을 한 번에 다시 그린다 (현재 위치 마커는 유지)
private fun renderInfraMarkers(
    kakaoMap: KakaoMap,
    infraByCategory: Map<String, List<InfraNearby>>,
) {
    val labelManager = kakaoMap.labelManager ?: return
    val labelLayer = labelManager.layer ?: return

    // 카테고리별 마커는 텍스트만 다르게 표시 (아이콘은 동일 placeholder)
    // 추후 카테고리별 vector drawable로 교체 가능
    val baseStyle = labelManager.addLabelStyles(
        LabelStyles.from(
            LabelStyle.from(R.drawable.current_location_marker)
                .setAnchorPoint(0.5f, 0.5f)
                .setTextStyles(LabelTextStyle.from(28, 0xFF333333.toInt()))
        )
    )

    infraByCategory.forEach { (category, list) ->
        val labelText = INFRA_CATEGORIES.firstOrNull { it.first == category }?.second
            ?: category.take(2)
        list.forEach { item ->
            val opt = LabelOptions.from(LatLng.from(item.lat, item.lon))
                .setStyles(baseStyle)
                .setTexts(LabelTextBuilder().setTexts(labelText))
            labelLayer.addLabel(opt)
        }
    }
}

@Composable
fun MapRoute(
    onOpenBuilding: (Int) -> Unit,
    onOpenLivability: (Int) -> Unit,
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as ArPropertyApplication).appContainer
    val viewModel: MapViewModel = viewModel(
        factory = MapViewModelFactory(appContainer.livabilityRepository),
    )
    val uiState by viewModel.uiState.collectAsState()
    val activity = remember(context) { context.requireActivity() }
    val sharedVm: SharedSelectionViewModel = viewModel(viewModelStoreOwner = activity)
    val selectedBuilding by sharedVm.selectedBuilding.collectAsState()

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
                    moveToCurrentLocation(context, kakaoMap) { lat, lon ->
                        viewModel.setCenter(lat, lon)
                    }
                }
            } else {
                Log.d("Location", "위치 권한이 거부됨")
            }
        }

    // 활성 카테고리/데이터 변화 시 마커 갱신 (현재 위치 마커도 함께 다시 추가)
    LaunchedEffect(preparedMap, uiState.infraByCategory) {
        val map = preparedMap ?: return@LaunchedEffect
        val labelLayer = map.labelManager?.layer ?: return@LaunchedEffect
        labelLayer.removeAll()
        addCurrentLocationMarker(map, LatLng.from(uiState.centerLat, uiState.centerLon))
        renderInfraMarkers(map, uiState.infraByCategory)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Map",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
        selectedBuilding?.let {
            Text(
                text = "AR에서 선택: ${it.complexName} ${it.dongName}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

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
                        moveToCurrentLocation(context, kakaoMap) { lat, lon ->
                            viewModel.setCenter(lat, lon)
                        }
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
            com.arproperty.android.core.designsystem.PlaceholderCard(
                title = "KAKAO_NATIVE_APP_KEY가 비어 있습니다",
                body = "local.properties에 KAKAO_NATIVE_APP_KEY를 넣으면 카카오맵을 사용할 수 있습니다."
            )
        }

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

                INFRA_CATEGORIES.forEach { (key, label) ->
                    FilterChip(
                        selected = key in uiState.activeCategories,
                        onClick = { viewModel.toggleCategory(key) },
                        label = { Text(label) },
                    )
                }
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
