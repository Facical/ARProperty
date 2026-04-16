package com.arproperty.android.feature.ar

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arproperty.android.core.common.arRequiredPermissions
import com.arproperty.android.core.common.hasAllPermissions
import com.arproperty.android.core.designsystem.NotSupportedState
import com.arproperty.android.core.designsystem.PermissionRequiredState
import com.arproperty.android.core.designsystem.PlaceholderCard
import com.google.ar.core.ArCoreApk

data class ArUiState(
    val sampleBuildingId: Int = 42,
    val introTitle: String = "AR 탐색 화면",
    val introBody: String = "이 화면은 카메라, 위치, ARCore 지원 여부를 확인한 뒤 향후 건물 오버레이가 올라갈 자리를 제공합니다.",
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
            PlaceholderCard(
                title = uiState.introTitle,
                body = uiState.introBody,
            )

            PlaceholderCard(
                title = "오버레이 영역 Placeholder",
                body = "여기에 이후 ARCore 세션, 건물 후보 오버레이, 동 선택 UI가 추가됩니다.",
            )
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
