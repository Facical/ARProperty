package com.arproperty.android.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.arproperty.android.core.designsystem.theme.ARPropertyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ARPropertyTheme {
                ARPropertyApp()
            }
        }
    }
}
