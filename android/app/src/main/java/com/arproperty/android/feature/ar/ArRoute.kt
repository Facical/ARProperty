package com.arproperty.android.feature.ar

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arproperty.android.core.common.arRequiredPermissions
import com.arproperty.android.core.common.hasAllPermissions
import com.arproperty.android.core.designsystem.NotSupportedState
import com.arproperty.android.core.designsystem.PermissionRequiredState
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.ar.ARScene

data class ArUiState(
    val sampleBuildingId: Int = 42,
)

class ArViewModel : ViewModel() {
    val uiState = ArUiState()
}

@Composable
fun ArRoute(
    onOpenBuilding: (Int) -> Unit,
    onOpenLivability: (Int) -> Unit,
    onOpenMap: () -> Unit,
    viewModel: ArViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "AR",
            style = MaterialTheme.typography.headlineMedium,
        )

        if (!hasPermissions) {
            PermissionRequiredState(
                title = "권한이 필요합니다",
                body = "AR 화면은 카메라와 위치 권한이 있어야 초기 점검과 위치 기반 안내를 진행할 수 있습니다.",
                actionLabel = "권한 요청",
                onActionClick = {
                    permissionLauncher.launch(arRequiredPermissions.toTypedArray())
                },
            )
        } else if (!availability.isSupported) {
            NotSupportedState(
                title = "ARCore 미지원 환경",
                body = "현재 기기 또는 환경에서는 ARCore를 바로 사용할 수 없습니다. 그래도 지도와 상세 흐름은 계속 확인할 수 있습니다.",
            )
        } else {
            ArCameraScene()
        }

        Button(onClick = { onOpenBuilding(uiState.sampleBuildingId) }) {
            Text(text = "샘플 건물 상세 열기")
        }

        Button(onClick = { onOpenLivability(uiState.sampleBuildingId) }) {
            Text(text = "샘플 생활 점수 열기")
        }

        Button(onClick = onOpenMap) {
            Text(text = "지도 탭으로 이동")
        }
    }
}

@Composable
private fun ArCameraScene() {
    var trackingState by remember { mutableStateOf("초기화 대기") }
    var trackingFailure by remember { mutableStateOf<TrackingFailureReason?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp),
    ) {
        ARScene(
            modifier = Modifier.fillMaxSize(),
            sessionConfiguration = { _, config ->
                config.focusMode = Config.FocusMode.AUTO
            },
            onTrackingFailureChanged = { reason ->
                trackingFailure = reason
            },
            onSessionUpdated = { _, frame ->
                trackingState = frame.camera.trackingState.name
            },
        )

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            shape = MaterialTheme.shapes.small,
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = "Tracking: $trackingState",
                    style = MaterialTheme.typography.bodySmall,
                )
                trackingFailure?.let {
                    Text(
                        text = "Failure: ${it.name}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
