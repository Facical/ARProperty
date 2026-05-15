package com.arproperty.android.feature.ar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
fun PanelContent(
    building: BuildingSummary?,
    onDetailClick: () -> Unit,
    onMoveToMapPage: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
            .padding(start = 24.dp, end = 24.dp, top = 30.dp, bottom = 10.dp),
    ) {
        if (building == null) {
            Text(
                text = "주변 건물을 탭하세요",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "AR 카메라 화면 하단의 칩 목록에서 단지를 선택하면 정보가 표시됩니다.",
                fontSize = 14.sp,
                color = Color.Gray,
            )
        } else {
            Text(
                text = "${building.complexName} ${building.dongName}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
            )
            building.distanceMeters?.let {
                Text(
                    text = "거리 약 ${it.toInt()}m · 등급 ${building.livabilityGrade ?: "-"}",
                    fontSize = 14.sp,
                    color = Color.Gray,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("최근 거래가", fontSize = 16.sp, color = Color.Gray)
                    Text(
                        text = formatKoreanPrice(building.latestTrade?.dealAmount),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("전용면적", fontSize = 16.sp, color = Color.Gray)
                    Text(
                        text = building.latestTrade?.exclusiveArea
                            ?.let { "%.1f㎡".format(it) } ?: "—",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("지상 층수", fontSize = 16.sp, color = Color.Gray)
                    Text(
                        text = building.groundFloors?.let { "${it}층" } ?: "—",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                Button(
                    onClick = onDetailClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD2D2D2),
                        contentColor = Color(0xFF005CAF),
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                ) {
                    Text(text = "상세정보", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = onMoveToMapPage,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF005CAF),
                contentColor = Color.White,
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "지도 페이지 이동", fontWeight = FontWeight.Bold)
        }
    }
}

internal fun formatKoreanPrice(amountManwon: Int?): String {
    if (amountManwon == null) return "—"
    val eok = amountManwon / 10000
    val man = amountManwon % 10000
    return when {
        eok > 0 && man > 0 -> "%d억 %,d".format(eok, man)
        eok > 0 -> "%d억".format(eok)
        else -> "%,d만".format(man)
    }
}
