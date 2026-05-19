package com.arproperty.android.feature.ar.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arproperty.android.core.model.BuildingDetail
import com.arproperty.android.core.model.BuildingSummary
import com.arproperty.android.core.model.LatestTrade
import com.arproperty.android.core.model.TradeItem

@Composable
fun DetailPopupContent(
    building: BuildingSummary?,
    buildingDetail: BuildingDetail? = null,
    isDetailLoading: Boolean = false,
    detailError: String? = null,
    trades: List<TradeItem> = emptyList(),
    isTradesLoading: Boolean = false,
    tradesError: String? = null,
    onClose: () -> Unit,
    onMoveToMapPage: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxHeight(0.8f)
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = building?.let { "${it.complexName} ${it.dongName}" } ?: "건물 미선택",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(20.dp))

        if (building == null) {
            Text(
                text = "AR 화면에서 단지를 먼저 선택해주세요.",
                fontSize = 14.sp,
                color = Color.Gray,
            )
        } else {
            InfoSectionTitle("🏢 건물 요약")
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                when {
                    isDetailLoading -> Text("상세 정보 불러오는 중...", fontSize = 13.sp, color = Color.Gray)
                    detailError != null -> Text(
                        "상세 정보는 잠시 불러오지 못했습니다. 요약 정보로 표시합니다.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                    )
                }
                InfoRow(
                    "지상 층수",
                    formatFloor(buildingDetail?.groundFloors ?: building.groundFloors),
                )
                InfoRow("최고 층수", formatFloor(buildingDetail?.highestFloor))
                InfoRow("건물 높이", formatMeters(buildingDetail?.buildingHeight))
                InfoRow("구조", buildingDetail?.structureType ?: "—")
                InfoRow("세대수", formatCount(buildingDetail?.complexInfo?.households))
                InfoRow(
                    "생활 등급",
                    formatLivability(
                        grade = buildingDetail?.livability?.grade ?: building.livabilityGrade,
                        score = buildingDetail?.livability?.totalScore ?: building.livabilityScore,
                    ),
                )
                InfoRow(
                    "거리",
                    building.distanceMeters?.let { "%.0fm".format(it) } ?: "—",
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            InfoSectionTitle("🏗 단지/건축 정보")
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                InfoRow("사용승인일", buildingDetail?.useApprovalDate ?: "—")
                InfoRow("연면적", formatArea(buildingDetail?.totalArea))
                InfoRow("단지 동수", formatCount(buildingDetail?.complexInfo?.buildingCount))
                InfoRow("주차대수", formatCount(buildingDetail?.complexInfo?.parkingCount))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            InfoSectionTitle("📋 최근 거래")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PriceTrendChart(trades = trades)

                when {
                    isTradesLoading -> {
                        Text("최근 거래 불러오는 중...", fontSize = 14.sp, color = Color.Gray)
                        building.latestTrade?.let { LatestTradeFallback(it) }
                    }

                    trades.isNotEmpty() -> {
                        trades.take(3).forEach { trade ->
                            TradeHistoryRow(
                                date = trade.dealDate,
                                price = formatTradePrice(trade),
                                floor = trade.floor?.let { "${it}층" } ?: "-",
                            )
                            Text(
                                text = listOfNotNull(
                                    trade.tradeType,
                                    trade.exclusiveArea?.let { "%.1f㎡".format(it) },
                                    trade.dealingType,
                                ).joinToString(" · "),
                                fontSize = 12.sp,
                                color = Color.Gray,
                            )
                        }
                    }

                    else -> {
                        tradesError?.let {
                            Text(
                                "다건 거래 이력은 잠시 불러오지 못했습니다.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                            )
                        }
                        val fallbackTrade = building.latestTrade
                        if (fallbackTrade == null) {
                            Text("거래 기록 없음", fontSize = 14.sp, color = Color.Gray)
                        } else {
                            LatestTradeFallback(fallbackTrade)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD2D2D2)),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("닫기", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatFloor(value: Int?): String =
    value?.takeIf { it > 0 }?.let { "${it}층" } ?: "—"

private fun formatCount(value: Int?): String =
    value?.takeIf { it > 0 }?.let { "%,d".format(it) } ?: "—"

private fun formatMeters(value: Double?): String =
    value?.takeIf { it > 0.0 }?.let { "%.1fm".format(it) } ?: "—"

private fun formatArea(value: Double?): String =
    value?.takeIf { it > 0.0 }?.let { "%,.1f㎡".format(it) } ?: "—"

private fun formatLivability(grade: String?, score: Double?): String =
    "${grade ?: "-"} (${score?.let { "%.1f".format(it) } ?: "-"})"

@Composable
private fun PriceTrendChart(trades: List<TradeItem>) {
    val chartType = trades.firstOrNull { it.chartAmount() != null }?.tradeType ?: return
    val points = trades
        .filter { it.tradeType == chartType }
        .take(6)
        .mapNotNull { trade ->
            trade.chartAmount()?.let { amount ->
                PricePoint(
                    label = trade.dealDate.takeLast(5),
                    amount = amount,
                )
            }
        }
        .asReversed()

    if (points.isEmpty()) return

    val min = points.minOf { it.amount }
    val max = points.maxOf { it.amount }
    val range = (max - min).takeIf { it > 0 } ?: 1
    val lineColor = Color(0xFF005CAF)
    val gridColor = Color(0xFFE2E6EA)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("가격 추세", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                "${formatKoreanPrice(min)} ~ ${formatKoreanPrice(max)}",
                fontSize = 12.sp,
                color = Color.Gray,
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp),
        ) {
            val left = 8.dp.toPx()
            val right = size.width - 8.dp.toPx()
            val top = 12.dp.toPx()
            val bottom = size.height - 18.dp.toPx()
            val chartHeight = bottom - top
            val chartWidth = right - left

            repeat(3) { index ->
                val y = top + chartHeight * index / 2f
                drawLine(
                    color = gridColor,
                    start = Offset(left, y),
                    end = Offset(right, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            val offsets = points.mapIndexed { index, point ->
                val x = if (points.size == 1) {
                    left + chartWidth / 2f
                } else {
                    left + chartWidth * index / (points.lastIndex.toFloat())
                }
                val normalized = (point.amount - min).toFloat() / range
                val y = bottom - chartHeight * normalized
                Offset(x, y)
            }

            if (offsets.size >= 2) {
                val path = Path().apply {
                    moveTo(offsets.first().x, offsets.first().y)
                    offsets.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()),
                )
            }

            offsets.forEach { offset ->
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = offset)
                drawCircle(color = lineColor, radius = 3.dp.toPx(), center = offset)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(points.first().label, fontSize = 11.sp, color = Color.Gray)
            Text(points.last().label, fontSize = 11.sp, color = Color.Gray)
        }
        Text(
            text = "$chartType 기준 최근 가격 흐름",
            fontSize = 11.sp,
            color = Color.Gray,
        )
    }
}

@Composable
private fun LatestTradeFallback(trade: LatestTrade) {
    TradeHistoryRow(
        date = trade.dealDate ?: "-",
        price = formatKoreanPrice(trade.dealAmount),
        floor = trade.floor?.let { "${it}층" } ?: "-",
    )
    Text(
        text = listOfNotNull(
            trade.tradeType,
            trade.exclusiveArea?.let { "%.1f㎡".format(it) },
        ).joinToString(" · ").ifBlank { "nearby 최신 거래 요약" },
        fontSize = 12.sp,
        color = Color.Gray,
    )
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

private fun TradeItem.chartAmount(): Int? =
    when (tradeType) {
        "월세" -> deposit
        "전세" -> deposit
        else -> dealAmount
    }

private data class PricePoint(
    val label: String,
    val amount: Int,
)
