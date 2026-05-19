package com.arproperty.android.feature.ar

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.arproperty.android.BuildConfig
import com.arproperty.android.app.ArPropertyApplication
import com.arproperty.android.core.common.arRequiredPermissions
import com.arproperty.android.core.common.hasAllPermissions
import com.arproperty.android.core.designsystem.NotSupportedState
import com.arproperty.android.core.designsystem.PermissionRequiredState
import com.arproperty.android.core.model.BuildingDetail
import com.arproperty.android.core.model.BuildingSummary
import com.arproperty.android.core.model.TradeItem
import com.arproperty.android.core.network.BuildingRepository
import com.arproperty.android.feature.ar.ui.DetailPopupContent
import com.arproperty.android.feature.ar.ui.PanelContent
import com.arproperty.android.feature.shared.SharedSelectionViewModel
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import com.google.android.gms.location.LocationServices
import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.gesture.GestureDetector
import io.github.sceneview.gesture.MoveGestureDetector
import io.github.sceneview.gesture.RotateGestureDetector
import io.github.sceneview.gesture.ScaleGestureDetector
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.Node
import io.github.sceneview.node.ViewNode2
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberNodes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val NEARBY_RADIUS_METERS = 500
private const val SELECTED_SCALE = 1.6f
private const val DEFAULT_SCALE = 1.0f
private const val ANCHOR_ALTITUDE_OFFSET_METERS = 5.0
private const val DEFAULT_OKGYE_LAT = 36.13918
private const val DEFAULT_OKGYE_LON = 128.42137
private const val FALLBACK_MARKER_LIMIT = 5

data class ArUiState(
    val nearbyBuildings: List<BuildingSummary> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val centerLat: Double? = null,
    val centerLon: Double? = null,
    val detailBuildingId: Int? = null,
    val selectedBuildingDetail: BuildingDetail? = null,
    val isDetailLoading: Boolean = false,
    val detailError: String? = null,
    val tradesBuildingId: Int? = null,
    val selectedBuildingTrades: List<TradeItem> = emptyList(),
    val isTradesLoading: Boolean = false,
    val tradesError: String? = null,
    val tradesLoaded: Boolean = false,
)

class ArViewModel(
    private val buildingRepository: BuildingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArUiState())
    val uiState: StateFlow<ArUiState> = _uiState.asStateFlow()

    fun loadNearby(lat: Double, lon: Double, radius: Int = NEARBY_RADIUS_METERS) {
        _uiState.update {
            it.copy(isLoading = true, error = null, centerLat = lat, centerLon = lon)
        }
        viewModelScope.launch {
            buildingRepository.getNearbyBuildings(lat, lon, radius)
                .onSuccess { list ->
                    _uiState.update { it.copy(isLoading = false, nearbyBuildings = list) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "주변 건물 조회 실패")
                    }
                }
        }
    }

    fun loadBuildingDetail(buildingId: Int) {
        val current = _uiState.value
        if (
            current.detailBuildingId == buildingId &&
            (current.isDetailLoading || current.selectedBuildingDetail != null)
        ) {
            return
        }

        _uiState.update {
            it.copy(
                detailBuildingId = buildingId,
                selectedBuildingDetail = if (it.detailBuildingId == buildingId) {
                    it.selectedBuildingDetail
                } else {
                    null
                },
                isDetailLoading = true,
                detailError = null,
            )
        }

        viewModelScope.launch {
            buildingRepository.getBuildingDetail(buildingId)
                .onSuccess { detail ->
                    _uiState.update {
                        if (it.detailBuildingId == buildingId) {
                            it.copy(
                                selectedBuildingDetail = detail,
                                isDetailLoading = false,
                                detailError = null,
                            )
                        } else {
                            it
                        }
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        if (it.detailBuildingId == buildingId) {
                            it.copy(
                                selectedBuildingDetail = null,
                                isDetailLoading = false,
                                detailError = e.message ?: "건물 상세 조회 실패",
                            )
                        } else {
                            it
                        }
                    }
                }
        }
    }

    fun loadBuildingTrades(buildingId: Int) {
        val current = _uiState.value
        if (
            current.tradesBuildingId == buildingId &&
            (current.isTradesLoading || current.tradesLoaded)
        ) {
            return
        }

        _uiState.update {
            it.copy(
                tradesBuildingId = buildingId,
                selectedBuildingTrades = if (it.tradesBuildingId == buildingId) {
                    it.selectedBuildingTrades
                } else {
                    emptyList()
                },
                isTradesLoading = true,
                tradesError = null,
                tradesLoaded = false,
            )
        }

        viewModelScope.launch {
            buildingRepository.getBuildingTrades(buildingId)
                .onSuccess { trades ->
                    _uiState.update {
                        if (it.tradesBuildingId == buildingId) {
                            it.copy(
                                selectedBuildingTrades = trades,
                                isTradesLoading = false,
                                tradesError = null,
                                tradesLoaded = true,
                            )
                        } else {
                            it
                        }
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        if (it.tradesBuildingId == buildingId) {
                            it.copy(
                                selectedBuildingTrades = emptyList(),
                                isTradesLoading = false,
                                tradesError = e.message ?: "거래 이력 조회 실패",
                                tradesLoaded = false,
                            )
                        } else {
                            it
                        }
                    }
                }
        }
    }

    companion object {
        fun factory(repo: BuildingRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { ArViewModel(repo) }
        }
    }
}

