package com.arproperty.android.feature.building

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arproperty.android.app.ArPropertyApplication
import com.arproperty.android.core.designsystem.LoadingState
import com.arproperty.android.core.designsystem.PlaceholderCard
import com.arproperty.android.core.model.BuildingDetail
import com.arproperty.android.core.model.TradeItem
import com.arproperty.android.core.network.BuildingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BuildingDetailUiState(
    val buildingId: Int? = null,
    val isLoading: Boolean = false,
    val detail: BuildingDetail? = null,
    val error: String? = null,
    val isTradesLoading: Boolean = false,
    val trades: List<TradeItem> = emptyList(),
    val tradesError: String? = null,
)

class BuildingDetailViewModel(
    private val buildingRepository: BuildingRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BuildingDetailUiState())
    val uiState: StateFlow<BuildingDetailUiState> = _uiState.asStateFlow()

    fun load(buildingId: Int) {
        if (buildingId <= 0) {
            _uiState.update {
                it.copy(
                    buildingId = buildingId,
                    isLoading = false,
                    detail = null,
                    error = "유효하지 않은 건물 ID입니다.",
                    isTradesLoading = false,
                    trades = emptyList(),
                    tradesError = null,
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                buildingId = buildingId,
                isLoading = true,
                error = null,
                isTradesLoading = true,
                trades = emptyList(),
                tradesError = null,
            )
        }
        viewModelScope.launch {
            val result = buildingRepository.getBuildingDetail(buildingId)
            _uiState.update {
                if (it.buildingId != buildingId) return@update it
                result.fold(
                    onSuccess = { detail ->
                        it.copy(isLoading = false, detail = detail, error = null)
                    },
                    onFailure = { error ->
                        it.copy(
                            isLoading = false,
                            detail = null,
                            error = error.message ?: "건물 상세 정보를 불러오지 못했습니다.",
                        )
                    },
                )
            }
        }
        viewModelScope.launch {
            val result = buildingRepository.getBuildingTrades(buildingId)
            _uiState.update {
                if (it.buildingId != buildingId) return@update it
                result.fold(
                    onSuccess = { trades ->
                        it.copy(isTradesLoading = false, trades = trades, tradesError = null)
                    },
                    onFailure = { error ->
                        it.copy(
                            isTradesLoading = false,
                            trades = emptyList(),
                            tradesError = error.message ?: "거래 이력을 불러오지 못했습니다.",
                        )
                    },
                )
            }
        }
    }
}

class BuildingDetailViewModelFactory(
    private val buildingRepository: BuildingRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BuildingDetailViewModel(buildingRepository) as T
    }
}

@Composable
fun BuildingDetailRoute(
    buildingId: Int,
    onOpenComplex: (Int) -> Unit,
    onOpenLivability: (Int) -> Unit,
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as ArPropertyApplication).appContainer
    val viewModel: BuildingDetailViewModel = viewModel(
        factory = BuildingDetailViewModelFactory(appContainer.buildingRepository),
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(buildingId) {
        viewModel.load(buildingId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "건물 상세",
            style = MaterialTheme.typography.headlineMedium,
        )

        when {
            uiState.isLoading -> {
                LoadingState(message = "건물 상세 정보를 불러오는 중입니다.")
            }

            uiState.error != null -> {
                PlaceholderCard(
                    title = "상세 정보를 불러오지 못했습니다",
                    body = uiState.error.orEmpty(),
                    actionLabel = "다시 시도",
                    onActionClick = { viewModel.load(buildingId) },
                )
            }

            uiState.detail != null -> {
                BuildingDetailContent(
                    detail = requireNotNull(uiState.detail),
                    trades = uiState.trades,
                    isTradesLoading = uiState.isTradesLoading,
                    tradesError = uiState.tradesError,
                    onOpenComplex = onOpenComplex,
                    onOpenLivability = onOpenLivability,
                )
            }
        }
    }
}

