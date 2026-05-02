package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DetailPopupContent(
    onClose: () -> Unit,
    onMoveToMapPage: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxHeight(0.8f)
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("상세 정보", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))

        // 1. 건축물대장 정보 섹션
        InfoSectionTitle("🏢 건축물대장 정보")
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            InfoRow("건축년도", "2024년")
            InfoRow("층수", "지상 20층 / 지하 3층")
            InfoRow("구조", "철근콘크리트 구조")
            InfoRow("승강기 수", "상용 4대 / 비상 1대")
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        // 2. 가격 변동 추이 (꺾은선 그래프 와이어프레임)
        InfoSectionTitle("📈 가격 변동 추이")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(vertical = 16.dp)
                .drawBehind {
                    // 간단한 꺾은선 그래프 와이어프레임 그리기
                    val points = listOf(0.2f, 0.5f, 0.4f, 0.8f, 0.7f, 1.0f)
                    val widthStep = size.width / (points.size - 1)
                    for (i in 0 until points.size - 1) {
                        drawLine(
                            color = Color(0xFF005CAF),
                            start = Offset(i * widthStep, size.height * (1 - points[i])),
                            end = Offset((i + 1) * widthStep, size.height * (1 - points[i + 1])),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                }
        )
        Text("최근 1년간 지속적 상승세", fontSize = 12.sp, color = Color.Gray)

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        // 3. 상세 거래 이력 리스트
        InfoSectionTitle("📋 상세 거래 이력")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TradeHistoryRow("2024.03.15", "1억 2,000", "15층")
            TradeHistoryRow("2023.11.02", "1억 1,500", "8층")
            TradeHistoryRow("2023.08.20", "1억 1,000", "12층")
        }

        Spacer(modifier = Modifier.height(30.dp))



        // 닫기 버튼
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD2D2D2)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("닫기", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}