private fun Context.requireActivity(): ComponentActivity {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is ComponentActivity) return current
        current = current.baseContext
    }
    error("Context is not a ComponentActivity")
}

@Composable
fun ArRoute(
    onOpenBuilding: (Int) -> Unit,
    onOpenLivability: (Int) -> Unit,
    onOpenMap: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.requireActivity() }
    val container = (context.applicationContext as ArPropertyApplication).appContainer

    val viewModel: ArViewModel = viewModel(
        factory = ArViewModel.factory(container.buildingRepository),
    )
    val sharedVm: SharedSelectionViewModel =
        viewModel(viewModelStoreOwner = activity)

    val uiState by viewModel.uiState.collectAsState()
    val selectedBuilding by sharedVm.selectedBuilding.collectAsState()

    var permissionRefreshTrigger by remember { mutableStateOf(0) }
    val hasPermissions = remember(permissionRefreshTrigger) {
        context.hasAllPermissions(arRequiredPermissions)
    }
    val availability = remember {
        ArCoreApk.getInstance().checkAvailability(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionRefreshTrigger++
    }

    when {
        !hasPermissions -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                PermissionRequiredState(
                    title = "권한이 필요합니다",
                    body = "AR 화면은 카메라와 위치 권한이 있어야 초기 점검과 위치 기반 안내를 진행할 수 있습니다.",
                    actionLabel = "권한 요청",
                    onActionClick = {
                        permissionLauncher.launch(arRequiredPermissions.toTypedArray())
                    },
                )
            }
        }
        !availability.isSupported -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                NotSupportedState(
                    title = "ARCore 미지원 환경",
                    body = "현재 기기 또는 환경에서는 ARCore를 바로 사용할 수 없습니다. 그래도 지도와 상세 흐름은 계속 확인할 수 있습니다.",
                )
            }
        }
        else -> {
            ArCameraSceneWithPanel(
                uiState = uiState,
                selectedBuilding = selectedBuilding,
                onLoadNearby = viewModel::loadNearby,
                onLoadBuildingDetail = viewModel::loadBuildingDetail,
                onLoadBuildingTrades = viewModel::loadBuildingTrades,
                onSelect = { sharedVm.select(it) },
                onOpenMap = onOpenMap,
                onOpenBuildingDetail = { id -> onOpenBuilding(id) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArCameraSceneWithPanel(
    uiState: ArUiState,
    selectedBuilding: BuildingSummary?,
    onLoadNearby: (Double, Double) -> Unit,
    onLoadBuildingDetail: (Int) -> Unit,
    onLoadBuildingTrades: (Int) -> Unit,
    onSelect: (BuildingSummary) -> Unit,
    onOpenMap: () -> Unit,
    onOpenBuildingDetail: (Int) -> Unit,
) {
    val context = LocalContext.current
    var trackingState by remember { mutableStateOf("초기화 대기") }
    var trackingFailure by remember { mutableStateOf<TrackingFailureReason?>(null) }
    var geospatialStatus by remember {
        mutableStateOf(
            if (BuildConfig.HAS_GEOSPATIAL_API_KEY) "Geospatial: 활성 대기"
            else "Geospatial: 키 없음 (비활성)",
        )
    }
    var anchorStatus by remember {
        mutableStateOf(
            if (BuildConfig.HAS_GEOSPATIAL_API_KEY) "Anchor: Earth tracking 대기"
            else "Anchor: Geospatial 비활성",
        )
    }
    var showDetail by remember { mutableStateOf(false) }
    var fallbackLoadRequested by remember { mutableStateOf(false) }
    var geospatialLoadRequested by remember { mutableStateOf(false) }
    var showDebug by remember { mutableStateOf(false) }
    var lastAnchorError by remember { mutableStateOf<String?>(null) }
    var cameraLat by remember { mutableStateOf<Double?>(null) }
    var cameraLon by remember { mutableStateOf<Double?>(null) }

    val container = (context.applicationContext as ArPropertyApplication).appContainer
    val baseUrlStore = container.baseUrlStore
    val defaultBaseUrl = container.baseBuildUrl
    val currentBaseUrl = baseUrlStore.get() ?: defaultBaseUrl

    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val childNodes = rememberNodes()
    val nodeMap = remember { mutableStateMapOf<Int, AnchorNode>() }
    val cubeMap = remember { mutableStateMapOf<Int, CubeNode>() }
    val labelMap = remember { mutableStateMapOf<Int, ViewNode2>() }
    val fallbackNodeMap = remember { mutableStateMapOf<Int, Node>() }
    val fallbackCubeMap = remember { mutableStateMapOf<Int, CubeNode>() }
    val fallbackLabelMap = remember { mutableStateMapOf<Int, ViewNode2>() }
    val windowManager = remember { ViewNode2.WindowManager(context) }
    val scaffoldState = rememberBottomSheetScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(showDetail, selectedBuilding?.buildingId) {
        val id = selectedBuilding?.buildingId
        if (showDetail && id != null) {
            onLoadBuildingDetail(id)
            onLoadBuildingTrades(id)
        }
    }

    // 탭 처리: SimpleOnGestureListener로 onSingleTapConfirmed만 override
    val gestureListener = remember(uiState.nearbyBuildings) {
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent, node: Node?) {
                val id = node?.findBuildingId() ?: return
                val b = uiState.nearbyBuildings.firstOrNull { it.buildingId == id } ?: return
                onSelect(b)
                coroutineScope.launch {
                    scaffoldState.bottomSheetState.expand()
                }
            }
        }
    }

    // 선택 변경 시 큐브 scale 강조
    LaunchedEffect(selectedBuilding?.buildingId, cubeMap.size, fallbackCubeMap.size) {
        cubeMap.forEach { (id, cube) ->
            cube.setScale(if (id == selectedBuilding?.buildingId) SELECTED_SCALE else DEFAULT_SCALE)
        }
        fallbackCubeMap.forEach { (id, cube) ->
            cube.setScale(if (id == selectedBuilding?.buildingId) SELECTED_SCALE else DEFAULT_SCALE)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            clearFallbackMarkers(
                fallbackNodeMap = fallbackNodeMap,
                fallbackCubeMap = fallbackCubeMap,
                fallbackLabelMap = fallbackLabelMap,
                childNodes = childNodes,
            )
            labelMap.values.forEach { runCatching { it.destroy() } }
            labelMap.clear()
            cubeMap.values.forEach { runCatching { it.destroy() } }
            cubeMap.clear()
            nodeMap.values.forEach {
                runCatching { it.anchor.detach() }
                runCatching { it.destroy() }
            }
            nodeMap.clear()
            runCatching { windowManager.destroy() }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = 80.dp,
            sheetContent = {
                PanelContent(
                    building = selectedBuilding,
                    onDetailClick = { showDetail = true },
                    onMoveToMapPage = onOpenMap,
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                ARScene(
                    modifier = Modifier.fillMaxSize(),
                    engine = engine,
                    materialLoader = materialLoader,
                    childNodes = childNodes,
                    onGestureListener = gestureListener,
                    sessionConfiguration = { session, config ->
                        config.focusMode = Config.FocusMode.AUTO
                        if (BuildConfig.HAS_GEOSPATIAL_API_KEY &&
                            session.isGeospatialModeSupported(Config.GeospatialMode.ENABLED)
                        ) {
                            config.geospatialMode = Config.GeospatialMode.ENABLED
                        }
                    },
                    onTrackingFailureChanged = { reason -> trackingFailure = reason },
                    onSessionUpdated = { session, frame ->
                        trackingState = frame.camera.trackingState.name
                        if (!fallbackLoadRequested) {
                            fallbackLoadRequested = true
                            loadNearbyFromDeviceOrOkgye(
                                context = context,
                                onLoadNearby = onLoadNearby,
                            ) { lat, lon, source ->
                                cameraLat = lat
                                cameraLon = lon
                                if (!BuildConfig.HAS_GEOSPATIAL_API_KEY) {
                                    geospatialStatus = source
                                }
                            }
                        }

                        if (!BuildConfig.HAS_GEOSPATIAL_API_KEY) {
                            geospatialStatus = "Geospatial: 키 없음 (옥계 fallback)"
                            syncFallbackMarkers(
                                buildings = uiState.nearbyBuildings,
                                fallbackNodeMap = fallbackNodeMap,
                                fallbackCubeMap = fallbackCubeMap,
                                fallbackLabelMap = fallbackLabelMap,
                                childNodes = childNodes,
                                engine = engine,
                                materialLoader = materialLoader,
                                windowManager = windowManager,
                                selectedId = selectedBuilding?.buildingId,
                                onStatus = { anchorStatus = it },
                            )
                            return@ARScene
                        }
                        val earth = session.earth
                        if (earth == null) {
                            geospatialStatus = "Geospatial: earth 미초기화"
                            syncFallbackMarkers(
                                buildings = uiState.nearbyBuildings,
                                fallbackNodeMap = fallbackNodeMap,
                                fallbackCubeMap = fallbackCubeMap,
                                fallbackLabelMap = fallbackLabelMap,
                                childNodes = childNodes,
                                engine = engine,
                                materialLoader = materialLoader,
                                windowManager = windowManager,
                                selectedId = selectedBuilding?.buildingId,
                                onStatus = { anchorStatus = it },
                            )
                            return@ARScene
                        }
                        if (earth.trackingState != TrackingState.TRACKING) {
                            geospatialStatus = "Earth: ${earth.trackingState.name}"
                            syncFallbackMarkers(
                                buildings = uiState.nearbyBuildings,
                                fallbackNodeMap = fallbackNodeMap,
                                fallbackCubeMap = fallbackCubeMap,
                                fallbackLabelMap = fallbackLabelMap,
                                childNodes = childNodes,
                                engine = engine,
                                materialLoader = materialLoader,
                                windowManager = windowManager,
                                selectedId = selectedBuilding?.buildingId,
                                onStatus = { anchorStatus = it },
                            )
                            return@ARScene
                        }
                        clearFallbackMarkers(
                            fallbackNodeMap = fallbackNodeMap,
                            fallbackCubeMap = fallbackCubeMap,
                            fallbackLabelMap = fallbackLabelMap,
                            childNodes = childNodes,
                        )
                        val pose = earth.cameraGeospatialPose
                        cameraLat = pose.latitude
                        cameraLon = pose.longitude
                        geospatialStatus = "lat=%.5f lng=%.5f alt=%.1fm".format(
                            pose.latitude, pose.longitude, pose.altitude,
                        )

                        if (!geospatialLoadRequested) {
                            geospatialLoadRequested = true
                            onLoadNearby(pose.latitude, pose.longitude)
                        }

                        syncMarkers(
                            buildings = uiState.nearbyBuildings,
                            nodeMap = nodeMap,
                            cubeMap = cubeMap,
                            labelMap = labelMap,
                            childNodes = childNodes,
                            engine = engine,
                            materialLoader = materialLoader,
                            windowManager = windowManager,
                            earth = earth,
                            altitude = pose.altitude + ANCHOR_ALTITUDE_OFFSET_METERS,
                            selectedId = selectedBuilding?.buildingId,
                            onStatus = { anchorStatus = it },
                            onAnchorError = { lastAnchorError = it },
                        )
                    },
                )

                val nearestMeters: Int? = remember(uiState.nearbyBuildings, cameraLat, cameraLon) {
                    val lat = cameraLat
                    val lon = cameraLon
                    if (lat == null || lon == null) null
                    else uiState.nearbyBuildings
                        .mapNotNull { it.distanceMeters?.toInt() }
                        .minOrNull()
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clickable { showDebug = true },
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text("URL: $currentBaseUrl", style = MaterialTheme.typography.bodySmall)
                        Text("Tracking: $trackingState", style = MaterialTheme.typography.bodySmall)
                        Text(geospatialStatus, style = MaterialTheme.typography.bodySmall)
                        Text(anchorStatus, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = when {
                                uiState.isLoading -> "Buildings: 로드 중…"
                                uiState.error != null -> "Buildings ERR: ${uiState.error}"
                                else -> "Buildings: ${uiState.nearbyBuildings.size}개" +
                                    (nearestMeters?.let { " (nearest=${it}m)" } ?: "")
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                        trackingFailure?.let {
                            Text("Failure: ${it.name}", style = MaterialTheme.typography.bodySmall)
                        }
                        lastAnchorError?.let {
                            Text("AnchorERR: $it", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("⚙ 탭하여 디버그", style = MaterialTheme.typography.labelSmall)
                    }
                }

                BuildingChipRow(
                    buildings = uiState.nearbyBuildings,
                    selectedId = selectedBuilding?.buildingId,
                    onChipClick = { b ->
                        onSelect(b)
                        coroutineScope.launch {
                            scaffoldState.bottomSheetState.expand()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 88.dp),
                )
            }
        }

        if (showDetail) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    color = Color.White,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.padding(16.dp),
                ) {
                    DetailPopupContent(
                        building = selectedBuilding,
                        buildingDetail = uiState.selectedBuildingDetail
                            ?.takeIf { it.buildingId == selectedBuilding?.buildingId },
                        isDetailLoading = uiState.isDetailLoading &&
                            uiState.detailBuildingId == selectedBuilding?.buildingId,
                        detailError = uiState.detailError
                            ?.takeIf { uiState.detailBuildingId == selectedBuilding?.buildingId },
                        trades = if (uiState.tradesBuildingId == selectedBuilding?.buildingId) {
                            uiState.selectedBuildingTrades
                        } else {
                            emptyList()
                        },
                        isTradesLoading = uiState.isTradesLoading &&
                            uiState.tradesBuildingId == selectedBuilding?.buildingId,
                        tradesError = uiState.tradesError
                            ?.takeIf { uiState.tradesBuildingId == selectedBuilding?.buildingId },
                        onClose = { showDetail = false },
                        onMoveToMapPage = {
                            showDetail = false
                            onOpenMap()
                        },
                    )
                }
            }
        }

        if (showDebug) {
            DebugDialog(
                currentBaseUrl = currentBaseUrl,
                defaultBaseUrl = defaultBaseUrl,
                hasOverride = baseUrlStore.get() != null,
                onApplyBaseUrl = { url ->
                    baseUrlStore.set(url)
                },
                onResetBaseUrl = { baseUrlStore.set(null) },
                onForceLoadNearby = { lat, lon ->
                    onLoadNearby(lat, lon)
                },
                onDismiss = { showDebug = false },
            )
        }
    }
}

private data class QuickCoord(val label: String, val lat: Double, val lon: Double)

@SuppressLint("MissingPermission")
private fun loadNearbyFromDeviceOrOkgye(
    context: Context,
    onLoadNearby: (Double, Double) -> Unit,
    onCoordinate: (Double, Double, String) -> Unit,
) {
    onCoordinate(DEFAULT_OKGYE_LAT, DEFAULT_OKGYE_LON, "Fallback: 옥계 기본 좌표")
    onLoadNearby(DEFAULT_OKGYE_LAT, DEFAULT_OKGYE_LON)

    LocationServices.getFusedLocationProviderClient(context)
        .lastLocation
        .addOnSuccessListener { location ->
            if (location == null) {
                return@addOnSuccessListener
            }
            val lat = location.latitude
            val lon = location.longitude
            if (isGumiCoordinate(lat, lon)) {
                onCoordinate(lat, lon, "Fallback: 기기 위치 %.5f, %.5f".format(lat, lon))
                onLoadNearby(lat, lon)
            }
        }
}

private fun isGumiCoordinate(lat: Double, lon: Double): Boolean =
    lat in 36.05..36.25 && lon in 128.20..128.50

@Composable
private fun DebugDialog(
    currentBaseUrl: String,
    defaultBaseUrl: String,
    hasOverride: Boolean,
    onApplyBaseUrl: (String) -> Unit,
    onResetBaseUrl: () -> Unit,
    onForceLoadNearby: (Double, Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var urlInput by remember { mutableStateOf(currentBaseUrl) }
    var latInput by remember { mutableStateOf("36.139") }
    var lonInput by remember { mutableStateOf("128.421") }
    val scrollState = rememberScrollState()

    val quickCoords = listOf(
        QuickCoord("옥계삼구트리니엔", 36.13918, 128.42137),
        QuickCoord("옥계세영리첼", 36.13910, 128.43546),
        QuickCoord("옥계더힐", 36.14141, 128.42513),
        QuickCoord("옥계동 중심", 36.141, 128.422),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        },
        title = { Text("디버그 도구") },
        text = {
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                Text("BASE URL", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text("현재: $currentBaseUrl", style = MaterialTheme.typography.bodySmall)
                Text("기본(빌드값): $defaultBaseUrl", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("override URL (예: https://xxx.trycloudflare.com/)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onApplyBaseUrl(urlInput) }) { Text("적용") }
                    OutlinedButton(
                        enabled = hasOverride,
                        onClick = {
                            onResetBaseUrl()
                            urlInput = defaultBaseUrl
                        },
                    ) { Text("초기화") }
                }

                Spacer(Modifier.height(16.dp))
                Text("좌표 강제 주입 (nearby 호출)", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latInput,
                        onValueChange = { latInput = it },
                        label = { Text("lat") },
                        singleLine = true,
                        modifier = Modifier.width(120.dp),
                    )
                    OutlinedTextField(
                        value = lonInput,
                        onValueChange = { lonInput = it },
                        label = { Text("lon") },
                        singleLine = true,
                        modifier = Modifier.width(140.dp),
                    )
                }
                Button(onClick = {
                    val lat = latInput.toDoubleOrNull()
                    val lon = lonInput.toDoubleOrNull()
                    if (lat != null && lon != null) onForceLoadNearby(lat, lon)
                }) { Text("이 좌표로 nearby 호출") }

                Spacer(Modifier.height(12.dp))
                Text("빠른 선택", style = MaterialTheme.typography.labelMedium)
                quickCoords.forEach { qc ->
                    TextButton(onClick = {
                        latInput = qc.lat.toString()
                        lonInput = qc.lon.toString()
                        onForceLoadNearby(qc.lat, qc.lon)
                    }) { Text("${qc.label}  (${qc.lat}, ${qc.lon})") }
                }
            }
        },
    )
}

/**
 * 매 프레임 호출되어 nodeMap/cubeMap/labelMap을 nearbyBuildings와 diff하여 노드를 추가/제거한다.
 * earth 객체는 onSessionUpdated 안에서만 안정적으로 접근 가능하므로 여기서 처리.
 */
private fun syncFallbackMarkers(
    buildings: List<BuildingSummary>,
    fallbackNodeMap: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, Node>,
    fallbackCubeMap: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, CubeNode>,
    fallbackLabelMap: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, ViewNode2>,
    childNodes: androidx.compose.runtime.snapshots.SnapshotStateList<Node>,
    engine: com.google.android.filament.Engine,
    materialLoader: io.github.sceneview.loaders.MaterialLoader,
    windowManager: ViewNode2.WindowManager,
    selectedId: Int?,
    onStatus: (String) -> Unit,
) {
    val incoming = buildings.take(FALLBACK_MARKER_LIMIT)
    val incomingIds = incoming.map { it.buildingId }.toSet()

    fallbackNodeMap.keys.filter { it !in incomingIds }.forEach { id ->
        fallbackLabelMap[id]?.let { runCatching { it.destroy() } }
        fallbackLabelMap.remove(id)
        fallbackCubeMap[id]?.let { runCatching { it.destroy() } }
        fallbackCubeMap.remove(id)
        val node = fallbackNodeMap[id] ?: return@forEach
        childNodes.remove(node)
        runCatching { node.destroy() }
        fallbackNodeMap.remove(id)
    }

    incoming.forEachIndexed { index, b ->
        val node = fallbackNodeMap[b.buildingId]
        if (node != null) {
            node.position = fallbackMarkerPosition(index)
            return@forEachIndexed
        }

        val fallbackNode = Node(engine = engine)
        fallbackNode.name = b.buildingId.toString()
        fallbackNode.position = fallbackMarkerPosition(index)

        val cube = CubeNode(
            engine = engine,
            size = Size(0.7f, 1.4f, 0.7f),
            center = Position(0f, -0.65f, 0f),
            materialInstance = materialLoader.createColorInstance(
                color = gradeColor(b.livabilityGrade),
            ),
        )
        cube.name = b.buildingId.toString()
        cube.setScale(if (b.buildingId == selectedId) SELECTED_SCALE else DEFAULT_SCALE)
        fallbackNode.addChildNode(cube)

        val label = ViewNode2(
            engine = engine,
            windowManager = windowManager,
            materialLoader = materialLoader,
            /* unlit */ true,
            /* invertFrontFaceWinding */ false,
        ) {
            BuildingLabel(
                dongName = b.dongName,
                grade = b.livabilityGrade ?: "-",
            )
        }
        label.name = b.buildingId.toString()
        label.position = Position(0f, 0.4f, 0f)
        label.pxPerUnits = 500f
        label.viewSize = Float3(0.8f, 0.4f, 1f)
        fallbackNode.addChildNode(label)

        childNodes += fallbackNode
        fallbackNodeMap[b.buildingId] = fallbackNode
        fallbackCubeMap[b.buildingId] = cube
        fallbackLabelMap[b.buildingId] = label
    }

    onStatus(
        if (incoming.isEmpty()) "Fallback tags: nearby 데이터 대기"
        else "Fallback tags: ${fallbackNodeMap.size}/${incoming.size} (Earth 대기)"
    )
}

private fun fallbackMarkerPosition(index: Int): Float3 {
    val column = index % 3
    val row = index / 3
    val x = (column - 1) * 1.0f
    val y = -0.15f - row * 0.85f
    val z = -3.0f - row * 0.35f
    return Position(x, y, z)
}

private fun clearFallbackMarkers(
    fallbackNodeMap: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, Node>,
    fallbackCubeMap: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, CubeNode>,
    fallbackLabelMap: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, ViewNode2>,
    childNodes: androidx.compose.runtime.snapshots.SnapshotStateList<Node>,
) {
    fallbackLabelMap.values.forEach { runCatching { it.destroy() } }
    fallbackLabelMap.clear()
    fallbackCubeMap.values.forEach { runCatching { it.destroy() } }
    fallbackCubeMap.clear()
    fallbackNodeMap.values.forEach { node ->
        childNodes.remove(node)
        runCatching { node.destroy() }
    }
    fallbackNodeMap.clear()
}

private fun syncMarkers(
    buildings: List<BuildingSummary>,
    nodeMap: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, AnchorNode>,
    cubeMap: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, CubeNode>,
    labelMap: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, ViewNode2>,
    childNodes: androidx.compose.runtime.snapshots.SnapshotStateList<Node>,
    engine: com.google.android.filament.Engine,
    materialLoader: io.github.sceneview.loaders.MaterialLoader,
    windowManager: ViewNode2.WindowManager,
    earth: com.google.ar.core.Earth,
    altitude: Double,
    selectedId: Int?,
    onStatus: (String) -> Unit,
    onAnchorError: (String) -> Unit = {},
) {
    val incoming = buildings.associateBy { it.buildingId }

    // 제거: nodeMap에는 있지만 incoming에 없는 id
    val toRemove = nodeMap.keys.filter { it !in incoming.keys }
    toRemove.forEach { id ->
        labelMap[id]?.let { runCatching { it.destroy() } }
        labelMap.remove(id)
        cubeMap[id]?.let { runCatching { it.destroy() } }
        cubeMap.remove(id)
        val node = nodeMap[id] ?: return@forEach
        runCatching { node.anchor.detach() }
        childNodes.remove(node)
        runCatching { node.destroy() }
        nodeMap.remove(id)
    }

    // 추가: incoming에 있지만 nodeMap에 없는 id
    var added = 0
    var failed = 0
    incoming.forEach { (id, b) ->
        if (id in nodeMap) return@forEach
        runCatching {
            val anchor = earth.createAnchor(b.lat, b.lon, altitude, 0f, 0f, 0f, 1f)
            val anchorNode = AnchorNode(engine = engine, anchor = anchor)
            anchorNode.name = id.toString()

            val cube = CubeNode(
                engine = engine,
                size = Size(0.8f, 2.5f, 0.8f),
                center = Position(0f, 0f, 0f),
                materialInstance = materialLoader.createColorInstance(
                    color = gradeColor(b.livabilityGrade),
                ),
            )
            cube.name = id.toString()
            cube.setScale(if (id == selectedId) SELECTED_SCALE else DEFAULT_SCALE)
            anchorNode.addChildNode(cube)

            val label = ViewNode2(
                engine = engine,
                windowManager = windowManager,
                materialLoader = materialLoader,
                /* unlit */ true,
                /* invertFrontFaceWinding */ false,
            ) {
                BuildingLabel(
                    dongName = b.dongName,
                    grade = b.livabilityGrade ?: "-",
                )
            }
            label.name = id.toString()
            label.position = Position(0f, 1.7f, 0f)
            label.pxPerUnits = 500f
            label.viewSize = Float3(0.8f, 0.4f, 1f)
            anchorNode.addChildNode(label)

            childNodes += anchorNode
            nodeMap[id] = anchorNode
            cubeMap[id] = cube
            labelMap[id] = label
            added++
        }.onFailure {
            failed++
            onAnchorError("${it::class.simpleName}: ${it.message ?: "unknown"}")
        }
    }
    onStatus("Anchors: ${nodeMap.size}/${incoming.size} (added=$added failed=$failed)")
}

private fun gradeColor(grade: String?): Color = when (grade?.uppercase()) {
    "S", "A" -> Color(0xFF2ECC71)
    "B", "C" -> Color(0xFFF1C40F)
    "D", "F" -> Color(0xFFE74C3C)
    else -> Color(0xFF95A5A6)
}

/** 자식 노드(CubeNode/ViewNode2)가 hit될 수 있으므로 부모 체인을 거슬러 buildingId 추출 */
private fun Node.findBuildingId(): Int? {
    var n: Node? = this
    while (n != null) {
        n.name?.toIntOrNull()?.let { return it }
        n = n.parent
    }
    return null
}

@Composable
private fun BuildingLabel(dongName: String, grade: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xCC000000))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Column {
            Text(
                text = dongName,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "등급 $grade",
                color = Color.White,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun BuildingChipRow(
    buildings: List<BuildingSummary>,
    selectedId: Int?,
    onChipClick: (BuildingSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (buildings.isEmpty()) return
    val scrollState = rememberScrollState()
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            buildings.forEach { b ->
                val grade = b.livabilityGrade ?: "-"
                val label = "${b.complexName} ${b.dongName} · $grade"
                AssistChip(
                    onClick = { onChipClick(b) },
                    label = { Text(label) },
                    colors = if (b.buildingId == selectedId) {
                        AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    } else AssistChipDefaults.assistChipColors(),
                )
            }
            Spacer(Modifier.height(0.dp))
        }
    }
}