@Composable
private fun BuildingDetailContent(
    detail: BuildingDetail,
    trades: List<TradeItem>,
    isTradesLoading: Boolean,
    tradesError: String?,
    onOpenComplex: (Int) -> Unit,
    onOpenLivability: (Int) -> Unit,
) {
    Text(
        text = "${detail.complexName} ${detail.dongName}",
        style = MaterialTheme.typography.titleLarge,
    )

    PlaceholderCard(
        title = "건물 기본 정보",
        body = listOf(
            "건물 ID: ${detail.buildingId}",
            "좌표: %.5f, %.5f".format(detail.lat, detail.lon),
            "층수: 지상 ${detail.groundFloors}층 / 지하 ${detail.undergroundFloors}층 / 최고 ${detail.highestFloor}층",
            "높이: ${formatMeters(detail.buildingHeight)}",
            "구조: ${detail.structureType ?: "정보 없음"}",
            "연면적: ${formatArea(detail.totalArea)}",
            "사용승인일: ${detail.useApprovalDate ?: "정보 없음"}",
        ).joinToString("\n"),
    )

    val complexInfo = detail.complexInfo
    PlaceholderCard(
        title = "단지 정보",
        body = listOf(
            "단지 ID: ${detail.complexId}",
            "K-apt 코드: ${complexInfo?.kaptCode ?: "정보 없음"}",
            "세대수: ${formatCount(complexInfo?.households, "세대")}",
            "동 수: ${formatCount(complexInfo?.buildingCount, "동")}",
            "주차대수: ${formatCount(complexInfo?.parkingCount, "대")}",
            "승강기: ${formatCount(complexInfo?.elevatorCount, "대")}",
            "난방: ${complexInfo?.heatingType ?: "정보 없음"}",
            "시공사: ${complexInfo?.constructor ?: "정보 없음"}",
        ).joinToString("\n"),
    )

    val livability = detail.livability
    PlaceholderCard(
        title = "생활 편의 요약",
        body = if (livability == null) {
            "아직 계산된 생활 편의 점수가 없습니다."
        } else {
            "등급: ${livability.grade ?: "-"}\n점수: ${livability.totalScore?.let { "%.1f".format(it) } ?: "-"}"
        },
    )

    PlaceholderCard(
        title = "최근 거래 이력",
        body = formatTradeHistory(
            trades = trades,
            isLoading = isTradesLoading,
            error = tradesError,
        ),
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = { onOpenComplex(detail.complexId) },
            modifier = Modifier.weight(1f),
        ) {
            Text(text = "단지 상세")
        }

        Button(
            onClick = { onOpenLivability(detail.buildingId) },
            modifier = Modifier.weight(1f),
        ) {
            Text(text = "생활 점수")
        }
    }
}

private fun formatCount(value: Int?, suffix: String): String =
    value?.let { "%,d$suffix".format(it) } ?: "정보 없음"

private fun formatMeters(value: Double?): String =
    value?.let { "%.1fm".format(it) } ?: "정보 없음"

private fun formatArea(value: Double?): String =
    value?.let { "%,.1f㎡".format(it) } ?: "정보 없음"

private fun formatTradeHistory(
    trades: List<TradeItem>,
    isLoading: Boolean,
    error: String?,
): String {
    if (isLoading) return "최근 거래 이력을 불러오는 중입니다."
    if (error != null) return "거래 이력을 불러오지 못했습니다.\n$error"
    if (trades.isEmpty()) return "표시할 거래 이력이 없습니다."

    return trades.take(10).joinToString("\n") { trade ->
        listOfNotNull(
            trade.dealDate,
            trade.tradeType,
            formatTradePrice(trade),
            trade.exclusiveArea?.let { "%.1f㎡".format(it) },
            trade.floor?.let { "${it}층" },
            trade.dealingType,
        ).joinToString(" · ")
    }
}

private fun formatTradePrice(trade: TradeItem): String =
    when (trade.tradeType) {
        "월세" -> {
            val deposit = trade.deposit?.let { "%,d".format(it) } ?: "-"
            val rent = trade.monthlyRent?.let { "%,d".format(it) } ?: "-"
            "보증금 ${deposit}만 / 월세 ${rent}만"
        }
        "전세" -> "보증금 ${formatKoreanPrice(trade.deposit)}"
        else -> formatKoreanPrice(trade.dealAmount)
    }

private fun formatKoreanPrice(amountManwon: Int?): String {
    if (amountManwon == null) return "가격 정보 없음"
    val eok = amountManwon / 10000
    val man = amountManwon % 10000
    return when {
        eok > 0 && man > 0 -> "%d억 %,d만".format(eok, man)
        eok > 0 -> "%d억".format(eok)
        else -> "%,d만".format(man)
    }
}
