package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource


import com.example.myapplication.ui.components.CameraPreview
import com.example.myapplication.ui.components.DetailPopupContent
import com.example.myapplication.ui.components.PanelContent


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(onMoveToMapPage: () -> Unit
) {
    val scaffoldState = rememberBottomSheetScaffoldState()

    var showDetailPopup by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = 80.dp, // 하단 패널이 올라와 있는 높이
            sheetShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            sheetContainerColor = Color(0xCCFFFFFF),
            sheetDragHandle = { BottomSheetDefaults.DragHandle() }, // 상단 회색 바
            sheetContent = {
                PanelContent(
                    onDetailClick = {
                        showDetailPopup = true
                    },
                    onMoveToMapPage = {
                        onMoveToMapPage()
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFEFEFEF)),
                contentAlignment = Alignment.Center
            ) {
                CameraPreview()
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // [주소창] 모양
                Surface(
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "현재 주소",
                            color = Color.DarkGray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        if (showDetailPopup) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)) // 바깥 누르면 닫힘
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        showDetailPopup = false
                    },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .wrapContentHeight()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { },
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 10.dp
                ) {
                    DetailPopupContent(
                        onClose = {
                            showDetailPopup = false
                        },
                        onMoveToMapPage = {
                            onMoveToMapPage()
                        }
                    )
                }
            }
        }
    }
}