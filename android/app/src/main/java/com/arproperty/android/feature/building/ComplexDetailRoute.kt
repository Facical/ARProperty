package com.arproperty.android.feature.building

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arproperty.android.core.designsystem.PlaceholderCard

@Composable
fun ComplexDetailRoute(complexId: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Complex Detail",
            style = MaterialTheme.typography.headlineMedium,
        )

        PlaceholderCard(
            title = "단지 ID: $complexId",
            body = "이 화면은 단지 메타데이터와 동 목록, 단지 전체 거래 흐름을 연결할 자리입니다.",
        )
    }
}
