package com.example.myapplication.ui.components

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


@Composable
fun PanelContent(
    onDetailClick: () -> Unit,
    onMoveToMapPage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
            .padding(
                start = 24.dp,
                end = 24.dp,
                top = 30.dp,
                bottom = 10.dp
            )
    ) {
        // 건물명
        Text(
            text = "[건물명]",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp) // 줄 사이 간격
        ) {
            // 1. 최근 거래가 줄
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween, // 양 끝으로 밀어내기
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "최근 거래가",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
                Text(
                    text = "1억",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            // 2. 전용면적 줄
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "전용면적",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
                Text(
                    text = "84㎡",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Button(
                onClick = onDetailClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD2D2D2),
                    contentColor = Color(0xFF005CAF)
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp) // 평평한 디자인
            ) {
                Text(
                    text = "상세정보",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))


        Button(
            onClick = onMoveToMapPage,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF005CAF),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "지도 페이지 이동",
                fontWeight = FontWeight.Bold
            )
        }
    }
}