package com.example.myapplication.ui.components

import android.annotation.SuppressLint
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview as CameraPreviewUseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CameraPreview() {

    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { context ->

            val previewView = PreviewView(context)

            val cameraProviderFuture =
                ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({

                val cameraProvider =
                    cameraProviderFuture.get()

                val preview =
                    CameraPreviewUseCase.Builder().build()

                preview.setSurfaceProvider(
                    previewView.surfaceProvider
                )

                val cameraSelector =
                    CameraSelector.DEFAULT_BACK_CAMERA

                try {

                    cameraProvider.unbindAll()

                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )

                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }, ContextCompat.getMainExecutor(context))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}