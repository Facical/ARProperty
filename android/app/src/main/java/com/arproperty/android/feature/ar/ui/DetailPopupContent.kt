package com.arproperty.android.feature.ar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arproperty.android.core.model.BuildingSummary

@Composable
fun DetailPopupContent(
    building: BuildingSummary?,
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
                InfoRow("지상 층수", building.groundFloors?.let { "${it}층" } ?: "—")
                InfoRow(
                    "생활 등급",
                    "${building.livabilityGrade ?: "-"} (${building.livabilityScore?.let { "%.1f".format(it) } ?: "-"})",
                )
                InfoRow(
                    "거리",
                    building.distanceMeters?.let { "%.0fm".format(it) } ?: "—",
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            InfoSectionTitle("📋 최근 거래")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val trade = building.latestTrade
                if (trade == null) {
                    Text("거래 기록 없음", fontSize = 14.sp, color = Color.Gray)
                } else {
                    TradeHistoryRow(
                        date = trade.dealDate ?: "-",
                        price = formatKoreanPrice(trade.dealAmount),
                        floor = trade.floor?.let { "${it}층" } ?: "-",
                    )
                    Text(
                        text = "* 추가 거래 이력은 후속 PR에서 /api/v1/buildings/{id}/trades 연결 후 표시됩니다.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                    )
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
