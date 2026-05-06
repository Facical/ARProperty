package com.arproperty.android.feature.ar

import android.app.Activity
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arproperty.android.core.common.arRequiredPermissions
import com.arproperty.android.core.common.hasAllPermissions
import com.arproperty.android.core.designsystem.NotSupportedState
import com.arproperty.android.core.designsystem.PermissionRequiredState
import com.arproperty.android.core.designsystem.PlaceholderCard
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableException

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
            ArSessionLifecycleCard()
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
private fun ArSessionLifecycleCard() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var status by remember { mutableStateOf("ARCore 세션 초기화 대기") }
    var session by remember { mutableStateOf<Session?>(null) }
    var userRequestedInstall by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val activity = context as? Activity
        if (activity == null) {
            status = "Activity 컨텍스트를 찾을 수 없음"
            return@DisposableEffect onDispose { }
        }
        try {
            when (ArCoreApk.getInstance().requestInstall(activity, !userRequestedInstall)) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    userRequestedInstall = true
                    status = "ARCore 설치/업데이트 요청됨 — 완료 후 화면을 다시 진입해 주세요"
                }
                ArCoreApk.InstallStatus.INSTALLED -> {
                    val newSession = Session(context).apply {
                        configure(
                            Config(this).apply {
                                focusMode = Config.FocusMode.AUTO
                            },
                        )
                    }
                    session = newSession
                    status = "ARCore 세션 생성 완료 — 라이프사이클 대기 중"
                }
            }
        } catch (e: UnavailableException) {
            status = "ARCore 사용 불가: ${e::class.simpleName}"
        } catch (e: Exception) {
            status = "세션 초기화 실패: ${e::class.simpleName}"
        }

        onDispose {
            session?.close()
            session = null
        }
    }

    DisposableEffect(session, lifecycleOwner) {
        val current = session
        if (current == null) {
            return@DisposableEffect onDispose { }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> runCatching { current.resume() }
                    .onSuccess { status = "ARCore 세션 활성" }
                    .onFailure { status = "세션 resume 실패: ${it::class.simpleName}" }
                Lifecycle.Event.ON_PAUSE -> runCatching { current.pause() }
                    .onSuccess { status = "ARCore 세션 일시정지" }
                    .onFailure { status = "세션 pause 실패: ${it::class.simpleName}" }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    PlaceholderCard(
        title = "ARCore 세션 상태",
        body = status,
    )
}
